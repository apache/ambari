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

import java.text.MessageFormat;
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

import javax.persistence.RollbackException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.HostAddedEvent;
import org.apache.ambari.server.events.HostRegisteredEvent;
import org.apache.ambari.server.events.HostRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalHostDAO;
import org.apache.ambari.server.orm.dao.RequestOperationLevelDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.TopologyHostInfoDAO;
import org.apache.ambari.server.orm.dao.TopologyHostRequestDAO;
import org.apache.ambari.server.orm.dao.TopologyLogicalTaskDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalTaskEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.utils.RetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ClustersImpl implements Clusters {

  private static final Logger LOG = LoggerFactory.getLogger(
      ClustersImpl.class);

  private ConcurrentHashMap<String, Cluster> clusters;
  private ConcurrentHashMap<Long, Cluster> clustersById;
  private ConcurrentHashMap<String, Host> hosts;
  private ConcurrentHashMap<Long, Host> hostsById;
  private ConcurrentHashMap<String, Set<Cluster>> hostClusterMap;
  private ConcurrentHashMap<String, Set<Host>> clusterHostMap;

  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  private final Lock r = rwl.readLock();
  private final Lock w = rwl.writeLock();

  private volatile boolean clustersLoaded = false;

  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private HostDAO hostDAO;
  @Inject
  private ClusterVersionDAO clusterVersionDAO;
  @Inject
  private HostVersionDAO hostVersionDAO;
  @Inject
  private HostStateDAO hostStateDAO;
  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;
  @Inject
  private ResourceTypeDAO resourceTypeDAO;
  @Inject
  private RequestOperationLevelDAO requestOperationLevelDAO;
  @Inject
  private KerberosPrincipalHostDAO kerberosPrincipalHostDAO;
  @Inject
  private HostConfigMappingDAO hostConfigMappingDAO;
  @Inject
  private ServiceConfigDAO serviceConfigDAO;
  @Inject
  private ClusterFactory clusterFactory;
  @Inject
  private HostFactory hostFactory;
  @Inject
  private Configuration configuration;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  @Inject
  private SecurityHelper securityHelper;
  @Inject
  private TopologyLogicalTaskDAO topologyLogicalTaskDAO;
  @Inject
  private TopologyHostRequestDAO topologyHostRequestDAO;
  @Inject
  private TopologyRequestDAO topologyRequestDAO;
  @Inject
  private TopologyHostInfoDAO topologyHostInfoDAO;

  /**
   * Data access object for stacks.
   */
  @Inject
  private StackDAO stackDAO;

  /**
   * Used to publish events relating to cluster CRUD operations.
   */
  @Inject
  private AmbariEventPublisher eventPublisher;

  @Inject
  public ClustersImpl() {
    clusters = new ConcurrentHashMap<String, Cluster>();
    clustersById = new ConcurrentHashMap<Long, Cluster>();
    hosts = new ConcurrentHashMap<String, Host>();
    hostsById = new ConcurrentHashMap<Long, Host>();
    hostClusterMap = new ConcurrentHashMap<String, Set<Cluster>>();
    clusterHostMap = new ConcurrentHashMap<String, Set<Host>>();

    LOG.info("Initializing the ClustersImpl");
  }

  private void checkLoaded() {
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
      hostsById.put(hostEntity.getHostId(), host);
      Set<Cluster> cSet = Collections.newSetFromMap(new ConcurrentHashMap<Cluster, Boolean>());
      hostClusterMap.put(hostEntity.getHostName(), cSet);

      for (ClusterEntity clusterEntity : hostEntity.getClusterEntities()) {
        clusterHostMap.get(clusterEntity.getClusterName()).add(host);
        cSet.add(clusters.get(clusterEntity.getClusterName()));
      }
    }
  }

  @Override
  public void addCluster(String clusterName, StackId stackId)
    throws AmbariException {
    addCluster(clusterName, stackId, null);
  }

  @Override
  public void addCluster(String clusterName, StackId stackId, SecurityType securityType)
      throws AmbariException {
    checkLoaded();

    Cluster cluster = null;

    w.lock();
    try {
      if (clusters.containsKey(clusterName)) {
        throw new DuplicateResourceException("Attempted to create a Cluster which already exists"
            + ", clusterName=" + clusterName);
      }

      // create an admin resource to represent this cluster
      ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
      if (resourceTypeEntity == null) {
        resourceTypeEntity = new ResourceTypeEntity();
        resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
        resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
        resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
      }

      ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setResourceType(resourceTypeEntity);

      StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
          stackId.getStackVersion());

      // retrieve new cluster id
      // add cluster id -> cluster mapping into clustersById
      ClusterEntity clusterEntity = new ClusterEntity();
      clusterEntity.setClusterName(clusterName);
      clusterEntity.setDesiredStack(stackEntity);
      clusterEntity.setResource(resourceEntity);
      if (securityType != null) {
        clusterEntity.setSecurityType(securityType);
      }

      try {
        clusterDAO.create(clusterEntity);
        clusterEntity = clusterDAO.merge(clusterEntity);
      } catch (RollbackException e) {
        LOG.warn("Unable to create cluster " + clusterName, e);
        throw new AmbariException("Unable to create cluster " + clusterName, e);
      }

      cluster = clusterFactory.create(clusterEntity);
      clusters.put(clusterName, cluster);
      clustersById.put(cluster.getClusterId(), cluster);
      clusterHostMap.put(clusterName, new HashSet<Host>());
    } finally {
      w.unlock();
    }

    cluster.setCurrentStackVersion(stackId);
  }

  @Override
  public Cluster getCluster(String clusterName)
      throws AmbariException {
    checkLoaded();

    Cluster cluster = null;
    if (clusterName != null) {
      cluster = clusters.get(clusterName);
    }
    if (null == cluster) {
      throw new ClusterNotFoundException(clusterName);
    }
    RetryHelper.addAffectedCluster(cluster);
    return cluster;
  }

  @Override
  public Cluster getClusterById(long id) throws AmbariException {
    checkLoaded();

    Cluster cluster = clustersById.get(id);
    if (null == cluster) {
      throw new ClusterNotFoundException("clusterID=" + id);
    }

    return clustersById.get(id);
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

    Cluster cluster = null;

    r.lock();
    try {
      if (!clusters.containsKey(clusterName)) {
        throw new ClusterNotFoundException(clusterName);
      }

      cluster = clusters.get(clusterName);
    } finally {
      r.unlock();
    }

    cluster.setCurrentStackVersion(stackId);
  }

  @Override
  public List<Host> getHosts() {
    checkLoaded();

    return new ArrayList<Host>(hosts.values());
  }

  @Override
  public Set<Cluster> getClustersForHost(String hostname)
      throws AmbariException {
    checkLoaded();
    Set<Cluster> clusters = hostClusterMap.get(hostname);
    if(clusters == null){
      throw new HostNotFoundException(hostname);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Looking up clusters for hostname"
          + ", hostname=" + hostname
          + ", mappedClusters=" + clusters.size());
    }
    return Collections.unmodifiableSet(clusters);

  }

  @Override
  public Host getHost(String hostname) throws AmbariException {
    checkLoaded();

    if (!hosts.containsKey(hostname)) {
      throw new HostNotFoundException(hostname);
    }

    return hosts.get(hostname);
  }

  @Override
  public boolean hostExists(String hostname){
    checkLoaded();

    return hosts.containsKey(hostname);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isHostMappedToCluster(String clusterName, String hostName) {
    checkLoaded();

    Set<Cluster> clusters = hostClusterMap.get(hostName);
    for (Cluster cluster : clusters) {
      if (clusterName.equals(cluster.getClusterName())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Host getHostById(Long hostId) throws AmbariException {
    checkLoaded();

    if (!hostsById.containsKey(hostId)) {
      throw new HostNotFoundException("Host Id = " + hostId);
    }

    return hostsById.get(hostId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateHostMappings(Host host) {
    Long hostId = host.getHostId();

    if (null != hostId) {
      hostsById.put(hostId, host);
    }
  }

  /**
   * Register a host by creating a {@link HostEntity} object in the database and setting its state to
   * {@link HostState#INIT}. This does not add the host the cluster.
   * @param hostname Host to be added
   * @throws AmbariException
   */
  @Override
  public void addHost(String hostname) throws AmbariException {
    checkLoaded();

    if (hosts.containsKey(hostname)) {
      throw new AmbariException(MessageFormat.format("Duplicate entry for Host {0}", hostname));
    }

    w.lock();

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

      // the hosts by ID map is updated separately since the host has not yet
      // been persisted yet - the below event is what causes the persist
      hosts.put(hostname, host);

      hostClusterMap.put(hostname, Collections.newSetFromMap(new ConcurrentHashMap<Cluster, Boolean>()));

      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding a host to Clusters"
            + ", hostname=" + hostname);
      }
    } finally {
      w.unlock();
    }

    // publish the event
    HostRegisteredEvent event = new HostRegisteredEvent(hostname);
    eventPublisher.publish(event);
  }

  private boolean isOsSupportedByClusterStack(Cluster c, Host h) throws AmbariException {
    Map<String, List<RepositoryInfo>> repos =
        ambariMetaInfo.getRepository(c.getDesiredStackVersion().getStackName(),
            c.getDesiredStackVersion().getStackVersion());
    return !(repos == null || repos.isEmpty()) && repos.containsKey(h.getOsFamily());
  }

  @Override
  public void updateHostWithClusterAndAttributes(
      Map<String, Set<String>> hostClusters,
      Map<String, Map<String, String>> hostAttributes) throws AmbariException {
    checkLoaded();
    w.lock();

    try {
      if (hostClusters != null) {
        Map<String, Host> hostMap = getHostsMap(hostClusters.keySet());
        Set<String> clusterNames = new HashSet<String>();
        for (Set<String> cSet : hostClusters.values()) {
          clusterNames.addAll(cSet);
        }

        for (String hostname : hostClusters.keySet()) {
          Host host = hostMap.get(hostname);
          Map<String, String>  attributes = hostAttributes.get(hostname);
          if (attributes != null && !attributes.isEmpty()){
            host.setHostAttributes(attributes);
          }

          host.refresh();

          Set<String> hostClusterNames = hostClusters.get(hostname);
          for (String clusterName : hostClusterNames) {
            if (clusterName != null && !clusterName.isEmpty()) {
              mapHostToCluster(hostname, clusterName);
            }
          }
        }
      }
    } finally {
      w.unlock();
    }
  }

  private Map<String, Host> getHostsMap(Collection<String> hostSet) throws
      HostNotFoundException {
    checkLoaded();

    Map<String, Host> hostMap = new HashMap<String, Host>();
    Host host = null;
    for (String hostName : hostSet) {
      if (null != hostName) {
          host= hosts.get(hostName);
        if (host == null) {
          throw new HostNotFoundException(hostName);
        }
      } else {
        throw new HostNotFoundException(hostName);
      }

      hostMap.put(hostName, host);
    }

    return hostMap;
  }

  /**
   *  For each host, attempts to map it to the cluster, and apply the cluster's current version to the host.
   * @param hostnames Collection of host names
   * @param clusterName Cluster name
   * @throws AmbariException
   */
  @Override
  public void mapHostsToCluster(Set<String> hostnames, String clusterName) throws AmbariException {
    checkLoaded();
    w.lock();
    try {
      ClusterVersionEntity clusterVersionEntity = clusterVersionDAO.findByClusterAndStateCurrent(clusterName);
      for (String hostname : hostnames) {
        mapHostToCluster(hostname, clusterName, clusterVersionEntity);
      }
    } finally {
      w.unlock();
    }
  }

  /**
   * Attempts to map the host to the cluster via clusterhostmapping table if not already present, and add a host_version
   * record for the cluster's currently applied (stack, version) if not already present.
   * @param hostname Host name
   * @param clusterName Cluster name
   * @param currentClusterVersion Cluster's current stack version
   * @throws AmbariException May throw a DuplicateResourceException.
   */
  public void mapHostToCluster(String hostname, String clusterName,
      ClusterVersionEntity currentClusterVersion) throws AmbariException {
    Host host = null;
    Cluster cluster = null;

    checkLoaded();

    r.lock();
    try {
      host = getHost(hostname);
      cluster = getCluster(clusterName);

      // check to ensure there are no duplicates
      for (Cluster c : hostClusterMap.get(hostname)) {
        if (c.getClusterName().equals(clusterName)) {
          throw new DuplicateResourceException("Attempted to create a host which already exists: clusterName=" +
              clusterName + ", hostName=" + hostname);
        }
      }
    } finally {
      r.unlock();
    }

    if (!isOsSupportedByClusterStack(cluster, host)) {
      String message = "Trying to map host to cluster where stack does not"
          + " support host's os type" + ", clusterName=" + clusterName
          + ", clusterStackId=" + cluster.getDesiredStackVersion().getStackId()
          + ", hostname=" + hostname + ", hostOsFamily=" + host.getOsFamily();
      LOG.warn(message);
      throw new AmbariException(message);
    }

    long clusterId = cluster.getClusterId();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Mapping host {} to cluster {} (id={})", hostname, clusterName,
          clusterId);
    }

    w.lock();
    try {
      mapHostClusterEntities(hostname, clusterId);
      hostClusterMap.get(hostname).add(cluster);
      clusterHostMap.get(clusterName).add(host);
    } finally {
      w.unlock();
    }

    cluster.refresh();
    host.refresh();
  }

  /**
   * Attempts to map the host to the cluster via clusterhostmapping table if not already present, and add a host_version
   * record for the cluster's currently applied (stack, version) if not already present. This function is overloaded.
   * @param hostname Host name
   * @param clusterName Cluster name
   * @throws AmbariException May throw a DuplicateResourceException.
   */
  @Override
  public void mapHostToCluster(String hostname, String clusterName)
      throws AmbariException {
    checkLoaded();

    ClusterVersionEntity clusterVersionEntity = clusterVersionDAO.findByClusterAndStateCurrent(clusterName);
    mapHostToCluster(hostname, clusterName, clusterVersionEntity);
  }

  @Transactional
  void mapHostClusterEntities(String hostName, Long clusterId) {
    HostEntity hostEntity = hostDAO.findByName(hostName);
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);

    hostEntity.getClusterEntities().add(clusterEntity);
    clusterEntity.getHostEntities().add(hostEntity);

    clusterDAO.merge(clusterEntity);
    hostDAO.merge(hostEntity);

    // publish the event for adding a host to a cluster
    HostAddedEvent event = new HostAddedEvent(clusterId, hostName);
    eventPublisher.publish(event);
  }

  @Override
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
  public void updateClusterName(String oldName, String newName) {
    w.lock();
    try {
      clusters.put(newName, clusters.remove(oldName));
      clusterHostMap.put(newName, clusterHostMap.remove(oldName));
    } finally {
      w.unlock();
    }
  }


  @Override
  public void debugDump(StringBuilder sb) {
    r.lock();
    try {
      sb.append("Clusters=[ ");
      boolean first = true;
      for (Cluster c : clusters.values()) {
        if (!first) {
          sb.append(" , ");
        }
        first = false;
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

    checkLoaded();
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
  public Map<Long, Host> getHostIdsForCluster(String clusterName)
      throws AmbariException {

    checkLoaded();
    r.lock();

    try {
      Map<Long, Host> hosts = new HashMap<Long, Host>();

      for (Host h : clusterHostMap.get(clusterName)) {
        HostEntity hostEntity = hostDAO.findByName(h.getHostName());
        hosts.put(hostEntity.getHostId(), h);
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

      Collection<ClusterVersionEntity> clusterVersions = cluster.getAllClusterVersions();
      for (ClusterVersionEntity clusterVersion : clusterVersions) {
        clusterVersionDAO.remove(clusterVersion);
      }

      clusters.remove(clusterName);
    } finally {
      w.unlock();
    }
  }

  @Override
  public void unmapHostFromCluster(String hostname, String clusterName) throws AmbariException {
    final Cluster cluster = getCluster(clusterName);
    unmapHostFromClusters(hostname, Sets.newHashSet(cluster));
  }

  @Transactional
  void unmapHostFromClusters(String hostname, Set<Cluster> clusters) throws AmbariException {
    Host host = null;
    HostEntity hostEntity = null;

    checkLoaded();
    if (clusters.isEmpty()) {
      return;
    }

    r.lock();
    try {
      host = getHost(hostname);
      hostEntity = hostDAO.findByName(hostname);
    } finally {
      r.unlock();
    }

    w.lock();
    try {
      for (Cluster cluster : clusters) {
        long clusterId = cluster.getClusterId();
        if (LOG.isDebugEnabled()) {
          LOG.debug("Unmapping host {} from cluster {} (id={})", hostname,
              cluster.getClusterName(), clusterId);
        }

        unmapHostClusterEntities(hostname, cluster.getClusterId());

        hostClusterMap.get(hostname).remove(cluster);
        clusterHostMap.get(cluster.getClusterName()).remove(host);

        host.refresh();
        cluster.refresh();
      }

      deleteConfigGroupHostMapping(hostEntity.getHostId());

      // Remove mapping of principals to the unmapped host
      kerberosPrincipalHostDAO.removeByHost(hostEntity.getHostId());
    } finally {
      w.unlock();
    }
  }

  @Transactional
  void unmapHostClusterEntities(String hostName, long clusterId) {
    HostEntity hostEntity = hostDAO.findByName(hostName);
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);

    hostEntity.getClusterEntities().remove(clusterEntity);
    clusterEntity.getHostEntities().remove(hostEntity);

    hostDAO.merge(hostEntity);
    clusterDAO.merge(clusterEntity, true);
  }

  @Transactional
  void deleteConfigGroupHostMapping(Long hostId) throws AmbariException {
    // Remove Config group mapping
    for (Cluster cluster : clusters.values()) {
      for (ConfigGroup configGroup : cluster.getConfigGroups().values()) {
        configGroup.removeHost(hostId);
      }
    }
  }

  /***
   * Delete a host entirely from the cluster and all database tables, except
   * AlertHistory. If the host is not found, throws
   * {@link org.apache.ambari.server.HostNotFoundException}.
   * <p/>
   * This method will trigger a {@link HostRemovedEvent} when completed.
   *
   * @param hostname
   * @throws AmbariException
   */
  @Override
  public void deleteHost(String hostname) throws AmbariException {
    // unmapping hosts from a cluster modifies the collections directly; keep
    // a copy of this to ensure that we can pass in the original set of
    // clusters that the host belonged to to the host removal event
    Set<Cluster> clusters = hostClusterMap.get(hostname);
    Set<Cluster> hostsClusters = new HashSet<>(clusters);

    deleteHostEntityRelationships(hostname);

    // Publish the event, using the original list of clusters that the host
    // belonged to
    HostRemovedEvent event = new HostRemovedEvent(hostname, hostsClusters);
    eventPublisher.publish(event);
  }

  /***
   * Deletes all of the JPA relationships between a host and other entities.
   * This method will not fire {@link HostRemovedEvent} since it is performed
   * within an {@link Transactional} and the event must fire after the
   * transaction is successfully committed.
   *
   * @param hostname
   * @throws AmbariException
   */
  @Transactional
  void deleteHostEntityRelationships(String hostname) throws AmbariException {
    checkLoaded();

    if (!hosts.containsKey(hostname)) {
      throw new HostNotFoundException("Could not find host " + hostname);
    }

    w.lock();

    try {
      HostEntity entity = hostDAO.findByName(hostname);

      if (entity == null) {
        return;
      }
      // Remove from all clusters in the cluster_host_mapping table.
      // This will also remove from kerberos_principal_hosts, hostconfigmapping, and configgrouphostmapping
      Set<Cluster> clusters = hostClusterMap.get(hostname);
      Set<Long> clusterIds = Sets.newHashSet();
      for (Cluster cluster: clusters) {
        clusterIds.add(cluster.getClusterId());
      }




      unmapHostFromClusters(hostname, clusters);
      hostDAO.refresh(entity);

      hostVersionDAO.removeByHostName(hostname);

      // Remove blueprint tasks before hostRoleCommands
      // TopologyLogicalTask owns the OneToOne relationship but Cascade is on HostRoleCommandEntity
      if (entity.getHostRoleCommandEntities() != null) {
        for (HostRoleCommandEntity hrcEntity : entity.getHostRoleCommandEntities()) {
          TopologyLogicalTaskEntity topologyLogicalTaskEnity = hrcEntity.getTopologyLogicalTaskEntity();
          if (topologyLogicalTaskEnity != null) {
            topologyLogicalTaskDAO.remove(topologyLogicalTaskEnity);
            hrcEntity.setTopologyLogicalTaskEntity(null);
          }
        }
      }
      for (Long clusterId: clusterIds) {
        for (TopologyRequestEntity topologyRequestEntity: topologyRequestDAO.findByClusterId(clusterId)) {
          TopologyLogicalRequestEntity topologyLogicalRequestEntity = topologyRequestEntity.getTopologyLogicalRequestEntity();

          for (TopologyHostRequestEntity topologyHostRequestEntity: topologyLogicalRequestEntity.getTopologyHostRequestEntities()) {
            if (hostname.equals(topologyHostRequestEntity.getHostName())) {
              topologyHostRequestDAO.remove(topologyHostRequestEntity);
            }
          }
        }
      }


      entity.setHostRoleCommandEntities(null);
      hostRoleCommandDAO.removeByHostId(entity.getHostId());

      entity.setHostStateEntity(null);
      hostStateDAO.removeByHostId(entity.getHostId());
      hostConfigMappingDAO.removeByHostId(entity.getHostId());
      serviceConfigDAO.removeHostFromServiceConfigs(entity.getHostId());
      requestOperationLevelDAO.removeByHostId(entity.getHostId());
      topologyHostInfoDAO.removeByHost(entity);

      // Remove from dictionaries
      hosts.remove(hostname);
      hostsById.remove(entity.getHostId());

      hostDAO.remove(entity);

      // Note, if the host is still heartbeating, then new records will be re-inserted
      // into the hosts and hoststate tables
    } catch (Exception e) {
      throw new AmbariException("Could not remove host", e);
    } finally {
      w.unlock();
    }
  }

  @Override
  public boolean checkPermission(String clusterName, boolean readOnly) {

    Cluster cluster = findCluster(clusterName);

    return (cluster == null && readOnly) || !configuration.getApiAuthentication()
      || checkPermission(cluster, readOnly);
  }

  @Override
  public void addSessionAttributes(String name, Map<String, Object> attributes) {
    Cluster cluster = findCluster(name);
    if (cluster != null) {
      cluster.addSessionAttributes(attributes);
    }
  }

  @Override
  public Map<String, Object> getSessionAttributes(String name) {
    Cluster cluster = findCluster(name);
    return cluster == null ? Collections.<String, Object>emptyMap() : cluster.getSessionAttributes();
  }


  // ----- helper methods ---------------------------------------------------

  /**
   * Find the cluster for the given name.
   *
   * @param name  the cluster name
   *
   * @return the cluster for the given name; null if the cluster can not be found
   */
  protected Cluster findCluster(String name) {
    Cluster cluster = null;
    try {
      cluster = name == null ? null : getCluster(name);
    } catch (AmbariException e) {
      // do nothing
    }
    return cluster;
  }

  /**
   * Determine whether or not access to the given cluster resource should be allowed based
   * on the privileges of the current user.
   *
   * @param cluster   the cluster
   * @param readOnly  indicate whether or not this check is for a read only operation
   *
   * @return true if the access to this cluster is allowed
   */
  private boolean checkPermission(Cluster cluster, boolean readOnly) {
    for (GrantedAuthority grantedAuthority : securityHelper.getCurrentAuthorities()) {
      if (grantedAuthority instanceof AmbariGrantedAuthority) {

        AmbariGrantedAuthority authority       = (AmbariGrantedAuthority) grantedAuthority;
        PrivilegeEntity        privilegeEntity = authority.getPrivilegeEntity();
        Integer                permissionId    = privilegeEntity.getPermission().getId();

        // admin has full access
        if (permissionId.equals(PermissionEntity.AMBARI_ADMIN_PERMISSION)) {
          return true;
        }
        if (cluster != null) {
          if (cluster.checkPermission(privilegeEntity, readOnly)) {
            return true;
          }
        }
      }
    }
    // TODO : should we log this?
    return false;
  }
}
