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

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse.HostGroup;
import org.apache.ambari.server.state.ChangedConfigInfo;
import org.apache.ambari.server.topology.MpackInstance;
import org.apache.ambari.server.topology.ServiceInstance;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Mpack Advisor request.
 */
public class MpackAdvisorRequest {

  private MpackAdvisorRequestType requestType;
  private List<String> hosts = new ArrayList<>();
  // Mpack Instance Name -> Component Name -> host(s)
  private Map<String, Map<String, Set<String>>> mpackComponentHostsMap = new HashMap<>();
  private Map<String, Map<String, Map<String, String>>> configurations = new HashMap<>();
  private List<ChangedConfigInfo> changedConfigurations = new LinkedList<>();
  private Map<String, String> userContext = new HashMap<>();
  private Boolean gplLicenseAccepted;

  @JsonProperty
  private Recommendation recommendation = new Recommendation();

  public static class Recommendation {
    @JsonProperty
    private Blueprint blueprint =  new Blueprint();

    private Map<String, Set<String>> blueprint_cluster_binding =  new HashMap<>();

    public Blueprint getBlueprint() {
      return blueprint;
    }

    public void setBlueprint(Blueprint blueprint) {
      this.blueprint = blueprint;
    }

    public Map<String, Set<String>> getBlueprintClusterBinding() {
      return blueprint_cluster_binding;
    }

    public void setBlueprintClusterBinding(Map<String, Set<String>> blueprint_cluster_binding) {
      this.blueprint_cluster_binding = blueprint_cluster_binding;
    }
  }

  public static class Blueprint {
    @JsonProperty("host_groups")
    private Collection<HostGroup> hostGroups;
    @JsonProperty("mpack_instances")
    private Collection<MpackInstance> mpacks;

    public void setHostGroups(Collection<HostGroup> hostGroups) {
      this.hostGroups = hostGroups;
    }

    public Collection<HostGroup> getHostGroups() {
      return this.hostGroups;
    }

    public void setMpackInstances(Collection<MpackInstance> mpacks) {
      this.mpacks = mpacks;
    }

    public Collection<MpackInstance> getMpackInstances() {
      return this.mpacks;
    }

    @JsonIgnore
    public Collection<String> getServiceInstanceNames() {
      return mpacks.stream()
        .flatMap(mpack -> mpack.getServiceInstances().stream())
        .map(ServiceInstance::getName)
        .collect(toSet());
    }


  }

  public Recommendation getRecommendation() {
    return recommendation;
  }

  public void setRecommendations(Recommendation recommendation) {
    this.recommendation = recommendation;
  }

  public MpackAdvisorRequestType getRequestType() {
    return requestType;
  }

  public Map<String, Map<String, Map<String, String>>> getConfigurations() {
    return configurations;
  }

  public List<ChangedConfigInfo> getChangedConfigurations() {
    return changedConfigurations;
  }

  public void setChangedConfigurations(List<ChangedConfigInfo> changedConfigurations) {
    this.changedConfigurations = changedConfigurations;
  }

  public Map<String, String> getUserContext() {
    return this.userContext;
  }

  public void setUserContext(Map<String, String> userContext) {
    this.userContext = userContext;
  }

  /**
   * @return true if GPL license is accepted, false otherwise
   */
  public Boolean getGplLicenseAccepted() {
    return gplLicenseAccepted;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public static class MpackAdvisorRequestBuilder {
    MpackAdvisorRequest instance;

    private MpackAdvisorRequestBuilder() {
      this.instance = new MpackAdvisorRequest();
    }

    public static MpackAdvisorRequestBuilder forStack() {
      return new MpackAdvisorRequestBuilder();
    }

    public MpackAdvisorRequestBuilder ofType(MpackAdvisorRequestType requestType) {
      this.instance.requestType = requestType;
      return this;
    }

    public MpackAdvisorRequestBuilder forHosts(List<String> hosts) {
      this.instance.hosts = hosts;
      return this;
    }

    public MpackAdvisorRequestBuilder forMpackInstances(Collection<MpackInstance> mpackInstances) {
      this.instance.recommendation.blueprint.mpacks = mpackInstances;
      return this;
    }

    public MpackAdvisorRequestBuilder forComponentHostsMap(Collection<HostGroup> hgCompMap) {
      this.instance.recommendation.blueprint.hostGroups = hgCompMap;
      return this;
    }

    public MpackAdvisorRequestBuilder forHostsGroupBindings(
        Map<String, Set<String>> blueprint_cluster_binding) {
      this.instance.recommendation.blueprint_cluster_binding = blueprint_cluster_binding;
      return this;
    }

    public MpackAdvisorRequest.MpackAdvisorRequestBuilder withMpacksToComponentsHostsMap(
        Map<String, Map<String, Set<String>>> componentHostsMap) {
      this.instance.mpackComponentHostsMap = componentHostsMap;
      return this;
    }

    public MpackAdvisorRequest.MpackAdvisorRequestBuilder withConfigurations(
        Map<String, Map<String, Map<String, String>>> configurations) {
      this.instance.configurations = configurations;
      return this;
    }

    public MpackAdvisorRequest.MpackAdvisorRequestBuilder withChangedConfigurations(
        List<ChangedConfigInfo> changedConfigurations) {
      this.instance.changedConfigurations = changedConfigurations;
      return this;
    }

    public MpackAdvisorRequest.MpackAdvisorRequestBuilder withUserContext(
        Map<String, String> userContext) {
      this.instance.userContext = userContext;
      return this;
    }

    /**
     * Set GPL license acceptance parameter to request.
     * @param gplLicenseAccepted is GPL license accepted.
     * @return mpack advisor request builder.
     */
    public MpackAdvisorRequest.MpackAdvisorRequestBuilder withGPLLicenseAccepted(
        Boolean gplLicenseAccepted) {
      this.instance.gplLicenseAccepted = gplLicenseAccepted;
      return this;
    }

    public MpackAdvisorRequest build() {
      return this.instance;
    }
  }

  public enum MpackAdvisorRequestType {
    HOST_GROUPS("host_groups"),
    CONFIGURATIONS("configurations"),
    SSO_CONFIGURATIONS("sso-configurations"),
    CONFIGURATION_DEPENDENCIES("configuration-dependencies");

    private String type;

    MpackAdvisorRequestType(String type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return type;
    }

    public static MpackAdvisorRequestType fromString(String text) throws MpackAdvisorException {
      if (text != null) {
        for (MpackAdvisorRequestType next : MpackAdvisorRequestType.values()) {
          if (text.equalsIgnoreCase(next.type)) {
            return next;
          }
        }
      }
      throw new MpackAdvisorException(String.format(
          "Unknown request type: %s, possible values: %s", text, Arrays.toString(MpackAdvisorRequestType.values())));
    }
  }

  public String getMpackServiceInstanceTypeCommaSeparated(MpackInstance mpackInstance) {
    Collection<String> serviceInstancesTypeList = new ArrayList<>();
    for (ServiceInstance serviceInstance : mpackInstance.getServiceInstances()) {
      serviceInstancesTypeList.add(serviceInstance.getType());
    }
    return StringUtils.join(serviceInstancesTypeList, ",");
  }

  public Map<String, Map<String, Set<String>>> getMpackComponentHostsMap() {
    return mpackComponentHostsMap;
  }

  public String getHostsCommaSeparated() {
    return StringUtils.join(hosts, ",");
  }

  /******
   * Helper Functions
   *******/

  public Collection<MpackInstance> getMpackInstances() {
    return this.getRecommendation().getBlueprint().getMpackInstances();
  }
}
