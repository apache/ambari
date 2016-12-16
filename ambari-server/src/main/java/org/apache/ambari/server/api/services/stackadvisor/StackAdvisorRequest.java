/**
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

package org.apache.ambari.server.api.services.stackadvisor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.state.ChangedConfigInfo;
import org.apache.commons.lang.StringUtils;

/**
 * Stack advisor request.
 */
public class StackAdvisorRequest {

  private String stackName;
  private String stackVersion;
  private StackAdvisorRequestType requestType;
  private List<String> hosts = new ArrayList<String>();
  private List<String> services = new ArrayList<String>();
  private Map<String, Set<String>> componentHostsMap = new HashMap<String, Set<String>>();
  private Map<String, Set<String>> hostComponents = new HashMap<String, Set<String>>();
  private Map<String, Set<String>> hostGroupBindings = new HashMap<String, Set<String>>();
  private Map<String, Map<String, Map<String, String>>> configurations = new HashMap<String, Map<String, Map<String, String>>>();
  private List<ChangedConfigInfo> changedConfigurations = new LinkedList<ChangedConfigInfo>();
  private Set<RecommendationResponse.ConfigGroup> configGroups;

  public String getStackName() {
    return stackName;
  }

  public String getStackVersion() {
    return stackVersion;
  }

  public StackAdvisorRequestType getRequestType() {
    return requestType;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public List<String> getServices() {
    return services;
  }

  public Map<String, Set<String>> getComponentHostsMap() {
    return componentHostsMap;
  }

  public String getHostsCommaSeparated() {
    return StringUtils.join(hosts, ",");
  }

  public String getServicesCommaSeparated() {
    return StringUtils.join(services, ",");
  }

  public Map<String, Set<String>> getHostComponents() {
    return hostComponents;
  }

  public Map<String, Set<String>> getHostGroupBindings() {
    return hostGroupBindings;
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

  public Set<RecommendationResponse.ConfigGroup> getConfigGroups() {
    return configGroups;
  }

  public void setConfigGroups(Set<RecommendationResponse.ConfigGroup> configGroups) {
    this.configGroups = configGroups;
  }

  private StackAdvisorRequest(String stackName, String stackVersion) {
    this.stackName = stackName;
    this.stackVersion = stackVersion;
  }

  public static class StackAdvisorRequestBuilder {
    StackAdvisorRequest instance;

    private StackAdvisorRequestBuilder(String stackName, String stackVersion) {
      this.instance = new StackAdvisorRequest(stackName, stackVersion);
    }

    public static StackAdvisorRequestBuilder forStack(String stackName, String stackVersion) {
      return new StackAdvisorRequestBuilder(stackName, stackVersion);
    }

    public StackAdvisorRequestBuilder ofType(StackAdvisorRequestType requestType) {
      this.instance.requestType = requestType;
      return this;
    }

    public StackAdvisorRequestBuilder forHosts(List<String> hosts) {
      this.instance.hosts = hosts;
      return this;
    }

    public StackAdvisorRequestBuilder forServices(List<String> services) {
      this.instance.services = services;
      return this;
    }

    public StackAdvisorRequestBuilder withComponentHostsMap(
        Map<String, Set<String>> componentHostsMap) {
      this.instance.componentHostsMap = componentHostsMap;
      return this;
    }

    public StackAdvisorRequestBuilder forHostComponents(Map<String, Set<String>> hostComponents) {
      this.instance.hostComponents = hostComponents;
      return this;
    }

    public StackAdvisorRequestBuilder forHostsGroupBindings(
        Map<String, Set<String>> hostGroupBindings) {
      this.instance.hostGroupBindings = hostGroupBindings;
      return this;
    }

    public StackAdvisorRequestBuilder withConfigurations(
        Map<String, Map<String, Map<String, String>>> configurations) {
      this.instance.configurations = configurations;
      return this;
    }

    public StackAdvisorRequestBuilder withChangedConfigurations(
      List<ChangedConfigInfo> changedConfigurations) {
      this.instance.changedConfigurations = changedConfigurations;
      return this;
    }

    public StackAdvisorRequestBuilder withConfigGroups(
      Set<RecommendationResponse.ConfigGroup> configGroups) {
      this.instance.configGroups = configGroups;
      return this;
    }

    public StackAdvisorRequest build() {
      return this.instance;
    }
  }

  public enum StackAdvisorRequestType {
    HOST_GROUPS("host_groups"),
    CONFIGURATIONS("configurations"),
    CONFIGURATION_DEPENDENCIES("configuration-dependencies");

    private String type;

    private StackAdvisorRequestType(String type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return type;
    }

    public static StackAdvisorRequestType fromString(String text) throws StackAdvisorException {
      if (text != null) {
        for (StackAdvisorRequestType next : StackAdvisorRequestType.values()) {
          if (text.equalsIgnoreCase(next.type)) {
            return next;
          }
        }
      }
      throw new StackAdvisorException(String.format(
          "Unknown request type: %s, possible values: %s", text,
          Arrays.toString(StackAdvisorRequestType.values())));
    }
  }
}
