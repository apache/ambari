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

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;

public class LogsearchAuthenticationProviderTest {
  private static final Authentication SUCCESSFUL_AUTHENTICATION = new TestingAuthenticationToken("principal", "credentials");
  private static final Authentication FAILED_AUTHENTICATION = new TestingAuthenticationToken("principal", "credentials");
  static {
    SUCCESSFUL_AUTHENTICATION.setAuthenticated(true);
    FAILED_AUTHENTICATION.setAuthenticated(false);
  }
  
  private LogsearchAuthenticationProvider provider;

  private LogsearchFileAuthenticationProvider mockFileProvider;
  private LogsearchExternalServerAuthenticationProvider mockExternalServerProvider;
  private LogsearchSimpleAuthenticationProvider mockSimpleProvider;
  
  @Before
  public void resetContext() throws Exception {
    provider = new LogsearchAuthenticationProvider();

    mockFileProvider = strictMock(LogsearchFileAuthenticationProvider.class);
    mockExternalServerProvider = strictMock(LogsearchExternalServerAuthenticationProvider.class);
    mockSimpleProvider = strictMock(LogsearchSimpleAuthenticationProvider.class);
    
    Field fileProviderField = LogsearchAuthenticationProvider.class.getDeclaredField("fileAuthenticationProvider");
    fileProviderField.setAccessible(true);
    fileProviderField.set(provider, mockFileProvider);
    
    Field extarnalProviderField = LogsearchAuthenticationProvider.class.getDeclaredField("externalServerAuthenticationProvider");
    extarnalProviderField.setAccessible(true);
    extarnalProviderField.set(provider, mockExternalServerProvider);
    
    Field simpleProviderField = LogsearchAuthenticationProvider.class.getDeclaredField("simpleAuthenticationProvider");
    simpleProviderField.setAccessible(true);
    simpleProviderField.set(provider, mockSimpleProvider);
  }
  
  @Test
  public void testFileAuthenticates() {
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    expect(mockFileProvider.authenticate(authentication)).andReturn(SUCCESSFUL_AUTHENTICATION);
    
    replay(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);

    Authentication authenticationResult = provider.authenticate(authentication);
    assertSame(authenticationResult, SUCCESSFUL_AUTHENTICATION);
    
    verify(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
  }
  
  @Test
  public void testExternalAuthenticates() {
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    expect(mockFileProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);
    expect(mockExternalServerProvider.authenticate(authentication)).andReturn(SUCCESSFUL_AUTHENTICATION);
    
    replay(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
    
    Authentication authenticationResult = provider.authenticate(authentication);
    assertSame(authenticationResult, SUCCESSFUL_AUTHENTICATION);
    
    verify(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
  }
  
  @Test
  public void testSimpleAuthenticates() {
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    expect(mockFileProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);
    expect(mockExternalServerProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);
    expect(mockSimpleProvider.authenticate(authentication)).andReturn(SUCCESSFUL_AUTHENTICATION);
    
    replay(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
    
    Authentication authenticationResult = provider.authenticate(authentication);
    assertSame(authenticationResult, SUCCESSFUL_AUTHENTICATION);
    
    verify(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
  }
  
  @Test
  public void testNoOneAuthenticates() {
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    expect(mockFileProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);
    expect(mockExternalServerProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);
    expect(mockSimpleProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);
    
    replay(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
    
    Authentication authenticationResult = provider.authenticate(authentication);
    assertSame(authenticationResult, FAILED_AUTHENTICATION);
    
    verify(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
  }
  
  @Test
  public void testOneExceptionAndAuthenticates() {
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    expect(mockFileProvider.authenticate(authentication)).andReturn(SUCCESSFUL_AUTHENTICATION);
    
    replay(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
    
    Authentication authenticationResult = provider.authenticate(authentication);
    assertSame(authenticationResult, SUCCESSFUL_AUTHENTICATION);
    
    verify(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
  }
  
  @Test
  public void testOneExceptionNoOneAuthenticates() {
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    expect(mockFileProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);
    expect(mockExternalServerProvider.authenticate(authentication)).andThrow(new AuthenticationException("msg1") {});
    expect(mockSimpleProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);
    
    replay(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
    
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown AuthenticationException", false);
    } catch(AuthenticationException e) {
      assertEquals(e.getMessage(), "msg1");
    }
    
    verify(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
  }
  
  @Test
  public void testTwoExceptionNoOneAuthenticates() {
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    expect(mockFileProvider.authenticate(authentication)).andThrow(new AuthenticationException("msg1") {});
    expect(mockExternalServerProvider.authenticate(authentication)).andThrow(new AuthenticationException("msg2") {});
    expect(mockSimpleProvider.authenticate(authentication)).andReturn(FAILED_AUTHENTICATION);

    replay(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
    
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown AuthenticationException", false);
    } catch(AuthenticationException e) {
      assertEquals(e.getMessage(), "msg1");
    }
    
    verify(mockFileProvider, mockSimpleProvider, mockExternalServerProvider);
  }
}
