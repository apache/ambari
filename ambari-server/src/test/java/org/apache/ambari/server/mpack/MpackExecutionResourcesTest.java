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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.*;
import com.google.inject.Provider;
import org.junit.Test;

public class MpackExecutionResourcesTest {
  @Test
  public void legacyTaskDoesNotRequireMpackDefinitionContext() {
    MpackExecutionResources resources = new MpackExecutionResources(
        new MpackRuntime(), mock(Configuration.class));
    ExecutionCommand command = new ExecutionCommand();
    command.setClusterName("legacy-cluster");
    command.setServiceName("LEGACY_SERVICE");
    Map<String, String> originalParams = command.getCommandParams();

    resources.pin(command);

    assertSame(originalParams, command.getCommandParams());
    assertTrue(originalParams.isEmpty());
  }

  @Test
  public void unrelatedPublicationPreservesPreparedTaskIdentityAndOriginalArchives() throws Exception {
    String oldId = "a".repeat(64), currentId = "b".repeat(64), digest = "c".repeat(64);
    String path = "stacks/BASE/1.0/services/EXAMPLE/package";
    String oldPath = "mpacks/" + oldId + "/" + path;
    var old = new MpackSnapshots.Snapshot(1, oldId, null, List.of(), List.of(), Map.of(), Map.of(oldPath, digest));
    var current = new MpackSnapshots.Snapshot(1, currentId, null, List.of(), List.of(), Map.of(), Map.of());
    MpackRuntime runtime = new MpackRuntime(); runtime.writeLock().lock();
    try { runtime.restore(current, null); } finally { runtime.writeLock().unlock(); }
    MpackExecutionResources resources = new MpackExecutionResources(runtime, mock(Configuration.class));
    Clusters clusters = mock(Clusters.class); Cluster cluster = mock(Cluster.class); Service service = mock(Service.class);
    when(clusters.getCluster("test")).thenReturn(cluster); when(cluster.getService("EXAMPLE")).thenReturn(service);
    when(service.getDesiredStackId()).thenReturn(new StackId("BASE", "1.0"));
    AmbariMetaInfo metadata = mock(AmbariMetaInfo.class); MpackPlanner planner = mock(MpackPlanner.class);
    when(planner.executionIdentity(any(), any(), any(), any())).thenReturn(digest);
    MpackSnapshots snapshots = mock(MpackSnapshots.class); when(snapshots.load(oldId)).thenReturn(old);
    MpackActivationTest.inject(resources, "clusters", (Provider<Clusters>) () -> clusters);
    MpackActivationTest.inject(resources, "metadata", (Provider<AmbariMetaInfo>) () -> metadata);
    MpackActivationTest.inject(resources, "planner", (Provider<MpackPlanner>) () -> planner);
    MpackActivationTest.inject(resources, "snapshots", (Provider<MpackSnapshots>) () -> snapshots);
    ExecutionCommand command = new ExecutionCommand(); command.setClusterName("test"); command.setServiceName("EXAMPLE");
    Map<String, String> params = new TreeMap<>(Map.of(
        MpackExecutionResources.SNAPSHOT_ID, oldId, MpackExecutionResources.DEFINITION_IDENTITY, digest,
        MpackExecutionResources.EXECUTION_ID, UUID.randomUUID().toString(),
        MpackExecutionResources.RESOURCE_CONTRACT, MpackExecutionResources.RESOURCE_PROTOCOL,
        MpackExecutionResources.RESOURCE_REFERENCES, MpackJson.canonical(MpackJson.tree(Map.of(path, oldPath))),
        ExecutionCommand.KeyNames.RESOURCE_ARCHIVE_DIGESTS, MpackJson.canonical(MpackJson.tree(old.archiveDigests())),
        "service_package_folder", oldPath));
    command.setCommandParams(params);
    resources.pin(command);
    assertSame(params, command.getCommandParams());
    assertEquals(oldPath, command.getCommandParams().get("service_package_folder"));
    params.put(MpackExecutionResources.DEFINITION_IDENTITY, "d".repeat(64));
    assertEquals(MpackException.Code.STALE_PLAN, assertThrows(MpackException.class, () -> resources.pin(command)).getCode());
  }
}
