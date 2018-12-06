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
package org.apache.ambari.server.topology.addservice;

import static java.util.stream.Collectors.joining;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.controller.AddServiceRequest;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.topology.Configuration;

/**
 * Processed info for adding new services/components to an existing cluster.
 */
public final class AddServiceInfo {

  private final AddServiceRequest request;
  private final String clusterName;
  private final Stack stack;
  private final KerberosDescriptor kerberosDescriptor;
  private final Map<String, Map<String, Set<String>>> newServices;
  private final RequestStageContainer stages;
  private final Configuration config;

  public AddServiceInfo(
    AddServiceRequest request,
    String clusterName,
    Stack stack,
    Configuration config,
    KerberosDescriptor kerberosDescriptor,
    RequestStageContainer stages,
    Map<String, Map<String, Set<String>>> newServices
  ) {
    this.request = request;
    this.clusterName = clusterName;
    this.stack = stack;
    this.kerberosDescriptor = kerberosDescriptor;
    this.newServices = newServices;
    this.stages = stages;
    this.config = config;
  }

  public AddServiceInfo withNewServices(Map<String, Map<String, Set<String>>> services) {
    return new AddServiceInfo(request, clusterName, stack, config, kerberosDescriptor, stages, services);
  }

  @Override
  public String toString() {
    return "AddServiceRequest(" + stages.getId() + ")";
  }

  public AddServiceRequest getRequest() {
    return request;
  }

  public String clusterName() {
    return clusterName;
  }

  public RequestStageContainer getStages() {
    return stages;
  }

  /**
   * New services to be added to the cluster: service -> component -> host
   * This should include both explicitly requested services, and services of the requested components.
   */
  public Map<String, Map<String, Set<String>>> newServices() {
    return newServices;
  }

  public Stack getStack() {
    return stack;
  }

  public Configuration getConfig() {
    return config;
  }

  public Optional<KerberosDescriptor> getKerberosDescriptor() {
    return Optional.ofNullable(kerberosDescriptor);
  }

  /**
   * Creates a descriptive label to be displayed in the UI.
   */
  public String describe() {
    int maxServicesToShow = 3;
    StringBuilder sb = new StringBuilder("Add Services: ")
      .append(newServices.keySet().stream().sorted().limit(maxServicesToShow).collect(joining(", ")));
    if (newServices.size() > maxServicesToShow) {
      sb.append(" and ").append(newServices.size() - maxServicesToShow).append(" more");
    }
    sb.append(" to cluster ").append(clusterName);
    return sb.toString();
  }

}
