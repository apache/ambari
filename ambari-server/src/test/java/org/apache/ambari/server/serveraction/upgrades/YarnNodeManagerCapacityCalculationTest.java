/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.serveraction.upgrades;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Injector;


/**
 * Tests upgrade-related server side actions for YarnConfigCalculation
 */
@RunWith(PowerMockRunner.class)
public class YarnNodeManagerCapacityCalculationTest {
  private static final String YARN_SITE_CONFIG_TYPE = "yarn-site";
  private static final String YARN_ENV_CONFIG_TYPE = "yarn-env";
  private static final String YARN_HBASE_ENV_CONFIG_TYPE = "yarn-hbase-env";
  private static final String CAPACITY_SCHEDULER_CONFIG_TYPE = "capacity-scheduler";

  private static final String YARN_SYSTEM_SERVICE_USER_NAME = "yarn_ats_user";
  private static final String YARN_DEFAULT_QUEUE = "default";
  private static final String YARN_SYSTEM_SERVICE_QUEUE_NAME = "yarn-system";
  private static final String CAPACITY_SCHEDULER_ROOT_QUEUES = "yarn.scheduler.capacity.root.queues";
  private static final String YARN_SYSTEM_SERVICE_QUEUE_PREFIX = "yarn.scheduler.capacity.root." + YARN_SYSTEM_SERVICE_QUEUE_NAME;
  private static final String YARN_NM_PMEM_MB_PROPERTY_NAME = "yarn.nodemanager.resource.memory-mb";
  private static final String YARN_HBASE_SYSTEM_SERVICE_QUEUE_PROPERTY_NAME = "yarn_hbase_system_service_queue_name";
  private static final String YARN_HBASE_SYSTEM_SERVICE_LAUNCH_PROPERTY_NAME = "is_hbase_system_service_launch";

  private static final String CLUSTER_NAME = "C1";
  private static final String ats_user = "test1";

  private Injector m_injector;
  private Clusters m_clusters;
  private AgentConfigsHolder agentConfigsHolder;
  private Cluster cluster;
  private Field clusterField;
  private Field agentConfigsHolderField;

  private ServiceComponent serviceComponent;
  private Service service;
  private Set<String> hosts;

  @Before
  public void setup() throws Exception {
    m_injector = EasyMock.createMock(Injector.class);
    m_clusters = EasyMock.createMock(Clusters.class);
    agentConfigsHolder = createMock(AgentConfigsHolder.class);
    cluster = EasyMock.createMock(Cluster.class);

    Map<String, String> mockYarnProperties = new HashMap<String, String>() {
      {
        put(YARN_NM_PMEM_MB_PROPERTY_NAME, "20480");
      }
    };

    Config yarnConfig = EasyMock.createNiceMock(Config.class);
    expect(yarnConfig.getType()).andReturn(YARN_SITE_CONFIG_TYPE).anyTimes();
    expect(yarnConfig.getProperties()).andReturn(mockYarnProperties).anyTimes();
    expect(cluster.getDesiredConfigByType(YARN_SITE_CONFIG_TYPE)).andReturn(yarnConfig).anyTimes();

    Map<String, String> mockHbaseEnvProps =
        new HashMap<String, String>() {{
          put(YARN_HBASE_SYSTEM_SERVICE_LAUNCH_PROPERTY_NAME, "false");
          put(YARN_HBASE_SYSTEM_SERVICE_QUEUE_PROPERTY_NAME, YARN_DEFAULT_QUEUE);
        }};
    Config hbaseEnvConfig = EasyMock.createNiceMock(Config.class);
    expect(hbaseEnvConfig.getType()).andReturn(YARN_HBASE_ENV_CONFIG_TYPE).anyTimes();
    expect(hbaseEnvConfig.getProperties()).andReturn(mockHbaseEnvProps).anyTimes();
    expect(cluster.getDesiredConfigByType(YARN_HBASE_ENV_CONFIG_TYPE)).andReturn(hbaseEnvConfig).anyTimes();


    Map<String, String> mockYarnEnvProps =
        new HashMap<String, String>() {{
          put(YARN_SYSTEM_SERVICE_USER_NAME, ats_user);
        }};
    Config yarnEnvConfig = EasyMock.createNiceMock(Config.class);
    expect(yarnEnvConfig.getType()).andReturn(YARN_ENV_CONFIG_TYPE).anyTimes();
    expect(yarnEnvConfig.getProperties()).andReturn(mockYarnEnvProps).anyTimes();
    expect(cluster.getDesiredConfigByType(YARN_ENV_CONFIG_TYPE)).andReturn(yarnEnvConfig).anyTimes();

    Map<String, String> mockCsProps =
        new HashMap<String, String>() {{
          put(CAPACITY_SCHEDULER_ROOT_QUEUES, YARN_DEFAULT_QUEUE);
        }};
    Config yarnCsConfig = EasyMock.createNiceMock(Config.class);
    expect(yarnCsConfig.getType()).andReturn(CAPACITY_SCHEDULER_CONFIG_TYPE).anyTimes();
    expect(yarnCsConfig.getProperties()).andReturn(mockCsProps).anyTimes();
    expect(cluster.getDesiredConfigByType(CAPACITY_SCHEDULER_CONFIG_TYPE)).andReturn(yarnCsConfig).anyTimes();



    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(m_injector.getInstance(Clusters.class)).andReturn(m_clusters).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).atLeastOnce();
    expect(cluster.getHosts()).andReturn(Collections.emptyList()).atLeastOnce();

    hosts = Mockito.mock(Set.class);
    service = Mockito.mock(Service.class);
    serviceComponent = Mockito.mock(ServiceComponent.class);
    expect(cluster.getService("YARN")).andReturn(service);
    Mockito.when(service.getServiceComponent("NODEMANAGER")).thenReturn(serviceComponent);


