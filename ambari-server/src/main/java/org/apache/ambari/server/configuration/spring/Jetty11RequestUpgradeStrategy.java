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

import java.lang.reflect.UndeclaredThrowableException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * Bridges Spring WebSocket support to the Jetty 11 API used by Ambari.
 */
public class Jetty11RequestUpgradeStrategy implements RequestUpgradeStrategy {
  private static final String[] SUPPORTED_VERSIONS = {"13"};

  @Override
  public String[] getSupportedVersions() {
    return SUPPORTED_VERSIONS;
  }

  @Override
  public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
    return Collections.emptyList();
  }

  @Override
  public void upgrade(ServerHttpRequest request, ServerHttpResponse response, String selectedProtocol,
      List<WebSocketExtension> selectedExtensions, Principal user, WebSocketHandler handler,
      Map<String, Object> attributes) throws HandshakeFailureException {
    Assert.isInstanceOf(ServletServerHttpRequest.class, request, "ServletServerHttpRequest required");
    HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
    ServletContext servletContext = servletRequest.getServletContext();

    Assert.isInstanceOf(ServletServerHttpResponse.class, response, "ServletServerHttpResponse required");
    HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

    JettyWebSocketSession session = new JettyWebSocketSession(attributes, user);
    JettyWebSocketHandlerAdapter handlerAdapter = new JettyWebSocketHandlerAdapter(handler, session);
    JettyWebSocketCreator webSocketCreator = (upgradeRequest, upgradeResponse) -> {
      if (selectedProtocol != null) {
        upgradeResponse.setAcceptedSubProtocol(selectedProtocol);
      }
      return handlerAdapter;
    };

    JettyWebSocketServerContainer container = JettyWebSocketServerContainer.getContainer(servletContext);
    try {
      container.upgrade(webSocketCreator, servletRequest, servletResponse);
    } catch (UndeclaredThrowableException e) {
      throw new HandshakeFailureException("Failed to upgrade", e.getUndeclaredThrowable());
    } catch (Exception e) {
      throw new HandshakeFailureException("Failed to upgrade", e);
    }
  }
}
