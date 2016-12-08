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
import static org.junit.Assert.assertEquals;
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
 * Tests upgrade-related server side actions
 */
public class RangerConfigCalculationTest {

  private Injector m_injector;
  private Clusters m_clusters;
  private Field m_clusterField;

  @Before
  public void setup() throws Exception {
    m_injector = EasyMock.createMock(Injector.class);
    m_clusters = EasyMock.createMock(Clusters.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);

    Map<String, String> mockProperties = new HashMap<String, String>() {{
      put("DB_FLAVOR", "MYSQL");
      put("db_host", "host1");
      put("db_name", "ranger");
      put("audit_db_name", "ranger_audit");
    }};

    Config adminConfig = EasyMock.createNiceMock(Config.class);
    expect(adminConfig.getType()).andReturn("admin-properties").anyTimes();
    expect(adminConfig.getProperties()).andReturn(mockProperties).anyTimes();

    mockProperties = new HashMap<String, String>();

    Config adminSiteConfig = EasyMock.createNiceMock(Config.class);
    expect(adminSiteConfig.getType()).andReturn("admin-properties").anyTimes();
    expect(adminSiteConfig.getProperties()).andReturn(mockProperties).anyTimes();

    Config rangerEnv = EasyMock.createNiceMock(Config.class);
    expect(rangerEnv.getType()).andReturn("ranger-env").anyTimes();
    expect(rangerEnv.getProperties()).andReturn(mockProperties).anyTimes();


    expect(cluster.getDesiredConfigByType("admin-properties")).andReturn(adminConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("ranger-admin-site")).andReturn(adminSiteConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("ranger-env")).andReturn(rangerEnv).atLeastOnce();

    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(m_injector.getInstance(Clusters.class)).andReturn(m_clusters).atLeastOnce();

    replay(m_injector, m_clusters, cluster, adminConfig, adminSiteConfig, rangerEnv);

    m_clusterField = RangerConfigCalculation.class.getDeclaredField("m_clusters");
    m_clusterField.setAccessible(true);
  }

  @Test
  public void testAction() throws Exception {

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    RangerConfigCalculation action = new RangerConfigCalculation();
    m_clusterField.set(action, m_clusters);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Cluster c = m_clusters.getCluster("c1");
    Config config = c.getDesiredConfigByType("ranger-admin-site");
    Map<String, String> map = config.getProperties();

    assertTrue(map.containsKey("ranger.jpa.jdbc.driver"));
    assertTrue(map.containsKey("ranger.jpa.jdbc.url"));
    assertTrue(map.containsKey("ranger.jpa.jdbc.dialect"));
    assertTrue(map.containsKey("ranger.jpa.audit.jdbc.driver"));
    assertTrue(map.containsKey("ranger.jpa.audit.jdbc.url"));
    assertTrue(map.containsKey("ranger.jpa.audit.jdbc.dialect"));

    assertEquals("com.mysql.jdbc.Driver", map.get("ranger.jpa.jdbc.driver"));
    assertEquals("jdbc:mysql://host1/ranger", map.get("ranger.jpa.jdbc.url"));
    assertEquals("org.eclipse.persistence.platform.database.MySQLPlatform", map.get("ranger.jpa.jdbc.dialect"));

    assertEquals("com.mysql.jdbc.Driver", map.get("ranger.jpa.audit.jdbc.driver"));
    assertEquals("jdbc:mysql://host1/ranger_audit", map.get("ranger.jpa.audit.jdbc.url"));
    assertEquals("org.eclipse.persistence.platform.database.MySQLPlatform", map.get("ranger.jpa.audit.jdbc.dialect"));

    config = c.getDesiredConfigByType("ranger-env");
    map = config.getProperties();
    assertEquals("jdbc:mysql://host1", map.get("ranger_privelege_user_jdbc_url"));

    config = c.getDesiredConfigByType("admin-properties");
    config.getProperties().put("DB_FLAVOR", "oracle");

    report = action.execute(null);
    assertNotNull(report);

    config = c.getDesiredConfigByType("ranger-admin-site");
    map = config.getProperties();

    assertEquals("oracle.jdbc.OracleDriver", map.get("ranger.jpa.jdbc.driver"));
    assertEquals("jdbc:oracle:thin:@//host1", map.get("ranger.jpa.jdbc.url"));
    assertEquals("org.eclipse.persistence.platform.database.OraclePlatform", map.get("ranger.jpa.jdbc.dialect"));

    assertEquals("oracle.jdbc.OracleDriver", map.get("ranger.jpa.audit.jdbc.driver"));
    assertEquals("jdbc:oracle:thin:@//host1", map.get("ranger.jpa.audit.jdbc.url"));
    assertEquals("org.eclipse.persistence.platform.database.OraclePlatform", map.get("ranger.jpa.audit.jdbc.dialect"));

    config = c.getDesiredConfigByType("ranger-env");
    map = config.getProperties();
    assertEquals("jdbc:oracle:thin:@//host1", map.get("ranger_privelege_user_jdbc_url"));

  }


}