    agentConfigsHolder.updateData(eq(1L), eq(Collections.emptyList()));
    expectLastCall().atLeastOnce();

    replay(m_injector, m_clusters, yarnConfig, hbaseEnvConfig, yarnEnvConfig, yarnCsConfig, agentConfigsHolder);

    clusterField =
        AbstractUpgradeServerAction.class.getDeclaredField("m_clusters");
    clusterField.setAccessible(true);
    agentConfigsHolderField = AbstractUpgradeServerAction.class.getDeclaredField("agentConfigsHolder");
    agentConfigsHolderField.setAccessible(true);
  }

  @Test
  public void testActionForNotCreatingYarnSystemServiceQueueWhenClusterIsSmall()
      throws Exception {
    Mockito.when(serviceComponent.getServiceComponentsHosts()).thenReturn(hosts);
    Mockito.when(hosts.size()).thenReturn(2);

    YarnNodeManagerCapacityCalculation action = getYarnNodeManagerCapacityCalculation();

    // validate before values
    Cluster c = m_clusters.getCluster(CLUSTER_NAME);

    Config hbaseEnvConfig =
        c.getDesiredConfigByType(YARN_HBASE_ENV_CONFIG_TYPE);
    validateYarnHBaseEnvProperties(hbaseEnvConfig, false, YARN_DEFAULT_QUEUE);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    validateYarnHBaseEnvProperties(hbaseEnvConfig, false, YARN_DEFAULT_QUEUE);
  }

  @Test
  public void testActionForCreatingYarnSystemServiceQueue()
      throws Exception {

    Mockito.when(serviceComponent.getServiceComponentsHosts()).thenReturn(hosts);
    Mockito.when(hosts.size()).thenReturn(3);

    YarnNodeManagerCapacityCalculation action = getYarnNodeManagerCapacityCalculation();

    // validate before values
    Cluster c = m_clusters.getCluster(CLUSTER_NAME);

    // Before validation
    Config hbaseEnvConfig =
        c.getDesiredConfigByType(YARN_HBASE_ENV_CONFIG_TYPE);
    validateYarnHBaseEnvProperties(hbaseEnvConfig, false, YARN_DEFAULT_QUEUE);


    Config csConfig =
        c.getDesiredConfigByType(CAPACITY_SCHEDULER_CONFIG_TYPE);
    validateYarnCapacitySchedulerProperties(csConfig, true);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    // After validation
    validateYarnHBaseEnvProperties(hbaseEnvConfig, false, YARN_SYSTEM_SERVICE_QUEUE_NAME);
    validateYarnCapacitySchedulerProperties(csConfig, false);
  }

  private void validateYarnCapacitySchedulerProperties(Config csConfig, boolean before) {
    Map<String, String> csProps = csConfig.getProperties();

    assertTrue(csProps.containsKey(CAPACITY_SCHEDULER_ROOT_QUEUES));
    String[] split = csProps.get(CAPACITY_SCHEDULER_ROOT_QUEUES).split(",");
    if(before){
      assertEquals(1, split.length);
      assertEquals(YARN_DEFAULT_QUEUE, split[0]);
    } else{
      assertEquals(2, split.length);
      assertEquals(YARN_DEFAULT_QUEUE, split[0]);
      assertEquals(YARN_SYSTEM_SERVICE_QUEUE_NAME, split[1]);

      assertEquals("0",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".capacity"));
      assertEquals("100",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".maximum-capacity"));
      assertEquals("1",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".user-limit-factor"));
      assertEquals("100",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".minimum-user-limit-percent"));
      assertEquals("RUNNING",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".state"));
      assertEquals("fifo",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".ordering-policy"));
      assertEquals(ats_user,csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".acl_submit_applications"));
      assertEquals(ats_user,csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".acl_administer_queue"));
      assertEquals("0.5",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".maximum-am-resource-percent"));
      assertEquals("true",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".disable_preemption"));
      assertEquals("true",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".intra-queue-preemption.disable_preemption"));
      assertEquals("32768",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".priority"));
      assertEquals("-1",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".maximum-application-lifetime"));
      assertEquals("-1",csProps.get(YARN_SYSTEM_SERVICE_QUEUE_PREFIX+".default-application-lifetime"));
    }
  }

  private YarnNodeManagerCapacityCalculation getYarnNodeManagerCapacityCalculation()
      throws IllegalAccessException {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<>();
    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    YarnNodeManagerCapacityCalculation action = new YarnNodeManagerCapacityCalculation();

    commandParams.put("clusterName", CLUSTER_NAME);
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(CLUSTER_NAME);

    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper())
        .andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(cluster, hrc);

    clusterField.set(action, m_clusters);
    agentConfigsHolderField.set(action, agentConfigsHolder);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);
    return action;
  }

  private void validateYarnHBaseEnvProperties(Config hbaseEnvConfig,
      boolean expected, String queueName) {
    Map<String, String> map = hbaseEnvConfig.getProperties();
    assertTrue(map.containsKey(YARN_HBASE_SYSTEM_SERVICE_LAUNCH_PROPERTY_NAME));
    assertEquals(expected,
        Boolean.parseBoolean(map.get(YARN_HBASE_SYSTEM_SERVICE_LAUNCH_PROPERTY_NAME)));

    assertTrue(map.containsKey(YARN_HBASE_SYSTEM_SERVICE_QUEUE_PROPERTY_NAME));
    assertEquals(queueName, map.get(YARN_HBASE_SYSTEM_SERVICE_QUEUE_PROPERTY_NAME));
  }
}