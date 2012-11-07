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
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Set;

/**
 * Base handler for operations that persist state to the back-end.
 */
public class BaseManagementHandler implements RequestHandler {
  @Override
  public Result handleRequest(Request request) {
    ResourceDefinition resource = request.getResourceDefinition();
    resource.setProperties(request.getHttpBodyProperties());
    RequestStatus status = request.getPersistenceManager().persist(resource);

    return createResult(request, status);
  }

  private Result createResult(Request request, RequestStatus requestStatus) {
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
      String requestHref = buildRequestHref(request, requestStatus);
      r.setProperty("href", requestHref);
    }

    return result;
  }

  //todo: this needs to be rewritten and needs to support operating on clusters collection
  private String buildRequestHref(Request request, RequestStatus requestStatus) {
    StringBuilder sb = new StringBuilder();
    String origHref = request.getURI();
    String[] toks = origHref.split("/");

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
        PropertyHelper.getPropertyId("id", "Requests"));

    sb.append("requests/").append(requestId);

    return sb.toString();
  }
}
