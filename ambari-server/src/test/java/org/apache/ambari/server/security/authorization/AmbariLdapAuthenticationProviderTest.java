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

import com.google.inject.persist.PersistService;
import junit.framework.Assert;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.security.ClientSecurityType;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.server.ApacheDSContainer;
import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

public class AmbariLdapAuthenticationProviderTest extends AmbariLdapAuthenticationProviderBaseTest {

  private static ApacheDSContainer apacheDSContainer;
  private static Injector injector;

  @Inject
  private AmbariLdapAuthenticationProvider authenticationProvider;
  @Inject
  private UserDAO userDAO;
  @Inject
  Configuration configuration;

  @BeforeClass
  public static void beforeClass() throws Exception{
    createCleanApacheDSContainerWorkDir();
    apacheDSContainer = new ApacheDSContainer("dc=ambari,dc=apache,dc=org", "classpath:/users.ldif");
    apacheDSContainer.setPort(33389);
    apacheDSContainer.afterPropertiesSet();
  }

  @Before
  public void setUp() {
    injector = Guice.createInjector(new AuthorizationTestModule());
    injector.injectMembers(this);
    injector.getInstance(GuiceJpaInitializer.class);
    configuration.setClientSecurityType(ClientSecurityType.LDAP);
  }

  @After
  public void tearDown() throws Exception {
    injector.getInstance(PersistService.class).stop();
  }

  @Test(expected = BadCredentialsException.class)
  public void testBadCredential() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken("notFound", "wrong");
    authenticationProvider.authenticate(authentication);
  }

  @Test
  public void testGoodManagerCredentials() throws Exception {
    AmbariLdapAuthoritiesPopulator authoritiesPopulator = createMock(AmbariLdapAuthoritiesPopulator.class);
    AmbariLdapAuthenticationProvider provider = createMockBuilder(AmbariLdapAuthenticationProvider.class)
            .addMockedMethod("loadLdapAuthenticationProvider")
            .addMockedMethod("isLdapEnabled")
            .withConstructor(configuration, authoritiesPopulator).createMock();
    // Create the last thrown exception
    org.springframework.security.core.AuthenticationException exception =
            createNiceMock(org.springframework.security.core.AuthenticationException.class);
    expect(exception.getCause()).andReturn(exception).atLeastOnce();

    expect(provider.isLdapEnabled()).andReturn(true);
    expect(provider.loadLdapAuthenticationProvider()).andThrow(exception);
    // Logging call
    Logger log = createNiceMock(Logger.class);
    provider.LOG = log;
    log.warn(find("LDAP manager credentials"), (Throwable) anyObject());
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        fail("Should not print warning when LDAP manager credentials are not wrong");
        return null;
      }
    }).anyTimes();
    replayAll();
    Authentication authentication = new UsernamePasswordAuthenticationToken("notFound", "wrong");
    try {
      provider.authenticate(authentication);
      fail("Should throw exception");
    } catch(org.springframework.security.core.AuthenticationException e) {
      // expected
    }
    verifyAll();
  }

  @Test
  public void testBadManagerCredentials() throws Exception {
    AmbariLdapAuthoritiesPopulator authoritiesPopulator = createMock(AmbariLdapAuthoritiesPopulator.class);
    AmbariLdapAuthenticationProvider provider = createMockBuilder(AmbariLdapAuthenticationProvider.class)
            .addMockedMethod("loadLdapAuthenticationProvider")
            .addMockedMethod("isLdapEnabled")
            .withConstructor(configuration, authoritiesPopulator).createMock();
    // Create the cause
    org.springframework.ldap.AuthenticationException cause =
            createNiceMock(org.springframework.ldap.AuthenticationException.class);
    // Create the last thrown exception
    org.springframework.security.core.AuthenticationException exception =
            createNiceMock(org.springframework.security.core.AuthenticationException.class);
    expect(exception.getCause()).andReturn(cause).atLeastOnce();

    expect(provider.isLdapEnabled()).andReturn(true);
    expect(provider.loadLdapAuthenticationProvider()).andThrow(exception);
    // Logging call
    Logger log = createNiceMock(Logger.class);
    provider.LOG = log;
    log.warn(find("LDAP manager credentials"), (Throwable) anyObject());
    expectLastCall().atLeastOnce();
    replayAll();
    Authentication authentication = new UsernamePasswordAuthenticationToken("notFound", "wrong");
    try {
      provider.authenticate(authentication);
      fail("Should throw exception");
    } catch(org.springframework.security.core.AuthenticationException e) {
      // expected
    }
    verifyAll();
  }

  @Test
  public void testAuthenticate() throws Exception {
    assertNull("User alread exists in DB", userDAO.findLdapUserByName("allowedUser"));
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser", "password");
    Authentication result = authenticationProvider.authenticate(authentication);
    assertTrue(result.isAuthenticated());
    result = authenticationProvider.authenticate(authentication);
    assertTrue(result.isAuthenticated());
  }

  @Test
  public void testDisabled() throws Exception {
    configuration.setClientSecurityType(ClientSecurityType.LOCAL);
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser", "password");
    Authentication auth = authenticationProvider.authenticate(authentication);
    Assert.assertTrue(auth == null);
  }

  @AfterClass
  public static void afterClass() {
    apacheDSContainer.stop();
  }
}
