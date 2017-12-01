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
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.ambari.server.controller.internal.ProvisionAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class TopologyTemplate {

  private String blueprint;
  private String defaultPassword;
  private ConfigRecommendationStrategy configRecommendationStrategy = ConfigRecommendationStrategy.NEVER_APPLY;
  private ProvisionAction provisionAction = ProvisionAction.INSTALL_AND_START;
  private Map<ServiceId, TopologyTemplate.Service> servicesById = Collections.emptyMap();
  private Map<String, TopologyTemplate.HostGroup> hostGroups = Collections.emptyMap();
  private Collection<Credential> credentials = Collections.emptySet();
  private SecurityConfiguration securityConfiguration = SecurityConfiguration.NONE;

  public String getBlueprint() {
    return blueprint;
  }

  @JsonProperty("blueprint")
  public void setBlueprint(String blueprint) {
    this.blueprint = blueprint;
  }

  public String getDefaultPassword() {
    return defaultPassword;
  }

  @JsonProperty("default_password")
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
    servicesById = services.stream().collect(toMap(Service::getId, Function.identity()));
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

  @JsonProperty("security")
  public void setSecurityConfiguration(SecurityConfiguration securityConfiguration) {
    this.securityConfiguration = securityConfiguration;
  }

  public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
    return configRecommendationStrategy;
  }

  @JsonProperty("config_recommendation_strategy")
  public void setConfigRecommendationStrategy(ConfigRecommendationStrategy configRecommendationStrategy) {
    this.configRecommendationStrategy = configRecommendationStrategy;
  }

  public ProvisionAction getProvisionAction() {
    return provisionAction;
  }

  @JsonProperty("provision_action")
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
    this.hostGroups = hostGroups.stream().collect(toMap(HostGroup::getName, Function.identity()));
  }

  public void validate() throws IllegalStateException {
    Preconditions.checkArgument(hostGroups != null && !hostGroups.isEmpty(), "At least one host group must be specified");
    getHostGroups().forEach(HostGroup::validate);
    getServices().forEach(Service::validate);
    getCredentials().forEach(Credential::validate);
  }

  public static class HostGroup implements Configurable {
    private String name;
    private Configuration configuration = Configuration.createEmpty();
    private Collection<Host> hosts = Collections.emptyList();
    private int hostCount;
    private String hostPredicate;

    public String getName() {
      return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public Configuration getConfiguration() {
      return configuration;
    }

    @JsonIgnore
    @Override
    public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
    }

    public Collection<Host> getHosts() {
      return hosts;
    }

    @JsonProperty("hosts")
    public void setHosts(Collection<Host> hosts) {
      this.hosts = hosts;
    }

    public int getHostCount() {
      return hostCount;
    }

    @JsonProperty("host_count")
    public void setHostCount(int hostCount) {
      this.hostCount = hostCount;
    }

    public String getHostPredicate() {
      return hostPredicate;
    }

    @JsonProperty("host_predicate")
    public void setHostPredicate(String hostPredicate) {
      this.hostPredicate = hostPredicate;
    }

    void validate() throws IllegalStateException {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Host group name must be specified");
      Preconditions.checkArgument(hostCount > 0 || !hosts.isEmpty(),
        "Host group '%s' must contain either 'hosts' or 'host_count'", name);
      Preconditions.checkArgument(hostCount == 0 || hosts.isEmpty(),
        "Host group '%s' must not contain both 'hosts' and 'host_count'", name);
      Preconditions.checkArgument(hostPredicate == null || hostCount > 0,
        "Host group '%s' must not specify 'host_predicate' without 'host_count'", name);

      hosts.forEach(Host::validate);
    }
  }

  public static class Service implements Configurable {
    private String name;
    private String serviceGroup;
    private Configuration configuration = Configuration.createEmpty();

    @Override
    public Configuration getConfiguration() {
      return configuration;
    }

    @JsonIgnore
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

    @JsonProperty("name")
    public void setName(String name) {
      this.name = name;
    }

    public String getServiceGroup() {
      return serviceGroup;
    }

    @JsonProperty("service_group")
    public void setServiceGroup(String serviceGroup) {
      this.serviceGroup = serviceGroup;
    }

    public void validate() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Service name must be specified");
      Preconditions.checkArgument(!Strings.isNullOrEmpty(serviceGroup), "Service group name must be specified for service %s", name);
    }
  }

  public static class Host {
    private String fqdn;
    private String rackInfo;

    public String getFqdn() {
      return fqdn;
    }

    @JsonProperty("fqdn")
    public void setFqdn(String fqdn) {
      this.fqdn = fqdn;
    }

    public String getRackInfo() {
      return rackInfo;
    }

    @JsonProperty("rack_info")
    public void setRackInfo(String rackInfo) {
      this.rackInfo = rackInfo;
    }

    public void validate() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(fqdn), "Host name must be specified");
    }
  }

}
