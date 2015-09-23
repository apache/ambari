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
import org.apache.ambari.view.capacityscheduler.utils.ServiceFormattedException;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration service for accessing Ambari REST API for capacity scheduler config.
 *
 */
public class ConfigurationService {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);
  private final AmbariApi ambariApi;

  private ViewContext context;
  private static final String REFRESH_RM_REQUEST_DATA =
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
  private static final String RESTART_RM_REQUEST_DATA = "{\"RequestInfo\": {\n" +
      "    \"command\":\"RESTART\",\n" +
      "    \"context\":\"Restart ResourceManager\",\n" +
      "    \"operation_level\": {\n" +
      "        \"level\":\"HOST_COMPONENT\",\n" +
      "        \"cluster_name\":\"%s\",\n" +
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
   * Constructor.
   
   * @param context     the ViewContext instance (may not be <code>null</code>)
   */
  public ConfigurationService(ViewContext context) {
    this.context = context;
    this.ambariApi = new AmbariApi(context);
    this.ambariApi.setRequestedBy("view-capacity-scheduler");
  }

  // ================================================================================
  // Configuration Reading
  // ================================================================================

  private static final String VERSION_TAG_URL = "?fields=Clusters/desired_configs/capacity-scheduler";
  private static final String CONFIGURATION_URL = "configurations?type=capacity-scheduler";
  private static final String CONFIGURATION_URL_BY_TAG = "configurations?type=capacity-scheduler&tag=%s";

  private static final String RM_GET_NODE_LABEL_URL = "%s/ws/v1/cluster/get-node-labels";

  // ================================================================================
  // Privilege Reading
  // ================================================================================

  private static final String CLUSTER_OPERATOR_PRIVILEGE_URL = "?privileges/PrivilegeInfo/permission_name=CLUSTER.OPERATE&privileges/PrivilegeInfo/principal_name=%s";
  private static final String AMBARI_ADMIN_PRIVILEGE_URL = "/api/v1/users/%s?Users/admin=true";

  /**
   * Gets capacity scheduler configuration.
   *
   * @return scheduler configuration
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response readLatestConfiguration() {
    Response response = null;
    try {
      validateViewConfiguration();
      
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

  /**
   * Gets capacity scheduler configuration by all tags.
   *
   * @return scheduler configuration
   */
  @GET
  @Path("cluster")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readClusterInfo() {
    Response response = null;
    try {
      validateViewConfiguration();

      JSONObject configurations = readFromCluster("");
      response = Response.ok(configurations).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }

    return response;
  }

  /**
   * Gets capacity scheduler configuration by all tags.
   *
   * @return scheduler configuration
   */
  @GET
  @Path("all")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readAllConfigurations() {
    Response response = null;
    try {
      validateViewConfiguration();

      JSONObject responseJSON = readFromCluster(CONFIGURATION_URL);
      response = Response.ok( responseJSON ).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }

    return response;
  }

  /**
   * Gets capacity scheduler configuration by specific tag.
   *
   * @return scheduler configuration
   */
  @GET
  @Path("byTag/{tag}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readConfigurationByTag(@PathParam("tag") String tag) {
    Response response = null;
    try {
      validateViewConfiguration();

      JSONObject configurations = getConfigurationFromAmbari(tag);
      response = Response.ok(configurations).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }

    return response;
  }

  /**
   * Gets the privilege for this user.
   *
   * @return scheduler configuration
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/privilege")
  public Response getPrivilege() {
    Response response = null;

    try {
      boolean   operator = isOperator();

      response = Response.ok(operator).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }

    return response;
  }

  /**
   * Gets node labels from RM
   *
   * @return node labels
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/nodeLabels")
  public Response getNodeLabels() {
    Response response;

    try {
      String url = String.format(RM_GET_NODE_LABEL_URL, getRMUrl());

      InputStream rmResponse = context.getURLStreamProvider().readFrom(
          url, "GET", (String) null, new HashMap<String, String>());
      String nodeLabels = IOUtils.toString(rmResponse);

      response = Response.ok(nodeLabels).build();
    } catch (ConnectException ex) {
      throw new ServiceFormattedException("Connection to Resource Manager refused", ex);
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }

    return response;
  }


  /**
   * Checks if the user is an operator.
   *
   * @return    if <code>true</code>, the user is an operator; otherwise <code>false</code>
   */
  private   boolean isOperator() {
      validateViewConfiguration();
            
      // first check if the user is an CLUSTER.OPERATOR
      String url = String.format(CLUSTER_OPERATOR_PRIVILEGE_URL, context.getUsername());
      JSONObject json = readFromCluster(url);

      if (json == null || json.size() <= 0) {
        // user is not a CLUSTER.OPERATOR but might be an AMBARI.ADMIN
        url = String.format(AMBARI_ADMIN_PRIVILEGE_URL, context.getUsername());
        String response = ambariApi.readFromAmbari(url, "GET", null, null);
        if (response == null || response.isEmpty()) {
          return false;
        }
        json = getJsonObject(response);
        if (json == null || json.size() <= 0) {
          return false;
        }
      }
      
    return  true;
  }

  private JSONObject readFromCluster(String url) {
    String response = ambariApi.requestClusterAPI(url);
    if (response == null || response.isEmpty()) {
      return null;
    }

    return getJsonObject(response);
  }

  private JSONObject getJsonObject(String response) {
    if (response == null || response.isEmpty()) {
      return null;
    }
    JSONObject jsonObject = (JSONObject) JSONValue.parse(response);

    if (jsonObject.get("status") != null && (Long)jsonObject.get("status") >= 400L) {
      // Throw exception if HTTP status is not OK
      String message;
      if (jsonObject.containsKey("message")) {
        message = (String) jsonObject.get("message");
      } else {
        message = "without message";
      }
      throw new ServiceFormattedException("Proxy: Server returned error " + jsonObject.get("status") + " " +
          message + ". Check Capacity-Scheduler instance properties.");
    }
    return jsonObject;
  }

  /**
   * Validates the view configuration properties.
   *
   * @throws MisconfigurationFormattedException if one of the required view configuration properties are not set
   */
  private void validateViewConfiguration() {
    // check if we are cluster config'd, if so, just go
    if (ambariApi.isLocalCluster()) {
      return;
    }

    String hostname = context.getProperties().get("ambari.server.url");
    if (hostname == null) {
      throw new MisconfigurationFormattedException("ambari.server.url");
    }

    String username = context.getProperties().get("ambari.server.username");
    if (username == null) {
      throw new MisconfigurationFormattedException("ambari.server.username");
    }

    String password = context.getProperties().get("ambari.server.password");
    if (password == null) {
      throw new MisconfigurationFormattedException("ambari.server.password");
    }
  }

  private JSONObject getConfigurationFromAmbari(String versionTag) {
    String url = String.format(CONFIGURATION_URL_BY_TAG, versionTag);
    JSONObject responseJSON = readFromCluster(url);
    return  responseJSON;
  }

  /**
   * Gets the capacity scheduler version tag.
   *
   * @return    the capacity scheduler version tag
   */
  private String getVersionTag() {
    JSONObject json = getDesiredConfigs();
    JSONObject clusters = (JSONObject) json.get("Clusters");
    JSONObject configs = (JSONObject) clusters.get("desired_configs");
    JSONObject scheduler = (JSONObject) configs.get("capacity-scheduler");
    return (String) scheduler.get("tag");
  }

  /**
   * Gets the cluster name.
   *
   * @return    the cluster name
   */
  private String getClusterName() {
    JSONObject json = getDesiredConfigs();
    JSONObject clusters = (JSONObject) json.get("Clusters");
    return (String) clusters.get("cluster_name");
  }

  /**
   * Gets the desired config.
   *
   * @return  the desired config JSON object
   */
  private JSONObject getDesiredConfigs() {
    JSONObject response = readFromCluster(VERSION_TAG_URL);
    return response;
  }

  // ================================================================================
  // Configuration Writing
  // ================================================================================
  
  /**
   * Sets capacity scheduler configuration.
   *
   * @return the http response
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response writeConfiguration(JSONObject request) {
    JSONObject response;
    try {
      validateViewConfiguration();

      if (isOperator() == false) {
        return Response.status(401).build();
      }

      Map<String, String> headers = new HashMap<String, String>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      String responseString = ambariApi.requestClusterAPI("", "PUT", request.toJSONString(), headers);
      response = getJsonObject(responseString);

    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }

    return Response.ok(response).build();
  }

  /**
   * Sets capacity scheduler configuration and refresh ResourceManager.
   *
   * @return http response
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/saveAndRefresh")
  public Response writeAndRefreshConfiguration(JSONObject request) {
    try {

      if (isOperator() == false) {
        return Response.status(401).build();
      }

      String rmHosts = getRMHosts();
      JSONObject data = getJsonObject(String.format(REFRESH_RM_REQUEST_DATA, rmHosts));

      Map<String, String> headers = new HashMap<String, String>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      ambariApi.requestClusterAPI("requests/", "POST", data.toJSONString(), headers);

    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
    return readLatestConfiguration();
  }

  /**
   * Sets capacity scheduler configuration and restart ResourceManager.
   *
   * @return http response
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/saveAndRestart")
  public Response writeAndRestartConfiguration(JSONObject request) {
    try {

      if (isOperator() == false) {
        return Response.status(401).build();
      }

      String rmHosts = getRMHosts();
      JSONObject data = getJsonObject(String.format(RESTART_RM_REQUEST_DATA,
          ambariApi.getCluster().getName(), rmHosts, rmHosts));

      Map<String, String> headers = new HashMap<String, String>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      ambariApi.requestClusterAPI("requests/", "POST", data.toJSONString(), headers);

    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
    return readLatestConfiguration();
  }

  private String getRMUrl() {
    return ambariApi.getServices().getRMUrl();
  }

  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/getConfig")
  public Response getConfigurationValue(@QueryParam("siteName") String siteName,@QueryParam("configName") String configName){
    try{
      String configValue = ambariApi.getCluster().getConfigurationValue(siteName,configName);
      JSONObject res = new JSONObject();
      JSONArray arr = new JSONArray();
      JSONObject conf = new JSONObject();
      conf.put("siteName",siteName);
      conf.put("configName", configName);
      conf.put("configValue", configValue);
      arr.add(conf);
      res.put("configs" ,arr);
      return Response.ok(res).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  private String getRMHosts() {
    StringBuilder hosts = new StringBuilder();
    boolean first = true;
    for (String host : ambariApi.getHostsWithComponent("RESOURCEMANAGER")) {
      if (!first) {
        hosts.append(",");
      }
      hosts.append(host);
      first = false;
    }
    return hosts.toString();
  }
} // end ConfigurationService
