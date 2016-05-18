/**
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

package org.apache.ambari.server.controller;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.SessionCookieConfig;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.velocity.app.Velocity;
import org.easymock.EasyMock;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AmbariServerTest {

  private Injector injector;


  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws AmbariException {
  }

  @Test
  public void testVelocityLogger() throws Exception {
    new AmbariServer();
    Assert.assertEquals(AmbariServer.VELOCITY_LOG_CATEGORY, Velocity.getProperty("runtime.log.logsystem.log4j.logger"));
  }

  @Test
  public void testConfigureSessionManager() throws Exception {
    AmbariServer ambariServer = new AmbariServer();

    Configuration configuration = createNiceMock(Configuration.class);
    SessionManager sessionManager = createNiceMock(SessionManager.class);
    SessionCookieConfig sessionCookieConfig = createNiceMock(SessionCookieConfig.class);

    ambariServer.configs = configuration;

    expect(sessionManager.getSessionCookieConfig()).andReturn(sessionCookieConfig).anyTimes();

    expect(configuration.getApiSSLAuthentication()).andReturn(false);
    sessionCookieConfig.setHttpOnly(true);

    expect(configuration.getApiSSLAuthentication()).andReturn(true);
    sessionCookieConfig.setHttpOnly(true);
    sessionCookieConfig.setSecure(true);

    replay(configuration, sessionManager, sessionCookieConfig);

    // getApiSSLAuthentication == false
    ambariServer.configureSessionManager(sessionManager);

    // getApiSSLAuthentication == true
    ambariServer.configureSessionManager(sessionManager);

    verify(configuration, sessionManager, sessionCookieConfig);
  }

  @Test
  public void testSystemProperties() throws Exception {
    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    expect(configuration.getServerTempDir()).andReturn("/ambari/server/temp/dir").anyTimes();
    replay(configuration);
    AmbariServer.setSystemProperties(configuration);
    Assert.assertEquals(System.getProperty("java.io.tmpdir"), "/ambari/server/temp/dir");
  }

  @Test
  public void testProxyUser() throws Exception {

    PasswordAuthentication pa = Authenticator.requestPasswordAuthentication(
        InetAddress.getLocalHost(), 80, null, null, null);
    Assert.assertNull(pa);

    System.setProperty("http.proxyUser", "abc");
    System.setProperty("http.proxyPassword", "def");

    AmbariServer.setupProxyAuth();

    pa = Authenticator.requestPasswordAuthentication(
        InetAddress.getLocalHost(), 80, null, null, null);
    Assert.assertNotNull(pa);
    Assert.assertEquals("abc", pa.getUserName());
    Assert.assertArrayEquals("def".toCharArray(), pa.getPassword());

  }

  @Test
  public void testConfigureRootHandler() throws Exception {
    final ServletContextHandler handler =
        EasyMock.createNiceMock(ServletContextHandler.class);
    final FilterHolder filter = EasyMock.createNiceMock(FilterHolder.class);

    handler.setMaxFormContentSize(-1);
    EasyMock.expectLastCall().once();
    EasyMock.expect(handler.addFilter(GzipFilter.class, "/*",
        EnumSet.of(DispatcherType.REQUEST))).andReturn(filter).once();
    replay(handler, filter);

    injector.getInstance(AmbariServer.class).configureRootHandler(handler);

    EasyMock.verify(handler);
  }

  @Test
  public void testConfigureCompression() throws Exception {
    final ServletContextHandler handler =
        EasyMock.createNiceMock(ServletContextHandler.class);
    final FilterHolder filter = EasyMock.createNiceMock(FilterHolder.class);

    EasyMock.expect(handler.addFilter(GzipFilter.class, "/*",
        EnumSet.of(DispatcherType.REQUEST))).andReturn(filter).once();
    filter.setInitParameter(anyObject(String.class),anyObject(String.class));
    EasyMock.expectLastCall().times(3);
    replay(handler, filter);

    injector.getInstance(AmbariServer.class).configureHandlerCompression(handler);

    EasyMock.verify(handler);
  }

  /**
   * Tests that Jetty pools are configured with the correct number of
   * Acceptor/Selector threads.
   *
   * @throws Exception
   */
  @Test
  public void testJettyThreadPoolCalculation() throws Exception {
    Server server = new Server();
    AmbariServer ambariServer = new AmbariServer();

    // 12 acceptors (48 core machine) with a configured pool size of 25
    ambariServer.configureJettyThreadPool(server, 12, "mock-pool", 25);
    Assert.assertEquals(44, ((QueuedThreadPool) server.getThreadPool()).getMaxThreads());

    // 2 acceptors (8 core machine) with a configured pool size of 25
    ambariServer.configureJettyThreadPool(server, 2, "mock-pool", 25);
    Assert.assertEquals(25, ((QueuedThreadPool) server.getThreadPool()).getMaxThreads());

    // 16 acceptors (64 core machine) with a configured pool size of 35
    ambariServer.configureJettyThreadPool(server, 16, "mock-pool", 35);
    Assert.assertEquals(52, ((QueuedThreadPool) server.getThreadPool()).getMaxThreads());

  }
}
