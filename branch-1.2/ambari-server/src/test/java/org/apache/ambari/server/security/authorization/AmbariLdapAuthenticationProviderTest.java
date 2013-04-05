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

import junit.framework.Assert;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.jpa.JpaPersistModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.security.ClientSecurityType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.server.ApacheDSContainer;

import static org.junit.Assert.*;

public class AmbariLdapAuthenticationProviderTest{

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
    injector = Guice.createInjector(new AuthorizationTestModule(), new JpaPersistModule("ambari-javadb"));
    injector.getInstance(GuiceJpaInitializer.class);

    apacheDSContainer = new ApacheDSContainer("dc=ambari,dc=apache,dc=org", "classpath:/users.ldif");
    apacheDSContainer.setPort(33389);
    apacheDSContainer.afterPropertiesSet();
  }

  @Before
  public void setUp() {
    injector.injectMembers(this);
    configuration.setClientSecurityType(ClientSecurityType.LDAP);
  }

  @Test(expected = BadCredentialsException.class)
  public void testBadCredential() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken("notFound", "wrong");
    authenticationProvider.authenticate(authentication);
  }

  @Test
  public void testAuthenticate() throws Exception {
    assertNull("User alread exists in DB", userDAO.findLdapUserByName("allowedUser"));
    Authentication authentication = new UsernamePasswordAuthenticationToken("allowedUser", "password");
    Authentication result = authenticationProvider.authenticate(authentication);
    assertTrue(result.isAuthenticated());
    assertNotNull("User was not created", userDAO.findLdapUserByName("allowedUser"));
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
