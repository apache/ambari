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
package org.apache.ambari.server.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.PassiveState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Used to help manage passive state checks.
 */
public class PassiveStateHelper {
  private static final String NAGIOS_SERVICE = "NAGIOS";
  private static final String NAGIOS_COMPONENT = "NAGIOS_SERVER";
  private static final String NAGIOS_ACTION_NAME = "nagios_update_ignore";
  
  @Inject
  private Clusters clusters;
  
  @Inject
  public PassiveStateHelper(Injector injector) {
    injector.injectMembers(this);
  }

  /**
   * Gets the effective state for a HostComponents
   * @param sch the host component
   * @return the passive state
   * @throws AmbariException
   */
  public PassiveState getEffectiveState(ServiceComponentHost sch) throws AmbariException {
    Cluster cluster = clusters.getCluster(sch.getClusterName());
    Service service = cluster.getService(sch.getServiceName());
    
    Map<String, Host> map = clusters.getHostsForCluster(cluster.getClusterName());
    if (null == map)
      return PassiveState.ACTIVE;
    
    Host host = clusters.getHostsForCluster(cluster.getClusterName()).get(sch.getHostName());
    if (null == host) // better not
      throw new HostNotFoundException(cluster.getClusterName(), sch.getHostName());
    
    return getEffectiveState(cluster.getClusterId(), service, host, sch);
  }
  
  private static PassiveState getEffectiveState(long clusterId, Service service,
      Host host, ServiceComponentHost sch) {
    if (PassiveState.PASSIVE == sch.getPassiveState())
      return PassiveState.PASSIVE;

    if (PassiveState.ACTIVE != service.getPassiveState() ||
        PassiveState.ACTIVE != host.getPassiveState(clusterId))
      return PassiveState.IMPLIED;
    
    return sch.getPassiveState();
  }

  /**
   * @param cluster
   * @return
   */
  public static Set<Map<String, String>> getPassiveHostComponents(Clusters clusters,
      Cluster cluster) throws AmbariException {
    
    Set<Map<String, String>> set = new HashSet<Map<String, String>>();
    
    for (Service service : cluster.getServices().values()) {
      for (ServiceComponent sc : service.getServiceComponents().values()) {
        if (sc.isClientComponent())
          continue;

        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          Host host = clusters.getHostsForCluster(
              cluster.getClusterName()).get(sch.getHostName());
          
          if (PassiveState.ACTIVE != getEffectiveState(cluster.getClusterId(),
              service, host, sch)) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("host", sch.getHostName());
            map.put("service", sch.getServiceName());
            map.put("component", sch.getServiceComponentName());
            set.add(map);
          }
        }
      }
    }
    
    return set;
  }
  
  public static RequestStatusResponse createRequest(AmbariManagementController amc,
      String clusterName, String desc) throws AmbariException {
    
    Map<String, String> params = new HashMap<String, String>();
    
    ExecuteActionRequest actionRequest = new ExecuteActionRequest(
        clusterName, RoleCommand.ACTIONEXECUTE.name(),
        NAGIOS_ACTION_NAME, NAGIOS_SERVICE, NAGIOS_COMPONENT, null, params);

    Map<String, String> map = new HashMap<String, String>();
    map.put("context", "Update " + desc + " passive state");
    
    return amc.createAction(actionRequest, map);
  }  
  
}
