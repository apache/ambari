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

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.controller.AddServiceRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.internal.UnitUpdater;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.StackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;

/**
 * Validates a specific {@link AddServiceRequest}.
 */
public class RequestValidator {

  private static final Logger LOG = LoggerFactory.getLogger(RequestValidator.class);

  private static final Set<String> NOT_ALLOWED_CONFIG_TYPES = ImmutableSet.of("kerberos-env", "krb5-conf");

  private final AddServiceRequest request;
  private final Cluster cluster;
  private final AmbariManagementController controller;
  private final ConfigHelper configHelper;
  private final StackFactory stackFactory;
  private final AtomicBoolean serviceInfoCreated = new AtomicBoolean();

  private State state;

  @Inject
  public RequestValidator(
    @Assisted AddServiceRequest request, @Assisted Cluster cluster,
    AmbariManagementController controller, ConfigHelper configHelper,
    StackFactory stackFactory
  ) {
    this.state = State.INITIAL;
    this.request = request;
    this.cluster = cluster;
    this.controller = controller;
    this.configHelper = configHelper;
    this.stackFactory = stackFactory;
  }

  /**
   * Perform validation of the request.
   */
  void validate() {
    validateSecurity();
    validateStack();
    validateServicesAndComponents();
    validateHosts();
    validateConfiguration();
  }

  /**
   * Create an {@link AddServiceInfo} based on the validated request.
   */
  AddServiceInfo createValidServiceInfo(ActionManager actionManager, RequestFactory requestFactory) {
    final State state = this.state;

    checkState(state.isValid(), "The request needs to be validated first");
    checkState(!serviceInfoCreated.getAndSet(true), "Can create only one instance for each validated add service request");

    RequestStageContainer stages = new RequestStageContainer(actionManager.getNextRequestId(), null, requestFactory, actionManager);
    AddServiceInfo validatedRequest = new AddServiceInfo(request, cluster.getClusterName(), state.getStack(), state.getConfig(), stages, state.getNewServices(), null);
    stages.setRequestContext(validatedRequest.describe());
    return validatedRequest;
  }

  @VisibleForTesting
  State getState() {
    return state;
  }

  @VisibleForTesting
  void setState(State state) {
    this.state = state;
  }

  @VisibleForTesting
  void validateSecurity() {
    request.getSecurity().ifPresent(requestSecurity ->
      checkArgument(requestSecurity.getType() == cluster.getSecurityType(),
        "Security type in the request (%s), if specified, should match cluster's security type (%s)",
        requestSecurity.getType(), cluster.getSecurityType()
      )
    );
  }

  @VisibleForTesting
  void validateStack() {
    Optional<StackId> requestStackId = request.getStackId();
    StackId stackId = requestStackId.orElseGet(cluster::getCurrentStackVersion);
    try {
      Stack stack = stackFactory.createStack(stackId.getStackName(), stackId.getStackVersion(), controller);
      state = state.with(stack);
    } catch (AmbariException e) {
      logAndThrow(requestStackId.isPresent()
        ? msg -> new IllegalArgumentException(msg, e)
        : IllegalStateException::new,
        "Stack %s not found", stackId
      );
    }
  }

  @VisibleForTesting
  void validateServicesAndComponents() {
    Stack stack = state.getStack();
    Map<String, Map<String, Set<String>>> newServices = new LinkedHashMap<>();

    Set<String> existingServices = cluster.getServices().keySet();

    // process service declarations
    for (AddServiceRequest.Service service : request.getServices()) {
      String serviceName = service.getName();

      checkArgument(stack.getServices().contains(serviceName),
        "Unknown service %s in %s", service, stack);
      checkArgument(!existingServices.contains(serviceName),
        "Service %s already exists in cluster %s", serviceName, cluster.getClusterName());

      newServices.computeIfAbsent(serviceName, __ -> new HashMap<>());
    }

    // process component declarations
    for (AddServiceRequest.Component requestedComponent : request.getComponents()) {
      String componentName = requestedComponent.getName();
      String serviceName = stack.getServiceForComponent(componentName);

      checkArgument(serviceName != null,
        "No service found for component %s in %s", componentName, stack);
      checkArgument(!existingServices.contains(serviceName),
        "Service %s (for component %s) already exists in cluster %s", serviceName, componentName, cluster.getClusterName());

      newServices.computeIfAbsent(serviceName, __ -> new HashMap<>())
        .computeIfAbsent(componentName, __ -> new HashSet<>())
        .add(requestedComponent.getFqdn());
    }

    checkArgument(!newServices.isEmpty(), "Request should have at least one new service or component to be added");

    state = state.withNewServices(newServices);
  }

