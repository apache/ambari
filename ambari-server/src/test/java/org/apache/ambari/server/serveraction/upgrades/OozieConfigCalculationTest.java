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

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ServiceComponentSupport;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * Tests OozieConfigCalculation logic
 */
public class OozieConfigCalculationTest {

  private Injector m_injector;
  private Clusters m_clusters;
  private AgentConfigsHolder agentConfigsHolder;
  private Field m_clusterField;
  private Field m_componentSupportField;
  private Field agentConfigsHolderField;
  private OozieConfigCalculation m_action;
  private final UpgradeContext m_mockUpgradeContext = EasyMock.createNiceMock(UpgradeContext.class);
  private ServiceComponentSupport componentSupport;

  @Before
  public void setup() throws Exception {

    m_injector = EasyMock.createMock(Injector.class);
    componentSupport = EasyMock.createMock(ServiceComponentSupport.class);
    m_clusters = EasyMock.createMock(Clusters.class);
    agentConfigsHolder = createMock(AgentConfigsHolder.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);

    m_action = PowerMock.createNicePartialMock(OozieConfigCalculation.class, "getUpgradeContext");

    expect(m_action.getUpgradeContext(cluster)).andReturn(m_mockUpgradeContext).once();
    StackId stackId = new StackId("HDP", "3.0.0");
    expect(m_mockUpgradeContext.getTargetStack()).andReturn(stackId).once();

    expect(componentSupport.isServiceSupported("FALCON", stackId.getStackName(), stackId.getStackVersion()))
        .andReturn(false).once();

    Map<String, String> siteMockProperties = new HashMap<String, String>() {{
      put("oozie.service.ELService.ext.functions.coord-action-create", "some value");
      put("oozie.service.ELService.ext.functions.coord-action-start", "some value");
      put("oozie.systemmode", "NORMAL");
    }};

    Config oozie_site = EasyMock.createNiceMock(Config.class);
    expect(oozie_site.getType()).andReturn("oozie-site").anyTimes();
    expect(oozie_site.getProperties()).andReturn(siteMockProperties).anyTimes();
    expect(cluster.getDesiredConfigByType("oozie-site")).andReturn(oozie_site).atLeastOnce();

    Map<String, String> mockProperties = new HashMap<String, String>() {{
      put("content", "#!/bin/bash\n" +
          "\n" +
          "if [ -d \"/usr/lib/bigtop-tomcat\" ]; then\n" +
          "  export OOZIE_CONFIG=${OOZIE_CONFIG:-/etc/oozie/conf}\n" +
          "  export CATALINA_BASE=${CATALINA_BASE:-{{oozie_server_dir}}}\n" +
          "  export CATALINA_TMPDIR=${CATALINA_TMPDIR:-/var/tmp/oozie}\n" +
          "  export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat\n" +
          "fi\n" +
          "\n" +
          "# export OOZIE_BASE_URL=\"http://${OOZIE_HTTP_HOSTNAME}:${OOZIE_HTTP_PORT}/oozie\"\n" +
          "export JAVA_LIBRARY_PATH={{hadoop_lib_home}}/native/Linux-amd64-64");
    }};

    Config oozieEnv = EasyMock.createNiceMock(Config.class);
    expect(oozieEnv.getType()).andReturn("oozie-env").anyTimes();
    expect(oozieEnv.getProperties()).andReturn(mockProperties).anyTimes();
    expect(cluster.getDesiredConfigByType("oozie-env")).andReturn(oozieEnv).atLeastOnce();

    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).atLeastOnce();
    expect(cluster.getHosts()).andReturn(Collections.emptyList()).atLeastOnce();
    agentConfigsHolder.updateData(eq(1L), eq(Collections.emptyList()));
    expectLastCall().atLeastOnce();

    expect(m_injector.getInstance(Clusters.class)).andReturn(m_clusters).atLeastOnce();

    expect(m_injector.getInstance(ServiceComponentSupport.class)).andReturn(componentSupport).atLeastOnce();

    replay(componentSupport, m_mockUpgradeContext, m_action, m_injector, m_clusters, cluster, oozieEnv, oozie_site, agentConfigsHolder);

    m_clusterField = AbstractUpgradeServerAction.class.getDeclaredField("m_clusters");
    m_clusterField.setAccessible(true);

    m_componentSupportField = OozieConfigCalculation.class.getDeclaredField("serviceComponentSupport");
    m_componentSupportField.setAccessible(true);

