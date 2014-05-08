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

import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 * BaseStacksResourceDefinition unit tests
 */
public class BaseStacksResourceDefinitionTest {

  @Test
  public void testHrefCreation() {
    TestResourceDefinition resourceDefinition = new TestResourceDefinition();
    ResourceDefinition.PostProcessor processor = resourceDefinition.getPostProcessors().iterator().next();

    TreeNode<Resource> node = new TreeNodeImpl<Resource>(new TreeNodeImpl<Resource>(null, null, null), null, "test");

    String href = "/stacks/HDP/versions/1.3.2/stackServices/foo";
    processor.process(null, node, href);
    assertEquals("/stacks/HDP/versions/1.3.2/services/foo", node.getProperty("href"));

    href = "/stacks2/HDP/versions/1.3.2/stackServices/foo";
    processor.process(null, node, href);
    assertEquals("/stacks2/HDP/versions/1.3.2/stackServices/foo", node.getProperty("href"));

    href = "/stacks/HDP/versions/1.3.2/stackServices/foo/serviceComponents";
    processor.process(null, node, href);
    assertEquals("/stacks/HDP/versions/1.3.2/services/foo/components", node.getProperty("href"));

    href = "/stacks2/HDP/versions/1.3.2/stackServices/foo/serviceComponents";
    processor.process(null, node, href);
    assertEquals("/stacks2/HDP/versions/1.3.2/stackServices/foo/serviceComponents", node.getProperty("href"));

    href = "/stacks/HDP/versions/1.3.2/operatingSystems";
    processor.process(null, node, href);
    assertEquals("/stacks/HDP/versions/1.3.2/operating_systems", node.getProperty("href"));

    href = "/stacks2/HDP/versions/1.3.2/operatingSystems";
    processor.process(null, node, href);
    assertEquals("/stacks2/HDP/versions/1.3.2/operatingSystems", node.getProperty("href"));
  }

  private class TestResourceDefinition extends BaseStacksResourceDefinition {
    private TestResourceDefinition() {
      super(Resource.Type.StackServiceComponent);
    }

    @Override
    public String getPluralName() {
      return "tests";
    }

    @Override
    public String getSingularName() {
      return "test";
    }
  }
}

