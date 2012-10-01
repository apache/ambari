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

package org.apache.ambari.server.agent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import javax.ws.rs.core.MediaType;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.rest.AgentResource;
import org.apache.ambari.server.state.live.Clusters;
import org.apache.ambari.server.state.live.ClustersImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

public class AgentResourceTest extends JerseyTest {
  static String PACKAGE_NAME = "org.apache.ambari.server.agent.rest";
  private static Log LOG = LogFactory.getLog(AgentResourceTest.class);
  HeartBeatHandler handler;
  ActionManager actionManager;
  Injector injector;

  public AgentResourceTest() {
    super(new WebAppDescriptor.Builder(PACKAGE_NAME).servletClass(ServletContainer.class)
        .build());
  }

  public class MockModule extends AbstractModule {

    RegistrationResponse response = new RegistrationResponse();
    HeartBeatResponse hresponse = new HeartBeatResponse();

    @Override
    protected void configure() {
      handler = mock(HeartBeatHandler.class);
      response.setResponseStatus(RegistrationStatus.OK);
      hresponse.setResponseId(0L);
      try {
        when(handler.handleRegistration(any(Register.class))).thenReturn(
            response);
        when(handler.handleHeartBeat(any(HeartBeat.class))).thenReturn(
            hresponse);
      } catch (Exception ex) {
        // The test will fail anyway
      }
      requestStaticInjection(AgentResource.class);
      bind(Clusters.class).to(ClustersImpl.class);
      actionManager = mock(ActionManager.class);
      bind(ActionManager.class).toInstance(actionManager);
      bind(AgentCommand.class).to(ExecutionCommand.class);
      bind(HeartBeatHandler.class).toInstance(handler);
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    handler = mock(HeartBeatHandler.class);
    injector = Guice.createInjector(new MockModule());
    injector.injectMembers(handler);
  }

  private JSONObject createDummyJSONRegister() throws JSONException {
    JSONObject json = new JSONObject();
    json.append("responseId" , -1);
    json.append("timestamp" , System.currentTimeMillis());
    json.append("hostname",   "dummyHost");
    return json;
  }

  private JSONObject createDummyHeartBeat() throws JSONException {
    JSONObject json = new JSONObject();
    json.put("responseId", -1);
    json.put("timestamp" , System.currentTimeMillis());
    json.put("hostname", "dummyHost");
    return json;
  }

  @Test
  public void agentRegistration() throws UniformInterfaceException, JSONException {
    RegistrationResponse response;
    WebResource webResource = resource();
    response = webResource.path("/register/dummyhost").type(MediaType.APPLICATION_JSON)
      .post(RegistrationResponse.class, createDummyJSONRegister());
    LOG.info("Returned from Server " + response.getResponseStatus());
    Assert.assertEquals(response.getResponseStatus(), RegistrationStatus.OK);
  }

  @Test
  public void agentHeartBeat() throws UniformInterfaceException, JSONException {
    HeartBeatResponse response;
    WebResource resource = resource();
    response = resource.path("/heartbeat/dummyhost").type(MediaType.APPLICATION_JSON)
        .post(HeartBeatResponse.class, createDummyHeartBeat());
    LOG.info("Returned from Server: " + "clusterid = " + response.getClusterId()
        + " responseid=" +   response.getResponseId());
    Assert.assertEquals(response.getResponseId(), 0L);
  }
}
