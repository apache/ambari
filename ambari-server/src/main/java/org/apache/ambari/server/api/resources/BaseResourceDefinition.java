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


import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.MinimalRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base resource definition.  Contains behavior common to all resource types.
 */
public abstract class BaseResourceDefinition implements ResourceDefinition {

  /**
   * Resource type.  One of {@link Resource.Type}
   */
  private Resource.Type m_type;


  /**
   * Constructor.
   *
   * @param resourceType resource type
   */
  public BaseResourceDefinition(Resource.Type resourceType) {
    m_type = resourceType;
  }

  @Override
  public Resource.Type getType() {
    return m_type;
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    return Collections.emptySet();
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = new ArrayList<PostProcessor>();
    listProcessors.add(new BaseHrefPostProcessor());
    return listProcessors;
  }

  @Override
  public Renderer getRenderer(String name) {
    if (name == null || name.equals("default")) {
      return new DefaultRenderer();
    } else if (name.equals("minimal")) {
      return new MinimalRenderer();
    } else {
      throw new IllegalArgumentException("Invalid renderer name: " + name +
          " for resource of type: " + m_type);
    }
  }

  ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }

  @Override
  public Collection<String> getCreateDirectives() {
    // return a collection which can be modified by sub resources
    return new HashSet<String>();
  }

  @Override
  public boolean equals(Object o) {
    boolean result =false;
    if(this == o) result = true;
    if(o instanceof BaseResourceDefinition){
        BaseResourceDefinition other = (BaseResourceDefinition) o;
        if(m_type == other.m_type )
            result = true;
    }
    return result;
  }

  @Override
  public int hashCode() {
    return m_type.hashCode();
  }

  @Override
  public boolean isCreatable() {
    // by default all resources are creatable
    return true;
  }

  class BaseHrefPostProcessor implements PostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      Resource r = resultNode.getObject();
      TreeNode<Resource> parent = resultNode.getParent();

      if (parent.getName() != null) {

        int i = href.indexOf("?");
        if (i != -1) {
          href = href.substring(0, i);
        }

        if (!href.endsWith("/")) {
          href = href + '/';
        }

        Schema schema = getClusterController().getSchema(r.getType());
        Object id     = r.getPropertyValue(schema.getKeyPropertyId(r.getType()));

        href = parent.getProperty("isCollection").equals("true") ?
            href + id : href + parent.getName() + '/' + id;
      }
      resultNode.setProperty("href", href);
    }
  }
}
