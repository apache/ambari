/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.mpack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Path;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ExtensionInfo;

import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.junit.Before;
import org.junit.Test;

public class MpackPlannerTest {
  private MpackPlanner planner;
  private Cluster cluster;
  private StackManager definitions;
  private MpackSnapshots.Snapshot previous;
  private MpackSnapshots.Snapshot candidate;

  @Before
  public void setUp() {
    cluster = mock(Cluster.class);
    Clusters clusters = mock(Clusters.class);
    when(clusters.getClusters()).thenReturn(Map.of("ordinary", cluster));
    when(cluster.getClusterName()).thenReturn("ordinary");
    when(cluster.getDesiredStackVersion()).thenReturn(new StackId("GENERIC", "1.0"));
    when(cluster.getServices()).thenReturn(Map.of());
    StackInfo stack = new StackInfo();
    stack.setName("GENERIC");
    stack.setVersion("1.0");
    stack.setHooksFolder("");
    definitions = mock(StackManager.class);
    when(definitions.getStack("GENERIC", "1.0")).thenReturn(stack);
    previous = snapshot("a".repeat(64), "c".repeat(64));
    candidate = snapshot("b".repeat(64), "d".repeat(64));
    MpackDefinitionLoader loader = mock(MpackDefinitionLoader.class);
    when(loader.resolve(previous)).thenReturn(definitions);
    planner = new MpackPlanner(mock(MpackCatalog.class), mock(MpackResources.class),
        mock(MpackSnapshots.class), loader, () -> null, () -> clusters);
  }

  @Test
  public void changedContextReportsEvenAClusterWithoutServices() {
    MpackPlanner.Impact impact = planner.impact(previous, candidate, definitions);
    assertEquals(List.of("ordinary"), impact.clusters());
    assertEquals(false, impact.inUseDefinitions());
  }

  @Test
  public void affectedActiveUpgradeBlocksDefinitionMutation() {
    UpgradeEntity upgrade = mock(UpgradeEntity.class);
    when(upgrade.getId()).thenReturn(41L);
    when(cluster.getUpgradeInProgress()).thenReturn(upgrade);
    MpackException error = assertThrows(MpackException.class,
        () -> planner.impact(previous, candidate, definitions));
    assertEquals(MpackException.Code.OPERATION_CONFLICT, error.getCode());
    assertEquals(41L, error.getDetails().get("upgrade_id"));
  }

  @Test
  public void unchangedDefinitionsDoNotDisruptAnUpgrade() {
    when(cluster.getUpgradeInProgress()).thenReturn(mock(UpgradeEntity.class));
    assertEquals(List.of(), planner.impact(previous, previous, definitions).clusters());
  }

  @Test
  public void deployedCategoryChangesRequireMigration() throws Exception {
    StackInfo before = definitions.getStack("GENERIC", "1.0");
    ServiceInfo oldService = new ServiceInfo(); oldService.setName("EXAMPLE");
    ComponentInfo oldComponent = new ComponentInfo(); oldComponent.setName("EXAMPLE_SERVER"); oldComponent.setCategory("MASTER");
    oldService.getComponents().add(oldComponent); before.setServices(List.of(oldService));
    ServiceInfo nextService = new ServiceInfo(); nextService.setName("EXAMPLE");
    ComponentInfo nextComponent = new ComponentInfo(); nextComponent.setName("EXAMPLE_SERVER"); nextComponent.setCategory("CLIENT");
    nextService.getComponents().add(nextComponent);
    StackInfo next = new StackInfo(); next.setName("GENERIC"); next.setVersion("1.0"); next.setHooksFolder("");
    next.setServices(List.of(nextService));
    StackManager nextDefinitions = mock(StackManager.class); when(nextDefinitions.getStack("GENERIC", "1.0")).thenReturn(next);
    Service service = mock(Service.class);
    when(service.getName()).thenReturn("EXAMPLE"); when(service.getDesiredStackId()).thenReturn(new StackId("GENERIC", "1.0"));
    when(service.getServiceComponents()).thenReturn(Map.of("EXAMPLE_SERVER", mock(ServiceComponent.class)));
    when(cluster.getServices()).thenReturn(Map.of("EXAMPLE", service));
    assertEquals(MpackException.Code.UNSUPPORTED_OPERATION,
        assertThrows(MpackException.class, () -> planner.impact(previous, candidate, nextDefinitions)).getCode());
  }

