/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType.SSO_CONFIGURATIONS;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.SSO_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_ENABLED_SERVICES;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_MANAGE_SERVICES;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * AmbariServerSSOConfigurationHandler is an {@link AmbariServerConfigurationHandler} implementation
 * handing changes to the SSO configuration
 */
@Singleton
public class AmbariServerSSOConfigurationHandler extends AmbariServerConfigurationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerSSOConfigurationHandler.class);

  private final Clusters clusters;

  private final ConfigHelper configHelper;

  private final AmbariManagementController managementController;

  private final StackAdvisorHelper stackAdvisorHelper;

  @Inject
  public AmbariServerSSOConfigurationHandler(Clusters clusters, ConfigHelper configHelper,
                                             AmbariManagementController managementController,
                                             StackAdvisorHelper stackAdvisorHelper,
                                             AmbariConfigurationDAO ambariConfigurationDAO,
                                             AmbariEventPublisher publisher,
                                             Configuration ambariConfiguration) {
    super(ambariConfigurationDAO, publisher, ambariConfiguration);
    this.clusters = clusters;
    this.configHelper = configHelper;
    this.managementController = managementController;
    this.stackAdvisorHelper = stackAdvisorHelper;
  }

  @Override
  public void updateComponentCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) throws AmbariException {
    // Use the default implementation of #updateComponentCategory; however if Ambari is managing the SSO implementations
    // always process them, even the of sso-configuration properties have not been changed since we do not
    // know of the Ambari SSO data has changed in the ambari.properties file.  For example the authentication.jwt.providerUrl
    // or authentication.jwt.publicKey values.
    super.updateComponentCategory(categoryName, properties, removePropertiesIfNotSpecified);

    // Determine if Ambari is managing SSO configurations...
    boolean manageSSOConfigurations;

    Map<String, String> ssoProperties = getConfigurationProperties(SSO_CONFIGURATION.getCategoryName());
    manageSSOConfigurations = (ssoProperties != null) && "true".equalsIgnoreCase(ssoProperties.get(SSO_MANAGE_SERVICES.key()));

    if (manageSSOConfigurations) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null) {
        for (Cluster cluster : clusterMap.values()) {
          try {
            LOGGER.info(String.format("Managing the SSO configuration for the cluster named '%s'", cluster.getClusterName()));
            processCluster(cluster);
          } catch (AmbariException | StackAdvisorException e) {
            LOGGER.warn(String.format("Failed to update the the SSO configuration for the cluster named '%s': ", cluster.getClusterName()), e);
          }
        }
      }
    }
  }

  /**
   * Gets the set of services for which the user declared  Ambari to enable SSO integration.
   * <p>
   * If Ambari is not managing SSO integration configuration for services the set of names will be empry.
   *
   * @return a set of service names
   */
  public Set<String> getSSOEnabledServices() {
    Map<String, String> ssoProperties = getConfigurationProperties(SSO_CONFIGURATION.getCategoryName());
    boolean manageSSOConfigurations = (ssoProperties != null) && "true".equalsIgnoreCase(ssoProperties.get(SSO_MANAGE_SERVICES.key()));
    String ssoEnabledServices = (manageSSOConfigurations) ? ssoProperties.get(SSO_ENABLED_SERVICES.key()) : null;

    if (StringUtils.isEmpty(ssoEnabledServices)) {
      return Collections.emptySet();
    } else {
      return Arrays.stream(ssoEnabledServices.split(","))
          .map(String::trim)
          .map(String::toUpperCase)
          .collect(Collectors.toSet());
    }
  }

  /**
   * Build the stack advisor request, call the stack advisor, then automatically handle the recommendations.
   * <p>
   * Any recommendation coming back from the Stack/service advisor is expected to be only SSO-related
   * configurations.
   * <p>
   * If there are no changes to the current configurations, no new configuration versions will be created.
   *
   * @param cluster the cluster to process
   * @throws AmbariException
   * @throws StackAdvisorException
   */
  private void processCluster(Cluster cluster) throws AmbariException, StackAdvisorException {
    StackId stackVersion = cluster.getCurrentStackVersion();
    List<String> hosts = cluster.getHosts().stream().map(Host::getHostName).collect(Collectors.toList());
    Set<String> serviceNames = cluster.getServices().values().stream().map(Service::getName).collect(Collectors.toSet());

    // Build the StackAdvisor request for SSO-related configurations.  it is expected that the stack
    // advisor will abide by the configurations set in the Ambari sso-configurations to enable and
    // disable SSO integration for the relevant services.
    StackAdvisorRequest request = StackAdvisorRequest.StackAdvisorRequestBuilder.
        forStack(stackVersion.getStackName(), stackVersion.getStackVersion())
        .ofType(SSO_CONFIGURATIONS)
        .forHosts(hosts)
        .forServices(serviceNames)
        .withComponentHostsMap(cluster.getServiceComponentHostMap(null, null))
        .withConfigurations(calculateExistingConfigurations(cluster))
        .build();

    // Execute the stack advisor
    RecommendationResponse response = stackAdvisorHelper.recommend(request);

    // Process the recommendations and automatically apply them.  Ideally this is what the user wanted
    RecommendationResponse.Recommendation recommendation = (response == null) ? null : response.getRecommendations();
    RecommendationResponse.Blueprint blueprint = (recommendation == null) ? null : recommendation.getBlueprint();
    Map<String, RecommendationResponse.BlueprintConfigurations> configurations = (blueprint == null) ? null : blueprint.getConfigurations();

    if (configurations != null) {
      for (Map.Entry<String, RecommendationResponse.BlueprintConfigurations> configuration : configurations.entrySet()) {
        processConfigurationType(cluster, configuration.getKey(), configuration.getValue());
      }
    }
  }

  /**
   * Process the configuration to add, update, and remove properties as needed.
   *
   * @param cluster        the cluster
   * @param configType     the configuration type
   * @param configurations the recommended configuration values
   * @throws AmbariException
   */
  private void processConfigurationType(Cluster cluster, String configType,
                                        RecommendationResponse.BlueprintConfigurations configurations)
      throws AmbariException {

    Map<String, String> updates = new HashMap<>();
    Collection<String> removals = new HashSet<>();

    // Gather the updates
    Map<String, String> recommendedConfigProperties = configurations.getProperties();
    if (recommendedConfigProperties != null) {
      updates.putAll(recommendedConfigProperties);
    }

    // Determine if any properties need to be removed
    Map<String, ValueAttributesInfo> recommendedConfigPropertyAttributes = configurations.getPropertyAttributes();
    if (recommendedConfigPropertyAttributes != null) {
      for (Map.Entry<String, ValueAttributesInfo> entry : recommendedConfigPropertyAttributes.entrySet()) {
        ValueAttributesInfo info = entry.getValue();

        if ((info != null) && "true".equalsIgnoreCase(info.getDelete())) {
          updates.remove(entry.getKey());
          removals.add(entry.getKey());
        }
      }
    }

    configHelper.updateConfigType(cluster, cluster.getCurrentStackVersion(), managementController,
        configType, updates, removals,
        "internal", "Ambari-managed single sign-on configurations");
  }

  /**
   * Calculate the current configurations for all services
   *
   * @param cluster the cluster
   * @return a map of services and their configurations
   * @throws AmbariException
   */
  private Map<String, Map<String, Map<String, String>>> calculateExistingConfigurations(Cluster cluster) throws AmbariException {
    Map<String, Map<String, String>> configurationTags = configHelper.getEffectiveDesiredTags(cluster, null);
    Map<String, Map<String, String>> effectiveConfigs = configHelper.getEffectiveConfigProperties(cluster, configurationTags);

    Map<String, Map<String, Map<String, String>>> requestConfigurations = new HashMap<>();
    if (effectiveConfigs != null) {
      for (Map.Entry<String, Map<String, String>> configuration : effectiveConfigs.entrySet()) {
        Map<String, Map<String, String>> properties = new HashMap<>();
        String configType = configuration.getKey();
        Map<String, String> configurationProperties = configuration.getValue();

        if (configurationProperties == null) {
          configurationProperties = Collections.emptyMap();
        }

        properties.put("properties", configurationProperties);
        requestConfigurations.put(configType, properties);
      }
    }

    return requestConfigurations;
  }
}
