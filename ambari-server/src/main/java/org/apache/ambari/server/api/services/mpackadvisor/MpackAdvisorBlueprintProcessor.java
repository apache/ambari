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

package org.apache.ambari.server.api.services.mpackadvisor;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.utils.ExceptionUtils.unchecked;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.AdvisorBlueprintProcessor;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorBlueprintProcessor;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.AdvisedConfiguration;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.MpackInstance;
import org.apache.ambari.server.topology.ServiceInstance;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;

/**
 * Generate advised configurations for blueprint cluster provisioning by the mpack advisor.
 */
@Singleton
public class MpackAdvisorBlueprintProcessor implements AdvisorBlueprintProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(StackAdvisorBlueprintProcessor.class);

  private static MpackAdvisorHelper mpackAdvisorHelper;

  static final String RECOMMENDATION_FAILED = "Configuration recommendation failed.";
  static final String INVALID_RESPONSE = "Configuration recommendation returned with invalid response.";
  private static AmbariMetaInfo metaInfo;

  public static void init(MpackAdvisorHelper instance, AmbariMetaInfo ambariMetaInfo) {
    mpackAdvisorHelper = instance;
    metaInfo = ambariMetaInfo;
  }

  private static final Map<String, String> userContext;
  static
  {
    userContext = new HashMap<>();
    userContext.put("operation", "ClusterCreate");
  }

  /**
   * Recommend configurations by the mpack advisor, then store the results in cluster topology.
   * @param clusterTopology cluster topology instance
   * @param userProvidedConfigurations User configurations of cluster provided in Blueprint + Cluster template
   */
  public void adviseConfiguration(ClusterTopology clusterTopology, Map<String, Map<String, String>> userProvidedConfigurations) throws ConfigurationTopologyException {
    MpackAdvisorRequest request = createMpackAdvisorRequest(clusterTopology, MpackAdvisorRequest.MpackAdvisorRequestType.CONFIGURATIONS);
    try {
      MpackRecommendationResponse response = mpackAdvisorHelper.recommend(request);
      addAllAdvisedConfigurationsToTopology(response, clusterTopology, userProvidedConfigurations);
    } catch (MpackAdvisorException e) {
      throw new ConfigurationTopologyException(RECOMMENDATION_FAILED, e);
    } catch (IllegalArgumentException e) {
      throw new ConfigurationTopologyException(INVALID_RESPONSE, e);
    }
  }

  private MpackAdvisorRequest createMpackAdvisorRequest(ClusterTopology clusterTopology,
                                                        MpackAdvisorRequest.MpackAdvisorRequestType requestType) {
    Map<String, Set<Component>> hgComponentsMap = gatherHostGroupComponents(clusterTopology);
    Map<String, Set<String>> hgHostsMap = gatherHostGroupBindings(clusterTopology);
    Map<String, Map<String, Set<String>>> mpackComponentsHostsMap = gatherMackComponentsHostsMap(hgComponentsMap,
      hgHostsMap, clusterTopology.getMpacks());
    Map<String, Set<String>> mpackComponentsMap = mpackComponentsHostsMap.entrySet().stream().collect(toMap(
      Map.Entry::getKey,
      componentHostMap -> Sets.newHashSet(componentHostMap.getValue().keySet())
    ));
    Set<MpackInstance> mpacks = copyAndEnrichMpackInstances(clusterTopology, mpackComponentsMap);
//    Configuration configuration = copyServiceConfigurationsToServiceInstances(mpacks, clusterTopology);
    Configuration configuration = clusterTopology.getConfiguration();
    return MpackAdvisorRequest.MpackAdvisorRequestBuilder
      .forStack()
      .forMpackInstances(mpacks)
      .forHosts(gatherHosts(clusterTopology))
      .forHostsGroupBindings(gatherHostGroupBindings(clusterTopology))
      .forComponentHostsMap(getHostgroups(clusterTopology))
      .withMpacksToComponentsHostsMap(mpackComponentsHostsMap)
      .withConfigurations(calculateConfigs(configuration))
      .withUserContext(userContext)
      .ofType(requestType)
      .build();
  }

  private Configuration copyServiceConfigurationsToServiceInstances(Set<MpackInstance> mpacks, ClusterTopology clusterTopology) {
    Configuration originalConfig = clusterTopology.getConfiguration();
    Configuration copiedConfig = new Configuration(originalConfig.getFullProperties(), originalConfig.getFullAttributes());
    Map<String, Map<String, String>> fullProperties = copiedConfig.getFullProperties();
    Map<String, Map<String, Map<String, String>>> fullAttributes = copiedConfig.getFullAttributes();
    for (String configType: copiedConfig.getAllConfigTypes()) {
      boolean isServiceConfig = false;
      Set<String> serviceTypes = clusterTopology.getStack().getServicesForConfigType(configType).collect(toSet());
      for (MpackInstance mpack: mpacks) {
        for (ServiceInstance service: mpack.getServiceInstances()) {
          if (serviceTypes.contains(service.getType())) {
            isServiceConfig = true;
            if (null == service.getConfiguration()) {
              service.setConfiguration(new Configuration());
            }
            if (fullProperties.containsKey(configType)) {
              service.getConfiguration().getProperties().put(configType, fullProperties.get(configType));
            }
            if (fullAttributes.containsKey(configType)) {
              service.getConfiguration().getAttributes().put(configType, fullAttributes.get(configType));
            }
          }
        }
      }
      if (isServiceConfig) { // Only global configs such as cluster-env should remain in the central configuration
        copiedConfig.removeConfigType(configType);
      }
    }
    return copiedConfig;
  }

  private Set<MpackInstance> copyAndEnrichMpackInstances(ClusterTopology topology,
                                                         Map<String, Set<String>> mpackComponentsMap) {
    // Copy mpacks
    Set<MpackInstance> mpacks = topology.getMpacks().stream().map(MpackInstance::copy).collect(toSet());
    // Add missing service instances
    for (MpackInstance mpack: mpacks) {
      Set<String> mpackComponents = mpackComponentsMap.get(mpack.getMpackType()); // TODO: this should be getMpackName() once mpack advisor fixed
      for (ServiceInfo serviceInfo : unchecked(() -> metaInfo.getStack(mpack.getStackId())).getServices()) {
        boolean serviceUsedInBlueprint =
          serviceInfo.getComponents().stream().filter(comp -> mpackComponents.contains(comp.getName())).findAny().isPresent();
        // TODO: we will will have to check for existing service instances once multi-service will be enabled and
        // mpack instances in blueprints may contain service instances
        if (serviceUsedInBlueprint) {
          mpack.getServiceInstances().add(new ServiceInstance(serviceInfo.getName(), serviceInfo.getName(), null, mpack));
        }
      }
    }
    return mpacks;
  }

  private Collection<MpackRecommendationResponse.HostGroup> getHostgroups(ClusterTopology topology) {
    // TODO: this will need to rewritten for true multi-everything (multiple mpacks of the same type/version under
    //  different names)
    Map<StackId, String> mpackNameByStackId = topology.getMpacks().stream().collect(
      toMap(
        MpackInstance::getStackId,
        MpackInstance::getMpackName
      ));

    topology.getComponentsByHostgroup().entrySet().stream().collect(
      toMap(
  //      Map.Entry::getKey,
        e -> e.getKey(),
        components ->
          components.getValue().stream()
            .map(comp ->
              MpackRecommendationResponse.HostGroup.createComponent(comp.componentName(),
                mpackNameByStackId.get(comp.stackId()),
                comp.serviceName().orElseGet(() -> comp.serviceType())))
            .collect(toSet())
      )
    );

    return null;
  }

  private Map<String, Set<String>> gatherHostGroupBindings(ClusterTopology clusterTopology) {
    Map<String, Set<String>> hgBindngs = Maps.newHashMap();
    for (Map.Entry<String, HostGroupInfo> hgEnrty: clusterTopology.getHostGroupInfo().entrySet()) {
      hgBindngs.put(hgEnrty.getKey(), Sets.newCopyOnWriteArraySet(hgEnrty.getValue().getHostNames()));
    }
    return hgBindngs;
  }

  private Map<String, Set<Component>> gatherHostGroupComponents(ClusterTopology clusterTopology) {
    Map<String, Set<Component>> hgComponentsMap = Maps.newHashMap();
    for (Map.Entry<String, HostGroup> hgEnrty: clusterTopology.getBlueprint().getHostGroups().entrySet()) {
      hgComponentsMap.put(hgEnrty.getKey(), Sets.newCopyOnWriteArraySet(hgEnrty.getValue().getComponents()));
    }
    return hgComponentsMap;
  }

  private Map<String, Map<String, Map<String, String>>> calculateConfigs(Configuration configuration) {
    Map<String, Map<String, Map<String, String>>> result = Maps.newHashMap();
    Map<String, Map<String, String>> fullProperties = configuration.getFullProperties();
    for (Map.Entry<String, Map<String, String>> siteEntry : fullProperties.entrySet()) {
      Map<String, Map<String, String>> propsMap = Maps.newHashMap();
      propsMap.put("properties", siteEntry.getValue());
      result.put(siteEntry.getKey(), propsMap);
    }
    return result;
  }

  private Map<String, Map<String, Set<String>>> gatherMackComponentsHostsMap(Map<String, Set<Component>> hostGroupComponents,
                                                                Map<String, Set<String>> hostGroupHosts,
                                                                Set<MpackInstance> mpacks) {

    // Calculate helper maps
    Map<String, Set<String>> mpackComponents = mpacks.stream()
      .collect(toMap(
        MpackInstance::getMpackType, // TODO: this is supposed to be MpackInstance::getMpackName, fix mpack advisor
        mpack -> getStackComponents(mpack.getStackId())
      ));

    Map<String, Set<String>> componentNameToMpacks = mpackComponents.entrySet().stream()
      .flatMap( mpc -> mpc.getValue().stream().map(component -> Pair.of(component, ImmutableSet.of(mpc.getKey()))) )
      .collect(toMap(
        Pair::getLeft,
        Pair::getRight,
        (set1, set2) -> ImmutableSet.copyOf(Sets.union(set1, set2))
      ));

    Map<String, Set<Component>> hostComponents = hostGroupComponents.entrySet().stream()
      .flatMap( hgc -> hostGroupHosts.get(hgc.getKey()).stream().map(host -> Pair.of(host, hgc.getValue())))
      .collect(toMap(Pair::getLeft, Pair::getRight));

    // Calculate map to return: mpack -> component -> host
    Map<String, Map<String, Set<String>>> mpackComponentHostsMap = new HashMap<>();

    hostComponents.entrySet().forEach( entry -> {
      String hostName = entry.getKey();
      Set<Component> components = entry.getValue();
      components.forEach( component -> {
        Set<String> mpacksForComponent = getMpacksForComponent(component, componentNameToMpacks);
        mpacksForComponent.forEach(mpack -> {
          mpackComponentHostsMap
            .computeIfAbsent(mpack, __ -> new HashMap<>())
            .computeIfAbsent(component.getName(), __ -> new HashSet<>())
            .add(hostName);
        });
      });
    });
    return mpackComponentHostsMap;
  }

  private Set<String> getMpacksForComponent(Component component, Map<String, Set<String>> componentToMpacks) {
    Set<String> mpacksForComponent = component.getMpackInstance() != null
      ? ImmutableSet.of(component.getMpackInstance())
      : componentToMpacks.getOrDefault(component.getName(), ImmutableSet.of());
    if (mpacksForComponent.isEmpty()) {
      LOG.error("No mpack found for component [{}]", component.getName());
    }
    return mpacksForComponent;
  }

  private List<String> gatherHosts(ClusterTopology clusterTopology) {
    List<String> hosts = Lists.newArrayList();
    for (Map.Entry<String, HostGroupInfo> entry : clusterTopology.getHostGroupInfo().entrySet()) {
      hosts.addAll(entry.getValue().getHostNames());
    }
    return hosts;
  }

  private void addAllAdvisedConfigurationsToTopology(MpackRecommendationResponse response,
                                                     ClusterTopology topology, Map<String, Map<String, String>> userProvidedConfigurations) {
    Preconditions.checkArgument(response.getRecommendations() != null,
      "Recommendation response is empty.");
    Preconditions.checkArgument(response.getRecommendations().getBlueprint() != null,
      "Blueprint field is missing from the recommendation response.");

    MpackRecommendationResponse.Blueprint blueprint = response.getRecommendations().getBlueprint();

    Map<String, MpackRecommendationResponse.BlueprintConfigurations> recommendedConfigurations = blueprint.getConfigurations();

    addAdvisedConfigurationToTopology(blueprint.getConfigurations(), topology, userProvidedConfigurations);

    blueprint.getMpackInstances().forEach( mpack -> {
      mpack.getServiceInstances().forEach( svc -> {
        addAdvisedConfigurationToTopology(svc.getConfigurations(), topology, userProvidedConfigurations);
      });
    });
  }

  private void addAdvisedConfigurationToTopology(Map<String, MpackRecommendationResponse.BlueprintConfigurations> recommendedConfigurations,
                                                  ClusterTopology topology, Map<String, Map<String, String>> userProvidedConfigurations) {
    if (null != recommendedConfigurations) {
      for (Map.Entry<String, MpackRecommendationResponse.BlueprintConfigurations> configEntry : recommendedConfigurations.entrySet()) {
        String configType = configEntry.getKey();
        // add recommended config type only if related service is present in Blueprint
        if (topology.isValidConfigType(configType)) {
          MpackRecommendationResponse.BlueprintConfigurations blueprintConfig = filterBlueprintConfig(configType, configEntry.getValue(),
            userProvidedConfigurations, topology);
          topology.getAdvisedConfigurations().put(configType, new AdvisedConfiguration(
            blueprintConfig.getProperties(), blueprintConfig.getPropertyAttributes()));
        }
      }
    }
  }


  /**
   * Remove user defined properties from Stack Advisor output in case of ONLY_STACK_DEFAULTS_APPLY or
   * ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES.
   */
  private MpackRecommendationResponse.BlueprintConfigurations filterBlueprintConfig(String configType, MpackRecommendationResponse.BlueprintConfigurations config,
                                                                               Map<String, Map<String, String>> userProvidedConfigurations,
                                                                               ClusterTopology topology) {
    if (topology.getConfigRecommendationStrategy() == ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY ||
      topology.getConfigRecommendationStrategy() == ConfigRecommendationStrategy
        .ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES) {
      if (userProvidedConfigurations.containsKey(configType)) {
        MpackRecommendationResponse.BlueprintConfigurations newConfig = new MpackRecommendationResponse.BlueprintConfigurations();
        Map<String, String> filteredProps = Maps.filterKeys(config.getProperties(),
          Predicates.not(Predicates.in(userProvidedConfigurations.get(configType).keySet())));
        newConfig.setProperties(Maps.newHashMap(filteredProps));

        if (config.getPropertyAttributes() != null) {
          Map<String, ValueAttributesInfo> filteredAttributes = Maps.filterKeys(config.getPropertyAttributes(),
            Predicates.not(Predicates.in(userProvidedConfigurations.get(configType).keySet())));
          newConfig.setPropertyAttributes(Maps.newHashMap(filteredAttributes));
        }
        return newConfig;
      }
    }
    return config;
  }

  Set<String> getStackComponents(StackId stackId) {
    return unchecked(() -> metaInfo.getStack(stackId)).getServices().stream()
      .flatMap( svc -> svc.getComponents().stream() )
      .map(ComponentInfo::getName)
      .collect(toSet());
  }

}
