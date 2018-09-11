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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.AdvisorBlueprintProcessor;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.state.ComponentInfo;
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
import org.apache.ambari.server.topology.ResolvedComponent;
import org.apache.ambari.server.topology.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
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

  private static final Logger LOG = LoggerFactory.getLogger(MpackAdvisorBlueprintProcessor.class);

  private static MpackAdvisorHelper mpackAdvisorHelper;

  private static AmbariMetaInfo metaInfo;

  public static void init(MpackAdvisorHelper instance, AmbariMetaInfo ambariMetaInfo) {
    mpackAdvisorHelper = instance;
    metaInfo = ambariMetaInfo;
  }

  private static final Map<String, String> userContext = ImmutableMap.of("operation", "ClusterCreate");

  /**
   * {@inheritDoc}
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
    Map<String, Map<String, Set<String>>> mpackComponentsHostsMap = gatherMackComponentsHostsMap(clusterTopology);
    Set<MpackInstance> mpacks = copyAndEnrichMpackInstances(clusterTopology);
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

  private Set<MpackInstance> copyAndEnrichMpackInstances(ClusterTopology topology) {
    // Copy mpacks
    Set<MpackInstance> mpacks = topology.getMpacks().stream().map(MpackInstance::copy).collect(toSet());

    // Add missing service instances
    Map<StackId, Set<String>> mpackServices = topology.getComponents().collect(toMap(
      ResolvedComponent::stackId,
      comp -> ImmutableSet.of(comp.serviceInfo().getName()),
      (set1, set2) -> ImmutableSet.copyOf(Sets.union(set1, set2))
    ));
    for (MpackInstance mpack: mpacks) {
      if (!mpackServices.containsKey(mpack.getStackId())) {
        LOG.warn("No services declared for mpack {}.", mpack.getStackId());
      }
      else {
        Set<String> existingMpackServices = mpack.getServiceInstances().stream().map(ServiceInstance::getType).collect(toSet());
        for(String service: mpackServices.get(mpack.getStackId())) {
          if (existingMpackServices.contains(service)) {
            LOG.debug("Mpack instance {} already contains service {}", mpack.getStackId(), service);
          }
          else {
            LOG.debug("Adding service {} to mpack instance {}", service, mpack.getStackId());
            mpack.getServiceInstances().add(new ServiceInstance(service, service, null, mpack));
          }
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

    topology.getComponentsByHostGroup().entrySet().stream().collect(
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
    Map<String, Set<String>> hgBindings = Maps.newHashMap();
    for (Map.Entry<String, HostGroupInfo> hgEnrty: clusterTopology.getHostGroupInfo().entrySet()) {
      hgBindings.put(hgEnrty.getKey(), Sets.newCopyOnWriteArraySet(hgEnrty.getValue().getHostNames()));
    }
    return hgBindings;
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

  private Map<String, Map<String, Set<String>>> gatherMackComponentsHostsMap(ClusterTopology topology) {
    Map<String, Map<String, Set<String>>> mpackComponentsHostsMap = new HashMap<>();
    for (Map.Entry<String, Set<ResolvedComponent>> hgToComps : topology.getComponentsByHostGroup().entrySet()) {
      String hostGroup = hgToComps.getKey();
      Set<ResolvedComponent> components = hgToComps.getValue();
      Set<String> hosts = topology.getHostGroupInfo().get(hostGroup).getHostNames();
      for (ResolvedComponent component: components) {
        String mpackName = component.stackId().getStackName(); // TODO: support multiple mpacks under different names?
        mpackComponentsHostsMap
          .computeIfAbsent(mpackName, __ -> new HashMap<>())
          .put(component.componentName(), hosts);
      }
    }
    return mpackComponentsHostsMap;
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
