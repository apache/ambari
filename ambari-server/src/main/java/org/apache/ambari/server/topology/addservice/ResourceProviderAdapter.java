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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.internal.CredentialResourceProvider;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Creates resources using the resource providers.
 * Translates {@link AddServiceInfo} to internal requests accepted by those.
 */
@Singleton
public class ResourceProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceProviderAdapter.class);

  @Inject
  private AmbariManagementController controller;

  public void createServices(AddServiceInfo request) {
    LOG.info("Creating service resources for {}", request);

    Set<Map<String, Object>> properties = request.newServices().keySet().stream()
      .map(service -> createServiceRequestProperties(request, service))
      .collect(toSet());

    createResources(request, properties, Resource.Type.Service, false);
  }

  public void createComponents(AddServiceInfo request) {
    LOG.info("Creating component resources for {}", request);

    Set<Map<String, Object>> properties = request.newServices().entrySet().stream()
      .flatMap(componentsOfService -> componentsOfService.getValue().keySet().stream()
        .map(component -> createComponentRequestProperties(request, componentsOfService.getKey(), component)))
      .collect(toSet());

    createResources(request, properties, Resource.Type.Component, false);
  }

  public void createHostComponents(AddServiceInfo request) {
    LOG.info("Creating host component resources for {}", request);

    Set<Map<String, Object>> properties = request.newServices().entrySet().stream()
      .flatMap(componentsOfService -> componentsOfService.getValue().entrySet().stream()
        .flatMap(hostsOfComponent -> hostsOfComponent.getValue().stream()
          .map(host -> createHostComponentRequestProperties(request, componentsOfService.getKey(), hostsOfComponent.getKey(), host))))
      .collect(toSet());

    createResources(request, properties, Resource.Type.HostComponent, false);
  }

  public void createConfigs(AddServiceInfo request) {
    LOG.info("Creating configurations for {}", request);
    Set<ClusterRequest> requests = createConfigRequestsForNewServices(request);
    updateCluster(request, requests, "Error creating configurations for %s");
  }

  public void createCredentials(AddServiceInfo request) {
    if (!request.getRequest().getCredentials().isEmpty()) {
      LOG.info("Creating {} credential(s) for {}", request.getRequest().getCredentials().size(), request);

      request.getRequest().getCredentials().values().stream()
        .peek(credential -> LOG.debug("Creating credential {}", credential))
        .map(credential -> createCredentialRequestProperties(request.clusterName(), credential))
        .forEach(
          properties -> createResources(request, ImmutableSet.of(properties), Resource.Type.Credential, true)
        );
    }
  }

  public void updateExistingConfigs(AddServiceInfo request, Set<String> existingServices) {
    LOG.info("Updating existing configurations for {}", request);
    Set<ClusterRequest> requests = createConfigRequestsForExistingServices(request, existingServices);
    updateCluster(request, requests, "Error updating configurations for %s");
  }

  public void updateServiceDesiredState(AddServiceInfo request, State desiredState) {
    LOG.info("Updating service desired state to {} for {}", desiredState, request);

    Set<Map<String, Object>> properties = ImmutableSet.of(ImmutableMap.of(
      ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, desiredState.name()
    ));
    updateResources(request, properties, Resource.Type.Service, predicateForNewServices(request, "ServiceInfo"));
  }

  public void updateHostComponentDesiredState(AddServiceInfo request, State desiredState) {
    LOG.info("Updating host component desired state to {} for {}", desiredState, request);

    Set<Map<String, Object>> properties = ImmutableSet.of(ImmutableMap.of(
      HostComponentResourceProvider.STATE, desiredState.name(),
      "context", String.format("Put new components to %s state", desiredState)
    ));
    HostComponentResourceProvider rp = (HostComponentResourceProvider) getClusterController().ensureResourceProvider(Resource.Type.HostComponent);
    Request internalRequest = createRequest(request.clusterName(), properties, Resource.Type.HostComponent);
    try {
      rp.doUpdateResources(request.getStages(), internalRequest, predicateForNewServices(request, HostComponentResourceProvider.HOST_ROLES), false, false, false);
    } catch (UnsupportedPropertyException | SystemException | NoSuchParentResourceException | NoSuchResourceException e) {
      String msg = String.format("Error updating host component desired state for %s", request);
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

  private static void createResources(AddServiceInfo request, Set<Map<String, Object>> properties, Resource.Type resourceType, boolean okIfExists) {
    Request internalRequest = new RequestImpl(null, properties, null, null);
    ResourceProvider rp = getClusterController().ensureResourceProvider(resourceType);
    try {
      rp.createResources(internalRequest);
    } catch (UnsupportedPropertyException | SystemException | ResourceAlreadyExistsException | NoSuchParentResourceException e) {
      if (okIfExists && e instanceof ResourceAlreadyExistsException) {
        LOG.info("Resource already exists: {}, no need to create", e.getMessage());
      } else {
        String msg = String.format("Error creating resources %s for %s", resourceType, request);
        LOG.error(msg, e);
        throw new RuntimeException(msg, e);
      }
    }
  }

  private static void updateResources(AddServiceInfo request, Set<Map<String, Object>> properties, Resource.Type resourceType, Predicate predicate) {
    Request internalRequest = createRequest(request.clusterName(), properties, resourceType);
    ResourceProvider rp = getClusterController().ensureResourceProvider(resourceType);
    try {
      rp.updateResources(internalRequest, predicate);
    } catch (UnsupportedPropertyException | SystemException | NoSuchParentResourceException | NoSuchResourceException e) {
      String msg = String.format("Error updating resources %s for %s", resourceType, request);
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

  private void updateCluster(AddServiceInfo addServiceRequest, Set<ClusterRequest> requests, String errorMessageFormat) {
    try {
      controller.updateClusters(requests, null);
    } catch (AmbariException | AuthorizationException e) {
      String msg = String.format(errorMessageFormat, addServiceRequest);
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

  private static Request createRequest(String clusterName, Set<Map<String, Object>> properties, Resource.Type resourceType) {
    Map<String, String> requestInfoProperties = ImmutableMap.of(
      RequestOperationLevel.OPERATION_LEVEL_ID, RequestOperationLevel.getExternalLevelName(resourceType.name()),
      RequestOperationLevel.OPERATION_CLUSTER_ID, clusterName
    );
    return new RequestImpl(null, properties, requestInfoProperties, null);
  }

  private static Map<String, Object> createServiceRequestProperties(AddServiceInfo request, String service) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, request.clusterName());
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, service);
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, State.INIT.name());

    return properties.build();
  }

  private static Map<String, Object> createComponentRequestProperties(AddServiceInfo request, String service, String component) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(ComponentResourceProvider.CLUSTER_NAME, request.clusterName());
    properties.put(ComponentResourceProvider.SERVICE_NAME, service);
    properties.put(ComponentResourceProvider.COMPONENT_NAME, component);
    properties.put(ComponentResourceProvider.STATE, State.INIT.name());

    return properties.build();
  }

  private static Map<String, Object> createHostComponentRequestProperties(AddServiceInfo request, String service, String component, String host) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(HostComponentResourceProvider.CLUSTER_NAME, request.clusterName());
    properties.put(HostComponentResourceProvider.SERVICE_NAME, service);
    properties.put(HostComponentResourceProvider.COMPONENT_NAME, component);
    properties.put(HostComponentResourceProvider.HOST_NAME, host);
    properties.put(HostComponentResourceProvider.STATE, State.INIT.name());

    return properties.build();
  }

  public static Map<String, Object> createCredentialRequestProperties(String clusterName, Credential credential) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID, credential.getAlias());
    properties.put(CredentialResourceProvider.CREDENTIAL_PRINCIPAL_PROPERTY_ID, credential.getPrincipal());
    properties.put(CredentialResourceProvider.CREDENTIAL_KEY_PROPERTY_ID, credential.getKey());
    properties.put(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID, credential.getType().name());

    return properties.build();
  }

  private static Set<ClusterRequest> createConfigRequestsForNewServices(AddServiceInfo request) {
    Map<String, Map<String, String>> fullProperties = request.getConfig().getFullProperties();
    Map<String, Map<String, Map<String, String>>> fullAttributes = request.getConfig().getFullAttributes();

    return createConfigRequestsForServices(
      request.newServices().keySet(),
      configType -> !Objects.equals(configType, ConfigHelper.CLUSTER_ENV),
      request, fullProperties, fullAttributes
    );
  }

  private Set<ClusterRequest> createConfigRequestsForExistingServices(AddServiceInfo request, Set<String> existingServices) {
    Set<String> configTypesInRequest = ImmutableSet.copyOf(
      Sets.difference(
        Sets.union(
          request.getConfig().getProperties().keySet(),
          request.getConfig().getAttributes().keySet()),
        ImmutableSet.of(ConfigHelper.CLUSTER_ENV))
    );

    Map<String, Map<String, String>> fullProperties = request.getConfig().getFullProperties();
    Map<String, Map<String, Map<String, String>>> fullAttributes = request.getConfig().getFullAttributes();

    Set<ClusterRequest> clusterRequests = createConfigRequestsForServices(
      existingServices,
      configTypesInRequest::contains,
      request, fullProperties, fullAttributes
    );

    if (request.getConfig().getProperties().containsKey(ConfigHelper.CLUSTER_ENV)) {
      Optional<ClusterRequest> clusterEnvRequest = createConfigRequestForConfigTypes(Stream.of(ConfigHelper.CLUSTER_ENV),
        request, fullProperties, fullAttributes);
      clusterEnvRequest.ifPresent(clusterRequests::add);
    }

    return clusterRequests;
  }

  private static Set<ClusterRequest> createConfigRequestsForServices(
    Set<String> services,
    java.util.function.Predicate<String> predicate,
    AddServiceInfo request,
    Map<String, Map<String, String>> fullProperties,
    Map<String, Map<String, Map<String, String>>> fullAttributes
  ) {
    return services.stream()
      .map(service -> createConfigRequestForConfigTypes(
        request.getStack().getConfigurationTypes(service).stream()
          .filter(predicate),
        request, fullProperties, fullAttributes
      ))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toSet());
  }

  /**
   * Creates a {@link ConfigurationRequest} for each config type in the {@code configTypes} stream.
   *
   * @return an {@code Optional} {@link ClusterRequest} with desired configs set to all the {@code ConfigurationRequests},
   * or an empty {@code Optional} if the incoming {@code configTypes} stream is empty
   */
  private static Optional<ClusterRequest> createConfigRequestForConfigTypes(
    Stream<String> configTypes,
    AddServiceInfo addServiceRequest,
    Map<String, Map<String, String>> fullProperties,
    Map<String, Map<String, Map<String, String>>> fullAttributes
  ) {
    List<ConfigurationRequest> configRequests = configTypes
      .peek(configType -> LOG.info("Creating request for config type {} for {}", configType, addServiceRequest))
      .map(configType -> new ConfigurationRequest(addServiceRequest.clusterName(), configType, "ADD_SERVICE",
        fullProperties.getOrDefault(configType, ImmutableMap.of()),
        fullAttributes.getOrDefault(configType, ImmutableMap.of())))
      .collect(toList());

    if (configRequests.isEmpty()) {
      return Optional.empty();
    }

    ClusterRequest clusterRequest = new ClusterRequest(null, addServiceRequest.clusterName(), null, null);
    clusterRequest.setDesiredConfig(configRequests);
    return Optional.of(clusterRequest);
  }

  private static Predicate predicateForNewServices(AddServiceInfo request, String category) {
    return new AndPredicate(
      new EqualsPredicate<>(PropertyHelper.getPropertyId(category, ClusterResourceProvider.CLUSTER_NAME), request.clusterName()),
      new OrPredicate(
        request.newServices().keySet().stream()
          .map(service -> new EqualsPredicate<>(PropertyHelper.getPropertyId(category, "service_name"), service))
          .toArray(Predicate[]::new)
      )
    );
  }

  private static ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }
}
