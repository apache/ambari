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

package org.apache.ambari.server.security;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.Assert;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.util.Properties;

import static org.easymock.EasyMock.expectLastCall;

public class SecurityHeaderFilterTest extends EasyMockSupport {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    temporaryFolder.create();
  }

  @After
  public void tearDown() throws Exception {
    temporaryFolder.delete();
  }

  @Test
  public void testDoFilter_DefaultValuesNoSSL() throws Exception {
    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.API_USE_SSL, "false");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(SecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE_DEFAULT);
    expectLastCall().once();
    servletResponse.setHeader(SecurityHeaderFilter.X_XSS_PROTECTION_HEADER, Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE_DEFAULT);
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    SecurityHeaderFilter securityFilter = injector.getInstance(SecurityHeaderFilter.class);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_DefaultValuesSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.API_USE_SSL, "true");
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY, httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY, httpPassFile.getName());

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(SecurityHeaderFilter.STRICT_TRANSPORT_HEADER, Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE_DEFAULT);
    expectLastCall().once();
    servletResponse.setHeader(SecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE_DEFAULT);
    expectLastCall().once();
    servletResponse.setHeader(SecurityHeaderFilter.X_XSS_PROTECTION_HEADER, Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE_DEFAULT);
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    SecurityHeaderFilter securityFilter = injector.getInstance(SecurityHeaderFilter.class);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_CustomValuesNoSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY, httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY, httpPassFile.getName());
        properties.setProperty(Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE_KEY, "custom1");
        properties.setProperty(Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE_KEY, "custom2");
        properties.setProperty(Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE_KEY, "custom3");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(SecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, "custom2");
    expectLastCall().once();
    servletResponse.setHeader(SecurityHeaderFilter.X_XSS_PROTECTION_HEADER, "custom3");
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    SecurityHeaderFilter securityFilter = injector.getInstance(SecurityHeaderFilter.class);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_CustomValuesSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.API_USE_SSL, "true");
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY, httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY, httpPassFile.getName());
        properties.setProperty(Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE_KEY, "custom1");
        properties.setProperty(Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE_KEY, "custom2");
        properties.setProperty(Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE_KEY, "custom3");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(SecurityHeaderFilter.STRICT_TRANSPORT_HEADER, "custom1");
    expectLastCall().once();
    servletResponse.setHeader(SecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, "custom2");
    expectLastCall().once();
    servletResponse.setHeader(SecurityHeaderFilter.X_XSS_PROTECTION_HEADER, "custom3");
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    SecurityHeaderFilter securityFilter = injector.getInstance(SecurityHeaderFilter.class);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_EmptyValuesNoSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY, httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY, httpPassFile.getName());
        properties.setProperty(Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE_KEY, "");
        properties.setProperty(Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE_KEY, "");
        properties.setProperty(Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE_KEY, "");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    SecurityHeaderFilter securityFilter = injector.getInstance(SecurityHeaderFilter.class);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

  @Test
  public void testDoFilter_EmptyValuesSSL() throws Exception {
    final File httpPassFile = temporaryFolder.newFile();

    Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.API_USE_SSL, "true");
        properties.setProperty(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY, httpPassFile.getParent());
        properties.setProperty(Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY, httpPassFile.getName());
        properties.setProperty(Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE_KEY, "");
        properties.setProperty(Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE_KEY, "");
        properties.setProperty(Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE_KEY, "");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    SecurityHeaderFilter securityFilter = injector.getInstance(SecurityHeaderFilter.class);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }
}