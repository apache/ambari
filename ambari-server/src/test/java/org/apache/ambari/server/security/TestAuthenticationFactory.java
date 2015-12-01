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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class TestAuthenticationFactory {
  public static Authentication createAdministrator(String name) {
    return new TestAuthorization(name, Collections.singleton(createAdministratorGrantedAuthority()));
  }

  public static Authentication createClusterAdministrator(String name) {
    return new TestAuthorization(name, Collections.singleton(createClusterAdministratorGrantedAuthority()));
  }

  private static GrantedAuthority createAdministratorGrantedAuthority() {
    return new AmbariGrantedAuthority(createAdministratorPrivilegeEntity());
  }

  private static GrantedAuthority createClusterAdministratorGrantedAuthority() {
    return new AmbariGrantedAuthority(createClusterAdministratorPrivilegeEntity());
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

  private static PermissionEntity createAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.AMBARI));

    Collection<RoleAuthorizationEntity> authorizations = new ArrayList<RoleAuthorizationEntity>();
    for (RoleAuthorization roleAuthorization : RoleAuthorization.values()) {
      authorizations.add(createRoleAuthorizationEntity(roleAuthorization));
    }

    permissionEntity.setAuthorizations(authorizations);

    return permissionEntity;
  }

  private static PermissionEntity createClusterAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setAuthorizations(Arrays.asList(
        createRoleAuthorizationEntity(RoleAuthorization.CLUSTER_VIEW_ALERTS),
        createRoleAuthorizationEntity(RoleAuthorization.CLUSTER_TOGGLE_ALERTS)));

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
