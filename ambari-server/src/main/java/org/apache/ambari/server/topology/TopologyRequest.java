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

package org.apache.ambari.server.topology;

import java.util.List;
import java.util.Map;

/**
 * A request which is used to create or modify a cluster topology.
 */
public interface TopologyRequest {
  /**
   * Request types.
   */
  public enum Type { PROVISION, SCALE, EXPORT }

  /**
   * Get the cluster id associated with the request. Can be <code>null</code>.
   *
   * @return associated cluster id
   */
  public Long getClusterId();

  /**
   * Get the request type.
   *
   * @return the type of request
   */
  public Type getType();

  //todo: only a single BP may be specified so all host groups have the same bp.
  //todo: BP really needs to be associated with the HostGroupInfo, even for create which will have a single BP
  //todo: for all HG's.

  /**
   * Get the blueprint instance associated with the request.
   *
   * @return associated blueprint instance
   */
  public Blueprint getBlueprint();

  /**
   * Get the cluster scoped configuration for the request.
   *
   * @return cluster scoped configuration
   */
  public Configuration getConfiguration();

  /**
   * Get host group info.
   *
   * @return map of host group name to group info
   */
  public Map<String, HostGroupInfo> getHostGroupInfo();

  /**
   * Get request topology validators.
   *
   * @return list of topology validators
   */
  public List<TopologyValidator> getTopologyValidators();

  /**
   * Get request description.
   *
   * @return string description of the request
   */
  public String getDescription();
}
