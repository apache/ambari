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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
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
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Manages the ability to start and get status of upgrades.
 */
@StaticallyInject
public class UpgradeItemResourceProvider extends AbstractControllerResourceProvider {

  protected static final String UPGRADE_ID = "UpgradeItem/upgrade_id";
  protected static final String UPGRADE_ITEM_ID = "UpgradeItem/id";
  protected static final String UPGRADE_ITEM_STATE = "UpgradeItem/state";
  protected static final String UPGRADE_ITEM_TEXT = "UpgradeItem/text";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(UPGRADE_ID, UPGRADE_ITEM_ID));
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();

  @Inject
  private static UpgradeDAO m_dao = null;
  @Inject
  private static Provider<AmbariMetaInfo> m_metaProvider = null;

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_ID);
    PROPERTY_IDS.add(UPGRADE_ITEM_ID);
    PROPERTY_IDS.add(UPGRADE_ITEM_STATE);
    PROPERTY_IDS.add(UPGRADE_ITEM_TEXT);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeItem, UPGRADE_ITEM_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_ID);
  }

  /**
   * Constructor.
   *
   * @param controller
   */
  UpgradeItemResourceProvider(AmbariManagementController controller) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new SystemException("Upgrade Items can only be created with an upgrade");
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

      if (null == upgrade) {
        throw new NoSuchResourceException(String.format("Cannot load upgrade for %s", upgradeIdStr));
      }

      Collection<UpgradeItemEntity> items = upgrade.getUpgradeItems();
      if (null != items) {
        for (UpgradeItemEntity entity : items) {
          results.add(toResource(entity, upgradeId, requestPropertyIds));
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

  private Resource toResource(UpgradeItemEntity entity, long upgradeId, Set<String> requestedIds) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.UpgradeItem);

    setResourceProperty(resource, UPGRADE_ID, Long.valueOf(upgradeId), requestedIds);
    setResourceProperty(resource, UPGRADE_ITEM_ID, entity.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_ITEM_STATE, entity.getState(), requestedIds);
    setResourceProperty(resource, UPGRADE_ITEM_TEXT, entity.getText(), requestedIds);

    return resource;
  }

}
