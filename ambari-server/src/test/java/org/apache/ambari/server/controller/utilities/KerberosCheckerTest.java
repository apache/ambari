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

package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LoginContext.class, KerberosChecker.class})
public class KerberosCheckerTest {

  @Test
  public void testCheckPassed() throws Exception {
    Configuration config =  createMock(Configuration.class);
    LoginContext lc =  createMock(LoginContext.class);

    expect(config.isKerberosJaasConfigurationCheckEnabled()).andReturn(true).once();

    expectNew(LoginContext.class, new Class<?>[] { String.class, CallbackHandler.class },
      isA(String.class), isA(CallbackHandler.class) )
      .andReturn(lc);
    lc.login();
    expectLastCall().once();
    lc.logout();
    expectLastCall().once();

    replay(config, LoginContext.class, lc);

    KerberosChecker.config = config;
    KerberosChecker.checkJaasConfiguration();

    verifyAll();
  }

  @Test(expected = AmbariException.class)
  public void testCheckFailed() throws Exception {
    Configuration config =  createMock(Configuration.class);
    LoginContext lc =  createMock(LoginContext.class);

    expect(config.isKerberosJaasConfigurationCheckEnabled()).andReturn(true).once();

    expectNew(LoginContext.class, new Class<?>[] { String.class, CallbackHandler.class },
      isA(String.class), isA(CallbackHandler.class) )
      .andReturn(lc);
    lc.login();
    expectLastCall().andThrow(new LoginException()).once();

    replay(config, LoginContext.class, lc);

    KerberosChecker.config = config;
    KerberosChecker.checkJaasConfiguration();

    verifyAll();
  }

}
