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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.RepositoryXml.Os;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The {@link DefaultOperatingSystemResourceProvider} is used to provide the
 * original operating system resources which shipped with a management pack.
 */
@StaticallyInject
public class DefaultOperatingSystemResourceProvider extends ReadOnlyResourceProvider {

  public static final String OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId("OperatingSystems", "os_type");
  public static final String OPERATING_SYSTEM_REPOS = PropertyHelper.getPropertyId("OperatingSystems", "repositories");
  public static final String OPERATING_SYSTEM_MPACK_ID = PropertyHelper.getPropertyId("OperatingSystems", "mpack_id");

  private static Set<String> pkPropertyIds = Sets.newHashSet(OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID);

  public static Set<String> propertyIds = Sets.newHashSet(OPERATING_SYSTEM_MPACK_ID,
      OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID, OPERATING_SYSTEM_REPOS);

  public static Map<Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Mpack, OPERATING_SYSTEM_MPACK_ID)
      .put(Resource.Type.DefaultOperatingSystem, OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID)
      .build();

  /**
   * Used to retrieve the in-memory mpack.
   */
  @Inject
  private static Provider<AmbariMetaInfo> m_ambariMetaInfoProvider;

  /**
   * Constructor.
   *
   * @param managementController
   */
  protected DefaultOperatingSystemResourceProvider(
      AmbariManagementController managementController) {
    super(Resource.Type.DefaultOperatingSystem, propertyIds, keyPropertyIds, managementController);
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
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    // use a collection which preserves order since JPA sorts the results
    Set<Resource> results = new LinkedHashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String mpackIdString = (String) propertyMap.get(OPERATING_SYSTEM_MPACK_ID);
      Long mpackId = Long.valueOf(mpackIdString);

      Mpack mpack = m_ambariMetaInfoProvider.get().getMpack(mpackId);
      if (null == mpack) {
        throw new IllegalArgumentException();
      }

      RepositoryXml repositoryXml = mpack.getRepositoryXml();
      for (Os operatingSystem : repositoryXml.getOses()) {
        Resource resource = toResource(operatingSystem, requestPropertyIds);
        resource.setProperty(OPERATING_SYSTEM_MPACK_ID, mpack.getResourceId());
        results.add(resource);
      }
    }

    return results;
  }

  /**
   * Convert the repository entity to a response resource for serialization.
   *
   * @param mpack
   *          the mpack
   * @param requestedIds
   *          the list of requested IDs to use when setting optional properties.
   * @return the resource to be serialized in the response.
   */
  private Resource toResource(Os operatingSystem, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.DefaultOperatingSystem);
    resource.setProperty(OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID, operatingSystem.getFamily());
    resource.setProperty(OPERATING_SYSTEM_REPOS, operatingSystem.getRepos());
    return resource;
  }
}
