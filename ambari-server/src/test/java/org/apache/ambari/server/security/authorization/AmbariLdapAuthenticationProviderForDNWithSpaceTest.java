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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

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

  private static Injector injector;

  @Inject
  private AmbariLdapAuthenticationProvider authenticationProvider;
  @Inject
  private UserDAO userDAO;
  @Inject
  private Users users;

  @Inject
  Configuration configuration;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new ControllerModule(getTestProperties()), new AuditLoggerModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    configuration.setClientSecurityType(ClientSecurityType.LDAP);
    configuration.setProperty(Configuration.LDAP_PRIMARY_URL, "localhost:" + getLdapServer().getPort());
  }

  @After
  public void tearDown() throws Exception {
    injector.getInstance(PersistService.class).stop();
  }

  @Test(expected = InvalidUsernamePasswordCombinationException.class)
  public void testBadCredential() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken("notFound", "wrong");
    authenticationProvider.authenticate(authentication);
  }

  @Test
  public void testAuthenticate() throws Exception {
    assertNull("User alread exists in DB", userDAO.findLdapUserByName("the allowedUser"));
    users.createUser("the allowedUser", "password", UserType.LDAP, true, false);
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
    //make ambari detect active configuration
    properties.setProperty(Configuration.LDAP_BASE_DN.getKey(), "dc=ambari,dc=the apache,dc=org");
    properties.setProperty(Configuration.LDAP_GROUP_BASE.getKey(), "ou=the groups,dc=ambari,dc=the apache,dc=org");
    return properties;
  }
}
