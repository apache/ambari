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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.addservice.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Creates resources using the resource providers.
 * Translates {@link AddServiceInfo} to internal requests accepted by those.
 */
public class ResourceProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceProviderAdapter.class);

  public void createServices(AddServiceInfo request) {
    LOG.info("Creating service resources for {}", request);

    Set<Map<String, Object>> properties = request.newServices().keySet().stream()
      .map(service -> createServiceRequestProperties(request, service))
      .collect(toSet());

    Request internalRequest = new RequestImpl(null, properties, null, null);
    ResourceProvider rp = getClusterController().ensureResourceProvider(Resource.Type.Service);
    try {
      rp.createResources(internalRequest);
    } catch (UnsupportedPropertyException | SystemException | ResourceAlreadyExistsException | NoSuchParentResourceException e) {
      LOG.error("Error creating services", e);
      throw new RuntimeException("Error creating services", e);
    }
  }

  private static Map<String, Object> createServiceRequestProperties(AddServiceInfo request, Service service) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, request.clusterName());
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, service.name());
    properties.put(ServiceResourceProvider.SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID, String.valueOf(request.repositoryVersionId()));
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, State.INIT.name());

    service.credentialStoreEnabled().ifPresent(enabled ->
      properties.put(ServiceResourceProvider.SERVICE_CREDENTIAL_STORE_ENABLED_PROPERTY_ID, String.valueOf(enabled))
    );

    return properties.build();
  }

  private ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }

  public void createComponents(AddServiceInfo request) {
    LOG.info("Creating component resources for {}", request);
    // TODO implement
  }

  public void createHostComponents(AddServiceInfo request) {
    LOG.info("Creating host component resources for {}", request);
    // TODO implement
  }

  public void updateServiceDesiredState(AddServiceInfo request, State desiredState) {
    LOG.info("Updating service desired state to {} for {}", desiredState, request);
    // TODO implement, reuse parts of AmbariContext#createAmbariServiceAndComponentResources
  }
}
