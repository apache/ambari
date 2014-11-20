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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
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
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
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

  /**
   * Json schema used for repositories validation.
   */
  private static JsonSchema repositoriesJsonSchema;
  static {
    final String schema = "{\"type\":\"array\",\"$schema\":\"http://json-schema.org/draft-04/schema#\","
        + "\"items\":{\"type\":\"object\",\"required\":[\"baseurls\",\"os\"],\"properties\":{\"baseurls\":{\"type\":\"array\",\"items\":"
        + "{\"type\":\"object\",\"required\":[\"type\",\"baseurl\",\"id\"],\"properties\":{\"type\":{\"type\":\"string\"},\"baseurl\":"
        + "{\"type\":\"string\"},\"id\":{\"type\":\"string\"}}},\"minItems\":1},\"os\":{\"type\":\"string\"}}}\r\n,\"minItems\":1}";
    try {
      repositoriesJsonSchema = JsonSchemaFactory.byDefault().getJsonSchema(JsonLoader.fromString(schema));
    } catch (Exception e) {
      LOG.error("Could not create instance of json schema for validating repositories");
    }
  }

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

          if (propertyMap.get(REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID) != null
              || propertyMap.get(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID) != null) {

            final List<ClusterVersionEntity> clusterVersionEntities =
                clusterVersionDAO.findByStackAndVersion(entity.getStack(), entity.getVersion());

            if (!clusterVersionEntities.isEmpty()) {
              final ClusterVersionEntity firstClusterVersion = clusterVersionEntities.get(0);
              throw new AmbariException("Repository version can't be updated as it is " +
                firstClusterVersion.getState().name() + " on cluster " + firstClusterVersion.getClusterEntity().getClusterName());
            }

            if (propertyMap.get(REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID) != null) {
              final Object repositories = propertyMap.get(REPOSITORY_VERSION_REPOSITORIES_PROPERTY_ID);
              entity.setRepositories(new Gson().toJson(repositories));
            }

            if (propertyMap.get(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID) != null) {
              entity.setUpgradePackage(propertyMap.get(REPOSITORY_VERSION_UPGRADE_PACK_PROPERTY_ID).toString());
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

    // check that given repositories node is a valid json
    ProcessingReport jsonValidationReport;
    JsonNode repositoriesJson;
    try {
      repositoriesJson = JsonLoader.fromString(repositoryVersion.getRepositories());
      jsonValidationReport = repositoriesJsonSchema.validate(repositoriesJson);
    } catch (Exception ex) {
      throw new AmbariException("Could not process repositories json");
    }
    if (!jsonValidationReport.isSuccess()) {
      final StringBuilder errors = new StringBuilder();
      final Iterator<ProcessingMessage> iterator = jsonValidationReport.iterator();
      while (iterator.hasNext()) {
        errors.append(iterator.next().toString());
      }
      throw new AmbariException("Failed to validate repositories json: " + errors.toString());
    }

    // check that repositories contain only supported operating systems
    final Set<String> osSupported = new HashSet<String>();
    for (OperatingSystemInfo osInfo: ambariMetaInfo.getOperatingSystems(stackName, stackMajorVersion)) {
      osSupported.add(osInfo.getOsType());
    }
    final Set<String> osRepositoryVersion = new HashSet<String>();
    final Iterator<JsonNode> repositoriesIterator = repositoriesJson.elements();
    while (repositoriesIterator.hasNext()) {
      osRepositoryVersion.add(repositoriesIterator.next().get("os").asText());
    }
    if (osRepositoryVersion.isEmpty()) {
      throw new AmbariException("At least one set of repositories for OS should be provided");
    }
    for (String os: osRepositoryVersion) {
      if (!osSupported.contains(os)) {
        throw new AmbariException("Operating system type " + os + " is not supported by stack " + stackFullName);
      }
    }

    // check that upgrade pack for the stack exists
    final UpgradePack upgradePack = stackInfo.getUpgradePacks().get(repositoryVersion.getUpgradePackage());
    if (upgradePack == null) {
      throw new AmbariException("Upgrade pack " + repositoryVersion.getUpgradePackage()
          + " is not available for stack " + stackFullName);
    }

    // check that upgrade pack has <target> node
    if (StringUtils.isBlank(upgradePack.getTarget())) {
      throw new AmbariException("Upgrade pack " + repositoryVersion.getUpgradePackage()
          + " is corrupted, it should contain <target> node");
    }

    // check that upgrade pack can be applied to selected stack
    // converting 2.2.*.* -> 2\.2(\.\d+)?(\.\d+)?(-\d+)?
    String regexPattern = upgradePack.getTarget();
    regexPattern = regexPattern.replaceAll("\\.", "\\\\."); // . -> \.
    regexPattern = regexPattern.replaceAll("\\\\\\.\\*", "(\\\\\\.\\\\d+)?"); // \.* -> (\.\d+)?
    regexPattern = regexPattern.concat("(-\\d+)?");
    if (!Pattern.matches(regexPattern, repositoryVersion.getVersion())) {
      throw new AmbariException("Upgrade pack " + repositoryVersion.getUpgradePackage()
          + " can't be applied to stack " + stackFullName);
    }
  }
}
