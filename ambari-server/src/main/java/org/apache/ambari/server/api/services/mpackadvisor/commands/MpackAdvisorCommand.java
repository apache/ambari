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

package org.apache.ambari.server.api.services.mpackadvisor.commands;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.LocalUriInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.RootServiceComponentConfiguration;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorException;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequestException;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorResponse;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRunner;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.MpackInstance;
import org.apache.ambari.server.topology.ServiceInstance;
import org.apache.ambari.server.utils.DateUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent for all commands.
 */
public abstract class MpackAdvisorCommand<T extends MpackAdvisorResponse> extends BaseService {

  /**
   * Type of response object provided by extending classes when
   *  is called.
   */
  private Class<T> type;

  private static final Logger LOG = LoggerFactory.getLogger(MpackAdvisorCommand.class);

  private static final String GET_HOSTS_INFO_URI = "/api/v1/hosts"
      + "?fields=Hosts/*&Hosts/host_name.in(%s)";

  private static final String GET_SERVICES_INFO_URI = "/api/v1/stacks/%s/versions/%s/"
      + "?fields=services/StackServices/service_name,services/StackServices/service_version"
      + ",services/components/StackServiceComponents,services/components/dependencies/Dependencies/scope"
      + ",services/components/dependencies/Dependencies/conditions,services/components/auto_deploy"
      + ",services/configurations/StackConfigurations/property_depends_on"
      + ",services/configurations/dependencies/StackConfigurationDependency/dependency_name"
      + ",services/configurations/dependencies/StackConfigurationDependency/dependency_type,services/configurations/StackConfigurations/type"
      + "&services/StackServices/service_name.in(%s)";

  private static final String SERVICES_PROPERTY = "services";
  private static final String SERVICES_COMPONENTS_PROPERTY = "components";
  private static final String STACK_SERVICES_PROPERTY = "StackServices";
  private static final String COMPONENT_INFO_PROPERTY = "StackServiceComponents";
  private static final String COMPONENT_MPACK_NAME_PROPERTY = "stack_name";
  private static final String COMPONENT_NAME_PROPERTY = "component_name";
  private static final String COMPONENT_HOSTNAMES_PROPERTY = "hostnames";
  private static final String CONFIGURATIONS_PROPERTY = "configurations";
  private static final String CHANGED_CONFIGURATIONS_PROPERTY = "changed-configurations";
  private static final String USER_CONTEXT_PROPERTY = "user-context";
  private static final String GPL_LICENSE_ACCEPTED = "gpl-license-accepted";
  private static final String AMBARI_SERVER_PROPERTIES_PROPERTY = "ambari-server-properties";
  private static final String AMBARI_SERVER_CONFIGURATIONS_PROPERTY = "ambari-server-configuration";

  private File mpackRecommendationsDir;
  private String recommendationsArtifactsLifetime;
  private ServiceInfo.ServiceAdvisorType serviceAdvisorType;

  private int requestId;
  private File mpackRequestDirectory;
  private MpackAdvisorRunner maRunner;

  protected ObjectMapper mapper;

  private final AmbariMetaInfo metaInfo;

  private final AmbariServerConfigurationHandler ambariServerConfigurationHandler;

  @SuppressWarnings("unchecked")
  public MpackAdvisorCommand(File mpackmpackRecommendationsDir, String recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType serviceAdvisorType, int requestId,
                             MpackAdvisorRunner maRunner, AmbariMetaInfo metaInfo, AmbariServerConfigurationHandler ambariServerConfigurationHandler) {
    this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
        .getActualTypeArguments()[0];

    this.mapper = new ObjectMapper();
    this.mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

    this.mpackRecommendationsDir = mpackmpackRecommendationsDir;
    this.recommendationsArtifactsLifetime = recommendationsArtifactsLifetime;
    this.serviceAdvisorType = serviceAdvisorType;
    this.requestId = requestId;
    this.maRunner = maRunner;
    this.metaInfo = metaInfo;
    this.ambariServerConfigurationHandler = ambariServerConfigurationHandler;
  }

  public ServiceInfo.ServiceAdvisorType getServiceAdvisorType() {
    return this.serviceAdvisorType;
  }

  protected abstract MpackAdvisorCommandType getCommandType();

  /**
   * Simple holder for 'hosts.json' and 'services.json' data.
   */
  public static class MpackAdvisorData {
    protected String hostsJSON;
    protected String servicesJSON;

