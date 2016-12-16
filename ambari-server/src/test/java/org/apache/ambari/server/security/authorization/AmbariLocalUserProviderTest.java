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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class AmbariLocalUserProviderTest {
  private static Injector injector;

  @Inject
  PasswordEncoder passwordEncoder;

  private static final String TEST_USER_NAME = "userName";
  private static final String TEST_USER_PASS = "userPass";
  private static final String TEST_USER_INCORRECT_PASS = "userIncorrectPass";

  @BeforeClass
  public static void prepareData() {
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(OrmTestHelper.class).createTestUsers();
  }

  @Before
  public void setUp() throws Exception {
    injector.injectMembers(this);
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testSuccessfulAuth() {
    Users users = createMock(Users.class);
    UserDAO userDAO = createMock(UserDAO.class);
    Authentication authentication = createMock(Authentication.class);

    UserEntity userEntity = combineUserEntity();

    expect(authentication.getName()).andReturn(TEST_USER_NAME);
    expect(userDAO.findLocalUserByName(TEST_USER_NAME)).andReturn(userEntity);
    expect(authentication.getCredentials()).andReturn(TEST_USER_PASS).anyTimes();
    expect(users.getUserAuthorities(userEntity.getUserName(), userEntity.getUserType())).andReturn(null);

    replay(users, userDAO, authentication);

    AmbariLocalUserProvider ambariLocalUserProvider = new AmbariLocalUserProvider(userDAO, users, passwordEncoder);
    Authentication resultedAuth = ambariLocalUserProvider.authenticate(authentication);

    verify(users, userDAO, authentication);

    assertNotNull(resultedAuth);
    assertEquals(true, resultedAuth.isAuthenticated());
    assertTrue(resultedAuth instanceof AmbariUserAuthentication);
    assertEquals(1, ((User) resultedAuth.getPrincipal()).getUserId());
  }

  @Test(expected = InvalidUsernamePasswordCombinationException.class)
  public void testAuthWithIncorrectName() {
    Users users = createMock(Users.class);
    UserDAO userDAO = createMock(UserDAO.class);
    Authentication authentication = createMock(Authentication.class);

    expect(authentication.getName()).andReturn(TEST_USER_NAME);
    expect(userDAO.findLocalUserByName(TEST_USER_NAME)).andReturn(null);

    replay(users, userDAO, authentication);

    AmbariLocalUserProvider ambariLocalUserProvider = new AmbariLocalUserProvider(userDAO, users, passwordEncoder);
    ambariLocalUserProvider.authenticate(authentication);
  }

  @Test(expected = InvalidUsernamePasswordCombinationException.class)
  public void testAuthWithoutPass() {
    Users users = createMock(Users.class);
    UserDAO userDAO = createMock(UserDAO.class);
    Authentication authentication = createMock(Authentication.class);

    UserEntity userEntity = combineUserEntity();

    expect(authentication.getName()).andReturn(TEST_USER_NAME);
    expect(userDAO.findLocalUserByName(TEST_USER_NAME)).andReturn(userEntity);
    expect(authentication.getCredentials()).andReturn(null);

    replay(users, userDAO, authentication);

    AmbariLocalUserProvider ambariLocalUserProvider = new AmbariLocalUserProvider(userDAO, users, passwordEncoder);
    ambariLocalUserProvider.authenticate(authentication);
  }

  @Test(expected = InvalidUsernamePasswordCombinationException.class)
  public void testAuthWithIncorrectPass() {
    Users users = createMock(Users.class);
    UserDAO userDAO = createMock(UserDAO.class);
    Authentication authentication = createMock(Authentication.class);

    UserEntity userEntity = combineUserEntity();

    expect(authentication.getName()).andReturn(TEST_USER_NAME);
    expect(userDAO.findLocalUserByName(TEST_USER_NAME)).andReturn(userEntity);
    expect(authentication.getCredentials()).andReturn(TEST_USER_INCORRECT_PASS).anyTimes();

    replay(users, userDAO, authentication);

    AmbariLocalUserProvider ambariLocalUserProvider = new AmbariLocalUserProvider(userDAO, users, passwordEncoder);
    ambariLocalUserProvider.authenticate(authentication);
  }



  private UserEntity combineUserEntity() {
    PrincipalEntity principalEntity = new PrincipalEntity();
    UserEntity userEntity = new UserEntity();
    userEntity.setUserId(1);
    userEntity.setUserName(TEST_USER_NAME);
    userEntity.setUserPassword(passwordEncoder.encode(TEST_USER_PASS));
    userEntity.setUserType(UserType.LOCAL);
    userEntity.setPrincipal(principalEntity);

    return userEntity;
  }
}
