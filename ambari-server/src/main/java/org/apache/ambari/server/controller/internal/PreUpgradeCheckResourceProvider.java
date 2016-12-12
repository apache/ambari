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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.checks.UpgradeCheckRegistry;
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
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.CheckHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.UpgradeHelper;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Resource provider for pre-upgrade checks.
 */
@StaticallyInject
public class PreUpgradeCheckResourceProvider extends ReadOnlyResourceProvider {
  private static Logger LOG = LoggerFactory.getLogger(PreUpgradeCheckResourceProvider.class);

  //----- Property ID constants ---------------------------------------------

  public static final String UPGRADE_CHECK_ID_PROPERTY_ID                 = PropertyHelper.getPropertyId("UpgradeChecks", "id");
  public static final String UPGRADE_CHECK_CHECK_PROPERTY_ID              = PropertyHelper.getPropertyId("UpgradeChecks", "check");
  public static final String UPGRADE_CHECK_STATUS_PROPERTY_ID             = PropertyHelper.getPropertyId("UpgradeChecks", "status");
  public static final String UPGRADE_CHECK_REASON_PROPERTY_ID             = PropertyHelper.getPropertyId("UpgradeChecks", "reason");
  public static final String UPGRADE_CHECK_FAILED_ON_PROPERTY_ID          = PropertyHelper.getPropertyId("UpgradeChecks", "failed_on");
  public static final String UPGRADE_CHECK_FAILED_DETAIL_PROPERTY_ID      = PropertyHelper.getPropertyId("UpgradeChecks", "failed_detail");
  public static final String UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID         = PropertyHelper.getPropertyId("UpgradeChecks", "check_type");
  public static final String UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID       = PropertyHelper.getPropertyId("UpgradeChecks", "cluster_name");
  public static final String UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID       = PropertyHelper.getPropertyId("UpgradeChecks", "upgrade_type");
  /**
   * Optional parameter to specify the preferred Upgrade Pack to use.
   */
  public static final String UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID       = PropertyHelper.getPropertyId("UpgradeChecks", "upgrade_pack");
  public static final String UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("UpgradeChecks", "repository_version");

  @Inject
  private static Provider<Clusters> clustersProvider;

  @Inject
  private static RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private static UpgradeCheckRegistry upgradeCheckRegistry;

  @Inject
  private static Provider<UpgradeHelper> upgradeHelper;

  @Inject
  private static CheckHelper checkHelper;

  private static Set<String> pkPropertyIds = Collections.singleton(UPGRADE_CHECK_ID_PROPERTY_ID);

  public static Set<String> propertyIds = Sets.newHashSet(
      UPGRADE_CHECK_ID_PROPERTY_ID,
      UPGRADE_CHECK_CHECK_PROPERTY_ID,
      UPGRADE_CHECK_STATUS_PROPERTY_ID,
      UPGRADE_CHECK_REASON_PROPERTY_ID,
      UPGRADE_CHECK_FAILED_ON_PROPERTY_ID,
      UPGRADE_CHECK_FAILED_DETAIL_PROPERTY_ID,
      UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID,
      UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID,
      UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID,
      UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID,
      UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID);


