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
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Help service
 */
public class ConfigurationService {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);
  private final Proxy proxy;
  private final String baseUrl;
  private final String serverUrl;

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
   * Constructor.
   
   * @param context     the ViewContext instance (may not be <code>null</code>)
   */
  public ConfigurationService(ViewContext context) {
    this.context = context;

    proxy = new Proxy(context.getURLStreamProvider());
    proxy.setUseAuthorization(true);
    proxy.setUsername(context.getProperties().get("ambari.server.username"));
    proxy.setPassword(context.getProperties().get("ambari.server.password"));

    HashMap<String, String> customHeaders = new HashMap<String, String>();
    customHeaders.put("X-Requested-By", "view-capacity-scheduler");
    proxy.setCustomHeaders(customHeaders);

    baseUrl = context.getProperties().get("ambari.server.url");
    
    URL url = null;
    try {
        url = new URL(baseUrl);
    } catch (MalformedURLException ex) {
      throw new ServiceFormattedException("Error in ambari.server.url property: " + ex.getMessage(), ex);
    }

    String clusterName = url.getFile(); // TODO: make it more robust
    serverUrl = baseUrl.substring(0, baseUrl.length() - clusterName.length());
  }

  // ================================================================================
  // Configuration Reading
  // ================================================================================

  private final String versionTagUrl = "%s?fields=Clusters/desired_configs/capacity-scheduler";
  private final String configurationUrl = "%s/configurations?type=capacity-scheduler";
  private final String configurationUrlByTag = "%%s/configurations?type=capacity-scheduler&tag=%s";
  private final String rmHostUrl = "%s/services/YARN/components/RESOURCEMANAGER?fields=host_components/host_name";

  private final String rmGetNodeLabelUrl = "http://%s:8088/ws/v1/cluster/get-node-labels";

  // ================================================================================
  // Privilege Reading
  // ================================================================================

  private final String clusterOperatorPrivilegeUrl = "%s?privileges/PrivilegeInfo/permission_name=CLUSTER.OPERATE&privileges/PrivilegeInfo/principal_name=%s";
  private final String ambariAdminPrivilegeUrl = "%s/api/v1/users/%s?Users/admin=true";

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
  @Path("all")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readAllConfigurations() {
    Response response = null;
    try {
      validateViewConfiguration();

      String url = String.format(configurationUrl, baseUrl);
      JSONObject configurations = proxy.request(url).get().asJSON();
      response = Response.ok(configurations).build();
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
      String nodeLabels = proxy.request(String.format(rmGetNodeLabelUrl, getRMHost())).get().asString();

      response = Response.ok(nodeLabels).build();
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
      String url = String.format(clusterOperatorPrivilegeUrl, baseUrl, context.getUsername()); 
      JSONObject json = proxy.request(url).get().asJSON();

      if (json == null || json.size() <= 0) {
        // user is not a CLUSTER.OPERATOR but might be an AMBARI.ADMIN
        url = String.format(ambariAdminPrivilegeUrl, serverUrl, context.getUsername()); 
        json = proxy.request(url).get().asJSON();
        if (json == null || json.size() <= 0)
            return false;
      }
      
    return  true;
  }
  
  /**
   * Validates the view configuration properties.
   *
   * @throws MisconfigurationFormattedException if one of the required view configuration properties are not set
   */
  private void validateViewConfiguration() {
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
    String urlTemplate = String.format(configurationUrlByTag, versionTag);
    String url = String.format(urlTemplate, baseUrl);
    return proxy.request(url).get().asJSON();
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
    String url = String.format(versionTagUrl, baseUrl);
    return proxy.request(url).get().asJSON();
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

      response = proxy.request(baseUrl).
                    setData(request).
                    put().asJSON();

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
    return readLatestConfiguration();
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

}
