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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.state.live.host.Host;
import org.apache.ambari.server.state.live.host.HostImpl;

public class ClustersImpl implements Clusters {

  private Map<String, Cluster> clusters;
  private Map<Long, Cluster> clustersById;
  private Map<String, Host> hosts;
  private Map<String, Set<Cluster>> hostClusterMap;

  public ClustersImpl() {
    clusters = new HashMap<String, Cluster>();
    clustersById = new HashMap<Long, Cluster>();
    hosts = new HashMap<String, Host>();
    hostClusterMap = new HashMap<String, Set<Cluster>>();

  }

  @Override
  public synchronized void addCluster(String clusterName)
      throws AmbariException {
    if (clusters.containsKey(clusterName)) {
      throw new AmbariException("Duplicate entry for Cluster"
          + ", clusterName= " + clusterName);
    }
    // TODO persist cluster into DB
    // retrieve new cluster id
    // add cluster id -> cluster mapping into clustersById
    long clusterId = 0;
    Cluster impl = new ClusterImpl(this, clusterId, clusterName);
    clusters.put(clusterName, impl);
    clustersById.put(clusterId, impl);
  }

  @Override
  public synchronized Cluster getCluster(String clusterName)
      throws AmbariException {
    if (!clusters.containsKey(clusterName)) {
      throw new ClusterNotFoundException(clusterName);
    }
    return clusters.get(clusterName);
  }

  @Override
  public synchronized List<Host> getAllHosts() {
    return new ArrayList<Host>(hosts.values());
  }

  @Override
  public synchronized List<Cluster> getClustersForHost(String hostname)
      throws AmbariException {
    if (!hostClusterMap.containsKey(hostname)) {
      throw new HostNotFoundException(hostname);
    }
    List<Cluster> cList = new ArrayList<Cluster>();
    cList.addAll(hostClusterMap.get(hostname));
    return cList;
  }

  @Override
  public synchronized Host getHost(String hostname) throws AmbariException {
    return hosts.get(hostname);
  }

  @Override
  public synchronized void addHost(String hostname) throws AmbariException {
    if (hosts.containsKey(hostname)) {
      throw new AmbariException("Duplicate entry for Host"
          + ", hostName= " + hostname);
    }
    hosts.put(hostname, new HostImpl(hostname));
    hostClusterMap.put(hostname, new HashSet<Cluster>());
  }

  @Override
  public synchronized void mapHostToCluster(String hostname,
      String clusterName) throws AmbariException {
    getCluster(clusterName);
    getHost(hostname);
    if (!hostClusterMap.containsKey(hostname)) {
      throw new HostNotFoundException(hostname);
    }
    hostClusterMap.get(hostname).add(getCluster(clusterName));
  }

  @Override
  public List<String> getHostComponents(String hostname) {
    // TODO Auto-generated method stub
    return null;
  }
}
