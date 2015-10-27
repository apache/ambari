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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.resources.OperatingSystemResourceDefinition;
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
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Resource provider for repository versions resources.
 */
@StaticallyInject
public class CompatibleRepositoryVersionResourceProvider extends ReadOnlyResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String REPOSITORY_VERSION_ID_PROPERTY_ID = "CompatibleRepositoryVersions/id";
  public static final String REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID = "CompatibleRepositoryVersions/stack_name";
  public static final String REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID = "CompatibleRepositoryVersions/stack_version";
  public static final String REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID = "CompatibleRepositoryVersions/repository_version";
  public static final String REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID = "CompatibleRepositoryVersions/display_name";
  public static final String REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID = "CompatibleRepositoryVersions/upgrade_types";
  public static final String SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID = new OperatingSystemResourceDefinition().getPluralName();

  private static Set<String> pkPropertyIds = Collections.singleton(REPOSITORY_VERSION_ID_PROPERTY_ID);

  static Set<String> propertyIds = Sets.newHashSet(
    REPOSITORY_VERSION_ID_PROPERTY_ID,
    REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID,
    REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
    REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
    REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
    SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
    REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID);

  static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Type.Stack, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID);
      put(Type.StackVersion, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
      put(Type.Upgrade, REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID);
      put(Type.CompatibleRepositoryVersion, REPOSITORY_VERSION_ID_PROPERTY_ID);
    }
  };

  @Inject
  private static RepositoryVersionDAO s_repositoryVersionDAO;

  @Inject
  private static Provider<AmbariMetaInfo> s_ambariMetaInfo;

  @Inject
  private static Provider<RepositoryVersionHelper> s_repositoryVersionHelper;

  /**
   * Create a new resource provider.
   */
  public CompatibleRepositoryVersionResourceProvider(AmbariManagementController amc) {
    super(propertyIds, keyPropertyIds, amc);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<Resource>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    List<CompatibleRepositoryVersion> requestedEntities = new ArrayList<CompatibleRepositoryVersion>();
    String currentStackUniqueId = null;
    Map<String, CompatibleRepositoryVersion> compatibleRepositoryVersionsMap = new HashMap<String, CompatibleRepositoryVersion>();

    for (Map<String, Object> propertyMap : propertyMaps) {

      final StackId stackId = getStackInformationFromUrl(propertyMap);

      if (stackId != null) {
        if (propertyMaps.size() == 1) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Stack Name : " + stackId.getStackName() + ", Stack Version : " + stackId.getStackVersion());
          }
          for (RepositoryVersionEntity repositoryVersionEntity : s_repositoryVersionDAO.findByStack(stackId)) {
            currentStackUniqueId = Long.toString(repositoryVersionEntity.getId());
            compatibleRepositoryVersionsMap.put(currentStackUniqueId, new CompatibleRepositoryVersion(repositoryVersionEntity));
            if (LOG.isDebugEnabled()) {
              LOG.debug("Added current stack id : " + currentStackUniqueId + " to Map.");
            }
          }

          Map<String, UpgradePack> packs = s_ambariMetaInfo.get().getUpgradePacks(
            stackId.getStackName(), stackId.getStackVersion());

          for (UpgradePack up : packs.values()) {
            if (null != up.getTargetStack()) {
              StackId targetStackId = new StackId(up.getTargetStack());
              List<RepositoryVersionEntity> repositoryVersionEntities = s_repositoryVersionDAO.findByStack(targetStackId);
              for (RepositoryVersionEntity repositoryVersionEntity : repositoryVersionEntities) {
                if (compatibleRepositoryVersionsMap.containsKey(Long.toString(repositoryVersionEntity.getId()))) {
                  compatibleRepositoryVersionsMap.get(Long.toString(repositoryVersionEntity.getId())).addUpgradePackType(up.getType().toString());
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Stack id : " + repositoryVersionEntity.getId() + " exists in Map. " + "Appended new Upgrade type : " + up.getType());
                  }
                } else {
                  CompatibleRepositoryVersion compatibleRepositoryVersionEntity = new CompatibleRepositoryVersion(repositoryVersionEntity);
                  compatibleRepositoryVersionEntity.addUpgradePackType(up.getType().toString());
                  compatibleRepositoryVersionsMap.put(Long.toString(repositoryVersionEntity.getId()), compatibleRepositoryVersionEntity);
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("Added Stack id : " + repositoryVersionEntity.getId() + " to Map with Upgrade type : " + up.getType());
                  }
                }
              }
            } else {
              if (currentStackUniqueId != null) {
                compatibleRepositoryVersionsMap.get(currentStackUniqueId).addUpgradePackType(up.getType().toString());
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Current Stack id : " + currentStackUniqueId + " retrieved from Map. Added Upgrade type : " + up.getType());
                }
              } else {
                LOG.error("Couldn't retrieve Current stack entry from Map.");
              }
            }
          }

        } else {
          LOG.error("Property Maps size NOT equal to 1. Current 'propertyMaps' size = " + propertyMaps.size());
        }
      } else {
        LOG.error("StackId is NULL.");
      }
    }

    for (String stackId : compatibleRepositoryVersionsMap.keySet()) {
      CompatibleRepositoryVersion entity = compatibleRepositoryVersionsMap.get(stackId);
      RepositoryVersionEntity repositoryVersionEntity = entity.getRepositoryVersionEntity();
      final Resource resource = new ResourceImpl(Resource.Type.CompatibleRepositoryVersion);
      setResourceProperty(resource, REPOSITORY_VERSION_ID_PROPERTY_ID, repositoryVersionEntity.getId(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, repositoryVersionEntity.getStackName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, repositoryVersionEntity.getStackVersion(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, repositoryVersionEntity.getDisplayName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, repositoryVersionEntity.getVersion(), requestedIds);
      setResourceProperty(resource, REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID, entity.getSupportedTypes(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Gets the stack id from the request map
   *
   * @param propertyMap the request map
   * @return the StackId, or {@code null} if not found.
   */
  protected StackId getStackInformationFromUrl(Map<String, Object> propertyMap) {
    if (propertyMap.containsKey(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID) && propertyMap.containsKey(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID)) {
      return new StackId(propertyMap.get(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString(), propertyMap.get(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString());
    }
    return null;
  }

}
