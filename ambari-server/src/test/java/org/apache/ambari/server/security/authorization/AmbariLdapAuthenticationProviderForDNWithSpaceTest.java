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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.Assert.*;

@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true,
    name = "Test",
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
    transports = {@CreateTransport(protocol = "LDAP", port = 33389)})
@ApplyLdifFiles("users_for_dn_with_space.ldif")
public class AmbariLdapAuthenticationProviderForDNWithSpaceTest extends AmbariLdapAuthenticationProviderBaseTest {

  private static Injector injector;

  @Inject
  private AmbariLdapAuthenticationProvider authenticationProvider;
  @Inject
  private UserDAO userDAO;
  @Inject
  Configuration configuration;

  @Before
  public void setUp() {
    injector = Guice.createInjector(new AuthorizationTestModuleForLdapDNWithSpace());
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
  public void testAuthenticate() throws Exception {
    assertNull("User alread exists in DB", userDAO.findLdapUserByName("the allowedUser"));
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
}
