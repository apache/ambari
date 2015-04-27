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
//todo: naming
public interface TopologyRequest {

  public String getClusterName();
  //todo: only a single BP may be specified so all host groups have the same bp.
  //todo: There is no reason really that we couldn't allow hostgroups from different blueprints assuming that
  //todo: the stack matches across the groups.  For scaling operations, we allow different blueprints (rather arbitrary)
  //todo: so BP really needs to get associated with the HostGroupInfo, even for create which will have a single BP
  //todo: for all HG's.
  public Blueprint getBlueprint();
  public Configuration getConfiguration();
  public Map<String, HostGroupInfo> getHostGroupInfo();
  public List<TopologyValidator> getTopologyValidators();
}
