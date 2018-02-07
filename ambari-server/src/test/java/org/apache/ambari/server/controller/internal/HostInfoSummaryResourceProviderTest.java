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
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.ambari.server.api.handlers.ReadHandler;
import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.predicate.PredicateCompiler;
import org.apache.ambari.server.api.query.render.HostInfoSummaryRenderer;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactory;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.services.GetRequest;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.services.serializers.JsonSerializer;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostInfoSummaryDAO;
import org.apache.ambari.server.orm.dao.HostInfoSummaryDTO;
import org.apache.ambari.server.view.ViewRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class HostInfoSummaryResourceProviderTest {

  private HostInfoSummaryDAO hostInfoSummaryDAO  = null;
  private AmbariEventPublisher publisher;

  @Before
  public void before() {
    SecurityContextHolder.getContext().setAuthentication(null);

    hostInfoSummaryDAO = createStrictMock(HostInfoSummaryDAO.class);
    Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    publisher = createNiceMock(AmbariEventPublisher.class);
  }

  @Test
  public void testGetResourcesWithClusterName() throws Exception {
    String uri = "http://xxx.com/api/v1/clusters/c1/hosts?format=summary";
    testGetResources(uri);
  }

  @Test
  public void testGetResourcesWithoutClusterName() throws Exception {
    String uri = "http://xxx.com/api/v1/hosts?format=summary";
    testGetResources(uri);
  }

  private void testGetResources(String clusterName) throws Exception {
    GetRequest readRequest = createNiceMock(GetRequest.class);
    HostInfoSummaryRenderer renderer = new HostInfoSummaryRenderer();
    expect(readRequest.getRenderer()).andReturn(renderer).anyTimes();
    PredicateCompiler predicateCompiler = new PredicateCompiler();
    Predicate predicate = predicateCompiler.compile("/api/v1/hosts?format=summary");
    expect(readRequest.getQueryPredicate()).andReturn(null).anyTimes();
    ResourceImpl resource = new ResourceImpl(Resource.Type.HostSummary);
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Host, null);
    mapIds.put(Resource.Type.Cluster, "c1");
    ResourceInstanceFactory resourceFactory = new ResourceInstanceFactoryImpl();
    ResourceInstance resourceInstance = resourceFactory.createResource(Resource.Type.HostSummary, mapIds);
    expect(readRequest.getResource()).andReturn(resourceInstance).anyTimes();

    expect(readRequest.getURI()).andReturn(clusterName).anyTimes();
    Map<String, TemporalInfo> fields = new HashMap<>();
    expect(readRequest.getFields()).andReturn(fields).anyTimes();
    List<HostInfoSummaryDTO> queryResults = new ArrayList<>();
    queryResults.add(new HostInfoSummaryDTO("redhat6", 10));
    queryResults.add(new HostInfoSummaryDTO("debian7", 20));
    expect(hostInfoSummaryDAO.findHostInfoSummary("c1")).andReturn(queryResults).anyTimes();
    expect(hostInfoSummaryDAO.findHostInfoSummary(null)).andReturn(queryResults).anyTimes();

    replay(publisher, hostInfoSummaryDAO, readRequest);

    ViewRegistry.initInstance(new ViewRegistry(publisher));
    RequestHandler requestHandler = new ReadHandler();
    Result result = requestHandler.handleRequest(readRequest);

    Assert.assertEquals(ResultStatus.STATUS.OK, result.getStatus().getStatus());

    verify(publisher, hostInfoSummaryDAO, readRequest);
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
     *
     */
    @Override
    public void configure(Binder binder) {
      binder.bind(HostInfoSummaryDAO.class).toInstance(hostInfoSummaryDAO);
    }
  }
}
