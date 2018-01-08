/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import java.util.Properties;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationKeys;
import org.apache.ambari.server.orm.dao.UserDAO;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true,
  name = "AmbariLdapAuthenticationProviderForDuplicateUserTest",
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
@ApplyLdifFiles("users_with_duplicate_uid.ldif")
public class AmbariLdapAuthenticationProviderForDuplicateUserTest extends AmbariLdapAuthenticationProviderBaseTest {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock(type = MockType.NICE)
  private AmbariLdapAuthoritiesPopulator authoritiesPopulator;

  @Mock(type = MockType.NICE)
  private UserDAO userDAO;

  private AmbariLdapAuthenticationProvider authenticationProvider;

  @Before
  public void setUp() {
    Properties properties = new Properties();
    properties.setProperty(Configuration.CLIENT_SECURITY.getKey(), "ldap");
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(),"src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(),"src/test/resources/version");
    properties.setProperty(Configuration.OS_VERSION.getKey(),"centos5");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), "src/test/resources/");
    Configuration configuration = new Configuration(properties);
    
    final AmbariLdapConfiguration ldapConfiguration = new AmbariLdapConfiguration();
    ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.USER_SEARCH_BASE, "dc=apache,dc=org");
    ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.SERVER_HOST, "localhost");
    ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.SERVER_PORT, String.valueOf(getLdapServer().getPort()));

    authenticationProvider = new AmbariLdapAuthenticationProvider(configuration, ldapConfiguration, authoritiesPopulator, userDAO);
  }

  @Test
  public void testAuthenticateDuplicateUserAltUserSearchDisabled() throws Exception {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken("user_dup", "password");
    authenticationProvider.ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_ENABLED, "false");

    expectedException.expect(DuplicateLdapUserFoundAuthenticationException.class);
    expectedException.expectMessage("Login Failed: More than one user with that username found, please work with your Ambari Administrator to adjust your LDAP configuration");

    // When
    authenticationProvider.authenticate(authentication);

    // Then
    // DuplicateLdapUserFoundAuthenticationException should be thrown


  }

  @Test
  public void testAuthenticateDuplicateUserAltUserSearchEnabled() throws Exception {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken("user_dup", "password");
    authenticationProvider.ldapConfiguration.setValueFor(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_ENABLED, "true");

    expectedException.expect(DuplicateLdapUserFoundAuthenticationException.class);
    expectedException.expectMessage("Login Failed: Please append your domain to your username and try again.  Example: user_dup@domain");

    // When
    authenticationProvider.authenticate(authentication);

    // Then
    // DuplicateLdapUserFoundAuthenticationException should be thrown


  }
}
