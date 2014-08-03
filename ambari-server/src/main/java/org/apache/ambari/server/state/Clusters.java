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

import org.apache.ambari.server.AmbariException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single entity that tracks all clusters and hosts that are managed
 * by the Ambari server
 */
public interface Clusters {

  /**
   * Add a new Cluster
   * @param clusterName
   */
  public void addCluster(String clusterName)
      throws AmbariException;

  /**
   * Gets the Cluster given the cluster name
   * @param clusterName Name of the Cluster to retrieve
   * @return  <code>Cluster</code> identified by the given name
   */
  public Cluster getCluster(String clusterName)
      throws AmbariException;

  /**
   * Get all clusters
   * @return <code>Map</code> of clusters with cluster name as key
   */
  public Map<String, Cluster> getClusters();

  /**
   * Get all hosts being tracked by the Ambari server
   * @return <code>List</code> of <code>Host</code>
   */
  public List<Host> getHosts();

  /**
   * Returns all the cluster names for this hostname
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
   * A host can belong to multiple clusters
   * @param hostname
   * @param clusterName
   * @throws AmbariException
   */
  public void mapHostToCluster(String hostname, String clusterName)
      throws AmbariException;

  /**
   * Maps a set of hosts to the given cluster
   * @param hostnames
   * @param clusterName
   * @throws AmbariException
   */
  public void mapHostsToCluster(Set<String> hostnames, String clusterName)
      throws AmbariException;

  /**
   * Updates the name of the cluster
   * @param oldName
   * @param newName
   * @throws AmbariException
   */
  public void updateClusterName(String oldName, String newName);

  /**
   * Gets the cluster using the id.
   * @param id The identifier associated with the cluster
   * @return <code>Cluster</code> identified by the identifier
   * @throws AmbariException
   */
  public Cluster getClusterById(long id) throws AmbariException;

  /**
   * Produces a debug dump into the supplied string buffer
   * @param sb The string buffer to add the debug dump to
   */
  public void debugDump(StringBuilder sb);

  /**
   * Gets all the hosts associated with the cluster
   * @param clusterName The name of the cluster
   * @return <code>Map</code> containing host name and <code>Host</code>
   * @throws AmbariException
   */
  public Map<String, Host> getHostsForCluster(String clusterName)
      throws AmbariException;

  /**
   * Deletes the cluster identified by the name
   * @param clusterName The name of the cluster
   * @throws AmbariException
   */
  public void deleteCluster(String clusterName)
      throws AmbariException;

  /**
   * Sets the current stack version for the cluster
   * @param clusterName The name of the cluster
   * @param stackId The identifier for the stack
   * @throws AmbariException
   */
  public void setCurrentStackVersion(String clusterName, StackId stackId)
      throws AmbariException;

  /**
   * Update the host set for clusters and the host attributes associated with the hosts
   * @param hostsClusters
   * @param hostAttributes
   * @throws AmbariException
   */
  public void updateHostWithClusterAndAttributes(
      Map<String, Set<String>> hostsClusters, Map<String, Map<String, String>> hostAttributes)
      throws AmbariException;

  /**
   * Removes a host from a cluster.  Inverts {@link #mapHostToCluster(String, String)
   * @param hostname
   * @param clusterName
   */
  public void unmapHostFromCluster(String hostname, String clusterName)
      throws AmbariException;

  /**
   * Removes a host.  Inverts {@link #addHost(String)}
   * @param hostname
   */
  public void deleteHost(String hostname)
      throws AmbariException;

  /**
   * Determine whether or not access to the cluster resource identified
   * by the given cluster name should be allowed based on the permissions
   * granted to the current user.
   *
   * @param clusterName  the cluster name
   * @param readOnly     indicate whether or not this check is for a read only operation
   *
   * @return true if access to the cluster is allowed
   */
  public boolean checkPermission(String clusterName, boolean readOnly);
}
