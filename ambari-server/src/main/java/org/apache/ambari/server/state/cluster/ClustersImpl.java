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

package org.apache.ambari.server.state.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.host.HostImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class ClustersImpl implements Clusters {

  private static final Logger LOG = LoggerFactory.getLogger(
      ClustersImpl.class);

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
    long clusterId = clusterName.hashCode();
    Cluster impl = new ClusterImpl(this, clusterId, clusterName);
    clusters.put(clusterName, impl);
    clustersById.put(clusterId, impl);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a new cluster"
          + ", clusterName=" + clusterName
          + ", clusterId=" + clusterId);
    }
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
  public synchronized List<Host> getHosts() {
    return new ArrayList<Host>(hosts.values());
  }

  @Override
  public synchronized Set<Cluster> getClustersForHost(String hostname)
      throws AmbariException {
    if (!hostClusterMap.containsKey(hostname)) {
      throw new HostNotFoundException(hostname);
    }
    return Collections.unmodifiableSet(hostClusterMap.get(hostname));
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
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a host to Clusters"
          + ", hostname=" + hostname);
    }
  }

  @Override
  public synchronized void mapHostToCluster(String hostname,
      String clusterName) throws AmbariException {
    Cluster c = getCluster(clusterName);
    getHost(hostname);
    if (!hostClusterMap.containsKey(hostname)) {
      throw new HostNotFoundException(hostname);
    }
    hostClusterMap.get(hostname).add(c);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Mapping a host to a cluster"
          + ", clusterName=" + clusterName
          + ", clusterId=" + c.getClusterId()
          + ", hostname=" + hostname);
    }
  }

  @Override
  public synchronized Map<String, Cluster> getClusters() {
    return Collections.unmodifiableMap(clusters);
  }

  @Override
  public synchronized void mapHostsToCluster(List<String> hostnames,
      String clusterName) throws AmbariException {
    Cluster c = getCluster(clusterName);
    for (String hostname : hostnames) {
      if (!hostClusterMap.containsKey(hostname)) {
        throw new HostNotFoundException(hostname);
      }
      hostClusterMap.get(hostname).add(c);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Mapping a host to a cluster"
            + ", clusterName=" + clusterName
            + ", clusterId=" + c.getClusterId()
            + ", hostname=" + hostname);
      }
    }
  }

  public void debugDump(StringBuilder sb) {
    sb.append("Clusters=[ ");
    boolean first = true;
    for(Cluster c : clusters.values()) {
      if (!first) {
        sb.append(" , ");
        first = false;
      }
      sb.append("\n  ");
      c.debugDump(sb);
      sb.append(" ");
    }
    sb.append(" ]");
  }

}
