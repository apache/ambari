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

package org.apache.ambari.server.security.authentication.kerberos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationEventHandler;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationException;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationFilter;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

public class AmbariKerberosAuthenticationFilterTest {
  private Configuration configuration;
  private AuthenticationEntryPoint entryPoint;
  private AuthenticationManager authenticationManager;
  private AmbariAuthenticationEventHandler eventHandler;

  @Before
  public void setUp() {
    SecurityContextHolder.getContext().setAuthentication(null);

    entryPoint = mock(AmbariEntryPoint.class);
    configuration = mock(Configuration.class);
    authenticationManager = mock(AuthenticationManager.class);
    eventHandler = mock(AmbariAuthenticationEventHandler.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void ensureNonNullEventHandler() {
    new AmbariKerberosAuthenticationFilter(authenticationManager, entryPoint, configuration, null);
  }

  @Test
  public void shouldApplyTrue() throws Exception {
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getHeader("Authorization")).thenReturn("Negotiate .....");
    doReturn(createProperties(true)).when(configuration).getKerberosAuthenticationProperties();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        eventHandler
    );

    Assert.assertTrue(filter.shouldApply(httpServletRequest));
  }

  @Test
  public void shouldApplyFalseMissingHeader() throws Exception {
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
    doReturn(createProperties(true)).when(configuration).getKerberosAuthenticationProperties();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        eventHandler
    );

    Assert.assertFalse(filter.shouldApply(httpServletRequest));
  }

  @Test
  public void shouldApplyNotFalseEnabled() throws Exception {
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    doReturn(createProperties(true)).when(configuration).getKerberosAuthenticationProperties();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        eventHandler
    );

    Assert.assertFalse(filter.shouldApply(httpServletRequest));
  }

  @Test
  public void testDoFilterSuccessful() throws IOException, ServletException {
    List<AmbariAuthenticationFilter> capturedFilters = new ArrayList<>();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn("Negotiate ");

    when(request.getHeader(Mockito.startsWith("X-Forwarded-"))).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    when(request.getSession(false)).thenReturn(session);
    when(request.getQueryString()).thenReturn(null);
    when(request.getParameter(anyString())).thenReturn(null);
    when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
    when(session.getId()).thenReturn("sessionID");

    doReturn(createProperties(true)).when(configuration).getKerberosAuthenticationProperties();

    doAnswer(invocation -> {
      capturedFilters.add((AmbariAuthenticationFilter) invocation.getArgument(0));
      return null;
    }).when(eventHandler).beforeAttemptAuthentication(any(), eq(request), eq(response));

    when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));

    doAnswer(invocation -> {
      capturedFilters.add((AmbariAuthenticationFilter) invocation.getArgument(0));
      return null;
    }).when(eventHandler).onSuccessfulAuthentication(any(), eq(request), eq(response), any(Authentication.class));

    doNothing().when(filterChain).doFilter(request, response);

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(authenticationManager, entryPoint, configuration, eventHandler);
    filter.doFilter(request, response, filterChain);
    verify(filterChain, times(1)).doFilter(any(), any());

    for (AmbariAuthenticationFilter capturedFiltered : capturedFilters) {
      Assert.assertSame(filter, capturedFiltered);
    }
  }

  @Test
  public void testDoFilterUnsuccessful() throws IOException, ServletException {
    List<AmbariAuthenticationFilter> capturedFilters = new ArrayList<>();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getHeader("Authorization")).thenReturn("Negotiate ");
    when(request.getHeader(Mockito.startsWith("X-Forwarded-"))).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    when(request.getSession(false)).thenReturn(session);
    when(request.getQueryString()).thenReturn(null);
    when(request.getParameter(anyString())).thenReturn(null);
    when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
    when(session.getId()).thenReturn("sessionID");

    doReturn(createProperties(true)).when(configuration).getKerberosAuthenticationProperties();

    doAnswer(invocation -> {
      capturedFilters.add((AmbariAuthenticationFilter) invocation.getArgument(0));
      return null;
    }).when(eventHandler).beforeAttemptAuthentication(any(), eq(request), eq(response));

    when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new InvalidUsernamePasswordCombinationException("user"));

    doAnswer(invocation -> {
      capturedFilters.add((AmbariAuthenticationFilter) invocation.getArgument(0));
      return null;
    }).when(eventHandler).onUnsuccessfulAuthentication(any(), eq(request), eq(response), any(AmbariAuthenticationException.class));

    doNothing().when(entryPoint).commence(eq(request), eq(response), any(AmbariAuthenticationException.class));

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(authenticationManager, entryPoint, configuration, eventHandler);
    filter.doFilter(request, response, filterChain);

    for (AmbariAuthenticationFilter capturedFiltered : capturedFilters) {
      Assert.assertSame(filter, capturedFiltered);
    }
  }

  private AmbariKerberosAuthenticationProperties createProperties(Boolean enabled) {
    AmbariKerberosAuthenticationProperties properties = mock(AmbariKerberosAuthenticationProperties.class);
    when(properties.isKerberosAuthenticationEnabled()).thenReturn(enabled);
    return properties;
  }
}
