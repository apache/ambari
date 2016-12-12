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

package org.apache.ambari.server.security.authorization.jwt;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserType;
import org.apache.ambari.server.security.authorization.Users;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import junit.framework.Assert;

public class JwtAuthenticationFilterTest {
  private static RSAPublicKey publicKey;
  private static RSAPrivateKey privateKey;
  private static RSAPrivateKey invalidPrivateKey;


  @BeforeClass
  public static void generateKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(512);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    publicKey = (RSAPublicKey) keyPair.getPublic();
    privateKey = (RSAPrivateKey) keyPair.getPrivate();

    keyPair = keyPairGenerator.generateKeyPair();
    invalidPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
  }

  private JwtAuthenticationProperties createTestProperties() {
    return createTestProperties(Collections.singletonList("test-audience"));
  }

  private JwtAuthenticationProperties createTestProperties(List<String> audiences) {
    JwtAuthenticationProperties properties = new JwtAuthenticationProperties();
    properties.setCookieName("non-default");
    properties.setPublicKey(publicKey);
    properties.setAudiences(audiences);

    return properties;
  }

  private SignedJWT getSignedToken() throws JOSEException {
    return getSignedToken("test-audience");
  }

  private SignedJWT getSignedToken(String audience) throws JOSEException {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());
    calendar.add(Calendar.DATE, 1); //add one day
    return getSignedToken(calendar.getTime(), audience);
  }
  
  private SignedJWT getSignedToken(Date expirationTime, String audience) throws JOSEException {
    RSASSASigner signer = new RSASSASigner(privateKey);

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());
    JWTClaimsSet claimsSet = new JWTClaimsSet();
    claimsSet.setSubject("test-user");
    claimsSet.setIssuer("unit-test");
    claimsSet.setIssueTime(calendar.getTime());

    claimsSet.setExpirationTime(expirationTime);

    claimsSet.setAudience(audience);

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
    signedJWT.sign(signer);

    return signedJWT;
  }

  private SignedJWT getInvalidToken() throws JOSEException {
    RSASSASigner signer = new RSASSASigner(invalidPrivateKey);

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());
    calendar.add(Calendar.DATE, -2);

    JWTClaimsSet claimsSet = new JWTClaimsSet();
    claimsSet.setSubject("test-user");
    claimsSet.setIssuer("unit-test");
    claimsSet.setIssueTime(calendar.getTime());

    calendar.add(Calendar.DATE, 1); //add one day
    claimsSet.setExpirationTime(calendar.getTime());

    claimsSet.setAudience("test-audience-invalid");

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
    signedJWT.sign(signer);

    return signedJWT;
  }


  @Test
  @Ignore
  public void testDoFilter() throws Exception {
    Users users = createNiceMock(Users.class);
    AuthenticationEntryPoint entryPoint = createNiceMock(AuthenticationEntryPoint.class);
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    HttpServletResponse response = createNiceMock(HttpServletResponse.class);
    FilterChain chain = createNiceMock(FilterChain.class);
    AmbariGrantedAuthority authority = createNiceMock(AmbariGrantedAuthority.class);
    User user = createNiceMock(User.class);

    SignedJWT signedJWT = getSignedToken();

    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = createMockBuilder(JwtAuthenticationFilter.class).
      addMockedMethod("getJWTFromCookie").
      withConstructor(properties, entryPoint, users).createNiceMock();

    expect(filter.getJWTFromCookie(anyObject(HttpServletRequest.class))).andReturn(signedJWT.serialize());
    expect(users.getUser(eq("test-user"), eq(UserType.JWT))).andReturn(null).once();
    expect(users.getUser(eq("test-user"), eq(UserType.JWT))).andReturn(user).anyTimes();

    users.createUser(eq("test-user"), anyObject(String.class), eq(UserType.JWT), eq(true), eq(false));
    expectLastCall();

    expect(users.getUserAuthorities(eq("test-user"), eq(UserType.JWT))).andReturn(Collections.singletonList(authority));

    expect(user.getUserName()).andReturn("test-user");
    expect(user.getUserType()).andReturn(UserType.JWT);

    expect(user.getUserId()).andReturn(1);

    replay(users, request, response, chain, filter, entryPoint, user, authority);

    filter.doFilter(request, response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertEquals(1L, AuthorizationHelper.getAuthenticatedId());

    verify(users, request, response, chain, filter, entryPoint, user, authority);

    assertEquals(true, authentication.isAuthenticated());
  }

  @Test
  public void testGetJWTFromCookie() throws Exception {
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    Cookie cookie = createNiceMock(Cookie.class);

    expect(cookie.getName()).andReturn("non-default");
    expect(cookie.getValue()).andReturn("stubtokenstring");

    expect(request.getCookies()).andReturn(new Cookie[]{cookie});

    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    replay(request, cookie);

    String jwtFromCookie = filter.getJWTFromCookie(request);

    verify(request, cookie);

    assertEquals("stubtokenstring", jwtFromCookie);
  }

  @Test
  public void testValidateSignature() throws Exception {
    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    boolean isValid = filter.validateSignature(getSignedToken());

    assertEquals(true, isValid);

    isValid = filter.validateSignature(getInvalidToken());

    assertEquals(false, isValid);

  }

  @Test
  public void testValidateAudiences() throws Exception {
    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    boolean isValid = filter.validateAudiences(getSignedToken());

    assertEquals(true, isValid);

    isValid = filter.validateAudiences(getInvalidToken());

    assertEquals(false, isValid);
  }

  @Test
  public void testValidateNullAudiences() throws Exception {
    JwtAuthenticationProperties properties = createTestProperties(null);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    boolean isValid = filter.validateAudiences(getSignedToken());

    assertEquals(true, isValid);

    isValid = filter.validateAudiences(getInvalidToken());

    assertEquals(true, isValid);
  }

  @Test
  public void testValidateTokenWithoutAudiences() throws Exception {
    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    boolean isValid = filter.validateAudiences(getSignedToken(null));

    assertEquals(false, isValid);
  }

  @Test
  public void testValidateExpiration() throws Exception {
    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    boolean isValid = filter.validateExpiration(getSignedToken());

    assertEquals(true, isValid);

    isValid = filter.validateExpiration(getInvalidToken());

    assertEquals(false, isValid);

  }

  @Test
  public void testValidateNoExpiration() throws Exception {
    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    boolean isValid = filter.validateExpiration(getSignedToken(null, "test-audience"));

    assertEquals(true, isValid);

    isValid = filter.validateExpiration(getInvalidToken());

    assertEquals(false, isValid);

  }

  @Test
  public void testShouldApplyTrue() throws JOSEException {
    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    SignedJWT token = getInvalidToken();

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("non-default").atLeastOnce();
    expect(cookie.getValue()).andReturn(token.serialize()).atLeastOnce();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getCookies()).andReturn(new Cookie[]{cookie});

    replay(request, cookie);

    Assert.assertTrue(filter.shouldApply(request));

    verify(request, cookie);
  }

  @Test
  public void testShouldApplyTrueBadToken() throws JOSEException {
    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("non-default").atLeastOnce();
    expect(cookie.getValue()).andReturn("bad token").atLeastOnce();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getCookies()).andReturn(new Cookie[]{cookie});

    replay(request, cookie);

    Assert.assertTrue(filter.shouldApply(request));

    verify(request, cookie);
  }

  @Test
  public void testShouldApplyFalseMissingCookie() throws JOSEException {
    JwtAuthenticationProperties properties = createTestProperties();
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(properties, null, null);

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("some-other-cookie").atLeastOnce();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getCookies()).andReturn(new Cookie[]{cookie});

    replay(request, cookie);

    Assert.assertFalse(filter.shouldApply(request));

    verify(request, cookie);
  }

  @Test
  public void testShouldApplyFalseNotEnabled() throws JOSEException {
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter((JwtAuthenticationProperties) null, null, null);

    HttpServletRequest request = createMock(HttpServletRequest.class);

    replay(request);

    Assert.assertFalse(filter.shouldApply(request));

    verify(request);
  }
}