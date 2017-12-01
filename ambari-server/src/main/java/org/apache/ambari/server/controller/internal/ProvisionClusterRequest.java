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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileBuilder;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileEvaluationException;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.Credential;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.NoSuchBlueprintException;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.Service;
import org.apache.ambari.server.topology.TopologyTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request for provisioning a cluster.
 */
public class ProvisionClusterRequest extends BaseClusterRequest {

  private final static Logger LOG = LoggerFactory.getLogger(ProvisionClusterRequest.class);

  /**
   * The global quick link filters property
   */
  static final String QUICKLINKS_PROFILE_FILTERS_PROPERTY = "quicklinks_profile/filters";

  /**
   * The service and component level quick link filters property
   */
  static final String QUICKLINKS_PROFILE_SERVICES_PROPERTY = "quicklinks_profile/services";

  private final String clusterName;
  private final TopologyTemplate topologyTemplate;
  private final Map<String, Credential> credentialsMap;
  private final String quickLinksProfileJson;

  /**
   * Constructor.
   *
   * @param topologyTemplate requested topology
   * @param securityConfiguration  security config related properties
   */
  public ProvisionClusterRequest(TopologyTemplate topologyTemplate, Map<String, Object> properties, SecurityConfiguration securityConfiguration)
      throws InvalidTopologyTemplateException {

    this.topologyTemplate = topologyTemplate;
    topologyTemplate.validate();

    clusterName = String.valueOf(properties.get(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));

    try {
      parseBlueprint();
    } catch (NoSuchStackException e) {
      throw new InvalidTopologyTemplateException("The specified stack doesn't exist: " + e, e);
    } catch (NoSuchBlueprintException e) {
      throw new InvalidTopologyTemplateException("The specified blueprint doesn't exist: " + e, e);
    }

    this.securityConfiguration = securityConfiguration;
    serviceConfigs = mergeServiceConfigs();
    processHostGroups();
    credentialsMap = this.topologyTemplate.getCredentials().stream()
      .collect(toMap(Credential::getAlias, Function.identity()));

    setProvisionAction(topologyTemplate.getProvisionAction());

    try {
      this.quickLinksProfileJson = processQuickLinksProfile(properties);
    } catch (QuickLinksProfileEvaluationException ex) {
      throw new InvalidTopologyTemplateException("Invalid quick links profile", ex);
    }
  }

  private List<Service> mergeServiceConfigs() throws InvalidTopologyTemplateException {
    // merge with BP service configs
    List<Service> serviceConfigs = new ArrayList<>();
    for (TopologyTemplate.Service s : topologyTemplate.getServices()) {
      Service service = blueprint.getServiceById(s.getId());
      if (service == null) {
        throw new InvalidTopologyTemplateException("Service: " + s.getName() + " in service group: " + s.getServiceGroup() + " not found.");
      }
      Configuration configuration = s.getConfiguration();
      configuration.setParentConfiguration(service.getConfiguration());
      service.setConfiguration(configuration);
      serviceConfigs.add(service);
    }
    return serviceConfigs;
  }

  private String processQuickLinksProfile(Map<String, Object> properties) throws QuickLinksProfileEvaluationException {
    Object globalFilters = properties.get(QUICKLINKS_PROFILE_FILTERS_PROPERTY);
    Object serviceFilters = properties.get(QUICKLINKS_PROFILE_SERVICES_PROPERTY);
    return (null != globalFilters || null != serviceFilters) ?
      new QuickLinksProfileBuilder().buildQuickLinksProfile(globalFilters, serviceFilters) : null;
  }


  public Map<String, Credential> getCredentialsMap() {
    return credentialsMap;
  }

  public String getClusterName() {
    return clusterName;
  }

  public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
    return topologyTemplate.getConfigRecommendationStrategy();
  }

  @Override
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public Type getType() {
    return Type.PROVISION;
  }

  @Override
  public String getDescription() {
    return String.format("Provision Cluster '%s'", clusterName);
  }

  /**
   * Load the blueprint specified in the request from the DB.
   *
   * @throws NoSuchStackException     if specified stack doesn't exist
   * @throws NoSuchBlueprintException if specified blueprint doesn't exist
   */
  private void parseBlueprint() throws NoSuchStackException, NoSuchBlueprintException {
    String blueprintName = topologyTemplate.getBlueprint();
    // set blueprint field
    try {
      setBlueprint(getBlueprintFactory().getBlueprint(blueprintName));
    } catch (IOException e) {
      LOG.error("Could not parse JSON stored in DB for blueprint {}", blueprintName, e);
      throw new NoSuchBlueprintException(blueprintName);
    }

    if (blueprint == null) {
      throw new NoSuchBlueprintException(blueprintName);
    }
  }

  private void processHostGroups() {
    getHostGroupInfo().putAll(topologyTemplate.getHostGroups().stream()
      .map(this::processHostGroup)
      .collect(toMap(HostGroupInfo::getHostGroupName, Function.identity())));
  }

  // TODO get rid of duplicate topology host group representation
  private HostGroupInfo processHostGroup(TopologyTemplate.HostGroup input) {
    HostGroupInfo output = new HostGroupInfo(input.getName());
    output.setPredicate(input.getHostPredicate());
    output.setRequestedCount(input.getHostCount());
    if (input.getHosts() != null) {
      output.addHosts(input.getHosts().stream().map(TopologyTemplate.Host::getFqdn).collect(toSet()));
      input.getHosts().forEach(h -> output.addHostRackInfo(h.getFqdn(), h.getRackInfo()));
    }
    output.setConfiguration(input.getConfiguration());
    return output;
  }

  /**
   * @return the quick links profile in Json string format
   */
  public String getQuickLinksProfileJson() {
    return quickLinksProfileJson;
  }

  public String getDefaultPassword() {
    return topologyTemplate.getDefaultPassword();
  }

}
