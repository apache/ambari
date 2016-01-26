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

import java.util.List;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.internal.RepositoryVersionResourceProvider;
import org.apache.ambari.server.controller.internal.VersionDefinitionResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.Lists;

/**
 * The Resource Definition used for Version Definition files.
 */
public class VersionDefinitionResourceDefinition extends BaseResourceDefinition {
  private static final String STACKS_NAME = new StackResourceDefinition().getPluralName();
  private static final String STACK_VERSIONS_NAME = new StackVersionResourceDefinition().getPluralName();
  private static final String REPO_VERSIONS_NAME = new RepositoryVersionResourceDefinition().getPluralName();

  private static final String HREF_TEMPLATE =
      STACKS_NAME + "/%s/" + STACK_VERSIONS_NAME + "/%s/" + REPO_VERSIONS_NAME;


  public VersionDefinitionResourceDefinition() {
    super(Resource.Type.VersionDefinition);
  }

  @Override
  public String getPluralName() {
    return "version_definitions";
  }

  @Override
  public String getSingularName() {
    return "version_definition";
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> list = Lists.newArrayList();

    list.add(new HrefPostProcessor());

    return list;
  }


  class HrefPostProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      super.process(request, resultNode, href);

      Object stackNameObj = resultNode.getObject().getPropertyValue(
          VersionDefinitionResourceProvider.VERSION_DEF_STACK_NAME);
      Object stackVersionObj = resultNode.getObject().getPropertyValue(
          VersionDefinitionResourceProvider.VERSION_DEF_STACK_VERSION);

      if (resultNode.getObject().getType() == Resource.Type.VersionDefinition &&
          null != stackNameObj && null != stackVersionObj &&
          null != resultNode.getProperty("href")) {

        String oldHref = resultNode.getProperty("href").toString();

        String newPath = String.format(HREF_TEMPLATE, stackNameObj, stackVersionObj);

        String newHref = oldHref.replace(getPluralName(), newPath);
        newHref = newHref.replace(VersionDefinitionResourceProvider.VERSION_DEF,
            RepositoryVersionResourceProvider.REPOSITORY_VERSION);

        resultNode.setProperty("href", newHref);
      }
    }
  }

}
