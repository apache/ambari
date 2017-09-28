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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import nl.jqno.equalsverifier.EqualsVerifier;

public class AmbariAuthenticationTest extends EasyMockSupport {

  private final Integer DEFAULT_USER_ID = 0;

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private ServletRequestAttributes servletRequestAttributes;

  @Mock(type = MockType.NICE)
  private Authentication testAuthentication;

  @Before
  public void setUp() {
    resetAll();

    RequestContextHolder.setRequestAttributes(servletRequestAttributes);

  }

  @Test
  public void testGetPrincipalNoOverride() throws Exception {
    // Given
    Principal origPrincipal = new Principal() {
      @Override
      public String getName() {
        return "user";
      }
    };

    Authentication authentication = new TestingAuthenticationToken(origPrincipal, "password");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    Object principal = ambariAuthentication.getPrincipal();

    // Then
    assertSame(origPrincipal, principal);
  }


  @Test
  public void testGetPrincipal() throws Exception {
    // Given
    Authentication authentication = new TestingAuthenticationToken("user", "password");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    Object principal = ambariAuthentication.getPrincipal();

    // Then
    assertEquals("user", principal);
  }

  @Test
  public void testGetPrincipalWithLoginAlias() throws Exception {
    // Given
    Authentication authentication = new TestingAuthenticationToken("loginAlias", "password");
    expect(servletRequestAttributes.getAttribute(eq("loginAlias"), eq(RequestAttributes.SCOPE_SESSION)))
      .andReturn("user").atLeastOnce();

    replayAll();

    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    verifyAll();
    Object principal = ambariAuthentication.getPrincipal();

    // Then
    assertEquals("user", principal);
  }

  @Test
  public void testGetUserDetailPrincipal() throws Exception {
    // Given
    UserDetails userDetails = new User("user", "password", Collections.emptyList());
    Authentication authentication = new TestingAuthenticationToken(userDetails, userDetails.getPassword());

    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    Object principal = ambariAuthentication.getPrincipal();

    // Then
    assertEquals(userDetails, principal);
  }

  @Test
  public void testGetUserDetailPrincipalWithLoginAlias() throws Exception {
    // Given
    UserDetails userDetails = new User("loginAlias", "password", Collections.emptyList());
    Authentication authentication = new TestingAuthenticationToken(userDetails, userDetails.getPassword());

    expect(servletRequestAttributes.getAttribute(eq("loginAlias"), eq(RequestAttributes.SCOPE_SESSION)))
      .andReturn("user").atLeastOnce();

    replayAll();

    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    Object principal = ambariAuthentication.getPrincipal();

    // Then
    verify();
    UserDetails expectedUserDetails = new User("user", "password", Collections.emptyList()); // user detail with login alias resolved

    assertEquals(expectedUserDetails, principal);
  }



  @Test
  public void testGetNameNoOverride () throws Exception {
    // Given
    Principal origPrincipal = new Principal() {
      @Override
      public String getName() {
        return "user1";
      }
    };
    Authentication authentication = new TestingAuthenticationToken(origPrincipal, "password");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    String name = ambariAuthentication.getName();

    // Then
    assertEquals("user1", name);
  }

  @Test
  public void testGetName() throws Exception {
    // Given
    Authentication authentication = new TestingAuthenticationToken("user", "password");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    String name = ambariAuthentication.getName();

    // Then
    assertEquals("user", name);
  }

  @Test
  public void testGetNameWithLoginAlias() throws Exception {
    // Given
    Authentication authentication = new TestingAuthenticationToken("loginAlias", "password");
    expect(servletRequestAttributes.getAttribute(eq("loginAlias"), eq(RequestAttributes.SCOPE_SESSION)))
      .andReturn("user").atLeastOnce();

    replayAll();

    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    String name = ambariAuthentication.getName();

    // Then
    verifyAll();
    assertEquals("user", name);
  }

  @Test
  public void testGetNameWithUserDetailsPrincipal() throws Exception {
    // Given
    UserDetails userDetails = new User("user", "password", Collections.emptyList());
    Authentication authentication = new TestingAuthenticationToken(userDetails, userDetails.getPassword());

    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    String name = ambariAuthentication.getName();

    // Then
    assertEquals("user", name);
  }

  @Test
  public void testGetNameWithUserDetailsPrincipalWithLoginAlias() throws Exception {
    // Given
    UserDetails userDetails = new User("loginAlias", "password", Collections.emptyList());
    Authentication authentication = new TestingAuthenticationToken(userDetails, userDetails.getPassword());

    expect(servletRequestAttributes.getAttribute(eq("loginAlias"), eq(RequestAttributes.SCOPE_SESSION)))
      .andReturn("user").atLeastOnce();

    replayAll();

    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    String name = ambariAuthentication.getName();

    // Then
    verifyAll();
    assertEquals("user", name);
  }

  @Test
  public void testGetAuthorities() throws Exception {
    // Given
    Authentication authentication = new TestingAuthenticationToken("user", "password", "test_role");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    Collection<?>  grantedAuthorities =  ambariAuthentication.getAuthorities();

    // Then
    Collection<?> expectedAuthorities = authentication.getAuthorities();

    assertSame(expectedAuthorities, grantedAuthorities);
  }

  @Test
  public void testGetCredentials() throws Exception {
    // Given
    String passord = "password";
    Authentication authentication = new TestingAuthenticationToken("user", passord);
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    Object credentials = ambariAuthentication.getCredentials();

    // Then
    assertSame(passord, credentials);
  }

  @Test
  public void testGetDetails() throws Exception {
    // Given
    TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password");
    authentication.setDetails("test auth details");
    Authentication ambariAuthentication = new AmbariAuthentication(authentication, DEFAULT_USER_ID);

    // When
    Object authDetails = ambariAuthentication.getDetails();

    // Then
    Object expecteAuthDetails = authentication.getDetails();

    assertSame(expecteAuthDetails, authDetails);
  }

  @Test
  public void testIsAuthenticated() throws Exception {
    // Given
    expect(testAuthentication.isAuthenticated()).andReturn(false).once();

    replayAll();

    Authentication ambariAuthentication = new AmbariAuthentication(testAuthentication, DEFAULT_USER_ID);

    // When
    ambariAuthentication.isAuthenticated();

    // Then
    verifyAll();
  }

  @Test
  public void setTestAuthentication() throws Exception {
    // Given
    testAuthentication.setAuthenticated(true);
    expectLastCall().once();

    replayAll();

    Authentication ambariAuthentication = new AmbariAuthentication(testAuthentication, DEFAULT_USER_ID);

    // When
    ambariAuthentication.setAuthenticated(true);

    // Then
    verifyAll();
  }

  @Test
  public void testEquals() throws Exception {
    EqualsVerifier.forClass(AmbariAuthentication.class)
      .verify();
  }


}
