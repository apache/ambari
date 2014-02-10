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

import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.PassiveState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Used to help manage passive state checks.
 */
public class PassiveStateHelper {

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
    
    if (PassiveState.PASSIVE == sch.getPassiveState())
      return PassiveState.PASSIVE;

    if (PassiveState.ACTIVE != service.getPassiveState() ||
        PassiveState.ACTIVE != host.getPassiveState(cluster.getClusterId()))
      return PassiveState.IMPLIED;
    
    return sch.getPassiveState();
  }
  
}
