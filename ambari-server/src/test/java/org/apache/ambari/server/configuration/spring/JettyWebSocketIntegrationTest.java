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

package org.apache.ambari.server.configuration.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.awaitility.Awaitility;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy;

public class JettyWebSocketIntegrationTest {

  private static final int MAX_MESSAGE_SIZE = 128 * 1024;

  private final AtomicInteger openedConnections = new AtomicInteger();
  private final AtomicInteger closedConnections = new AtomicInteger();
  private Server server;

  @AfterEach
  public void stopServer() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testMessageExchangeAndReconnectLifecycle() throws Exception {
    AtomicReference<JettyWebSocketServerContainer> containerReference = new AtomicReference<>();
    server = new Server(0);
    ServletContextHandler context = new ServletContextHandler("/", ServletContextHandler.NO_SESSIONS);
    context.addServlet(DefaultServlet.class, "/");
    JettyWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
      container.setMaxTextMessageSize(MAX_MESSAGE_SIZE);
      containerReference.set(container);
    });
    context.addServlet(new SpringUpgradeServlet(
        new JettyRequestUpgradeStrategy(), new EchoWebSocketHandler(openedConnections, closedConnections)),
        "/agent/stomp/v1");
    server.setHandler(context);
    server.start();

    JettyWebSocketServerContainer container = containerReference.get();
    assertTrue(container.isStarted());
    assertEquals(MAX_MESSAGE_SIZE, container.getMaxTextMessageSize());

    URI endpoint = URI.create("ws://localhost:"
        + ((ServerConnector) server.getConnectors()[0]).getLocalPort() + "/agent/stomp/v1");
    assertEquals("first", connectAndEcho(endpoint, "first"));
    assertEquals("second", connectAndEcho(endpoint, "second"));

    Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertEquals(2, openedConnections.get());
      assertEquals(2, closedConnections.get());
    });

    server.stop();
    server = null;
    assertTrue(container.isStopped());
  }

  private String connectAndEcho(URI endpoint, String message) throws Exception {
    ClientListener listener = new ClientListener();
    WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder()
        .buildAsync(endpoint, listener).get(5, TimeUnit.SECONDS);
    webSocket.sendText(message, true).get(5, TimeUnit.SECONDS);
    String echoed = listener.message.get(5, TimeUnit.SECONDS);
    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").get(5, TimeUnit.SECONDS);
    listener.closed.get(5, TimeUnit.SECONDS);
    return echoed;
  }

  private static class EchoWebSocketHandler extends TextWebSocketHandler {
    private final AtomicInteger openedConnections;
    private final AtomicInteger closedConnections;

    private EchoWebSocketHandler(AtomicInteger openedConnections, AtomicInteger closedConnections) {
      this.openedConnections = openedConnections;
      this.closedConnections = closedConnections;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
      openedConnections.incrementAndGet();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
      session.sendMessage(message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
      closedConnections.incrementAndGet();
    }
  }

  private static class SpringUpgradeServlet extends HttpServlet {
    private final JettyRequestUpgradeStrategy upgradeStrategy;
    private final TextWebSocketHandler webSocketHandler;

    private SpringUpgradeServlet(JettyRequestUpgradeStrategy upgradeStrategy,
        TextWebSocketHandler webSocketHandler) {
      this.upgradeStrategy = upgradeStrategy;
      this.webSocketHandler = webSocketHandler;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
      try {
        upgradeStrategy.upgrade(new ServletServerHttpRequest(request), new ServletServerHttpResponse(response),
            null, Collections.emptyList(), request.getUserPrincipal(), webSocketHandler, Collections.emptyMap());
      } catch (RuntimeException e) {
        throw new ServletException("WebSocket upgrade failed", e);
      }
    }
  }

  private static class ClientListener implements WebSocket.Listener {
    private final CompletableFuture<String> message = new CompletableFuture<>();
    private final CompletableFuture<Integer> closed = new CompletableFuture<>();

    @Override
    public void onOpen(WebSocket webSocket) {
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      if (last) {
        message.complete(data.toString());
      }
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      closed.complete(statusCode);
      return CompletableFuture.completedFuture(null);
    }
  }
}
