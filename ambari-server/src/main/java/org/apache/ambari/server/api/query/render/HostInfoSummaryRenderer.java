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
package org.apache.ambari.server.api.query.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.services.ResultPostProcessorImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.HostInfoSummaryResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.lang.StringUtils;

public class HostInfoSummaryRenderer extends BaseRenderer implements Renderer {

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryTree, boolean isCollection) {

    QueryInfo queryInfo = queryTree.getObject();
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<>(
        null, queryInfo.getProperties(), queryTree.getName());

    copyPropertiesToResult(queryTree, resultTree);

    return resultTree;
  }

  @Override
  public boolean requiresPropertyProviderInput() {
    return false;
  }

  @Override
  public Result finalizeResult(Result queryResult) {
    // Convert fully qualified properties into short property names and flat the queryResult datastructure
    Map<String, Object> summaryMap = new HashMap<>();
    TreeNode<Resource> resultTree = queryResult.getResultTree();
    Collection<TreeNode<Resource>> nodes = resultTree.getChildren();
    if (nodes != null && !nodes.isEmpty()) {
      Resource resource = (Resource)((TreeNode)nodes.iterator().next()).getObject();
      Object o = resource.getPropertyValue(HostInfoSummaryResourceProvider.CLUSTER_NAME);
      if (o != null && o instanceof String &&  StringUtils.isNotBlank((String)o)) {
        summaryMap.put("cluster_name", (String)o);
      }
      o = resource.getPropertiesMap().get(HostInfoSummaryResourceProvider.HOSTS_SUMMARY);
      if (o != null && o instanceof Map<?,?> &&  ((Map<String, Object>)o).size() > 0) {
        summaryMap.putAll((Map<String, Object>)o);
      }
    }

    Resource resultResource = new ResourceImpl(Resource.Type.HostSummary);
    resultResource.setProperty("hosts_summary", summaryMap);
    Result summaryResult = new ResultImpl(true);
    TreeNode<Resource> summaryTree = summaryResult.getResultTree();
    summaryTree.addChild(resultResource, "hosts_summary");

    return summaryResult;
  }

  @Override
  public ResultPostProcessor getResultPostProcessor(Request request) {
    // simply return the native rendering
    return new ResultPostProcessorImpl(request);
  }
}
