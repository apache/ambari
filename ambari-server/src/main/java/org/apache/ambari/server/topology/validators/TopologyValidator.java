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

package org.apache.ambari.server.topology.validators;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;

/**
 * Performs topology validation.
 */
public interface TopologyValidator {

  /**
   * Performs some validation on {@code topology}.
   *
   * @return The given topology, or a new updated one if some changes are necessary and can be performed automatically
   *         (eg. {@link DependencyAndCardinalityValidator} may add some auto-deployable components)
   * @throws InvalidTopologyException if validation fails (most validators)
   * @throws IllegalArgumentException if validation fails (some validators)
   */
  ClusterTopology validate(ClusterTopology topology) throws InvalidTopologyException;
}