    public MpackAdvisorData(String hostsJSON, String servicesJSON) {
      this.hostsJSON = hostsJSON;
      this.servicesJSON = servicesJSON;
    }
  }

  /**
   * Name with the result JSON, e.g. "component-layout.json" or "validations.json" .
   *
   * @return the file name
   */
  protected abstract String getResultFileName();

  protected abstract void validate(MpackAdvisorRequest request) throws MpackAdvisorException;

  protected ObjectNode adjust(String servicesJSON, MpackAdvisorRequest request) {
    try {
      ObjectNode root = (ObjectNode) this.mapper.readTree(servicesJSON);

      populateComponentHostsMap(root, request.getComponentHostsMap());
      populateServiceAdvisors(root);
      populateServicesConfigurations(root, request);
      populateClusterLevelConfigurations(root, request);
      populateAmbariServerInfo(root);
      populateAmbariConfiguration(root);
      // Get previous data, if any from data.servicesJSON and append to it.
      return root;
    } catch (Exception e) {
      // should not happen
      String message = "Error parsing created services object for Mpack Advisor Python Code. " + e.getMessage();
      LOG.warn(message, e);
      throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(message).build());
    }
  }

  /**
   * Retrieves the Ambari configuration if exists and adds it to services.json
   *
   * @param root The JSON document that will become service.json when passed to the stack advisor engine
   */
  void populateAmbariConfiguration(ObjectNode root) throws NoSuchResourceException {
    Map<String, RootServiceComponentConfiguration> config = ambariServerConfigurationHandler.getConfigurations(null);
    Map<String, Map<String,String>> result = new HashMap<>();
    for (String category : config.keySet()) {
      result.put(category, config.get(category).getProperties());
    }
    root.put(AMBARI_SERVER_CONFIGURATIONS_PROPERTY, mapper.valueToTree(result));
  }

  /* Reads cluster level configurations (eg: cluster-settings).
   */
  private void populateClusterLevelConfigurations(ObjectNode root, MpackAdvisorRequest request) {
    Map<String, Map<String, Map<String, String>>> configurations =
        request.getConfigurations();
    // Check if 'configurations' node exists. If not, create.
    ObjectNode configurationsNode = null;
    if (root.get(CONFIGURATIONS_PROPERTY) != null) {
      configurationsNode = (ObjectNode) root.get(CONFIGURATIONS_PROPERTY);
    } else {
      configurationsNode = root.putObject(CONFIGURATIONS_PROPERTY);
    }
    for (String siteName : configurations.keySet()) {
      ObjectNode siteNode = configurationsNode.putObject(siteName);

      Map<String, Map<String, String>> siteMap = configurations.get(siteName);
      for (String properties : siteMap.keySet()) {
        ObjectNode propertiesNode = siteNode.putObject(properties);

        Map<String, String> propertiesMap = siteMap.get(properties);
        for (String propertyName : propertiesMap.keySet()) {
          String propertyValue = propertiesMap.get(propertyName);
          propertiesNode.put(propertyName, propertyValue);
        }
      }
    }
  }

  private void populateServicesConfigurations(ObjectNode root, MpackAdvisorRequest request) {
    Collection<MpackInstance> mpackInstances = request.getMpackInstances();
    Iterator<MpackInstance> mpackInstanceItr = mpackInstances.iterator();
    ObjectNode configurationsNode = root.putObject(CONFIGURATIONS_PROPERTY);
    while (mpackInstanceItr.hasNext()) {
      MpackInstance mpackInstance = mpackInstanceItr.next();
      Collection<ServiceInstance> serviceInstances = mpackInstance.getServiceInstances();
      Iterator<ServiceInstance> serviceInstanceItr = serviceInstances.iterator();
      while (serviceInstanceItr.hasNext()) {
        ServiceInstance serviceInstance = serviceInstanceItr.next();
        Configuration configurations = serviceInstance.getConfiguration();
        // We have read configuration properties in attributes as it allows the following format:
        // eg: {configType -> {attributeName -> {propName, attributeValue}}}
        Map<String, Map<String, Map<String, String>>> configProperties = configurations.getAttributes();
        for (String siteName : configProperties.keySet()) {
          ObjectNode siteNode = configurationsNode.putObject(siteName);

          Map<String, Map<String, String>> siteMap = configProperties.get(siteName);
          for (String properties : siteMap.keySet()) {
            ObjectNode propertiesNode = siteNode.putObject(properties);

            Map<String, String> propertiesMap = siteMap.get(properties);
            for (String propertyName : propertiesMap.keySet()) {
              String propertyValue = propertiesMap.get(propertyName);
              propertiesNode.put(propertyName, propertyValue);
            }
          }
        }
      }
    }

    JsonNode changedConfigs = mapper.valueToTree(request.getChangedConfigurations());
    root.put(CHANGED_CONFIGURATIONS_PROPERTY, changedConfigs);
    JsonNode userContext = mapper.valueToTree(request.getUserContext());
    root.put(USER_CONTEXT_PROPERTY, userContext);
    root.put(GPL_LICENSE_ACCEPTED, request.getGplLicenseAccepted());
  }

  protected void populateAmbariServerInfo(ObjectNode root) {
    Map<String, String> serverProperties = metaInfo.getAmbariServerProperties();

    if (serverProperties != null && !serverProperties.isEmpty()) {
      JsonNode serverPropertiesNode = mapper.convertValue(serverProperties, JsonNode.class);
      root.put(AMBARI_SERVER_PROPERTIES_PROPERTY, serverPropertiesNode);
    }
  }

  private void populateComponentHostsMap(ObjectNode root, Map<String, Map<String, Set<String>>> mpacksToComponentsHostsMap) throws MpackAdvisorRequestException {
    ArrayNode services = (ArrayNode) root.get(SERVICES_PROPERTY);
    if (services == null) {
      String mpackName = null;
      String mpackversion = null;
      String href = root.get("href").getTextValue();
      if (href != null && !href.isEmpty()) {
        // Expected href is of form : /api/v1/stacks/HDPCORE/versions/1.0.0-b412/?fields=[fields ...]
        String[] splits = href.split("\\?");
        if (splits.length >= 1) {
          String[] newSplits = splits[0].split("/");
          if (newSplits.length == 7) {
            mpackName = newSplits[4];
            mpackversion = newSplits[6];
          }
        }
      }
      throw new MpackAdvisorRequestException("Attempt to read services information for passed-in Mpack : \""
          +mpackName+"\" and version : \""+mpackversion+"\" is NULL. Check passed-in Mpack Name, Version " +
          "and corresponding stack's existence in cluster.");
    }
    Iterator<JsonNode> servicesIter = services.getElements();

    while (servicesIter.hasNext()) {
      JsonNode service = servicesIter.next();
      ArrayNode components = (ArrayNode) service.get(SERVICES_COMPONENTS_PROPERTY);
      Iterator<JsonNode> componentsIter = components.getElements();

      while (componentsIter.hasNext()) {
        JsonNode component = componentsIter.next();
        ObjectNode componentInfo = (ObjectNode) component.get(COMPONENT_INFO_PROPERTY);
        String componentMpackName = componentInfo.get(COMPONENT_MPACK_NAME_PROPERTY).getTextValue();
        String componentName = componentInfo.get(COMPONENT_NAME_PROPERTY).getTextValue();

        Map<String, Set<String>> mpackComponentsHostMap = mpacksToComponentsHostsMap.get(componentMpackName);
        ArrayNode hostnames = componentInfo.putArray(COMPONENT_HOSTNAMES_PROPERTY);
        if (mpackComponentsHostMap != null) {
          Set<String> componentHosts = mpackComponentsHostMap.get(componentName);
          if (null != componentHosts) {
            for (String hostName : componentHosts) {
              hostnames.add(hostName);
            }
          }
        } else {
          LOG.info("Mpack Instance : "+componentMpackName+"'s componentsHost Map is empty.");
        }
      }
    }
  }

  private void populateServiceAdvisors(ObjectNode root) {
    ArrayNode services = (ArrayNode) root.get(SERVICES_PROPERTY);
    Iterator<JsonNode> servicesIter = services.getElements();

    ObjectNode version = (ObjectNode) root.get("Versions");
    String stackName = version.get("stack_name").asText();
    String stackVersion = version.get("stack_version").asText();

    while (servicesIter.hasNext()) {
      JsonNode service = servicesIter.next();
      ObjectNode serviceVersion = (ObjectNode) service.get(STACK_SERVICES_PROPERTY);
      String serviceName = serviceVersion.get("service_name").getTextValue();
      try {
        ServiceInfo serviceInfo = metaInfo.getService(stackName, stackVersion, serviceName);
        if (serviceInfo.getAdvisorFile() != null) {
          serviceVersion.put("advisor_name", serviceInfo.getAdvisorName());
          serviceVersion.put("advisor_path", serviceInfo.getAdvisorFile().getAbsolutePath());
        }
      }
      catch (Exception e) {
        LOG.error("Error adding service advisor information to services.json", e);
      }
    }
  }

  public synchronized T invoke(MpackAdvisorRequest request) throws MpackAdvisorException {
    validate(request);
    String hostsJSON = getHostsInformation(request);
    MpackAdvisorData updatedMpackServicesData = getServicesInformation(request, hostsJSON);

    try {
      createRequestDirectory();

      FileUtils.writeStringToFile(new File(mpackRequestDirectory, "hosts.json"), updatedMpackServicesData.hostsJSON);
      FileUtils.writeStringToFile(new File(mpackRequestDirectory, "services.json"), updatedMpackServicesData.servicesJSON);

      maRunner.runScript(serviceAdvisorType, getCommandType(), mpackRequestDirectory);
      String result = FileUtils.readFileToString(new File(mpackRequestDirectory, getResultFileName()));

      T response = this.mapper.readValue(result, this.type);
      return updateResponse(request, setRequestId(response));
    } catch (MpackAdvisorException ex) {
      throw ex;
    } catch (Exception e) {
      String message = "Error occured during mpack advisor command invocation: ";
      LOG.warn(message, e);
      throw new MpackAdvisorException(message + e.getMessage());
    }
  }

  protected abstract T updateResponse(MpackAdvisorRequest request, T response);

  private T setRequestId(T response) {
    response.setId(requestId);
    return response;
  }

  /**
   * Create request id directory for each call
   */
  private void createRequestDirectory() throws IOException {
    if (!mpackRecommendationsDir.exists()) {
      if (!mpackRecommendationsDir.mkdirs()) {
        throw new IOException("Cannot create " + mpackRecommendationsDir);
      }
    }

    cleanupRequestDirectory();

    mpackRequestDirectory = new File(mpackRecommendationsDir, Integer.toString(requestId));

    if (mpackRequestDirectory.exists()) {
      FileUtils.deleteDirectory(mpackRequestDirectory);
    }
    if (!mpackRequestDirectory.mkdirs()) {
      throw new IOException("Cannot create " + mpackRequestDirectory);
    }
  }

  /**
   * Deletes folders older than (now - recommendationsArtifactsLifetime)
   */
  private void cleanupRequestDirectory() throws IOException {
    final Date cutoffDate = DateUtils.getDateSpecifiedTimeAgo(recommendationsArtifactsLifetime); // subdirectories older than this date will be deleted

    String[] oldDirectories = mpackRecommendationsDir.list(new FilenameFilter() {
      @Override
      public boolean accept(File current, String name) {
        File file = new File(current, name);
        return file.isDirectory() && !FileUtils.isFileNewer(file, cutoffDate);
      }
    });

    if(oldDirectories.length > 0) {
      LOG.info(String.format("Deleting old directories %s from %s", StringUtils.join(oldDirectories, ", "), mpackRecommendationsDir));
    }

    for(String oldDirectory:oldDirectories) {
      FileUtils.deleteQuietly(new File(mpackRecommendationsDir, oldDirectory));
    }
  }

  String getHostsInformation(MpackAdvisorRequest request) throws MpackAdvisorException {
    String hostsURI = String.format(GET_HOSTS_INFO_URI, request.getHostsCommaSeparated());

    Response response = handleRequest(null, null, new LocalUriInfo(hostsURI), Request.Type.GET,
        createHostResource());

    if (response.getStatus() != Status.OK.getStatusCode()) {
      String message = String.format(
          "Error occured during hosts information retrieving, status=%s, response=%s",
          response.getStatus(), (String) response.getEntity());
      LOG.warn(message);
      throw new MpackAdvisorException(message);
    }

    String hostsJSON = (String) response.getEntity();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Hosts information: {}", hostsJSON);
    }

    Collection<String> unregistered = getUnregisteredHosts(hostsJSON, request.getHosts());
    if (unregistered.size() > 0) {
      String message = String.format("There are unregistered hosts in the request, %s",
          Arrays.toString(unregistered.toArray()));
      LOG.warn(message);
      throw new MpackAdvisorException(message);
    }

    return hostsJSON;
  }

  @SuppressWarnings("unchecked")
  private Collection<String> getUnregisteredHosts(String hostsJSON, List<String> hosts)
      throws MpackAdvisorException {
    ObjectMapper mapper = new ObjectMapper();
    List<String> registeredHosts = new ArrayList<>();

    try {
      JsonNode root = mapper.readTree(hostsJSON);
      Iterator<JsonNode> iterator = root.get("items").getElements();
      while (iterator.hasNext()) {
        JsonNode next = iterator.next();
        String hostName = next.get("Hosts").get("host_name").getTextValue();
        registeredHosts.add(hostName);
      }

      return CollectionUtils.subtract(hosts, registeredHosts);
    } catch (Exception e) {
      throw new MpackAdvisorException("Error occured during calculating unregistered hosts", e);
    }
  }

  /*
   Gets the updated Services information across Mpacks passed-in.
   It does the following:
    - Iterates through all the passed-in Mpacks instances (eg: HDPCORE-MKG), fetches their Stack Information.
    - For each Mpack instance Type (eg: HDPCORE), makes call to its Stack API, and gathers the stack's static information (dependencies etc.)
    - Calls adjust() function, which updates the services related information.
    - Maintains a global ArrayNode across Mpack Instances 'updatedServicesAcrossMpacks' to keep the updated Service Array Nodes across Mpacks.
      (Reason : We want to write services.json with 'services' element, having array of service instances across Mpacks together, to maintain
      Python Stack Advisor compatibility.)
   */
  MpackAdvisorData getServicesInformation(MpackAdvisorRequest request, String hostsJSON) throws MpackAdvisorException {

    Collection<MpackInstance> mpackInstances = request.getMpackInstances();
    String servicesInfoFromMpack = null;
    MpackAdvisorData updatedMpackAdvisorData = new MpackAdvisorData(hostsJSON, null);

    // Maintains the updated Services Across mpacks as we iterate.
    ArrayNode updatedServicesAcrossMpacks = mapper.createArrayNode();

    // Maintains the updated Services in a given mpack, as we iterate which we add to 'updatedServicesAcrossMpacks'.
    ObjectNode updatedServicesInfoForMpack = mapper.createObjectNode();

    for (MpackInstance mpackInstance : mpackInstances) {
      String stackType = mpackInstance.getMpackType();
      String stackVersion = mpackInstance.getMpackVersion();
      String servicesURI = String.format(GET_SERVICES_INFO_URI, stackType, stackVersion,
          request.getMpackServiceInstanceTypeCommaSeparated(mpackInstance));

      Response response = handleRequest(null, null, new LocalUriInfo(servicesURI),
          Request.Type.GET, createStackVersionResource(stackType, stackVersion));

      servicesInfoFromMpack = (String) response.getEntity();
      if (response.getStatus() != Status.OK.getStatusCode()) {
        String message = String.format(
            "Error occured during services information retrieving, status=%s, response=%s",
            response.getStatus(), (String) response.getEntity());
        LOG.warn(message);
        throw new MpackAdvisorException(message);
      }

      updatedServicesInfoForMpack = adjust(servicesInfoFromMpack, request);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Services information: {}", servicesInfoFromMpack);
      }

      for (Iterator<JsonNode> svcInstanceItr = updatedServicesInfoForMpack.get("services").getElements(); svcInstanceItr.hasNext(); ) {
        updatedServicesAcrossMpacks.add(svcInstanceItr.next());
      }
    }

    // Preparing the return object as follows:
    updatedServicesInfoForMpack.put("services", updatedServicesAcrossMpacks);
    updatedServicesInfoForMpack.remove("Versions");
    updatedServicesInfoForMpack.remove("href");

    try {
      updatedMpackAdvisorData.servicesJSON = mapper.writeValueAsString(updatedServicesInfoForMpack);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return updatedMpackAdvisorData;
  }

  private ResourceInstance createHostResource() {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    return createResource(Resource.Type.Host, mapIds);
  }

  private ResourceInstance createConfigResource() {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.RootService, RootService.AMBARI.name());
    mapIds.put(Resource.Type.RootServiceComponent, RootComponent.AMBARI_SERVER.name());

    return createResource(Resource.Type.RootServiceComponentConfiguration, mapIds);
  }

  private ResourceInstance createStackVersionResource(String stackName, String stackVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);

    return createResource(Resource.Type.StackVersion, mapIds);
  }

}
