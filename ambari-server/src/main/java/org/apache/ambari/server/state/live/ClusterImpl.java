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
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.host.Host;
import org.apache.ambari.server.state.live.host.HostEvent;
import org.apache.ambari.server.state.live.host.HostImpl;
import org.apache.ambari.server.state.live.host.HostState;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHost;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostEvent;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostState;

public class ClusterImpl implements Cluster {

  private final String clusterName;
  private Map<String, Host> hosts;
  private Map<String, Map<String, Map<String, ServiceComponentHost>>>
      serviceComponentHosts;

  public ClusterImpl(String clusterName) {
    this.clusterName = clusterName;
    this.hosts = new HashMap<String, Host>();
    this.serviceComponentHosts = new HashMap<String,
        Map<String,Map<String,ServiceComponentHost>>>();
  }

  private Host getHost(String hostName) throws AmbariException {
    if (!hosts.containsKey(hostName)) {
      throw new HostNotFoundException(hostName);
    }
    return hosts.get(hostName);
  }

  public ServiceComponentHost getServiceComponentHost(String serviceName,
      String serviceComponentName, String hostName) throws AmbariException {
    if (!serviceComponentHosts.containsKey(serviceName)
        || !serviceComponentHosts.get(serviceName)
            .containsKey(serviceComponentName)
        || !serviceComponentHosts.get(serviceName).get(serviceComponentName)
            .containsKey(hostName)) {
      throw new ServiceComponentHostNotFoundException(serviceName,
          serviceComponentName, hostName);
    }
    return serviceComponentHosts.get(serviceName).get(serviceComponentName)
        .get(hostName);
  }

  @Override
  public String getClusterName() {
    return clusterName;
  }

  @Override
  public synchronized void addHost(String hostName) throws AmbariException {
    if (hosts.containsKey(hostName)) {
      throw new AmbariException("Duplicate entry for Host"
          + ", hostName=" + hostName);
    }
    hosts.put(hostName, new HostImpl(hostName));
  }

  @Override
  public synchronized void addServiceComponentHost(String serviceName,
      String componentName, String hostName, boolean isClient)
      throws AmbariException {
    if (!serviceComponentHosts.containsKey(serviceName)) {
      serviceComponentHosts.put(serviceName,
          new HashMap<String, Map<String,ServiceComponentHost>>());
    }
    if (!serviceComponentHosts.get(serviceName).containsKey(componentName)) {
      serviceComponentHosts.get(serviceName).put(componentName,
          new HashMap<String, ServiceComponentHost>());
    }

    if (serviceComponentHosts.get(serviceName).get(componentName).
        containsKey(hostName)) {
      throw new AmbariException("Duplicate entry for ServiceComponentHost"
          + ", serviceName=" + serviceName
          + ", serviceComponentName" + componentName
          + ", hostName= " + hostName);
    }
    serviceComponentHosts.get(serviceName).get(componentName).put(hostName,
        new ServiceComponentHostImpl(componentName, hostName, isClient));
  }

  @Override
  public HostState getHostState(String hostName) throws AmbariException{
    return getHost(hostName).getState();
  }

  @Override
  public void setHostState(String hostName, HostState state)
      throws AmbariException {
    getHost(hostName).setState(state);
  }

  @Override
  public void handleHostEvent(String hostName, HostEvent event)
      throws AmbariException, InvalidStateTransitonException {
    if (!hosts.containsKey(hostName)) {
      throw new HostNotFoundException(hostName);
    }
    hosts.get(hostName).handleEvent(event);
  }

  @Override
  public ServiceComponentHostState getServiceComponentHostState(String service,
      String serviceComponent, String hostName) throws AmbariException {
    return
        getServiceComponentHost(service, serviceComponent, hostName).getState();
  }

  @Override
  public void setServiceComponentHostState(String service,
      String serviceComponent, String hostName,
      ServiceComponentHostState state) throws AmbariException {
    getServiceComponentHost(service, serviceComponent, hostName)
      .setState(state);
  }

  @Override
  public void handleServiceComponentHostEvent(String service,
      String serviceComponent, String hostName,
      ServiceComponentHostEvent event)
      throws AmbariException, InvalidStateTransitonException {
    getServiceComponentHost(service, serviceComponent, hostName)
      .handleEvent(event);

  }

}
