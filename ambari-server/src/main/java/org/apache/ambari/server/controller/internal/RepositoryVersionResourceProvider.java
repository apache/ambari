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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.api.resources.OperatingSystemResourceDefinition;
import org.apache.ambari.server.api.resources.RepositoryResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
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
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.ManifestServiceInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

/**
 * Resource provider for repository versions resources.
 */
public class RepositoryVersionResourceProvider extends AbstractAuthorizedResourceProvider {

  // ----- Property ID constants ---------------------------------------------
  public static final String REPOSITORY_VERSION                                = "RepositoryVersions";
  public static final String REPOSITORY_VERSION_ID_PROPERTY_ID                 = PropertyHelper.getPropertyId("RepositoryVersions", "id");
  public static final String REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID         = PropertyHelper.getPropertyId("RepositoryVersions", "stack_name");
  public static final String REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID      = PropertyHelper.getPropertyId("RepositoryVersions", "stack_version");
  public static final String REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("RepositoryVersions", "repository_version");
  public static final String REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID       = PropertyHelper.getPropertyId("RepositoryVersions", "display_name");
  public static final String SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID         = new OperatingSystemResourceDefinition().getPluralName();
  public static final String SUBRESOURCE_REPOSITORIES_PROPERTY_ID              = new RepositoryResourceDefinition().getPluralName();

  public static final String REPOSITORY_VERSION_TYPE_PROPERTY_ID               = "RepositoryVersions/type";
  public static final String REPOSITORY_VERSION_RELEASE_VERSION                = "RepositoryVersions/release/version";
  public static final String REPOSITORY_VERSION_RELEASE_BUILD                  = "RepositoryVersions/release/build";
  public static final String REPOSITORY_VERSION_RELEASE_NOTES                  = "RepositoryVersions/release/notes";
  public static final String REPOSITORY_VERSION_RELEASE_COMPATIBLE_WITH        = "RepositoryVersions/release/compatible_with";
  public static final String REPOSITORY_VERSION_AVAILABLE_SERVICES             = "RepositoryVersions/services";
  public static final String REPOSITORY_VERSION_STACK_SERVICES                 = "RepositoryVersions/stack_services";

  public static final String REPOSITORY_VERSION_PARENT_ID                      = "RepositoryVersions/parent_id";
  public static final String REPOSITORY_VERSION_HAS_CHILDREN                   = "RepositoryVersions/has_children";

  @SuppressWarnings("serial")
  private static Set<String> pkPropertyIds = new HashSet<String>() {
    {
      add(REPOSITORY_VERSION_ID_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  public static Set<String> propertyIds = Sets.newHashSet(
      REPOSITORY_VERSION_ID_PROPERTY_ID,
      REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID,
      REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
      REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
      REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
      SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
      REPOSITORY_VERSION_TYPE_PROPERTY_ID,
      REPOSITORY_VERSION_RELEASE_BUILD,
      REPOSITORY_VERSION_RELEASE_COMPATIBLE_WITH,
      REPOSITORY_VERSION_RELEASE_NOTES,
      REPOSITORY_VERSION_RELEASE_VERSION,
      REPOSITORY_VERSION_PARENT_ID,
      REPOSITORY_VERSION_HAS_CHILDREN,
      REPOSITORY_VERSION_AVAILABLE_SERVICES,
      REPOSITORY_VERSION_STACK_SERVICES);

  @SuppressWarnings("serial")
  public static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Type.Stack, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID);
      put(Type.StackVersion, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
      put(Type.RepositoryVersion, REPOSITORY_VERSION_ID_PROPERTY_ID);
    }
  };

  @Inject
  private Gson gson;

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private ClusterVersionDAO clusterVersionDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private RepositoryVersionHelper repositoryVersionHelper;

  @Inject
  private Provider<Clusters> clusters;

  /**
   * Data access object used for lookup up stacks.
   */
  @Inject
  private StackDAO stackDAO;

  /**
   * Create a new resource provider.
   *
   */
  public RepositoryVersionResourceProvider() {
    super(propertyIds, keyPropertyIds);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
    setRequiredUpdateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS, RoleAuthorization.AMBARI_EDIT_STACK_REPOS));

