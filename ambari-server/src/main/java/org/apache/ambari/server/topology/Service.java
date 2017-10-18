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


import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Service implements Configurable {

  private String type;

  private ServiceId id = new ServiceId();

  private String stackId;

  private Configuration configuration;

  private Set<ServiceId> dependencies;

  /**
   * Gets the name of this service
   *
   * @return component name
   */
  public String getName() {
    return this.id.getName();
  }

  public String getServiceGroup() {
    return this.id.getServiceGroup();
  }

  public String getType() {
    return type;
  }

  public String getStackId() {
    return stackId;
  }

  public Set<ServiceId> getDependencies() {
    return dependencies;
  }

  public Configuration getConfiguration() {
    return configuration;
  }


  public void setType(String type) {
    this.type = type;
  }

  public void setName(String name) {
    this.id.setName(name);
  }

  public void setServiceGroup(String serviceGroup) {
    this.id.setServiceGroup(serviceGroup);
  }

  @JsonProperty("stack_id")
  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setDependencies(Set<ServiceId> dependencies) {
    this.dependencies = dependencies;
  }

  public ServiceId getId() {
    return id;
  }

}
