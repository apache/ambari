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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Properties;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true,
  name = "AmbariLdapAuthenticationProviderForDNWithSpaceTest",
  partitions = {
    @CreatePartition(name = "Root",
      suffix = "dc=the apache,dc=org",
      contextEntry = @ContextEntry(
        entryLdif =
          "dn: dc=the apache,dc=org\n" +
            "dc: the apache\n" +
            "objectClass: top\n" +
            "objectClass: domain\n\n" +
            "dn: dc=ambari,dc=the apache,dc=org\n" +
            "dc: ambari\n" +
            "objectClass: top\n" +
            "objectClass: domain\n\n"))
  })
@CreateLdapServer(allowAnonymousAccess = true,
  transports = {@CreateTransport(protocol = "LDAP")})
@ApplyLdifFiles("users_for_dn_with_space.ldif")
public class AmbariLdapAuthenticationProviderForDNWithSpaceTest extends AmbariLdapAuthenticationProviderBaseTest {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  private static Injector injector;

  @Inject
  private UserDAO userDAO;

  @Inject
  private Users users;

  @Inject
  Configuration configuration;

  @Mock(type = MockType.NICE)
  private AmbariLdapAuthoritiesPopulator authoritiesPopulator;

  @Mock(type = MockType.NICE)
  private AmbariLdapConfigurationProvider ldapConfigurationProvider;

  private AmbariLdapAuthenticationProvider authenticationProvider;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new ControllerModule(getTestProperties()), new AuditLoggerModule(), new LdapModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    configuration.setClientSecurityType(ClientSecurityType.LDAP);
    final AmbariLdapConfiguration ldapConfiguration = new AmbariLdapConfiguration();
    ldapConfiguration.setValueFor(AmbariServerConfigurationKey.SERVER_HOST, "localhost");
    ldapConfiguration.setValueFor(AmbariServerConfigurationKey.SERVER_PORT, String.valueOf(getLdapServer().getPort()));
    ldapConfiguration.setValueFor(AmbariServerConfigurationKey.USER_SEARCH_BASE, "dc=ambari,dc=the apache,dc=org");
    ldapConfiguration.setValueFor(AmbariServerConfigurationKey.GROUP_BASE, "ou=the groups,dc=ambari,dc=the apache,dc=org");
    expect(ldapConfigurationProvider.get()).andReturn(ldapConfiguration).anyTimes();
    expect(authoritiesPopulator.getGrantedAuthorities(anyObject(), anyObject())).andReturn(Collections.emptyList()).anyTimes();
    replayAll();

    authenticationProvider = new AmbariLdapAuthenticationProvider(users, configuration, ldapConfigurationProvider, authoritiesPopulator);
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
  public void testAuthenticateMatchingDN() throws Exception {
    testAuthenticate("uid=the allowedUser,ou=the people,dc=ambari,dc=the apache,dc=org");
  }

  @Test
  public void testAuthenticateNullDN() throws Exception {
    testAuthenticate(null);
  }

  @Test(expected = InvalidUsernamePasswordCombinationException.class)
  public void testAuthenticateNonMatchingDN() throws Exception {
    testAuthenticate("This is not a matching DN");
  }

  private void testAuthenticate(String dn) throws AmbariException {
    assertNull("User already exists in DB", userDAO.findUserByName("the allowedUser"));
    UserEntity userEntity = users.createUser("the allowedUser", null, null);
    users.addLdapAuthentication(userEntity, dn);

    Authentication authentication = new UsernamePasswordAuthenticationToken("the allowedUser", "password");
    Authentication result = authenticationProvider.authenticate(authentication);
    assertTrue(result.isAuthenticated());
    result = authenticationProvider.authenticate(authentication);
    assertTrue(result.isAuthenticated());
  }

  @Test
  public void testDisabled() throws Exception {
    configuration.setClientSecurityType(ClientSecurityType.LOCAL);
    Authentication authentication = new UsernamePasswordAuthenticationToken("the allowedUser", "password");
    Authentication auth = authenticationProvider.authenticate(authentication);
    assertTrue(auth == null);
  }


  protected Properties getTestProperties() {
    Properties properties = new Properties();
    properties.setProperty(Configuration.CLIENT_SECURITY.getKey(), "ldap");
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), "src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), "src/test/resources/version");
    properties.setProperty(Configuration.OS_VERSION.getKey(), "centos5");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), "src/test/resources/");
    return properties;
  }
}
