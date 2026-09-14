/*
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
package org.apache.ambari.server.api.stomp;

import java.util.EnumSet;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.springframework.security.core.Authentication;

public class ApiStompAuthorizationService {
  private static final Set<RoleAuthorization> ALERT_AUTHORIZATIONS = EnumSet.of(
      RoleAuthorization.CLUSTER_VIEW_ALERTS,
      RoleAuthorization.CLUSTER_TOGGLE_ALERTS,
      RoleAuthorization.CLUSTER_MANAGE_ALERTS,
      RoleAuthorization.SERVICE_VIEW_ALERTS,
      RoleAuthorization.SERVICE_TOGGLE_ALERTS,
      RoleAuthorization.SERVICE_MANAGE_ALERTS);

  private static final Set<RoleAuthorization> CONFIG_AUTHORIZATIONS = EnumSet.of(
      RoleAuthorization.CLUSTER_VIEW_CONFIGS,
      RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS,
      RoleAuthorization.CLUSTER_MODIFY_CONFIGS,
      RoleAuthorization.SERVICE_VIEW_CONFIGS,
      RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS,
      RoleAuthorization.SERVICE_MODIFY_CONFIGS,
      RoleAuthorization.SERVICE_COMPARE_CONFIGS);

  private static final Set<RoleAuthorization> REQUEST_AUTHORIZATIONS = EnumSet.of(
      RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
      RoleAuthorization.HOST_VIEW_STATUS_INFO,
      RoleAuthorization.SERVICE_VIEW_STATUS_INFO);

  private final Clusters clusters;
  private final HostRoleCommandDAO hostRoleCommandDAO;
  private final RequestDAO requestDAO;

  public ApiStompAuthorizationService(Clusters clusters, HostRoleCommandDAO hostRoleCommandDAO,
                                      RequestDAO requestDAO) {
    this.clusters = clusters;
    this.hostRoleCommandDAO = hostRoleCommandDAO;
    this.requestDAO = requestDAO;
  }

  public boolean isAuthorized(Authentication authentication, Long clusterId, EventAccess access) {
    Cluster cluster = findCluster(clusterId);
    return cluster != null && AuthorizationHelper.isAuthorized(
        authentication, ResourceType.CLUSTER, cluster.getResourceId(), authorizationsFor(access));
  }

  public boolean isAuthorized(Authentication authentication, String clusterName, EventAccess access) {
    Cluster cluster = findCluster(clusterName);
    return cluster != null && AuthorizationHelper.isAuthorized(
        authentication, ResourceType.CLUSTER, cluster.getResourceId(), authorizationsFor(access));
  }

  public boolean isAmbariAuthorized(Authentication authentication) {
    return AuthorizationHelper.isAuthorized(authentication, ResourceType.AMBARI, null,
        RoleAuthorization.AMBARI_VIEW_STATUS_INFO);
  }

  public boolean canViewRequest(Authentication authentication, Long requestId, String assertedClusterName) {
    if (requestId == null) {
      return false;
    }
    RequestEntity request = requestDAO.findByPK(requestId);
    if (request == null) {
      return false;
    }

    Long clusterId = request.getClusterId();
    if (clusterId == null || clusterId == -1L) {
      return assertedClusterName == null && isAmbariAuthorized(authentication);
    }

    Cluster cluster = findCluster(clusterId);
    return cluster != null
        && (assertedClusterName == null || assertedClusterName.equals(cluster.getClusterName()))
        && AuthorizationHelper.isAuthorized(authentication, ResourceType.CLUSTER,
            cluster.getResourceId(), REQUEST_AUTHORIZATIONS);
  }

  public boolean canViewTask(Authentication authentication, Long taskId, Long assertedRequestId) {
    if (taskId == null) {
      return false;
    }
    HostRoleCommandEntity task = hostRoleCommandDAO.findByPK(taskId);
    if (task == null || task.getRequestId() == null
        || (assertedRequestId != null && !assertedRequestId.equals(task.getRequestId()))) {
      return false;
    }
    return canViewRequest(authentication, task.getRequestId(), null);
  }

  public boolean canViewUpgrade(Authentication authentication, Long clusterId, Long requestId) {
    if (!isAuthorized(authentication, clusterId, EventAccess.UPGRADE)) {
      return false;
    }
    if (requestId == null) {
      return true;
    }
    RequestEntity request = requestDAO.findByPK(requestId);
    return request != null && clusterId.equals(request.getClusterId());
  }

  private Set<RoleAuthorization> authorizationsFor(EventAccess access) {
    switch (access) {
      case ALERT:
        return ALERT_AUTHORIZATIONS;
      case CONFIG:
        return CONFIG_AUTHORIZATIONS;
      case SERVICE:
        return RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE;
      case CLUSTER:
      case UPGRADE:
      default:
        return RoleAuthorization.AUTHORIZATIONS_VIEW_CLUSTER;
    }
  }

  private Cluster findCluster(Long clusterId) {
    if (clusterId == null || clusterId < 0) {
      return null;
    }
    try {
      return clusters.getCluster(clusterId);
    } catch (AmbariException e) {
      return null;
    }
  }

  private Cluster findCluster(String clusterName) {
    if (clusterName == null || clusterName.isEmpty()) {
      return null;
    }
    try {
      return clusters.getCluster(clusterName);
    } catch (AmbariException e) {
      return null;
    }
  }

  public enum EventAccess {
    ALERT,
    CLUSTER,
    CONFIG,
    SERVICE,
    UPGRADE
  }
}
