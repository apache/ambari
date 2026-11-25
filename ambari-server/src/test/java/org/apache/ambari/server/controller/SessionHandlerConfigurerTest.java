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
package org.apache.ambari.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ambari.server.configuration.Configuration;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SessionHandlerConfigurerTest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testConfigureBeforeStartup(boolean secure) {
    SessionHandler handler = new SessionHandler();
    configurer(secure).configureSessionHandler(handler);
    assertConfiguration(handler, secure);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testConfigureViewDuringRequestToStartedContext(boolean secure) throws Exception {
    SessionHandlerConfigurer configurer = configurer(secure);
    AtomicReference<SessionHandler> viewSessions = new AtomicReference<>();
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new HttpServlet() {
      @Override
      protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionHandler handler = new SessionHandler();
        handler.setSessionIdManager(context.getSessionHandler().getSessionIdManager());
        configurer.configureSessionHandler(handler);
        viewSessions.set(handler);
        response.setStatus(HttpServletResponse.SC_OK);
      }
    }), "/create-view");

    Server server = new Server(0);
    server.setHandler(context);
    try {
      server.start();
      int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
      HttpResponse<Void> response = HttpClient.newHttpClient().send(
          HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/create-view")).build(),
          HttpResponse.BodyHandlers.discarding());

      assertEquals(200, response.statusCode());
      assertNotNull(viewSessions.get());
      assertConfiguration(viewSessions.get(), secure);
    } finally {
      server.stop();
    }
  }

  private SessionHandlerConfigurer configurer(boolean secure) {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getApiSSLAuthentication()).thenReturn(secure);
    when(configuration.getHttpSessionInactiveTimeout()).thenReturn(1200);
    SessionHandlerConfigurer configurer = new SessionHandlerConfigurer();
    configurer.configuration = configuration;
    return configurer;
  }

  private void assertConfiguration(SessionHandler handler, boolean secure) {
    assertEquals("/", handler.getSessionCookieConfig().getPath());
    assertEquals("AMBARISESSIONID", handler.getSessionCookieConfig().getName());
    assertTrue(handler.getSessionCookieConfig().isHttpOnly());
    assertEquals(secure, handler.getSessionCookieConfig().isSecure());
    assertEquals(1200, handler.getMaxInactiveInterval());
  }
}
