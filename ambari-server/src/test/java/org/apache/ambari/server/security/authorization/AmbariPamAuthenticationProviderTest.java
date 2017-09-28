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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Collections;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authentication.pam.PamAuthenticationFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class AmbariPamAuthenticationProviderTest extends EasyMockSupport {

  private static final String TEST_USER_NAME = "userName";
  private static final String TEST_USER_PASS = "userPass";
  private static final String TEST_USER_INCORRECT_PASS = "userIncorrectPass";

  private Injector injector;

  @Before
  public void setup() {
    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(HookContextFactory.class).toInstance(createNiceMock(HookContextFactory.class));
        bind(HookService.class).toInstance(createNiceMock(HookService.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(UserDAO.class).toInstance(createNiceMock(UserDAO.class));
        bind(PamAuthenticationFactory.class).toInstance(createMock(PamAuthenticationFactory.class));
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
      }
    });

    Configuration configuration = injector.getInstance(Configuration.class);
    configuration.setClientSecurityType(ClientSecurityType.PAM);
    configuration.setProperty(Configuration.PAM_CONFIGURATION_FILE, "ambari-pam");
  }

  @Test(expected = AuthenticationException.class)
  public void testBadCredential() throws Exception {

    PAM pam = createMock(PAM.class);
    expect(pam.authenticate(eq(TEST_USER_NAME), eq(TEST_USER_INCORRECT_PASS)))
        .andThrow(new PAMException())
        .once();
    pam.dispose();
    expectLastCall().once();

    PamAuthenticationFactory pamAuthenticationFactory = injector.getInstance(PamAuthenticationFactory.class);
    expect(pamAuthenticationFactory.createInstance(anyObject(String.class))).andReturn(pam).once();

    replayAll();

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_INCORRECT_PASS);

    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);
    authenticationProvider.authenticate(authentication);

    verifyAll();
  }

  @Test
  public void testAuthenticate() throws Exception {

    UnixUser unixUser = createNiceMock(UnixUser.class);
    expect(unixUser.getGroups()).andReturn(Collections.singleton("group")).atLeastOnce();

    PAM pam = createMock(PAM.class);
    expect(pam.authenticate(eq(TEST_USER_NAME), eq(TEST_USER_PASS)))
        .andReturn(unixUser)
        .once();
    pam.dispose();
    expectLastCall().once();

    PamAuthenticationFactory pamAuthenticationFactory = injector.getInstance(PamAuthenticationFactory.class);
    expect(pamAuthenticationFactory.createInstance(anyObject(String.class))).andReturn(pam).once();

    replayAll();

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_PASS);

    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);

    Authentication result = authenticationProvider.authenticate(authentication);

    verifyAll();

    Assert.assertNotNull(result);
    Assert.assertEquals(true, result.isAuthenticated());
    Assert.assertTrue(result instanceof AmbariUserAuthentication);
  }

  @Test
  public void testDisabled() throws Exception {

    Configuration configuration = injector.getInstance(Configuration.class);
    configuration.setClientSecurityType(ClientSecurityType.LOCAL);

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_PASS);

    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);
    Authentication auth = authenticationProvider.authenticate(authentication);
    Assert.assertTrue(auth == null);
  }
}
