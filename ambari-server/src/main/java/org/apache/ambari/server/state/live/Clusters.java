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

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.live.host.Host;
import org.apache.ambari.server.state.live.host.HostState;

public interface Clusters {

  /**
   * Add a new Cluster
   * @param clusterName
   */
  public void addCluster(String clusterName) throws AmbariException;

  /**
   * Get the Cluster given the cluster name
   * @param clusterName Name of the Cluster to retrieve
   * @return
   */
  public Cluster getCluster(String clusterName) throws AmbariException;

  public boolean handleHeartbeat(String hostname, long timestamp);

  public void updateStatus(String hostname, String status);

  public List<Host> getAllHosts();

  public List<String> getHostComponents(String hostname);

  public void handleRegistration(String hostname);

  /**
   * Returns all the cluster names for this hostname.
   * @param hostname
   * @return List of cluster names
   */
  public List<Cluster> getClusters(String hostname);

  /**
   * Get a Host object
   */
  public Host getHost(String host) throws AmbariException;

}
