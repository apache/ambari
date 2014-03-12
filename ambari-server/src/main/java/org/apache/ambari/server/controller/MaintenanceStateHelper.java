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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Used to help manage maintenance state checks.
 */
public class MaintenanceStateHelper {
  private static final String NAGIOS_SERVICE = "NAGIOS";
  private static final String NAGIOS_COMPONENT = "NAGIOS_SERVER";
  private static final String NAGIOS_ACTION_NAME = "nagios_update_ignore";
  
  @Inject
  private Clusters clusters;
  
  @Inject
  public MaintenanceStateHelper(Injector injector) {
    injector.injectMembers(this);
  }

  /**
   * Get effective state of HostComponent
   * @param sch the host component
   * @param host the host
   * @return the maintenance state
   * @throws AmbariException
   */
  public MaintenanceState getEffectiveState(ServiceComponentHost sch,
                                            Host host) throws AmbariException {
    Cluster cluster = clusters.getCluster(sch.getClusterName());
    Service service = cluster.getService(sch.getServiceName());

    if (null == host) // better not
      throw new HostNotFoundException(cluster.getClusterName(), sch.getHostName());

    return getEffectiveState(cluster.getClusterId(), service, host, sch);
  }

  /**
   * Gets the effective state for a HostComponent
   * @param sch the host component
   * @return the maintenance state
   * @throws AmbariException
   */
  public MaintenanceState getEffectiveState(ServiceComponentHost sch) throws AmbariException {
    Cluster cluster = clusters.getCluster(sch.getClusterName());

    Map<String, Host> map = clusters.getHostsForCluster(cluster.getClusterName());
    if (null == map)
      return MaintenanceState.OFF;

    Host host = map.get(sch.getHostName());

    return getEffectiveState(sch, host);
  }
  
  /**
   * @param clusterId the cluster id
   * @param service the service
   * @param host the host
   * @param sch the host component
   * @return the effective maintenance state
   */
  private static MaintenanceState getEffectiveState(long clusterId, Service service,
      Host host, ServiceComponentHost sch) {
    if (MaintenanceState.ON == sch.getMaintenanceState())
      return MaintenanceState.ON;

    if (MaintenanceState.OFF != service.getMaintenanceState() ||
        MaintenanceState.OFF != host.getMaintenanceState(clusterId))
      return MaintenanceState.IMPLIED;
    
    return sch.getMaintenanceState();
  }

  /**
   * @param clusters the collection of clusters
   * @param cluster the specific cluster to check
   * @return a property map of all host components that are in a
   * maintenance state (either {@link MaintenanceState#ON} or
   * {@link MaintenanceState#IMPLIED})
   */
  public static Set<Map<String, String>> getMaintenanceHostComponents(Clusters clusters, Cluster cluster) throws AmbariException {
    
    Set<Map<String, String>> set = new HashSet<Map<String, String>>();

    Map<String, Host> hosts = clusters.getHostsForCluster(cluster.getClusterName());
    
    for (Service service : cluster.getServices().values()) {
      for (ServiceComponent sc : service.getServiceComponents().values()) {
        if (sc.isClientComponent())
          continue;

        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          Host host = hosts.get(sch.getHostName());
          
          if (MaintenanceState.OFF != getEffectiveState(cluster.getClusterId(),
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
  
  /**
   * Creates the requests to send to the clusters
   * @param amc the controller
   * @param requestProperties the request properties
   * @param clusterNames the names of all the clusters to update
   * @return the response
   * @throws AmbariException
   */
  public static RequestStatusResponse createRequests(AmbariManagementController amc,
      Map<String, String> requestProperties, Set<String> clusterNames) throws AmbariException {
    
    Map<String, String> params = new HashMap<String, String>();
    
    // return the first one, just like amc.createStages()
    RequestStatusResponse response = null;

    RequestResourceFilter resourceFilter =
      new RequestResourceFilter(NAGIOS_SERVICE, NAGIOS_COMPONENT, null);

    for (String clusterName : clusterNames) {
      ExecuteActionRequest actionRequest = new ExecuteActionRequest(
        clusterName, null, NAGIOS_ACTION_NAME,
        Collections.singletonList(resourceFilter), params);
      
      if (null == response)
        response = amc.createAction(actionRequest, requestProperties);
    }    
    return response;
  }  
  
}
