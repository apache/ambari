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

public class HostRenderer extends DefaultRenderer {

  // A list of host properties to be summarized
  private final String OPERATING_SYSTEMS = "operating systems";
  // Renderer name: Host or Summary
  private String name = "Host";

  public HostRenderer() {
    super();
  }

  public HostRenderer(String name) {
    this.name = StringUtils.isBlank(name) ? this.name : name;
  }

  @Override
  public Result finalizeResult(Result queryResult) {
    // Host still uses defaultRenderer
    if (name.equals("Host")) {
      return super.finalizeResult(queryResult);
    }
    // Summary needs aggregation
    TreeNode<Resource> resultTree = queryResult.getResultTree();
    // iterate over all returned flattened hosts and build the summary info
    List<Object> summary = new ArrayList<>();
    Map<String, Map<String, Integer>> osSummary = new HashMap<>();
    osSummary.put(OPERATING_SYSTEMS, new HashMap<String, Integer>());
    for (TreeNode<Resource> node : resultTree.getChildren()) {
      Resource resource = node.getObject();
      String osType = (String) resource.getPropertyValue(HostResourceProvider.HOST_OS_TYPE_PROPERTY_ID);
      if (StringUtils.isNotBlank(osType)) {
        Map<String, Integer> os = osSummary.get(OPERATING_SYSTEMS);
        os.put(osType, os.getOrDefault(osType, 0) + 1);
      }
    }
    if (!osSummary.isEmpty()) {
      summary.add(osSummary);
    }

    Result result = new ResultImpl(true);
    TreeNode<Resource> summaryResultTree = result.getResultTree();
    Resource resource = new ResourceImpl(Resource.Type.Host);
    TreeNode<Resource> summaryTree = result.getResultTree();
    summaryTree.addChild(resource, "summary");
    resource.setProperty("summary", summary);
    return result;
  }
}
