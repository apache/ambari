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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Unit tests for ClusterService.
 */
public class ClusterServiceTest extends BaseServiceTest {


  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<ServiceTestInvocation>();

    //getCluster
    ClusterService clusterService = new TestClusterService("clusterName");
    Method m = clusterService.getClass().getMethod("getCluster", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, clusterService, m, args, null));

    //getClusters
    clusterService = new TestClusterService(null);
    m = clusterService.getClass().getMethod("getClusters", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, clusterService, m, args, null));

    //createCluster
    clusterService = new TestClusterService("clusterName");
    m = clusterService.getClass().getMethod("createCluster", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, clusterService, m, args, "body"));

    //createCluster
    clusterService = new TestClusterService("clusterName");
    m = clusterService.getClass().getMethod("updateCluster", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, clusterService, m, args, "body"));

    //deleteCluster
    clusterService = new TestClusterService("clusterName");
    m = clusterService.getClass().getMethod("deleteCluster", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, clusterService, m, args, null));

    return listInvocations;
  }


  private class TestClusterService extends ClusterService {
    private String m_clusterId;

    private TestClusterService(String clusterId) {
      m_clusterId = clusterId;
    }

    @Override
    ResourceInstance createClusterResource(String clusterName) {
      assertEquals(m_clusterId, clusterName);
      return getTestResource();
    }

    @Override
    RequestFactory getRequestFactory() {
      return getTestRequestFactory();
    }

    @Override
    protected RequestBodyParser getBodyParser() {
      return getTestBodyParser();
    }

    @Override
    protected ResultSerializer getResultSerializer() {
      return getTestResultSerializer();
    }
  }

  //todo: test getHostHandler, getServiceHandler, getHostComponentHandler
}
