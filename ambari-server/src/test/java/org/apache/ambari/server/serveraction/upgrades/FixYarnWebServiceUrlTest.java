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
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

/**
 * Test FixYarnWebServiceUrl logic
 */
public class FixYarnWebServiceUrlTest {

    private Injector injector;
    private Clusters clusters;
    private AgentConfigsHolder agentConfigsHolder;
    private Cluster cluster;
    private Field clustersField;
    private Field agentConfigsHolderField;
    private static final String SOURCE_CONFIG_TYPE = "yarn-site";
    private static final String YARN_TIMELINE_WEBAPP_HTTPADDRESS = "yarn.timeline-service.webapp.address";
    private static final String YARN_TIMELINE_WEBAPP_HTTPSADDRESS = "yarn.timeline-service.webapp.https.address";
    private static final String YARN_LOGSERVER_WEBSERVICE_URL = "yarn.log.server.web-service.url";
    private static final String YARN_HTTP_POLICY = "yarn.http.policy";

    @Before
    public void setup() throws Exception {
        injector = EasyMock.createMock(Injector.class);
        clusters = EasyMock.createMock(Clusters.class);
        cluster = EasyMock.createMock(Cluster.class);
        agentConfigsHolder = createMock(AgentConfigsHolder.class);
        clustersField = AbstractUpgradeServerAction.class.getDeclaredField("m_clusters");
        clustersField.setAccessible(true);
        agentConfigsHolderField = AbstractUpgradeServerAction.class.getDeclaredField("agentConfigsHolder");
        agentConfigsHolderField.setAccessible(true);

        expect(clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
        expect(cluster.getClusterId()).andReturn(1L).atLeastOnce();
        expect(cluster.getHosts()).andReturn(Collections.emptyList()).atLeastOnce();
        agentConfigsHolder.updateData(eq(1L), eq(Collections.emptyList()));
        expectLastCall().atLeastOnce();

        expect(injector.getInstance(Clusters.class)).andReturn(clusters).atLeastOnce();
        replay(injector, clusters, agentConfigsHolder);
    }
    /**
     * Test when http policy is set to HTTP_ONLY
     * @throws Exception
     */
    @Test
    public void testHttpOnly() throws Exception {

        Map<String, String> mockProperties = new HashMap<String, String>() {{
            put(YARN_TIMELINE_WEBAPP_HTTPADDRESS, "c6401.ambari.apache.org:8188");
            put(YARN_TIMELINE_WEBAPP_HTTPSADDRESS, "c6401.ambari.apache.org:8190");
            put(YARN_HTTP_POLICY, "HTTP_ONLY");
            put(YARN_LOGSERVER_WEBSERVICE_URL, "http://localhost:8188/ws/v1/applicationhistory");
        }};

        Config yarnSiteConfig = EasyMock.createNiceMock(Config.class);
        expect(yarnSiteConfig.getType()).andReturn("yarn-site").anyTimes();
        expect(yarnSiteConfig.getProperties()).andReturn(mockProperties).anyTimes();

        expect(cluster.getDesiredConfigByType(SOURCE_CONFIG_TYPE)).andReturn(yarnSiteConfig).atLeastOnce();

        Map<String, String> commandParams = new HashMap<>();
        commandParams.put("clusterName", "c1");

        ExecutionCommand executionCommand = new ExecutionCommand();
        executionCommand.setCommandParams(commandParams);
        executionCommand.setClusterName("c1");

        HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
        expect(hrc.getRequestId()).andReturn(1L).anyTimes();
        expect(hrc.getStageId()).andReturn(2L).anyTimes();
        expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
        replay(cluster, hrc,yarnSiteConfig);

        FixYarnWebServiceUrl action = new FixYarnWebServiceUrl();
        clustersField.set(action, clusters);
        agentConfigsHolderField.set(action, agentConfigsHolder);

        action.setExecutionCommand(executionCommand);
        action.setHostRoleCommand(hrc);

        CommandReport report = action.execute(null);
        assertNotNull(report);

        Cluster c = clusters.getCluster("c1");
        Config desiredYarnSiteConfig = c.getDesiredConfigByType(SOURCE_CONFIG_TYPE);

        Map<String, String> yarnSiteConfigMap = desiredYarnSiteConfig.getProperties();

        assertTrue(yarnSiteConfigMap.containsKey(YARN_LOGSERVER_WEBSERVICE_URL));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_HTTP_POLICY));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_TIMELINE_WEBAPP_HTTPADDRESS));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_TIMELINE_WEBAPP_HTTPSADDRESS));
        String yarnLogServerWebServiceUrl = yarnSiteConfigMap.get(YARN_LOGSERVER_WEBSERVICE_URL);

        assertEquals("http://c6401.ambari.apache.org:8188/ws/v1/applicationhistory", yarnLogServerWebServiceUrl);

    }

    /**
     * Test when http policy is set to HTTPS_ONLY
     * @throws Exception
     */
    @Test
    public void testHttpsOnly() throws Exception {

        Map<String, String> mockProperties = new HashMap<String, String>() {{
            put(YARN_TIMELINE_WEBAPP_HTTPADDRESS, "c6401.ambari.apache.org:8188");
            put(YARN_TIMELINE_WEBAPP_HTTPSADDRESS, "c6401.ambari.apache.org:8190");
            put(YARN_HTTP_POLICY, "HTTPS_ONLY");
            put(YARN_LOGSERVER_WEBSERVICE_URL, "http://localhost:8188/ws/v1/applicationhistory");
        }};

        Config yarnSiteConfig = EasyMock.createNiceMock(Config.class);
        expect(yarnSiteConfig.getType()).andReturn("yarn-site").anyTimes();
        expect(yarnSiteConfig.getProperties()).andReturn(mockProperties).anyTimes();

        expect(cluster.getDesiredConfigByType(SOURCE_CONFIG_TYPE)).andReturn(yarnSiteConfig).atLeastOnce();

        Map<String, String> commandParams = new HashMap<>();
        commandParams.put("clusterName", "c1");

        ExecutionCommand executionCommand = new ExecutionCommand();
        executionCommand.setCommandParams(commandParams);
        executionCommand.setClusterName("c1");

        HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
        expect(hrc.getRequestId()).andReturn(1L).anyTimes();
        expect(hrc.getStageId()).andReturn(2L).anyTimes();
        expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
        replay(cluster, hrc,yarnSiteConfig);

        FixYarnWebServiceUrl action = new FixYarnWebServiceUrl();
        clustersField.set(action, clusters);

        action.setExecutionCommand(executionCommand);
        action.setHostRoleCommand(hrc);
        agentConfigsHolderField.set(action, agentConfigsHolder);

        CommandReport report = action.execute(null);
        assertNotNull(report);

        Cluster c = clusters.getCluster("c1");
        Config desiredYarnSiteConfig = c.getDesiredConfigByType(SOURCE_CONFIG_TYPE);

        Map<String, String> yarnSiteConfigMap = desiredYarnSiteConfig.getProperties();

        assertTrue(yarnSiteConfigMap.containsKey(YARN_LOGSERVER_WEBSERVICE_URL));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_HTTP_POLICY));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_TIMELINE_WEBAPP_HTTPADDRESS));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_TIMELINE_WEBAPP_HTTPSADDRESS));
        String yarnLogServerWebServiceUrl = yarnSiteConfigMap.get(YARN_LOGSERVER_WEBSERVICE_URL);

        assertEquals("https://c6401.ambari.apache.org:8190/ws/v1/applicationhistory", yarnLogServerWebServiceUrl);

    }

    /**
     * Test when http policy is set to incorrect value
     * @throws Exception
     */
    @Test
    public void testIncorrectValue() throws Exception {

        Map<String, String> mockProperties = new HashMap<String, String>() {{
            put(YARN_TIMELINE_WEBAPP_HTTPADDRESS, "c6401.ambari.apache.org:8188");
            put(YARN_TIMELINE_WEBAPP_HTTPSADDRESS, "c6401.ambari.apache.org:8190");
            put(YARN_HTTP_POLICY, "abc");
            put(YARN_LOGSERVER_WEBSERVICE_URL, "http://localhost:8188/ws/v1/applicationhistory");
        }};

        Config yarnSiteConfig = EasyMock.createNiceMock(Config.class);
        expect(yarnSiteConfig.getType()).andReturn("yarn-site").anyTimes();
        expect(yarnSiteConfig.getProperties()).andReturn(mockProperties).anyTimes();

        expect(cluster.getDesiredConfigByType(SOURCE_CONFIG_TYPE)).andReturn(yarnSiteConfig).atLeastOnce();

        Map<String, String> commandParams = new HashMap<>();
        commandParams.put("clusterName", "c1");

        ExecutionCommand executionCommand = new ExecutionCommand();
        executionCommand.setCommandParams(commandParams);
        executionCommand.setClusterName("c1");

        HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
        expect(hrc.getRequestId()).andReturn(1L).anyTimes();
        expect(hrc.getStageId()).andReturn(2L).anyTimes();
        expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
        replay(cluster, hrc,yarnSiteConfig);

        FixYarnWebServiceUrl action = new FixYarnWebServiceUrl();
        clustersField.set(action, clusters);
        agentConfigsHolderField.set(action, agentConfigsHolder);

        action.setExecutionCommand(executionCommand);
        action.setHostRoleCommand(hrc);

        CommandReport report = action.execute(null);
        assertNotNull(report);

        Cluster c = clusters.getCluster("c1");
        Config desiredYarnSiteConfig = c.getDesiredConfigByType(SOURCE_CONFIG_TYPE);

        Map<String, String> yarnSiteConfigMap = desiredYarnSiteConfig.getProperties();

        assertTrue(yarnSiteConfigMap.containsKey(YARN_LOGSERVER_WEBSERVICE_URL));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_HTTP_POLICY));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_TIMELINE_WEBAPP_HTTPADDRESS));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_TIMELINE_WEBAPP_HTTPSADDRESS));
        String yarnLogServerWebServiceUrl = yarnSiteConfigMap.get(YARN_LOGSERVER_WEBSERVICE_URL);

        assertEquals("http://localhost:8188/ws/v1/applicationhistory",yarnLogServerWebServiceUrl);
        assertEquals(SOURCE_CONFIG_TYPE +"/" + YARN_HTTP_POLICY + " property contains an invalid value. It should be from [HTTP_ONLY,HTTPS_ONLY]", report.getStdOut());

    }

    /**
     * Test when some values are null
     * @throws Exception
     */
    @Test
    public void testNullValues() throws Exception {

        Map<String, String> mockProperties = new HashMap<String, String>() {{
            put(YARN_TIMELINE_WEBAPP_HTTPADDRESS, null);
            put(YARN_TIMELINE_WEBAPP_HTTPSADDRESS, "c6401.ambari.apache.org:8190");
            put(YARN_HTTP_POLICY, null);
            put(YARN_LOGSERVER_WEBSERVICE_URL, "http://localhost:8188/ws/v1/applicationhistory");
        }};

        Config yarnSiteConfig = EasyMock.createNiceMock(Config.class);
        expect(yarnSiteConfig.getType()).andReturn("yarn-site").anyTimes();
        expect(yarnSiteConfig.getProperties()).andReturn(mockProperties).anyTimes();

        expect(cluster.getDesiredConfigByType(SOURCE_CONFIG_TYPE)).andReturn(yarnSiteConfig).atLeastOnce();


        Map<String, String> commandParams = new HashMap<>();
        commandParams.put("clusterName", "c1");

        ExecutionCommand executionCommand = new ExecutionCommand();
        executionCommand.setCommandParams(commandParams);
        executionCommand.setClusterName("c1");

        HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
        expect(hrc.getRequestId()).andReturn(1L).anyTimes();
        expect(hrc.getStageId()).andReturn(2L).anyTimes();
        expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
        replay(cluster, hrc,yarnSiteConfig);

        FixYarnWebServiceUrl action = new FixYarnWebServiceUrl();
        clustersField.set(action, clusters);
        agentConfigsHolderField.set(action, agentConfigsHolder);

        action.setExecutionCommand(executionCommand);
        action.setHostRoleCommand(hrc);

        CommandReport report = action.execute(null);
        assertNotNull(report);

        Cluster c = clusters.getCluster("c1");
        Config desiredYarnSiteConfig = c.getDesiredConfigByType(SOURCE_CONFIG_TYPE);

        Map<String, String> yarnSiteConfigMap = yarnSiteConfig.getProperties();
        yarnSiteConfigMap.put(YARN_TIMELINE_WEBAPP_HTTPADDRESS, "");

        assertTrue(yarnSiteConfigMap.containsKey(YARN_LOGSERVER_WEBSERVICE_URL));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_HTTP_POLICY));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_TIMELINE_WEBAPP_HTTPADDRESS));
        assertTrue(yarnSiteConfigMap.containsKey(YARN_TIMELINE_WEBAPP_HTTPSADDRESS));
        String yarnLogServerWebServiceUrl = yarnSiteConfigMap.get(YARN_LOGSERVER_WEBSERVICE_URL);

        assertEquals("http://localhost:8188/ws/v1/applicationhistory", yarnLogServerWebServiceUrl);
        assertEquals(SOURCE_CONFIG_TYPE + "/" +YARN_HTTP_POLICY +" property is null", report.getStdOut());

    }



}
