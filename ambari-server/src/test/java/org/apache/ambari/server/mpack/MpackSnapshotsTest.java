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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.ambari.server.configuration.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MpackSnapshotsTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();
  private MpackArchiveStore archives;
  private MpackResources resources;
  private MpackSnapshots snapshots;

  @Before
  public void initialize() throws Exception {
    Path root = temporary.newFolder("resources").toPath();
    Path stacks = root.resolve("stacks");
    Path version = stacks.resolve("BASE/1.0");
    Files.createDirectories(version);
    Files.writeString(version.resolve("metainfo.xml"),
        "<metainfo><versions><active>true</active></versions></metainfo>");
    Configuration configuration = mock(Configuration.class);
    when(configuration.getMetadataPath()).thenReturn(stacks.toString());
    when(configuration.getResourceDirPath()).thenReturn(root.toString());
    when(configuration.getProperty(Configuration.HOOKS_FOLDER)).thenReturn("stack-hooks");
    archives = new MpackArchiveStore(temporary.newFolder("store").toPath(),
        new MpackArchiveStore.Limits(1048576, 1048576, 100));
    resources = new MpackResources(archives);
    snapshots = new MpackSnapshots(archives, resources, configuration);
  }

  private MpackResources.PreparedPack pack(String script) throws Exception {
    byte[] bytes = MpackArchiveStoreTest.archive(
        new MpackArchiveStoreTest.Member("mpack.json", MpackManifestTest.manifest().toString(), null),
        new MpackArchiveStoreTest.Member("extensions/NGINX/1.0/metainfo.xml",
            "<metainfo><versions><active>true</active></versions></metainfo>", null),
        new MpackArchiveStoreTest.Member("extensions/NGINX/1.0/services/NGINX/package/scripts/service.py",
            script, null));
    return resources.inspect(archives.accept(new ByteArrayInputStream(bytes), null).digest());
  }

  @Test
  public void preservesBuiltinOwnershipAndBothExecutionGenerations() throws Exception {
    MpackSnapshots.Snapshot builtin = snapshots.captureBuiltins(List.of());
    MpackSnapshots.Snapshot old = snapshots.compose(builtin, List.of(pack("old script")), List.of());
    MpackSnapshots.Snapshot next = snapshots.compose(builtin, List.of(pack("new script")), List.of());
    assertFalse(old.id().equals(next.id()));
    assertEquals("builtin", next.resources().get("stacks/BASE/1.0/metainfo.xml").provider());
    assertEquals("old script", Files.readString(snapshots.resourceRoot(old.id())
        .resolve("extensions/NGINX/1.0/services/NGINX/package/scripts/service.py")));
    assertEquals("new script", Files.readString(snapshots.resourceRoot(next.id())
        .resolve("extensions/NGINX/1.0/services/NGINX/package/scripts/service.py")));
    assertTrue(old.archiveDigests().keySet().iterator().next().startsWith("mpacks/" + old.id() + "/"));
    snapshots.verify(snapshots.load(old.id()));
    snapshots.verify(snapshots.load(next.id()));
  }

  @Test
  public void snapshotIdentityAndArchivesAreDeterministic() throws Exception {
    MpackSnapshots.Snapshot builtin = snapshots.captureBuiltins(List.of());
    MpackResources.PreparedPack pack = pack("script");
    MpackSnapshots.Snapshot first = snapshots.compose(builtin, List.of(pack), List.of());
    MpackSnapshots.Snapshot retry = snapshots.compose(builtin, List.of(pack), List.of());
    assertEquals(first.id(), retry.id());
    assertEquals(first.archiveDigests(), retry.archiveDigests());
  }

  @Test
  public void rejectsChangedOrAdditionalResourceAndChangedArchive() throws Exception {
    MpackSnapshots.Snapshot builtin = snapshots.captureBuiltins(List.of());
    MpackSnapshots.Snapshot candidate = snapshots.compose(builtin, List.of(pack("script")), List.of());
    Path root = snapshots.resourceRoot(candidate.id());
    Files.writeString(root.resolve("extensions/NGINX/1.0/services/NGINX/package/scripts/foreign.py"), "noise");
    assertEquals(MpackException.Code.DIGEST_MISMATCH,
        assertThrows(MpackException.class, () -> snapshots.verify(candidate)).getCode());
    Files.delete(root.resolve("extensions/NGINX/1.0/services/NGINX/package/scripts/foreign.py"));
    Files.writeString(root.resolve("extensions/NGINX/1.0/services/NGINX/package/archive.zip"), "corrupt");
    assertThrows(MpackException.class, () -> snapshots.verify(candidate));
  }

  @Test
  public void rejectsBuiltinReplacementInsteadOfTreatingFilesAsUnowned() throws Exception {
    MpackSnapshots.Snapshot builtin = snapshots.captureBuiltins(List.of());
    MpackResources.PreparedPack imported = pack("script");
    MpackResources.Contribution replacement = new MpackResources.Contribution(imported.archiveDigest(),
        "replacement", "extensions/NGINX/1.0/metainfo.xml", "stacks/BASE/1.0/metainfo.xml",
        "stacks/BASE/1.0", "stacks/BASE/1.0", "a".repeat(64), 1, false);
    MpackResources.PreparedPack conflicting = new MpackResources.PreparedPack(imported.manifest(),
        imported.archiveDigest(), List.of(replacement), "b".repeat(64));
    assertEquals(MpackException.Code.RESOURCE_CONFLICT,
        assertThrows(MpackException.class,
            () -> snapshots.compose(builtin, List.of(conflicting), List.of())).getCode());
  }

  @Test
  public void advisorImportsDoNotModifyTheVerifiedSnapshot() throws Exception {
    Path source = temporary.getRoot().toPath().resolve("resources/stacks/value.py");
    Files.writeString(source, "VALUE = 1\n");
    MpackSnapshots.Snapshot snapshot = snapshots.captureBuiltins(List.of());
    Path script = temporary.newFile("advisor.py").toPath();
    Files.writeString(script, "#!/usr/bin/env python3\nimport importlib.util, os\n"
        + "path = os.path.join(os.environ['METADATA_DIR_PATH'], 'value.py')\n"
        + "spec = importlib.util.spec_from_file_location('value', path)\n"
        + "module = importlib.util.module_from_spec(spec)\nspec.loader.exec_module(module)\n");
    assertTrue(script.toFile().setExecutable(true));
    Configuration config = mock(Configuration.class);
    when(config.getStackAdvisorScript()).thenReturn(script.toString());
    when(config.getMetadataPath()).thenReturn(snapshots.resourceRoot(snapshot.id()).resolve("stacks").toString());
    var runner = new org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner();
    runner.setConfigs(config);
    runner.runScript(org.apache.ambari.server.state.ServiceInfo.ServiceAdvisorType.PYTHON,
        org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT,
        temporary.newFolder("advisor-actions"));
    snapshots.verify(snapshot);
    assertFalse(Files.exists(snapshots.resourceRoot(snapshot.id()).resolve("stacks/__pycache__")));
  }
}
