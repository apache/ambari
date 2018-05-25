package org.apache.ambari.server.api.services.mpackadvisor.validations;

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

import java.util.Set;

import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorResponse;
import org.apache.ambari.server.api.services.mpackadvisor.validations.MpackValidationResponse.ValidationItem;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Validation response POJO.
 */
public class MpackValidationResponse extends MpackAdvisorResponse {

  @JsonProperty
  private Set<ValidationItem> items;

  public Set<ValidationItem> getItems() {
    return items;
  }

  public void setItems(Set<ValidationItem> items) {
    this.items = items;
  }

  public static class ValidationItem {
    @JsonProperty
    private String type;

    @JsonProperty
    private String level;

    @JsonProperty
    private String message;

    @JsonProperty("mpack_instance_name")
    private String mpackInstanceName;

    @JsonProperty("mpack_instance_type")
    private String mpackInstanceType;

    @JsonProperty("mpack_version")
    private String mpackVersion;

    @JsonProperty("service_instance_name")
    private String serviceInstanceName;

    @JsonProperty("service_instance_type")
    private String serviceInstanceType;

    @JsonProperty("component_instance_name")
    private String componentInstanceName;

    @JsonProperty("component_instance_type")
    private String componentInstanceType;

    @JsonProperty
    private String host;

    @JsonProperty("config_type")
    private String configType;

    @JsonProperty("config_name")
    private String configName;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getLevel() {
      return level;
    }

    public void setLevel(String level) {
      this.level = level;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getMpackInstanceName() {
      return mpackInstanceName;
    }

    public void setMpackInstanceName(String mpackInstanceName) {
      this.mpackInstanceName = mpackInstanceName;
    }

    public String getMpackInstanceType() {
      return mpackInstanceType;
    }

    public void setMpackInstanceType(String mpackInstanceType) {
      this.mpackInstanceType = mpackInstanceType;
    }

    public String getMpackVersion() {
      return mpackVersion;
    }

    public void setMpackVersion(String mpackVersion) {
      this.mpackVersion = mpackVersion;
    }

    public String getServiceInstanceName() {
      return serviceInstanceName;
    }

    public void setServiceInstanceName(String serviceInstanceName) {
      this.serviceInstanceName = serviceInstanceName;
    }

    public String getServiceInstanceType() {
      return serviceInstanceType;
    }

    public void setServiceInstanceType(String serviceInstanceType) {
      this.serviceInstanceType = serviceInstanceType;
    }

    public String getComponentInstanceName() {
      return componentInstanceName;
    }

    public void setComponentInstanceName(String componentInstanceName) {
      this.componentInstanceName = componentInstanceName;
    }

    public String getComponentInstanceType() {
      return componentInstanceType;
    }

    public void setComponentInstanceType(String componentInstanceType) {
      this.componentInstanceType = componentInstanceType;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getConfigType() {
      return configType;
    }

    public void setConfigType(String configType) {
      this.configType = configType;
    }

    public String getConfigName() {
      return configName;
    }

    public void setConfigName(String configName) {
      this.configName = configName;
    }
  }

}