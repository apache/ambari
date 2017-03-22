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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.web.security;

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import static junit.framework.Assert.assertSame;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;

public class LogsearchLdapAuthenticationProviderTest {

  private LogsearchLdapAuthenticationProvider provider;
  private AuthPropsConfig mockAuthPropsConfig;
  
  @Before
  public void init() throws Exception {
    provider = new LogsearchLdapAuthenticationProvider();
    mockAuthPropsConfig = strictMock(AuthPropsConfig.class);
    
    Field f = LogsearchLdapAuthenticationProvider.class.getDeclaredField("authPropsConfig");
    f.setAccessible(true);
    f.set(provider, mockAuthPropsConfig);
  }
  
  @Test
  public void testAuthenticationDisabled() {
    expect(mockAuthPropsConfig.isAuthLdapEnabled()).andReturn(false);
    
    replay(mockAuthPropsConfig);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    assertSame(provider.authenticate(authentication), authentication);
    
    verify(mockAuthPropsConfig);
  }
}