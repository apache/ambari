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

package org.apache.ambari.server.security.authorization;

import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;

import static org.easymock.EasyMock.*;

public class AmbariAuthorizationFilterTest {

  @Test
  public void testDoFilter_postPersist_hasOperatePermission() throws Exception {
    FilterChain chain = createNiceMock(FilterChain.class);
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    HttpServletResponse response = createNiceMock(HttpServletResponse.class);
    AmbariAuthorizationFilter filter = createMockBuilder(AmbariAuthorizationFilter.class)
        .addMockedMethod("getSecurityContext").withConstructor().createMock();
    SecurityContext securityContext = createNiceMock(SecurityContext.class);
    Authentication authentication = createNiceMock(Authentication.class);
    AmbariGrantedAuthority authority = createNiceMock(AmbariGrantedAuthority.class);
    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    PermissionEntity permission = createNiceMock(PermissionEntity.class);
    FilterConfig filterConfig = createNiceMock(FilterConfig.class);


    expect(filterConfig.getInitParameter("realm")).andReturn("AuthFilter");
    expect(authentication.isAuthenticated()).andReturn(true);
    expect(request.getRequestURI()).andReturn("/api/v1/persist/some_val");
    expect(authority.getPrivilegeEntity()).andReturn(privilegeEntity);
    expect(privilegeEntity.getPermission()).andReturn(permission);
    EasyMock.<Collection<? extends GrantedAuthority>>expect(authentication.getAuthorities())
        .andReturn(Collections.singletonList(authority));
    expect(filter.getSecurityContext()).andReturn(securityContext);
    expect(securityContext.getAuthentication()).andReturn(authentication);

    expect(permission.getId()).andReturn(PermissionEntity.CLUSTER_OPERATE_PERMISSION);

    // expect continue filtering
    chain.doFilter(request, response);

    replay(request, response, chain, filter, securityContext, authentication, authority,
        privilegeEntity, permission, filterConfig);

    filter.init(filterConfig);
    filter.doFilter(request, response, chain);

    verify(request, response, chain, filter, securityContext, authentication, authority,
        privilegeEntity, permission, filterConfig);
  }

  @Test
  public void testDoFilter_postPersist_hasNoOperatePermission() throws Exception {
    FilterChain chain = createNiceMock(FilterChain.class);
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    HttpServletResponse response = createNiceMock(HttpServletResponse.class);
    AmbariAuthorizationFilter filter = createMockBuilder(AmbariAuthorizationFilter.class)
        .addMockedMethod("getSecurityContext").withConstructor().createMock();
    SecurityContext securityContext = createNiceMock(SecurityContext.class);
    Authentication authentication = createNiceMock(Authentication.class);
    AmbariGrantedAuthority authority = createNiceMock(AmbariGrantedAuthority.class);
    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    PermissionEntity permission = createNiceMock(PermissionEntity.class);
    FilterConfig filterConfig = createNiceMock(FilterConfig.class);


    expect(filterConfig.getInitParameter("realm")).andReturn("AuthFilter");
    expect(authentication.isAuthenticated()).andReturn(true);
    expect(request.getRequestURI()).andReturn("/api/v1/persist/some_val");
    expect(authority.getPrivilegeEntity()).andReturn(privilegeEntity);
    expect(privilegeEntity.getPermission()).andReturn(permission);
    EasyMock.<Collection<? extends GrantedAuthority>>expect(authentication.getAuthorities())
        .andReturn(Collections.singletonList(authority));
    expect(filter.getSecurityContext()).andReturn(securityContext);
    expect(securityContext.getAuthentication()).andReturn(authentication);


    expect(request.getMethod()).andReturn("POST");
    expect(permission.getId()).andReturn(PermissionEntity.VIEW_USE_PERMISSION);

    // expect permission denial
    response.setHeader("WWW-Authenticate", "Basic realm=\"AuthFilter\"");
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permissions to access this resource.");
    response.flushBuffer();

    replay(request, response, chain, filter, securityContext, authentication, authority,
        privilegeEntity, permission, filterConfig);

    filter.init(filterConfig);
    filter.doFilter(request, response, chain);

    verify(request, response, chain, filter, securityContext, authentication, authority,
        privilegeEntity, permission, filterConfig);
  }
}