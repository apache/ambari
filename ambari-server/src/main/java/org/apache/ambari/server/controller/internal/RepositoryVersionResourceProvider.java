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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;

import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * Resource provider for repository versions resources.
 */
public class RepositoryVersionResourceProvider extends AbstractResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String REPOSITORY_VERSION_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("RepositoryVersions", "id");
  protected static final String REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("RepositoryVersions", "display_name");
  protected static final String REPOSITORY_VERSION_STACK_PROPERTY_ID        = PropertyHelper.getPropertyId("RepositoryVersions", "stack");
  protected static final String REPOSITORY_VERSION_VERSION_PROPERTY_ID      = PropertyHelper.getPropertyId("RepositoryVersions", "version");
  protected static final String REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID = PropertyHelper.getPropertyId("RepositoryVersions", "upgrade_pack");
  protected static final String REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID = PropertyHelper.getPropertyId("RepositoryVersions", "repositories");

  @SuppressWarnings("serial")
  private static Set<String> pkPropertyIds = new HashSet<String>() {
    {
      add(REPOSITORY_VERSION_ID_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  private static Set<String> propertyIds = new HashSet<String>() {
    {
      add(REPOSITORY_VERSION_ID_PROPERTY_ID);
      add(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID);
      add(REPOSITORY_VERSION_STACK_PROPERTY_ID);
      add(REPOSITORY_VERSION_VERSION_PROPERTY_ID);
      add(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID);
      add(REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  private static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Resource.Type.RepositoryVersion, REPOSITORY_VERSION_ID_PROPERTY_ID);
    }
  };

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private ClusterVersionDAO clusterVersionDAO;

  /**
   * Create a new resource provider.
   *
   */
  public RepositoryVersionResourceProvider() {
    super(propertyIds, keyPropertyIds);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    for (final Map<String, Object> properties : request.getProperties()) {
      createResources(new Command<Void>() {

        @Override
        public Void invoke() throws AmbariException {
          final RepositoryVersionEntity entity = new RepositoryVersionEntity();
          final String[] requiredProperties = {
              REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
              REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID,
              REPOSITORY_VERSION_STACK_PROPERTY_ID,
              REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID,
              REPOSITORY_VERSION_VERSION_PROPERTY_ID
          };
          for (String propertyName: requiredProperties) {
            if (properties.get(propertyName) == null) {
              throw new AmbariException("Property " + propertyName + " should be provided");
            }
          }
          entity.setDisplayName(properties.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID).toString());
          entity.setStack(properties.get(REPOSITORY_VERSION_STACK_PROPERTY_ID).toString());
          entity.setUpgradePackage(properties.get(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID).toString());
          entity.setVersion(properties.get(REPOSITORY_VERSION_VERSION_PROPERTY_ID).toString());
          final Object repositories = properties.get(REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID);
          entity.setRepositories(new Gson().toJson(repositories));

          if (repositoryVersionDAO.findByDisplayName(entity.getDisplayName()) != null) {
            throw new AmbariException("Repository version with name " + entity.getDisplayName() + " already exists");
          }
          if (repositoryVersionDAO.findByStackAndVersion(entity.getStack(), entity.getVersion()) != null) {
            throw new AmbariException("Repository version for stack " + entity.getStack() + " and version " + entity.getVersion() + " already exists");
          }
          repositoryVersionDAO.create(entity);
          notifyCreate(Resource.Type.RepositoryVersion, request);
          return null;
        }
      });
    }

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<Resource>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    List<RepositoryVersionEntity> requestedEntities = new ArrayList<RepositoryVersionEntity>();
    if (propertyMaps.isEmpty()) {
      requestedEntities = repositoryVersionDAO.findAll();
    } else {
      for (Map<String, Object> propertyMap: propertyMaps) {
        final Long id;
        try {
          id = Long.parseLong(propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID).toString());
        } catch (Exception ex) {
          throw new SystemException("Repository version should have numerical id");
        }
        final RepositoryVersionEntity entity = repositoryVersionDAO.findByPK(id);
        if (entity == null) {
          throw new NoSuchResourceException("There is no repository version with id " + id);
        } else {
          requestedEntities.add(entity);
        }
      }
    }

    for (RepositoryVersionEntity entity: requestedEntities) {
      final Resource resource = new ResourceImpl(Resource.Type.RepositoryVersion);

      setResourceProperty(resource, REPOSITORY_VERSION_ID_PROPERTY_ID, entity.getId(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, entity.getDisplayName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID, new Gson().fromJson(entity.getRepositories(), Object.class), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_STACK_PROPERTY_ID, entity.getStack(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, entity.getUpgradePackage(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_VERSION_PROPERTY_ID, entity.getVersion(), requestedIds);

      if (predicate == null || predicate.evaluate(resource)) {
        resources.add(resource);
      }
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Map<String, Object>> propertyMaps = request.getProperties();

    for (Map<String, Object> propertyMap : propertyMaps) {
      final Long id;
      try {
        id = Long.parseLong(propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID).toString());
      } catch (Exception ex) {
        throw new SystemException("Repository version should have numerical id");
      }

      final RepositoryVersionEntity entity = repositoryVersionDAO.findByPK(id);
      if (entity == null) {
        throw new NoSuchResourceException("There is no repository version with id " + id);
      }

      if (propertyMap.get(REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID) != null
          || propertyMap.get(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID) != null) {

        final List<ClusterVersionEntity> clusterVersionEntities =
            clusterVersionDAO.findByStackAndVersion(entity.getStack(), entity.getVersion());

        if (!clusterVersionEntities.isEmpty()) {
          final ClusterVersionEntity firstClusterVersion = clusterVersionEntities.get(0);
          throw new SystemException("Repository version can't be updated as it is " +
            firstClusterVersion.getState().name() + " on cluster " + firstClusterVersion.getClusterEntity().getClusterName());
        }

        if (propertyMap.get(REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID) != null) {
          entity.setRepositories(propertyMap.get(REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID).toString());
        }

        if (propertyMap.get(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID) != null) {
          entity.setUpgradePackage(propertyMap.get(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID).toString());
        }
      }

      if (propertyMap.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID) != null) {
        entity.setDisplayName(propertyMap.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID).toString());
      }

      repositoryVersionDAO.merge(entity);
    }

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    final List<RepositoryVersionEntity> entitiesToBeRemoved = new ArrayList<RepositoryVersionEntity>();
    for (Map<String, Object> propertyMap : propertyMaps) {
      final Long id;
      try {
        id = Long.parseLong(propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID).toString());
      } catch (Exception ex) {
        throw new SystemException("Repository version should have numerical id");
      }

      final RepositoryVersionEntity entity = repositoryVersionDAO.findByPK(id);
      if (entity == null) {
        throw new NoSuchResourceException("There is no repository version with id " + id);
      }

      final List<ClusterVersionEntity> clusterVersionEntities =
          clusterVersionDAO.findByStackAndVersion(entity.getStack(), entity.getVersion());

      if (!clusterVersionEntities.isEmpty()) {
        final ClusterVersionEntity firstClusterVersion = clusterVersionEntities.get(0);
        throw new SystemException("Repository version can't be deleted as it is " +
          firstClusterVersion.getState().name() + " on cluster " + firstClusterVersion.getClusterEntity().getClusterName());
      }

      entitiesToBeRemoved.add(entity);
    }

    for (RepositoryVersionEntity entity: entitiesToBeRemoved) {
      repositoryVersionDAO.remove(entity);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }
}