  @SuppressWarnings("serial")
  public static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Type.PreUpgradeCheck, UPGRADE_CHECK_ID_PROPERTY_ID);
      put(Type.Cluster, UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID);
    }
  };

  /**
   * Constructor.
   *
   * @param managementController management controller
   */
  public PreUpgradeCheckResourceProvider(AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException,
    NoSuchResourceException, NoSuchParentResourceException {

    final Set<Resource> resources = new HashSet<Resource>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    for (Map<String, Object> propertyMap: propertyMaps) {
      final String clusterName = propertyMap.get(UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID).toString();

      UpgradeType upgradeType = UpgradeType.ROLLING;
      if (propertyMap.containsKey(UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID)) {
        try {
          upgradeType = UpgradeType.valueOf(propertyMap.get(UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID).toString());
        } catch(Exception e){
          throw new SystemException(String.format("Property %s has an incorrect value of %s.", UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID, propertyMap.get(UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID)));
        }
      }

      final Cluster cluster;

      try {
        cluster = clustersProvider.get().getCluster(clusterName);
      } catch (AmbariException ambariException) {
        throw new NoSuchResourceException(ambariException.getMessage());
      }

      String stackName = cluster.getCurrentStackVersion().getStackName();
      String sourceStackVersion = cluster.getCurrentStackVersion().getStackVersion();

      final PrereqCheckRequest upgradeCheckRequest = new PrereqCheckRequest(clusterName, upgradeType);
      upgradeCheckRequest.setSourceStackId(cluster.getCurrentStackVersion());

      if (propertyMap.containsKey(UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID)) {
        String repositoryVersionId = propertyMap.get(UPGRADE_CHECK_REPOSITORY_VERSION_PROPERTY_ID).toString();
        RepositoryVersionEntity repositoryVersionEntity = repositoryVersionDAO.findByStackNameAndVersion(stackName, repositoryVersionId);
        // set some required properties on the check request
        upgradeCheckRequest.setRepositoryVersion(repositoryVersionId);
        upgradeCheckRequest.setTargetStackId(repositoryVersionEntity.getStackId());
      }

      //ambariMetaInfo.getStack(stackName, cluster.getCurrentStackVersion().getStackVersion()).getUpgradePacks()
      // TODO AMBARI-12698, filter the upgrade checks to run based on the stack and upgrade type, or the upgrade pack.
      UpgradePack upgradePack = null;
      String preferredUpgradePackName = propertyMap.containsKey(UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID) ?
          (String) propertyMap.get(UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID) : null;
      try{
        // Hint: PreChecks currently executing only before UPGRADE direction
        upgradePack = upgradeHelper.get().suggestUpgradePack(clusterName, sourceStackVersion,
            upgradeCheckRequest.getRepositoryVersion(), Direction.UPGRADE, upgradeType, preferredUpgradePackName);
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }

      if (upgradePack == null) {
        throw new SystemException(String.format("Upgrade pack not found for the target repository version %s",
          upgradeCheckRequest.getRepositoryVersion()));
      }

      // ToDo: properly handle exceptions, i.e. create fake check with error description
      List<AbstractCheckDescriptor> upgradeChecksToRun = upgradeCheckRegistry.getFilteredUpgradeChecks(upgradePack);
      upgradeCheckRequest.setPrerequisiteCheckConfig(upgradePack.getPrerequisiteCheckConfig());

      try {
        // Register all the custom prechecks from the services
        Map<String, ServiceInfo> services = getManagementController().getAmbariMetaInfo().getServices(stackName, sourceStackVersion);
        List<AbstractCheckDescriptor> serviceLevelUpgradeChecksToRun = upgradeCheckRegistry.getServiceLevelUpgradeChecks(upgradePack, services);
        upgradeChecksToRun.addAll(serviceLevelUpgradeChecksToRun);
      } catch (ParentObjectNotFoundException parentNotFoundException) {
        LOG.error("Invalid stack version: " + stackName + "-" + sourceStackVersion, parentNotFoundException);
      } catch (AmbariException ambariException) {
        LOG.error("Unable to register all the custom prechecks from the services", ambariException);
      } catch (Exception e) {
        LOG.error("Failed to register custom prechecks for the services", e);
      }

      for (PrerequisiteCheck prerequisiteCheck : checkHelper.performChecks(upgradeCheckRequest, upgradeChecksToRun)) {
        final Resource resource = new ResourceImpl(Resource.Type.PreUpgradeCheck);
        setResourceProperty(resource, UPGRADE_CHECK_ID_PROPERTY_ID, prerequisiteCheck.getId(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CHECK_PROPERTY_ID, prerequisiteCheck.getDescription(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_STATUS_PROPERTY_ID, prerequisiteCheck.getStatus(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_REASON_PROPERTY_ID, prerequisiteCheck.getFailReason(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_FAILED_ON_PROPERTY_ID, prerequisiteCheck.getFailedOn(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_FAILED_DETAIL_PROPERTY_ID,prerequisiteCheck.getFailedDetail(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CHECK_TYPE_PROPERTY_ID, prerequisiteCheck.getType(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID, prerequisiteCheck.getClusterName(), requestedIds);
        setResourceProperty(resource, UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID, upgradeType, requestedIds);
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
