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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.UnixUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

public class AmbariPamAuthenticationProviderTest {

  private static Injector injector;

  @Inject
  private AmbariPamAuthenticationProvider authenticationProvider;
  @Inject
  PasswordEncoder passwordEncoder;
  @Inject
  Configuration configuration;

  private static final String TEST_USER_NAME = "userName";
  private static final String TEST_USER_PASS = "userPass";
  private static final String TEST_USER_INCORRECT_PASS = "userIncorrectPass";

  @Before
  public void setUp() {
    injector = Guice.createInjector(new AuditLoggerModule(), new AuthorizationTestModule());
    injector.injectMembers(this);
    injector.getInstance(GuiceJpaInitializer.class);
    configuration.setClientSecurityType(ClientSecurityType.PAM);
    configuration.setProperty(Configuration.PAM_CONFIGURATION_FILE, "ambari-pam");
  }

  @After
  public void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test(expected = AuthenticationException.class)
  public void testBadCredential() throws Exception {
    UserEntity userEntity = combineUserEntity();
    User user = new User(userEntity);
    Collection<AmbariGrantedAuthority> userAuthorities = Collections.singletonList(createNiceMock(AmbariGrantedAuthority.class));
    Authentication authentication = new AmbariUserAuthentication("wrong", user, userAuthorities);
    authenticationProvider.authenticate(authentication);
  }

  @Test
  public void testAuthenticate() throws Exception {
    PAM pam = createNiceMock(PAM.class);
    UnixUser unixUser = createNiceMock(UnixUser.class);
    UserEntity userEntity = combineUserEntity();
    User user = new User(userEntity);
    UserDAO userDAO = createNiceMock(UserDAO.class);
    Collection<AmbariGrantedAuthority> userAuthorities = Collections.singletonList(createNiceMock(AmbariGrantedAuthority.class));
    expect(pam.authenticate(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class))).andReturn(unixUser).atLeastOnce();
    expect(unixUser.getGroups()).andReturn(new HashSet<String>(Arrays.asList("group"))).atLeastOnce();
    EasyMock.replay(unixUser);
    EasyMock.replay(pam);
    Authentication authentication = new AmbariUserAuthentication("userPass", user, userAuthorities);
    Authentication result = authenticationProvider.authenticateViaPam(pam,authentication);
    expect(userDAO.findUserByName("userName")).andReturn(null).once();
    Assert.assertNotNull(result);
    Assert.assertEquals(true, result.isAuthenticated());
    Assert.assertTrue(result instanceof AmbariUserAuthentication);
  }

  @Test
  public void testDisabled() throws Exception {
    UserEntity userEntity = combineUserEntity();
    User user = new User(userEntity);
    Collection<AmbariGrantedAuthority> userAuthorities = Collections.singletonList(createNiceMock(AmbariGrantedAuthority.class));
    configuration.setClientSecurityType(ClientSecurityType.LOCAL);
    Authentication authentication = new AmbariUserAuthentication("userPass", user, userAuthorities);
    Authentication auth = authenticationProvider.authenticate(authentication);
    Assert.assertTrue(auth == null);
  }

  private UserEntity combineUserEntity() {
    PrincipalEntity principalEntity = new PrincipalEntity();
    UserEntity userEntity = new UserEntity();
    userEntity.setUserId(1);
    userEntity.setUserName(UserName.fromString(TEST_USER_NAME));
    userEntity.setUserPassword(passwordEncoder.encode(TEST_USER_PASS));
    userEntity.setUserType(UserType.PAM);
    userEntity.setPrincipal(principalEntity);
    return userEntity;
  }

}
