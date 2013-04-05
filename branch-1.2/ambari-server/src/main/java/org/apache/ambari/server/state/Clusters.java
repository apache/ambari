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

package org.apache.ambari.server.state;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;

/**
 * Single entity that tracks all clusters and hosts that are managed
 * by the Ambari server
 */
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

  /**
   * Get all known clusters
   * @return
   */
  public Map<String, Cluster> getClusters();

  /**
   * Get all hosts being tracked by the Ambari server
   * @return
   */
  public List<Host> getHosts();

  /**
   * Returns all the cluster names for this hostname.
   * @param hostname
   * @return List of cluster names
   * @throws AmbariException
   */
  public Set<Cluster> getClustersForHost(String hostname)
      throws AmbariException;


  /**
   * Get a Host object managed by this server
   * @param hostname Name of the host requested
   * @return Host object
   * @throws AmbariException
   */
  public Host getHost(String hostname) throws AmbariException;

  /**
   * Add a Host object to be managed by this server
   * @param hostname Host to be added
   * @throws AmbariException
   */
  public void addHost(String hostname) throws AmbariException;

  /**
   * Map host to the given cluster.
   * A host can belong to multiple clusters.
   * @param hostname
   * @param clusterName
   * @throws AmbariException
   */
  public void mapHostToCluster(String hostname, String clusterName)
      throws AmbariException;


  public void mapHostsToCluster(Set<String> hostnames, String clusterName)
      throws AmbariException;

  public void updateClusterName(String oldName, String newName);

  public Cluster getClusterById(long id) throws AmbariException;

  public void debugDump(StringBuilder sb);

  public Map<String, Host> getHostsForCluster(String clusterName)
      throws AmbariException;

  public void deleteCluster(String clusterName) throws AmbariException;

}
