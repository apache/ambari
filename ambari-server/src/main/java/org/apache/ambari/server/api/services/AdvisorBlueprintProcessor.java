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

package org.apache.ambari.server.api.services;

import java.util.Map;

import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.topology.ClusterTopology;

/**
 * Common interface for topology/configuration recommendation engines. Currently there is a legacy implementation for
 * stack advisor and a new implementation for mpack advisor.
 * <p>See:
 * {@link org.apache.ambari.server.api.services.stackadvisor.StackAdvisorBlueprintProcessor}
 * {@link org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorBlueprintProcessor}
 * </p>
 */
public interface AdvisorBlueprintProcessor {

  static final String RECOMMENDATION_FAILED = "Configuration recommendation failed.";
  static final String INVALID_RESPONSE = "Configuration recommendation returned with invalid response.";

  /**
   * Recommend configurations by the advisor, then store the results in cluster topology.
   * @param clusterTopology cluster topology instance
   * @param userProvidedConfigurations User configurations of cluster provided in Blueprint + Cluster template
   */
  void adviseConfiguration(ClusterTopology clusterTopology, Map<String, Map<String, String>> userProvidedConfigurations) throws ConfigurationTopologyException;

}
