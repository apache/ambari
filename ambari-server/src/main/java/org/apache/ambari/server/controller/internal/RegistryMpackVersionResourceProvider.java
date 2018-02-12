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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RegistryMpackVersionRequest;
import org.apache.ambari.server.controller.RegistryMpackVersionResponse;
import org.apache.ambari.server.controller.spi.ExtendedResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.exceptions.RegistryMpackNotFoundException;
import org.apache.ambari.server.registry.Registry;
import org.apache.ambari.server.registry.RegistryMpack;
import org.apache.ambari.server.registry.RegistryMpackVersion;
import org.apache.ambari.server.utils.VersionUtils;

/**
 * ResourceProvider for mpacks in software registry
 */
public class RegistryMpackVersionResourceProvider extends AbstractControllerResourceProvider implements ExtendedResourceProvider {
  public static final String RESPONSE_KEY = "RegistryMpackVersionInfo";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";

  public static final String REGISTRY_ID =  RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP +  "registry_id";
  public static final String REGISTRY_MPACK_NAME = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_name";
  public static final String REGISTRY_MPACK_VERSION = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_version";
  public static final String REGISTRY_MPACK_BUILDNUM = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_buildnum";
  public static final String REGISTRY_MPACK_URL = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_url";
  public static final String REGISTRY_MPACK_DOC_URL = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpack_doc_url";
  public static final String REGISTRY_MPACK_SERVICES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "services";
  public static final String REGISTRY_MPACK_COMPATIBLE_MPACKS = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "compatible_mpacks";
  public static final String REGISTRY_MPACK_STACK_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "stack_name";
  public static final String REGISTRY_MPACK_STACK_VERSION_PROPERTY_ID =
          RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "stack_version";


  private static Set<String> pkPropertyIds = new HashSet<>(
    Arrays.asList(REGISTRY_ID, REGISTRY_MPACK_NAME));

  /**
   * The property ids for a software registry resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for a software registry resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  /***
   * Resource count for get resources
   */
  private static int resourceCount;

