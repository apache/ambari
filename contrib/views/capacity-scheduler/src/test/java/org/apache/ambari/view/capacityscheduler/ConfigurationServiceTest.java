/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.capacityscheduler;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;

import static org.easymock.EasyMock.*;


public class ConfigurationServiceTest {
    private ViewContext context;
    private Cluster ambariCluster;
    private ConfigurationService configurationService;

    public static final String BASE_URI = "http://localhost:8084/myapp/";


    @Before
    public void setUp() throws Exception {
        context = createNiceMock(ViewContext.class);
        ambariCluster = createNiceMock(Cluster.class);

        expect(ambariCluster.getConfigurationValue("ranger-yarn-plugin-properties", "ranger-yarn-plugin-enabled")).andReturn("Yes").anyTimes();
        expect(context.getCluster()).andReturn(ambariCluster).anyTimes();
        expect(context.getProperties()).andReturn(null).anyTimes();
        replay(context);
        replay(ambariCluster);
        System.out.println("context.getProperties() : " + context.getProperties());
        configurationService = new ConfigurationService(context);
    }

    @Test
    public void testRightConfigurationValue() {
        Response response = configurationService.getConfigurationValue("ranger-yarn-plugin-properties", "ranger-yarn-plugin-enabled");
        JSONObject jsonObject = (JSONObject) response.getEntity();
        JSONArray arr = (JSONArray) jsonObject.get("configs");
        Assert.assertEquals(arr.size(), 1);
        JSONObject obj = (JSONObject) arr.get(0);

        Assert.assertEquals(obj.get("siteName"), "ranger-yarn-plugin-properties");
        Assert.assertEquals(obj.get("configName"), "ranger-yarn-plugin-enabled");
        Assert.assertEquals(obj.get("configValue"), "Yes"); // because I set it myself.
    }

    @Test
    public void testExceptionOnWrongConfigurationValue() {
        Response response = configurationService.getConfigurationValue("random-site", "random-key");
        JSONObject jsonObject = (JSONObject) response.getEntity();
        JSONArray arr = (JSONArray) jsonObject.get("configs");
        Assert.assertEquals(arr.size(), 1);
        JSONObject obj = (JSONObject) arr.get(0);

        Assert.assertEquals(obj.get("siteName"), "random-site");
        Assert.assertEquals(obj.get("configName"), "random-key");
        Assert.assertEquals(obj.get("configValue"), null);
    }

    @Test
    public void testRefreshResourceManagerRequestIsValidJson() throws Exception {
        JSONObject request = parseRequestTemplate("REFRESH_RM_REQUEST_DATA", "rm1.example.com,rm2.example.com");
        JSONObject requestInfo = (JSONObject) request.get("RequestInfo");
        JSONArray filters = (JSONArray) request.get("Requests/resource_filters");

        Assert.assertEquals("REFRESHQUEUES", requestInfo.get("command"));
        Assert.assertEquals("capacity-scheduler", requestInfo.get("parameters/forceRefreshConfigTags"));
        Assert.assertEquals("rm1.example.com,rm2.example.com", ((JSONObject) filters.get(0)).get("hosts"));
    }

    @Test
    public void testRestartResourceManagerRequestIsValidJson() throws Exception {
        JSONObject request = parseRequestTemplate(
            "RESTART_RM_REQUEST_DATA", "test-cluster", "rm1.example.com,rm2.example.com",
            "rm1.example.com,rm2.example.com");
        JSONObject requestInfo = (JSONObject) request.get("RequestInfo");
        JSONObject operationLevel = (JSONObject) requestInfo.get("operation_level");
        JSONArray filters = (JSONArray) request.get("Requests/resource_filters");

        Assert.assertEquals("RESTART", requestInfo.get("command"));
        Assert.assertEquals("test-cluster", operationLevel.get("cluster_name"));
        Assert.assertEquals("rm1.example.com,rm2.example.com", ((JSONObject) filters.get(0)).get("hosts"));
    }

    private JSONObject parseRequestTemplate(String fieldName, Object... values) throws Exception {
        Field field = ConfigurationService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        String request = String.format((String) field.get(null), values);
        Object parsed = JSONValue.parse(request);
        Assert.assertNotNull("Request template must contain valid JSON: " + request, parsed);
        return (JSONObject) parsed;
    }
}
