/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.mpack.MpackSnapshots;
import org.apache.ambari.server.resources.ResourceManager;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.Provider;

public class MpackResourcesHttpTest {
  @Rule
  public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void servesSnapshotArchivesWithoutStaticAliasesAndRejectsUnindexedFiles() throws Exception {
    String id = "a".repeat(64);
    Path staticRoot = temporary.newFolder("static").toPath();
    Path snapshotRoot = temporary.newFolder("snapshot").toPath();
    Path hostScripts = Files.createDirectories(snapshotRoot.resolve("host_scripts"));
    Files.writeString(hostScripts.resolve(".hash"), "directory-content-digest");
    Files.writeString(hostScripts.resolve("archive.zip"), "pinned-archive");
    Files.writeString(hostScripts.resolve("private.json"), "not-a-download");
    Path legacy = Files.createDirectories(staticRoot.resolve("host_scripts"));
    Files.writeString(legacy.resolve(".hash"), "legacy-digest");

    MpackSnapshots snapshots = mock(MpackSnapshots.class);
    when(snapshots.load(id)).thenReturn(new MpackSnapshots.Snapshot(1, id, null,
        List.of(), List.of(), Map.of(), Map.of("mpacks/" + id + "/host_scripts", "b".repeat(64))));
    when(snapshots.resourceRoot(id)).thenReturn(snapshotRoot);
    ResourceManager manager = new ResourceManager();
    java.lang.reflect.Field field = ResourceManager.class.getDeclaredField("mpackSnapshots");
    field.setAccessible(true);
    field.set(manager, (Provider<MpackSnapshots>) () -> snapshots);

    Server server = new Server(0);
    try {
      ServletContextHandler context = new ServletContextHandler("/", ServletContextHandler.NO_SESSIONS);
      AmbariServer.configureResourcesServlet(context, staticRoot.toFile(), manager);
      server.setHandler(context);
      server.start();
      int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
      HttpClient client = HttpClient.newHttpClient();
      String prefix = "http://localhost:" + port + "/resources/";
      for (String name : List.of(".hash", "archive.zip")) {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(
            URI.create(prefix + "mpacks/" + id + "/host_scripts/" + name)).build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(Files.readString(hostScripts.resolve(name)), response.body());
      }
      for (String path : List.of("host_scripts/private.json", "unknown/archive.zip")) {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(
            URI.create(prefix + "mpacks/" + id + "/" + path)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
      }
      HttpResponse<String> original = client.send(HttpRequest.newBuilder(
          URI.create(prefix + "host_scripts/.hash")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, original.statusCode());
      assertEquals("legacy-digest", original.body());
    } finally {
      server.stop();
    }
  }
}
