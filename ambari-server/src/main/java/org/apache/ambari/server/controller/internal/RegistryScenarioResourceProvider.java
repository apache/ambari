/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RegistryScenarioRequest;
import org.apache.ambari.server.controller.RegistryScenarioResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.exceptions.RegistryScenarioNotFoundException;
import org.apache.ambari.server.registry.Registry;
import org.apache.ambari.server.registry.RegistryScenario;

/**
 * ResourceProvider for scenarios in software registry
 */
public class RegistryScenarioResourceProvider extends AbstractControllerResourceProvider {
  public static final String RESPONSE_KEY = "RegistryScenarioInfo";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
  public static final String REGISTRY_ID =  RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP +  "registry_id";
  public static final String REGISTRY_SCENARIO_NAME = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "scenario_name";
  public static final String REGISTRY_SCENARIO_DESC = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "scenario_description";
  public static final String REGISTRY_SCENARIO_MPACKS = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "scenario_mpacks";

  private static Set<String> pkPropertyIds = new HashSet<>(
    Arrays.asList(REGISTRY_ID, REGISTRY_SCENARIO_NAME));

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
    PROPERTY_IDS.add(REGISTRY_SCENARIO_NAME);
    PROPERTY_IDS.add(REGISTRY_SCENARIO_DESC);
    PROPERTY_IDS.add(REGISTRY_SCENARIO_MPACKS);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Registry, REGISTRY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.RegistryScenario, REGISTRY_SCENARIO_NAME);
  }

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  protected RegistryScenarioResourceProvider(final AmbariManagementController managementController) {
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
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException,
    NoSuchResourceException, NoSuchParentResourceException {

    final Set<RegistryScenarioRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<RegistryScenarioResponse> responses = getResources(new Command<Set<RegistryScenarioResponse>>() {
      @Override
      public Set<RegistryScenarioResponse> invoke() throws AmbariException {
        return getRegistryScenarios(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();
    for (RegistryScenarioResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RegistryScenario);
      setResourceProperty(resource, REGISTRY_ID, response.getRegistryId(), requestedIds);
      setResourceProperty(resource, REGISTRY_SCENARIO_NAME, response.getScenarioName(), requestedIds);
      setResourceProperty(resource, REGISTRY_SCENARIO_DESC, response.getScenarioDescription(), requestedIds);
      setResourceProperty(resource, REGISTRY_SCENARIO_MPACKS, response.getScenarioMpacks(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  private RegistryScenarioRequest getRequest(Map<String, Object> properties) {

    Long registryId = properties.containsKey(REGISTRY_ID) && properties.get(REGISTRY_ID) != null?
      Long.valueOf((String) properties.get(REGISTRY_ID)) : null;
    String scenarioName = properties.containsKey(REGISTRY_SCENARIO_NAME)?
      (String) properties.get(REGISTRY_SCENARIO_NAME) : null;
    RegistryScenarioRequest registryScenarioRequest = new RegistryScenarioRequest(registryId, scenarioName);
    return registryScenarioRequest;
  }

  private Set<RegistryScenarioResponse> getRegistryScenarios(Set<RegistryScenarioRequest> requests)
    throws AmbariException {
    Set<RegistryScenarioResponse> responses = new HashSet<>();
    for (RegistryScenarioRequest request : requests) {
      try {
        responses.addAll(getRegistryScenarios(request));
      } catch (RegistryScenarioNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return responses;
  }

  private Set<RegistryScenarioResponse> getRegistryScenarios(RegistryScenarioRequest request)
    throws AmbariException {
    if (request.getRegistryId() == null) {
      throw new AmbariException("Invalid arguments, registry id cannot be null");
    }
    AmbariManagementController amc = getManagementController();
    final Registry registry;
    try {
      registry = amc.getRegistry(request.getRegistryId());
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent registry resource doesn't exist", e);
    }
    Set<RegistryScenarioResponse> responses = new HashSet<>();

    if(request.getScenarioName() == null) {
      for (RegistryScenario registryScenario : registry.getRegistryScenarios()) {
        RegistryScenarioResponse response = new RegistryScenarioResponse(
          registry.getRegistryId(),
          registryScenario.getScenarioName(),
          registryScenario.getScenarioDescription(),
          registryScenario.getScenarioMpacks());
        responses.add(response);
      }
    } else {
      RegistryScenario registryScenario = registry.getRegistryScenario(request.getScenarioName());
      if(registryScenario != null) {
        RegistryScenarioResponse response = new RegistryScenarioResponse(
          registry.getRegistryId(),
          registryScenario.getScenarioName(),
          registryScenario.getScenarioDescription(),
          registryScenario.getScenarioMpacks());
        responses.add(response);
      }
    }
    return responses;
  }
}
