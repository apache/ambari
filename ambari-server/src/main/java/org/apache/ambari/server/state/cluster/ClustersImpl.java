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

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupHostMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.host.HostFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.RollbackException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class ClustersImpl implements Clusters {

  private static final Logger LOG = LoggerFactory.getLogger(
      ClustersImpl.class);

  private ConcurrentHashMap<String, Cluster> clusters;
  private ConcurrentHashMap<Long, Cluster> clustersById;
  private ConcurrentHashMap<String, Host> hosts;
  private ConcurrentHashMap<String, Set<Cluster>> hostClusterMap;
  private ConcurrentHashMap<String, Set<Host>> clusterHostMap;

  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  private final Lock r = rwl.readLock();
  private final Lock w = rwl.writeLock();

  volatile boolean clustersLoaded = false;

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
  private ConfigGroupHostMappingDAO configGroupHostMappingDAO;

  @Inject
  public ClustersImpl() {
    clusters = new ConcurrentHashMap<String, Cluster>();
    clustersById = new ConcurrentHashMap<Long, Cluster>();
    hosts = new ConcurrentHashMap<String, Host>();
    hostClusterMap = new ConcurrentHashMap<String, Set<Cluster>>();
    clusterHostMap = new ConcurrentHashMap<String, Set<Host>>();

    LOG.info("Initializing the ClustersImpl");
  }

  void  checkLoaded() {
    if (!clustersLoaded) {
      w.lock();
      try {
        if (!clustersLoaded) {
          loadClustersAndHosts();
        }
        clustersLoaded = true;
      } finally {
        w.unlock();
      }
    }
  }

  @Transactional
  void loadClustersAndHosts() {
    for (ClusterEntity clusterEntity : clusterDAO.findAll()) {
      Cluster currentCluster = clusterFactory.create(clusterEntity);
      clusters.put(clusterEntity.getClusterName(), currentCluster);
      clustersById.put(currentCluster.getClusterId(), currentCluster);
      clusterHostMap.put(currentCluster.getClusterName(), Collections.newSetFromMap(new ConcurrentHashMap<Host, Boolean>()));
    }

    for (HostEntity hostEntity : hostDAO.findAll()) {
      Host host = hostFactory.create(hostEntity, true);
      hosts.put(hostEntity.getHostName(), host);
      Set<Cluster> cSet = Collections.newSetFromMap(new ConcurrentHashMap<Cluster, Boolean>());
      hostClusterMap.put(hostEntity.getHostName(), cSet);

      for (ClusterEntity clusterEntity : hostEntity.getClusterEntities()) {
        clusterHostMap.get(clusterEntity.getClusterName()).add(host);
        cSet.add(clusters.get(clusterEntity.getClusterName()));
      }
    }

  }

  @Override
  public void addCluster(String clusterName)
      throws AmbariException {
    checkLoaded();

    if (clusters.containsKey(clusterName)) {
      throw new DuplicateResourceException("Attempted to create a Cluster which already exists"
          + ", clusterName=" + clusterName);
    }

    w.lock();
    try {
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
      } catch (RollbackException e) {
        LOG.warn("Unable to create cluster " + clusterName, e);
        throw new AmbariException("Unable to create cluster " + clusterName, e);
      }

      Cluster cluster = clusterFactory.create(clusterEntity);
      clusters.put(clusterName, cluster);
      clustersById.put(cluster.getClusterId(), cluster);
      clusterHostMap.put(clusterName, new HashSet<Host>());
    } finally {
      w.unlock();
    }
  }

  @Override
  public Cluster getCluster(String clusterName)
      throws AmbariException {
    checkLoaded();
    r.lock();
    try {
      if (!clusters.containsKey(clusterName)) {
        throw new ClusterNotFoundException(clusterName);
      }
      return clusters.get(clusterName);
    } finally {
      r.unlock();
    }
  }

  @Override
  public Cluster getClusterById(long id) throws AmbariException {
    checkLoaded();
    r.lock();
    try {
      if (!clustersById.containsKey(id)) {
        throw new ClusterNotFoundException("clusterID=" + id);
      }
      return clustersById.get(id);
    } finally {
      r.unlock();
    }
  }

  @Override
  public void setCurrentStackVersion(String clusterName, StackId stackId)
      throws AmbariException{
    if(stackId == null || clusterName == null || clusterName.isEmpty()){
      LOG.warn("Unable to set version for cluster " + clusterName);
      throw new AmbariException("Unable to set"
          + " version=" + stackId
          + " for cluster " + clusterName);
    }

    checkLoaded();
    r.lock();
    try {
      if (!clusters.containsKey(clusterName)) {
        throw new ClusterNotFoundException(clusterName);
      }
      Cluster cluster = clusters.get(clusterName);
      cluster.setCurrentStackVersion(stackId);
    } finally {
      r.unlock();
    }
  }

  @Override
  @Transactional
  public List<Host> getHosts() {
    checkLoaded();
    r.lock();

    try {
      List<Host> hostList = new ArrayList<Host>(hosts.size());
      hostList.addAll(hosts.values());
      return hostList;
    } finally {
      r.unlock();
    }
  }

  @Override
  public Set<Cluster> getClustersForHost(String hostname)
      throws AmbariException {
    checkLoaded();
    r.lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Looking up clusters for hostname"
            + ", hostname=" + hostname
            + ", mappedClusters=" + hostClusterMap.get(hostname).size());
      }
      return Collections.unmodifiableSet(hostClusterMap.get(hostname));
    } finally {
      r.unlock();
    }
  }

  @Override
  public Host getHost(String hostname) throws AmbariException {
    checkLoaded();
    r.lock();
    try {
      if (!hosts.containsKey(hostname)) {
        throw new HostNotFoundException(hostname);
      }
      return hosts.get(hostname);
    } finally {
      r.unlock();
    }
  }

  @Override
  public void addHost(String hostname) throws AmbariException {
    checkLoaded();
    String duplicateMessage = "Duplicate entry for Host"
        + ", hostName= " + hostname;

    if (hosts.containsKey(hostname)) {
      throw new AmbariException(duplicateMessage);
    }
    r.lock();

    try {
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
      hostClusterMap.put(hostname, Collections.newSetFromMap(new ConcurrentHashMap<Cluster, Boolean>()));

      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding a host to Clusters"
            + ", hostname=" + hostname);
      }
    } finally {
      r.unlock();
    }
  }

  private boolean isOsSupportedByClusterStack(Cluster c, Host h) throws AmbariException {
    Map<String, List<RepositoryInfo>> repos =
        ambariMetaInfo.getRepository(c.getDesiredStackVersion().getStackName(),
            c.getDesiredStackVersion().getStackVersion());
    if (repos == null || repos.isEmpty()) {
      return false;
    }
    return repos.containsKey(h.getOsType());
  }

  @Override
  @Transactional
  public void updateHostWithClusterAndAttributes(Map<String, Set<String>> hostClusters, Map<String,
      Map<String, String>> hostAttributes)
      throws AmbariException {
    checkLoaded();
    w.lock();
    try {
      if (hostClusters != null) {
        Map<String, Host> hostMap = getHostsMap(hostClusters.keySet());
        Set<String> clusterNames = new HashSet<String>();
        for (Set<String> cSet : hostClusters.values()) {
          clusterNames.addAll(cSet);
        }
        Map<String, Cluster> clusterMap = getClustersMap(clusterNames);

        for (String hostname : hostClusters.keySet()) {
          Host host = hostMap.get(hostname);
          Map<String, String>  attributes = hostAttributes.get(hostname);
          if (attributes != null && !attributes.isEmpty()){
            host.setHostAttributes(attributes);
          }

          Set<String> hostClusterNames = hostClusters.get(hostname);
          for (String clusterName : hostClusterNames) {
            if (clusterName != null && !clusterName.isEmpty()) {
              Cluster cluster = clusterMap.get(clusterName);

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

              if (LOG.isDebugEnabled()) {
                LOG.debug("Mapping a host to a cluster"
                    + ", clusterName=" + clusterName
                    + ", clusterId=" + cluster.getClusterId()
                    + ", hostname=" + hostname);
              }
            }
          }
        }
      }
    } finally {
      w.unlock();
    }
  }

  @Transactional
  private Map<String, Host> getHostsMap(Collection<String> hostSet) throws
      HostNotFoundException {
    checkLoaded();
    Map<String, Host> hostMap = new HashMap<String, Host>();
    r.lock();
    try {
      for (String host : hostSet) {
        if (!hosts.containsKey(host))
          throw new HostNotFoundException(host);
        else
          hostMap.put(host, hosts.get(host));
      }
    } finally {
      r.unlock();
    }
    return hostMap;
  }

  @Transactional
  private Map<String, Cluster> getClustersMap(Collection<String> clusterSet) throws
      ClusterNotFoundException {
    checkLoaded();
    Map<String, Cluster> clusterMap = new HashMap<String, Cluster>();
    r.lock();
    try {
      for (String c : clusterSet) {
        if (c != null) {
          if (!clusters.containsKey(c))
            throw new ClusterNotFoundException(c);
          else
            clusterMap.put(c, clusters.get(c));
        }
      }
    } finally {
      r.unlock();
    }
    return clusterMap;
  }

  @Override
  public void mapHostToCluster(String hostname,
                               String clusterName) throws AmbariException {
    checkLoaded();
    w.lock();

    try {
      Host host = getHost(hostname);
      Cluster cluster = getCluster(clusterName);

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

      if (LOG.isDebugEnabled()) {
        LOG.debug("Mapping a host to a cluster"
            + ", clusterName=" + clusterName
            + ", clusterId=" + cluster.getClusterId()
            + ", hostname=" + hostname);
      }
    } finally {
      w.unlock();
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
  public Map<String, Cluster> getClusters() {
    checkLoaded();
    r.lock();
    try {
      return Collections.unmodifiableMap(clusters);
    } finally {
      r.unlock();
    }
  }

  @Override
  public void mapHostsToCluster(Set<String> hostnames,
                                             String clusterName) throws AmbariException {
    checkLoaded();
    w.lock();
    try {
      for (String hostname : hostnames) {
        mapHostToCluster(hostname, clusterName);
      }
    } finally {
      w.unlock();
    }
  }

  @Override
  public void updateClusterName(String oldName, String newName) {
    w.lock();
    try {
      clusters.put(newName, clusters.remove(oldName));
      clusterHostMap.put(newName, clusterHostMap.remove(oldName));
    } finally {
      w.unlock();
    }
  }


  public void debugDump(StringBuilder sb) {
    r.lock();
    try {
      sb.append("Clusters=[ ");
      boolean first = true;
      for (Cluster c : clusters.values()) {
        if (!first) {
          sb.append(" , ");
          first = false;
        }
        sb.append("\n  ");
        c.debugDump(sb);
        sb.append(" ");
      }
      sb.append(" ]");
    } finally {
      r.unlock();
    }
  }

  @Override
  public Map<String, Host> getHostsForCluster(String clusterName)
      throws AmbariException {
    if (!clustersLoaded) {
      checkLoaded();
    }
    r.lock();

    try {
      Map<String, Host> hosts = new HashMap<String, Host>();

      for (Host h : clusterHostMap.get(clusterName)) {
        hosts.put(h.getHostName(), h);
      }

      return hosts;
    } finally {
      r.unlock();
    }
  }

  @Override
  public void deleteCluster(String clusterName)
      throws AmbariException {
    checkLoaded();
    w.lock();
    try {
      Cluster cluster = getCluster(clusterName);
      if (!cluster.canBeRemoved()) {
        throw new AmbariException("Could not delete cluster"
            + ", clusterName=" + clusterName);
      }
      LOG.info("Deleting cluster " + cluster.getClusterName());
      cluster.delete();

      //clear maps
      for (Set<Cluster> clusterSet : hostClusterMap.values()) {
        clusterSet.remove(cluster);
      }
      clusterHostMap.remove(cluster.getClusterName());
      clusters.remove(clusterName);
    } finally {
      w.unlock();
    }
  }
  
  @Override
  public void unmapHostFromCluster(String hostname, String clusterName)
      throws AmbariException {

    checkLoaded();
    
    w.lock();

    try {
      Host host = getHost(hostname);
      Cluster cluster = getCluster(clusterName);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Unmapping a host from a cluster"
            + ", clusterName=" + clusterName
            + ", clusterId=" + cluster.getClusterId()
            + ", hostname=" + hostname);
      }
      
      unmapHostClusterEntities(hostname, cluster.getClusterId());

      hostClusterMap.get(hostname).remove(cluster);
      clusterHostMap.get(clusterName).remove(host);

      deleteConfigGroupHostMapping(hostname);

    } finally {
      w.unlock();
    }
    
  }
  
  @Transactional
  private void unmapHostClusterEntities(String hostName, long clusterId) {
    HostEntity hostEntity = hostDAO.findByName(hostName);
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);

    hostEntity.getClusterEntities().remove(clusterEntity);
    clusterEntity.getHostEntities().remove(hostEntity);

    hostDAO.merge(hostEntity);
    clusterDAO.merge(clusterEntity);
  }

  @Transactional
  private void deleteConfigGroupHostMapping(String hostname) throws AmbariException {
    // Remove Config group mapping
    for (Cluster cluster : clusters.values()) {
      for (ConfigGroup configGroup : cluster.getConfigGroups().values()) {
        configGroup.removeHost(hostname);
      }
    }
  }
  
  @Override
  public void deleteHost(String hostname) throws AmbariException {
    checkLoaded();

    if (!hosts.containsKey(hostname))
      return;
    
    w.lock();

    try {
      HostEntity entity = hostDAO.findByName(hostname);
      hostDAO.refresh(entity);
      hostDAO.remove(entity);
      hosts.remove(hostname);
      deleteConfigGroupHostMapping(hostname);
    } catch (Exception e) {
      throw new AmbariException("Could not remove host", e);
    } finally {
      w.unlock();
    }
    
  }

}
