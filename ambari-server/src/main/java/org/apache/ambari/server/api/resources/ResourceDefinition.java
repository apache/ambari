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

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.api.util.TreeNode;

import org.apache.ambari.server.api.services.ResultPostProcessor;

import java.util.List;
import java.util.Map;

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
   * Obtain the value of the primary id of the resource.
   *
   * @return the value of the primary id of the resource
   */
  public String getId();

  /**
   * Obtain the type of resource.  Is one of {@link Resource.Type}.
   *
   * @return the type of resource
   */
  public Resource.Type getType();

  /**
   * Set the values of the parent foreign keys.
   *
   * @param mapIds  map of all parent foreign keys. Map from resource type to id value.
   */
  public void setParentIds(Map<Resource.Type, String> mapIds);

  /**
   * Obtain the primary and foreign key properties for the resource.
   *
   * @return map of primary and foreign key values keyed by resource type
   */
  public Map<Resource.Type, String> getResourceIds();

  /**
   * Obtain sub-resources of this resource.  A sub-resource is a resource that is contained in
   * another parent resource.
   *
   * @return map of sub resource definitions keyed by resource name
   */
  public Map<String, ResourceDefinition> getSubResources();

  /**
   * Return the query associated with the resource.
   * Each resource has one query.
   *
   * @return the associated query
   */
  public Query getQuery();

  /**
   * Obtain any resource post processors.  A resource processor is used to provide resource specific processing of
   * results and is called by the {@link ResultPostProcessor} while post processing a result.
   *
   * @return list of resource specific result processors
   */
  public List<PostProcessor> getPostProcessors();

  /**
   * Resource specific result processor.
   * Used to provide resource specific processing of a result.
   */
  public interface PostProcessor {
    public void process(Request request, TreeNode<Resource> resultNode, String href);
  }
}
