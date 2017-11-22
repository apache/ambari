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

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.controller.internal.ProvisionAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ComponentV2 implements Configurable {

  private String type;
  private String name;
  private ServiceId serviceId = ServiceId.NULL;
  private ProvisionAction provisionAction = ProvisionAction.INSTALL_AND_START;
  private Configuration configuration;
  private boolean masterComponent;

  @JsonIgnore
  private Service service;

  public ComponentV2() { }


  /**
   * Gets the name of this component
   *
   * @return component name
   */
  public String getName() {
    return this.name;
  }

  /** @return the masterComponent flag */
  public boolean isMasterComponent() {
    return masterComponent;
  }

  /**
   * Gets the provision action associated with this component.
   *
   * @return the provision action for this component, which
   *         may be null if the default action is to be used
   */
  public ProvisionAction getProvisionAction() {
    return this.provisionAction;
  }

  @JsonIgnore
  public ServiceId getServiceId() {
    return serviceId;
  }

  public Service getService() {
    return service;
  }

  //TODO
  @JsonIgnore
  public ServiceGroup getServiceGroup() {
    return null;
  }

  @JsonIgnore
  public Configuration getConfiguration() {
    return configuration;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
    if (null == this.name) {
      this.name = type;
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("service_group")
  public String getServiceGroupName() {
    return serviceId.getServiceGroup();
  }

  @JsonProperty("service_group")
  public void setServiceGroup(String serviceGroup) {
    this.serviceId = new ServiceId(this.serviceId.getName(), serviceGroup);
  }

  @JsonProperty("service_name")
  public void setServiceName(String serviceName) {
    this.serviceId = new ServiceId(serviceName, this.serviceId.getServiceGroup());
  }

  public String getServiceName() {
    return serviceId.getName();
  }

  @JsonProperty("provision_action")
  public void setProvisionAction(ProvisionAction provisionAction) {
    this.provisionAction = provisionAction;
  }

  @JsonIgnore
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  @JsonIgnore
  public void setMasterComponent(StackV2 stack) {
    this.masterComponent = stack.isMasterComponent(this.type);
  }

  public void setService(Service service) {
    this.service = service;
  }

  @Override
  public String toString() {
    return "Component{" +
      "serviceGroup=" + serviceId.getServiceGroup() + " " +
      "service=" + serviceId.getName() + " " +
      "component=" + type +
    "}";
  }
}
