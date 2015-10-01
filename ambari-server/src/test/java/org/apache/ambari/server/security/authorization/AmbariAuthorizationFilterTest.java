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

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity.ViewInstanceVersionDTO;
import org.apache.ambari.server.view.ViewRegistry;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import org.springframework.security.core.context.SecurityContextHolder;

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
    Authentication auth = new UsernamePasswordAuthenticationToken("admin",null);
    SecurityContextHolder.getContext().setAuthentication(auth);


    expect(filterConfig.getInitParameter("realm")).andReturn("AuthFilter");
    expect(authentication.isAuthenticated()).andReturn(true);
    expect(request.getRequestURI()).andReturn("/api/v1/persist/some_val");
    expect(authority.getPrivilegeEntity()).andReturn(privilegeEntity);
    expect(privilegeEntity.getPermission()).andReturn(permission);
    EasyMock.<Collection<? extends GrantedAuthority>>expect(authentication.getAuthorities())
        .andReturn(Collections.singletonList(authority));
    expect(filter.getSecurityContext()).andReturn(securityContext);
    expect(securityContext.getAuthentication()).andReturn(authentication);
    response.setHeader("User", "admin");
    expectLastCall().andAnswer(new IAnswer() {
      public Object answer() {
        String arg1 = (String) getCurrentArguments()[0];
        String arg2 = (String) getCurrentArguments()[1];
        Assert.assertEquals("User", arg1);
        Assert.assertEquals("admin", arg2);
        return null;
      }
    });

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


    expect(request.getMethod()).andReturn("POST").anyTimes();
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

  @Test
  public void testDoFilter_adminAccess() throws Exception {
    final Table<String, String, Boolean> urlTests = HashBasedTable.create();
    urlTests.put("/api/v1/clusters/cluster", "GET",  true);
    urlTests.put("/api/v1/clusters/cluster", "POST",  true);
    urlTests.put("/api/v1/views", "GET", true);
    urlTests.put("/api/v1/views", "POST", true);
    urlTests.put("/api/v1/persist/SomeValue", "GET", true);
    urlTests.put("/api/v1/persist/SomeValue", "POST", true);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "POST", true);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "PUT", true);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "GET", true);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "DELETE", true);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "POST", true);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "PUT", true);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "GET", true);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "DELETE", true);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "GET", true);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "POST", true);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "GET", true);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "POST", true);
    urlTests.put("/api/v1/users/user1", "GET", true);
    urlTests.put("/api/v1/users/user1", "POST", true);
    urlTests.put("/api/v1/users/user2", "GET", true);
    urlTests.put("/api/v1/users/user2", "POST", true);
    urlTests.put("/api/v1/groups", "GET", true);
    urlTests.put("/api/v1/ldap_sync_events", "GET", true);
    urlTests.put("/any/other/URL", "GET", true);
    urlTests.put("/any/other/URL", "POST", true);

    performGeneralDoFilterTest("admin", new int[] {PermissionEntity.AMBARI_ADMIN_PERMISSION}, urlTests, false);
  }

  @Test
  public void testDoFilter_clusterViewerAccess() throws Exception {
    final Table<String, String, Boolean> urlTests = HashBasedTable.create();
    urlTests.put("/api/v1/clusters/cluster", "GET",  true);
    urlTests.put("/api/v1/clusters/cluster", "POST",  true);
    urlTests.put("/api/v1/views", "GET", true);
    urlTests.put("/api/v1/views", "POST", false);
    urlTests.put("/api/v1/persist/SomeValue", "GET", true);
    urlTests.put("/api/v1/persist/SomeValue", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "PUT", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "GET", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "DELETE", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "PUT", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "GET", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "DELETE", false);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "GET", false);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "POST", false);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "GET", false);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "POST", false);
    urlTests.put("/api/v1/users/user1", "GET", true);
    urlTests.put("/api/v1/users/user1", "POST", true);
    urlTests.put("/api/v1/users/user2", "GET", false);
    urlTests.put("/api/v1/users/user2", "POST", false);
    urlTests.put("/api/v1/groups", "GET", false);
    urlTests.put("/api/v1/ldap_sync_events", "GET", false);
    urlTests.put("/any/other/URL", "GET", true);
    urlTests.put("/any/other/URL", "POST", false);

    performGeneralDoFilterTest("user1", new int[] {PermissionEntity.CLUSTER_READ_PERMISSION}, urlTests, false);
  }

  @Test
  public void testDoFilter_clusterOperatorAccess() throws Exception {
    final Table<String, String, Boolean> urlTests = HashBasedTable.create();
    urlTests.put("/api/v1/clusters/cluster", "GET",  true);
    urlTests.put("/api/v1/clusters/cluster", "POST",  true);
    urlTests.put("/api/v1/views", "GET", true);
    urlTests.put("/api/v1/views", "POST", false);
    urlTests.put("/api/v1/persist/SomeValue", "GET", true);
    urlTests.put("/api/v1/persist/SomeValue", "POST", true);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "PUT", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "GET", true);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "DELETE", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "POST", true);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "PUT", true);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "GET", true);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "DELETE", true);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "GET", false);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "POST", false);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "GET", false);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "POST", false);
    urlTests.put("/api/v1/users/user1", "GET", true);
    urlTests.put("/api/v1/users/user1", "POST", true);
    urlTests.put("/api/v1/users/user2", "GET", false);
    urlTests.put("/api/v1/users/user2", "POST", false);
    urlTests.put("/api/v1/groups", "GET", false);
    urlTests.put("/api/v1/ldap_sync_events", "GET", false);
    urlTests.put("/any/other/URL", "GET", true);
    urlTests.put("/any/other/URL", "POST", false);

    performGeneralDoFilterTest("user1", new int[] {PermissionEntity.CLUSTER_OPERATE_PERMISSION}, urlTests, false);
  }

  @Test
  public void testDoFilter_viewUserAccess() throws Exception {
    final Table<String, String, Boolean> urlTests = HashBasedTable.create();
    urlTests.put("/api/v1/clusters/cluster", "GET",  true);
    urlTests.put("/api/v1/clusters/cluster", "POST",  false);
    urlTests.put("/api/v1/views", "GET", true);
    urlTests.put("/api/v1/views", "POST", true);
    urlTests.put("/api/v1/persist/SomeValue", "GET", true);
    urlTests.put("/api/v1/persist/SomeValue", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "PUT", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "GET", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "DELETE", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "PUT", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "GET", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "DELETE", false);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "GET", true);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "POST", true);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "GET", false);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "POST", false);
    urlTests.put("/api/v1/users/user1", "GET", true);
    urlTests.put("/api/v1/users/user1", "POST", true);
    urlTests.put("/api/v1/users/user2", "GET", false);
    urlTests.put("/api/v1/users/user2", "POST", false);
    urlTests.put("/api/v1/groups", "GET", false);
    urlTests.put("/api/v1/ldap_sync_events", "GET", false);
    urlTests.put("/any/other/URL", "GET", true);
    urlTests.put("/any/other/URL", "POST", false);

    performGeneralDoFilterTest("user1", new int[] {PermissionEntity.VIEW_USE_PERMISSION}, urlTests, false);
  }

  @Test
  public void testDoFilter_userNoPermissionsAccess() throws Exception {
    final Table<String, String, Boolean> urlTests = HashBasedTable.create();
    urlTests.put("/api/v1/clusters/cluster", "GET",  true);
    urlTests.put("/api/v1/clusters/cluster", "POST",  false);
    urlTests.put("/api/v1/views", "GET", true);
    urlTests.put("/api/v1/views", "POST", false);
    urlTests.put("/api/v1/persist/SomeValue", "GET", true);
    urlTests.put("/api/v1/persist/SomeValue", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "PUT", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "GET", false);
    urlTests.put("/api/v1/clusters/c1/credentials/ambari.credential", "DELETE", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "POST", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "PUT", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "GET", false);
    urlTests.put("/api/v1/clusters/c1/credentials/cluster.credential", "DELETE", false);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "GET", false);
    urlTests.put("/views/AllowedView/SomeVersion/SomeInstance", "POST", false);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "GET", false);
    urlTests.put("/views/DeniedView/AnotherVersion/AnotherInstance", "POST", false);
    urlTests.put("/api/v1/users/user1", "GET", false);
    urlTests.put("/api/v1/users/user1", "POST", false);
    urlTests.put("/api/v1/users/user2", "GET", true);
    urlTests.put("/api/v1/users/user2", "POST", true);
    urlTests.put("/any/other/URL", "GET", true);
    urlTests.put("/any/other/URL", "POST", false);

    performGeneralDoFilterTest("user2", new int[0], urlTests, false);
  }

  @Test
  public void testDoFilter_viewNotLoggedIn() throws Exception {
    final Table<String, String, Boolean> urlTests = HashBasedTable.create();
    urlTests.put("/views/SomeView/SomeVersion/SomeInstance", "GET", false);
    urlTests.put("/views/SomeView/SomeVersion/SomeInstance?foo=bar", "GET", false);

    performGeneralDoFilterTest(null, new int[0], urlTests, true);
  }

  @Test
  public void testDoFilter_stackAdvisorCalls() throws Exception {
    final Table<String, String, Boolean> urlTests = HashBasedTable.create();
    urlTests.put("/api/v1/stacks/HDP/versions/2.3/validations", "POST", true);
    urlTests.put("/api/v1/stacks/HDP/versions/2.3/recommendations", "POST", true);
    performGeneralDoFilterTest("user1", new int[] { PermissionEntity.CLUSTER_OPERATE_PERMISSION }, urlTests, false);
    performGeneralDoFilterTest("user2", new int[] { PermissionEntity.CLUSTER_READ_PERMISSION }, urlTests, false);
    performGeneralDoFilterTest("admin", new int[] { PermissionEntity.AMBARI_ADMIN_PERMISSION }, urlTests, false);
  }

  /**
   * Creates mocks with given permissions and performs all given url tests.
   *
   * @param username user name
   * @param permissionsGranted array of user permissions
   * @param urlTests map of triples: url - http method - is allowed
   * @param expectRedirect true if the requests should redirect to login
   * @throws Exception
   */
  private void performGeneralDoFilterTest(String username, final int[] permissionsGranted, Table<String, String, Boolean> urlTests, boolean expectRedirect) throws Exception {
    final SecurityContext securityContext = createNiceMock(SecurityContext.class);
    final Authentication authentication = createNiceMock(Authentication.class);
    final FilterConfig filterConfig = createNiceMock(FilterConfig.class);
    final AmbariAuthorizationFilter filter = createMockBuilder(AmbariAuthorizationFilter.class)
        .addMockedMethod("getSecurityContext").addMockedMethod("getViewRegistry").withConstructor().createMock();
    final List<AmbariGrantedAuthority> authorities = new ArrayList<AmbariGrantedAuthority>();
    final ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    for (int permissionGranted: permissionsGranted) {
      final AmbariGrantedAuthority authority = createNiceMock(AmbariGrantedAuthority.class);
      final PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
      final PermissionEntity permission = createNiceMock(PermissionEntity.class);

      expect(authority.getPrivilegeEntity()).andReturn(privilegeEntity).anyTimes();
      expect(privilegeEntity.getPermission()).andReturn(permission).anyTimes();
      expect(permission.getId()).andReturn(permissionGranted).anyTimes();

      replay(authority, privilegeEntity, permission);
      authorities.add(authority);
    }

    EasyMock.<Collection<? extends GrantedAuthority>>expect(authentication.getAuthorities()).andReturn(authorities).anyTimes();
    expect(filterConfig.getInitParameter("realm")).andReturn("AuthFilter").anyTimes();
    if (username == null) {
      expect(authentication.isAuthenticated()).andReturn(false).anyTimes();
    } else {
      expect(authentication.isAuthenticated()).andReturn(true).anyTimes();
      expect(authentication.getName()).andReturn(username).anyTimes();
    }
    expect(filter.getSecurityContext()).andReturn(securityContext).anyTimes();
    expect(filter.getViewRegistry()).andReturn(viewRegistry).anyTimes();
    expect(securityContext.getAuthentication()).andReturn(authentication).anyTimes();
    expect(viewRegistry.checkPermission(EasyMock.eq("AllowedView"), EasyMock.<String>anyObject(), EasyMock.<String>anyObject(), EasyMock.anyBoolean())).andAnswer(new IAnswer<Boolean>() {
      @Override
      public Boolean answer() throws Throwable {
        for (int permissionGranted: permissionsGranted) {
          if (permissionGranted == PermissionEntity.VIEW_USE_PERMISSION) {
            return true;
          }
        }
        return false;
      }
    }).anyTimes();
    expect(viewRegistry.checkPermission(EasyMock.eq("DeniedView"), EasyMock.<String>anyObject(), EasyMock.<String>anyObject(), EasyMock.anyBoolean())).andReturn(false).anyTimes();

    replay(authentication, filterConfig, filter, securityContext, viewRegistry);

    for (final Cell<String, String, Boolean> urlTest: urlTests.cellSet()) {
      final FilterChain chain = EasyMock.createStrictMock(FilterChain.class);
      final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
      final HttpServletResponse response = createNiceMock(HttpServletResponse.class);

      String URI = urlTest.getRowKey();
      String[] URIParts = URI.split("\\?");

      expect(request.getRequestURI()).andReturn(URIParts[0]).anyTimes();
      expect(request.getQueryString()).andReturn(URIParts.length == 2 ? URIParts[1] : null).anyTimes();
      expect(request.getMethod()).andReturn(urlTest.getColumnKey()).anyTimes();

      if (expectRedirect) {
        String redirectURL = AmbariAuthorizationFilter.LOGIN_REDIRECT_BASE + urlTest.getRowKey();
        expect(response.encodeRedirectURL(redirectURL)).andReturn(redirectURL);
        response.sendRedirect(redirectURL);
      }

      if (urlTest.getValue()) {
        chain.doFilter(EasyMock.<ServletRequest>anyObject(), EasyMock.<ServletResponse>anyObject());
        EasyMock.expectLastCall().once();
      }

      replay(request, response, chain);

      try {
        filter.doFilter(request, response, chain);
      } catch (AssertionError error) {
        throw new Exception("doFilter() should not be chained on " + urlTest.getColumnKey() + " " + urlTest.getRowKey(), error);
      }

      try {
        verify(chain);

        if (expectRedirect) {
          verify(response);
        }
      } catch (AssertionError error) {
        throw new Exception("verify( failed on " + urlTest.getColumnKey() + " " + urlTest.getRowKey(), error);
      }
    }
  }

  @Test
  public void testParseUserName() throws Exception {
    final String[] pathesToTest = {
        "/api/v1/users/user",
        "/api/v1/users/user?fields=*",
        "/api/v22/users/user?fields=*"
    };
    for (String contextPath: pathesToTest) {
      final String username = AmbariAuthorizationFilter.parseUserName(contextPath);
      Assert.assertEquals("user", username);
    }
  }

  @Test
  public void testParseUserNameSpecial() throws Exception {
    String contextPath = "/api/v1/users/user%3F";
    String username = AmbariAuthorizationFilter.parseUserName(contextPath);
    Assert.assertEquals("user?", username);

    contextPath = "/api/v1/users/a%20b";
    username = AmbariAuthorizationFilter.parseUserName(contextPath);
    Assert.assertEquals("a b", username);

    contextPath = "/api/v1/users/a%2Bb";
    username = AmbariAuthorizationFilter.parseUserName(contextPath);
    Assert.assertEquals("a+b", username);

    contextPath = "/api/v1/users/a%21";
    username = AmbariAuthorizationFilter.parseUserName(contextPath);
    Assert.assertEquals("a!", username);

    contextPath = "/api/v1/users/a%3D";
    username = AmbariAuthorizationFilter.parseUserName(contextPath);
    Assert.assertEquals("a=", username);

    contextPath = "/api/v1/users/a%2Fb";
    username = AmbariAuthorizationFilter.parseUserName(contextPath);
    Assert.assertEquals("a/b", username);

    contextPath = "/api/v1/users/a%23";
    username = AmbariAuthorizationFilter.parseUserName(contextPath);
    Assert.assertEquals("a#", username);

    contextPath = "/api/v1/users/%3F%3F";
    username = AmbariAuthorizationFilter.parseUserName(contextPath);
    Assert.assertEquals("??", username);
  }

  @Test
  public void testParseViewContextPath() throws Exception {
    final String[] pathesToTest = {
        AmbariAuthorizationFilter.VIEWS_CONTEXT_PATH_PREFIX + "MY_VIEW/1.0.0/INSTANCE1",
        AmbariAuthorizationFilter.VIEWS_CONTEXT_PATH_PREFIX + "MY_VIEW/1.0.0/INSTANCE1/index.html",
        AmbariAuthorizationFilter.VIEWS_CONTEXT_PATH_PREFIX + "MY_VIEW/1.0.0/INSTANCE1/api/test"
    };
    for (String contextPath: pathesToTest) {
      final ViewInstanceVersionDTO dto = AmbariAuthorizationFilter.parseViewInstanceInfo(contextPath);
      Assert.assertEquals("INSTANCE1", dto.getInstanceName());
      Assert.assertEquals("MY_VIEW", dto.getViewName());
      Assert.assertEquals("1.0.0", dto.getVersion());
    }
  }
}
