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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Resource;

import java.util.*;

/**
 * Handler for creates that are applied to the results of a query.
 */
public class QueryCreateHandler extends BaseManagementHandler {

  @Override
  public Result handleRequest(Request request) {
    ResourceInstance resource = request.getResource();
    Query query = resource.getQuery();
    query.setUserPredicate(request.getQueryPredicate());

    Result queryResult;
    try {
      //todo: only care about primary key
      queryResult = query.execute();
    } catch (AmbariException e) {
      //TODO: exceptions
      throw new RuntimeException("An exception occurred executing the query portion of the request: " + e, e);
    }

    Resource.Type createType = getCreateType(request.getHttpBody(), resource);
    Set<Map<String, Object>> setProperties = buildCreateSet(request, queryResult, createType);
    ResourceInstance createResource = getResourceFactory().createResource(
        createType, request.getResource().getIds());

    return super.handleRequest(request.getPersistenceManager(),
        createResource, setProperties, request.getURI());
  }

  private Set<Map<String, Object>> buildCreateSet(Request request, Result queryResult, Resource.Type createType) {
    Set<Map<String, Object>> setRequestProps = request.getHttpBodyProperties();
    Set<Map<String, Object>> setCreateProps = new HashSet<Map<String, Object>>(setRequestProps.size());

    ResourceInstance  resource            = request.getResource();
    Resource.Type     type                = resource.getResourceDefinition().getType();
    ClusterController controller          = getClusterController();
    String            resourceKeyProperty = controller.getSchema(type).getKeyPropertyId(type);
    String            createKeyProperty   = controller.getSchema(createType).getKeyPropertyId(type);

    TreeNode<Resource> tree = queryResult.getResultTree();
    Collection<TreeNode<Resource>> treeChildren = tree.getChildren();
    for (TreeNode<Resource> node : treeChildren) {
      Resource r = node.getObject();
      Object keyVal = r.getPropertyValue(resourceKeyProperty);

      for (Map<String, Object> mapProps : setRequestProps) {
        Map<String, Object> mapResourceProps = new HashMap<String, Object>(mapProps);
        mapResourceProps.put(createKeyProperty, keyVal);
        setCreateProps.add(mapResourceProps);
      }
    }
    return setCreateProps;
  }

  private Resource.Type getCreateType(String requestBody, ResourceInstance resource) {
    int startIdx = requestBody.indexOf("\"") + 1;
    int endIdx = requestBody.indexOf("\"", startIdx + 1);

    ResourceInstance res =  resource.getSubResources().get(requestBody.substring(startIdx, endIdx));
    return res == null ? null : res.getResourceDefinition().getType();
  }
}
