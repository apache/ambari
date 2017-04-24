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

package org.apache.ambari.server.security.authorization;

import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

public class AmbariAuthorizationProviderDisableUserTest {

  private Users users;

  private UserDAO userDAO;
  
  private PasswordEncoder encoder = new StandardPasswordEncoder();

  private AmbariLocalUserProvider alup;

  private AmbariLdapAuthoritiesPopulator ldapPopulator;

  @Before
  public void setUp() {
    userDAO = Mockito.mock(UserDAO.class);
    users = Mockito.mock(Users.class);

    createUser("activeUser", true);
    createUser("disabledUser", false);
    
    MemberDAO memberDao = Mockito.mock(MemberDAO.class);
    PrivilegeDAO privilegeDao = Mockito.mock(PrivilegeDAO.class);
    AuthorizationHelper authorizationHelper = new AuthorizationHelper();

    alup = new AmbariLocalUserProvider(userDAO, users, encoder);
    
    ldapPopulator = new AmbariLdapAuthoritiesPopulator(authorizationHelper, userDAO, memberDao, privilegeDao, users);
    
  }
  
  @Test public void testDisabledUserViaDaoProvider(){
    try {
      alup.authenticate(new UsernamePasswordAuthenticationToken("disabledUser","pwd"));
      Assert.fail("Disabled user passes authentication");
    } catch (InvalidUsernamePasswordCombinationException e){
      //expected
      Assert.assertEquals(InvalidUsernamePasswordCombinationException.MESSAGE, e.getMessage());//UI depends on this
    }
    Authentication auth = alup.authenticate(new UsernamePasswordAuthenticationToken("activeUser","pwd"));
    Assert.assertNotNull(auth);
    Assert.assertTrue(auth.isAuthenticated());
  }

  @Test public void testDisabledUserViaLdapProvider(){
    try {
      ldapPopulator.getGrantedAuthorities(null, "disabledUser");
      Assert.fail("Disabled user passes authentication");
    } catch (InvalidUsernamePasswordCombinationException e) {
      //expected
      Assert.assertEquals(InvalidUsernamePasswordCombinationException.MESSAGE, e.getMessage());//UI depends on this
    }
  }
  
  private void createUser(String login, boolean isActive) {
    PrincipalEntity principalEntity = new PrincipalEntity();
    UserEntity activeUser = new UserEntity();
    activeUser.setUserId(1);
    activeUser.setActive(isActive);
    activeUser.setUserName(UserName.fromString(login));
    activeUser.setUserPassword(encoder.encode("pwd"));
    activeUser.setPrincipal(principalEntity);
    Mockito.when(userDAO.findLocalUserByName(login)).thenReturn(activeUser);
    Mockito.when(userDAO.findLdapUserByName(login)).thenReturn(activeUser);
  }
}