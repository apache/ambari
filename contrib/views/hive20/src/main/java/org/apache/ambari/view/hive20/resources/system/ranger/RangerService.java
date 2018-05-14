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

package org.apache.ambari.view.hive20.resources.system.ranger;

import com.google.common.collect.Lists;
import org.apache.ambari.view.AmbariHttpException;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.utils.AuthorizationChecker;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class RangerService {

  public static final String RANGER_HIVE_AUTHORIZER_FACTORY_CLASSNAME = "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory";
  private static final String RANGER_CONFIG_URL = "/api/v1/clusters/%s/configurations/service_config_versions?service_name=RANGER&is_current=true";
  public static final String HIVESERVER2_SITE = "hiveserver2-site";
  public static final String AUTHORIZATION_MANAGER_KEY = "hive.security.authorization.manager";

  protected final Logger LOG = LoggerFactory.getLogger(getClass());

  private final AuthorizationChecker authChecker;
  private final ViewContext context;

  @Inject
  public RangerService(AuthorizationChecker authChecker, ViewContext context) {
    this.authChecker = authChecker;
    this.context = context;
  }

  public List<Policy> getPolicies(String database, String table) {


    if (context.getCluster() == null) {
      return getPoliciesFromNonAmbariCluster(database, table);
    } else {
      if (!authChecker.isOperator()) {
        LOG.error("User is not authorized to access the table authorization information");
        throw new RangerException("User " + context.getUsername() + " does not have privilege to access the table authorization information", "NOT_OPERATOR_OR_ADMIN", 400);
      }
      return getPoliciesFromAmbariCluster(database, table);
    }

  }

  private List<Policy> getPoliciesFromAmbariCluster(String database, String table) {

    if (!isHiveRangerPluginEnabled()) {
      LOG.error("Ranger authorization is not enabled for Hive");
      throw new RangerException("Ranger authorization is not enabled for Hive", "CONFIGURATION_ERROR", 500);
    }

    String rangerUrl = null;
    try {
      rangerUrl = getRangerUrlFromAmbari();
    } catch (AmbariHttpException e) {
      LOG.error("Failed to fetch Ranger URL from ambari. Exception: {}", e);
      throw new RangerException("Failed to fetch Ranger URL from Ambari", "AMBARI_FETCH_FAILED", 500, e);
    }
    if (StringUtils.isEmpty(rangerUrl)) {
      LOG.info("Ranger url is not configured for the instance");
      throw new RangerException("Ranger url is not configured in Ambari.", "CONFIGURATION_ERROR", 500);
    }

    return getPoliciesFromRanger(rangerUrl, database, table);
  }

  private List<Policy> getPoliciesFromNonAmbariCluster(String database, String table) {
    String rangerUrl = getRangerUrlFromConfig();
    if (StringUtils.isEmpty(rangerUrl)) {
      LOG.info("Ranger url is not configured for the instance");
      throw new RangerException("Ranger url is not configured in Ambari Instance.", "CONFIGURATION_ERROR", 500);
    }

    return getPoliciesFromRanger(rangerUrl, database, table);
  }

  private List<Policy> getPoliciesFromRanger(String rangerUrl, String database, String table) {
    RangerCred cred = getRangerCredFromConfig();
    if (!cred.isValid()) {
      LOG.info("Ranger username and password are not configured");
      throw new RangerException("Bad ranger username/password", "CONFIGURATION_ERROR", 500);
    }

    String rangerResponse = fetchResponseFromRanger(rangerUrl, cred.username, cred.password, database, table);
    if (StringUtils.isEmpty(rangerResponse)) {
      return Lists.newArrayList();
    }

    return parseResponse(rangerResponse);
  }

  private List<Policy> parseResponse(String rangerResponse) {
    Object parsedResult = JSONValue.parse(rangerResponse);
    if (parsedResult instanceof JSONObject) {
      JSONObject obj = (JSONObject) parsedResult;
      LOG.error("Bad response from Ranger: {}", rangerResponse);
      int status = ((Long) obj.get("statusCode")).intValue();
      status = status == Response.Status.UNAUTHORIZED.getStatusCode() ? Response.Status.FORBIDDEN.getStatusCode() : status;
      throw new RangerException((String) obj.get("msgDesc"), "RANGER_ERROR", status);
    }
    JSONArray jsonArray = (JSONArray) parsedResult;
    if (jsonArray.size() == 0) {
      return new ArrayList<>();
    }

    List<Policy> policies = new ArrayList<>();

    for (Object policy : jsonArray) {
      JSONObject policyJson = (JSONObject) policy;
      if ((Boolean) policyJson.get("isEnabled")) {
        policies.add(parsePolicy(policyJson));
      }
    }

    return policies;
  }

  private Policy parsePolicy(JSONObject policyJson) {
    String name = (String) policyJson.get("name");
    JSONArray policyItems = (JSONArray) policyJson.get("policyItems");
    Policy policy = new Policy(name);

    for (Object item : policyItems) {
      PolicyCondition condition = new PolicyCondition();
      JSONObject policyItem = (JSONObject) item;
      JSONArray usersJson = (JSONArray) policyItem.get("users");
      JSONArray groupsJson = (JSONArray) policyItem.get("groups");
      JSONArray accesses = (JSONArray) policyItem.get("accesses");


      for (Object accessJson : accesses) {
        JSONObject access = (JSONObject) accessJson;
        Boolean isAllowed = (Boolean) access.get("isAllowed");
        if (isAllowed) {
          condition.addAccess((String) access.get("type"));
        }
      }

      for (Object user : usersJson) {
        condition.addUser((String) user);
      }

      for (Object group : groupsJson) {
        condition.addGroup((String) group);
      }

      policy.addCondition(condition);
    }

    return policy;
  }

  private String fetchResponseFromRanger(String rangerUrl, String username, String password, String database, String table) {

    String serviceName = context.getProperties().get("hive.ranger.servicename");
    if (StringUtils.isEmpty(serviceName)) {
      LOG.error("Bad service name configured");
      throw new RangerException("Ranger service name is not configured in Ambari Instance.", "CONFIGURATION_ERROR", 500);
    }

    Map<String, String> headers = getRangerHeaders(username, password);
    StringBuilder urlBuilder = getRangerUrl(rangerUrl, database, table, serviceName);

    try {
      InputStream stream = context.getURLStreamProvider().readFrom(urlBuilder.toString(), "GET", (String) null, headers);
      if (stream == null) {
        LOG.error("Ranger returned an empty stream.");
        throw new RangerException("Ranger returned an empty stream.", "RANGER_ERROR", 500);
      }

      return IOUtils.toString(stream);
    } catch (IOException e) {
      LOG.error("Bad response from Ranger. Exception: {}", e);
      throw new RangerException("Bad response from Ranger", "RANGER_ERROR", 500, e);
    }
  }

  private StringBuilder getRangerUrl(String rangerUrl, String database, String table, String serviceName) {
    StringBuilder queryParams = new StringBuilder();
    if (!StringUtils.isEmpty(database)) {
      queryParams.append("resource:database=");
      queryParams.append(database);
      if (!StringUtils.isEmpty(table)) {
        queryParams.append("&");
      }
    }

    if (!StringUtils.isEmpty(table)) {
      queryParams.append("resource:table=");
      queryParams.append(table);
    }


    String queryParamString = queryParams.toString();

    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append(rangerUrl);
    urlBuilder.append("/service/public/v2/api/service/");
    urlBuilder.append(serviceName);
    urlBuilder.append("/policy");
    if (!StringUtils.isEmpty(queryParamString)) {
      urlBuilder.append("?");
      urlBuilder.append(queryParamString);
    }
    return urlBuilder;
  }

  private Map<String, String> getRangerHeaders(String username, String password) {
    String authString = username + ":" + password;
    byte[] authBytes = Base64.encodeBase64(authString.getBytes());
    String auth = new String(authBytes);
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Basic " + auth);
    return headers;
  }

  private RangerCred getRangerCredFromConfig() {
    return new RangerCred(context.getProperties().get("hive.ranger.username"),
      context.getProperties().get("hive.ranger.password"));
  }

  public String getRangerUrlFromAmbari() throws AmbariHttpException {

    AmbariApi ambariApi = new AmbariApi(context);
    String url = String.format(RANGER_CONFIG_URL, context.getCluster().getName());
    String config = ambariApi.readFromAmbari(url, "GET", null, null);
    JSONObject configJson = (JSONObject) JSONValue.parse(config);
    JSONArray itemsArray = (JSONArray) configJson.get("items");
    if (itemsArray.size() == 0) {
      LOG.error("Ranger service is not enabled in Ambari");
      throw new RangerException("Ranger service is not enabled in Ambari", "SERVICE_ERROR", 500);
    }
    JSONObject item = (JSONObject) itemsArray.get(0);
    JSONArray configurations = (JSONArray) item.get("configurations");
    for (Object configuration : configurations) {
      JSONObject configurationJson = (JSONObject) configuration;
      String type = (String) configurationJson.get("type");
      if (type.equalsIgnoreCase("admin-properties")) {
        JSONObject properties = (JSONObject) configurationJson.get("properties");
        return (String) properties.get("policymgr_external_url");
      }
    }
    return null;
  }

  public String getRangerUrlFromConfig() {
    return context.getProperties().get("hive.ranger.url");
  }

  /**
   * Check if the ranger plugin is enable for hive
   */
  private boolean isHiveRangerPluginEnabled() {
    String authManagerConf = context.getCluster().getConfigurationValue(HIVESERVER2_SITE, AUTHORIZATION_MANAGER_KEY);
    return !StringUtils.isEmpty(authManagerConf) && authManagerConf.equals(RANGER_HIVE_AUTHORIZER_FACTORY_CLASSNAME);
  }

  /**
   * POJO class to store the policy information from Ranger
   */
  public static class Policy {
    private String name;
    private List<PolicyCondition> conditions = new ArrayList<>();

    public Policy(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<PolicyCondition> getConditions() {
      return conditions;
    }

    public void addCondition(PolicyCondition condition) {
      this.conditions.add(condition);
    }
  }

  public static class PolicyCondition {
    private List<String> users = new ArrayList<>();
    private List<String> groups = new ArrayList<>();
    private List<String> accesses = new ArrayList<>();

    public List<String> getUsers() {
      return users;
    }

    public void setUsers(List<String> users) {
      this.users = users;
    }

    public List<String> getGroups() {
      return groups;
    }

    public void setGroups(List<String> groups) {
      this.groups = groups;
    }

    public List<String> getAccesses() {
      return accesses;
    }

    public void setAccesses(List<String> accesses) {
      this.accesses = accesses;
    }

    public void addUser(String user) {
      users.add(user);
    }

    public void addGroup(String group) {
      groups.add(group);
    }

    public void addAccess(String access) {
      accesses.add(access);
    }
  }

  /**
   * POJO class to store the username and password for ranger access
   */
  private class RangerCred {
    public String username;
    public String password;

    public RangerCred(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public boolean isValid() {
      return !(StringUtils.isEmpty(username) || StringUtils.isEmpty(password));
    }
  }
}
