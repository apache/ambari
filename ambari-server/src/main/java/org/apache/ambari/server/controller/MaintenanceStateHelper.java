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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to help manage maintenance state checks.
 */
@Singleton
public class MaintenanceStateHelper {
  private static final String NAGIOS_SERVICE = "NAGIOS";
  private static final String NAGIOS_COMPONENT = "NAGIOS_SERVER";
  private static final String NAGIOS_ACTION_NAME = "nagios_update_ignore";
  private static final Logger LOG = LoggerFactory.getLogger(MaintenanceStateHelper.class);
  
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

    Host host = clusters.getHost(sch.getHostName());

    return getEffectiveState(sch, host);
  }
  
  /**
   * @param clusterId the cluster id
   * @param service the service
   * @param host the host
   * @param sch the host component
   * @return the effective maintenance state
   */
  private MaintenanceState getEffectiveState(long clusterId,
      Service service, Host host, ServiceComponentHost sch) {

    MaintenanceState schState = sch.getMaintenanceState();
    if (MaintenanceState.ON == schState) {
      return MaintenanceState.ON;
    }

    MaintenanceState serviceState = service.getMaintenanceState();
    MaintenanceState hostState = host.getMaintenanceState(clusterId);

    if (MaintenanceState.OFF != serviceState && MaintenanceState.OFF != hostState) {
      return MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST;
    }

    if (MaintenanceState.OFF != serviceState) {
      return MaintenanceState.IMPLIED_FROM_SERVICE;
    }

    if (MaintenanceState.OFF != hostState) {
      return MaintenanceState.IMPLIED_FROM_HOST;
    }
    
    return schState;
  }

  /**
   * @param clusters the collection of clusters
   * @param cluster the specific cluster to check
   * @return a property map of all host components that are in a
   * maintenance state (either {@link MaintenanceState#ON} or
   * {@link MaintenanceState#IMPLIED_FROM_HOST} or
   * {@link MaintenanceState#IMPLIED_FROM_SERVICE} or
   * {@link MaintenanceState#IMPLIED_FROM_SERVICE_AND_HOST})
   */
  public Set<Map<String, String>> getMaintenanceHostComponents(Clusters clusters, Cluster cluster) throws AmbariException {
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
  public RequestStatusResponse createRequests(AmbariManagementController amc,
      Map<String, String> requestProperties, Set<String> clusterNames) throws AmbariException {
    
    Map<String, String> params = new HashMap<String, String>();
    
    // return the first one, just like amc.createStages()
    RequestStatusResponse response = null;

    RequestResourceFilter resourceFilter =
      new RequestResourceFilter(NAGIOS_SERVICE, NAGIOS_COMPONENT, null);

    for (String clusterName : clusterNames) {
      ExecuteActionRequest actionRequest = new ExecuteActionRequest(
        clusterName, null, NAGIOS_ACTION_NAME,
        Collections.singletonList(resourceFilter),
        null, params);
      
      if (null == response) {
        response = amc.createAction(actionRequest, requestProperties);
      }
    }    
    return response;
  }

  /**
   * Determine based on the requesting Resource level and the state of the
   * operand whether to allow operations on it.
   *
   * @param operationLevel Request Source: {CLUSTER, SERVICE, HOSTCOMPONENT, HOST}
   * @param sch HostComponent which is the operand of the operation
   * @return
   * @throws AmbariException
   */
  public boolean isOperationAllowed(Resource.Type operationLevel,
                                    ServiceComponentHost sch) throws AmbariException {
    MaintenanceState maintenanceState = sch.getMaintenanceState();

    switch (operationLevel.getInternalType()) {
      case Cluster:
          if (maintenanceState.equals(MaintenanceState.OFF)) {
            return true;
          }
          break;
      case Service:
        if (maintenanceState.equals(MaintenanceState.IMPLIED_FROM_SERVICE)
                || maintenanceState.equals(MaintenanceState.OFF)) {
          return true;
        }
        break;
      case Host:
        if (maintenanceState.equals(MaintenanceState.IMPLIED_FROM_HOST)
                || maintenanceState.equals(MaintenanceState.OFF)) {
          return true;
        }
        break;
      case HostComponent: {
        return true;
      }
      default:
        LOG.warn("Unsupported Resource type, type = " + operationLevel);
        break;
    }
    return false;
  }

}
