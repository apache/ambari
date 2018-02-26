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
import org.apache.ambari.server.controller.RegistryMpackRequest;
import org.apache.ambari.server.controller.RegistryMpackResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.exceptions.RegistryMpackNotFoundException;
import org.apache.ambari.server.registry.Registry;
import org.apache.ambari.server.registry.RegistryMpack;

/**
 * ResourceProvider for mpacks in software registry
 */
public class RegistryMpackResourceProvider extends AbstractControllerResourceProvider {
  public static final String RESPONSE_KEY = "RegistryMpackInfo";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
  public static final String REGISTRY_ID =  RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP +  "registry_id";
  public static final String REGISTRY_MPACK_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_id";
  public static final String REGISTRY_MPACK_NAME = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_name";
  public static final String REGISTRY_MPACK_DESC = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_description";
  public static final String REGISTRY_MPACK_LOGO_URI = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_logo_uri";

  private static Set<String> pkPropertyIds = new HashSet<>(
    Arrays.asList(REGISTRY_ID, REGISTRY_MPACK_ID, REGISTRY_MPACK_NAME));

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
    PROPERTY_IDS.add(REGISTRY_MPACK_ID);
    PROPERTY_IDS.add(REGISTRY_MPACK_NAME);
    PROPERTY_IDS.add(REGISTRY_MPACK_DESC);
    PROPERTY_IDS.add(REGISTRY_MPACK_LOGO_URI);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Registry, REGISTRY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.RegistryMpack, REGISTRY_MPACK_NAME);
  }

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  protected RegistryMpackResourceProvider(final AmbariManagementController managementController) {
    super(Resource.Type.RegistryMpack, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException,
    NoSuchResourceException, NoSuchParentResourceException {

    final Set<RegistryMpackRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<RegistryMpackResponse> responses = getResources(new Command<Set<RegistryMpackResponse>>() {
      @Override
      public Set<RegistryMpackResponse> invoke() throws AmbariException {
        return getRegistryMpacks(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();
    for (RegistryMpackResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RegistryMpack);
      setResourceProperty(resource, REGISTRY_ID, response.getRegistryId(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_ID, response.getMpackId(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_NAME, response.getMpackName(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_DESC, response.getMpackDescription(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_LOGO_URI, response.getMpackLogoUri(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  private RegistryMpackRequest getRequest(Map<String, Object> properties) {

    Long registryId = properties.containsKey(REGISTRY_ID) && properties.get(REGISTRY_ID) != null?
      Long.valueOf((String) properties.get(REGISTRY_ID)) : null;
    String mpackName = properties.containsKey(REGISTRY_MPACK_NAME)?
      (String) properties.get(REGISTRY_MPACK_NAME) : null;
    RegistryMpackRequest registryMpackRequest = new RegistryMpackRequest(registryId, mpackName);
    return registryMpackRequest;
  }

  private Set<RegistryMpackResponse> getRegistryMpacks(Set<RegistryMpackRequest> requests)
    throws AmbariException {
    Set<RegistryMpackResponse> responses = new HashSet<>();
    for (RegistryMpackRequest request : requests) {
      try {
        responses.addAll(getRegistryMpacks(request));
      } catch (RegistryMpackNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return responses;
  }

  private Set<RegistryMpackResponse> getRegistryMpacks(RegistryMpackRequest request)
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
    Set<RegistryMpackResponse> responses = new HashSet<>();

    if(request.getMpackName() == null) {
      for (RegistryMpack registryMpack : registry.getRegistryMpacks()) {
        RegistryMpackResponse response = new RegistryMpackResponse(
          registry.getRegistryId(),
          registryMpack.getMpackId(),
          registryMpack.getMpackName(),
          registryMpack.getMpackDescription(),
          registryMpack.getMpackLogoUri());
        responses.add(response);
      }
    } else {
      RegistryMpack registryMpack = registry.getRegistryMpack(request.getMpackName());
      if(registryMpack != null) {
        RegistryMpackResponse response = new RegistryMpackResponse(
          registry.getRegistryId(),
          registryMpack.getMpackId(),
          registryMpack.getMpackName(),
          registryMpack.getMpackDescription(),
          registryMpack.getMpackLogoUri());
        responses.add(response);
      }
    }
    return responses;
  }
}
