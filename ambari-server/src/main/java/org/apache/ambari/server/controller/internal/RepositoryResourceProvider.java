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


package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RepositoryRequest;
import org.apache.ambari.server.controller.RepositoryResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

public class RepositoryResourceProvider extends ReadOnlyResourceProvider {

  public static final String REPOSITORY_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Repositories", "repo_name");

  private static final String STACK_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Repositories", "stack_name");

  private static final String STACK_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("Repositories", "stack_version");

  private static final String OS_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("Repositories", "os_type");

  private static final String REPOSITORY_BASE_URL_PROPERTY_ID = PropertyHelper
      .getPropertyId("Repositories", "base_url");

  private static final String REPOSITORY_OS_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("Repositories", "os_type");

  private static final String REPO_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("Repositories", "repo_id");

  private static final String REPOSITORY_MIRRORS_LIST_PROPERTY_ID = PropertyHelper
      .getPropertyId("Repositories", "mirrors_list");;

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { STACK_NAME_PROPERTY_ID,
              STACK_VERSION_PROPERTY_ID, OS_TYPE_PROPERTY_ID,
              REPO_ID_PROPERTY_ID }));

  public RepositoryResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final RepositoryRequest repositoryRequest = getRequest(PredicateHelper
        .getProperties(predicate));
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<RepositoryResponse> responses = getResources(new Command<Set<RepositoryResponse>>() {
      @Override
      public Set<RepositoryResponse> invoke() throws AmbariException {
        return getManagementController().getRepositories(
            Collections.singleton(repositoryRequest));
      }
    });

    Set<Resource> resources = new HashSet<Resource>();

    for (RepositoryResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Repository);

        setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
            repositoryRequest.getStackName(), requestedIds);

        setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
            repositoryRequest.getStackVersion(), requestedIds);

        setResourceProperty(resource, REPOSITORY_NAME_PROPERTY_ID,
            response.getRepoName(), requestedIds);

        setResourceProperty(resource, REPOSITORY_BASE_URL_PROPERTY_ID,
            response.getBaseUrl(), requestedIds);

        setResourceProperty(resource, REPOSITORY_OS_TYPE_PROPERTY_ID,
            response.getOsType(), requestedIds);

        setResourceProperty(resource, REPO_ID_PROPERTY_ID,
            response.getRepoId(), requestedIds);

        setResourceProperty(resource, REPOSITORY_MIRRORS_LIST_PROPERTY_ID,
            response.getMirrorsList(), requestedIds);

        resources.add(resource);
    }

    return resources;
  }

  private RepositoryRequest getRequest(Map<String, Object> properties) {
    return new RepositoryRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(OS_TYPE_PROPERTY_ID),
        (String) properties.get(REPO_ID_PROPERTY_ID)
        );
  }

  @Override
  public Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }
}
