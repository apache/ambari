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

package org.apache.ambari.server.bootstrap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.server.api.rest.BootStrapResource;
import org.apache.ambari.server.bootstrap.BSResponse.BSRunStat;
import org.apache.ambari.server.bootstrap.BootStrapStatus.BSStat;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 *  Testing bootstrap API.
 */
public class BootStrapResourceTest extends JerseyTest {

  static String PACKAGE_NAME = "org.apache.ambari.server.api.rest";
  private static final Logger LOG = LoggerFactory.getLogger(BootStrapResourceTest.class);
  Injector injector;
  BootStrapImpl bsImpl;

  @Override
  protected ResourceConfig configure() {
    ResourceConfig config = new ResourceConfig();
    config.packages(PACKAGE_NAME);
    DeploymentContext.builder(config).build();
    return config;
  }

  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      BootStrapImpl bsImpl = mock(BootStrapImpl.class);
      when(bsImpl.getStatus(0)).thenReturn(generateDummyBSStatus());
      when(bsImpl.runBootStrap(any(SshHostInfo.class))).thenReturn(generateBSResponse());
      bind(BootStrapImpl.class).toInstance(bsImpl);
      requestStaticInjection(BootStrapResource.class);
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new MockModule());
  }

  protected int getPort(int defaultPort) {
    // Find a free port
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      // Ignore
    }
    return defaultPort;
  }

  protected SshHostInfo createDummySshInfo() throws JSONException {
    SshHostInfo sshInfo = new SshHostInfo();
    sshInfo.setSshKey("awesome");
    ArrayList<String> hosts = new ArrayList<>();
    hosts.add("host1");
    sshInfo.setHosts(hosts);
    sshInfo.setVerbose(true);
    return sshInfo;
  }

  protected BSResponse generateBSResponse() {
    BSResponse response = new BSResponse();
    response.setLog("Logging");
    response.setRequestId(1);
    response.setStatus(BSRunStat.OK);
    return response;
  }

  protected BootStrapStatus generateDummyBSStatus() {
    BootStrapStatus status = new BootStrapStatus();
    status.setLog("Logging ");
    status.setStatus(BSStat.ERROR);
    status.setHostsStatus(new ArrayList<>());
    return status;
  }

  @Test
  public void bootStrapGet() {
    WebTarget webTarget = target("/bootstrap/0");
    BootStrapStatus status = webTarget.request(MediaType.APPLICATION_JSON)
            .get(BootStrapStatus.class);
    LOG.info("GET Response from the API " + status.getLog() + " " +
            status.getStatus());
    Assert.assertEquals(BSStat.ERROR, status.getStatus());
  }

  @Test
  public void bootStrapPost() throws JSONException {
    WebTarget webTarget = target("/bootstrap");
    JsonNode object = webTarget.request(MediaType.APPLICATION_JSON)
            .post(Entity.json(createDummySshInfo()), JsonNode.class);
    Assert.assertEquals("OK", object.get("status").asText());
  }
}
