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

import org.apache.ambari.server.controller.spi.Resource;

import java.util.Collections;
import java.util.Map;


/**
 * Task resource definition.
 */
public class TaskResourceDefinition extends BaseResourceDefinition {

  /**
   * Value of cluster id foreign key.
   */
  private String m_clusterId;

  /**
   * Value of request id foreign key.
   */
  private String m_requestId;


  /**
   * Constructor.
   *
   * @param id         task id value
   * @param clusterId  cluster id value
   */
  public TaskResourceDefinition(String id, String clusterId, String requestId) {
    super(Resource.Type.Task, id);
    m_clusterId = clusterId;
    m_requestId = requestId;
    setResourceId(Resource.Type.Cluster, m_clusterId);
    setResourceId(Resource.Type.Request, m_requestId);
  }

  @Override
  public String getPluralName() {
    return "tasks";
  }

  @Override
  public String getSingularName() {
    return "task";
  }

  @Override
  public Map<String, ResourceDefinition> getSubResources() {
    return Collections.emptyMap();
  }
}