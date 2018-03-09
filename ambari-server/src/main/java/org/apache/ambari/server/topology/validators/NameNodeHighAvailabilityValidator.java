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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that dfs_ha_initial_namenode_active and dfs_ha_initial_namenode_standby properties
 * in hadoop-env reference host groups with NAMENODE components.
 */
public class NameNodeHighAvailabilityValidator implements TopologyValidator {

  private static final Logger LOG = LoggerFactory.getLogger(NameNodeHighAvailabilityValidator.class);

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    Blueprint blueprint = topology.getBlueprint();

    Map<String, Map<String, String>> clusterConfigurations = topology.getConfiguration().getProperties();

    if (!BlueprintConfigurationProcessor.isNameNodeHAEnabled(clusterConfigurations)) {
      LOG.info("NAMENODE HA is not enabled, skipping validation of {}", blueprint.getName());
      return;
    }

    LOG.info("Validating NAMENODE HA for blueprint: {}", blueprint.getName());

    List<String> hostGroupsForComponent = new ArrayList<>(topology.getHostGroupsForComponent("NAMENODE"));

    for (HostGroup hostGroup : topology.getBlueprint().getHostGroups().values()) {
      Map<String, Map<String, String>> operationalConfiguration = new HashMap<>(clusterConfigurations);

      operationalConfiguration.putAll(hostGroup.getConfiguration().getProperties());
      if (hostGroup.getComponentNames().contains("NAMENODE")) {
        Map<String, String> hadoopEnvConfig = operationalConfiguration.get("hadoop-env");
        if(hadoopEnvConfig != null && !hadoopEnvConfig.isEmpty() && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_active") && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_standby")) {
          Set<String> givenHostGroups = new HashSet<>();
          givenHostGroups.add(hadoopEnvConfig.get("dfs_ha_initial_namenode_active"));
          givenHostGroups.add(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby"));
          if(givenHostGroups.size() != hostGroupsForComponent.size()) {
             throw new IllegalArgumentException("NAMENODE HA host groups mapped incorrectly for properties 'dfs_ha_initial_namenode_active' and 'dfs_ha_initial_namenode_standby'. Expected Host groups are :" + hostGroupsForComponent);
          }
          if (BlueprintConfigurationProcessor.HOST_GROUP_PLACEHOLDER_PATTERN.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_active")).matches() && BlueprintConfigurationProcessor.HOST_GROUP_PLACEHOLDER_PATTERN.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby")).matches()) {
            for (String hostGroupForComponent : hostGroupsForComponent) {
              givenHostGroups.removeIf(s -> s.contains(hostGroupForComponent));
            }
          }

          if(!givenHostGroups.isEmpty()){
            throw new IllegalArgumentException("NAMENODE HA host groups mapped incorrectly for properties 'dfs_ha_initial_namenode_active' and 'dfs_ha_initial_namenode_standby'. Expected Host groups are :" + hostGroupsForComponent);
          }
        }
      }
    }
  }

}
