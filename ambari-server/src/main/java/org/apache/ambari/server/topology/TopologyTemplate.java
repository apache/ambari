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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.ambari.server.controller.internal.ProvisionAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

public class TopologyTemplate {

  private String blueprint = null;

  @JsonProperty("default_password")
  private String defaultPassword = null;

  @JsonProperty("config_recommendation_strategy")
  private ConfigRecommendationStrategy configRecommendationStrategy;

  @JsonProperty("provision_action")
  private ProvisionAction provisionAction;

  private Map<ServiceId, TopologyTemplate.Service> servicesById = Collections.emptyMap();

  private Map<String, TopologyTemplate.HostGroup> hostGroups = Collections.emptyMap();

  private Collection<Credential> credentials;

  @JsonProperty("security")
  private SecurityConfiguration securityConfiguration;

  public String getBlueprint() {
    return blueprint;
  }

  public void setBlueprint(String blueprint) {
    this.blueprint = blueprint;
  }

  public String getDefaultPassword() {
    return defaultPassword;
  }

  public void setDefaultPassword(String defaultPassword) {
    this.defaultPassword = defaultPassword;
  }

  public @Nullable Service getServiceById(ServiceId serviceId) {
    return servicesById.get(serviceId);
  }

  @JsonProperty("services")
  public Collection<Service> getServices() {
    return servicesById.values();
  }

  @JsonProperty("services")
  public void setServices(Collection<Service> services) {
    this.servicesById = services.stream().collect(toMap(
      s -> s.getId(),
      s -> s
    ));
  }

  public Collection<Credential> getCredentials() {
    return credentials;
  }

  public void setCredentials(Collection<Credential> credentials) {
    this.credentials = credentials;
  }

  public SecurityConfiguration getSecurityConfiguration() {
    return securityConfiguration;
  }

  public void setSecurityConfiguration(SecurityConfiguration securityConfiguration) {
    this.securityConfiguration = securityConfiguration;
  }

  public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
    return configRecommendationStrategy;
  }

  public void setConfigRecommendationStrategy(ConfigRecommendationStrategy configRecommendationStrategy) {
    this.configRecommendationStrategy = configRecommendationStrategy;
  }

  public ProvisionAction getProvisionAction() {
    return provisionAction;
  }

  public void setProvisionAction(ProvisionAction provisionAction) {
    this.provisionAction = provisionAction;
  }

  @JsonProperty("host_groups")
  public Collection<HostGroup> getHostGroups() {
    return hostGroups.values();
  }

  public HostGroup getHostGroupByName(String name) {
    return hostGroups.get(name);
  }

  @JsonProperty("host_groups")
  public void setHostGroups(Collection<HostGroup> hostGroups) {
    this.hostGroups = hostGroups.stream().collect(toMap(
      hg -> hg.getName(),
      hg -> hg
    ));
  }

  public void validate() throws IllegalStateException {
    getHostGroups().forEach(HostGroup::validate);
  }

  public static class HostGroup implements Configurable {
    private String name;
    @JsonIgnore
    private Configuration configuration;
    private Collection<Host> hosts = Collections.emptyList();
    @JsonProperty("host_count")
    private int hostCount = 0;
    @JsonProperty("host_predicate")
    private String hostPredicate;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Override
    public Configuration getConfiguration() {
      return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
    }

    public Collection<Host> getHosts() {
      return hosts;
    }

    public void setHosts(Collection<Host> hosts) {
      this.hosts = hosts;
    }

    public int getHostCount() {
      return hostCount;
    }

    public void setHostCount(int hostCount) {
      this.hostCount = hostCount;
    }

    public String getHostPredicate() {
      return hostPredicate;
    }

    public void setHostPredicate(String hostPredicate) {
      this.hostPredicate = hostPredicate;
    }

    void validate() throws IllegalStateException {
      Preconditions.checkState((hostCount == 0 && null == hostPredicate) || getHosts().isEmpty(),
        "Invalid custer topology template. Host group %s must have either declatere its hosts or " +
          "hostcount (and optionally host predicate)", name);
    }
  }

  public static class Service implements Configurable {
    private String name;
    @JsonProperty("service_group")
    private String serviceGroup;
    @JsonIgnore
    private Configuration configuration;

    @Override
    public Configuration getConfiguration() {
      return configuration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
    }

    @JsonIgnore
    public ServiceId getId() {
      return new ServiceId(name, serviceGroup);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getServiceGroup() {
      return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
      this.serviceGroup = serviceGroup;
    }
  }

  public static class Host {
    private String fqdn;
    @JsonProperty("rack_info")
    private String  rackInfo;

    public String getFqdn() {
      return fqdn;
    }

    public void setFqdn(String fqdn) {
      this.fqdn = fqdn;
    }

    public String getRackInfo() {
      return rackInfo;
    }

    public void setRackInfo(String rackInfo) {
      this.rackInfo = rackInfo;
    }
  }

}