  @VisibleForTesting
  void validateConfiguration() {
    Configuration config = request.getConfiguration();

    for (String type : NOT_ALLOWED_CONFIG_TYPES) {
      checkArgument(!config.getProperties().containsKey(type), "Cannot change '%s' configuration in Add Service request", type);
    }

    Configuration clusterConfig = getClusterDesiredConfigs();
    clusterConfig.setParentConfiguration(state.getStack().getDefaultConfig());
    config.setParentConfiguration(clusterConfig);

    UnitUpdater.removeUnits(config, state.getStack()); // stack advisor doesn't like units; they'll be added back after recommendation
    state = state.with(config);
  }

  @VisibleForTesting
  void validateHosts() {
    Set<String> clusterHosts = cluster.getHostNames();
    Set<String> requestHosts = state.getNewServices().values().stream()
      .flatMap(componentHosts -> componentHosts.values().stream())
      .flatMap(Collection::stream)
      .collect(toSet());
    Set<String> unknownHosts = new TreeSet<>(Sets.difference(requestHosts, clusterHosts));

    checkArgument(unknownHosts.isEmpty(),
      "Requested host not associated with cluster %s: %s", cluster.getClusterName(), unknownHosts);
  }

  private Configuration getClusterDesiredConfigs() {
    try {
      return Configuration.of(configHelper.calculateExistingConfigs(cluster));
    } catch (AmbariException e) {
      logAndThrow(msg -> new IllegalStateException(msg, e), "Error getting effective configuration of cluster %s", cluster.getClusterName());
      return Configuration.newEmpty(); // unreachable
    }
  }

  private static void checkArgument(boolean expression, String errorMessage, Object... messageParams) {
    if (!expression) {
      logAndThrow(IllegalArgumentException::new, errorMessage, messageParams);
    }
  }

  private static void checkState(boolean expression, String errorMessage, Object... messageParams) {
    if (!expression) {
      logAndThrow(IllegalStateException::new, errorMessage, messageParams);
    }
  }

  private static void logAndThrow(Function<String, RuntimeException> exceptionCreator, String errorMessage, Object... messageParams) {
    String msg = String.format(errorMessage, messageParams);
    LOG.error(msg);
    throw exceptionCreator.apply(msg);
  }

  @VisibleForTesting
  static class State {

    static final State INITIAL = new State(null, null, null);

    private final Stack stack;
    private final Map<String, Map<String, Set<String>>> newServices;
    private final Configuration config;

    State(Stack stack, Map<String, Map<String, Set<String>>> newServices, Configuration config) {
      this.stack = stack;
      this.newServices = newServices;
      this.config = config;
    }

    boolean isValid() {
      return stack != null && newServices != null && config != null;
    }

    State with(Stack stack) {
      return new State(stack, newServices, config);
    }

    State withNewServices(Map<String, Map<String, Set<String>>> newServices) {
      return new State(stack, newServices, config);
    }

    State with(Configuration config) {
      return new State(stack, newServices, config);
    }

    Stack getStack() {
      return stack;
    }

    Map<String, Map<String, Set<String>>> getNewServices() {
      return newServices;
    }

    Configuration getConfig() {
      return config;
    }
  }

}