  static {
    // properties
    PROPERTY_IDS.add(REGISTRY_ID);
    PROPERTY_IDS.add(REGISTRY_MPACK_NAME);
    PROPERTY_IDS.add(REGISTRY_MPACK_VERSION);
    PROPERTY_IDS.add(REGISTRY_MPACK_BUILDNUM);
    PROPERTY_IDS.add(REGISTRY_MPACK_URL);
    PROPERTY_IDS.add(REGISTRY_MPACK_DOC_URL);
    PROPERTY_IDS.add(REGISTRY_MPACK_SERVICES);
    PROPERTY_IDS.add(REGISTRY_MPACK_COMPATIBLE_MPACKS);
    PROPERTY_IDS.add(REGISTRY_MPACK_STACK_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(REGISTRY_MPACK_STACK_VERSION_PROPERTY_ID);


    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Registry, REGISTRY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.RegistryMpack, REGISTRY_MPACK_NAME);
    KEY_PROPERTY_IDS.put(Resource.Type.RegistryMpackVersion, REGISTRY_MPACK_VERSION);
  }

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  protected RegistryMpackVersionResourceProvider(final AmbariManagementController managementController) {
    super(Resource.Type.RegistryMpackVersion, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
    resourceCount = 0;
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
   */
  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException,
    NoSuchResourceException, NoSuchParentResourceException {

    final Set<RegistryMpackVersionRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<RegistryMpackVersionResponse> responses = getResources(new Command<Set<RegistryMpackVersionResponse>>() {
      @Override
      public Set<RegistryMpackVersionResponse> invoke() throws AmbariException {
        return getRegistryMpackVersions(requests);
      }
    });

    List<Resource> sortedResources = new LinkedList<>();
    for (RegistryMpackVersionResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RegistryMpackVersion);
      setResourceProperty(resource, REGISTRY_ID, response.getRegistryId(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_NAME, response.getMpackName(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_VERSION, response.getMpackVersion(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_BUILDNUM, response.getMpackBuildNumber(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_URL, response.getMpackUrl(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_DOC_URL, response.getMpackDocUrl(), requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_SERVICES, response.getMpackServices(), requestedIds);
      String[] stackid = ((String) response.getStackId()).split("-");
      setResourceProperty(resource, REGISTRY_MPACK_STACK_NAME_PROPERTY_ID, stackid[0], requestedIds);
      setResourceProperty(resource, REGISTRY_MPACK_STACK_VERSION_PROPERTY_ID, stackid[1], requestedIds);
      sortedResources.add(resource);
    }
    sortedResources.sort(new Comparator<Resource>() {
      @Override
      public int compare(final Resource o1, final Resource o2) {
        int o1Wins = 0;
        int o2Wins = 0;
          int compareResult = VersionUtils.compareVersions((String)o1.getPropertyValue(REGISTRY_MPACK_VERSION), (String)o2.getPropertyValue(REGISTRY_MPACK_VERSION));
          if(compareResult > 0) {
            o1Wins++;
          } else if(compareResult < 0) {
            o2Wins++;
          }
        // Order in reverse order
        return o2Wins - o1Wins;
      }
    });
    resourceCount = sortedResources.size();
    return new LinkedHashSet<>(sortedResources);
  }

  private RegistryMpackVersionRequest getRequest(Map<String, Object> properties) {

    Long registryId = properties.containsKey(REGISTRY_ID) && properties.get(REGISTRY_ID) != null?
      Long.valueOf((String) properties.get(REGISTRY_ID)) : null;
    String mpackName = properties.containsKey(REGISTRY_MPACK_NAME)?
      (String) properties.get(REGISTRY_MPACK_NAME) : null;
    String mpackVersion = properties.containsKey(REGISTRY_MPACK_VERSION)?
      (String) properties.get(REGISTRY_MPACK_VERSION) : null;

    RegistryMpackVersionRequest registryMpackVersionRequest = new RegistryMpackVersionRequest(
      registryId, mpackName, mpackVersion);
    return registryMpackVersionRequest;
  }

  private Set<RegistryMpackVersionResponse> getRegistryMpackVersions(Set<RegistryMpackVersionRequest> requests)
    throws AmbariException {
    Set<RegistryMpackVersionResponse> responses = new HashSet<>();
    for (RegistryMpackVersionRequest request : requests) {
      try {
        responses.addAll(getRegistryMpackVersions(request));
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

  private Set<RegistryMpackVersionResponse> getRegistryMpackVersions(RegistryMpackVersionRequest request)
    throws AmbariException {
    if (request.getRegistryId() == null || request.getMpackName() == null) {
      throw new AmbariException("Invalid arguments, registry id and mpack name cannot be null");
    }
    AmbariManagementController amc = getManagementController();
    final Registry registry;
    try {
      registry = amc.getRegistry(request.getRegistryId());
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent registry resource doesn't exist", e);
    }

    final RegistryMpack registryMpack;
    try {
      registryMpack = registry.getRegistryMpack(request.getMpackName());
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent registry mpack resource doesn't exist", e);
    }

    Set<RegistryMpackVersionResponse> responses = new HashSet<>();

    if(request.getMpackVersion() == null) {
      for (RegistryMpackVersion registryMpackVersion : registryMpack.getMpackVersions()) {
        RegistryMpackVersionResponse response = new RegistryMpackVersionResponse(
          registry.getRegistryId(),
          registryMpack.getMpackName(),
          registryMpackVersion.getMpackVersion(),
          registryMpackVersion.getMpackBuildNumber(),
          registryMpackVersion.getMpackUrl(),
          registryMpackVersion.getMpackDocUrl(),
          registryMpackVersion.getMpackServices(),
          registryMpackVersion.getCompatibleMpacks(),
          registryMpackVersion.getMpackStackId());
        responses.add(response);
      }
    } else {
      RegistryMpackVersion registryMpackVersion = registryMpack.getMpackVersion(request.getMpackVersion());
      if(registryMpackVersion != null) {
        RegistryMpackVersionResponse response = new RegistryMpackVersionResponse(
          registry.getRegistryId(),
          registryMpack.getMpackName(),
          registryMpackVersion.getMpackVersion(),
          registryMpackVersion.getMpackBuildNumber(),
          registryMpackVersion.getMpackUrl(),
          registryMpackVersion.getMpackDocUrl(),
          registryMpackVersion.getMpackServices(),
          registryMpackVersion.getCompatibleMpacks(),
          registryMpackVersion.getMpackStackId());
        responses.add(response);
      }
    }
    return responses;
  }

  @Override
  public QueryResponse queryForResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    return new QueryResponseImpl(
            getResourcesAuthorized(request, predicate),
            true,
            request.getPageRequest() != null,
            getResourceCount());
  }

  /***
   *
   * @return number of resources returned in response to GET calls.
   */
  private int getResourceCount() {
    return resourceCount;
  }
}
