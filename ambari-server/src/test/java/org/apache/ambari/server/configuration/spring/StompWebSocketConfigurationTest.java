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
package org.apache.ambari.server.configuration.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;

import jakarta.servlet.ServletContext;

import org.apache.ambari.server.agent.stomp.AgentCurrentDataController;
import org.apache.ambari.server.agent.stomp.AgentReportsController;
import org.apache.ambari.server.agent.stomp.HeartbeatController;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.state.Clusters;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;

import com.google.inject.Injector;

public class StompWebSocketConfigurationTest {
  private Injector injector;
  private ServletContext servletContext;

  @Before
  public void setUp() {
    injector = mock(Injector.class);
    servletContext = mock(ServletContext.class);
    when(injector.getInstance(Configuration.class)).thenReturn(mock(Configuration.class));
    when(injector.getInstance(Users.class)).thenReturn(mock(Users.class));
    when(injector.getInstance(Clusters.class)).thenReturn(mock(Clusters.class));
    when(injector.getInstance(HostRoleCommandDAO.class)).thenReturn(mock(HostRoleCommandDAO.class));
    when(injector.getInstance(RequestDAO.class)).thenReturn(mock(RequestDAO.class));
    when(injector.getInstance(SessionHandler.class)).thenReturn(mock(SessionHandler.class));
  }

  @Test
  public void usesJetty12UpgradeStrategyForEveryStompEndpoint() {
    AgentStompConfig agentConfig = new AgentStompConfig(servletContext, injector);
    ApiStompConfig apiConfig = new ApiStompConfig(injector);
    RootStompConfig rootConfig = new RootStompConfig(servletContext, injector);

    assertJetty12UpgradeStrategy(agentConfig.getHandshakeHandler());
    assertJetty12UpgradeStrategy(apiConfig.getHandshakeHandler());
    assertJetty12UpgradeStrategy(rootConfig.handshakeHandler());
  }

  @Test
  public void agentControllersExplicitlyMapSessionIdHeaders() {
    assertSessionIdHeaders(HeartbeatController.class);
    assertSessionIdHeaders(AgentCurrentDataController.class);
    assertSessionIdHeaders(AgentReportsController.class);
  }

  @Test
  public void apiEndpointDeclaresSameOriginPolicy() {
    StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
    StompWebSocketEndpointRegistration endpoint = mock(StompWebSocketEndpointRegistration.class);
    SockJsServiceRegistration sockJs = mock(SockJsServiceRegistration.class);
    when(registry.addEndpoint("/v1")).thenReturn(endpoint);
    when(endpoint.setHandshakeHandler(any(DefaultHandshakeHandler.class))).thenReturn(endpoint);
    when(endpoint.setAllowedOrigins()).thenReturn(endpoint);
    when(endpoint.addInterceptors(any(HandshakeInterceptor[].class))).thenReturn(endpoint);
    when(endpoint.withSockJS()).thenReturn(sockJs);
    when(sockJs.setHeartbeatTime(0L)).thenReturn(sockJs);

    new ApiStompConfig(injector).registerStompEndpoints(registry);

    verify(endpoint).setAllowedOrigins();
    verify(endpoint, never()).setAllowedOriginPatterns(any(String[].class));
  }

  @Test
  public void springSameOriginPolicyRejectsForeignOrigin() throws Exception {
    OriginHandshakeInterceptor policy = new OriginHandshakeInterceptor();
    WebSocketHandler handler = mock(WebSocketHandler.class);
    MockHttpServletResponse sameOriginResponse = new MockHttpServletResponse();
    MockHttpServletResponse foreignOriginResponse = new MockHttpServletResponse();

    assertTrue(policy.beforeHandshake(request("https://ambari.example"),
        new ServletServerHttpResponse(sameOriginResponse), handler, new HashMap<>()));
    assertFalse(policy.beforeHandshake(request("https://attacker.example"),
        new ServletServerHttpResponse(foreignOriginResponse), handler, new HashMap<>()));
    assertEquals(403, foreignOriginResponse.getStatus());
  }

  private void assertJetty12UpgradeStrategy(DefaultHandshakeHandler handshakeHandler) {
    assertTrue(handshakeHandler.getRequestUpgradeStrategy() instanceof JettyRequestUpgradeStrategy);
  }

  private void assertSessionIdHeaders(Class<?> controllerClass) {
    for (Method method : controllerClass.getDeclaredMethods()) {
      for (Parameter parameter : method.getParameters()) {
        Header header = parameter.getAnnotation(Header.class);
        if (header != null) {
          assertEquals(SimpMessageHeaderAccessor.SESSION_ID_HEADER, header.value());
        }
      }
    }
  }

  private ServletServerHttpRequest request(String origin) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme("https");
    request.setServerName("ambari.example");
    request.setServerPort(443);
    request.setRequestURI("/api/stomp/v1/websocket");
    request.addHeader(HttpHeaders.ORIGIN, origin);
    return new ServletServerHttpRequest(request);
  }
}
