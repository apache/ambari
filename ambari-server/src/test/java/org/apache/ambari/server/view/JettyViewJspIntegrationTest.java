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

package org.apache.ambari.server.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.ambari.server.api.AmbariErrorHandler;
import org.apache.ambari.server.api.AmbariViewErrorHandlerProxy;
import org.apache.ambari.server.security.authentication.jwt.JwtAuthenticationPropertiesProvider;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

public class JettyViewJspIntegrationTest {

  @TempDir
  private Path webRoot;

  private Server server;

  @AfterEach
  public void stopServer() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testViewDiscoversJava17AnnotatedServlet() throws Exception {
    Path source = webRoot.resolve("Java17Servlet.java");
    Files.writeString(source, """
        package view;
        import java.io.IOException;
        import jakarta.servlet.annotation.WebServlet;
        import jakarta.servlet.http.HttpServlet;
        import jakarta.servlet.http.HttpServletRequest;
        import jakarta.servlet.http.HttpServletResponse;
        @WebServlet("/java17")
        public class Java17Servlet extends HttpServlet {
          protected void doGet(HttpServletRequest request, HttpServletResponse response)
              throws IOException {
            response.getWriter().write("java17-view");
          }
        }
        """, StandardCharsets.UTF_8);
    Path classes = Files.createDirectories(webRoot.resolve("WEB-INF/classes"));
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler);
    ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
    int result = compiler.run(null, diagnostics, diagnostics, "--release", "17", "-classpath",
        System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
        "-d", classes.toString(), source.toString());
    assertEquals(0, result, diagnostics.toString(StandardCharsets.UTF_8));

    WebAppContext context = new WebAppContext();
    context.setContextPath("/");
    context.setBaseResourceAsString(webRoot.toString());
    context.setConfigurationDiscovered(true);
    context.setThrowUnavailableOnStartupException(true);
    server = new Server(0);
    server.setHandler(context);
    server.start();

    int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    HttpResponse<String> response = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/java17")).build(),
        HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertEquals("java17-view", response.body());
  }

  @Test
  public void testJspRenderingAndCustomErrorPageDelegation() throws Exception {
    Files.writeString(webRoot.resolve("index.jsp"),
        "<html><body><%= \"view-rendered\" %></body></html>", StandardCharsets.UTF_8);
    Files.writeString(webRoot.resolve("error.jsp"),
        "<html><body><%= \"custom-error-rendered\" %></body></html>", StandardCharsets.UTF_8);

    ErrorPageErrorHandler viewErrorHandler = new ErrorPageErrorHandler();
    viewErrorHandler.addErrorPage(404, "/error.jsp");
    AmbariErrorHandler ambariErrorHandler = new AmbariErrorHandler(
        new Gson(), mock(JwtAuthenticationPropertiesProvider.class));

    WebAppContext context = new WebAppContext();
    context.setContextPath("/");
    context.setBaseResourceAsString(webRoot.toString());
    context.setErrorHandler(new AmbariViewErrorHandlerProxy(viewErrorHandler, ambariErrorHandler));

    server = new Server(0);
    server.setHandler(context);
    server.start();

    int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    HttpClient client = HttpClient.newHttpClient();
    HttpResponse<String> rendered = client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/index.jsp")).build(),
        HttpResponse.BodyHandlers.ofString());
    assertEquals(200, rendered.statusCode());
    assertTrue(rendered.body().contains("view-rendered"));
    assertFalse(rendered.body().contains("<%="));

    HttpResponse<String> missing = client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/missing")).build(),
        HttpResponse.BodyHandlers.ofString());
    assertEquals(404, missing.statusCode());
    assertTrue(missing.body().contains("custom-error-rendered"));
    assertFalse(missing.body().contains("<%="));
  }
}
