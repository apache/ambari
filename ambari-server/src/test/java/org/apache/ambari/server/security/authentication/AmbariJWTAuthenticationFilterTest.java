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

package org.apache.ambari.server.security.authentication;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.authorization.jwt.JwtAuthenticationProperties;
import org.easymock.EasyMockSupport;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class AmbariJWTAuthenticationFilterTest extends EasyMockSupport {
  private static RSAPublicKey publicKey;
  private static RSAPrivateKey privateKey;

  @BeforeClass
  public static void generateKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(512);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    publicKey = (RSAPublicKey) keyPair.getPublic();
    privateKey = (RSAPrivateKey) keyPair.getPrivate();
  }

  @Test
  public void testDoFilterSuccess() throws Exception {
    SignedJWT token = getSignedToken("foobar");

    AmbariEntryPoint entryPoint = createMock(AmbariEntryPoint.class);

    JwtAuthenticationProperties properties = createMock(JwtAuthenticationProperties.class);
    expect(properties.getAuthenticationProviderUrl()).andReturn("some url").once();
    expect(properties.getPublicKey()).andReturn(publicKey).once();
    expect(properties.getAudiences()).andReturn(Collections.singletonList("foobar")).once();
    expect(properties.getCookieName()).andReturn("chocolate chip").once();
    expect(properties.getOriginalUrlQueryParam()).andReturn("question").once();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(properties).once();

    User user = createMock(User.class);
    expect(user.getUserName()).andReturn("test-user").once();
    expect(user.getUserType()).andReturn(UserType.JWT).once();

    Users users = createMock(Users.class);
    expect(users.getUser("test-user", UserType.JWT)).andReturn(user).once();
    expect(users.getUserAuthorities("test-user", UserType.JWT)).andReturn(null).once();

    AuditLogger auditLogger = createMock(AuditLogger.class);
    expect(auditLogger.isEnabled()).andReturn(false).times(2);

    PermissionHelper permissionHelper = createMock(PermissionHelper.class);

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("chocolate chip").once();
    expect(cookie.getValue()).andReturn(token.serialize()).once();


    HttpServletRequest servletRequest = createMock(HttpServletRequest.class);
    expect(servletRequest.getCookies()).andReturn(new Cookie[]{cookie}).once();

    HttpServletResponse servletResponse = createMock(HttpServletResponse.class);

    FilterChain filterChain = createMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AmbariJWTAuthenticationFilter filter = new AmbariJWTAuthenticationFilter(entryPoint, configuration, users, auditLogger, permissionHelper);
    filter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilterFailure() throws Exception {
    AmbariEntryPoint entryPoint = createMock(AmbariEntryPoint.class);

    JwtAuthenticationProperties properties = createMock(JwtAuthenticationProperties.class);
    expect(properties.getAuthenticationProviderUrl()).andReturn("some url").once();
    expect(properties.getPublicKey()).andReturn(publicKey).once();
    expect(properties.getAudiences()).andReturn(Collections.singletonList("foobar")).once();
    expect(properties.getCookieName()).andReturn("chocolate chip").once();
    expect(properties.getOriginalUrlQueryParam()).andReturn("question").once();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getJwtProperties()).andReturn(properties).once();

    Users users = createMock(Users.class);

    AuditLogger auditLogger = createMock(AuditLogger.class);
    expect(auditLogger.isEnabled()).andReturn(false).times(2);

    PermissionHelper permissionHelper = createMock(PermissionHelper.class);

    Cookie cookie = createMock(Cookie.class);
    expect(cookie.getName()).andReturn("chocolate chip").once();
    expect(cookie.getValue()).andReturn("invalid token").once();


    HttpServletRequest servletRequest = createMock(HttpServletRequest.class);
    expect(servletRequest.getCookies()).andReturn(new Cookie[]{cookie}).once();

    HttpServletResponse servletResponse = createMock(HttpServletResponse.class);

    FilterChain filterChain = createMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AmbariJWTAuthenticationFilter filter = new AmbariJWTAuthenticationFilter(entryPoint, configuration, users, auditLogger, permissionHelper);
    filter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
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
}