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

package org.apache.ambari.server.functionaltests.server;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.StringReader;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.http.HttpStatus;

/**
 * Simple test that starts the local ambari server,
 * tests it's status and shuts down the server.
 */
@Ignore
public class StartStopServerTest {
  /**
   * Run the ambari server on a thread.
   */
  private Thread serverThread = null;

  /**
   * Instance of the local ambari server, which wraps the actual
   * ambari server with test configuration.
   */
  private LocalAmbariServer server = null;

  /**
   * Server port
   */
  private static int serverPort = 9995;

  /**
   * Server URL
   */
  private static String SERVER_URL_FORMAT = "http://localhost:%d";

  /**
   * Test URL for GETting the status of the ambari server
   */
  private static String stacksPath = "/api/v1/stacks";

  /**
   * Start our local server on a thread so that it does not block.
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule testModule = new InMemoryDefaultTestModule();
    Properties properties = testModule.getProperties();
    properties.setProperty(Configuration.AGENT_USE_SSL, "false");
    properties.setProperty(Configuration.CLIENT_API_PORT_KEY, Integer.toString(serverPort));
    server = new LocalAmbariServer(testModule);
    serverThread = new Thread(server);
    serverThread.start();
    serverThread.join(20000);     // Give a few seconds for the ambari server to start up
  }

  /**
   * Shut down our local server.
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    if (serverThread != null) {
      serverThread.interrupt();
    }
    if (server != null) {
      server.stopServer();
    }
  }

  /**
   * Waits for the ambari server to startup and then checks it's
   * status by querying /api/v1/stacks (does not touch the DB)
   */
  @Test
  public void testServerStatus() throws IOException {
    /**
     * Query the ambari server for the list of stacks.
     * A successful GET returns the list of stacks.
     * We should get a json like:
     * {
     *   "href" : "http://localhost:9995/api/v1/stacks",
     *   "items" : [
     *   {
     *     "href" : "http://localhost:9995/api/v1/stacks/HDP",
     *     "Stacks" : {
     *     "stack_name" : "HDP"
     *     }
     *   }
     *  ]
     * }
     */

    String stacksUrl = String.format(SERVER_URL_FORMAT, serverPort) + stacksPath;
    HttpClient httpClient = new HttpClient();
    GetMethod getMethod = new GetMethod(stacksUrl);

    try {
      int statusCode = httpClient.executeMethod(getMethod);

      assertTrue (statusCode == HttpStatus.SC_OK); // HTTP status code 200

      String responseBody = getMethod.getResponseBodyAsString();

      assertTrue(responseBody != null); // Make sure response body is valid

      JsonElement jsonElement = new JsonParser().parse(new JsonReader(new StringReader(responseBody)));

      assertTrue (jsonElement != null); // Response was a JSON string

      JsonObject jsonObject = jsonElement.getAsJsonObject();

      assertTrue (jsonObject.has("items"));  // Should have "items" entry

      JsonArray stacksArray = jsonObject.get("items").getAsJsonArray();

      assertTrue (stacksArray.size() > 0); // Should have at least one stack

    } finally {
      getMethod.releaseConnection();
    }
  }
}
