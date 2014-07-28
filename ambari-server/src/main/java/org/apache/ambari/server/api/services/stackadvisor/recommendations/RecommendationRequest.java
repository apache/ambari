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

package org.apache.ambari.server.api.services.stackadvisor.recommendations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Recommendations request.
 */
public class RecommendationRequest {

  private String stackName;
  private String stackVersion;
  private List<String> hosts = new ArrayList<String>();
  private List<String> services = new ArrayList<String>();


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

  public String getHostsCommaSeparated() {
    return StringUtils.join(hosts, ",");
  }

  public String getServicesCommaSeparated() {
    return StringUtils.join(services, ",");
  }

  private RecommendationRequest(String stackName, String stackVersion) {
    this.stackName = stackName;
    this.stackVersion = stackVersion;
  }

  public static class RecommendationRequestBuilder {
    RecommendationRequest instance;

    private RecommendationRequestBuilder(String stackName, String stackVersion) {
      this.instance = new RecommendationRequest(stackName, stackVersion);
    }

    public static RecommendationRequestBuilder forStack(String stackName, String stackVersion) {
      return new RecommendationRequestBuilder(stackName, stackVersion);
    }

    public RecommendationRequestBuilder forHosts(List<String> hosts) {
      this.instance.hosts = hosts;
      return this;
    }

    public RecommendationRequestBuilder forServices(List<String> services) {
      this.instance.services = services;
      return this;
    }

    public RecommendationRequest build() {
      return this.instance;
    }
  }
}
