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

package org.apache.ambari.server.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configurable;
import org.apache.ambari.server.topology.Configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Data object representing an add service request.
 */
@ApiModel
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class AddServiceRequest {

  static final String OPEATION_TYPE = "operation_type";
  static final String CONFIG_RECOMMENDATION_STRATEGY = "config_recommendation_strategy";
  static final String PROVISION_ACTION = "provision_action";
  static final String STACK_NAME = "stack_name";
  static final String STACK_VERSION = "stack_version";
  static final String SERVICES = "services";
  static final String COMPONENTS = "components";

  private final OperationType operationType;
  private final ConfigRecommendationStrategy recommendationStrategy;
  private final ProvisionAction provisionAction;
  private final String stackName;
  private final String stackVersion;
  private final Set<Service> services;
  private final Set<Component> components;
  private final Configuration configuration;

  @JsonCreator
  public AddServiceRequest(@JsonProperty(value = OPEATION_TYPE, required = true) OperationType operationType,
                           @JsonProperty(CONFIG_RECOMMENDATION_STRATEGY) ConfigRecommendationStrategy recommendationStrategy,
                           @JsonProperty(PROVISION_ACTION)ProvisionAction provisionAction,
                           @JsonProperty(STACK_NAME) String stackName,
                           @JsonProperty(STACK_VERSION) String stackVersion,
                           @JsonProperty(SERVICES) Set<Service> services,
                           @JsonProperty(COMPONENTS)Set<Component> components,
                           @JsonProperty(Configurable.CONFIGURATIONS) Collection<? extends Map<String, ?>> configs) {
    this(operationType, recommendationStrategy, provisionAction, stackName, stackVersion, services, components,
      Configurable.parseConfigs(configs));
  }


  private AddServiceRequest(OperationType operationType,
                            ConfigRecommendationStrategy recommendationStrategy,
                            ProvisionAction provisionAction,
                            String stackName,
                            String stackVersion,
                            Set<Service> services,
                            Set<Component> components,
                            Configuration configuration) {
    this.operationType = operationType;
    this.recommendationStrategy = null != recommendationStrategy ? recommendationStrategy : ConfigRecommendationStrategy.NEVER_APPLY;
    this.provisionAction = null != provisionAction ? provisionAction : ProvisionAction.INSTALL_AND_START;
    this.stackName = stackName;
    this.stackVersion = stackVersion;
    this.services = null != services ? services : emptySet();
    this.components = null != components ? components : emptySet();
    this.configuration = null != configuration ? configuration : new Configuration(new HashMap<>(), new HashMap<>());

    checkNotNull(operationType, "operationType is mandatory");
    checkArgument(!this.services.isEmpty() || !this.components.isEmpty(), "Either services or components must be specified");
  }


  @JsonProperty(value = OPEATION_TYPE, required = true)
  @ApiModelProperty(name = OPEATION_TYPE)
  public OperationType getOperationType() {
    return operationType;
  }

  @JsonProperty(CONFIG_RECOMMENDATION_STRATEGY)
  @ApiModelProperty(name = CONFIG_RECOMMENDATION_STRATEGY)
  public ConfigRecommendationStrategy getRecommendationStrategy() {
    return recommendationStrategy;
  }

  @JsonProperty(PROVISION_ACTION)
  @ApiModelProperty(name = PROVISION_ACTION)
  public ProvisionAction getProvisionAction() {
    return provisionAction;
  }

  @JsonProperty(STACK_NAME)
  @ApiModelProperty(name = STACK_NAME)
  public String getStackName() {
    return stackName;
  }

  @JsonProperty(STACK_VERSION)
  @ApiModelProperty(name = STACK_VERSION)
  public String getStackVersion() {
    return stackVersion;
  }

  @JsonProperty(SERVICES)
  @ApiModelProperty(name = SERVICES)
  public Set<Service> getServices() {
    return services;
  }

  @JsonProperty(COMPONENTS)
  @ApiModelProperty(name = COMPONENTS)
  public Set<Component> getComponents() {
    return components;
  }

  @JsonIgnore
  @ApiIgnore
  public Configuration getConfiguration() {
    return configuration;
  }

  @JsonProperty(Configurable.CONFIGURATIONS)
  @ApiModelProperty(name = Configurable.CONFIGURATIONS)
  public Collection<Map<String, Map<String, ?>>> getConfigurationContents() {
    return Configurable.convertConfigToMap(configuration);
  }

// ------- inner classes -------

  public enum OperationType {
    ADD_SERVICE, DELETE_SERVICE, MOVE_SERVICE
  }

  public static class Component {
    static final String COMPONENT_NAME = "component_name";
    static final String FQDN = "fqdn";

    private String name;
    private String fqdn;

    public static final Component of(String name, String fqdn) {
      Component component = new Component();
      component.setName(name);
      component.setFqdn(fqdn);
      return component;
    }

    @JsonProperty(COMPONENT_NAME)
    @ApiModelProperty(name = COMPONENT_NAME)
    public String getName() {
      return name;
    }

    @JsonProperty(COMPONENT_NAME)
    public void setName(String name) {
      this.name = name;
    }

    @JsonProperty(FQDN)
    @ApiModelProperty(name = FQDN)
    public String getFqdn() {
      return fqdn;
    }

    @JsonProperty(FQDN)
    public void setFqdn(String fqdn) {
      this.fqdn = fqdn;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Component component = (Component) o;
      return Objects.equals(name, component.name) &&
        Objects.equals(fqdn, component.fqdn);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, fqdn);
    }
  }

  @ApiModel
  public static class Service {
    static final String NAME = "name";

    private String name;

    public static final Service of(String name) {
      Service service = new Service();
      service.setName(name);
      return service;
    }

    @JsonProperty(NAME)
    @ApiModelProperty(name = NAME)
    public String getName() {
      return name;
    }

    @JsonProperty(NAME)
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Service service = (Service) o;
      return Objects.equals(name, service.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }
}