    setRequiredGetAuthorizations(EnumSet.of(
        RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS,
        RoleAuthorization.AMBARI_EDIT_STACK_REPOS,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK));
  }

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    for (final Map<String, Object> properties : request.getProperties()) {
      createResources(new Command<Void>() {

        @Override
        public Void invoke() throws AmbariException {
          final String[] requiredProperties = {
            REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
            SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
            REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
            REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
            REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID
          };

          for (String propertyName : requiredProperties) {
            if (properties.get(propertyName) == null) {
              throw new AmbariException("Property " + propertyName + " should be provided");
            }
          }

          RepositoryVersionEntity entity = toRepositoryVersionEntity(properties);

          if (repositoryVersionDAO.findByDisplayName(entity.getDisplayName()) != null) {
            throw new AmbariException("Repository version with name " + entity.getDisplayName() + " already exists");
          }
          if (repositoryVersionDAO.findByStackAndVersion(entity.getStack(), entity.getVersion()) != null) {
            throw new AmbariException("Repository version for stack " + entity.getStack() + " and version " + entity.getVersion() + " already exists");
          }

          validateRepositoryVersion(repositoryVersionDAO, ambariMetaInfo, entity);

          repositoryVersionDAO.create(entity);
          notifyCreate(Resource.Type.RepositoryVersion, request);
          return null;
        }
      });
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<Resource>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);


    List<RepositoryVersionEntity> requestedEntities = new ArrayList<RepositoryVersionEntity>();
    for (Map<String, Object> propertyMap: propertyMaps) {
      final StackId stackId = getStackInformationFromUrl(propertyMap);

      if (stackId != null && propertyMaps.size() == 1 && propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID) == null) {
        requestedEntities.addAll(repositoryVersionDAO.findByStack(stackId));
      } else {
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
      setResourceProperty(resource, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, entity.getStackName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, entity.getStackVersion(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, entity.getDisplayName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, entity.getVersion(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_TYPE_PROPERTY_ID, entity.getType(), requestedIds);

      setResourceProperty(resource, REPOSITORY_VERSION_PARENT_ID, entity.getParentId(), requestedIds);

      List<RepositoryVersionEntity> children = entity.getChildren();
      setResourceProperty(resource, REPOSITORY_VERSION_HAS_CHILDREN,
          null != children && !children.isEmpty(), requestedIds);

      final VersionDefinitionXml xml;
      try {
        xml = entity.getRepositoryXml();
      } catch (Exception e) {
        throw new SystemException(String.format("Could not load xml for Repository %s", entity.getId()), e);
      }

      final StackInfo stack;
      try {
        stack = ambariMetaInfo.getStack(entity.getStackName(), entity.getStackVersion());
      } catch (AmbariException e) {
        throw new SystemException(String.format("Could not load stack %s for Repository %s",
            entity.getStackId().toString(), entity.getId()));
      }

      final List<ManifestServiceInfo> stackServices;

      if (null != xml) {
        setResourceProperty(resource, REPOSITORY_VERSION_RELEASE_VERSION, xml.release.version, requestedIds);
        setResourceProperty(resource, REPOSITORY_VERSION_RELEASE_BUILD, xml.release.build, requestedIds);
        setResourceProperty(resource, REPOSITORY_VERSION_RELEASE_COMPATIBLE_WITH, xml.release.compatibleWith, requestedIds);
        setResourceProperty(resource, REPOSITORY_VERSION_RELEASE_NOTES, xml.release.releaseNotes, requestedIds);
        setResourceProperty(resource, REPOSITORY_VERSION_AVAILABLE_SERVICES, xml.getAvailableServices(stack), requestedIds);
        stackServices = xml.getStackServices(stack);
      } else {
        stackServices = new ArrayList<>();

        for (ServiceInfo si : stack.getServices()) {
          stackServices.add(new ManifestServiceInfo(si.getName(), si.getDisplayName(), si.getComment(),
              Collections.singleton(si.getVersion())));
        }

      }

      setResourceProperty(resource, REPOSITORY_VERSION_STACK_SERVICES, stackServices, requestedIds);

      resources.add(resource);
    }
    return resources;
  }

  @Override
  @Transactional
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Map<String, Object>> propertyMaps = request.getProperties();

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        for (Map<String, Object> propertyMap : propertyMaps) {
          final Long id;
          try {
            id = Long.parseLong(propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID).toString());
          } catch (Exception ex) {
            throw new AmbariException("Repository version should have numerical id");
          }

          final RepositoryVersionEntity entity = repositoryVersionDAO.findByPK(id);
          if (entity == null) {
            throw new ObjectNotFoundException("There is no repository version with id " + id);
          }

          List<OperatingSystemEntity> operatingSystemEntities = null;

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID)))) {
            if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_EDIT_STACK_REPOS)) {
              throw new AuthorizationException("The authenticated user does not have authorization to modify stack repositories");
            }

            final Object operatingSystems = propertyMap.get(SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID);
            final String operatingSystemsJson = gson.toJson(operatingSystems);
            try {
              operatingSystemEntities = repositoryVersionHelper.parseOperatingSystems(operatingSystemsJson);
            } catch (Exception ex) {
              throw new AmbariException("Json structure for operating systems is incorrect", ex);
            }
            entity.setOperatingSystems(operatingSystemsJson);
          }

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID)))) {
            entity.setDisplayName(propertyMap.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID).toString());
          }

          validateRepositoryVersion(repositoryVersionDAO, ambariMetaInfo, entity);

          repositoryVersionDAO.merge(entity);

          //
          // Update metaInfo table as well
          //
          if (operatingSystemEntities != null) {
            String entityStackName = entity.getStackName();
            String entityStackVersion = entity.getStackVersion();
            for (OperatingSystemEntity osEntity : operatingSystemEntities) {
              List<RepositoryEntity> repositories = osEntity.getRepositories();
              for (RepositoryEntity repository : repositories) {
                ambariMetaInfo.updateRepo(entityStackName, entityStackVersion, osEntity.getOsType(), repository.getRepositoryId(), repository.getBaseUrl(), repository.getMirrorsList());
              }
            }
          }
        }
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
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

      StackEntity stackEntity = entity.getStack();
      String stackName = stackEntity.getStackName();
      String stackVersion = stackEntity.getStackVersion();

      final List<ClusterVersionEntity> clusterVersionEntities = clusterVersionDAO.findByStackAndVersion(
          stackName, stackVersion, entity.getVersion());

      final List<RepositoryVersionState> forbiddenToDeleteStates = Lists.newArrayList(
          RepositoryVersionState.CURRENT,
          RepositoryVersionState.INSTALLED,
          RepositoryVersionState.INSTALLING);
      for (ClusterVersionEntity clusterVersionEntity : clusterVersionEntities) {
        if (clusterVersionEntity.getRepositoryVersion().getId().equals(id) && forbiddenToDeleteStates.contains(clusterVersionEntity.getState())) {
          throw new SystemException("Repository version can't be deleted as it is " +
              clusterVersionEntity.getState().name() + " on cluster " + clusterVersionEntity.getClusterEntity().getClusterName());
        }
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

  /**
   * Validates newly created repository versions to contain actual information.
   *
   * @throws AmbariException exception with error message
   */
  protected static void validateRepositoryVersion(RepositoryVersionDAO dao,
      AmbariMetaInfo metaInfo, RepositoryVersionEntity repositoryVersion) throws AmbariException {
    validateRepositoryVersion(dao, metaInfo, repositoryVersion, false);
  }

  /**
   * Validates newly created repository versions to contain actual information.  Optionally
   * skip url duplication.
   *
   * @throws AmbariException exception with error message
   */
  protected static void validateRepositoryVersion(RepositoryVersionDAO dao,
      AmbariMetaInfo metaInfo, RepositoryVersionEntity repositoryVersion, boolean skipUrlCheck) throws AmbariException {
    final StackId requiredStack = new StackId(repositoryVersion.getStack());

    final String requiredStackName = requiredStack.getStackName();
    final String requiredStackVersion = requiredStack.getStackVersion();
    final String requiredStackId = requiredStack.getStackId();

    // List of all repo urls that are already added at stack
    Set<String> existingRepoUrls = new HashSet<String>();
    List<RepositoryVersionEntity> existingRepoVersions = dao.findByStack(requiredStack);
    for (RepositoryVersionEntity existingRepoVersion : existingRepoVersions) {
      for (OperatingSystemEntity operatingSystemEntity : existingRepoVersion.getOperatingSystems()) {
        for (RepositoryEntity repositoryEntity : operatingSystemEntity.getRepositories()) {
          if (repositoryEntity.isUnique() && !existingRepoVersion.getId().equals(repositoryVersion.getId())) { // Allow modifying already defined repo version
            existingRepoUrls.add(repositoryEntity.getBaseUrl());
          }
        }
      }
    }

    // check that repositories contain only supported operating systems
    final Set<String> osSupported = new HashSet<String>();
    for (OperatingSystemInfo osInfo: metaInfo.getOperatingSystems(requiredStackName, requiredStackVersion)) {
      osSupported.add(osInfo.getOsType());
    }

    final Set<String> osRepositoryVersion = new HashSet<String>();

    for (OperatingSystemEntity os: repositoryVersion.getOperatingSystems()) {
      osRepositoryVersion.add(os.getOsType());

      for (RepositoryEntity repositoryEntity : os.getRepositories()) {
        String baseUrl = repositoryEntity.getBaseUrl();
        if (!skipUrlCheck && os.isAmbariManagedRepos() && existingRepoUrls.contains(baseUrl)) {
          throw new AmbariException("Base url " + baseUrl + " is already defined for another repository version. " +
                  "Setting up base urls that contain the same versions of components will cause stack upgrade to fail.");
        }
      }
    }

    if (osRepositoryVersion.isEmpty()) {
      throw new AmbariException("At least one set of repositories for OS should be provided");
    }

    for (String os: osRepositoryVersion) {
      if (!osSupported.contains(os)) {
        throw new AmbariException("Operating system type " + os + " is not supported by stack " + requiredStackId);
      }
    }

    if (!RepositoryVersionEntity.isVersionInStack(repositoryVersion.getStackId(), repositoryVersion.getVersion())) {
      throw new AmbariException(MessageFormat.format("Version {0} needs to belong to stack {1}",
          repositoryVersion.getVersion(), repositoryVersion.getStackName() + "-" + repositoryVersion.getStackVersion()));
    }
  }

  /**
   * Transforms map of json properties to repository version entity.
   *
   * @param properties json map
   * @return constructed entity
   * @throws AmbariException if some properties are missing or json has incorrect structure
   */
  protected RepositoryVersionEntity toRepositoryVersionEntity(Map<String, Object> properties) throws AmbariException {
    final RepositoryVersionEntity entity = new RepositoryVersionEntity();

    final String stackName = properties.get(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString();
    final String stackVersion = properties.get(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString();

    StackEntity stackEntity = stackDAO.find(stackName, stackVersion);

    entity.setDisplayName(properties.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID).toString());
    entity.setStack(stackEntity);

    entity.setVersion(properties.get(REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID).toString());
    final Object operatingSystems = properties.get(SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID);
    final String operatingSystemsJson = gson.toJson(operatingSystems);
    try {
      repositoryVersionHelper.parseOperatingSystems(operatingSystemsJson);
    } catch (Exception ex) {
      throw new AmbariException("Json structure for operating systems is incorrect", ex);
    }
    entity.setOperatingSystems(operatingSystemsJson);

    return entity;
  }

  protected StackId getStackInformationFromUrl(Map<String, Object> propertyMap) {
    if (propertyMap.containsKey(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID) && propertyMap.containsKey(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID)) {
      return new StackId(propertyMap.get(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString(), propertyMap.get(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString());
    }
    return null;
  }

  @Override
  protected ResourceType getResourceType(Request request, Predicate predicate) {
    // This information is not associated with any particular resource
    return null;
  }
}
