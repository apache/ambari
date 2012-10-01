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

package org.apache.ambari.server.state.live;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHost;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostEvent;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostState;

public class ClusterImpl implements Cluster {

  private final Clusters clusters;

  private final long clusterId;

  private String clusterName;

  /**
   * [ ServiceName -> [ ServiceComponentName -> [ HostName -> [ ... ] ] ] ]
   */
  private Map<String, Map<String, Map<String, ServiceComponentHost>>>
      serviceComponentHosts;

  /**
   * [ HostName -> [ ... ] ]
   */
  private Map<String, Map<String, Map<String, ServiceComponentHost>>>
      serviceComponentHostsByHost;

  public ClusterImpl(Clusters clusters, long clusterId, String clusterName) {
    this.clusters = clusters;
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.serviceComponentHosts = new HashMap<String,
        Map<String,Map<String,ServiceComponentHost>>>();
    this.serviceComponentHostsByHost = new HashMap<String,
        Map<String,Map<String,ServiceComponentHost>>>();
  }

  public ServiceComponentHost getServiceComponentHost(String serviceName,
      String serviceComponentName, String hostname) throws AmbariException {
    if (!serviceComponentHosts.containsKey(serviceName)
        || !serviceComponentHosts.get(serviceName)
            .containsKey(serviceComponentName)
        || !serviceComponentHosts.get(serviceName).get(serviceComponentName)
            .containsKey(hostname)) {
      throw new ServiceComponentHostNotFoundException(serviceName,
          serviceComponentName, hostname);
    }
    return serviceComponentHosts.get(serviceName).get(serviceComponentName)
        .get(hostname);
  }

  @Override
  public synchronized String getClusterName() {
    return clusterName;
  }

  @Override
  public synchronized void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @Override
  public synchronized void addServiceComponentHost(String serviceName,
      String componentName, String hostname, boolean isClient)
      throws AmbariException {
    List<Cluster> cs = clusters.getClustersForHost(hostname);
    boolean clusterFound = false;
    for (Cluster c : cs) {
      if (c.getClusterId() == this.clusterId) {
        clusterFound = true;
        break;
      }
    }
    if (!clusterFound) {
      throw new AmbariException("Host does not belong this cluster"
          + ", hostname=" + hostname
          + ", clusterName=" + clusterName
          + ", clusterId=" + clusterId);
    }

    if (!serviceComponentHosts.containsKey(serviceName)) {
      serviceComponentHosts.put(serviceName,
          new HashMap<String, Map<String,ServiceComponentHost>>());
    }
    if (!serviceComponentHosts.get(serviceName).containsKey(componentName)) {
      serviceComponentHosts.get(serviceName).put(componentName,
          new HashMap<String, ServiceComponentHost>());
    }

    if (serviceComponentHosts.get(serviceName).get(componentName).
        containsKey(hostname)) {
      throw new AmbariException("Duplicate entry for ServiceComponentHost"
          + ", serviceName=" + serviceName
          + ", serviceComponentName" + componentName
          + ", hostname= " + hostname);
    }

    if (!serviceComponentHostsByHost.containsKey(hostname)) {
      serviceComponentHostsByHost.put(hostname,
          new HashMap<String, Map<String,ServiceComponentHost>>());
    }
    if (!serviceComponentHostsByHost.get(hostname).containsKey(serviceName)) {
      serviceComponentHostsByHost.get(hostname).put(serviceName,
          new HashMap<String, ServiceComponentHost>());
    }

    ServiceComponentHost impl =
        new ServiceComponentHostImpl(clusterId,
            serviceName, componentName, hostname, isClient);

    serviceComponentHosts.get(serviceName).get(componentName).put(hostname,
        impl);
    serviceComponentHostsByHost.get(hostname).get(serviceName).put(
        componentName, impl);
  }

  @Override
  public synchronized ServiceComponentHostState getServiceComponentHostState(String service,
      String serviceComponent, String hostname) throws AmbariException {
    return
        getServiceComponentHost(service, serviceComponent, hostname).getState();
  }

  @Override
  public synchronized void setServiceComponentHostState(String service,
      String serviceComponent, String hostname,
      ServiceComponentHostState state) throws AmbariException {
    getServiceComponentHost(service, serviceComponent, hostname)
      .setState(state);
  }

  @Override
  public synchronized void handleServiceComponentHostEvent(String service,
      String serviceComponent, String hostname,
      ServiceComponentHostEvent event)
      throws AmbariException, InvalidStateTransitonException {
    getServiceComponentHost(service, serviceComponent, hostname)
      .handleEvent(event);

  }

  @Override
  public long getClusterId() {
    return clusterId;
  }
  
  @Override
  public List<ServiceComponentHost> getServiceComponentHosts(String hostname) {
    // TODO Auto-generated method stub
    return null;
  }

}
