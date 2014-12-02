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
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
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
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.inject.Inject;

/**
 * Resource provider for repository versions resources.
 */
public class RepositoryVersionResourceProvider extends AbstractResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String REPOSITORY_VERSION_ID_PROPERTY_ID                 = PropertyHelper.getPropertyId("RepositoryVersions", "id");
  public static final String REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID         = PropertyHelper.getPropertyId("RepositoryVersions", "stack_name");
  public static final String REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID      = PropertyHelper.getPropertyId("RepositoryVersions", "stack_version");
  public static final String REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("RepositoryVersions", "repository_version");
  public static final String REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID       = PropertyHelper.getPropertyId("RepositoryVersions", "display_name");
  public static final String REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID       = PropertyHelper.getPropertyId("RepositoryVersions", "upgrade_pack");
  public static final String REPOSITORY_VERSION_OPERATING_SYSTEMS_PROPERTY_ID  = PropertyHelper.getPropertyId("RepositoryVersions", "operating_systems");

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
      add(REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
      add(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID);
      add(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID);
      add(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
      add(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID);
      add(REPOSITORY_VERSION_OPERATING_SYSTEMS_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  private static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Resource.Type.Stack, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID);
      put(Resource.Type.StackVersion, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
      put(Resource.Type.RepositoryVersion, REPOSITORY_VERSION_ID_PROPERTY_ID);
    }
  };

  private static Gson gson = new Gson();

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private ClusterVersionDAO clusterVersionDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

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
          final String[] requiredProperties = {
              REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
              REPOSITORY_VERSION_OPERATING_SYSTEMS_PROPERTY_ID,
              REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
              REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
              REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID
          };
          for (String propertyName: requiredProperties) {
            if (properties.get(propertyName) == null) {
              throw new AmbariException("Property " + propertyName + " should be provided");
            }
          }
          final RepositoryVersionEntity entity = toRepositoryVersionEntity(properties);

          if (repositoryVersionDAO.findByDisplayName(entity.getDisplayName()) != null) {
            throw new AmbariException("Repository version with name " + entity.getDisplayName() + " already exists");
          }
          if (repositoryVersionDAO.findByStackAndVersion(entity.getStack(), entity.getVersion()) != null) {
            throw new AmbariException("Repository version for stack " + entity.getStack() + " and version " + entity.getVersion() + " already exists");
          }
          validateRepositoryVersion(entity);
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
    for (Map<String, Object> propertyMap: propertyMaps) {
      final StackId stackId = getStackInformationFromUrl(propertyMap);
      if (propertyMaps.size() == 1 && propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID) == null) {
        requestedEntities.addAll(repositoryVersionDAO.findByStack(stackId.getStackId()));
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
      setResourceProperty(resource, REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID, entity.getUpgradePackage(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, entity.getVersion(), requestedIds);

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Map<String, Object>> propertyMaps = request.getProperties();

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
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

          if (propertyMap.get(REPOSITORY_VERSION_OPERATING_SYSTEMS_PROPERTY_ID) != null) {

            final List<ClusterVersionEntity> clusterVersionEntities =
                clusterVersionDAO.findByStackAndVersion(entity.getStack(), entity.getVersion());

            if (!clusterVersionEntities.isEmpty()) {
              final ClusterVersionEntity firstClusterVersion = clusterVersionEntities.get(0);
              throw new AmbariException("Repository version can't be updated as it is " +
                firstClusterVersion.getState().name() + " on cluster " + firstClusterVersion.getClusterEntity().getClusterName());
            }

            if (propertyMap.get(REPOSITORY_VERSION_OPERATING_SYSTEMS_PROPERTY_ID) != null) {
              final Object repositories = propertyMap.get(REPOSITORY_VERSION_OPERATING_SYSTEMS_PROPERTY_ID);
              entity.setOperatingSystems(gson.toJson(repositories));
            }

          }

          if (propertyMap.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID) != null) {
            entity.setDisplayName(propertyMap.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID).toString());
          }

          validateRepositoryVersion(entity);
          repositoryVersionDAO.merge(entity);
        }
        return null;
      }
    });

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

  /**
   * Validates newly created repository versions to contain actual information.
   *
   * @param repositoryVersion repository version
   * @throws AmbariException exception with error message
   */
  protected void validateRepositoryVersion(RepositoryVersionEntity repositoryVersion) throws AmbariException {
    final StackId requiredStack = new StackId(repositoryVersion.getStack());
    final String stackName = requiredStack.getStackName();
    final String stackMajorVersion = requiredStack.getStackVersion();
    final String stackFullName = requiredStack.getStackId();

    // check that stack exists
    final StackInfo stackInfo = ambariMetaInfo.getStack(stackName, stackMajorVersion);
    if (stackInfo.getUpgradePacks() == null) {
      throw new AmbariException("Stack " + stackFullName + " doesn't have upgrade packages");
    }

    // check that repositories contain only supported operating systems
    final Set<String> osSupported = new HashSet<String>();
    for (OperatingSystemInfo osInfo: ambariMetaInfo.getOperatingSystems(stackName, stackMajorVersion)) {
      osSupported.add(osInfo.getOsType());
    }
    final Set<String> osRepositoryVersion = new HashSet<String>();
    for (OperatingSystemEntity os: repositoryVersion.getOperatingSystems()) {
      osRepositoryVersion.add(os.getOsType());
    }
    if (osRepositoryVersion.isEmpty()) {
      throw new AmbariException("At least one set of repositories for OS should be provided");
    }
    for (String os: osRepositoryVersion) {
      if (!osSupported.contains(os)) {
        throw new AmbariException("Operating system type " + os + " is not supported by stack " + stackFullName);
      }
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
    entity.setDisplayName(properties.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID).toString());
    entity.setStack(new StackId(stackName, stackVersion).getStackId());
    entity.setUpgradePackage(properties.get(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID).toString());
    entity.setVersion(properties.get(REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID).toString());
    final Object repositories = properties.get(REPOSITORY_VERSION_OPERATING_SYSTEMS_PROPERTY_ID);
    final String repositoriesJson = gson.toJson(repositories);
    try {
      parseRepositories(repositoriesJson);
    } catch (Exception ex) {
      throw new AmbariException("Json structure for operating systems is incorrect", ex);
    }
    entity.setOperatingSystems(repositoriesJson);
    entity.setUpgradePackage(getUpgradePackageName(stackName, stackVersion, entity.getVersion()));
    return entity;
  }


  /**
   * Scans the given stack for upgrade packages which can be applied to update the cluster to given repository version.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @param repositoryVersion target repository version
   * @return upgrade pack name
   * @throws AmbariException if no upgrade packs suit the requirements
   */
  private String getUpgradePackageName(String stackName, String stackVersion, String repositoryVersion) throws AmbariException {
    final Map<String, UpgradePack> upgradePacks = ambariMetaInfo.getUpgradePacks(stackName, stackVersion);
    for (Entry<String, UpgradePack> upgradePackEntry : upgradePacks.entrySet()) {
      final UpgradePack upgradePack = upgradePackEntry.getValue();
      final String upgradePackName = upgradePackEntry.getKey();
      // check that upgrade pack has <target> node
      if (StringUtils.isBlank(upgradePack.getTarget())) {
        LOG.error("Upgrade pack " + upgradePackName + " is corrupted, it should contain <target> node");
        continue;
      }

      // check that upgrade pack can be applied to selected stack
      // converting 2.2.*.* -> 2\.2(\.\d+)?(\.\d+)?(-\d+)?
      String regexPattern = upgradePack.getTarget();
      regexPattern = regexPattern.replaceAll("\\.", "\\\\."); // . -> \.
      regexPattern = regexPattern.replaceAll("\\\\\\.\\*", "(\\\\\\.\\\\d+)?"); // \.* -> (\.\d+)?
      regexPattern = regexPattern.concat("(-\\d+)?");
      if (Pattern.matches(regexPattern, repositoryVersion)) {
        return upgradePackName;
      }
    }
    throw new AmbariException("There were no suitable upgrade packs for stack " + stackName + " " + stackVersion);
  }

  /**
   * Parses operating systems json to a list of entities. Expects json like:
   * <pre>
   * [
   *    {
   *       "repositories":[
   *          {
   *             "Repositories/base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos5/2.x/updates/2.2.0.0",
   *             "Repositories/repo_name":"HDP-UTILS",
   *             "Repositories/repo_id":"HDP-UTILS-1.1.0.20"
   *          },
   *          {
   *             "Repositories/base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos5/2.x/updates/2.2.0.0",
   *             "Repositories/repo_name":"HDP",
   *             "Repositories/repo_id":"HDP-2.2"
   *          }
   *       ],
   *       "OperatingSystems/os_type":"redhat5"
   *    }
   * ]
   * </pre>
   * @param repositoriesJson operating systems json
   * @return list of operating system entities
   * @throws Exception if any kind of json parsing error happened
   */
  public static List<OperatingSystemEntity> parseRepositories(String repositoriesJson) throws Exception {
    final List<OperatingSystemEntity> operatingSystems = new ArrayList<OperatingSystemEntity>();
    final JsonArray rootJson = new JsonParser().parse(repositoriesJson).getAsJsonArray();
    final String repositoriesSubresourceName = new RepositoryResourceDefinition().getPluralName();
    for (JsonElement operatingSystemJson: rootJson) {
      final OperatingSystemEntity operatingSystemEntity = new OperatingSystemEntity();
      operatingSystemEntity.setOsType(operatingSystemJson.getAsJsonObject().get(OperatingSystemResourceProvider.OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID).getAsString());
      for (JsonElement repositoryJson: operatingSystemJson.getAsJsonObject().get(repositoriesSubresourceName).getAsJsonArray()) {
        final RepositoryEntity repositoryEntity = new RepositoryEntity();
        repositoryEntity.setBaseUrl(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_BASE_URL_PROPERTY_ID).getAsString());
        repositoryEntity.setName(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_REPO_NAME_PROPERTY_ID).getAsString());
        repositoryEntity.setRepositoryId(repositoryJson.getAsJsonObject().get(RepositoryResourceProvider.REPOSITORY_REPO_ID_PROPERTY_ID).getAsString());
        operatingSystemEntity.getRepositories().add(repositoryEntity);
      }
      operatingSystems.add(operatingSystemEntity);
    }
    return operatingSystems;
  }

  protected StackId getStackInformationFromUrl(Map<String, Object> propertyMap) {
    if (propertyMap.containsKey(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID) && propertyMap.containsKey(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID)) {
      return new StackId(propertyMap.get(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString(), propertyMap.get(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString());
    }
    return null;
  }

}
