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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Stack advisor request.
 */
public class StackAdvisorRequest {

  private String stackName;
  private String stackVersion;
  private List<String> hosts = new ArrayList<String>();
  private List<String> services = new ArrayList<String>();
  private Map<String, Set<String>> componentHostsMap = new HashMap<String, Set<String>>();

  public String getStackName() {
    return stackName;
  }

  public String getStackVersion() {
    return stackVersion;
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

    public StackAdvisorRequest build() {
      return this.instance;
    }
  }

}
