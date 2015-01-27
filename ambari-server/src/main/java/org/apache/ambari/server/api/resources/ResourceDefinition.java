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

import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.api.util.TreeNode;

import org.apache.ambari.server.api.services.ResultPostProcessor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Resource Definition.
 * Provides information specific to a specific resource type.
 */
public interface ResourceDefinition {
  /**
   * Obtain the plural name of the resource.
   *
   * @return the plural name of the resource
   */
  public String getPluralName();

  /**
   * Obtain the singular name of the resource.
   *
   * @return the singular name of the resource
   */
  public String getSingularName();

  /**
   * Obtain the type of resource.  Is one of {@link Resource.Type}.
   *
   * @return the type of resource
   */
  public Resource.Type getType();

  /**
   * Obtain a set of all child resource types.
   *
   * @return set of sub-resource definitions
   */
  public Set<SubResourceDefinition> getSubResourceDefinitions();

  /**
   * Obtain any resource post processors.  A resource processor is used to provide resource specific processing of
   * results and is called by the {@link ResultPostProcessor} while post processing a result.
   *
   * @return list of resource specific result processors
   */
  public List<PostProcessor> getPostProcessors();

  /**
   * Obtain the associated renderer based on name.
   *
   * @param name  name of the renderer to obtain
   *
   * @return associated renderer instance
   * @throws IllegalArgumentException if name is invalid for this resource
   */
  public Renderer getRenderer(String name) throws IllegalArgumentException;

  /**
   * Obtain the set of create directives for the resource.  A create directive is
   * information that can be provided in the query string of a POST operation for
   * the resource.  These directives are not predicates but are put into the
   * map of request info properties used by the resource provider when creating
   * the resource.
   */
  public Collection<String> getCreateDirectives();

  /**
   * Defines if resource is actually created on the server side during POST
   * operation.
   * 
   * @see <a
   *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5">
   *      http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5</a>
   * @return {@code true} if resource is creatable, {@code false} otherwise
   */
  public boolean isCreatable();

  /**
   * Resource specific result processor.
   * Used to provide resource specific processing of a result.
   */
  public interface PostProcessor {
    public void process(Request request, TreeNode<Resource> resultNode, String href);
  }

  /**
   * Retrieves directives from the URI
   */
  public Collection<String> getUpdateDirectives();
}
