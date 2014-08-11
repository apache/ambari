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
package org.apache.ambari.server.security.authorization;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthorizationHelperTest {

  @Test
  public void testConvertPrivilegesToAuthorities() throws Exception {
    Collection<PrivilegeEntity> privilegeEntities = new ArrayList<PrivilegeEntity>();

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(1);
    resourceTypeEntity.setName("CLUSTER");

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(1L);
    resourceEntity.setResourceType(resourceTypeEntity);

    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();
    principalTypeEntity.setId(1);
    principalTypeEntity.setName("USER");

    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalEntity.setId(1L);

    PermissionEntity permissionEntity1 = new PermissionEntity();
    permissionEntity1.setPermissionName("Permission1");
    permissionEntity1.setResourceType(resourceTypeEntity);
    permissionEntity1.setId(2);
    permissionEntity1.setPermissionName("CLUSTER.READ");

    PermissionEntity permissionEntity2 = new PermissionEntity();
    permissionEntity2.setPermissionName("Permission1");
    permissionEntity2.setResourceType(resourceTypeEntity);
    permissionEntity2.setId(3);
    permissionEntity2.setPermissionName("CLUSTER.OPERATE");

    PrivilegeEntity privilegeEntity1 = new PrivilegeEntity();
    privilegeEntity1.setId(1);
    privilegeEntity1.setPermission(permissionEntity1);
    privilegeEntity1.setPrincipal(principalEntity);
    privilegeEntity1.setResource(resourceEntity);

    PrivilegeEntity privilegeEntity2 = new PrivilegeEntity();
    privilegeEntity2.setId(1);
    privilegeEntity2.setPermission(permissionEntity2);
    privilegeEntity2.setPrincipal(principalEntity);
    privilegeEntity2.setResource(resourceEntity);

    privilegeEntities.add(privilegeEntity1);
    privilegeEntities.add(privilegeEntity2);

    Collection<GrantedAuthority> authorities = new AuthorizationHelper().convertPrivilegesToAuthorities(privilegeEntities);

    assertEquals("Wrong number of authorities", 2, authorities.size());

    Set<String> authorityNames = new HashSet<String>();

    for (GrantedAuthority authority : authorities) {
      authorityNames.add(authority.getAuthority());
    }
    Assert.assertTrue(authorityNames.contains("CLUSTER.READ@1"));
    Assert.assertTrue(authorityNames.contains("CLUSTER.OPERATE@1"));
  }

  @Test
  public void testAuthName() throws Exception {
    String user = AuthorizationHelper.getAuthenticatedName();
    Assert.assertNull(user);
    
    Authentication auth = new UsernamePasswordAuthenticationToken("admin",null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    user = AuthorizationHelper.getAuthenticatedName();
    Assert.assertEquals("admin", user);
    
  }
}
