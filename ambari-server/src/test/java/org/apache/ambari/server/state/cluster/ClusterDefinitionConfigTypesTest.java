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
package org.apache.ambari.server.state.cluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.junit.Test;

import com.google.common.collect.HashMultimap;

public class ClusterDefinitionConfigTypesTest {
  @Test
  public void refreshesConfigOwnershipAfterPublicationAndRollback() throws Exception {
    ClusterImpl cluster = mock(ClusterImpl.class, CALLS_REAL_METHODS);
    AmbariMetaInfo metadata = mock(AmbariMetaInfo.class);
    StackManager before = mock(StackManager.class);
    StackManager after = mock(StackManager.class);
    ServiceInfo doris = mock(ServiceInfo.class);
    when(doris.getConfigTypeAttributes()).thenReturn(Map.of("doris-env", Map.of()));
    set(cluster, "ambariMetaInfo", metadata);
    set(cluster, "desiredStackVersion", new StackId("BIGTOP", "3.3.0"));
    set(cluster, "serviceConfigDefinitions", before);
    set(cluster, "serviceConfigTypes", HashMultimap.create());
    when(metadata.getStackManager()).thenReturn(before);
    assertEquals(List.of(), cluster.serviceNameByConfigType("doris-env"));
    when(metadata.getStackManager()).thenReturn(after);
    when(metadata.getServices("BIGTOP", "3.3.0")).thenReturn(Map.of("DORIS", doris));
    assertEquals(List.of("DORIS"), cluster.serviceNameByConfigType("doris-env"));
    when(metadata.getStackManager()).thenReturn(before);
    when(metadata.getServices("BIGTOP", "3.3.0")).thenReturn(Map.of());
    assertEquals(List.of(), cluster.serviceNameByConfigType("doris-env"));
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = ClusterImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
