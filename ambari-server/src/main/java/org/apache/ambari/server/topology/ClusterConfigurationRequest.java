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

package org.apache.ambari.server.topology;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorBlueprintProcessor;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Responsible for cluster configuration.
 */
public class ClusterConfigurationRequest {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterConfigurationRequest.class);

  /**
   * a regular expression Pattern used to find "clusterHostInfo.(component_name)_host" placeholders in strings
   */
  private static final Pattern CLUSTER_HOST_INFO_PATTERN_VARIABLE = Pattern.compile("\\$\\{clusterHostInfo/?([\\w\\-\\.]+)_host(?:\\s*\\|\\s*(.+?))?\\}");
  public static final String CLUSTER_HOST_INFO = "clusterHostInfo";

  private AmbariContext ambariContext;
  private ClusterTopology clusterTopology;
  private BlueprintConfigurationProcessor configurationProcessor;
  private StackAdvisorBlueprintProcessor stackAdvisorBlueprintProcessor;
  private StackDefinition stack;
  private boolean configureSecurity = false;

  public ClusterConfigurationRequest(AmbariContext ambariContext, ClusterTopology topology, boolean setInitial,
                                     StackAdvisorBlueprintProcessor stackAdvisorBlueprintProcessor, boolean configureSecurity) {
    this(ambariContext, topology, setInitial, stackAdvisorBlueprintProcessor);
    this.configureSecurity = configureSecurity;
  }

  public ClusterConfigurationRequest(AmbariContext ambariContext, ClusterTopology clusterTopology, boolean setInitial,
                                     StackAdvisorBlueprintProcessor stackAdvisorBlueprintProcessor) {
    this.ambariContext = ambariContext;
    this.clusterTopology = clusterTopology;
    this.stack = clusterTopology.getStack();
    // set initial configuration (not topology resolved)
    this.configurationProcessor = new BlueprintConfigurationProcessor(clusterTopology);
    this.stackAdvisorBlueprintProcessor = stackAdvisorBlueprintProcessor;
    removeOrphanConfigTypes();
    if (setInitial) {
      setConfigurationsOnCluster(clusterTopology, TopologyManager.INITIAL_CONFIG_TAG, Collections.emptySet());
    }
  }

  /**
   * Remove config-types from the given configuration if there is no any services related to them (except cluster-env and global).
   */
  private void removeOrphanConfigTypes(Configuration configuration) {
    Collection<String> configTypes = configuration.getAllConfigTypes();
    for (String configType : configTypes) {
      if (!clusterTopology.isValidConfigType(configType)) {
        configuration.removeConfigType(configType);
        LOG.info("Removing config type '{}' as related service is not present in either Blueprint or cluster creation template.", configType);
      }
    }
  }

  /**
   * Remove config-types, if there is no any services related to them (except cluster-env and global).
   */
  private void removeOrphanConfigTypes() {
    Configuration configuration = clusterTopology.getConfiguration();
    removeOrphanConfigTypes(configuration);

    Map<String, HostGroupInfo> hostGroupInfoMap = clusterTopology.getHostGroupInfo();
    if (MapUtils.isNotEmpty(hostGroupInfoMap)) {
      for (Map.Entry<String, HostGroupInfo> hostGroupInfo : hostGroupInfoMap.entrySet()) {
        configuration = hostGroupInfo.getValue().getConfiguration();

        if (configuration != null) {
          removeOrphanConfigTypes(configuration);
        }
      }
    }
  }

  // get names of required host groups
  public Collection<String> getRequiredHostGroups() {
    Collection<String> requiredHostGroups = new HashSet<>();
    requiredHostGroups.addAll(configurationProcessor.getRequiredHostGroups());
    if (configureSecurity) {
      requiredHostGroups.addAll(getRequiredHostgroupsForKerberosConfiguration());
    }
    return requiredHostGroups;
  }

  public void process() throws AmbariException, ConfigurationTopologyException {
    // this will update the topo cluster config and all host group configs in the cluster topology
    Set<String> updatedConfigTypes = new HashSet<>();

    Map<String, Map<String, String>> userProvidedConfigurations = clusterTopology.getConfiguration().getFullProperties(1);

    try {
      if (configureSecurity) {
        Configuration clusterConfiguration = clusterTopology.getConfiguration();
        Map<String, Map<String, String>> existingConfigurations = clusterConfiguration.getFullProperties();
        updatedConfigTypes.addAll(configureKerberos(clusterConfiguration, existingConfigurations));
      }

      // obtain recommended configurations before config updates
      if (!ConfigRecommendationStrategy.NEVER_APPLY.equals(this.clusterTopology.getConfigRecommendationStrategy())) {
        // get merged properties form Blueprint & cluster template (this doesn't contains stack default values)
        stackAdvisorBlueprintProcessor.adviseConfiguration(this.clusterTopology, userProvidedConfigurations);
      }

      updatedConfigTypes.addAll(configurationProcessor.doUpdateForClusterCreate());
    } catch (ConfigurationTopologyException e) {
      //log and continue to set configs on cluster to make progress
      LOG.error("An exception occurred while doing configuration topology update: " + e, e);
    }

    setConfigurationsOnCluster(clusterTopology, TopologyManager.TOPOLOGY_RESOLVED_TAG, updatedConfigTypes);
  }

  private Set<String> configureKerberos(Configuration clusterConfiguration, Map<String, Map<String, String>> existingConfigurations) throws AmbariException {
    Set<String> updatedConfigTypes = new HashSet<>();

    Cluster cluster = getCluster();
    Blueprint blueprint = clusterTopology.getBlueprint();

    Configuration stackDefaults = clusterTopology.getStack().getConfiguration(clusterTopology.getServices());
    Map<String, Map<String, String>> stackDefaultProps = stackDefaults.getProperties();

    // add clusterHostInfo containing components to hosts map, based on Topology, to use this one instead of
    // StageUtils.getClusterInfo()
    Map<String, String> componentHostsMap = createComponentHostMap();
    existingConfigurations.put("clusterHostInfo", componentHostsMap);

    try {
      // generate principals & keytabs for headless identities
      AmbariContext.getController().getKerberosHelper()
        .ensureHeadlessIdentities(cluster, existingConfigurations,
          new HashSet<>(clusterTopology.getServices()));

      // apply Kerberos specific configurations
      Map<String, Map<String, String>> updatedConfigs = AmbariContext.getController().getKerberosHelper()
        .getServiceConfigurationUpdates(cluster, existingConfigurations,
            createServiceComponentMap(), null, null, true, false);

      // ******************************************************************************************
      // Since Kerberos is being enabled, make sure the cluster-env/security_enabled property is
      // set to "true"
      Map<String, String> clusterEnv = updatedConfigs.get("cluster-env");

      if(clusterEnv == null) {
        clusterEnv = new HashMap<>();
        updatedConfigs.put("cluster-env", clusterEnv);
      }

      clusterEnv.put("security_enabled", "true");
      // ******************************************************************************************

      for (String configType : updatedConfigs.keySet()) {
        // apply only if config type has related services in Blueprint
        if (clusterTopology.isValidConfigType(configType)) {
          Map<String, String> propertyMap = updatedConfigs.get(configType);
          Map<String, String> clusterConfigProperties = existingConfigurations.get(configType);
          Map<String, String> stackDefaultConfigProperties = stackDefaultProps.get(configType);
          for (String property : propertyMap.keySet()) {
            // update value only if property value configured in Blueprint / ClusterTemplate is not a custom one
            String currentValue = clusterConfiguration.getPropertyValue(configType, property);
            String newValue = propertyMap.get(property);
            if (!propertyHasCustomValue(clusterConfigProperties, stackDefaultConfigProperties, property) &&
              (currentValue == null || !currentValue.equals(newValue))) {

              LOG.debug("Update Kerberos related config property: {} {} {}", configType, property, propertyMap.get
                (property));
              clusterConfiguration.setProperty(configType, property, newValue);
              updatedConfigTypes.add(configType);
            }
          }
        }
      }

    } catch (KerberosInvalidConfigurationException e) {
      LOG.error("An exception occurred while doing Kerberos related configuration update: " + e, e);
    }

    return updatedConfigTypes;
  }

  /**
   * Create a map of services and the relevant components that are specified in the Blueprint
   *
   * @return a map of service names to component names
   */
  private Map<String, Set<String>> createServiceComponentMap() {
    return clusterTopology.getComponents()
      .collect(groupingBy(ResolvedComponent::effectiveServiceName,
        mapping(ResolvedComponent::componentName, toSet())));
  }

  /**
   * Returns true if the property exists in clusterConfigProperties and has a custom user defined value. Property has
   * custom value in case we there's no stack default value for it or it's not equal to stack default value.
   * @param clusterConfigProperties
   * @param stackDefaultConfigProperties
   * @param property
   * @return
   */
  private boolean propertyHasCustomValue(Map<String, String> clusterConfigProperties, Map<String, String>
    stackDefaultConfigProperties, String property) {

    boolean propertyHasCustomValue = false;
    if (clusterConfigProperties != null) {
      String propertyValue = clusterConfigProperties.get(property);
      if (propertyValue != null) {
        if (stackDefaultConfigProperties != null) {
          String stackDefaultValue = stackDefaultConfigProperties.get(property);
          if (stackDefaultValue != null) {
            propertyHasCustomValue = !propertyValue.equals(stackDefaultValue);
          } else {
            propertyHasCustomValue = true;
          }
        } else {
          propertyHasCustomValue = true;
        }
      }
    }
    return propertyHasCustomValue;
  }

  private Map<String, String> createComponentHostMap() {
    Map<String, String> componentHostsMap = new HashMap<>();
    for (ResolvedComponent component : clusterTopology.getComponents().collect(toSet())) {
      String componentName = component.componentName();
      Collection<String> componentHost = clusterTopology.getHostAssignmentsForComponent(componentName);
      // retrieve corresponding clusterInfoKey for component using StageUtils
      String clusterInfoKey = StageUtils.getComponentToClusterInfoKeyMap().get(componentName);
      if (clusterInfoKey == null) {
        clusterInfoKey = componentName.toLowerCase() + "_hosts";
      }
      componentHostsMap.put(clusterInfoKey, StringUtils.join(componentHost, ","));
    }
    return componentHostsMap;
  }

  private Collection<String> getRequiredHostgroupsForKerberosConfiguration() {
    Collection<String> requiredHostGroups = new HashSet<>();

    try {
      Cluster cluster = getCluster();

      Configuration clusterConfiguration = clusterTopology.getConfiguration();
      Map<String, Map<String, String>> existingConfigurations = clusterConfiguration.getFullProperties();
      existingConfigurations.put(CLUSTER_HOST_INFO, new HashMap<>());

      // apply Kerberos specific configurations
      Map<String, Map<String, String>> updatedConfigs = AmbariContext.getController().getKerberosHelper()
        .getServiceConfigurationUpdates(cluster, existingConfigurations,
          createServiceComponentMap(), null, null, true, false);

      // retrieve hostgroup for component names extracted from variables like "{clusterHostInfo.(component_name)
      // _host}"
      for (String configType : updatedConfigs.keySet()) {
        Map<String, String> propertyMap = updatedConfigs.get(configType);
        for (String property : propertyMap.keySet()) {
          String propertyValue = propertyMap.get(property);
          Matcher matcher = CLUSTER_HOST_INFO_PATTERN_VARIABLE.matcher(propertyValue);
          while (matcher.find()) {
            String component = matcher.group(1).toUpperCase();
            Collection<String> hostGroups = clusterTopology.getHostGroupsForComponent(component);
            if (hostGroups.isEmpty()) {
              LOG.warn("No matching hostgroup found for component: {} specified in Kerberos config type: {} property:" +
                " " +
                "{}", component, configType, property);
            } else {
              requiredHostGroups.addAll(hostGroups);
            }
          }
        }
      }

    } catch (KerberosInvalidConfigurationException | AmbariException e) {
      LOG.error("An exception occurred while doing Kerberos related configuration update: " + e, e);
    }
    return requiredHostGroups;
  }

  private Cluster getCluster() throws AmbariException {
    String clusterName = ambariContext.getClusterName(clusterTopology.getClusterId());
    return AmbariContext.getController().getClusters().getCluster(clusterName);
  }

  /**
   * Set all configurations on the cluster resource.
   * @param clusterTopology  cluster topology
   * @param tag              config tag
   */
  public void setConfigurationsOnCluster(ClusterTopology clusterTopology, String tag, Set<String> updatedConfigTypes) {
    // TODO: This version works with Ambari 3.0 where it is assumed that any service with a configuration can be identified
    //   by its name. Even though the cluster is multi-stack (multi-mpack), service names should not conflict across mpacks,
    //   except client services which have no configuration. In 3.1, mpack may have conflicting service names
    //todo: also handle setting of host group scoped configuration which is updated by config processor
    List<Pair<String, ClusterRequest>> serviceNamesAndConfigurationRequests = new ArrayList<>();

    Configuration clusterConfiguration = clusterTopology.getConfiguration();

    final Set<String> clusterConfigTypes = clusterConfiguration.getFullProperties().keySet();
    final Set<String> globalConfigTypes = ImmutableSet.of("cluster-env");

    // TODO: do we need to handle security type? In the previous version it was handled but in a broken way
    for (ServiceResponse service : ambariContext.getServices(clusterTopology.getClusterName())) {
      ClusterRequest clusterRequest =
        new ClusterRequest(clusterTopology.getClusterId(), clusterTopology.getClusterName(), null, null, null, null);
      clusterRequest.setDesiredConfig(new ArrayList<>());

      Set<String> configTypes =
        Sets.difference(
          Sets.intersection(stack.getAllConfigurationTypes(service.getServiceName()), clusterConfigTypes),
          Sets.union(stack.getExcludedConfigurationTypes(service.getServiceName()), globalConfigTypes)
        );

      for (String serviceConfigType: configTypes) {
        Map<String, String> properties = clusterConfiguration.getFullProperties().get(serviceConfigType);
        Map<String, Map<String, String>> attributes = clusterConfiguration.getFullAttributes().get(serviceConfigType);

        removeNullValues(properties, attributes);

        ConfigurationRequest configurationRequest = new ConfigurationRequest(clusterTopology.getClusterName(),
          serviceConfigType,
          tag,
          properties,
          attributes,
          service.getServiceId(),
          service.getServiceGroupId());
        clusterRequest.getDesiredConfig().add(configurationRequest);
      }
      serviceNamesAndConfigurationRequests.add(Pair.of(service.getServiceName(), clusterRequest));
    }

    // since the stack returns "cluster-env" with each service's config ensure that only one
    // ClusterRequest occurs for the global cluster-env configuration
    ClusterRequest globalConfigClusterRequest =
      new ClusterRequest(clusterTopology.getClusterId(), clusterTopology.getClusterName(), null, null, null, null);

    Map<String, String> clusterEnvProps = clusterConfiguration.getFullProperties().get("cluster-env");
    Map<String, Map<String, String>> clusterEnvAttributes = clusterConfiguration.getFullAttributes().get("cluster-env");

    removeNullValues(clusterEnvProps, clusterEnvAttributes);

    ConfigurationRequest globalConfigurationRequest = new ConfigurationRequest(clusterTopology.getClusterName(),
      "cluster-env",
      tag,
      clusterEnvProps,
      clusterEnvAttributes,
      null,
      null);
    globalConfigClusterRequest.setDesiredConfig(Lists.newArrayList(globalConfigurationRequest));
    serviceNamesAndConfigurationRequests.add(Pair.of("GLOBAL-CONFIG", globalConfigClusterRequest));

    // send configurations
    setConfigurationsOnCluster(serviceNamesAndConfigurationRequests, tag, updatedConfigTypes);
  }

  private void removeNullValues(Map<String, String> configProperties, Map<String, Map<String, String>> configAttributes) {
    if (null != configProperties) {
      configProperties.values().removeIf(Objects::isNull);
    }
    if (null != configAttributes) {
      configAttributes.values().removeIf(Objects::isNull);
      configAttributes.values().forEach(map -> map.values().removeIf(Objects::isNull));
      configAttributes.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
  }


  /**
   * Creates a ClusterRequest for each service that
   *   includes any associated config types and configuration. The Blueprints
   *   implementation will now create one ClusterRequest per service, in order
   *   to comply with the ServiceConfigVersioning framework in Ambari.
   *
   * This method will also send these requests to the management controller.
   *
   * @param serviceNamesAndRequests a list of requests to send to the AmbariManagementController.
   */
  private void  setConfigurationsOnCluster(List<Pair<String, ClusterRequest>> serviceNamesAndRequests,
                                         String tag, Set<String> updatedConfigTypes)  {
    // iterate over services to deploy
    for (Pair<String, ClusterRequest> serviceNameAndRequest: serviceNamesAndRequests) {
      LOG.info("Sending cluster config update request for service = " + serviceNameAndRequest.getLeft());
      ambariContext.setConfigurationOnCluster(serviceNameAndRequest.getRight());
    }

    if (tag.equals(TopologyManager.TOPOLOGY_RESOLVED_TAG)) {
      // if this is a request to resolve config, then wait until resolution is completed
      try {
        // wait until the cluster topology configuration is set/resolved
        ambariContext.waitForConfigurationResolution(clusterTopology.getClusterName(), updatedConfigTypes);
      } catch (AmbariException e) {
        LOG.error("Error while attempting to wait for the cluster configuration to reach TOPOLOGY_RESOLVED state.", e);
      }
    }
  }

  /**
   * Internal class meant to represent the collection of configuration
   * items and configuration attributes that are associated with a given service.
   *
   * This class is used to support proper configuration versioning when
   * Ambari Blueprints is used to deploy a cluster.
   */
  private static class BlueprintServiceConfigRequest {

    private final String serviceName;
    private final Long serviceId;
    private final Long serviceGroupId;

    private List<BlueprintServiceConfigElement> configElements =
      new LinkedList<>();

    BlueprintServiceConfigRequest(String serviceName, Long serviceId, Long serviceGroupId) {
      this.serviceName = serviceName;
      this.serviceId = serviceId;
      this.serviceGroupId = serviceGroupId;
    }

    void addConfigElement(String type, Map<String, String> props, Map<String, Map<String, String>> attributes) {
      if (props == null) {
        props = Collections.emptyMap();
      }

      if (attributes == null) {
        attributes = Collections.emptyMap();
      }
      configElements.add(new BlueprintServiceConfigElement(type, props, attributes));
    }

    public String getServiceName() {
      return serviceName;
    }

    public Long getServiceId() {
      return serviceId;
    }

    public Long getServiceGroupId() {
      return serviceGroupId;
    }

    List<BlueprintServiceConfigElement> getConfigElements() {
      return configElements;
    }
  }

  /**
   * Internal class that represents the configuration
   *  and attributes for a given configuration type.
   */
  private static class BlueprintServiceConfigElement {
    private final String typeName;

    private final Map<String, String> configuration;

    private final Map<String, Map<String, String>> attributes;

    BlueprintServiceConfigElement(String type, Map<String, String> props, Map<String, Map<String, String>> attributes) {
      this.typeName = type;
      this.configuration = props;
      this.attributes = attributes;
    }

    public String getTypeName() {
      return typeName;
    }

    public Map<String, String> getConfiguration() {
      return configuration;
    }

    public Map<String, Map<String, String>> getAttributes() {
      return attributes;
    }
  }
}