    agentConfigsHolderField = AbstractUpgradeServerAction.class.getDeclaredField("agentConfigsHolder");
    agentConfigsHolderField.setAccessible(true);
  }

  @Test
  public void testAction() throws Exception {
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    m_clusterField.set(m_action, m_clusters);
    m_componentSupportField.set(m_action, componentSupport);
    agentConfigsHolderField.set(m_action, agentConfigsHolder);

    m_action.setExecutionCommand(executionCommand);
    m_action.setHostRoleCommand(hrc);

    CommandReport report = m_action.execute(null);
    assertNotNull(report);

    Cluster c = m_clusters.getCluster("c1");
    Config oozieSiteConfig = c.getDesiredConfigByType("oozie-site");
    Map<String, String> map = oozieSiteConfig.getProperties();

    assertTrue(map.size()==1);
    assertTrue(map.containsKey("oozie.systemmode"));
    assertTrue(map.get("oozie.systemmode").equals("NORMAL"));

    assertTrue(report.getStdOut().contains("Removed following properties"));

    Config oozieEnvConfig = c.getDesiredConfigByType("oozie-env");
    map = oozieEnvConfig.getProperties();

    Assert.assertTrue(map.containsKey("content"));
    String content = map.get("content");
    assertTrue(content.endsWith("export HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\" "));
  }

  /**
   * Checks that -Dhdp.version is added to $HADOOP_OPTS variable at oozie-env
   * content.
   * Also checks that it is not added multiple times during upgrades
   * @throws Exception
   */
  @Test
  public void testOozieEnvWithMissingParam() throws Exception {
    // Test case when old content does not contain $HADOOP_OPTS variable at all
    String oldContent = "#!/bin/bash\n" +
      "\n" +
      "if [ -d \"/usr/lib/bigtop-tomcat\" ]; then\n" +
      "  export OOZIE_CONFIG=${OOZIE_CONFIG:-/etc/oozie/conf}\n" +
      "  export CATALINA_BASE=${CATALINA_BASE:-{{oozie_server_dir}}}\n" +
      "  export CATALINA_TMPDIR=${CATALINA_TMPDIR:-/var/tmp/oozie}\n" +
      "  export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat\n" +
      "fi\n" +
      "\n" +
      "# export OOZIE_BASE_URL=\"http://${OOZIE_HTTP_HOSTNAME}:${OOZIE_HTTP_PORT}/oozie\"\n" +
      "export JAVA_LIBRARY_PATH={{hadoop_lib_home}}/native/Linux-amd64-64";
    String newContent = OozieConfigCalculation.processPropertyValue(oldContent);
    assertTrue(newContent.endsWith("export HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\" "));
    // Test case when old content contains proper $HADOOP_OPTS variable
    oldContent = newContent;
    newContent = OozieConfigCalculation.processPropertyValue(oldContent);
    assertEquals(newContent, oldContent);
    assertEquals(1, StringUtils.countMatches(newContent, "-Dhdp.version"));
    // Test case when old content contains $HADOOP_OPTS variable with some value
    oldContent = "#!/bin/bash\n" +
      "\n" +
      "if [ -d \"/usr/lib/bigtop-tomcat\" ]; then\n" +
      "  export OOZIE_CONFIG=${OOZIE_CONFIG:-/etc/oozie/conf}\n" +
      "  export CATALINA_BASE=${CATALINA_BASE:-{{oozie_server_dir}}}\n" +
      "  export CATALINA_TMPDIR=${CATALINA_TMPDIR:-/var/tmp/oozie}\n" +
      "  export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat\n" +
      "  export HADOOP_OPTS=-Dsome.option1 -Dsome.option1 $HADOOP_OPTS\n" +
      "fi\n" +
      "\n" +
      "# export OOZIE_BASE_URL=\"http://${OOZIE_HTTP_HOSTNAME}:${OOZIE_HTTP_PORT}/oozie\"\n" +
      "export JAVA_LIBRARY_PATH={{hadoop_lib_home}}/native/Linux-amd64-64";
    newContent = OozieConfigCalculation.processPropertyValue(oldContent);
    assertEquals("#!/bin/bash\n" +
      "\n" +
      "if [ -d \"/usr/lib/bigtop-tomcat\" ]; then\n" +
      "  export OOZIE_CONFIG=${OOZIE_CONFIG:-/etc/oozie/conf}\n" +
      "  export CATALINA_BASE=${CATALINA_BASE:-{{oozie_server_dir}}}\n" +
      "  export CATALINA_TMPDIR=${CATALINA_TMPDIR:-/var/tmp/oozie}\n" +
      "  export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat\n" +
      "  export HADOOP_OPTS=-Dsome.option1 -Dsome.option1 $HADOOP_OPTS\n" +
      "fi\n" +
      "\n" +
      "# export OOZIE_BASE_URL=\"http://${OOZIE_HTTP_HOSTNAME}:${OOZIE_HTTP_PORT}/oozie\"\n" +
      "export JAVA_LIBRARY_PATH={{hadoop_lib_home}}/native/Linux-amd64-64\n" +
      "export HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\" ", newContent);
  }

}
