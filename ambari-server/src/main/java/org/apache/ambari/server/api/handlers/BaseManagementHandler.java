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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactory;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.services.PersistenceManager;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Map;
import java.util.Set;

/**
 * Base handler for operations that persist state to the back-end.
 */
public class BaseManagementHandler implements RequestHandler {
  @Override
  public Result handleRequest(Request request) {
    ResourceInstance resource = request.getResource();
    Predicate queryPredicate = request.getQueryPredicate();
    if (queryPredicate != null) {
      resource.getQuery().setUserPredicate(queryPredicate);
    }

    return handleRequest(request.getPersistenceManager(), resource,
        request.getHttpBodyProperties(), request.getURI());
  }

  protected Result handleRequest(PersistenceManager pm, ResourceInstance resource,
                                 Set<Map<String, Object>> setProperties, String uri) {

    return createResult(uri, pm.persist(resource, setProperties));
  }

  private Result createResult(String uri, RequestStatus requestStatus) {
    boolean isSynchronous = requestStatus.getStatus() == RequestStatus.Status.Complete;

    Result result = new ResultImpl(isSynchronous);
    TreeNode<Resource> tree = result.getResultTree();

    Set<Resource> setResources = requestStatus.getAssociatedResources();
    TreeNode<Resource> resourcesNode = null;
    if (! setResources.isEmpty()) {
      resourcesNode = tree.addChild(null, "resources");
    }
    int count = 1;
    for (Resource resource : setResources) {
      //todo: provide a more meaningful node name
      resourcesNode.addChild(resource, resource.getType() + ":" + count++);
    }

    if (! isSynchronous) {
      Resource requestResource = requestStatus.getRequestResource();
      TreeNode<Resource> r = tree.addChild(requestResource, "request");
      r.setProperty("href", buildRequestHref(uri, requestStatus));
    }

    return result;
  }

  private String buildRequestHref(String uri, RequestStatus requestStatus) {
    StringBuilder sb = new StringBuilder();
    String[] toks = uri.split("/");

    for (int i = 0; i < toks.length; ++i) {
      String s = toks[i];
      sb.append(s).append('/');
      if ("clusters".equals(s)) {
        sb.append(toks[i + 1]).append('/');
        break;
      }
    }

    //todo: shouldn't know property name
    Object requestId = requestStatus.getRequestResource().getPropertyValue(
        PropertyHelper.getPropertyId("Requests", "id"));

    sb.append("requests/").append(requestId);

    return sb.toString();
  }

  //todo: How to get reference to factory?
  protected ResourceInstanceFactory getResourceFactory() {
    return new ResourceInstanceFactoryImpl();
  }

  //todo: how to get cluster controller?
  protected ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }
}
