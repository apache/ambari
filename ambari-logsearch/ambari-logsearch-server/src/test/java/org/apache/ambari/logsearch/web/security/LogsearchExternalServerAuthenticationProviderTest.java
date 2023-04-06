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

import org.apache.ambari.logsearch.common.ExternalServerClient;
import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.util.Arrays;

public class LogsearchExternalServerAuthenticationProviderTest {

  private LogsearchExternalServerAuthenticationProvider provider;
  private AuthPropsConfig mockAuthPropsConfig;
  private ExternalServerClient mockExternalServerClient;
  
  @Before
  public void init() throws Exception {
    provider = new LogsearchExternalServerAuthenticationProvider();
    mockAuthPropsConfig = strictMock(AuthPropsConfig.class);
    mockExternalServerClient = strictMock(ExternalServerClient.class);
    
    Field authPropsConfigField = LogsearchExternalServerAuthenticationProvider.class.getDeclaredField("authPropsConfig");
    authPropsConfigField.setAccessible(true);
    authPropsConfigField.set(provider, mockAuthPropsConfig);
    
    Field externalServerClientField = LogsearchExternalServerAuthenticationProvider.class.getDeclaredField("externalServerClient");
    externalServerClientField.setAccessible(true);
    externalServerClientField.set(provider, mockExternalServerClient);
  }
  
  @Test
  public void testAuthenticationDisabled() {
    expect(mockAuthPropsConfig.isAuthExternalEnabled()).andReturn(false);
    
    replay(mockAuthPropsConfig);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    assertSame(provider.authenticate(authentication), authentication);
    
    verify(mockAuthPropsConfig);
  }
  
  @Test
  public void testAuthenticationEmptyUser() {
    expect(mockAuthPropsConfig.isAuthExternalEnabled()).andReturn(true);
    
    replay(mockAuthPropsConfig);
    
    Authentication authentication = new TestingAuthenticationToken("", "credentials");
    
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown BadCredentialsException", false);
    } catch(BadCredentialsException e) {
      assertEquals("Username can't be null or empty.", e.getMessage());
    }
    
    verify(mockAuthPropsConfig);
  }
  
  @Test
  public void testAuthenticationNullUser() {
    expect(mockAuthPropsConfig.isAuthExternalEnabled()).andReturn(true);
    
    replay(mockAuthPropsConfig);
    
    Authentication authentication = new TestingAuthenticationToken(null, "credentials");
    
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown BadCredentialsException", false);
    } catch(BadCredentialsException e) {
      assertEquals("Username can't be null or empty.", e.getMessage());
    }
    
    verify(mockAuthPropsConfig);
  }
  
  
  @Test
  public void testAuthenticationEmptyPassword() {
    expect(mockAuthPropsConfig.isAuthExternalEnabled()).andReturn(true);
    
    replay(mockAuthPropsConfig);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "");
    
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown BadCredentialsException", false);
    } catch(BadCredentialsException e) {
      assertEquals("Password can't be null or empty.", e.getMessage());
    }
    
    verify(mockAuthPropsConfig);
  }
  
  @Test
  public void testAuthenticationNullPassword() {
    expect(mockAuthPropsConfig.isAuthExternalEnabled()).andReturn(true);
    
    replay(mockAuthPropsConfig);
    
    Authentication authentication = new TestingAuthenticationToken("principal", null);
    
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown BadCredentialsException", false);
    } catch(BadCredentialsException e) {
      assertEquals("Password can't be null or empty.", e.getMessage());
    }
    
    verify(mockAuthPropsConfig);
  }
  
  @Test
  public void testAuthenticationUnsuccessful() throws Exception {
    expect(mockAuthPropsConfig.isAuthExternalEnabled()).andReturn(true);
    expect(mockAuthPropsConfig.getExternalAuthLoginUrl()).andReturn("http://server.com?userName=$USERNAME");
    expect(mockAuthPropsConfig.getAllowedRoles()).andReturn(Arrays.asList("AMBARI.ADMINISTRATOR"));
    expect(mockExternalServerClient.sendGETRequest("http://server.com?userName=principal", String.class, "principal", "credentials"))
    .andReturn("{\"permission_name\": \"NOT.AMBARI.ADMINISTRATOR\" }");
    
    replay(mockAuthPropsConfig, mockExternalServerClient);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown BadCredentialsException", false);
    } catch (BadCredentialsException e) {
      assertEquals("Bad credentials", e.getMessage());
    }
    
    verify(mockAuthPropsConfig, mockExternalServerClient);
  }
  
  @Test
  public void testAuthenticationSuccessful() throws Exception {
    expect(mockAuthPropsConfig.isAuthExternalEnabled()).andReturn(true);
    expect(mockAuthPropsConfig.getExternalAuthLoginUrl()).andReturn("http://server.com?userName=$USERNAME");
    expect(mockAuthPropsConfig.getAllowedRoles()).andReturn(Arrays.asList("AMBARI.ADMINISTRATOR"));
    expect(mockExternalServerClient.sendGETRequest("http://server.com?userName=principal", String.class, "principal", "credentials"))
      .andReturn("{\"permission_name\": \"AMBARI.ADMINISTRATOR\" }");
    
    replay(mockAuthPropsConfig, mockExternalServerClient);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    Authentication authenticationResult = provider.authenticate(authentication);
    
    assertEquals("principal", authenticationResult.getName());
    assertEquals("credentials", authenticationResult.getCredentials());
    assertEquals(1, authenticationResult.getAuthorities().size());
    assertEquals(new SimpleGrantedAuthority("ROLE_USER"), authenticationResult.getAuthorities().iterator().next());
    
    verify(mockAuthPropsConfig, mockExternalServerClient);
  }
}
