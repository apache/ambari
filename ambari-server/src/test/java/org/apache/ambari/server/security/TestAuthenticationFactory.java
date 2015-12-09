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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class TestAuthenticationFactory {
  public static Authentication createAdministrator() {
    return createAdministrator("admin");
  }

  public static Authentication createAdministrator(String name) {
    return new TestAuthorization(name, Collections.singleton(createAdministratorGrantedAuthority()));
  }

  public static Authentication createClusterAdministrator() {
    return createClusterAdministrator("clusterAdmin");
  }

  public static Authentication createClusterAdministrator(String name) {
    return new TestAuthorization(name, Collections.singleton(createClusterAdministratorGrantedAuthority()));
  }

  public static Authentication createServiceAdministrator() {
    return createServiceAdministrator("serviceAdmin");
  }

  public static Authentication createServiceAdministrator(String name) {
    return new TestAuthorization(name, Collections.singleton(createServiceAdministratorGrantedAuthority()));
  }

  public static Authentication createServiceOperator() {
    return createServiceOperator("serviceOp");
  }

  public static Authentication createServiceOperator(String name) {
    return new TestAuthorization(name, Collections.singleton(createServiceOperatorGrantedAuthority()));
  }

  public static Authentication createClusterUser() {
    return createClusterUser("clusterUser");
  }

  public static Authentication createClusterUser(String name) {
    return new TestAuthorization(name, Collections.singleton(createClusterUserGrantedAuthority()));
  }

  private static GrantedAuthority createAdministratorGrantedAuthority() {
    return new AmbariGrantedAuthority(createAdministratorPrivilegeEntity());
  }

  private static GrantedAuthority createClusterAdministratorGrantedAuthority() {
    return new AmbariGrantedAuthority(createClusterAdministratorPrivilegeEntity());
  }

  private static GrantedAuthority createServiceAdministratorGrantedAuthority() {
    return new AmbariGrantedAuthority(createServiceAdministratorPrivilegeEntity());
  }

  private static GrantedAuthority createServiceOperatorGrantedAuthority() {
    return new AmbariGrantedAuthority(createServiceOperatorPrivilegeEntity());
  }

  private static GrantedAuthority createClusterUserGrantedAuthority() {
    return new AmbariGrantedAuthority(createClusterUserPrivilegeEntity());
  }

  private static PrivilegeEntity createAdministratorPrivilegeEntity() {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createAmbariResourceEntity());
    privilegeEntity.setPermission(createAdministratorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createClusterAdministratorPrivilegeEntity() {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity());
    privilegeEntity.setPermission(createClusterAdministratorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createServiceAdministratorPrivilegeEntity() {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity());
    privilegeEntity.setPermission(createServiceAdministratorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createServiceOperatorPrivilegeEntity() {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity());
    privilegeEntity.setPermission(createServiceOperatorPermission());
    return privilegeEntity;
  }

  private static PrivilegeEntity createClusterUserPrivilegeEntity() {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(createClusterResourceEntity());
    privilegeEntity.setPermission(createClusterUserPermission());
    return privilegeEntity;
  }

  private static PermissionEntity createAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.AMBARI));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.allOf(RoleAuthorization.class)));
    return permissionEntity;
  }

  private static PermissionEntity createClusterAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.of(
        RoleAuthorization.CLUSTER_MANAGE_CREDENTIALS,
        RoleAuthorization.CLUSTER_MODIFY_CONFIGS,
        RoleAuthorization.CLUSTER_TOGGLE_ALERTS,
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
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO)));
    return permissionEntity;
  }

  private static PermissionEntity createServiceAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(createAuthorizations(EnumSet.of(
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
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
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO)));
    return permissionEntity;
  }

  private static PermissionEntity createServiceOperatorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
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
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO
    )));
    return permissionEntity;
  }

  private static PermissionEntity createClusterUserPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
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
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO
    )));
    return permissionEntity;
  }

  private static ResourceEntity createAmbariResourceEntity() {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(null);
    resourceEntity.setResourceType(createResourceTypeEntity(ResourceType.AMBARI));
    return resourceEntity;
  }

  private static ResourceEntity createClusterResourceEntity() {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(2L);
    resourceEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    return resourceEntity;
  }

  private static ResourceTypeEntity createResourceTypeEntity(ResourceType resourceType) {
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(resourceType.getId());
    resourceTypeEntity.setName(resourceType.name());
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
