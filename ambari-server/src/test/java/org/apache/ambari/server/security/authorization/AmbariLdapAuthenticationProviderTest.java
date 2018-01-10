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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.find;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationKeys;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true,
    name = "AmbariLdapAuthenticationProviderTest",
    partitions = {
        @CreatePartition(name = "Root",
            suffix = "dc=apache,dc=org",
            contextEntry = @ContextEntry(
                entryLdif =
                    "dn: dc=apache,dc=org\n" +
                        "dc: apache\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" +
                        "dn: dc=ambari,dc=apache,dc=org\n" +
                        "dc: ambari\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"))
    })
@CreateLdapServer(allowAnonymousAccess = true,
    transports = {@CreateTransport(protocol = "LDAP")})
@ApplyLdifFiles("users.ldif")
public class AmbariLdapAuthenticationProviderTest extends AmbariLdapAuthenticationProviderBaseTest {

  private static Injector injector;

  @Inject
  private AmbariLdapAuthenticationProvider authenticationProvider;
  @Inject
  private UserDAO userDAO;
  @Inject
  private Users users;
  @Inject
  Configuration configuration;
  @Inject
  AmbariLdapConfiguration ldapConfiguration;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new AuthorizationTestModule(), new AuditLoggerModule(), new LdapModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    configuration.setClientSecurityType(ClientSecurityType.LDAP);
    ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_ENABLED, "false");
    ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_FILTER, "(&(mail={0})(objectClass={userObjectClass}))");
    ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.SERVER_HOST, "localhost");
    ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.SERVER_PORT, String.valueOf(getLdapServer().getPort()));
  }

  @After
  public void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test(expected = InvalidUsernamePasswordCombinationException.class)
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
            .withConstructor(configuration, ldapConfiguration, authoritiesPopulator, userDAO).createMock();
    // Create the last thrown exception
    org.springframework.security.core.AuthenticationException exception =
            createNiceMock(org.springframework.security.core.AuthenticationException.class);
    expect(exception.getCause()).andReturn(exception).atLeastOnce();

    expect(provider.isLdapEnabled()).andReturn(true);
    expect(provider.loadLdapAuthenticationProvider("notFound")).andThrow(exception);
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
            .withConstructor(configuration, ldapConfiguration, authoritiesPopulator, userDAO).createMock();
    // Create the cause
    org.springframework.ldap.AuthenticationException cause =
            createNiceMock(org.springframework.ldap.AuthenticationException.class);
    // Create the last thrown exception
    org.springframework.security.core.AuthenticationException exception =
            createNiceMock(org.springframework.security.core.AuthenticationException.class);
    expect(exception.getCause()).andReturn(cause).atLeastOnce();

    expect(provider.isLdapEnabled()).andReturn(true);
    expect(provider.loadLdapAuthenticationProvider("notFound")).andThrow(exception);
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
    users.createUser("allowedUser", "password", UserType.LDAP, true, false);
    UserEntity ldapUser = userDAO.findLdapUserByName("allowedUser");
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser", "password");

    AmbariAuthentication result = (AmbariAuthentication) authenticationProvider.authenticate(authentication);
    assertTrue(result.isAuthenticated());
    assertEquals(ldapUser.getUserId(), result.getUserId());

    result = (AmbariAuthentication) authenticationProvider.authenticate(authentication);
    assertTrue(result.isAuthenticated());
    assertEquals(ldapUser.getUserId(), result.getUserId());
  }

  @Test
  public void testDisabled() throws Exception {
    configuration.setClientSecurityType(ClientSecurityType.LOCAL);
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser", "password");
    Authentication auth = authenticationProvider.authenticate(authentication);
    Assert.assertTrue(auth == null);
  }

  @Test
  public void testAuthenticateLoginAlias() throws Exception {
    // Given
    assertNull("User already exists in DB", userDAO.findLdapUserByName("allowedUser@ambari.apache.org"));
    users.createUser("allowedUser@ambari.apache.org", "password", UserType.LDAP, true, false);
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser@ambari.apache.org", "password");
    authenticationProvider.ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_ENABLED, "true");

    // When
    Authentication result = authenticationProvider.authenticate(authentication);

    // Then
    assertTrue(result.isAuthenticated());
  }

  @Test(expected = InvalidUsernamePasswordCombinationException.class)
  public void testBadCredentialsForMissingLoginAlias() throws Exception {
    // Given
    assertNull("User already exists in DB", userDAO.findLdapUserByName("allowedUser"));
    Authentication authentication = new UsernamePasswordAuthenticationToken("missingloginalias@ambari.apache.org", "password");
    authenticationProvider.ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_ENABLED, "true");


    // When
    authenticationProvider.authenticate(authentication);

    // Then
    // InvalidUsernamePasswordCombinationException should be thrown due to no user with 'missingloginalias@ambari.apache.org'  is found in ldap
  }


  @Test(expected = InvalidUsernamePasswordCombinationException.class)
  public void testBadCredentialsBadPasswordForLoginAlias() throws Exception {
    // Given
    assertNull("User already exists in DB", userDAO.findLdapUserByName("allowedUser"));
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser@ambari.apache.org", "bad_password");
    authenticationProvider.ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_ENABLED, "true");


    // When
    authenticationProvider.authenticate(authentication);

    // Then
    // InvalidUsernamePasswordCombinationException should be thrown due to wrong password
  }

}
