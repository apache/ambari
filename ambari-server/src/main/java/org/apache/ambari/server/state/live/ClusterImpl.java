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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;

public class ClusterImpl implements Cluster {

  private final Clusters clusters;

  private final long clusterId;

  private String clusterName;
  
  private Map<String, Service> services;
  
  /**
   * [ ServiceName -> [ ServiceComponentName -> [ HostName -> [ ... ] ] ] ]
   */
  private Map<String, Map<String, Map<String, ServiceComponentHost>>>
      serviceComponentHosts;

  /**
   * [ HostName -> [ ... ] ]
   */
  private Map<String, List<ServiceComponentHost>>
      serviceComponentHostsByHost;

  public ClusterImpl(Clusters clusters, long clusterId, String clusterName) {
    this.clusters = clusters;
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.serviceComponentHosts = new HashMap<String,
        Map<String,Map<String,ServiceComponentHost>>>();
    this.serviceComponentHostsByHost = new HashMap<String,
        List<ServiceComponentHost>>();
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

  synchronized void addServiceComponentHost(ServiceComponentHost svcCompHost)
      throws AmbariException {
    final String hostname = svcCompHost.getHostName();
    final String serviceName = svcCompHost.getServiceName();
    final String componentName = svcCompHost.getServiceComponentName();
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
          new ArrayList<ServiceComponentHost>());
    }

    serviceComponentHosts.get(serviceName).get(componentName).put(hostname,
        svcCompHost);
    serviceComponentHostsByHost.get(hostname).add(svcCompHost);
  }

  @Override
  public long getClusterId() {
    return clusterId;
  }
  
  @Override
  public synchronized List<ServiceComponentHost> getServiceComponentHosts(
      String hostname) {
    return Collections.unmodifiableList(
        serviceComponentHostsByHost.get(hostname));
  }

  @Override
  public synchronized void addService(Service service) {
    
    
    this.services.put(service.getName(), service);
  }

  @Override
  public synchronized Service getService(String serviceName)
      throws AmbariException {
    return services.get(serviceName);
  }

  @Override
  public synchronized Map<String, Service> getServices() {
    return Collections.unmodifiableMap(services);
  }

}
