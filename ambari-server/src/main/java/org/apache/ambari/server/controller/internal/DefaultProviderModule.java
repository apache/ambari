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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import com.google.inject.Inject;

/**
 * The default provider module implementation.
 */
public class DefaultProviderModule extends AbstractProviderModule {
  @Inject
  private AmbariManagementController managementController;

  // ----- Constructors ------------------------------------------------------

  /**
   * Create a default provider module.
   */
  public DefaultProviderModule() {
    if (managementController == null) {
      managementController = AmbariServer.getController();
    }
  }


  // ----- utility methods ---------------------------------------------------

  @Override
  protected ResourceProvider createResourceProvider(Resource.Type type) {
    Set<String>               propertyIds    = PropertyHelper.getPropertyIds(type);
    Map<Resource.Type,String> keyPropertyIds = PropertyHelper.getKeyPropertyIds(type);

    switch (type.getInternalType()) {
      case Workflow:
        return new WorkflowResourceProvider(propertyIds, keyPropertyIds);
      case Job:
        return new JobResourceProvider(propertyIds, keyPropertyIds);
      case TaskAttempt:
        return new TaskAttemptResourceProvider(propertyIds, keyPropertyIds);
      case View:
        return new ViewResourceProvider();
      case ViewVersion:
        return new ViewVersionResourceProvider();
      case ViewInstance:
        return new ViewInstanceResourceProvider();
      case ViewURL:
        return new ViewURLResourceProvider();
      case StackServiceComponentDependency:
        return new StackDependencyResourceProvider(propertyIds, keyPropertyIds);
      case Permission:
        return new PermissionResourceProvider();
      case AmbariPrivilege:
        return new AmbariPrivilegeResourceProvider();
      case ViewPrivilege:
        return new ViewPrivilegeResourceProvider();
      case ViewPermission:
        return new ViewPermissionResourceProvider();
      case ClusterPrivilege:
        return new ClusterPrivilegeResourceProvider();
      case LdapSyncEvent:
        return new LdapSyncEventResourceProvider(managementController);
      case UserPrivilege:
        return new UserPrivilegeResourceProvider();
      case GroupPrivilege:
        return new GroupPrivilegeResourceProvider();
      case Alert:
        return new AlertResourceProvider(managementController);
      case AlertDefinition:
        return new AlertDefinitionResourceProvider(managementController);
      case AlertHistory:
        return new AlertHistoryResourceProvider(managementController);
      case AlertTarget:
        return new AlertTargetResourceProvider();
      case AlertGroup:
        return new AlertGroupResourceProvider(managementController);
      case AlertNotice:
        return new AlertNoticeResourceProvider(managementController);
      case UpgradeGroup:
        return new UpgradeGroupResourceProvider(managementController);
      case UpgradeItem:
        return new UpgradeItemResourceProvider(managementController);
      case UpgradeSummary:
        return new UpgradeSummaryResourceProvider(managementController);
      case ClusterStackVersion:
        return new ClusterStackVersionResourceProvider(managementController);
      case PreUpgradeCheck:
        return new PreUpgradeCheckResourceProvider(managementController);
      case HostStackVersion:
        return new HostStackVersionResourceProvider(managementController);
      case Stage:
        return new StageResourceProvider(managementController);
      case OperatingSystem:
        return new OperatingSystemResourceProvider(managementController);
      case Repository:
        return new RepositoryResourceProvider(managementController);
      case Setting:
        return new SettingResourceProvider();
      case Artifact:
        return new ArtifactResourceProvider(managementController);
      case RemoteCluster:
        return new RemoteClusterResourceProvider();

      default:
        return AbstractControllerResourceProvider.getResourceProvider(type, propertyIds,
            keyPropertyIds, managementController);
    }
  }
}
