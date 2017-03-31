/**
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import com.google.inject.Injector;

/**
 * Tests OozieConfigCalculation logic
 */
public class FixOozieAdminUsersTest {
  private Injector injector;
  private Clusters clusters;
  private Cluster cluster;
  private Field clustersField;

  @Before
  public void setup() throws Exception {
    injector = EasyMock.createMock(Injector.class);
    clusters = EasyMock.createMock(Clusters.class);
    cluster = EasyMock.createMock(Cluster.class);

    Map<String, String> mockProperties = new HashMap<String, String>() {{
      put("falcon_user", "falcon");
    }};

    Config falconEnvConfig = EasyMock.createNiceMock(Config.class);
    expect(falconEnvConfig.getType()).andReturn("falcon-env").anyTimes();
    expect(falconEnvConfig.getProperties()).andReturn(mockProperties).anyTimes();

    mockProperties = new HashMap<String, String>() {{
      put("oozie_admin_users", "oozie, oozie-admin");
    }};

    Config oozieEnvConfig = EasyMock.createNiceMock(Config.class);
    expect(oozieEnvConfig.getType()).andReturn("oozie-env").anyTimes();
    expect(oozieEnvConfig.getProperties()).andReturn(mockProperties).anyTimes();

    expect(cluster.getDesiredConfigByType("falcon-env")).andReturn(falconEnvConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("oozie-env")).andReturn(oozieEnvConfig).atLeastOnce();

    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(injector.getInstance(Clusters.class)).andReturn(clusters).atLeastOnce();
    replay(injector, clusters, falconEnvConfig, oozieEnvConfig);

    clustersField = FixOozieAdminUsers.class.getDeclaredField("clusters");
    clustersField.setAccessible(true);

  }

  /**
   * Makes sure falcon user is added to oozie admin users
   * @throws Exception
   */
  @Test
  public void testOozieAdminUserUpdated() throws Exception {

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(cluster, hrc);

    FixOozieAdminUsers action = new FixOozieAdminUsers();
    clustersField.set(action, clusters);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);


    Cluster c = clusters.getCluster("c1");
    Config oozieConfig = c.getDesiredConfigByType("oozie-env");
    Config falconConfig = c.getDesiredConfigByType("falcon-env");

    Map<String, String> oozieConfigMap = oozieConfig.getProperties();
    Map<String, String> falconConfigMap = falconConfig.getProperties();

    assertTrue(oozieConfigMap.containsKey("oozie_admin_users"));
    assertTrue(falconConfigMap.containsKey("falcon_user"));
    String oozieAdminUsers = oozieConfigMap.get("oozie_admin_users");
    String falconUser = falconConfigMap.get("falcon_user");

    assertTrue(oozieAdminUsers.indexOf(falconUser) != -1);

  }

}
