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
package org.apache.ambari.server.topology.tasks;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.ambari.server.events.ClusterConfigFinishedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.security.authorization.internal.RunWithInternalSecurityContext;
import org.apache.ambari.server.topology.ClusterConfigurationRequest;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.TopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ConfigureClusterTask implements Callable<Boolean> {

  private static Logger LOG = LoggerFactory.getLogger(ConfigureClusterTask.class);

  private ClusterConfigurationRequest configRequest;
  private ClusterTopology topology;
  private AmbariEventPublisher ambariEventPublisher;

  @AssistedInject
  public ConfigureClusterTask(@Assisted ClusterTopology topology, @Assisted ClusterConfigurationRequest configRequest,
                              @Assisted AmbariEventPublisher ambariEventPublisher) {
    this.configRequest = configRequest;
    this.topology = topology;
    this.ambariEventPublisher = ambariEventPublisher;
  }

  @Override
  @RunWithInternalSecurityContext(token = TopologyManager.INTERNAL_AUTH_TOKEN)
  public Boolean call() throws Exception {
    LOG.info("TopologyManager.ConfigureClusterTask: Entering");

    Collection<String> requiredHostGroups = getTopologyRequiredHostGroups();

    if (!areRequiredHostGroupsResolved(requiredHostGroups)) {
      LOG.debug("TopologyManager.ConfigureClusterTask - prerequisites for config request processing not yet " +
        "satisfied");
      throw new IllegalArgumentException("TopologyManager.ConfigureClusterTask - prerequisites for config " +
        "request processing not yet  satisfied");
    }

    try {
      LOG.info("TopologyManager.ConfigureClusterTask: All Required host groups are completed, Cluster " +
        "Configuration can now begin");
      configRequest.process();
    } catch (Exception e) {
      LOG.error("TopologyManager.ConfigureClusterTask: " +
        "An exception occurred while attempting to process cluster configs and set on cluster: ", e);

      // this will signal an unsuccessful run, retry will be triggered if required
      throw new Exception(e);
    }

    LOG.info("Cluster configuration finished successfully!");
    // Notify listeners that cluster configuration finished
    long clusterId = topology.getClusterId();
    ambariEventPublisher.publish(new ClusterConfigFinishedEvent(clusterId,
            topology.getAmbariContext().getClusterName(clusterId)));

    LOG.info("TopologyManager.ConfigureClusterTask: Exiting");
    return true;
  }

  /**
   * Return the set of host group names which are required for configuration topology resolution.
   *
   * @return set of required host group names
   */
  private Collection<String> getTopologyRequiredHostGroups() {
    Collection<String> requiredHostGroups;
    try {
      requiredHostGroups = configRequest.getRequiredHostGroups();
    } catch (RuntimeException e) {
      // just log error and allow config topology update
      LOG.error("TopologyManager.ConfigureClusterTask: An exception occurred while attempting to determine required" +
        " host groups for config update ", e);
      requiredHostGroups = Collections.emptyList();
    }
    return requiredHostGroups;
  }

  /**
   * Determine if all hosts for the given set of required host groups are known.
   *
   * @param requiredHostGroups set of required host groups
   * @return true if all required host groups are resolved
   */
  private boolean areRequiredHostGroupsResolved(Collection<String> requiredHostGroups) {
    boolean configTopologyResolved = true;
    Map<String, HostGroupInfo> hostGroupInfo = topology.getHostGroupInfo();
    for (String hostGroup : requiredHostGroups) {
      HostGroupInfo groupInfo = hostGroupInfo.get(hostGroup);
      if (groupInfo == null || groupInfo.getHostNames().size() < groupInfo.getRequestedHostCount()) {
        configTopologyResolved = false;
        if (groupInfo != null) {
          LOG.info("TopologyManager.ConfigureClusterTask areHostGroupsResolved: host group name = {} requires {} hosts to be mapped, but only {} are available.",
            groupInfo.getHostGroupName(), groupInfo.getRequestedHostCount(), groupInfo.getHostNames().size());
        }
        break;
      } else {
        LOG.info("TopologyManager.ConfigureClusterTask areHostGroupsResolved: host group name = {} has been fully resolved, as all {} required hosts are mapped to {} physical hosts.",
          groupInfo.getHostGroupName(), groupInfo.getRequestedHostCount(), groupInfo.getHostNames().size());
      }
    }
    return configTopologyResolved;
  }
}
