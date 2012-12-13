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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;

public class ClusterImpl implements Cluster {

  private static final Logger LOG =
      LoggerFactory.getLogger(ClusterImpl.class);

  @Inject
  private Clusters clusters;

  private StackId desiredStackVersion;
  private StackId desiredState;

  private Map<String, Service> services = null;

  /**
   * [ Config Type -> [ Config Version Tag -> Config ] ]
   */
  private Map<String, Map<String, Config>> configs;

  /**
   * [ ServiceName -> [ ServiceComponentName -> [ HostName -> [ ... ] ] ] ]
   */
  private Map<String, Map<String, Map<String, ServiceComponentHost>>>
      serviceComponentHosts;

  /**
   * [ HostName -> [ ... ] ]
   */
  private Map<String, List<ServiceComponentHost>>
      serviceComponentHostsByHost;

  private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private Lock readLock = readWriteLock.readLock();
  private Lock writeLock = readWriteLock.writeLock();

  private ClusterEntity clusterEntity;

  @Inject
  private ClusterDAO clusterDAO;
//  @Inject
//  private ClusterServiceDAO clusterServiceDAO;
  @Inject
  private ServiceFactory serviceFactory;
  @Inject
  private ConfigFactory configFactory;
  @Inject
  private Gson gson;

  @Inject
  public ClusterImpl(@Assisted ClusterEntity clusterEntity,
                     Injector injector) {
    injector.injectMembers(this);
    this.clusterEntity = clusterEntity;

    this.serviceComponentHosts = new HashMap<String,
        Map<String, Map<String, ServiceComponentHost>>>();
    this.serviceComponentHostsByHost = new HashMap<String,
        List<ServiceComponentHost>>();
    this.desiredStackVersion = gson.fromJson(clusterEntity.getDesiredStackVersion(), StackId.class);

    configs = new HashMap<String, Map<String, Config>>();
    if (!clusterEntity.getClusterConfigEntities().isEmpty()) {
      for (ClusterConfigEntity entity : clusterEntity.getClusterConfigEntities()) {

        if (!configs.containsKey(entity.getType())) {
          configs.put(entity.getType(), new HashMap<String, Config>());
        }

        Config config = configFactory.createExisting(this, entity);

        configs.get(entity.getType()).put(entity.getTag(), config);
      }
    }


  }

  private void loadServices() {
    if (services == null) {
      synchronized (this) {
        if (services == null) {
          services = new TreeMap<String, Service>();
          if (!clusterEntity.getClusterServiceEntities().isEmpty()) {
            for (ClusterServiceEntity serviceEntity : clusterEntity.getClusterServiceEntities()) {
              services.put(serviceEntity.getServiceName(), serviceFactory.createExisting(this, serviceEntity));
            }
          }
        }
      }
    }
  }

  public ServiceComponentHost getServiceComponentHost(String serviceName,
      String serviceComponentName, String hostname) throws AmbariException {
    if (!serviceComponentHosts.containsKey(serviceName)
        || !serviceComponentHosts.get(serviceName)
            .containsKey(serviceComponentName)
        || !serviceComponentHosts.get(serviceName).get(serviceComponentName)
            .containsKey(hostname)) {
      throw new ServiceComponentHostNotFoundException(getClusterName(), serviceName,
          serviceComponentName, hostname);
    }
    return serviceComponentHosts.get(serviceName).get(serviceComponentName)
        .get(hostname);
  }

  @Override
  public String getClusterName() {
    try {
      readLock.lock();
      return clusterEntity.getClusterName();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setClusterName(String clusterName) {
    try {
      writeLock.lock();
      String oldName = clusterEntity.getClusterName();
      clusterEntity.setClusterName(clusterName);
      clusterDAO.merge(clusterEntity); //RollbackException possibility if UNIQUE constraint violated
      clusters.updateClusterName(oldName, clusterName);
    } finally {
      writeLock.unlock();
    }
  }

  public synchronized void addServiceComponentHost(
      ServiceComponentHost svcCompHost) throws AmbariException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Trying to add ServiceComponentHost to ClusterHostMap cache"
          + ", serviceName=" + svcCompHost.getServiceName()
          + ", componentName=" + svcCompHost.getServiceComponentName()
          + ", hostname=" + svcCompHost.getHostName());
    }

