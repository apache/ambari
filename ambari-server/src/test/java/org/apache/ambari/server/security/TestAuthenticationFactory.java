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

package org.apache.ambari.server.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class TestAuthenticationFactory {
  public static Authentication createAdministrator() {
    return createAdministrator("admin");
  }

  public static Authentication createAdministrator(String name) {
    return new TestAuthorization(name, Collections.singleton(createAdministratorGrantedAuthority()));
  }

  public static Authentication createClusterAdministrator() {
    return createClusterAdministrator("clusterAdmin", 4L);
  }

  public static Authentication createClusterOperator() {
    return createClusterOperator("clusterOp", 4L);
  }

  public static Authentication createClusterAdministrator(String name, Long clusterResourceId) {
    return new TestAuthorization(name, Collections.singleton(createClusterAdministratorGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createClusterOperator(String name, Long clusterResourceId) {
    return new TestAuthorization(name, Collections.singleton(createClusterOperatorGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createServiceAdministrator() {
    return createServiceAdministrator("serviceAdmin", 4L);
  }

  public static Authentication createServiceAdministrator(String name, Long clusterResourceId) {
    return new TestAuthorization(name, Collections.singleton(createServiceAdministratorGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createServiceOperator() {
    return createServiceOperator("serviceOp", 4L);
  }

  public static Authentication createServiceOperator(String name, Long clusterResourceId) {
    return new TestAuthorization(name, Collections.singleton(createServiceOperatorGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createClusterUser() {
    return createClusterUser("clusterUser", 4L);
  }

  public static Authentication createClusterUser(String name, Long clusterResourceId) {
    return new TestAuthorization(name, Collections.singleton(createClusterUserGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createViewUser(Long viewResourceId) {
    return createViewUser("viewUser", viewResourceId);
  }

  public static Authentication createViewUser(String name, Long viewResourceId) {
    return new TestAuthorization(name, Collections.singleton(createViewUserGrantedAuthority(viewResourceId)));
  }

  private static GrantedAuthority createAdministratorGrantedAuthority() {
    return new AmbariGrantedAuthority(createAdministratorPrivilegeEntity());
  }

  private static GrantedAuthority createClusterAdministratorGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createClusterAdministratorPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createClusterOperatorGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createClusterOperatorPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createServiceAdministratorGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createServiceAdministratorPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createServiceOperatorGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createServiceOperatorPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createClusterUserGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createClusterUserPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createViewUserGrantedAuthority(Long resourceId) {
    return new AmbariGrantedAuthority(createViewUserPrivilegeEntity(resourceId));
  }

  private static PrivilegeEntity createAdministratorPrivilegeEntity() {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createAmbariResourceEntity());
    privilegeEntity.setPermission(createAdministratorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createClusterAdministratorPrivilegeEntity(Long clusterResourceId) {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity(clusterResourceId));
    privilegeEntity.setPermission(createClusterAdministratorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createClusterOperatorPrivilegeEntity(Long clusterResourceId) {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity(clusterResourceId));
    privilegeEntity.setPermission(createClusterOperatorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createServiceAdministratorPrivilegeEntity(Long clusterResourceId) {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity(clusterResourceId));
    privilegeEntity.setPermission(createServiceAdministratorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createServiceOperatorPrivilegeEntity(Long clusterResourceId) {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity(clusterResourceId));
    privilegeEntity.setPermission(createServiceOperatorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createClusterUserPrivilegeEntity(Long clusterResourceId) {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity(clusterResourceId));
    privilegeEntity.setPermission(createClusterUserPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createViewUserPrivilegeEntity(Long resourceId) {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createViewResourceEntity(resourceId));
    privilegeEntity.setPermission(createViewUserPermission());
    return privilegeEntity;
  }

  private static PermissionEntity createAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.AMBARI));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.allOf(RoleAuthorization.class)));
    return permissionEntity;
  }

  private static PermissionEntity createClusterAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.of(
        RoleAuthorization.CLUSTER_MANAGE_CREDENTIALS,
        RoleAuthorization.CLUSTER_MODIFY_CONFIGS,
        RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.CLUSTER_TOGGLE_ALERTS,
        RoleAuthorization.CLUSTER_MANAGE_ALERTS,
        RoleAuthorization.CLUSTER_TOGGLE_KERBEROS,
        RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.HOST_ADD_DELETE_COMPONENTS,
        RoleAuthorization.HOST_ADD_DELETE_HOSTS,
        RoleAuthorization.HOST_TOGGLE_MAINTENANCE,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_ADD_DELETE_SERVICES,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION,
        RoleAuthorization.SERVICE_ENABLE_HA,
        RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.SERVICE_MODIFY_CONFIGS,
        RoleAuthorization.SERVICE_MOVE,
        RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND,
        RoleAuthorization.SERVICE_RUN_SERVICE_CHECK,
        RoleAuthorization.SERVICE_START_STOP,
        RoleAuthorization.SERVICE_TOGGLE_ALERTS,
        RoleAuthorization.SERVICE_TOGGLE_MAINTENANCE,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_VIEW_OPERATIONAL_LOGS,
        RoleAuthorization.CLUSTER_RUN_CUSTOM_COMMAND,
        RoleAuthorization.SERVICE_MANAGE_AUTO_START,
        RoleAuthorization.CLUSTER_MANAGE_AUTO_START,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA)));
    return permissionEntity;
  }

  private static PermissionEntity createClusterOperatorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(5);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.of(
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.HOST_ADD_DELETE_COMPONENTS,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_MANAGE_ALERTS,
        RoleAuthorization.SERVICE_ENABLE_HA,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.SERVICE_TOGGLE_ALERTS,
        RoleAuthorization.SERVICE_MOVE,
        RoleAuthorization.SERVICE_RUN_SERVICE_CHECK,
        RoleAuthorization.SERVICE_MODIFY_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.HOST_ADD_DELETE_HOSTS,
        RoleAuthorization.SERVICE_START_STOP,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.HOST_TOGGLE_MAINTENANCE,
        RoleAuthorization.SERVICE_TOGGLE_MAINTENANCE,
        RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA,
        RoleAuthorization.SERVICE_VIEW_OPERATIONAL_LOGS,
        RoleAuthorization.SERVICE_MANAGE_AUTO_START,
        RoleAuthorization.CLUSTER_MANAGE_AUTO_START,
        RoleAuthorization.CLUSTER_MANAGE_CREDENTIALS)));
    return permissionEntity;
  }

  private static PermissionEntity createServiceAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(5);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.of(
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION,
        RoleAuthorization.SERVICE_ENABLE_HA,
        RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.SERVICE_MODIFY_CONFIGS,
        RoleAuthorization.SERVICE_MOVE,
        RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND,
        RoleAuthorization.SERVICE_RUN_SERVICE_CHECK,
        RoleAuthorization.SERVICE_START_STOP,
        RoleAuthorization.SERVICE_TOGGLE_ALERTS,
        RoleAuthorization.SERVICE_TOGGLE_MAINTENANCE,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_VIEW_OPERATIONAL_LOGS,
        RoleAuthorization.SERVICE_MANAGE_AUTO_START,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA)));
    return permissionEntity;
  }

  private static PermissionEntity createServiceOperatorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(6);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.of(
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.SERVICE_START_STOP,
        RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION,
        RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND,
        RoleAuthorization.SERVICE_RUN_SERVICE_CHECK,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA
    )));
    return permissionEntity;
  }

  private static PermissionEntity createClusterUserPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(PermissionEntity.CLUSTER_USER_PERMISSION);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.of(
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA
    )));
    return permissionEntity;
  }

  private static PermissionEntity createViewUserPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(PermissionEntity.VIEW_USER_PERMISSION);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.of(
      RoleAuthorization.VIEW_USE
    )));
    return permissionEntity;
  }

  private static ResourceEntity createAmbariResourceEntity() {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(null);
    resourceEntity.setResourceType(createResourceTypeEntity(ResourceType.AMBARI));
    return resourceEntity;
  }

  private static ResourceEntity createClusterResourceEntity(Long clusterResourceId) {
    return createResourceEntity(ResourceType.CLUSTER, clusterResourceId);
  }

  private static ResourceEntity createResourceEntity(ResourceType resourceType, Long resourceId) {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(resourceId);
    resourceEntity.setResourceType(createResourceTypeEntity(resourceType));
    return resourceEntity;
  }

  private static ResourceEntity createViewResourceEntity(Long resourceId) {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(resourceId);
    if(resourceId != null) {
      resourceEntity.setResourceType(createResourceTypeEntity(ResourceType.VIEW.name(), resourceId.intValue()));
    }
    return resourceEntity;
  }

  private static ResourceTypeEntity createResourceTypeEntity(ResourceType resourceType) {
    return createResourceTypeEntity(resourceType.name(), resourceType.getId());
  }

  private static ResourceTypeEntity createResourceTypeEntity(String resourceName, Integer resourceId) {
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(resourceId.intValue());
    resourceTypeEntity.setName(resourceName);
    return resourceTypeEntity;
  }

  private static RoleAuthorizationEntity createRoleAuthorizationEntity(RoleAuthorization authorization) {
    RoleAuthorizationEntity roleAuthorizationEntity = new RoleAuthorizationEntity();
    roleAuthorizationEntity.setAuthorizationId(authorization.getId());
    roleAuthorizationEntity.setAuthorizationName(authorization.name());
    return roleAuthorizationEntity;
  }

  private static Collection<RoleAuthorizationEntity> createAuthorizations(Set<RoleAuthorization> roleAuthorizations) {
    Collection<RoleAuthorizationEntity> roleAuthorizationEntities = new ArrayList<RoleAuthorizationEntity>();
    for (RoleAuthorization roleAuthorization : roleAuthorizations) {
      roleAuthorizationEntities.add(createRoleAuthorizationEntity(roleAuthorization));
    }
    return roleAuthorizationEntities;
  }

  private static class TestAuthorization implements Authentication {
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;

    private TestAuthorization(String name, Collection<? extends GrantedAuthority> authorities) {
      this.name = name;
      this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return authorities;
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getDetails() {
      return null;
    }

    @Override
    public Object getPrincipal() {
      return null;
    }

    @Override
    public boolean isAuthenticated() {
      return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
      return name;
    }
  }
}