  @Test
  public void addingAnExtensionDoesNotReserveUnrelatedServicesInTheSameStack() throws Exception {
    String oldId = "a".repeat(64), nextId = "b".repeat(64);
    Map<String, MpackSnapshots.Resource> oldFiles = Map.of(
        "extensions/OLD/1.0/metainfo.xml", resource("extensions/OLD/1.0/metainfo.xml", "c"),
        "extensions/OLD/1.0/services/DATABASE/metainfo.xml", resource("extensions/OLD/1.0/services/DATABASE/metainfo.xml", "d"));
    Map<String, MpackSnapshots.Resource> nextFiles = new java.util.TreeMap<>(oldFiles);
    nextFiles.put("extensions/NEW/1.0/metainfo.xml", resource("extensions/NEW/1.0/metainfo.xml", "e"));
    nextFiles.put("extensions/NEW/1.0/services/QUEUE/metainfo.xml", resource("extensions/NEW/1.0/services/QUEUE/metainfo.xml", "f"));
    MpackSnapshots.Snapshot first = new MpackSnapshots.Snapshot(1, oldId, null, List.of(), List.of(), oldFiles, Map.of());
    MpackSnapshots.Snapshot second = new MpackSnapshots.Snapshot(1, nextId, null, List.of(), List.of(), nextFiles, Map.of());
    StackInfo oldStack = scopeStack(oldId, false), nextStack = scopeStack(nextId, true);
    StackManager oldDefinitions = mock(StackManager.class), nextDefinitions = mock(StackManager.class);
    when(oldDefinitions.getStacks()).thenReturn(List.of(oldStack)); when(nextDefinitions.getStacks()).thenReturn(List.of(nextStack));
    when(oldDefinitions.getStack("BASE", "1.0")).thenReturn(oldStack); when(nextDefinitions.getStack("BASE", "1.0")).thenReturn(nextStack);
    MpackDefinitionLoader loader = mock(MpackDefinitionLoader.class); when(loader.resolve(first)).thenReturn(oldDefinitions);
    MpackSnapshots snapshots = mock(MpackSnapshots.class);
    when(snapshots.resourceRoot(oldId)).thenReturn(Path.of("/snapshots/" + oldId));
    when(snapshots.resourceRoot(nextId)).thenReturn(Path.of("/snapshots/" + nextId));
    MpackPlanner isolated = new MpackPlanner(mock(MpackCatalog.class), mock(MpackResources.class), snapshots,
        loader, () -> null, () -> mock(Clusters.class));
    List<MpackScope> scopes = isolated.definitionScopes(first, second, nextDefinitions);
    assertEquals(List.of("QUEUE"), scopes.stream().map(MpackScope::serviceName).toList());
  }

  private static MpackSnapshots.Resource resource(String path, String digest) {
    return new MpackSnapshots.Resource(path, "provider", digest.repeat(64), 1, false);
  }

  private static StackInfo scopeStack(String snapshot, boolean includeQueue) {
    StackInfo stack = new StackInfo(); stack.setName("BASE"); stack.setVersion("1.0"); stack.setHooksFolder("");
    java.util.ArrayList<ServiceInfo> services = new java.util.ArrayList<>();
    for (String extension : includeQueue ? List.of("OLD", "NEW") : List.of("OLD")) {
      ExtensionInfo info = new ExtensionInfo(); info.setName(extension); info.setVersion("1.0"); stack.addExtension(info);
      ServiceInfo service = new ServiceInfo(); service.setName(extension.equals("OLD") ? "DATABASE" : "QUEUE");
      service.addDefinitionResourceRoots(List.of("/snapshots/" + snapshot + "/extensions/" + extension + "/1.0/services/" + service.getName()));
      services.add(service);
    }
    stack.setServices(services);
    return stack;
  }

  private MpackSnapshots.Snapshot snapshot(String id, String contentDigest) {
    String path = "stacks/GENERIC/1.0/metainfo.xml";
    return new MpackSnapshots.Snapshot(1, id, null, List.of(), List.of(), Map.of(path,
        new MpackSnapshots.Resource(path, "generic-base/1.0", contentDigest, 1, false)), Map.of());
  }
}
