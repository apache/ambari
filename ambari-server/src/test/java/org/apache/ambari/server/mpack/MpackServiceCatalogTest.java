/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.mpack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.AmbariManagementHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.StackResolutionContext;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.OsFamily;
import org.junit.Test;

import com.google.inject.Provider;

public class MpackServiceCatalogTest {
  @Test
  public void resolvesEveryServiceInAnImportedPackageAndSelectsOneWithoutPublishing() throws Exception {
    MpackSnapshotsTest fixture = new MpackSnapshotsTest(); fixture.temporary.create();
    try {
      fixture.initialize();
      var snapshotsField = MpackSnapshotsTest.class.getDeclaredField("snapshots"); snapshotsField.setAccessible(true);
      MpackSnapshots snapshots = (MpackSnapshots) snapshotsField.get(fixture);
      var archiveField = MpackSnapshotsTest.class.getDeclaredField("archives"); archiveField.setAccessible(true);
      MpackArchiveStore archives = (MpackArchiveStore) archiveField.get(fixture);
      MpackResources resources = new MpackResources(archives);
      var manifest = MpackManifestTest.manifest().put("name", "examples");
      String extension = "<metainfo><versions><active>true</active></versions><prerequisites>"
          + "<min-stack-versions><stack><name>BASE</name><version>1.0</version></stack></min-stack-versions>"
          + "</prerequisites></metainfo>";
      byte[] archive = MpackArchiveStoreTest.archive(
          new MpackArchiveStoreTest.Member("mpack.json", manifest.toString(), null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/metainfo.xml", extension, null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/services/QUEUE/metainfo.xml", service("QUEUE"), null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/services/QUEUE/package/scripts/service.py", "pass\n", null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/services/DATABASE/metainfo.xml", service("DATABASE"), null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/services/DATABASE/package/scripts/service.py", "pass\n", null));
      var imported = resources.inspect(archives.accept(new ByteArrayInputStream(archive), null).digest());
      var release = new MpackLifecycleState.Release(1, imported.manifest().identity(), imported.archiveDigest(),
          imported.manifest().canonicalJson(), imported.contributions(), Map.of(), true);
      var builtin = snapshots.captureBuiltins(List.of());
      MpackCatalog catalog = mock(MpackCatalog.class);
      when(catalog.control()).thenReturn(new MpackCatalog.Versioned<>(MpackCatalog.CONTROL, 0,
          new MpackLifecycleState.Control(1, builtin.id(), builtin.id(), null, 0, List.of())));
      when(catalog.releases()).thenReturn(List.of(new MpackCatalog.Versioned<>(MpackCatalog.releaseKey(release.id()), 0, release)));
      ExtensionLinkDAO links = mock(ExtensionLinkDAO.class);
      StackManagerFactory factory = mock(StackManagerFactory.class);
      when(factory.createCandidate(any(), any(), any(), any(), any())).thenAnswer(call -> new StackManager(
          call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3), call.getArgument(4),
          mock(MetainfoDAO.class), new ActionMetadata(), mock(StackDAO.class), mock(ExtensionDAO.class), links,
          mock(AmbariManagementHelper.class)));
      MpackDefinitionLoader loader = new MpackDefinitionLoader(snapshots, factory, mock(OsFamily.class), links);
      Clusters clusters = mock(Clusters.class); when(clusters.getClusters()).thenReturn(Map.of());
      MpackServiceCatalog services = new MpackServiceCatalog();
      MpackActivationTest.inject(services, "catalog", catalog); MpackActivationTest.inject(services, "snapshots", snapshots);
      MpackActivationTest.inject(services, "resources", resources); MpackActivationTest.inject(services, "loader", loader);
      MpackActivationTest.inject(services, "clusters", (Provider<Clusters>) () -> clusters);
      var view = services.view();
      assertEquals(List.of(), view.unavailable());
      assertEquals(java.util.Set.of("QUEUE", "DATABASE"), view.items().stream().map(MpackServiceCatalog.Entry::serviceName)
          .collect(java.util.stream.Collectors.toSet()));
      assertTrue(view.items().stream().noneMatch(MpackServiceCatalog.Entry::enabled));
      var selected = view.items().stream().filter(entry -> entry.serviceName().equals("QUEUE")).findFirst().orElseThrow();
      var prepared = services.prepare(new MpackServiceCatalog.Selection(1, List.of(selected.id()), null, false));
      assertEquals(List.of("QUEUE"), prepared.deployment().serviceNames());
      assertEquals(List.of(release.id()), prepared.mutation().releaseIds());
      assertEquals(MpackLifecycleState.Action.ENABLE, prepared.mutation().action());
      assertEquals(List.of(new StackResolutionContext.Binding("BASE", "1.0", "EXAMPLES", "1.0")), prepared.mutation().bindings());
      assertEquals(MpackException.Code.STALE_PLAN, assertThrows(MpackException.class, () ->
          services.prepare(new MpackServiceCatalog.Selection(1, List.of("f".repeat(64)), null, false))).getCode());
      verify(catalog, never()).apply(anyList());

      manifest.put("version", "1.0.0.1");
      byte[] update = MpackArchiveStoreTest.archive(
          new MpackArchiveStoreTest.Member("mpack.json", manifest.toString(), null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/metainfo.xml", extension, null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/services/QUEUE/metainfo.xml", service("QUEUE"), null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/services/QUEUE/package/scripts/service.py", "pass\n", null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/services/DATABASE/metainfo.xml", service("DATABASE"), null),
          new MpackArchiveStoreTest.Member("extensions/EXAMPLES/1.0/services/DATABASE/package/scripts/service.py", "pass\n", null));
      var replacement = resources.inspect(archives.accept(new ByteArrayInputStream(update), null).digest());
      var newer = new MpackLifecycleState.Release(1, replacement.manifest().identity(), replacement.archiveDigest(),
          replacement.manifest().canonicalJson(), replacement.contributions(), Map.of(), true);
      var published = snapshots.compose(builtin, List.of(replacement), prepared.mutation().bindings());
      when(catalog.control()).thenReturn(new MpackCatalog.Versioned<>(MpackCatalog.CONTROL, 1,
          new MpackLifecycleState.Control(1, builtin.id(), published.id(), null, 1, List.of(newer.id()))));
      when(catalog.releases()).thenReturn(List.of(new MpackCatalog.Versioned<>(MpackCatalog.releaseKey(release.id()), 0, release),
          new MpackCatalog.Versioned<>(MpackCatalog.releaseKey(newer.id()), 0, newer)));
      var planner = new MpackPlanner(catalog, resources, snapshots, loader, () -> null, () -> clusters);
      var removal = planner.plan(new MpackLifecycleState.Mutation(1, MpackLifecycleState.Action.UNINSTALL,
          List.of(), List.of(release.id()), List.of(), true, false));
      assertEquals(published.id(), removal.candidateSnapshot());
      assertEquals(List.of(newer.id()), removal.activeReleases());
    } finally { fixture.temporary.delete(); }
  }

  private static String service(String name) {
    return "<metainfo><schemaVersion>2.0</schemaVersion><services><service><name>" + name
        + "</name><displayName>" + name + "</displayName><version>1.0</version><components><component><name>"
        + name + "_SERVER</name><displayName>Server</displayName><category>MASTER</category><cardinality>1</cardinality>"
        + "<versionAdvertised>false</versionAdvertised><commandScript><script>scripts/service.py</script>"
        + "<scriptType>PYTHON</scriptType><timeout>60</timeout></commandScript></component></components></service></services></metainfo>";
  }
}
