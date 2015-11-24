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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Properties;

/**
 * Base test infrastructure.
 */
public class ServerTestBase {
    /**
     * Run the ambari server on a thread.
     */
    protected Thread serverThread = null;

    /**
     * Instance of the local ambari server, which wraps the actual
     * ambari server with test configuration.
     */
    protected LocalAmbariServer server = null;

    /**
     * Server port
     */
    protected static int serverPort = 9995;

    /**
     * Server agent port
     */
    protected static int serverAgentPort = 9000;

    /**
     * Guice injector using an in-memory DB.
     */
    protected Injector injector = null;

    /**
     * Server URL
     */
    protected static String SERVER_URL_FORMAT = "http://localhost:%d";

    /**
     * Start our local server on a thread so that it does not block.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        InMemoryDefaultTestModule testModule = new InMemoryDefaultTestModule();
        Properties properties = testModule.getProperties();
        properties.setProperty(Configuration.AGENT_USE_SSL, "false");
        properties.setProperty(Configuration.CLIENT_API_PORT_KEY, Integer.toString(serverPort));
        properties.setProperty(Configuration.SRVR_ONE_WAY_SSL_PORT_KEY, Integer.toString(serverAgentPort));
        String tmpDir = System.getProperty("java.io.tmpdir");
        testModule.getProperties().setProperty(Configuration.SRVR_KSTR_DIR_KEY, tmpDir);
        injector = Guice.createInjector(testModule);
        server = injector.getInstance(LocalAmbariServer.class);
        serverThread = new Thread(server);
        serverThread.start();
        waitForServer();
    }

    /**
     * Waits for the local server until it is ready to accept requests.
     *
     * @throws Exception
     */
    private void waitForServer() throws Exception {
        int count = 1;

        while (!isServerUp()) {
            serverThread.join(count * 10000);     // Give a few seconds for the ambari server to start up
            //count += 1; // progressive back off
            //count *= 2; // exponential back off
        }
    }

    /**
     * Attempt to query the server for the stack. If the server is up,
     * we will get a response. If not, an exception will be thrown.
     *
     * @return - True if the local server is responsive to queries.
     *           False, otherwise.
     */
    private boolean isServerUp() {
        String apiPath = "/api/v1/stacks";

        String apiUrl = String.format(SERVER_URL_FORMAT, serverPort) + apiPath;
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(apiUrl);

        try {
            int statusCode = httpClient.executeMethod(getMethod);
            String response = getMethod.getResponseBodyAsString();

            return true;
        } catch (IOException ex) {

        } finally {
            getMethod.releaseConnection();
        }

        return false;
    }

    /**
     * Shut down the local server.
     *
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
}
