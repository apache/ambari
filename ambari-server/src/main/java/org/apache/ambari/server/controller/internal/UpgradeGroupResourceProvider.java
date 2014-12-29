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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
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
import org.apache.ambari.server.state.UpgradeHelper;

import com.google.inject.Inject;

/**
 * Manages groupings of upgrade items.
 */
@StaticallyInject
public class UpgradeGroupResourceProvider extends AbstractControllerResourceProvider {

  protected static final String UPGRADE_REQUEST_ID = "UpgradeGroup/request_id";
  protected static final String UPGRADE_GROUP_ID = "UpgradeGroup/group_id";
  protected static final String UPGRADE_CLUSTER_NAME = "UpgradeGroup/cluster_name";
  protected static final String UPGRADE_GROUP_NAME = "UpgradeGroup/name";
  protected static final String UPGRADE_GROUP_TITLE = "UpgradeGroup/title";
  protected static final String UPGRADE_GROUP_PROGRESS_PERCENT = "UpgradeGroup/progress_percent";
  protected static final String UPGRADE_GROUP_STATUS = "UpgradeGroup/status";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(UPGRADE_REQUEST_ID, UPGRADE_GROUP_ID));
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();

  @Inject
  private static UpgradeDAO m_dao = null;

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_REQUEST_ID);
    PROPERTY_IDS.add(UPGRADE_GROUP_ID);
    PROPERTY_IDS.add(UPGRADE_GROUP_NAME);
    PROPERTY_IDS.add(UPGRADE_GROUP_TITLE);
    PROPERTY_IDS.add(UPGRADE_GROUP_PROGRESS_PERCENT);
    PROPERTY_IDS.add(UPGRADE_GROUP_STATUS);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeGroup, UPGRADE_GROUP_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_REQUEST_ID);
  }

  /**
   * Constructor.
   *
   * @param controller the controller
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
      String upgradeIdStr = (String) propertyMap.get(UPGRADE_REQUEST_ID);
      String clusterName = (String) propertyMap.get(UPGRADE_CLUSTER_NAME);

      if (null == upgradeIdStr || upgradeIdStr.isEmpty()) {
        throw new IllegalArgumentException("The upgrade id is required when querying for upgrades");
      }

      Long upgradeId = Long.valueOf(upgradeIdStr);
      UpgradeEntity upgrade = m_dao.findUpgradeByRequestId(upgradeId);

      List<UpgradeGroupEntity> groups = upgrade.getUpgradeGroups();
      if (null != groups) {
        UpgradeHelper helper = new UpgradeHelper();

        for (UpgradeGroupEntity group : upgrade.getUpgradeGroups()) {
          Resource r = toResource(upgrade, group, requestPropertyIds);

          List<Long> stageIds = new ArrayList<Long>();
          for (UpgradeItemEntity itemEntity : group.getItems()) {
            stageIds.add(itemEntity.getStageId());
          }

          Set<Resource> stages = helper.getStageResources(clusterName, upgrade.getRequestId(), stageIds);

          aggregate(r, stages, requestPropertyIds);

          results.add(r);
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

    setResourceProperty(resource, UPGRADE_REQUEST_ID, upgrade.getRequestId(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_ID, group.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_NAME, group.getName(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_TITLE, group.getTitle(), requestedIds);

    return resource;
  }

  /**
   * Aggregates status and percent complete for stages and puts the results on the upgrade group
   *
   * @param upgradeGroup  the resource representing an upgrade group
   * @param stages        the collection of resources representing stages
   * @param requestedIds  the ids for the request
   */
  private void aggregate(Resource upgradeGroup, Collection<Resource> stages, Set<String> requestedIds) {

    Map<HostRoleStatus, Integer> counters =
        StageResourceProvider.calculateTaskStatusCounts(getHostRoleStatuses(stages));

    setResourceProperty(upgradeGroup, UPGRADE_GROUP_STATUS,
        StageResourceProvider.calculateSummaryStatus(counters, stages.size(), false), requestedIds);

    setResourceProperty(upgradeGroup, UPGRADE_GROUP_PROGRESS_PERCENT,
        StageResourceProvider.calculateProgressPercent(counters, stages.size()), requestedIds);
  }

  /**
   * Get a collection of statuses from the given collection of stage resources.
   *
   * @param stageResources  the stage resources
   *
   * @return a collection of statuses
   */
  private static Collection<HostRoleStatus> getHostRoleStatuses(Collection<Resource> stageResources) {
    Collection<HostRoleStatus> hostRoleStatuses = new LinkedList<HostRoleStatus>();

    for (Resource stage : stageResources) {
      HostRoleStatus status = getStatus(stage);

      if (status != null) {
        hostRoleStatuses.add(status);
      }
    }
    return hostRoleStatuses;
  }

  /**
   * Get the status of the given stage resource.
   *
   * @param stageResource  the resource
   *
   * @return  the stage status
   */
  private static HostRoleStatus getStatus(Resource stageResource) {
    String status = (String) stageResource.getPropertyValue(StageResourceProvider.STAGE_STATUS);

    return status == null ? null : HostRoleStatus.valueOf(status);
  }
}
