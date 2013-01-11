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

import javax.persistence.RollbackException;

import com.google.gson.Gson;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ClustersImpl implements Clusters {

  private static final Logger LOG = LoggerFactory.getLogger(
      ClustersImpl.class);

  private Map<String, Cluster> clusters;
  private Map<Long, Cluster> clustersById;
  private Map<String, Host> hosts;
  private Map<String, Set<Cluster>> hostClusterMap;
  private Map<String, Set<Host>> clusterHostMap;

  @Inject
  ClusterDAO clusterDAO;
  @Inject
  HostDAO hostDAO;
  @Inject
  ClusterFactory clusterFactory;
  @Inject
  HostFactory hostFactory;
  @Inject
  AmbariMetaInfo ambariMetaInfo;
  @Inject
  Gson gson;

  @Inject
  public ClustersImpl() {
    clusters = new HashMap<String, Cluster>();
    clustersById = new HashMap<Long, Cluster>();
    hosts = new HashMap<String, Host>();
    hostClusterMap = new HashMap<String, Set<Cluster>>();
    clusterHostMap = new HashMap<String, Set<Host>>();
    LOG.info("Initializing the ClustersImpl");
  }

  @Override
  public synchronized void addCluster(String clusterName)
      throws AmbariException {
    if (clusters.containsKey(clusterName)) {
      throw new DuplicateResourceException("Attempted to create a Cluster which already exists"
          + ", clusterName=" + clusterName);
    }

    // retrieve new cluster id
    // add cluster id -> cluster mapping into clustersById
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(clusterName);
    clusterEntity.setDesiredStackVersion(gson.toJson(new StackId()));
    try {
      clusterDAO.create(clusterEntity);
      clusterEntity = clusterDAO.merge(clusterEntity);
      Cluster cluster = clusterFactory.create(clusterEntity);

      clusters.put(clusterName, cluster);
      clustersById.put(cluster.getClusterId(), cluster);
      clusterHostMap.put(clusterName, new HashSet<Host>());
    } catch (RollbackException e) {
      LOG.warn("Unable to create cluster " + clusterName, e);
      throw new AmbariException("Unable to create cluster " + clusterName, e);
    }
  }

  @Override
  @Transactional
  public synchronized Cluster getCluster(String clusterName)
      throws AmbariException {
    if (!clusters.containsKey(clusterName)) {
      ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);
      if (clusterEntity != null) {
        Cluster cl = getClusterById(clusterEntity.getClusterId());
        clustersById.put(cl.getClusterId(), cl);
        clusters.put(cl.getClusterName(), cl);
        if (!clusterHostMap.containsKey(clusterEntity.getClusterName()))
          clusterHostMap.put(clusterEntity.getClusterName(), new HashSet<Host>());
      } else {
        throw new ClusterNotFoundException(clusterName);
      }
    }
    return clusters.get(clusterName);
  }

  @Override
  @Transactional
  public synchronized Cluster getClusterById(long id) throws AmbariException {
    if (!clustersById.containsKey(id)) {
      ClusterEntity clusterEntity = clusterDAO.findById(id);
      if (clusterEntity != null) {
        Cluster cluster = clusterFactory.create(clusterEntity);
        clustersById.put(cluster.getClusterId(), cluster);
        clusters.put(clusterEntity.getClusterName(), cluster);
        if (!clusterHostMap.containsKey(clusterEntity.getClusterName()))
          clusterHostMap.put(clusterEntity.getClusterName(), new HashSet<Host>());
      } else {
        throw new ClusterNotFoundException("clusterID=" + id);
      }
    }
    return clustersById.get(id);
  }

  @Override
  @Transactional
  public synchronized List<Host> getHosts() {
    List<Host> hostList = new ArrayList<Host>(hosts.size());
    hostList.addAll(hosts.values());

    for (HostEntity hostEntity : hostDAO.findAll()) {
      if (!hosts.containsKey(hostEntity.getHostName())) {
        try {
          hostList.add(getHost(hostEntity.getHostName()));
        } catch (AmbariException ignored) {
          LOG.error("Database externally modified?");
        }
      }
    }

    return hostList;
  }

  @Override
  public synchronized Set<Cluster> getClustersForHost(String hostname)
      throws AmbariException {
    if (!hostClusterMap.containsKey(hostname)) {
      getHost(hostname);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Looking up clusters for hostname"
          + ", hostname=" + hostname
          + ", mappedClusters=" + hostClusterMap.get(hostname).size());
    }
    return Collections.unmodifiableSet(hostClusterMap.get(hostname));
  }

  @Override
  @Transactional
  public synchronized Host getHost(String hostname) throws AmbariException {
    if (!hosts.containsKey(hostname)) {
      HostEntity hostEntity = hostDAO.findByName(hostname);
      if (hostEntity != null) {
        Host host = hostFactory.create(hostEntity, true);
        Set<Cluster> cSet = new HashSet<Cluster>();
        hosts.put(hostname, host);
        hostClusterMap.put(hostname, cSet);

        for (ClusterEntity clusterEntity : hostEntity.getClusterEntities()) {
          if (clustersById.containsKey(clusterEntity.getClusterId())) {
            cSet.add(clustersById.get(clusterEntity.getClusterId()));
          } else {
            cSet.add(getClusterById(clusterEntity.getClusterId()));
          }
        }
      } else {
        throw new HostNotFoundException(hostname);
      }
    }
    return hosts.get(hostname);
  }

  @Override
  public synchronized void addHost(String hostname) throws AmbariException {
    if (hosts.containsKey(hostname)) {
      throw new AmbariException("Duplicate entry for Host"
          + ", hostName= " + hostname);
    }
    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostname);
    hostEntity.setClusterEntities(new ArrayList<ClusterEntity>());
    //not stored to DB
    Host host = hostFactory.create(hostEntity, false);
    host.setAgentVersion(new AgentVersion(""));
    List<DiskInfo> emptyDiskList = new ArrayList<DiskInfo>();
    host.setDisksInfo(emptyDiskList);
    host.setHealthStatus(new HostHealthStatus(HealthStatus.UNKNOWN, ""));
    host.setHostAttributes(new HashMap<String, String>());
    host.setState(HostState.INIT);

    hosts.put(hostname, host);
    hostClusterMap.put(hostname, new HashSet<Cluster>());
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a host to Clusters"
          + ", hostname=" + hostname);
    }
  }

  private boolean isOsSupportedByClusterStack(Cluster c, Host h) {
    Map<String, List<RepositoryInfo>> repos =
        ambariMetaInfo.getRepository(c.getDesiredStackVersion().getStackName(),
            c.getDesiredStackVersion().getStackVersion());
    if (repos == null || repos.isEmpty()) {
      return false;
    }
    return repos.containsKey(h.getOsType());
  }

  @Override
  public synchronized void mapHostToCluster(String hostname,
      String clusterName) throws AmbariException {
    Cluster cluster = getCluster(clusterName);
    HostImpl host = (HostImpl) getHost(hostname);

    if (!hostClusterMap.containsKey(hostname)) {
      throw new HostNotFoundException(hostname);
    }

    for (Cluster c : hostClusterMap.get(hostname)) {
      if (c.getClusterName().equals(clusterName)) {
        throw new DuplicateResourceException("Attempted to create a host which already exists: clusterName=" +
            clusterName + ", hostName=" + hostname);
      }
    }

    if (!isOsSupportedByClusterStack(cluster, host)) {
      String message = "Trying to map host to cluster where stack does not"
          + " support host's os type"
          + ", clusterName=" + clusterName
          + ", clusterStackId=" + cluster.getDesiredStackVersion().getStackId()
          + ", hostname=" + hostname
          + ", hostOsType=" + host.getOsType();
      LOG.warn(message);
      throw new AmbariException(message);
    }

    mapHostClusterEntities(hostname, cluster.getClusterId());

    hostClusterMap.get(hostname).add(cluster);
    clusterHostMap.get(clusterName).add(host);

    cluster.refresh();
    host.refresh();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Mapping a host to a cluster"
          + ", clusterName=" + clusterName
          + ", clusterId=" + cluster.getClusterId()
          + ", hostname=" + hostname);
    }
  }

  @Transactional
  void mapHostClusterEntities(String hostName, Long clusterId) {
    HostEntity hostEntity = hostDAO.findByName(hostName);
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);

    hostEntity.getClusterEntities().add(clusterEntity);
    clusterEntity.getHostEntities().add(hostEntity);

    clusterDAO.merge(clusterEntity);
    hostDAO.merge(hostEntity);
  }

  @Override
  @Transactional
  public synchronized Map<String, Cluster> getClusters() {
    for (ClusterEntity clusterEntity : clusterDAO.findAll()) {
      try {
        if (!clustersById.containsKey(clusterEntity.getClusterId())) {
          getClusterById(clusterEntity.getClusterId());
        }
      } catch (AmbariException ignored) {

      }
    }
    return Collections.unmodifiableMap(clusters);
  }

  @Override
  public synchronized void mapHostsToCluster(Set<String> hostnames,
      String clusterName) throws AmbariException {
    for (String hostname : hostnames) {
      mapHostToCluster(hostname, clusterName);
    }
  }

  @Override
  public synchronized void updateClusterName(String oldName, String newName) {
    clusters.put(newName, clusters.remove(oldName));
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

  @Override
  @Transactional
  public Map<String, Host> getHostsForCluster(String clusterName)
      throws AmbariException {

    getCluster(clusterName);

    Map<String, Host> hosts = new HashMap<String, Host>();

    for (Host h : clusterHostMap.get(clusterName)) {
      hosts.put(h.getHostName(), h);
    }

    return hosts;
  }

  @Override
  @Transactional
  public synchronized void deleteCluster(String clusterName)
      throws AmbariException {
    Cluster cluster = getCluster(clusterName);
    if (!cluster.canBeRemoved()) {
      throw new AmbariException("Could not delete cluster"
          + ", clusterName=" + clusterName);
    }
    cluster.deleteAllServices();
    clusters.remove(clusterName);
    // FIXME update DB
  }

}
