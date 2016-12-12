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

import static org.easymock.EasyMock.expect;

import javax.servlet.http.HttpServletRequest;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.AuthenticationEntryPoint;

public class AmbariKerberosAuthenticationFilterTest extends EasyMockSupport {
  @Test
  public void shouldApplyTrue() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
    expect(httpServletRequest.getHeader("Authorization")).andReturn("Negotiate .....").once();

    AmbariKerberosAuthenticationProperties properties = createMock(AmbariKerberosAuthenticationProperties.class);
    expect(properties.isKerberosAuthenticationEnabled()).andReturn(true).once();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getKerberosAuthenticationProperties()).andReturn(properties).once();

    AuthenticationManager authenticationManager = createMock(AuthenticationManager.class);
    AuthenticationEntryPoint entryPoint = createMock(AuthenticationEntryPoint.class);
    AuditLogger auditLogger = createMock(AuditLogger.class);
    PermissionHelper permissionHelper = createMock(PermissionHelper.class);

    replayAll();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        auditLogger,
        permissionHelper
    );

    Assert.assertTrue(filter.shouldApply(httpServletRequest));

    verifyAll();
  }

  @Test
  public void shouldApplyFalseMissingHeader() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);
    expect(httpServletRequest.getHeader("Authorization")).andReturn(null).once();

    AmbariKerberosAuthenticationProperties properties = createMock(AmbariKerberosAuthenticationProperties.class);
    expect(properties.isKerberosAuthenticationEnabled()).andReturn(true).once();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getKerberosAuthenticationProperties()).andReturn(properties).once();

    AuthenticationManager authenticationManager = createMock(AuthenticationManager.class);
    AuthenticationEntryPoint entryPoint = createMock(AuthenticationEntryPoint.class);
    AuditLogger auditLogger = createMock(AuditLogger.class);
    PermissionHelper permissionHelper = createMock(PermissionHelper.class);

    replayAll();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        auditLogger,
        permissionHelper
    );

    Assert.assertFalse(filter.shouldApply(httpServletRequest));

    verifyAll();
  }

  @Test
  public void shouldApplyNotFalseEnabled() throws Exception {
    HttpServletRequest httpServletRequest = createMock(HttpServletRequest.class);

    AmbariKerberosAuthenticationProperties properties = createMock(AmbariKerberosAuthenticationProperties.class);
    expect(properties.isKerberosAuthenticationEnabled()).andReturn(false).once();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getKerberosAuthenticationProperties()).andReturn(properties).once();

    AuthenticationManager authenticationManager = createMock(AuthenticationManager.class);
    AuthenticationEntryPoint entryPoint = createMock(AuthenticationEntryPoint.class);
    AuditLogger auditLogger = createMock(AuditLogger.class);
    PermissionHelper permissionHelper = createMock(PermissionHelper.class);

    replayAll();

    AmbariKerberosAuthenticationFilter filter = new AmbariKerberosAuthenticationFilter(
        authenticationManager,
        entryPoint,
        configuration,
        auditLogger,
        permissionHelper
    );

    Assert.assertFalse(filter.shouldApply(httpServletRequest));

    verifyAll();
  }

  @Test
  public void doFilter() throws Exception {
    // Skip this test since the real work is being done by SpnegoAuthenticationProcessingFilter, which
    // is a class in the Spring libraries.
  }

}