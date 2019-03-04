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
import org.apache.ambari.logsearch.util.CommonUtil;
import org.apache.ambari.logsearch.web.model.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class LogsearchFileAuthenticationProviderTest {

  private LogsearchFileAuthenticationProvider provider;
  private AuthPropsConfig mockAuthPropsConfig;
  private UserDetailsService mockUserDetailsService;
  
  @Before
  public void init() throws Exception {
    provider = new LogsearchFileAuthenticationProvider();
    mockAuthPropsConfig = strictMock(AuthPropsConfig.class);
    mockUserDetailsService = strictMock(UserDetailsService.class);
    
    Field authPropsConfigField = LogsearchFileAuthenticationProvider.class.getDeclaredField("authPropsConfig");
    authPropsConfigField.setAccessible(true);
    authPropsConfigField.set(provider, mockAuthPropsConfig);
    
    Field userDetailsServiceField = LogsearchFileAuthenticationProvider.class.getDeclaredField("userDetailsService");
    userDetailsServiceField.setAccessible(true);
    userDetailsServiceField.set(provider, mockUserDetailsService);
  }
  
  @Test
  public void testAuthenticationDisabled() {
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(false);
    
    replay(mockAuthPropsConfig);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    assertSame(provider.authenticate(authentication), authentication);
    
    verify(mockAuthPropsConfig);
  }
  
  @Test
  public void testAuthenticationEmptyUser() {
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(true);
    
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
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(true);
    
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
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(true);
    
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
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(true);
    
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
  public void testAuthenticationUnknownUser() {
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(true);
    expect(mockUserDetailsService.loadUserByUsername("principal")).andReturn(null);
    
    replay(mockAuthPropsConfig, mockUserDetailsService);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown BadCredentialsException", false);
    } catch (BadCredentialsException e) {
      assertEquals("User not found.", e.getMessage());
    }
    
    verify(mockAuthPropsConfig, mockUserDetailsService);
  }
  
  @Test
  public void testAuthenticationNoPassword() {
    List<GrantedAuthority> grantedAuths = Arrays.<GrantedAuthority>asList(new SimpleGrantedAuthority("ROLE_USER"));
    User user = new User("principal", null, grantedAuths);
    
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(true);
    expect(mockUserDetailsService.loadUserByUsername("principal")).andReturn(user);
    
    replay(mockAuthPropsConfig, mockUserDetailsService);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown BadCredentialsException", false);
    } catch (BadCredentialsException e) {
      assertEquals("Password can't be null or empty.", e.getMessage());
    }
    
    verify(mockAuthPropsConfig, mockUserDetailsService);
  }
  
  @Test
  public void testAuthenticationWrongPassword() {
    List<GrantedAuthority> grantedAuths = Arrays.<GrantedAuthority>asList(new SimpleGrantedAuthority("ROLE_USER"));
    User user = new User("principal", CommonUtil.encryptPassword("principal", "notCredentials"), grantedAuths);
    
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(true);
    expect(mockUserDetailsService.loadUserByUsername("principal")).andReturn(user);
    
    replay(mockAuthPropsConfig, mockUserDetailsService);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    try {
      provider.authenticate(authentication);
      assertTrue("Should have thrown BadCredentialsException", false);
    } catch (BadCredentialsException e) {
      assertEquals("Wrong password.", e.getMessage());
    }
    
    verify(mockAuthPropsConfig, mockUserDetailsService);
  }
  
  @Test
  public void testAuthenticationSuccessful() {
    List<GrantedAuthority> grantedAuths = Arrays.<GrantedAuthority>asList(new SimpleGrantedAuthority("ROLE_USER"));
    User user = new User("principal", CommonUtil.encryptPassword("principal", "credentials"), grantedAuths);
    
    expect(mockAuthPropsConfig.isAuthFileEnabled()).andReturn(true);
    expect(mockUserDetailsService.loadUserByUsername("principal")).andReturn(user);
    
    replay(mockAuthPropsConfig, mockUserDetailsService);
    
    Authentication authentication = new TestingAuthenticationToken("principal", "credentials");
    
    Authentication authenticationResult = provider.authenticate(authentication);
    assertEquals("principal", authenticationResult.getName());
    assertEquals(CommonUtil.encryptPassword("principal", "credentials"), authenticationResult.getCredentials());
    assertEquals(1, authenticationResult.getAuthorities().size());
    assertEquals(new SimpleGrantedAuthority("ROLE_USER"), authenticationResult.getAuthorities().iterator().next());
    
    verify(mockAuthPropsConfig, mockUserDetailsService);
  }
}
