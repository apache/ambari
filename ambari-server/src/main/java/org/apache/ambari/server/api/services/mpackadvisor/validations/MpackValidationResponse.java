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
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Validation response POJO.
 */
public class MpackValidationResponse extends MpackAdvisorResponse {

  @JsonProperty
  private Set<org.apache.ambari.server.api.services.mpackadvisor.validations.MpackValidationResponse.ValidationItem> items;

  public Set<org.apache.ambari.server.api.services.mpackadvisor.validations.MpackValidationResponse.ValidationItem> getItems() {
    return items;
  }

  public void setItems(Set<org.apache.ambari.server.api.services.mpackadvisor.validations.MpackValidationResponse.ValidationItem> items) {
    this.items = items;
  }

  public static class ValidationItem {
    @JsonProperty
    private String type;

    @JsonProperty
    private String level;

    @JsonProperty
    private String message;

    @JsonProperty("mpack-name")
    private String mpackName;

    @JsonProperty("mpack-version")
    private String mpackVersion;

    @JsonProperty("component-name")
    private String componentName;

    @JsonProperty
    private String host;

    @JsonProperty("config-type")
    private String configType;

    @JsonProperty("config-name")
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

    public String getMpackName() {
      return mpackName;
    }

    public void setMpackName(String mpackName) {
      this.mpackName = mpackName;
    }

    public String getMpackVersion() {
      return mpackVersion;
    }

    public void setMpackVersion(String mpackVersion) {
      this.mpackVersion = mpackVersion;
    }

    public String getComponentName() {
      return componentName;
    }

    public void setComponentName(String componentName) {
      this.componentName = componentName;
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