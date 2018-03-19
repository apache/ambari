/*
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
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
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

/**
 * The {@link OperatingSystemResourceProvider} is used to provide CRUD
 * capabilities for repositories based on an operating system.
 */
@StaticallyInject
public class OperatingSystemResourceProvider extends AbstractControllerResourceProvider {

  public static final String OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId("OperatingSystems", "os_type");
  public static final String OPERATING_SYSTEM_IS_AMBARI_MANAGED = PropertyHelper.getPropertyId("OperatingSystems","is_ambari_managed");

  public static final String OPERATING_SYSTEM_REPOS = PropertyHelper.getPropertyId("OperatingSystems","repositories");
  public static final String OPERATING_SYSTEM_MPACK_ID = PropertyHelper.getPropertyId("OperatingSystems", "mpack_id");

  private static Set<String> pkPropertyIds = Sets.newHashSet(OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID);

  public static Set<String> propertyIds = Sets.newHashSet(
      OPERATING_SYSTEM_MPACK_ID,
      OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID,
      OPERATING_SYSTEM_IS_AMBARI_MANAGED, OPERATING_SYSTEM_REPOS);

  public static Map<Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Mpack, OPERATING_SYSTEM_MPACK_ID)
      .put(Resource.Type.OperatingSystem, OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID)
      .build();

  /**
   * Used to update
   */
  @Inject
  private static MpackDAO s_mpackDAO;

  /**
   * Used to deserialize the repository JSON into an object.
   */
  @Inject
  private static Gson s_gson;

  protected OperatingSystemResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.OperatingSystem, propertyIds, keyPropertyIds, managementController);
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
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    // use a collection which preserves order since JPA sorts the results
    Set<Resource> results = new LinkedHashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String mpackIdString = (String) propertyMap.get(OPERATING_SYSTEM_MPACK_ID);
      Long mpackId = Long.valueOf(mpackIdString);
      MpackEntity mpackEntity = s_mpackDAO.findById(mpackId);
      List<RepoOsEntity> repositoryOperatingSystems = mpackEntity.getRepositoryOperatingSystems();
      for (RepoOsEntity repoOsEntity : repositoryOperatingSystems) {
        Resource resource = toResource(repoOsEntity, requestPropertyIds);
        results.add(resource);
      }
    }

    return results;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String mpackIdString = (String) propertyMap.get(OPERATING_SYSTEM_MPACK_ID);
      Long mpackId = Long.valueOf(mpackIdString);

      if (StringUtils.isBlank(mpackIdString)) {
        throw new IllegalArgumentException(
            String.format("The property %s is required", OPERATING_SYSTEM_MPACK_ID));
      }

      String operatingSystem = (String)propertyMap.get(OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID);
      if (StringUtils.isBlank(operatingSystem)) {
        throw new IllegalArgumentException(
            String.format("The property %s is required", OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID));
      }

      MpackEntity mpackEntity = s_mpackDAO.findById(mpackId);
      List<RepoOsEntity> repositoryOperatingSystems = mpackEntity.getRepositoryOperatingSystems();
      Iterator<RepoOsEntity> iterator = repositoryOperatingSystems.iterator();
      while (iterator.hasNext()) {
        RepoOsEntity repoOsEntity = iterator.next();
        if (StringUtils.equals(operatingSystem, repoOsEntity.getFamily())) {
          iterator.remove();
        }
      }

      mpackEntity = s_mpackDAO.merge(mpackEntity);
    }

    notifyDelete(Resource.Type.OperatingSystem, predicate);
    return getRequestStatus(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestStatus createResources(final Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        createOperatingSystem(request.getProperties());
        return null;
      }
    });
    notifyCreate(Resource.Type.OperatingSystem, request);

    return getRequestStatus(null);
  }

  private void createOperatingSystem(Set<Map<String, Object>> requestMaps)
      throws AmbariException, AuthorizationException {
    for (Map<String, Object> requestMap : requestMaps) {
      String mpackIdString = (String) requestMap.get(OPERATING_SYSTEM_MPACK_ID);
      Long mpackId = Long.valueOf(mpackIdString);

      if (StringUtils.isBlank(mpackIdString)) {
        throw new IllegalArgumentException(
            String.format("The property %s is required", OPERATING_SYSTEM_MPACK_ID));
      }

      String operatingSystem = (String) requestMap.get(OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID);
      if (StringUtils.isBlank(operatingSystem)) {
        throw new IllegalArgumentException(
            String.format("The property %s is required", OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID));
      }

      MpackEntity mpackEntity = s_mpackDAO.findById(mpackId);
      if (null == mpackEntity) {
        throw new IllegalArgumentException(
            String.format("The mpack with ID %s was not found", mpackId));
      }

      RepoOsEntity repositoryOsEntity = new RepoOsEntity();
      repositoryOsEntity.setFamily(operatingSystem);
      repositoryOsEntity.setMpackEntity(mpackEntity);
      populateEntity(repositoryOsEntity, requestMap);

      mpackEntity.getRepositoryOperatingSystems().add(repositoryOsEntity);
      mpackEntity = s_mpackDAO.merge(mpackEntity);

      s_mpackDAO.refresh(mpackEntity);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    for (Map<String, Object> requestPropMap : request.getProperties()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(requestPropMap, predicate)) {
        String mpackIdString = (String) propertyMap.get(OPERATING_SYSTEM_MPACK_ID);
        Long mpackId = Long.valueOf(mpackIdString);

        if (StringUtils.isBlank(mpackIdString)) {
          throw new IllegalArgumentException(
              String.format("The property %s is required", OPERATING_SYSTEM_MPACK_ID));
        }

        String operatingSystem = (String) propertyMap.get(OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID);
        if (StringUtils.isBlank(operatingSystem)) {
          throw new IllegalArgumentException(
              String.format("The property %s is required", OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID));
        }

        MpackEntity mpackEntity = s_mpackDAO.findById(mpackId);
        if (null == mpackEntity) {
          throw new IllegalArgumentException(
              String.format("The mpack with ID %s was not found", mpackId));
        }

        List<RepoOsEntity> repositoryOperatingSystems = mpackEntity.getRepositoryOperatingSystems();
        for (RepoOsEntity repoOsEntity : repositoryOperatingSystems) {
          if (StringUtils.equals(operatingSystem, repoOsEntity.getFamily())) {
            try {
              populateEntity(repoOsEntity, propertyMap);
            } catch( AmbariException ambariException ) {
              throw new SystemException(ambariException.getMessage(), ambariException);
            }
          }
        }

        mpackEntity = s_mpackDAO.merge(mpackEntity);
      }
    }

    notifyUpdate(Resource.Type.OperatingSystem, request, predicate);
    return getRequestStatus(null);
  }

  /**
   * Convert the repository entity to a response resource for serialization.
   *
   * @param repositoryOsEntity
   *          the operating system result to seralize.
   * @param requestedIds
   *          the list of requested IDs to use when setting optional properties.
   * @return the resource to be serialized in the response.
   */
  private Resource toResource(RepoOsEntity repositoryOsEntity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.OperatingSystem);

    resource.setProperty(OPERATING_SYSTEM_MPACK_ID, repositoryOsEntity.getMpackId());
    resource.setProperty(OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID, repositoryOsEntity.getFamily());
    resource.setProperty(OPERATING_SYSTEM_IS_AMBARI_MANAGED, repositoryOsEntity.isAmbariManaged());

    Set<RepositoryInfo> repositories = new LinkedHashSet<>();
    for (RepoDefinitionEntity repoDefinitionEntity : repositoryOsEntity.getRepoDefinitionEntities()) {
      RepositoryInfo repositoryInfo = new RepositoryInfo();
      repositoryInfo.setAmbariManagedRepositories(repositoryOsEntity.isAmbariManaged());
      repositoryInfo.setBaseUrl(repoDefinitionEntity.getBaseUrl());
      repositoryInfo.setComponents(repoDefinitionEntity.getComponents());
      repositoryInfo.setDistribution(repoDefinitionEntity.getDistribution());
      repositoryInfo.setMirrorsList(repoDefinitionEntity.getMirrors());
      repositoryInfo.setOsType(repositoryOsEntity.getFamily());
      repositoryInfo.setTags(repoDefinitionEntity.getTags());
      repositoryInfo.setUnique(repoDefinitionEntity.isUnique());
      repositoryInfo.setRepoId(repoDefinitionEntity.getRepoID());
      repositoryInfo.setRepoName(repoDefinitionEntity.getRepoName());

      repositories.add(repositoryInfo);
    }

    resource.setProperty(OPERATING_SYSTEM_REPOS, repositories);

    return resource;
  }

  /**
   * Merges the map of properties into the specified entity. If the entity is
   * being created, an {@link IllegalArgumentException} is thrown when a
   * required property is absent. When updating, missing properties are assume
   * to not have changed.
   *
   * @param entity
   *          the entity to merge the properties into (not {@code null}).
   * @param requestMap
   *          the map of properties (not {@code null}).
   * @throws AmbariException
   */
  private void populateEntity(RepoOsEntity entity, Map<String, Object> requestMap)
      throws AmbariException, AuthorizationException {

    if (requestMap.containsKey(OPERATING_SYSTEM_IS_AMBARI_MANAGED)) {
      String isAmbariManagedString = (String) requestMap.get(OPERATING_SYSTEM_IS_AMBARI_MANAGED);
      entity.setAmbariManaged(Boolean.valueOf(isAmbariManagedString));
    }

    if (requestMap.containsKey(OPERATING_SYSTEM_REPOS)) {
      java.lang.reflect.Type listType = new TypeToken<ArrayList<RepositoryInfo>>(){}.getType();

      @SuppressWarnings("unchecked")
      Set<Map<String, Object>> repoMaps = (Set<Map<String, Object>>) requestMap.get(
          OPERATING_SYSTEM_REPOS);

      String json = s_gson.toJson(repoMaps);
      List<RepositoryInfo> repositories = s_gson.fromJson(json, listType);
      List<RepoDefinitionEntity> repoDefinitionEntities = entity.getRepoDefinitionEntities();
      repoDefinitionEntities.clear();

      for (RepositoryInfo repositoryInfo : repositories) {
        RepoDefinitionEntity repoDefinitionEntity = RepoDefinitionEntity.from(repositoryInfo);
        repoDefinitionEntity.setRepoOs(entity);
        repoDefinitionEntities.add(repoDefinitionEntity);
      }
    }
  }
}
