/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.agent.stomp.dto.ClusterConfigs;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.inject.Provider;

public class ConfigHelperInitialClusterTest {
  private ConfigHelper helper;
  private Cluster cluster;
  private AgentConfigsHolder holder;

  @Before
  public void setup() throws Exception {
    helper = spy(new ConfigHelper(mock(Clusters.class), mock(AmbariMetaInfo.class),
        mock(Configuration.class), mock(ClusterDAO.class)));
    cluster = mock(Cluster.class);
    holder = mock(AgentConfigsHolder.class);
    when(cluster.getClusterId()).thenReturn(2L);
    when(cluster.getServices()).thenReturn(Collections.singletonMap("HDFS", mock(Service.class)));
    doNothing().when(helper).checkStaleConfigsStatusOnConfigsUpdate(eq(2L), eq("HDFS"), anyMap());
    MetadataHolder metadata = mock(MetadataHolder.class);
    AmbariManagementControllerImpl controller = mock(AmbariManagementControllerImpl.class);
    when(controller.getClusterMetadataOnConfigsUpdate(cluster)).thenReturn(mock(MetadataUpdateEvent.class));
    inject("m_agentConfigsHolder", (Provider<AgentConfigsHolder>) () -> holder);
    inject("m_metadataHolder", (Provider<MetadataHolder>) () -> metadata);
    inject("m_ambariManagementController", (Provider<AmbariManagementControllerImpl>) () -> controller);
  }

  private void inject(String name, Object value) throws Exception {
    Field field = ConfigHelper.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(helper, value);
  }

  private ClusterConfigs configurations(String value) {
    SortedMap<String, String> properties = new TreeMap<>();
    properties.put("dfs.replication", value);
    SortedMap<String, SortedMap<String, String>> types = new TreeMap<>();
    types.put("hdfs-site", properties);
    return new ClusterConfigs(types, new TreeMap<>());
  }

  private void observe(SortedMap<String, ClusterConfigs> current, SortedMap<String, ClusterConfigs> previous) throws Exception {
    when(holder.getCurrentData(eq(7L), anyMap())).thenReturn(new AgentConfigsUpdateEvent(7L, current));
    when(holder.initializeDataIfNeeded(7L, true)).thenReturn(new AgentConfigsUpdateEvent(7L, previous));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void assertChangedReplication() throws Exception {
    helper.updateAgentConfigs(cluster, Collections.singletonList(7L));
    ArgumentCaptor<Map<Long, Map<String, Collection<String>>>> changes = ArgumentCaptor.forClass(Map.class);
    verify(helper).checkStaleConfigsStatusOnConfigsUpdate(eq(2L), eq("HDFS"), changes.capture());
    assertEquals(Collections.singleton(7L), changes.getValue().keySet());
    assertEquals(Collections.singleton("hdfs-site"), changes.getValue().get(7L).keySet());
    assertEquals(Collections.singleton("dfs.replication"), changes.getValue().get(7L).get("hdfs-site"));
    verify(holder).updateData(2L, Collections.singletonList(7L));
  }

  @Test
  public void publishesNewClusterWithoutLosingOtherClusterHistory() throws Exception {
    SortedMap<String, ClusterConfigs> previous = new TreeMap<>();
    previous.put("99", configurations("3"));
    SortedMap<String, ClusterConfigs> current = new TreeMap<>(previous);
    current.put("2", configurations("1"));
    observe(current, previous);
    assertChangedReplication();
    assertEquals(Collections.singleton("99"), previous.keySet());
  }

  @Test
  public void comparesExistingClusterConfigurations() throws Exception {
    observe(new TreeMap<>(Collections.singletonMap("2", configurations("1"))),
        new TreeMap<>(Collections.singletonMap("2", configurations("2"))));
    assertChangedReplication();
  }

  @Test
  public void rejectsMissingAuthoritativeCluster() throws Exception {
    observe(new TreeMap<>(Collections.singletonMap("99", configurations("1"))), new TreeMap<>());
    assertThrows(AmbariException.class, () -> helper.updateAgentConfigs(cluster, Collections.singletonList(7L)));
    verify(holder, never()).updateData(anyLong(), anyList());
  }

  @Test
  public void rejectsMalformedCachedCluster() throws Exception {
    observe(new TreeMap<>(Collections.singletonMap("2", configurations("1"))),
        new TreeMap<>(Collections.singletonMap("2", new ClusterConfigs(null, null))));
    assertThrows(AmbariException.class, () -> helper.updateAgentConfigs(cluster, Collections.singletonList(7L)));
    verify(holder, never()).updateData(anyLong(), anyList());
  }
}
