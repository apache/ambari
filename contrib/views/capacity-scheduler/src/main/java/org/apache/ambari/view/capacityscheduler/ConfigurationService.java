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

package org.apache.ambari.view.capacityscheduler;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.capacityscheduler.utils.MisconfigurationFormattedException;
import org.apache.ambari.view.capacityscheduler.proxy.Proxy;
import org.apache.ambari.view.capacityscheduler.utils.ServiceFormattedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;

/**
 * Help service
 */
public class ConfigurationService {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);
  private final Proxy proxy;
  private final String baseUrl;

  private ViewContext context;
  private final String refreshRMRequestData =
      "{\n" +
      "  \"RequestInfo\" : {\n" +
      "    \"command\" : \"REFRESHQUEUES\",\n" +
      "    \"context\" : \"Refresh YARN Capacity Scheduler\"\n" +
      "    \"parameters/forceRefreshConfigTags\" : \"capacity-scheduler\"\n" +
      "  },\n" +
      "  \"Requests/resource_filters\": [{\n" +
      "    \"service_name\" : \"YARN\",\n" +
      "    \"component_name\" : \"RESOURCEMANAGER\",\n" +
      "    \"hosts\" : \"%s\"\n" +
      "  }]\n" +
      "}";
  private final String restartRMRequestData = "{\"RequestInfo\": {\n" +
      "    \"command\":\"RESTART\",\n" +
      "    \"context\":\"Restart ResourceManager\",\n" +
      "    \"operation_level\": {\n" +
      "        \"level\":\"HOST_COMPONENT\",\n" +
      "        \"cluster_name\":\"MyCluster\",\n" +
      "        \"host_name\":\"%s\",\n" +
      "        \"service_name\":\"YARN\",\n" +
      "        \"hostcomponent_name\":\"RESOURCEMANAGER\"\n" +
      "        }\n" +
      "    },\n" +
      "    \"Requests/resource_filters\": [\n" +
      "        {\n" +
      "            \"service_name\":\"YARN\",\n" +
      "            \"component_name\":\"RESOURCEMANAGER\",\n" +
      "            \"hosts\":\"%s\"\n" +
      "        }\n" +
      "    ]\n" +
      "}\n";

  /**
   * Constructor
   * @param context View Context instance
   */
  public ConfigurationService(ViewContext context) {
    this.context = context;

    proxy = new Proxy(context.getURLStreamProvider());
    proxy.setUseAuthorization(true);
    proxy.setUsername(context.getProperties().get("ambari.server.username"));
    proxy.setPassword(context.getProperties().get("ambari.server.password"));

    HashMap<String, String> customHeaders = new HashMap<String, String>();
    customHeaders.put("X-Requested-By", "capacity-scheduler-app");
    proxy.setCustomHeaders(customHeaders);

    baseUrl = context.getProperties().get("ambari.server.url");
  }

  // ================================================================================
  // Configuration Reading
  // ================================================================================

  private final String versionTagUrl = "%s?fields=Clusters/desired_configs/capacity-scheduler";
  private final String configurationUrl = "%%s/configurations?type=capacity-scheduler&tag=%s";
  private final String rmHostUrl = "%s/services/YARN/components/RESOURCEMANAGER?fields=host_components/host_name";

  /**
   * Get capacity scheduler configuration
   * @return scheduler configuration
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response readConfiguration() {
    Response response = null;
    try {
      validateConfig();
      String versionTag = getVersionTag();
      JSONObject configurations = getConfigurationFromAmbari(versionTag);
      response = Response.ok(configurations).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }

    return response;
  }

  private void validateConfig() {
    String hostname = context.getProperties().get("ambari.server.url");
    if (hostname == null)
      throw new MisconfigurationFormattedException("ambari.server.url");

    String username = context.getProperties().get("ambari.server.username");
    if (username == null)
      throw new MisconfigurationFormattedException("ambari.server.username");

    String password = context.getProperties().get("ambari.server.password");
    if (password == null)
      throw new MisconfigurationFormattedException("ambari.server.password");
  }

  private JSONObject getConfigurationFromAmbari(String versionTag) {
    String urlTemplate = String.format(configurationUrl, versionTag);
    String url = String.format(urlTemplate, baseUrl);
    return proxy.request(url).get().asJSON();
  }

  private String getVersionTag() {
    JSONObject json = getDesiredConfigs();
    JSONObject clusters = (JSONObject) json.get("Clusters");
    JSONObject configs = (JSONObject) clusters.get("desired_configs");
    JSONObject scheduler = (JSONObject) configs.get("capacity-scheduler");
    return (String) scheduler.get("tag");
  }

  private String getClusterName() {
    JSONObject json = getDesiredConfigs();
    JSONObject clusters = (JSONObject) json.get("Clusters");
    return (String) clusters.get("cluster_name");
  }

  private JSONObject getDesiredConfigs() {
    String url = String.format(versionTagUrl, baseUrl);
    return proxy.request(url).get().asJSON();
  }

  // ================================================================================
  // Configuration Writing
  // ================================================================================
  /**
   * Set capacity scheduler configuration
   * @return http response
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response writeConfiguration(JSONObject request) {
    try {
      validateConfig();

      proxy.request(baseUrl).
            setData(makeConfigUpdateData(request)).
            put();

    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }

    return readConfiguration();
  }

  /**
   * Set capacity scheduler configuration and refresh RM
   * @return http response
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/saveAndRefresh")
  public Response writeAndRefreshConfiguration(JSONObject request) {
    try {

      writeConfiguration(request);

      String rmHost = getRMHost();
      JSONObject data = (JSONObject) JSONValue.parse(String.format(refreshRMRequestData, rmHost));
      proxy.request(baseUrl + "/requests/").
          setData(data).
          post();

    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
    return readConfiguration();
  }

  /**
   * Set capacity scheduler configuration and restart RM
   * @return http response
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/saveAndRestart")
  public Response writeAndRestartConfiguration(JSONObject request) {
    try {

      writeConfiguration(request);

      String rmHost = getRMHost();
      JSONObject data = (JSONObject) JSONValue.parse(String.format(restartRMRequestData, rmHost, rmHost));
      proxy.request(baseUrl + "/requests/").
          setData(data).
          post();

    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
    return readConfiguration();
  }

  private String getRMHost() {
    String rmHost = null;
    JSONObject rmData = proxy.request(String.format(rmHostUrl, baseUrl)).get().asJSON();
    JSONArray components = (JSONArray) rmData.get("host_components");
    for(Object component : components) {
      JSONObject roles = (JSONObject) ((JSONObject) component).get("HostRoles");
      if (roles.get("component_name").equals("RESOURCEMANAGER")) {
        rmHost = (String) roles.get("host_name");
        break;
      }
    }
    if (rmHost == null)
      throw new ServiceFormattedException("Can't retrieve Resource Manager Host");
    return rmHost;
  }

  private JSONObject makeConfigUpdateData(JSONObject request) {
    JSONObject desiredConfigs = (JSONObject) request.clone();
    desiredConfigs.put("type", "capacity-scheduler");
    desiredConfigs.put("tag", "version" + String.valueOf(System.currentTimeMillis()));

    JSONObject clusters = new JSONObject();
    clusters.put("desired_configs", desiredConfigs);

    JSONObject data = new JSONObject();
    data.put("Clusters", clusters);
    return data;
  }

}
