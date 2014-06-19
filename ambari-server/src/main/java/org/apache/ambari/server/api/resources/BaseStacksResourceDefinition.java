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

package org.apache.ambari.server.api.resources;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.Resource;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for all stacks related resource definitions.
 * Because we cloned /stacks from /stacks2 and changed the endpoint names in stacks,
 * this class is a TEMPORARY mechanism to deviate from the standard /stacks2 endpoint names.
 *
 */
public abstract class BaseStacksResourceDefinition extends BaseResourceDefinition {
  protected BaseStacksResourceDefinition(Resource.Type resourceType) {
    super(resourceType);
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    return Collections.<PostProcessor>singletonList(new StacksHrefProcessor());
  }

  private class StacksHrefProcessor extends BaseHrefPostProcessor {
    @Override
    /**
     * If processing a /stacks endpoint, replace the endpoint names.
     */
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      super.process(request, resultNode, href);

      String nodeHref = resultNode.getProperty("href");
      if (nodeHref != null && nodeHref.contains("/stacks/")) {
        nodeHref = nodeHref.replace("stackServices", "services");
        nodeHref = nodeHref.replace("serviceComponents", "components");
        nodeHref = nodeHref.replace("operatingSystems", "operating_systems");

        // The UI currently expects the old sub-resource names so don't do replacement
        // if the href contains "_=" as only the UI uses this syntax.
        if (! href.contains("_=")) {
          renameChildren(resultNode);
        }
      }

      resultNode.setProperty("href", nodeHref);
    }

    /**
     * Rename child nodes.
     *
     * @param resultNode result node
     */
    private void renameChildren(TreeNode<Resource> resultNode) {
      TreeNode<Resource> childNode = resultNode.removeChild("stackServices");
      if (childNode != null) {
        childNode.setName("services");
        resultNode.addChild(childNode);
        renameChildren(childNode);
      }

      childNode = resultNode.removeChild("serviceComponents");
      if (childNode != null) {
        childNode.setName("components");
        resultNode.addChild(childNode);
        renameChildren(childNode);
      }

      childNode = resultNode.removeChild("operatingSystems");
      if (childNode != null) {
        childNode.setName("operating_systems");
        resultNode.addChild(childNode);
        renameChildren(childNode);
      }
    }
  }
}
