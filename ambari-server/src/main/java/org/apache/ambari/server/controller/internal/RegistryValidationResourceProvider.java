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
package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.registry.RegistryAdvisorRequest;
import org.apache.ambari.server.registry.RegistryValidationResponse;


/**
 * Registry validation resource provider
 */
public class RegistryValidationResourceProvider extends RegistryAdvisorResourceProvider {

  protected static final String VALIDATE_PROPERTY_ID = "validate";
  protected static final String VALIDATION_ID_PROPERTY_ID = PropertyHelper.getPropertyId(
    "RegistryValidation", "id");
  protected static final String ITEMS_PROPERTY_ID = "items";

  private static Set<String> pkPropertyIds = new HashSet<>(
    Arrays.asList(REGISTRY_ID, VALIDATION_ID_PROPERTY_ID));

  /**
   * The property ids for a software registry resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for a software registry resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(REGISTRY_ID);
    PROPERTY_IDS.add(VALIDATION_ID_PROPERTY_ID);
    PROPERTY_IDS.add(VALIDATE_PROPERTY_ID);
    PROPERTY_IDS.add(SELECTED_SCENARIOS_PROPERTY_ID);
    PROPERTY_IDS.add(SELECTED_MPACKS_PROPERTY_ID);
    PROPERTY_IDS.add(ITEMS_PROPERTY_ID);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Registry, REGISTRY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.RegistryValidation, VALIDATION_ID_PROPERTY_ID);

  }

  protected RegistryValidationResourceProvider(
    final AmbariManagementController managementController) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestStatus createResourcesAuthorized(Request request)
    throws NoSuchParentResourceException, ResourceAlreadyExistsException, SystemException {
    RegistryAdvisorRequest registryAdvisorRequest = createRegistryAdvisorRequest(request);
    RegistryValidationResponse response = createResources(new Command<RegistryValidationResponse>() {
      @Override
      public RegistryValidationResponse invoke() throws AmbariException {
        return registryAdvisor.validate(registryAdvisorRequest);
      }
    });
    Resource resource = new ResourceImpl(Resource.Type.RegistryValidation);
    setResourceProperty(resource, REGISTRY_ID, response.getRegistryId(), getPropertyIds());
    setResourceProperty(resource, VALIDATION_ID_PROPERTY_ID, response.getId(), getPropertyIds());
    setResourceProperty(resource, VALIDATE_PROPERTY_ID, response.getRequestType().toString(), getPropertyIds());
    setResourceProperty(resource, SELECTED_SCENARIOS_PROPERTY_ID, response.getSelectedScenarios(), getPropertyIds());
    setResourceProperty(resource, SELECTED_MPACKS_PROPERTY_ID, response.getSelectedMpacks(), getPropertyIds());
    setResourceProperty(resource, ITEMS_PROPERTY_ID, response.getItems(), getPropertyIds());

    Set<Resource> associatedResources = new HashSet<>(Arrays.asList(resource));
    return getRequestStatus(null, associatedResources);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getRequestTypePropertyId() {
    return VALIDATE_PROPERTY_ID;
  }
}
