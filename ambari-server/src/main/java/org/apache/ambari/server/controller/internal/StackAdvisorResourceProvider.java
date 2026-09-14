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

package org.apache.ambari.server.controller.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestBuilder;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyStackAdvisorPlanner;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyStackAdvisorPlanner.PlanRequest;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyStackAdvisorPlanner.TrustedPlan;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.ChangedConfigInfo;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Abstract superclass for recommendations and validations.
 */
public abstract class StackAdvisorResourceProvider extends ReadOnlyResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(StackAdvisorResourceProvider.class);

  protected static final String STACK_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Versions",
      "stack_name");
  protected static final String STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "Versions", "stack_version");


  private static final String CLUSTER_ID_PROPERTY = "clusterId";
  private static final String SERVICE_NAME_PROPERTY = "serviceName";
  private static final String AUTO_COMPLETE_PROPERTY = "autoComplete";
  private static final String CONFIGS_RESPONSE_PROPERTY = "configsResponse";
  private static final String CONFIG_GROUPS_GROUP_ID_PROPERTY = "group_id";
  private static final String HOST_PROPERTY = "hosts";
  private static final String SERVICES_PROPERTY = "services";

  private static final String CHANGED_CONFIGURATIONS_PROPERTY = "changed_configurations";
  private static final String OPERATION_PROPERTY = "operation";
  private static final String OPERATION_DETAILS_PROPERTY = "operation_details";


  private static final String BLUEPRINT_HOST_GROUPS_PROPERTY = "recommendations/blueprint/host_groups";
  private static final String BINDING_HOST_GROUPS_PROPERTY = "recommendations/blueprint_cluster_binding/host_groups";

  private static final String BLUEPRINT_HOST_GROUPS_NAME_PROPERTY = "name";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY = "components";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY = "name";

  private static final String BINDING_HOST_GROUPS_NAME_PROPERTY = "name";
  private static final String BINDING_HOST_GROUPS_HOSTS_PROPERTY = "hosts";
  private static final String BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY = "fqdn";

  private static final String CONFIG_GROUPS_PROPERTY = "recommendations/config_groups";
  private static final String CONFIG_GROUPS_CONFIGURATIONS_PROPERTY = "configurations";
  private static final String CONFIG_GROUPS_HOSTS_PROPERTY = "hosts";

  protected static StackAdvisorHelper saHelper;
  private static Configuration configuration;
  private static Clusters clusters;
  private static AmbariMetaInfo ambariMetaInfo;
  protected static final String USER_CONTEXT_OPERATION_PROPERTY = "user_context/operation";
  protected static final String USER_CONTEXT_OPERATION_DETAILS_PROPERTY = "user_context/operation_details";
  protected static final String MANAGED_DEPENDENCY_PLAN_PROPERTY = "managed_dependency_plan";
  private static ManagedDependencyStackAdvisorPlanner managedDependencyPlanner;

  @Inject
  public static void init(StackAdvisorHelper instance, Configuration serverConfig, Clusters clusters,
                          AmbariMetaInfo ambariMetaInfo) {
    saHelper = instance;
    configuration = serverConfig;
    StackAdvisorResourceProvider.clusters = clusters;
    StackAdvisorResourceProvider.ambariMetaInfo = ambariMetaInfo;
  }

  public static void init(StackAdvisorHelper instance, Configuration serverConfig, Clusters clusters,
      AmbariMetaInfo ambariMetaInfo, ManagedDependencyStackAdvisorPlanner dependencyPlanner) {
    init(instance, serverConfig, clusters, ambariMetaInfo);
    managedDependencyPlanner = dependencyPlanner;
  }

  protected StackAdvisorResourceProvider(Resource.Type type, Set<String> propertyIds, Map<Type, String> keyPropertyIds,
                                         AmbariManagementController managementController) {
    super(type, propertyIds, keyPropertyIds, managementController);
  }

  protected abstract String getRequestTypePropertyId();

  @SuppressWarnings("unchecked")
  protected StackAdvisorRequest prepareStackAdvisorRequest(Request request) {
    try {
      Map<String, Object> requestProperties = requestProperties(request);
      String clusterIdProperty = (String) getRequestProperty(request, CLUSTER_ID_PROPERTY);
      Long clusterId = clusterIdProperty == null ? null : Long.valueOf(clusterIdProperty);

      String serviceName = (String) getRequestProperty(request, SERVICE_NAME_PROPERTY);

      String autoCompleteProperty = (String) getRequestProperty(request, AUTO_COMPLETE_PROPERTY);
      Boolean autoComplete = autoCompleteProperty == null ? false : Boolean.valueOf(autoCompleteProperty);

      String stackName = (String) getRequestProperty(request, STACK_NAME_PROPERTY_ID);
      String stackVersion = (String) getRequestProperty(request, STACK_VERSION_PROPERTY_ID);
      StackAdvisorRequestType requestType = StackAdvisorRequestType
          .fromString((String) getRequestProperty(request, getRequestTypePropertyId()));

      PlanRequest managedPlanRequest = ManagedDependencyStackAdvisorPlanner.parse(requestProperties);
      Set<String> explicitHosts = collectExplicitHosts(requestProperties);
      Cluster targetCluster = authorizeTarget(clusterId, managedPlanRequest, explicitHosts);
      if (targetCluster != null) {
        clusterId = targetCluster.getClusterId();
        if (managedPlanRequest != null) {
          StackId targetStack = targetCluster.getDesiredStackVersion();
          if (targetStack == null || !targetStack.getStackName().equals(stackName)
              || !targetStack.getStackVersion().equals(stackVersion)) {
            throw new ManagedDependencyIntegrationException(409, "ADVISOR_TARGET_STACK_MISMATCH",
                "The advisor stack does not match the selected cluster");
          }
        }
      }
      authorizeHosts(explicitHosts, targetCluster);

      List<String> hosts;
      List<String> services;
      Map<String, Set<String>> hgComponentsMap;
      Map<String, Set<String>> hgHostsMap;
      Map<String, Set<String>> componentHostsMap;
      Map<String, Map<String, Map<String, String>>> configurations;
      Set<RecommendationResponse.ConfigGroup> configGroups;

      // In auto complete case all required fields will be filled will cluster current info
      if (autoComplete) {
        if (clusterId == null || serviceName == null) {
          throw new Exception(
              String.format("Incomplete request, clusterId and/or serviceName are not valid, clusterId=%s, serviceName=%s",
                  clusterId, serviceName));
        }
        Cluster cluster = targetCluster;
        authorizeClusterConfigurationView(cluster);
        List<Host> hostObjects = new ArrayList<>(cluster.getHosts());
        Map<String, Service> serviceObjects = cluster.getServices();

        hosts = hostObjects.stream().map(h -> h.getHostName()).collect(Collectors.toList());
        services = new ArrayList<>(serviceObjects.keySet());
        hgComponentsMap = calculateHostGroupComponentsMap(cluster);
        hgHostsMap = calculateHostGroupHostsMap(cluster);
        componentHostsMap = calculateComponentHostsMap(cluster);
        configurations = calculateConfigurations(cluster, serviceName);

        configGroups = calculateConfigGroups(cluster, request);
      } else {
        /*
       * ClassCastException will occur if hosts or services are empty in the
       * request.
       *
       * @see JsonRequestBodyParser for arrays parsing
       */
        Object hostsObject = getRequestProperty(request, HOST_PROPERTY);
        if (hostsObject instanceof LinkedHashSet) {
          if (((LinkedHashSet)hostsObject).isEmpty()) {
            throw new Exception("Empty host list passed to recommendation service");
          }
        }
        hosts = (List<String>) hostsObject;

        Object servicesObject = getRequestProperty(request, SERVICES_PROPERTY);
        if (servicesObject instanceof LinkedHashSet) {
          if (((LinkedHashSet)servicesObject).isEmpty()) {
            throw new Exception("Empty service list passed to recommendation service");
          }
        }
        services = (List<String>) servicesObject;

        hgComponentsMap = calculateHostGroupComponentsMap(request);
        hgHostsMap = calculateHostGroupHostsMap(request);
        componentHostsMap = calculateComponentHostsMap(hgComponentsMap, hgHostsMap);
        configurations = calculateConfigurations(request);
        configGroups = calculateConfigGroups(request);
      }
      authorizeHosts(new LinkedHashSet<>(hosts), targetCluster);
      Map<String, String> userContext = readUserContext(request);
      Boolean gplLicenseAccepted = configuration.getGplLicenseAccepted();
      List<ChangedConfigInfo> changedConfigurations =
        requestType == StackAdvisorRequestType.CONFIGURATION_DEPENDENCIES ?
          calculateChangedConfigurations(request) : Collections.emptyList();

      String configsResponseProperty = (String) getRequestProperty(request, CONFIGS_RESPONSE_PROPERTY);
      Boolean configsResponse = configsResponseProperty == null ? false : Boolean.valueOf(configsResponseProperty);

      TrustedPlan managedDependencyPlan = null;
      if (managedPlanRequest != null) {
        if (managedDependencyPlanner == null) {
          throw new ManagedDependencyIntegrationException(503, "ADVISOR_PLAN_UNAVAILABLE",
              "Managed dependency planning is temporarily unavailable");
        }
        managedDependencyPlan = managedDependencyPlanner.authorize(managedPlanRequest, clusterId,
            stackName, stackVersion, services);
      }

      return StackAdvisorRequestBuilder.
        forStack(stackName, stackVersion).ofType(requestType).forHosts(hosts).
        forServices(services).forHostComponents(hgComponentsMap).
        forHostsGroupBindings(hgHostsMap).
        withComponentHostsMap(componentHostsMap).
        withConfigurations(configurations).
        withConfigGroups(configGroups).
        withChangedConfigurations(changedConfigurations).
        withUserContext(userContext).
        withGPLLicenseAccepted(gplLicenseAccepted).
        withClusterId(clusterId).
        withServiceName(serviceName).
        withConfigsResponse(configsResponse).
        withManagedDependencyPlan(managedDependencyPlan).build();
    } catch (WebApplicationException e) {
      throw e;
    } catch (ManagedDependencyIntegrationException e) {
      throw advisorError(e.getStatus(), e.getCode(), e.getMessage());
    } catch (AuthorizationException e) {
      throw advisorError(Status.FORBIDDEN.getStatusCode(), "ADVISOR_AUTHORIZATION_FAILED",
          "The authenticated user is not authorized for this advisor request");
    } catch (Exception e) {
      LOG.warn("Stack advisor request preparation failed: {}", e.getClass().getName());
      throw advisorError(Status.BAD_REQUEST.getStatusCode(), "INVALID_STACK_ADVISOR_REQUEST",
          "The stack advisor request body is invalid");
    }
  }

  private Cluster authorizeTarget(Long clusterId, PlanRequest managedPlan,
      Set<String> explicitHosts) throws AmbariException, AuthorizationException {
    Long plannedClusterId = managedPlan == null ? null : managedPlan.consumer().clusterId();
    if (managedPlan != null && managedPlan.consumer().scope()
        == ManagedDependencyStackAdvisorPlanner.ConsumerScope.DRAFT) {
      if (clusterId != null) {
        throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_CONSUMER_MISMATCH",
            "A dependency draft cannot target an existing cluster");
      }
      AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null,
          Set.of(RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS));
      return null;
    }
    if (plannedClusterId != null && !plannedClusterId.equals(clusterId)) {
      throw new ManagedDependencyIntegrationException(409, "DEPENDENCY_CONSUMER_MISMATCH",
          "The advisor target does not match the managed dependency consumer");
    }

    if (clusterId != null) {
      Cluster cluster;
      try {
        cluster = clusters.getCluster(clusterId);
      } catch (ClusterNotFoundException e) {
        throw new ManagedDependencyIntegrationException(404, "ADVISOR_TARGET_NOT_FOUND",
            "The selected advisor cluster was not found");
      }
      authorizeClusterView(cluster);
      return cluster;
    }

    Set<Cluster> mappedClusters = new LinkedHashSet<>();
    for (String hostName : explicitHosts) {
      if (!clusters.hostExists(hostName)) {
        continue;
      }
      Set<Cluster> hostClusters = clusters.getClustersForHost(hostName);
      for (Cluster cluster : hostClusters) {
        authorizeClusterView(cluster);
        mappedClusters.add(cluster);
      }
    }
    if (mappedClusters.size() > 1) {
      throw new AuthorizationException("Advisor hosts do not share one authorized cluster");
    }
    if (mappedClusters.size() == 1) {
      return mappedClusters.iterator().next();
    }
    AuthorizationHelper.verifyAuthorization(ResourceType.AMBARI, null,
        Set.of(RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS));
    return null;
  }

  private void authorizeClusterView(Cluster cluster) throws AuthorizationException {
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(),
        RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE);
  }

  private void authorizeClusterConfigurationView(Cluster cluster) throws AuthorizationException {
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(),
        Set.of(RoleAuthorization.CLUSTER_VIEW_CONFIGS));
  }

  private void authorizeHosts(Set<String> hostNames, Cluster targetCluster)
      throws AmbariException, AuthorizationException {
    boolean mayAddHosts = false;
    for (String hostName : hostNames) {
      if (hostName == null || hostName.isBlank() || !clusters.hostExists(hostName)) {
        throw new ManagedDependencyIntegrationException(400, "INVALID_ADVISOR_HOST",
            "The advisor request contains an unknown host");
      }
      Set<Cluster> mappedClusters = clusters.getClustersForHost(hostName);
      if (targetCluster == null) {
        if (!mappedClusters.isEmpty()) {
          throw new AuthorizationException("A creation advisor request cannot use an assigned host");
        }
      } else if (mappedClusters.isEmpty()) {
        if (!mayAddHosts) {
          AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER,
              targetCluster.getResourceId(), Set.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
          mayAddHosts = true;
        }
      } else if (mappedClusters.size() != 1
          || mappedClusters.iterator().next().getClusterId() != targetCluster.getClusterId()) {
        throw new AuthorizationException("An advisor host belongs to another cluster");
      }
    }
  }

  private Set<String> collectExplicitHosts(Map<String, Object> properties) {
    Set<String> result = new LinkedHashSet<>();
    addStringCollection(result, properties.get(HOST_PROPERTY));
    addNestedHosts(result, properties.get(BINDING_HOST_GROUPS_PROPERTY),
        BINDING_HOST_GROUPS_HOSTS_PROPERTY, BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY);
    Object configGroups = properties.get(CONFIG_GROUPS_PROPERTY);
    if (configGroups != null) {
      if (!(configGroups instanceof Collection<?> groups)) {
        throw new IllegalArgumentException("config_groups must be an array");
      }
      for (Object rawGroup : groups) {
        if (!(rawGroup instanceof Map<?, ?> group)) {
          throw new IllegalArgumentException("config_groups entries must be objects");
        }
        addStringCollection(result, group.get(CONFIG_GROUPS_HOSTS_PROPERTY));
      }
    }
    return result;
  }

  private void addNestedHosts(Set<String> result, Object rawGroups, String hostsField,
      String hostNameField) {
    if (rawGroups == null) {
      return;
    }
    if (!(rawGroups instanceof Collection<?> groups)) {
      throw new IllegalArgumentException("host groups must be an array");
    }
    for (Object rawGroup : groups) {
      if (!(rawGroup instanceof Map<?, ?> group)) {
        throw new IllegalArgumentException("host group entries must be objects");
      }
      Object rawHosts = group.get(hostsField);
      if (rawHosts == null) {
        continue;
      }
      if (!(rawHosts instanceof Collection<?> hosts)) {
        throw new IllegalArgumentException("host group hosts must be an array");
      }
      for (Object rawHost : hosts) {
        if (!(rawHost instanceof Map<?, ?> host)) {
          throw new IllegalArgumentException("host entries must be objects");
        }
        addHost(result, host.get(hostNameField));
      }
    }
  }

  private void addStringCollection(Set<String> result, Object rawValues) {
    if (rawValues == null) {
      return;
    }
    if (!(rawValues instanceof Collection<?> values)) {
      throw new IllegalArgumentException("hosts must be an array");
    }
    for (Object value : values) {
      addHost(result, value);
    }
  }

  private void addHost(Set<String> result, Object rawHost) {
    if (!(rawHost instanceof String host) || host.isBlank()) {
      throw new IllegalArgumentException("host names must be non-empty strings");
    }
    result.add(host);
  }

  private Map<String, Object> requestProperties(Request request) {
    Map<String, Object> properties = new HashMap<>();
    for (Map<String, Object> propertySet : request.getProperties()) {
      for (Map.Entry<String, Object> entry : propertySet.entrySet()) {
        Object previous = properties.putIfAbsent(entry.getKey(), entry.getValue());
        if (previous != null && !previous.equals(entry.getValue())) {
          throw new IllegalArgumentException("Duplicate stack advisor request property");
        }
      }
    }
    return properties;
  }

  private WebApplicationException advisorError(int status, String code, String message) {
    Response response = Response.status(status)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(Map.of("code", code, "message", message))
        .build();
    return new WebApplicationException(response);
  }

  /**
   * Will prepare host-group names to components names map from the
   * recommendation blueprint host groups.
   * 
   * @param request stack advisor request
   * @return host-group to components map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateHostGroupComponentsMap(Request request) {
    Set<Map<String, Object>> hostGroups = (Set<Map<String, Object>>) getRequestProperty(request,
        BLUEPRINT_HOST_GROUPS_PROPERTY);
    Map<String, Set<String>> map = new HashMap<>();
    if (hostGroups != null) {
      for (Map<String, Object> hostGroup : hostGroups) {
        String hostGroupName = (String) hostGroup.get(BLUEPRINT_HOST_GROUPS_NAME_PROPERTY);

        Set<Map<String, Object>> componentsSet = (Set<Map<String, Object>>) hostGroup
            .get(BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY);

        Set<String> components = new HashSet<>();
        for (Map<String, Object> component : componentsSet) {
          components.add((String) component.get(BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY));
        }

        map.put(hostGroupName, components);
      }
    }

    return map;
  }

  /**
   * Retrieves component names mapped by host groups, host name is used as host group identifier
   * @param cluster cluster for calculating components mapping by host groups
   * @return map "host group name" -> ["component name1", "component name 2", ...]
   */
  private Map<String, Set<String>> calculateHostGroupComponentsMap(Cluster cluster) {
    Map<String, Set<String>> map = new HashMap<>();
    List<Host> hosts = new ArrayList<>(cluster.getHosts());
    if (!hosts.isEmpty()) {
      for (Host host : hosts) {
        String hostGroupName = host.getHostName();

        Set<String> components = new HashSet<>();
        for (ServiceComponentHost sch : cluster.getServiceComponentHosts(host.getHostName())) {
          components.add(sch.getServiceComponentName());
        }
        map.put(hostGroupName, components);
      }
    }
    return map;
  }

  /**
   * Will prepare host-group names to hosts names map from the recommendation
   * binding host groups.
   * 
   * @param request stack advisor request
   * @return host-group to hosts map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateHostGroupHostsMap(Request request) {
    Set<Map<String, Object>> bindingHostGroups = (Set<Map<String, Object>>) getRequestProperty(
        request, BINDING_HOST_GROUPS_PROPERTY);
    Map<String, Set<String>> map = new HashMap<>();
    if (bindingHostGroups != null) {
      for (Map<String, Object> hostGroup : bindingHostGroups) {
        String hostGroupName = (String) hostGroup.get(BINDING_HOST_GROUPS_NAME_PROPERTY);

        Set<Map<String, Object>> hostsSet = (Set<Map<String, Object>>) hostGroup
            .get(BINDING_HOST_GROUPS_HOSTS_PROPERTY);

        Set<String> hosts = new HashSet<>();
        for (Map<String, Object> host : hostsSet) {
          hosts.add((String) host.get(BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY));
        }

        map.put(hostGroupName, hosts);
      }
    }

    return map;
  }

  /**
   * Retrieves hosts names mapped by host groups, host name is used as host group identifier
   * @param cluster cluster for calculating hosts mapping by host groups
   * @return map "host group name" -> ["host name 1"]
   */
  private Map<String, Set<String>> calculateHostGroupHostsMap(Cluster cluster) {
    Map<String, Set<String>> map = new HashMap<>();

    List<Host> hosts = new ArrayList<>(cluster.getHosts());
    if (!hosts.isEmpty()) {
      for (Host host : hosts) {
        map.put(host.getHostName(), Collections.singleton(host.getHostName()));
      }
    }

    return map;
  }

  protected List<ChangedConfigInfo> calculateChangedConfigurations(Request request) {
    List<ChangedConfigInfo> configs =
      new LinkedList<>();
    HashSet<HashMap<String, String>> changedConfigs =
      (HashSet<HashMap<String, String>>) getRequestProperty(request, CHANGED_CONFIGURATIONS_PROPERTY);
    for (HashMap<String, String> props: changedConfigs) {
      configs.add(new ChangedConfigInfo(props.get("type"), props.get("name"), props.get("old_value")));
    }

    return configs;
  }

  protected Set<RecommendationResponse.ConfigGroup> calculateConfigGroups(Request request) {

    Set<RecommendationResponse.ConfigGroup> configGroups =
      new HashSet<>();

    Set<HashMap<String, Object>> configGroupsProperties =
      (HashSet<HashMap<String, Object>>) getRequestProperty(request, CONFIG_GROUPS_PROPERTY);
    if (configGroupsProperties != null) {
      for (HashMap<String, Object> props : configGroupsProperties) {
        RecommendationResponse.ConfigGroup configGroup = new RecommendationResponse.ConfigGroup();
        configGroup.setHosts((List<String>) props.get(CONFIG_GROUPS_HOSTS_PROPERTY));

        for (Map<String, String> property : (Set<Map<String, String>>) props.get(CONFIG_GROUPS_CONFIGURATIONS_PROPERTY)) {
          for (Map.Entry<String, String> entry : property.entrySet()) {
            String[] propertyPath = entry.getKey().split("/"); // length == 3
            String siteName = propertyPath[0];
            String propertyName = propertyPath[2];

            if (!configGroup.getConfigurations().containsKey(siteName)) {
              RecommendationResponse.BlueprintConfigurations configurations =
                new RecommendationResponse.BlueprintConfigurations();
              configGroup.getConfigurations().put(siteName, configurations);
              configGroup.getConfigurations().get(siteName).setProperties(new HashMap<>());
            }
            configGroup.getConfigurations().get(siteName).getProperties().put(propertyName, entry.getValue());
          }
        }
        configGroups.add(configGroup);
      }
    }

    return configGroups;
  }

  protected Set<RecommendationResponse.ConfigGroup> calculateConfigGroups(Cluster cluster, Request request) {

    Set<RecommendationResponse.ConfigGroup> configGroups = new HashSet<>();

    Set<HashMap<String, Object>> configGroupsProperties =
      (HashSet<HashMap<String, Object>>) getRequestProperty(request, CONFIG_GROUPS_PROPERTY);
    if (configGroupsProperties != null) {
      for (HashMap<String, Object> props : configGroupsProperties) {
        RecommendationResponse.ConfigGroup configGroup = new RecommendationResponse.ConfigGroup();
        Object groupIdObject = props.get(CONFIG_GROUPS_GROUP_ID_PROPERTY);
        if (groupIdObject != null) {
          Long groupId = Long.valueOf((String) groupIdObject);
          ConfigGroup clusterConfigGroup = cluster.getConfigGroupsById(groupId);

          // convert configs
          Map<String, RecommendationResponse.BlueprintConfigurations> typedConfiguration = new HashMap<>();
          for (Map.Entry<String, Config> config : clusterConfigGroup.getConfigurations().entrySet()) {
            RecommendationResponse.BlueprintConfigurations blueprintConfiguration = new RecommendationResponse.BlueprintConfigurations();
            blueprintConfiguration.setProperties(config.getValue().getProperties());
            typedConfiguration.put(config.getKey(), blueprintConfiguration);
          }

          configGroup.setConfigurations(typedConfiguration);

          configGroup.setHosts(clusterConfigGroup.getHosts().values().stream().map(h -> h.getHostName()).collect(Collectors.toList()));
          configGroups.add(configGroup);
        }
      }
    }

    return configGroups;
  }

  /**
   * Parse the user contex for the call. Typical structure
   * { "operation" : "createCluster" }
   * { "operation" : "addService", "services" : "Atlas,Ranger" }
   * @param request
   * @return
   */
  protected Map<String, String> readUserContext(Request request) {
    Map<String, String> userContext = new HashMap<>();
    if (null != getRequestProperty(request, USER_CONTEXT_OPERATION_PROPERTY)) {
      userContext.put(OPERATION_PROPERTY,
                      (String) getRequestProperty(request, USER_CONTEXT_OPERATION_PROPERTY));
    }
    if (null != getRequestProperty(request, USER_CONTEXT_OPERATION_DETAILS_PROPERTY)) {
      userContext.put(OPERATION_DETAILS_PROPERTY,
                      (String) getRequestProperty(request, USER_CONTEXT_OPERATION_DETAILS_PROPERTY));
    }
    return userContext;
  }

  protected static final String CONFIGURATIONS_PROPERTY_ID = "recommendations/blueprint/configurations/";

  protected Map<String, Map<String, Map<String, String>>> calculateConfigurations(Request request) {
    Map<String, Map<String, Map<String, String>>> configurations = new HashMap<>();
    Map<String, Object> properties = request.getProperties().iterator().next();
    for (String property : properties.keySet()) {
      if (property.startsWith(CONFIGURATIONS_PROPERTY_ID)) {
        try {
          String propertyEnd = property.substring(CONFIGURATIONS_PROPERTY_ID.length()); // mapred-site/properties/yarn.app.mapreduce.am.resource.mb
          String[] propertyPath = propertyEnd.split("/"); // length == 3
          String siteName = propertyPath[0];
          String propertiesProperty = propertyPath[1];
          String propertyName = propertyPath[2];

          Map<String, Map<String, String>> siteMap = configurations.get(siteName);
          if (siteMap == null) {
            siteMap = new HashMap<>();
            configurations.put(siteName, siteMap);
          }

          Map<String, String> propertiesMap = siteMap.get(propertiesProperty);
          if (propertiesMap == null) {
            propertiesMap = new HashMap<>();
            siteMap.put(propertiesProperty, propertiesMap);
          }

          Object propVal = properties.get(property);
          if (propVal != null)
            propertiesMap.put(propertyName, propVal.toString());
          else
            LOG.info(String.format("No value specified for configuration property, name = %s ", property));

        } catch (Exception e) {
          LOG.debug(String.format("Error handling configuration property, name = %s", property), e);
          // do nothing
        }
      }
    }
    return configurations;
  }

  protected Map<String, Map<String, Map<String, String>>> calculateConfigurations(Cluster cluster, String serviceName)
      throws AmbariException {
    Map<String, Map<String, Map<String, String>>> configurations = new HashMap<>();
    Service service = cluster.getService(serviceName);

    StackId stackId = service.getDesiredStackId();
    ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);

    List<String> requiredConfigTypes = serviceInfo.getConfigDependenciesWithComponents();
    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    Map<String, DesiredConfig> requiredDesiredConfigs = new HashMap<>();
    for (String requiredConfigType : requiredConfigTypes) {
      if (desiredConfigs.containsKey(requiredConfigType)) {
        requiredDesiredConfigs.put(requiredConfigType, desiredConfigs.get(requiredConfigType));
      }
    }
    for (Map.Entry<String, DesiredConfig> requiredDesiredConfigEntry : requiredDesiredConfigs.entrySet()) {
      Config config = cluster.getConfig(requiredDesiredConfigEntry.getKey(), requiredDesiredConfigEntry.getValue().getTag());
      configurations.put(requiredDesiredConfigEntry.getKey(), Collections.singletonMap("properties", config.getProperties()));
    }
    return configurations;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateComponentHostsMap(Map<String, Set<String>> hostGroups,
                                                              Map<String, Set<String>> bindingHostGroups) {
    /*
     * ClassCastException may occur in case of body inconsistency: property
     * missed, etc.
     */

    Map<String, Set<String>> componentHostsMap = new HashMap<>();
    if (null != bindingHostGroups && null != hostGroups) {
      for (Map.Entry<String, Set<String>> hgComponents : hostGroups.entrySet()) {
        String hgName = hgComponents.getKey();
        Set<String> components = hgComponents.getValue();

        Set<String> hosts = bindingHostGroups.get(hgName);
        for (String component : components) {
          Set<String> componentHosts = componentHostsMap.get(component);
          if (componentHosts == null) { // if was not initialized
            componentHosts = new HashSet<>();
            componentHostsMap.put(component, componentHosts);
          }
          componentHosts.addAll(hosts);
        }
      }
    }

    return componentHostsMap;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateComponentHostsMap(Cluster cluster) {
    /*
     * ClassCastException may occur in case of body inconsistency: property
     * missed, etc.
     */

    Map<String, Set<String>> componentHostsMap = new HashMap<>();
    List<ServiceComponentHost> schs = cluster.getServiceComponentHosts();
    for (ServiceComponentHost sch : schs) {
      componentHostsMap.putIfAbsent(sch.getServiceComponentName(), new HashSet<>());
      componentHostsMap.get(sch.getServiceComponentName()).add(sch.getHostName());
    }

    return componentHostsMap;
  }

  protected Object getRequestProperty(Request request, String propertyName) {
    for (Map<String, Object> propertyMap : request.getProperties()) {
      if (propertyMap.containsKey(propertyName)) {
        return propertyMap.get(propertyName);
      }
    }
    return null;
  }

}
