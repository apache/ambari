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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;

/**
 * Test FixYarnWebServiceUrl logic
 */
public class FixCapacitySchedulerOrderingPolicyTest {

  private Injector injector;
  private Clusters clusters;
  private Cluster cluster;
  private Field clustersField;
  private static final String SOURCE_CONFIG_TYPE = "capacity-scheduler";

  private static final String POLICY_1 = "yarn.scheduler.capacity.root.hbase.ordering-policy";
  private static final String POLICY_2 = "yarn.scheduler.capacity.someRandom_name.ordering-policy";
  private static final String POLICY_3 = "yarn.scheduler.capacity._A.ordering-policy";

  @Before
  public void setup() throws Exception {
    injector = EasyMock.createMock(Injector.class);
    clusters = EasyMock.createMock(Clusters.class);
    cluster = EasyMock.createMock(Cluster.class);
    clustersField = AbstractUpgradeServerAction.class.getDeclaredField("m_clusters");
    clustersField.setAccessible(true);

    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(injector.getInstance(Clusters.class)).andReturn(clusters).atLeastOnce();
    replay(injector, clusters);
  }

  @Test
  public void testRootQueues() throws Exception {

    Map<String, String> mockProperties = new HashMap<>(new ImmutableMap.Builder<String, String>()
      .put("yarn.scheduler.capacity.root.hbase.queues", "a")
      .put("yarn.scheduler.capacity.someRandom_name.queues", "b")
      .put("yarn.scheduler.capacity.a-b-c.queues", "c")
      .put("yarn.scheduler.capacity._A.queues", "d")
      .put(POLICY_1, "fifo")
      .put(POLICY_2, "junk")
      .put(POLICY_3, "")

      .build());

    Config capacitySchedulerConfig = EasyMock.createNiceMock(Config.class);
    expect(capacitySchedulerConfig.getType()).andReturn("capacity-scheduler").anyTimes();
    expect(capacitySchedulerConfig.getProperties()).andReturn(mockProperties).anyTimes();

    expect(cluster.getDesiredConfigByType(SOURCE_CONFIG_TYPE)).andReturn(capacitySchedulerConfig).atLeastOnce();

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(cluster, hrc, capacitySchedulerConfig);


    FixCapacitySchedulerOrderingPolicy action = new FixCapacitySchedulerOrderingPolicy();
    clustersField.set(action, clusters);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Cluster c = clusters.getCluster("c1");
    Config desiredYarnSiteConfig = c.getDesiredConfigByType(SOURCE_CONFIG_TYPE);

    Map<String, String> yarnSiteConfigMap = desiredYarnSiteConfig.getProperties();

    assertEquals("utilization", yarnSiteConfigMap.get(POLICY_1));
    assertEquals("utilization", yarnSiteConfigMap.get(POLICY_2));
    assertEquals("utilization", yarnSiteConfigMap.get(POLICY_3));
    assertNull(yarnSiteConfigMap.get("yarn.scheduler.capacity.a-b-c.ordering-policy"));
  }

}
