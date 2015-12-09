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

import java.io.File;
import java.util.Map;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

import static org.easymock.EasyMock.expectLastCall;

public abstract class AbstractSecurityHeaderFilterTest extends EasyMockSupport {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final Class<? extends AbstractSecurityHeaderFilter> filterClass;
  private final Map<String, String> propertyNameMap;
  private final Map<String, String> defatulPropertyValueMap;

  protected AbstractSecurityHeaderFilterTest(Class<? extends AbstractSecurityHeaderFilter> filterClass, Map<String, String> propertyNameMap, Map<String, String> defatulPropertyValueMap) {
    this.filterClass = filterClass;
    this.propertyNameMap = propertyNameMap;
    this.defatulPropertyValueMap = defatulPropertyValueMap;
  }

  protected abstract void expectHttpServletRequestMock(HttpServletRequest request);

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
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER));
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
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
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER));
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, defatulPropertyValueMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER));
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
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
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER), "custom1");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER), "custom2");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER), "custom3");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, "custom2");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, "custom3");
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
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
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER), "custom1");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER), "custom2");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER), "custom3");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);
    servletResponse.setHeader(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER, "custom1");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER, "custom2");
    expectLastCall().once();
    servletResponse.setHeader(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER, "custom3");
    expectLastCall().once();

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
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
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER), "");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
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
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.STRICT_TRANSPORT_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_FRAME_OPTIONS_HEADER), "");
        properties.setProperty(propertyNameMap.get(AbstractSecurityHeaderFilter.X_XSS_PROTECTION_HEADER), "");

        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(new Configuration(properties));
      }
    });

    FilterConfig filterConfig = createNiceMock(FilterConfig.class);

    HttpServletRequest servletRequest = createStrictMock(HttpServletRequest.class);
    expectHttpServletRequestMock(servletRequest);

    HttpServletResponse servletResponse = createStrictMock(HttpServletResponse.class);

    FilterChain filterChain = createStrictMock(FilterChain.class);
    filterChain.doFilter(servletRequest, servletResponse);
    expectLastCall().once();

    replayAll();

    AbstractSecurityHeaderFilter securityFilter = injector.getInstance(filterClass);
    Assert.assertNotNull(securityFilter);

    securityFilter.init(filterConfig);
    securityFilter.doFilter(servletRequest, servletResponse, filterChain);

    verifyAll();
  }

}