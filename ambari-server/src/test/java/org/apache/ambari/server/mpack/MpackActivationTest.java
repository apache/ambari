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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.junit.Test;

import com.google.inject.Provider;

public class MpackActivationTest {
  static void inject(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  public void failedConsumerRefreshRestoresOneVerifiedViewWithoutRefreshingUnrelatedService() throws Exception {
    MpackActivation activation = new MpackActivation();
    MpackRuntime runtime = new MpackRuntime();
    MpackSnapshots snapshots = mock(MpackSnapshots.class);
    var old = new MpackSnapshots.Snapshot(1, "a".repeat(64), null, List.of(), List.of(), Map.of(), Map.of());
    var next = new MpackSnapshots.Snapshot(1, "b".repeat(64), null, List.of(), List.of(), Map.of(), Map.of());
    StackManager oldManager = mock(StackManager.class), nextManager = mock(StackManager.class);
    when(nextManager.getDefinitionSnapshotId()).thenReturn(next.id());
    Path root = Path.of("/verified-resources");
    when(snapshots.resourceRoot(next.id())).thenReturn(root);
    AmbariMetaInfo metadata = mock(AmbariMetaInfo.class);
    var previous = new AmbariMetaInfo.DefinitionView(oldManager, root.toFile(), root.toFile(), root.toFile());
    when(metadata.captureDefinitionView()).thenReturn(previous);
    AtomicReference<StackManager> visible = new AtomicReference<>(oldManager);
    doAnswer(call -> { visible.set(call.getArgument(0)); return null; }).when(metadata).publishMpackCandidate(any(), any());
    doAnswer(call -> { visible.set(previous.manager()); return null; }).when(metadata).restoreDefinitionView(previous);
    Service affected = mock(Service.class), unrelated = mock(Service.class);
    when(affected.getName()).thenReturn("DATABASE"); when(unrelated.getName()).thenReturn("QUEUE");
    when(affected.getDesiredStackId()).thenReturn(new StackId("BASE", "1.0"));
    when(unrelated.getDesiredStackId()).thenReturn(new StackId("BASE", "1.0"));
    when(affected.getServiceComponents()).thenReturn(Map.of());
    doThrow(new AmbariException("injected refresh failure")).doNothing().when(affected).updateServiceInfo();
    Cluster cluster = mock(Cluster.class); when(cluster.getServices()).thenReturn(Map.of("DATABASE", affected, "QUEUE", unrelated));
    Clusters clusters = mock(Clusters.class); when(clusters.getClusters()).thenReturn(Map.of("test", cluster));
    inject(activation, "runtime", runtime); inject(activation, "snapshots", snapshots);
    inject(activation, "metadata", (Provider<AmbariMetaInfo>) () -> metadata);
    inject(activation, "clusters", (Provider<Clusters>) () -> clusters);
    inject(activation, "advisors", (Provider<StackAdvisorHelper>) () -> mock(StackAdvisorHelper.class));
    runtime.writeLock().lock();
    try {
      runtime.restore(old, "operation");
      runtime.reserve("operation", List.of(new MpackScope("BASE", "1.0", "DATABASE", Set.of())));
      assertThrows(AmbariException.class, () -> activation.publishRuntime("operation", next, nextManager));
    } finally { runtime.writeLock().unlock(); }
    assertSame(oldManager, visible.get()); assertEquals(old, runtime.snapshot());
    runtime.requireReadable(new StackId("BASE", "1.0"), "DATABASE");
    verify(unrelated, never()).updateServiceInfo();
  }
}
