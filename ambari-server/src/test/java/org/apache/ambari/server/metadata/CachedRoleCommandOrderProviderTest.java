/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.server.metadata;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.Cluster;
import org.junit.Test;

import com.google.inject.Injector;
import com.google.inject.Provider;

public class CachedRoleCommandOrderProviderTest {
  @Test
  public void refreshesAfterDefinitionPublicationAndRollback() throws Exception {
    CachedRoleCommandOrderProvider provider = new CachedRoleCommandOrderProvider();
    Injector injector = mock(Injector.class);
    AmbariMetaInfo metadata = mock(AmbariMetaInfo.class);
    Cluster cluster = mock(Cluster.class);
    StackManager initial = mock(StackManager.class);
    StackManager published = mock(StackManager.class);
    RoleCommandOrder first = mock(RoleCommandOrder.class);
    RoleCommandOrder second = mock(RoleCommandOrder.class);
    RoleCommandOrder restored = mock(RoleCommandOrder.class);
    set(provider, "injector", injector);
    set(provider, "ambariMetaInfo", (Provider<AmbariMetaInfo>) () -> metadata);
    when(cluster.getClusterId()).thenReturn(1L);
    when(injector.getInstance(RoleCommandOrder.class)).thenReturn(first, second, restored);
    when(metadata.getStackManager()).thenReturn(initial);
    assertSame(first, provider.getRoleCommandOrder(cluster));
    assertSame(first, provider.getRoleCommandOrder(cluster));
    when(metadata.getStackManager()).thenReturn(published);
    assertSame(second, provider.getRoleCommandOrder(cluster));
    assertSame(second, provider.getRoleCommandOrder(cluster));
    when(metadata.getStackManager()).thenReturn(initial);
    assertSame(restored, provider.getRoleCommandOrder(cluster));
    verify(injector, times(3)).getInstance(RoleCommandOrder.class);
    verify(first).initialize(eq(cluster), any());
    verify(second).initialize(eq(cluster), any());
    verify(restored).initialize(eq(cluster), any());
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
