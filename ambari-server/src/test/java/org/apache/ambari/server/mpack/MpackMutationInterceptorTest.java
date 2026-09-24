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
import java.util.Set;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.junit.Test;

public class MpackMutationInterceptorTest {
  @Test
  public void directDomainCallsUseTheSameServiceReservationAsHttp() throws Throwable {
    MpackRuntime runtime = new MpackRuntime(); runtime.writeLock().lock();
    try { runtime.reserve("operation", List.of(new MpackScope("BASE", "1.0", "DATABASE", Set.of()))); }
    finally { runtime.writeLock().unlock(); }
    MpackMutationInterceptor guard = new MpackMutationInterceptor(); MpackActivationTest.inject(guard, "runtime", runtime);
    Cluster cluster = mock(Cluster.class); when(cluster.getDesiredStackVersion()).thenReturn(new StackId("BASE", "1.0"));
    MethodInvocation permitted = invocation(cluster, "QUEUE"); when(permitted.proceed()).thenReturn("created");
    assertEquals("created", guard.invoke(permitted));
    MethodInvocation blocked = invocation(cluster, "DATABASE");
    assertThrows(MpackException.class, () -> guard.invoke(blocked));
    verify(blocked, never()).proceed();
  }

  private static MethodInvocation invocation(Cluster cluster, String service) throws Exception {
    MethodInvocation invocation = mock(MethodInvocation.class);
    when(invocation.getThis()).thenReturn(cluster);
    when(invocation.getArguments()).thenReturn(new Object[] { service, null });
    when(invocation.getMethod()).thenReturn(ClusterImpl.class.getMethod("addService", String.class, RepositoryVersionEntity.class));
    return invocation;
  }
}