    final String hostname = svcCompHost.getHostName();
    final String serviceName = svcCompHost.getServiceName();
    final String componentName = svcCompHost.getServiceComponentName();
    Set<Cluster> cs = clusters.getClustersForHost(hostname);
    boolean clusterFound = false;
    Iterator<Cluster> iter = cs.iterator();
    while (iter.hasNext()) {
      Cluster c = iter.next();
      if (c.getClusterId() == this.getClusterId()) {
        clusterFound = true;
        break;
      }
    }
    if (!clusterFound) {
      throw new AmbariException("Host does not belong this cluster"
              + ", hostname=" + hostname
              + ", clusterName=" + getClusterName()
              + ", clusterId=" + getClusterId());
    }

    if (!serviceComponentHosts.containsKey(serviceName)) {
      serviceComponentHosts.put(serviceName,
          new HashMap<String, Map<String,ServiceComponentHost>>());
    }
    if (!serviceComponentHosts.get(serviceName).containsKey(componentName)) {
      serviceComponentHosts.get(serviceName).put(componentName,
          new HashMap<String, ServiceComponentHost>());
    }

    if (serviceComponentHosts.get(serviceName).get(componentName).
        containsKey(hostname)) {
      throw new AmbariException("Duplicate entry for ServiceComponentHost"
          + ", serviceName=" + serviceName
          + ", serviceComponentName" + componentName
          + ", hostname= " + hostname);
    }

