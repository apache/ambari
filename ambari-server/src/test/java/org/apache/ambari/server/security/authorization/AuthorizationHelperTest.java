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
import java.util.Iterator;

import org.apache.ambari.server.orm.entities.RoleEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthorizationHelperTest {

  @Test
  public void testConvertRolesToAuthorities() throws Exception {
    Collection<RoleEntity> roles = new ArrayList<RoleEntity>();
    RoleEntity role = new RoleEntity();
    role.setRoleName("admin");
    roles.add(role);
    role = new RoleEntity();
    role.setRoleName("user");
    roles.add(role);

    Collection<GrantedAuthority> authorities = new AuthorizationHelper().convertRolesToAuthorities(roles);

    assertEquals("Wrong number of authorities", 2, authorities.size());
    Iterator<GrantedAuthority> iterator = authorities.iterator();
    assertEquals("Wrong authority name", "ADMIN", iterator.next().getAuthority());

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
