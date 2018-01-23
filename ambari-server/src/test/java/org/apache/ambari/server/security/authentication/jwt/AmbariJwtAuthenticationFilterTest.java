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

package org.apache.ambari.server.security.authentication.jwt;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationEventHandler;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationException;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationFilter;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class AmbariJwtAuthenticationFilterTest extends EasyMockSupport {
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

  @Before
  public void setup() {
    SecurityContextHolder.clearContext();
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
  public void testGetJWTFromCookie() throws Exception {
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    Cookie cookie = createNiceMock(Cookie.class);

    expect(cookie.getName()).andReturn("non-default");
    expect(cookie.getValue()).andReturn("stubtokenstring");

    expect(request.getCookies()).andReturn(new Cookie[]{cookie});

    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    String jwtFromCookie = filter.getJWTFromCookie(request);

    verifyAll();

    assertEquals("stubtokenstring", jwtFromCookie);
  }

  @Test
  public void testValidateSignature() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    assertTrue(filter.validateSignature(getSignedToken()));
    assertFalse(filter.validateSignature(getInvalidToken()));

    verifyAll();
  }

  @Test
  public void testValidateAudiences() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);

    assertTrue(filter.validateAudiences(getSignedToken()));
    assertFalse(filter.validateAudiences(getInvalidToken()));

    verifyAll();
  }

  @Test
  public void testValidateNullAudiences() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties(null)).anyTimes();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    assertTrue(filter.validateAudiences(getSignedToken()));
    assertTrue(filter.validateAudiences(getInvalidToken()));

    verifyAll();
  }

  @Test
  public void testValidateTokenWithoutAudiences() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    assertFalse(filter.validateAudiences(getSignedToken(null)));

    verifyAll();
  }

  @Test
  public void testValidateExpiration() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    assertTrue(filter.validateExpiration(getSignedToken()));
    assertFalse(filter.validateExpiration(getInvalidToken()));

    verifyAll();
  }

  @Test
  public void testValidateNoExpiration() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);

    assertTrue(filter.validateExpiration(getSignedToken(null, "test-audience")));
    assertFalse(filter.validateExpiration(getInvalidToken()));

    verifyAll();
  }

  @Test
  public void testShouldApplyTrue() throws JOSEException {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    SignedJWT token = getInvalidToken();

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("non-default").atLeastOnce();
    expect(cookie.getValue()).andReturn(token.serialize()).atLeastOnce();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getCookies()).andReturn(new Cookie[]{cookie});

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    assertTrue(filter.shouldApply(request));

    verifyAll();
  }

  @Test
  public void testShouldApplyTrueBadToken() throws JOSEException {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("non-default").atLeastOnce();
    expect(cookie.getValue()).andReturn("bad token").atLeastOnce();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getCookies()).andReturn(new Cookie[]{cookie});

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    assertTrue(filter.shouldApply(request));

    verifyAll();
  }

  @Test
  public void testShouldApplyFalseMissingCookie() throws JOSEException {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("some-other-cookie").atLeastOnce();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    expect(request.getCookies()).andReturn(new Cookie[]{cookie});

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    assertFalse(filter.shouldApply(request));

    verifyAll();
  }

  @Test
  public void testShouldApplyFalseNotEnabled() throws JOSEException {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(null).anyTimes();

    HttpServletRequest request = createMock(HttpServletRequest.class);

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);

    replayAll();

    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(null, configuration, null, eventHandler);
    assertFalse(filter.shouldApply(request));

    verify(request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ensureNonNullEventHandler() {
    new AmbariJwtAuthenticationFilter(createNiceMock(AmbariEntryPoint.class), createNiceMock(Configuration.class), createNiceMock(AuthenticationProvider.class), null);
  }

  @Test
  public void testDoFilterSuccessful() throws Exception {
    Capture<? extends AmbariAuthenticationFilter> captureFilter = newCapture(CaptureType.ALL);

    SignedJWT token = getSignedToken();

    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();
    expect(configuration.getMaxAuthenticationFailures()).andReturn(10).anyTimes();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    FilterChain filterChain = createMock(FilterChain.class);

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("non-default").once();
    expect(cookie.getValue()).andReturn(token.serialize()).once();

    expect(request.getCookies()).andReturn(new Cookie[]{cookie}).once();

    UserAuthenticationEntity userAuthenticationEntity = createMock(UserAuthenticationEntity.class);
    expect(userAuthenticationEntity.getAuthenticationType()).andReturn(UserAuthenticationType.JWT).anyTimes();
    expect(userAuthenticationEntity.getAuthenticationKey()).andReturn("").anyTimes();

    PrincipalEntity principal = createMock(PrincipalEntity.class);
    expect(principal.getPrivileges()).andReturn(Collections.emptySet()).atLeastOnce();

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getAuthenticationEntities()).andReturn(Collections.singletonList(userAuthenticationEntity)).once();
    expect(userEntity.getActive()).andReturn(true).atLeastOnce();
    expect(userEntity.getUserId()).andReturn(1).atLeastOnce();
    expect(userEntity.getUserName()).andReturn("username").atLeastOnce();
    expect(userEntity.getCreateTime()).andReturn(new Date()).atLeastOnce();
    expect(userEntity.getMemberEntities()).andReturn(Collections.emptySet()).atLeastOnce();
    expect(userEntity.getAuthenticationEntities()).andReturn(Collections.singletonList(userAuthenticationEntity)).atLeastOnce();
    expect(userEntity.getPrincipal()).andReturn(principal).atLeastOnce();

    Users users = createMock(Users.class);
    expect(users.getUserEntity("test-user")).andReturn(userEntity).once();
    expect(users.getUserAuthorities(userEntity)).andReturn(Collections.emptyList()).once();
    users.validateLogin(userEntity, "test-user");
    expectLastCall().once();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);
    eventHandler.beforeAttemptAuthentication(capture(captureFilter), eq(request), eq(response));
    expectLastCall().once();
    eventHandler.onSuccessfulAuthentication(capture(captureFilter), eq(request), eq(response), anyObject(Authentication.class));
    expectLastCall().once();

    filterChain.doFilter(request, response);
    expectLastCall().once();

    AuthenticationEntryPoint entryPoint = createNiceMock(AmbariEntryPoint.class);

    replayAll();

    AmbariJwtAuthenticationProvider provider = new AmbariJwtAuthenticationProvider(users, configuration);
    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(entryPoint, configuration, provider, eventHandler);
    filter.doFilter(request, response, filterChain);

    verifyAll();

    List<? extends AmbariAuthenticationFilter> capturedFilters = captureFilter.getValues();
    for (AmbariAuthenticationFilter capturedFiltered : capturedFilters) {
      assertSame(filter, capturedFiltered);
    }
  }


  @Test
  public void testDoFilterUnsuccessful() throws Exception {
    Capture<? extends AmbariAuthenticationFilter> captureFilter = newCapture(CaptureType.ALL);

    SignedJWT token = getSignedToken();

    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(createTestProperties()).anyTimes();

    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);

    FilterChain filterChain = createMock(FilterChain.class);

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("non-default").once();
    expect(cookie.getValue()).andReturn(token.serialize()).once();

    expect(request.getCookies()).andReturn(new Cookie[]{cookie}).once();

    Users users = createMock(Users.class);
    expect(users.getUserEntity("test-user")).andReturn(null).once();

    AmbariAuthenticationEventHandler eventHandler = createNiceMock(AmbariAuthenticationEventHandler.class);
    eventHandler.beforeAttemptAuthentication(capture(captureFilter), eq(request), eq(response));
    expectLastCall().once();
    eventHandler.onUnsuccessfulAuthentication(capture(captureFilter), eq(request), eq(response), anyObject(AmbariAuthenticationException.class));
    expectLastCall().once();

    AuthenticationEntryPoint entryPoint = createNiceMock(AmbariEntryPoint.class);
    entryPoint.commence(eq(request), eq(response), anyObject(AmbariAuthenticationException.class));
    expectLastCall().once();

    replayAll();

    AmbariJwtAuthenticationProvider provider = new AmbariJwtAuthenticationProvider(users, configuration);
    AmbariJwtAuthenticationFilter filter = new AmbariJwtAuthenticationFilter(entryPoint, configuration, provider, eventHandler);
    filter.doFilter(request, response, filterChain);

    verifyAll();

    List<? extends AmbariAuthenticationFilter> capturedFilters = captureFilter.getValues();
    for (AmbariAuthenticationFilter capturedFiltered : capturedFilters) {
      assertSame(filter, capturedFiltered);
    }
  }

}