    if (!serviceComponentHostsByHost.containsKey(hostname)) {
      serviceComponentHostsByHost.put(hostname,
          new ArrayList<ServiceComponentHost>());
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a new ServiceComponentHost"
          + ", clusterName=" + getClusterName()
          + ", clusterId=" + getClusterId()
          + ", serviceName=" + serviceName
          + ", serviceComponentName" + componentName
          + ", hostname= " + hostname);
    }

    serviceComponentHosts.get(serviceName).get(componentName).put(hostname,
        svcCompHost);
    serviceComponentHostsByHost.get(hostname).add(svcCompHost);
  }

  @Override
  public long getClusterId() {
    return clusterEntity.getClusterId();
  }

  @Override
  public synchronized List<ServiceComponentHost> getServiceComponentHosts(
      String hostname) {
    if (serviceComponentHostsByHost.containsKey(hostname)) {
      return Collections.unmodifiableList(
          serviceComponentHostsByHost.get(hostname));
    }
    return new ArrayList<ServiceComponentHost>();
  }

  @Override
  public synchronized void addService(Service service)
      throws AmbariException {
    loadServices();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a new Service"
          + ", clusterName=" + getClusterName()
          + ", clusterId=" + getClusterId()
          + ", serviceName=" + service.getName());
    }
    if (services.containsKey(service.getName())) {
      throw new AmbariException("Service already exists"
          + ", clusterName=" + getClusterName()
          + ", clusterId=" + getClusterId()
          + ", serviceName=" + service.getName());
    }
    this.services.put(service.getName(), service);
  }

  @Override
  public synchronized Service addService(String serviceName) throws AmbariException{
    loadServices();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a new Service"
          + ", clusterName=" + getClusterName()
          + ", clusterId=" + getClusterId()
          + ", serviceName=" + serviceName);
    }
    if (services.containsKey(serviceName)) {
      throw new AmbariException("Service already exists"
          + ", clusterName=" + getClusterName()
          + ", clusterId=" + getClusterId()
          + ", serviceName=" + serviceName);
    }
    Service s = serviceFactory.createNew(this, serviceName);
    this.services.put(s.getName(), s);
    return s;
  }

  @Override
  public synchronized Service getService(String serviceName)
      throws AmbariException {
    loadServices();
    if (!services.containsKey(serviceName)) {
      throw new ServiceNotFoundException(getClusterName(), serviceName);
    }
    return services.get(serviceName);
  }

  @Override
  public synchronized Map<String, Service> getServices() {
    loadServices();
    return Collections.unmodifiableMap(services);
  }

  @Override
  public synchronized StackId getDesiredStackVersion() {
    return desiredStackVersion;
  }

  @Override
  public synchronized void setDesiredStackVersion(StackId stackVersion) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Changing DesiredStackVersion of Cluster"
        + ", clusterName=" + getClusterName()
        + ", clusterId=" + getClusterId()
        + ", currentDesiredStackVersion=" + this.desiredStackVersion
        + ", newDesiredStackVersion=" + stackVersion);
    }
    this.desiredStackVersion = stackVersion;
    clusterEntity.setDesiredStackVersion(gson.toJson(stackVersion));
    clusterDAO.merge(clusterEntity);
  }

  public synchronized StackId getDesiredState() {
    //TODO separate implementation, mapped to StackVersion for now
//    return desiredState; for separate implementation
    return getDesiredStackVersion();
  }

  public synchronized void setDesiredState(StackId desiredState) {
    //TODO separate implementation, mapped to StackVersion for now
//    LOG.debug("Changing desired state of cluster, clusterName={}, clusterId={}, oldState={}, newState={}",
//        getClusterName(), getClusterId(), this.desiredState, desiredState);
//    clusterEntity.setDesiredClusterState(gson.toJson(desiredState));
//    clusterDAO.merge(clusterEntity);
//    this.desiredState = desiredState;
    setDesiredStackVersion(desiredState);
  }


  @Override
  public synchronized Map<String, Config> getDesiredConfigsByType(String configType) {
    if (!configs.containsKey(configType))
      return null;

    return Collections.unmodifiableMap(configs.get(configType));
  }

  @Override
  public synchronized Config getDesiredConfig(String configType, String versionTag) {
    if (!configs.containsKey(configType)
        || !configs.get(configType).containsKey(versionTag)) {
      return null;
    }
    return configs.get(configType).get(versionTag);
  }

  @Override
  public synchronized void addDesiredConfig(Config config) {
    if (config.getType() == null
        || config.getType().isEmpty()
        || config.getVersionTag() == null
        || config.getVersionTag().isEmpty()) {
      // TODO throw error
    }
    if (!configs.containsKey(config.getType())) {
      configs.put(config.getType(), new HashMap<String, Config>());
    }

    configs.get(config.getType()).put(config.getVersionTag(), config);
  }

  public synchronized Collection<Config> getAllConfigs() {
    List<Config> list = new ArrayList<Config>();
    for (Entry<String,Map<String,Config>> entry : configs.entrySet()) {
      for (Config config : entry.getValue().values()) {
        list.add(config);
      }
    }
    return Collections.unmodifiableList(list);
  }

  @Override
  public synchronized ClusterResponse convertToResponse()
      throws AmbariException {
    ClusterResponse r = new ClusterResponse(getClusterId(), getClusterName(),
        clusters.getHostsForCluster(getClusterName()).keySet(),
        getDesiredStackVersion().getStackId());
    return r;
  }

  public synchronized void debugDump(StringBuilder sb) {
    loadServices();
    sb.append("Cluster={ clusterName=" + getClusterName()
        + ", clusterId=" + getClusterId()
        + ", desiredStackVersion=" + desiredStackVersion.getStackId()
        + ", services=[ ");
    boolean first = true;
    for(Service s : services.values()) {
      if (!first) {
        sb.append(" , ");
        first = false;
      }
      sb.append("\n    ");
      s.debugDump(sb);
      sb.append(" ");
    }
    sb.append(" ] }");
  }

  @Override
  @Transactional
  public synchronized void refresh() {
    clusterEntity = clusterDAO.findById(clusterEntity.getClusterId());
    clusterDAO.refresh(clusterEntity);
  }

  @Override
  public synchronized void deleteAllServices() throws AmbariException {
    loadServices();
    LOG.info("Deleting all services for cluster"
        + ", clusterName=" + getClusterName());
    for (Service service : services.values()) {
      if (!service.canBeRemoved()) {
        throw new AmbariException("Found non removable service when trying to"
            + " all services from cluster"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + service.getName());
      }
    }
    for (Service service : services.values()) {
      service.removeAllComponents();
    }
    services.clear();
    // FIXME update DB
  }

  @Override
  public synchronized void deleteService(String serviceName)
      throws AmbariException {
    loadServices();
    Service service = getService(serviceName);
    LOG.info("Deleting service for cluster"
        + ", clusterName=" + getClusterName()
        + ", serviceName=" + service.getName());
    // FIXME check dependencies from meta layer
    if (!service.canBeRemoved()) {
      throw new AmbariException("Could not delete service from cluster"
          + ", clusterName=" + getClusterName()
          + ", serviceName=" + service.getName());
    }
    service.removeAllComponents();
    services.remove(serviceName);
    // FIXME update DB
  }

  @Override
  public boolean canBeRemoved() {
    loadServices();
    boolean safeToRemove = true;
    for (Service service : services.values()) {
      if (!service.canBeRemoved()) {
        safeToRemove = false;
        LOG.warn("Found non removable service"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + service.getName());
      }
    }
    return safeToRemove;
  }
}
