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

package org.apache.ambari.server.api.handlers;

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.*;
import org.apache.ambari.server.api.services.persistence.PersistenceManager;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.view.ViewRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Unit tests for CreateHandler.
 */
public class CreateHandlerTest {

  @Before
  public void before() {
    AmbariEventPublisher publisher = createNiceMock(AmbariEventPublisher.class);
    replay(publisher);
    ViewRegistry.initInstance(new ViewRegistry(publisher));
  }

  @Test
  public void testHandleRequest__Synchronous_NoPropsInBody() throws Exception {
    Request request = createNiceMock(Request.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Query query = createStrictMock(Query.class);
    PersistenceManager pm = createStrictMock(PersistenceManager.class);
    RequestStatus status = createNiceMock(RequestStatus.class);
    Resource resource1 = createNiceMock(Resource.class);
    Resource resource2 = createNiceMock(Resource.class);
    Renderer renderer = new DefaultRenderer();

    Set<Resource> setResources = new HashSet<Resource>();
    setResources.add(resource1);
    setResources.add(resource2);

    // expectations
    expect(request.getResource()).andReturn(resource).atLeastOnce();
    expect(request.getQueryPredicate()).andReturn(null).atLeastOnce();
    expect(request.getRenderer()).andReturn(renderer);
    expect(request.getBody()).andReturn(body);

    expect(resource.getQuery()).andReturn(query);
    query.setRenderer(renderer);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.isCreatable()).andReturn(true).anyTimes();

    expect(pm.create(resource, body)).andReturn(status);
    expect(status.getStatus()).andReturn(RequestStatus.Status.Complete);
    expect(status.getAssociatedResources()).andReturn(setResources);
    expect(resource1.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(resource2.getType()).andReturn(Resource.Type.Cluster).anyTimes();

    replay(request, body, resource, resourceDefinition, query, pm, status, resource1, resource2);

    Result result = new TestCreateHandler(pm).handleRequest(request);

    assertNotNull(result);
    TreeNode<Resource> tree = result.getResultTree();
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> resourcesNode = tree.getChild("resources");
    assertEquals(2, resourcesNode.getChildren().size());
    boolean foundResource1 = false;
    boolean foundResource2 = false;
    for(TreeNode<Resource> child : resourcesNode.getChildren()) {
      Resource r = child.getObject();
      if (r == resource1 && ! foundResource1) {
        foundResource1 = true;
      } else if (r == resource2 && ! foundResource2) {
        foundResource2 = true;
      } else {
        fail();
      }
    }

    assertEquals(ResultStatus.STATUS.CREATED, result.getStatus().getStatus());
    verify(request, body, resource, resourceDefinition, query, pm, status, resource1, resource2);
  }

  @Test
  public void testHandleRequest__Synchronous() throws Exception {
    Request request = createNiceMock(Request.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Query query = createStrictMock(Query.class);
    PersistenceManager pm = createStrictMock(PersistenceManager.class);
    RequestStatus status = createNiceMock(RequestStatus.class);
    Resource resource1 = createNiceMock(Resource.class);
    Resource resource2 = createNiceMock(Resource.class);
    Renderer renderer = new DefaultRenderer();

    Set<Resource> setResources = new HashSet<Resource>();
    setResources.add(resource1);
    setResources.add(resource2);

    // expectations
    expect(request.getResource()).andReturn(resource).atLeastOnce();
    expect(request.getQueryPredicate()).andReturn(null).atLeastOnce();
    expect(request.getRenderer()).andReturn(renderer);
    expect(request.getBody()).andReturn(body).atLeastOnce();

    expect(resource.getQuery()).andReturn(query);
    query.setRenderer(renderer);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.isCreatable()).andReturn(true).anyTimes();

    expect(pm.create(resource, body)).andReturn(status);
    expect(status.getStatus()).andReturn(RequestStatus.Status.Complete);
    expect(status.getAssociatedResources()).andReturn(setResources);
    expect(resource1.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(resource2.getType()).andReturn(Resource.Type.Cluster).anyTimes();

    replay(request, body, resource, resourceDefinition, query, pm, status, resource1, resource2);

    Result result = new TestCreateHandler(pm).handleRequest(request);

    assertNotNull(result);
    TreeNode<Resource> tree = result.getResultTree();
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> resourcesNode = tree.getChild("resources");
    assertEquals(2, resourcesNode.getChildren().size());
    boolean foundResource1 = false;
    boolean foundResource2 = false;
    for(TreeNode<Resource> child : resourcesNode.getChildren()) {
      Resource r = child.getObject();
      if (r == resource1 && ! foundResource1) {
        foundResource1 = true;
      } else if (r == resource2 && ! foundResource2) {
        foundResource2 = true;
      } else {
        fail();
      }
    }
    assertEquals(ResultStatus.STATUS.CREATED, result.getStatus().getStatus());
    verify(request, body, resource, resourceDefinition, query, pm, status, resource1, resource2);
  }

  @Test
  public void testHandleRequest__Asynchronous() throws Exception {
    Request request = createNiceMock(Request.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Query query = createStrictMock(Query.class);
    PersistenceManager pm = createStrictMock(PersistenceManager.class);
    RequestStatus status = createNiceMock(RequestStatus.class);
    Resource resource1 = createNiceMock(Resource.class);
    Resource resource2 = createNiceMock(Resource.class);
    Resource requestResource = createNiceMock(Resource.class);
    Renderer renderer = new DefaultRenderer();

    Set<Resource> setResources = new HashSet<Resource>();
    setResources.add(resource1);
    setResources.add(resource2);

    // expectations
    expect(request.getResource()).andReturn(resource).atLeastOnce();
    expect(request.getBody()).andReturn(body).atLeastOnce();
    expect(request.getQueryPredicate()).andReturn(null).atLeastOnce();
    expect(request.getRenderer()).andReturn(renderer);

    expect(resource.getQuery()).andReturn(query);
    query.setRenderer(renderer);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.isCreatable()).andReturn(true).anyTimes();

    expect(pm.create(resource, body)).andReturn(status);
    expect(status.getStatus()).andReturn(RequestStatus.Status.Accepted);
    expect(status.getAssociatedResources()).andReturn(setResources);
    expect(resource1.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(resource2.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(status.getRequestResource()).andReturn(requestResource).anyTimes();

    replay(request, body, resource, resourceDefinition, query, pm, status, resource1, resource2,
        requestResource);

    Result result = new TestCreateHandler(pm).handleRequest(request);

    assertNotNull(result);
    TreeNode<Resource> tree = result.getResultTree();
    assertEquals(2, tree.getChildren().size());
    TreeNode<Resource> resourcesNode = tree.getChild("resources");
    assertEquals(2, resourcesNode.getChildren().size());
    boolean foundResource1 = false;
    boolean foundResource2 = false;
    for(TreeNode<Resource> child : resourcesNode.getChildren()) {
      Resource r = child.getObject();
      if (r == resource1 && ! foundResource1) {
        foundResource1 = true;
      } else if (r == resource2 && ! foundResource2) {
        foundResource2 = true;
      } else {
        fail();
      }
    }

    TreeNode<Resource> statusNode = tree.getChild("request");
    assertNotNull(statusNode);
    assertEquals(0, statusNode.getChildren().size());
    assertSame(requestResource, statusNode.getObject());

    assertEquals(ResultStatus.STATUS.ACCEPTED, result.getStatus().getStatus());
    verify(request, body, resource, resourceDefinition, query, pm, status, resource1, resource2,
        requestResource);
  }

  private class TestCreateHandler extends CreateHandler {
    private PersistenceManager m_testPm;

    private TestCreateHandler(PersistenceManager pm) {
      m_testPm = pm;
    }

    @Override
    protected PersistenceManager getPersistenceManager() {
      return m_testPm;
    }
  }
}
