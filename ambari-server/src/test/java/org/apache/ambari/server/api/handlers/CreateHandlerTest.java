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

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.services.PersistenceManager;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Unit tests for CreateHandler.
 */
public class CreateHandlerTest {

  @Test
  public void testHandleRequest__Synchronous() {
    Request request = createMock(Request.class);
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    PersistenceManager pm = createStrictMock(PersistenceManager.class);
    RequestStatus status = createMock(RequestStatus.class);
    Resource resource1 = createMock(Resource.class);
    Resource resource2 = createMock(Resource.class);

    Map<PropertyId, String> resourceProperties = new HashMap<PropertyId, String>();

    Set<Resource> setResources = new HashSet<Resource>();
    setResources.add(resource1);
    setResources.add(resource2);

    // expectations
    expect(request.getResourceDefinition()).andReturn(resource);
    expect(request.getHttpBodyProperties()).andReturn(resourceProperties);
    resource.setProperties(resourceProperties);
    expect(request.getPersistenceManager()).andReturn(pm);
    expect(pm.persist(resource)).andReturn(status);
    expect(status.getStatus()).andReturn(RequestStatus.Status.Complete);
    expect(status.getAssociatedResources()).andReturn(setResources);
    expect(resource1.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(resource2.getType()).andReturn(Resource.Type.Cluster).anyTimes();

    replay(request, resource, pm, status, resource1, resource2);

    Result result = new CreateHandler().handleRequest(request);

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

    verify(request, resource, pm, status, resource1, resource2);
  }

  @Test
  public void testHandleRequest__Asynchronous() {
    Request request = createMock(Request.class);
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    PersistenceManager pm = createStrictMock(PersistenceManager.class);
    RequestStatus status = createMock(RequestStatus.class);
    Resource resource1 = createMock(Resource.class);
    Resource resource2 = createMock(Resource.class);
    Resource requestResource = createMock(Resource.class);

    Map<PropertyId, String> resourceProperties = new HashMap<PropertyId, String>();

    Set<Resource> setResources = new HashSet<Resource>();
    setResources.add(resource1);
    setResources.add(resource2);

    // expectations
    expect(request.getResourceDefinition()).andReturn(resource);
    expect(request.getHttpBodyProperties()).andReturn(resourceProperties);
    resource.setProperties(resourceProperties);
    expect(request.getPersistenceManager()).andReturn(pm);
    expect(pm.persist(resource)).andReturn(status);
    expect(status.getStatus()).andReturn(RequestStatus.Status.Accepted);
    expect(status.getAssociatedResources()).andReturn(setResources);
    expect(resource1.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(resource2.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(status.getRequestResource()).andReturn(requestResource);
    expect(request.getURI()).andReturn("http://some.host.com:8080/clusters/cluster1/services/HDFS").atLeastOnce();
    expect(status.getRequestResource()).andReturn(requestResource).atLeastOnce();
    expect(requestResource.getPropertyValue(PropertyHelper.getPropertyId("id", "Requests"))).andReturn("requestID").atLeastOnce();

    replay(request, resource, pm, status, resource1, resource2, requestResource);

    Result result = new CreateHandler().handleRequest(request);

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
    assertEquals("http://some.host.com:8080/clusters/cluster1/requests/requestID", statusNode.getProperty("href"));


    verify(request, resource, pm, status, resource1, resource2, requestResource);
  }
}
