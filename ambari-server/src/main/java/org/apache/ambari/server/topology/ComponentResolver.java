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
package org.apache.ambari.server.topology;

import java.util.Map;
import java.util.Set;

/**
 * Resolves all incompletely specified host group components in the topology:
 * finds stack and/or service type that each component is defined in.
 */
public interface ComponentResolver {

  /**
   * @return the set resolved components for each host group (the map's keys are host group names)
   * @throws IllegalArgumentException if the components cannot be unambiguously resolved
   * (eg. if some component is not known, or if there are multiple component with the same name and
   * the request does not specify which one to select)
   */
  Map<String, Set<ResolvedComponent>> resolveComponents(BlueprintBasedClusterProvisionRequest request);

}
