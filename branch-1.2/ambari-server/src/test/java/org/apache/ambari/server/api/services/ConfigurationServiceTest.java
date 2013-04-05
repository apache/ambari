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
package org.apache.ambari.server.api.services;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.*;

import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.junit.Test;


public class ConfigurationServiceTest extends BaseServiceTest {
  
  @Test
  public void testCreateConfiguration() throws Exception{
    String clusterName = "clusterName";

    String body = "{ \"type\":\"hdfs-site\", \"tag\":\"my-new-tag\"," +
        "\"properties\":{ \"key1\":\"value1\", \"key2\":\"value2\" } }";

    registerExpectations(Request.Type.POST, body, 200, false);
    replayMocks();

    //test
    ConfigurationService configService = new TestConfigurationService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = configService.createConfigurations(body, getHttpHeaders(), getUriInfo());

    verifyResults(response, 200);
  }
  
  private class TestConfigurationService extends ConfigurationService {
    private RequestFactory m_requestFactory;
    private RequestHandler m_requestHandler;
    private ResourceInstance m_resourceInstance;
    private String m_clusterId;

    private TestConfigurationService(ResourceInstance resourceInstance, String clusterId, RequestFactory requestFactory,
                                     RequestHandler handler) {
      super(clusterId);
      m_resourceInstance = resourceInstance;
      m_clusterId = clusterId;
      m_requestFactory = requestFactory;
      m_requestHandler = handler;
    }

    @Override
    ResourceInstance createConfigurationResource(String clusterName) {
      assertEquals(m_clusterId, clusterName);
      return m_resourceInstance;
    }

    @Override
    RequestFactory getRequestFactory() {
      return m_requestFactory;
    }

    @Override
    RequestHandler getRequestHandler(Request.Type requestType) {
      return m_requestHandler;
    }    
  }

}
