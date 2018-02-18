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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.lang.StringUtils;

/**
 * HostSummaryRenderere
 *
 * This renderer is to summarize the properties of all hosts within a cluster and returns
 * a list of proerpties summary information: i.e
 *
 * summary:[
 *  {
 *    "operating_systems" : {
 *      "centos6" : 5,
 *      "centos7" : 10
 *    }
 *  }
 * ]
 *
 */
public class HostSummaryRenderer extends DefaultRenderer {

  // A list of host properties to be summarized
  enum HostSummaryProperties {
    OPERATINGSYSTEMS("operating_systems");

    private final String property;
    HostSummaryProperties(String property) {
      this.property = property;
    }
    public String getProperty() {
      return property;
    }
  }

  @Override
  public Result finalizeResult(Result queryResult) {
    TreeNode<Resource> queryResultTree = queryResult.getResultTree();
    // Iterate over all returned flattened hosts and build the summary info
    List<Object> summary = new ArrayList<>();
    // Build all summary info
    buildFinalizedSummary(queryResultTree, summary);
    // Create finalized result
    return buildFinalizedResult(summary);
  }

  private void buildFinalizedSummary(TreeNode<Resource> queryResultTree, List<Object> summary) {
    // Build osSummary info at this time
    Map<String, Map<String, Integer>> osSummary = new HashMap<>();
    summary.add(osSummary);
    Map<String, Integer> osTypeCount = new HashMap<>();
    osSummary.put(HostSummaryProperties.OPERATINGSYSTEMS.getProperty(), osTypeCount);
    for (TreeNode<Resource> node : queryResultTree.getChildren()) {
      Resource resource = node.getObject();
      String osType = (String) resource.getPropertyValue(HostResourceProvider.HOST_OS_TYPE_PROPERTY_ID);
      if (StringUtils.isNotBlank(osType)) {
        osTypeCount.put(osType, osTypeCount.getOrDefault(osTypeCount, 0) + 1);
      }
    }
  }

  private Result buildFinalizedResult(List<Object> summary) {
    Result result = new ResultImpl(true);
    Resource resource = new ResourceImpl(Resource.Type.Host);
    TreeNode<Resource> summaryTree = result.getResultTree();
    summaryTree.addChild(resource, HostResourceProvider.SUMMARY_PROPERTY_ID);
    resource.setProperty(HostResourceProvider.SUMMARY_PROPERTY_ID, summary);
    return result;
  }
}
