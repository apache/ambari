/**
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
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.google.inject.Inject;

@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true,
  name = "Test",
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
  transports = {@CreateTransport(protocol = "LDAP", port = 33389)})
@ApplyLdifFiles("users_with_duplicate_uid.ldif")
public class AmbariLdapAuthenticationProviderForDuplicateUserTest extends AmbariLdapAuthenticationProviderBaseTest {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private AmbariLdapAuthoritiesPopulator authoritiesPopulator;

  private AmbariLdapAuthenticationProvider authenticationProvider;

  @Before
  public void setUp() {
    Properties properties = new Properties();
    properties.setProperty(Configuration.CLIENT_SECURITY_KEY, "ldap");
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY, "in-memory");
    properties.setProperty(Configuration.METADATA_DIR_PATH,"src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE,"src/test/resources/version");
    properties.setProperty(Configuration.OS_VERSION_KEY,"centos5");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR_KEY, "src/test/resources/");
    properties.setProperty(Configuration.LDAP_BASE_DN_KEY, "dc=apache,dc=org");

    Configuration configuration = new Configuration(properties);

    authenticationProvider = new AmbariLdapAuthenticationProvider(configuration, authoritiesPopulator);
  }

  @Test(expected = DuplicateLdapUserFoundAuthenticationException.class)
  public void testAuthenticateDuplicateUser() throws Exception {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken("user_dup", "password");

    // When
    authenticationProvider.authenticate(authentication);

    // Then
    // DuplicateLdapUserFoundAuthenticationException should be thrown

  }
}
