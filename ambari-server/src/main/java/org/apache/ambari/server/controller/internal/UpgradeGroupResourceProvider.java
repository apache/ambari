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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.UpgradeState;

import com.google.inject.Inject;

/**
 * Manages groupings of upgrade items.
 */
@StaticallyInject
public class UpgradeGroupResourceProvider extends AbstractControllerResourceProvider {

  protected static final String UPGRADE_ID = "UpgradeGroup/upgrade_id";
  protected static final String UPGRADE_GROUP_ID = "UpgradeGroup/group_id";
  protected static final String UPGRADE_GROUP_NAME = "UpgradeGroup/name";
  protected static final String UPGRADE_GROUP_TITLE = "UpgradeGroup/title";
  protected static final String UPGRADE_GROUP_STATE = "UpgradeGroup/state";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(UPGRADE_ID, UPGRADE_GROUP_ID));
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();

  @Inject
  private static UpgradeDAO m_dao = null;

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_ID);
    PROPERTY_IDS.add(UPGRADE_GROUP_ID);
    PROPERTY_IDS.add(UPGRADE_GROUP_NAME);
    PROPERTY_IDS.add(UPGRADE_GROUP_TITLE);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeGroup, UPGRADE_GROUP_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_ID);
  }

  /**
   * Constructor.
   *
   * @param controller
   */
  UpgradeGroupResourceProvider(AmbariManagementController controller) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new SystemException("Upgrade Groups can only be created with an upgrade");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<Resource>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String upgradeIdStr = (String) propertyMap.get(UPGRADE_ID);

      if (null == upgradeIdStr || upgradeIdStr.isEmpty()) {
        throw new IllegalArgumentException("The upgrade id is required when querying for upgrades");
      }

      long upgradeId = Long.parseLong(upgradeIdStr);
      UpgradeEntity upgrade = m_dao.findUpgrade(upgradeId);

      List<UpgradeGroupEntity> groups = upgrade.getUpgradeGroups();
      if (null != groups) {
        for (UpgradeGroupEntity group : upgrade.getUpgradeGroups()) {
          results.add(toResource(upgrade, group, requestPropertyIds));
        }
      }

    }

    return results;
  }

  @Override
  public RequestStatus updateResources(final Request request,
      Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    throw new SystemException("Upgrade Items cannot be modified at this time");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Cannot delete upgrade items");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  private Resource toResource(UpgradeEntity upgrade, UpgradeGroupEntity group, Set<String> requestedIds) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.UpgradeGroup);

    setResourceProperty(resource, UPGRADE_ID, upgrade.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_ID, group.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_NAME, group.getName(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_TITLE, group.getTitle(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_STATE, "NONE", requestedIds);

    return resource;
  }

}
