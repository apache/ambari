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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.controller.AddServiceRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AddServiceOrchestrator {

  private static final Logger LOG = LoggerFactory.getLogger(AddServiceOrchestrator.class);

  @Inject
  private ResourceProviderAdapter resourceProviders;

  @Inject
  private AmbariManagementController controller;

  @Inject
  private ActionManager actionManager;

  @Inject
  private RequestFactory requestFactory;

  public RequestStatusResponse processAddServiceRequest(Cluster cluster, AddServiceRequest request) {
    LOG.info("Received {} request for {}: {}", request.getOperationType(), cluster.getClusterName(), request);

    AddServiceInfo validatedRequest = validate(cluster, request);
    AddServiceInfo requestWithLayout = recommendLayout(validatedRequest);
    AddServiceInfo requestWithConfig = recommendConfiguration(requestWithLayout);

    createResources(requestWithConfig);
    createHostTasks(requestWithConfig);

    return requestWithConfig.getStages().getRequestStatusResponse();
  }

  /**
   * Performs basic validation of the request and
   * fills in details about the requested services and components.
   *
   * @return validated information about the requested services
   */
  private AddServiceInfo validate(Cluster cluster, AddServiceRequest request) {
    LOG.info("Validating {}", request);

    Map<String, Map<String, Set<String>>> newServices = new LinkedHashMap<>();

    StackId stackId = new StackId(request.getStackName(), request.getStackVersion());
    Stack stack;
    try {
      stack = new Stack(stackId, controller);
      Set<String> existingServices = cluster.getServices().keySet();
      for (AddServiceRequest.Component requestedComponent : request.getComponents()) {
        String serviceName = stack.getServiceForComponent(requestedComponent.getName());
        if (serviceName == null) {
          String msg = String.format("No service found for component %s in stack %s", requestedComponent.getName(), stackId);
          LOG.error(msg);
          throw new IllegalArgumentException(msg);
        }
        if (existingServices.contains(serviceName)) {
          String msg = String.format("Service %s already exists in cluster %s", serviceName, cluster.getClusterName());
          LOG.error(msg);
          throw new IllegalArgumentException(msg);
        }

        newServices.computeIfAbsent(serviceName, __ -> new HashMap<>())
          .computeIfAbsent(requestedComponent.getName(), __ -> new HashSet<>())
          .add(requestedComponent.getFqdn());
      }
    } catch (AmbariException e) {
      LOG.error("Stack {} not found", stackId);
      throw new IllegalArgumentException(e);
    }

    if (newServices.isEmpty()) {
      throw new IllegalArgumentException("No new services to be added");
    }

    Configuration config = stack.getValidDefaultConfig();
    // TODO add user-defined config

    RequestStageContainer stages = new RequestStageContainer(actionManager.getNextRequestId(), null, requestFactory, actionManager);
    AddServiceInfo validatedRequest = new AddServiceInfo(cluster.getClusterName(), stack, config, stages, newServices);
    stages.setRequestContext(validatedRequest.describe());
    return validatedRequest;
  }

  /**
   * Requests layout recommendation from the stack advisor.
   * @return new request, updated based on the recommended layout
   * @throws IllegalArgumentException if the request cannot be satisfied
   */
  private AddServiceInfo recommendLayout(AddServiceInfo request) {
    LOG.info("Recommending layout for {}", request);
    // TODO implement
    return request;
  }

  /**
   * Requests config recommendation from the stack advisor.
   * @return new request, updated with the recommended config
   * @throws IllegalArgumentException if the request cannot be satisfied
   */
  private AddServiceInfo recommendConfiguration(AddServiceInfo request) {
    LOG.info("Recommending configuration for {}", request);
    // TODO implement
    return request;
  }

  /**
   * Creates the service, component and host component resources for the request.
   */
  private void createResources(AddServiceInfo request) {
    LOG.info("Creating resources for {}", request);
    resourceProviders.createServices(request);
    resourceProviders.createComponents(request);
    resourceProviders.createConfigs(request);
    resourceProviders.updateServiceDesiredState(request, State.INSTALLED);
    resourceProviders.updateServiceDesiredState(request, State.STARTED);
    resourceProviders.createHostComponents(request);
  }

  private void createHostTasks(AddServiceInfo request) {
    LOG.info("Creating host tasks for {}", request);

    resourceProviders.updateHostComponentDesiredState(request, State.INSTALLED);
    resourceProviders.updateHostComponentDesiredState(request, State.STARTED);
    try {
      request.getStages().persist();
    } catch (AmbariException e) {
      String msg = String.format("Error creating host tasks for %s", request);
      LOG.error(msg, e);
      throw new IllegalStateException(msg, e);
    }
  }

}
