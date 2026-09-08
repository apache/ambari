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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AmbariServerStaticResourcesTest {
  private Server server;

  @TempDir
  private Path resourcesDirectory;

  @AfterEach
  public void stopServer() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void servesAgentResourcesFromTheConfiguredDirectory() throws Exception {
    Path hostScripts = Files.createDirectories(resourcesDirectory.resolve("host_scripts"));
    Files.writeString(hostScripts.resolve(".hash"), "resource-hash", StandardCharsets.UTF_8);

    server = new Server(0);
    ServletContextHandler context = new ServletContextHandler("/", ServletContextHandler.NO_SESSIONS);
    AmbariServer.configureResourcesServlet(context, resourcesDirectory.toFile());
    server.setHandler(context);
    server.start();

    int localPort = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    HttpRequest request = HttpRequest.newBuilder(
        URI.create("http://localhost:" + localPort + "/resources/host_scripts/.hash")).build();
    HttpResponse<String> response = HttpClient.newHttpClient().send(
        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    assertEquals(200, response.statusCode());
    assertEquals("resource-hash", response.body());
  }
}
