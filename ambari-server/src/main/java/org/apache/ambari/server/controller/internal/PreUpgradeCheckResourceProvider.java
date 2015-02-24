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

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.checks.HostsHeartbeatCheck;
import org.apache.ambari.server.checks.HostsMasterMaintenanceCheck;
import org.apache.ambari.server.checks.HostsRepositoryVersionCheck;
import org.apache.ambari.server.checks.SecondaryNamenodeDeletedCheck;
import org.apache.ambari.server.checks.ServicesDecommissionCheck;
import org.apache.ambari.server.checks.ServicesMaintenanceModeCheck;
import org.apache.ambari.server.checks.ServicesMapReduceDistributedCacheCheck;
import org.apache.ambari.server.checks.ServicesNamenodeHighAvailabilityCheck;
import org.apache.ambari.server.checks.ServicesTezDistributedCacheCheck;
import org.apache.ambari.server.checks.ServicesUpCheck;
import org.apache.ambari.server.checks.ServicesYarnWorkPreservingCheck;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.CheckHelper;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;

import com.google.inject.Inject;

/**
 * Resource provider for pre-upgrade checks.
 */
@StaticallyInject
public class PreUpgradeCheckResourceProvider extends ReadOnlyResourceProvider {

  //----- Property ID constants ---------------------------------------------

  public static final String UPGRADE_CHECK_ID_PROPERTY_ID                 = PropertyHelper.getPropertyId("UpgradeChecks", "id");
  public static final String UPGRADE_CHECK_CHECK_PROPERTY_ID              = PropertyHelper.getPropertyId("UpgradeChecks", "check");
  public static final String UPGRADE_CHECK_STATUS_PROPERTY_ID             = PropertyHelper.getPropertyId("UpgradeChecks", "status");
  public static final String UPGRADE_CHECK_REASON_PROPERTY_ID             = PropertyHelper.getPropertyId("UpgradeChecks", "reason");
  public static final String UPGRADE_CHECK_FAILED_ON_PROPERTY_ID          = PropertyHelper.getPropertyId("UpgradeChecks", "failed_on");
  public static final String UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID         = PropertyHelper.getPropertyId("UpgradeChecks", "check_type");
  public static final String UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID       = PropertyHelper.getPropertyId("UpgradeChecks", "cluster_name");
  public static final String UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("UpgradeChecks", "repository_version");

  @Inject
  private static ServicesMaintenanceModeCheck servicesMaintenanceModeCheck;
  @Inject
  private static HostsMasterMaintenanceCheck hostsMasterMaintenanceCheck;
  @Inject
  private static HostsRepositoryVersionCheck hostsRepositoryVersionCheck;
  @Inject
  private static ServicesNamenodeHighAvailabilityCheck servicesNamenodeHighAvailabilityCheck;
  @Inject
  private static SecondaryNamenodeDeletedCheck secondaryNamenodeDeletedCheck;
  @Inject
  private static ServicesYarnWorkPreservingCheck servicesYarnWorkPreservingCheck;
  @Inject
  private static ServicesDecommissionCheck servicesDecommissionCheck;
  @Inject
  private static ServicesMapReduceDistributedCacheCheck servicesJobsDistributedCacheCheck;
  @Inject
  private static HostsHeartbeatCheck heartbeatCheck;
  @Inject
  private static ServicesUpCheck servicesUpCheck;
  @Inject
  private static ServicesTezDistributedCacheCheck servicesTezDistributedCacheCheck;

  /**
   * List of the registered upgrade checks.  Make sure that if a check that
   * depends on the result of another check comes earlier in the list.
   * For example, MR2 and Tez distributed cache checks rely on NN-HA check passing.
   */
  @SuppressWarnings("serial")
  private final List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<AbstractCheckDescriptor>() {
    {
      add(hostsMasterMaintenanceCheck);
      add(hostsRepositoryVersionCheck);
      add(servicesMaintenanceModeCheck);
      add(servicesNamenodeHighAvailabilityCheck);
      add(secondaryNamenodeDeletedCheck);
      add(servicesYarnWorkPreservingCheck);
      add(servicesDecommissionCheck);
      add(servicesJobsDistributedCacheCheck);
      add(heartbeatCheck);
      add(servicesUpCheck);
      add(servicesTezDistributedCacheCheck);
    }
  };

  @SuppressWarnings("serial")
  private static Set<String> pkPropertyIds = new HashSet<String>() {
    {
      add(UPGRADE_CHECK_ID_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  public static Set<String> propertyIds = new HashSet<String>() {
    {
      add(UPGRADE_CHECK_ID_PROPERTY_ID);
      add(UPGRADE_CHECK_CHECK_PROPERTY_ID);
      add(UPGRADE_CHECK_STATUS_PROPERTY_ID);
      add(UPGRADE_CHECK_REASON_PROPERTY_ID);
      add(UPGRADE_CHECK_FAILED_ON_PROPERTY_ID);
      add(UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID);
      add(UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID);
      add(UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  public static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Type.PreUpgradeCheck, UPGRADE_CHECK_ID_PROPERTY_ID);
      put(Type.Cluster, UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID);
    }
  };

  @Inject
  private static CheckHelper checkHelper;

  /**
   * Constructor.
   *
   * @param managementController management controller
   */
  public PreUpgradeCheckResourceProvider(AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<Resource> resources = new HashSet<Resource>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    for (Map<String, Object> propertyMap: propertyMaps) {
      final String clusterName = propertyMap.get(UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID).toString();
      final PrereqCheckRequest upgradeCheckRequest = new PrereqCheckRequest(clusterName);
      if (propertyMap.containsKey(UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID)) {
        upgradeCheckRequest.setRepositoryVersion(propertyMap.get(UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID).toString());
      }
      for (PrerequisiteCheck prerequisiteCheck : checkHelper.performChecks(upgradeCheckRequest, updateChecksRegistry)) {
        final Resource resource = new ResourceImpl(Resource.Type.PreUpgradeCheck);
        setResourceProperty(resource, UPGRADE_CHECK_ID_PROPERTY_ID, prerequisiteCheck.getId(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CHECK_PROPERTY_ID, prerequisiteCheck.getDescription(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_STATUS_PROPERTY_ID, prerequisiteCheck.getStatus(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_REASON_PROPERTY_ID, prerequisiteCheck.getFailReason(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_FAILED_ON_PROPERTY_ID, prerequisiteCheck.getFailedOn(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID, prerequisiteCheck.getType(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID, prerequisiteCheck.getClusterName(), requestedIds);
        if (upgradeCheckRequest.getRepositoryVersion() != null) {
          setResourceProperty(resource, UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID, upgradeCheckRequest.getRepositoryVersion(), requestedIds);
        }
        resources.add(resource);
      }
    }
    return resources;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
