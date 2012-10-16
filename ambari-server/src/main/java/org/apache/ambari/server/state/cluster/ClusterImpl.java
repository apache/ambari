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
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

public class ClusterImpl implements Cluster {

  private static final Logger LOG =
      LoggerFactory.getLogger(ClusterImpl.class);

  @Inject
  private Clusters clusters;

  private StackVersion desiredStackVersion;

  private Map<String, Service> services = new TreeMap<String, Service>();

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
  private final Injector injector;


  private ClusterEntity clusterEntity;

  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private ClusterServiceDAO clusterServiceDAO;
  @Inject
  private ServiceFactory serviceFactory;

  @Inject
  public ClusterImpl(@Assisted ClusterEntity clusterEntity,
                     Injector injector) {
    this.injector = injector;
    injector.injectMembers(this);

    this.clusterEntity = clusterDAO.merge(clusterEntity);

    if (!clusterEntity.getClusterServiceEntities().isEmpty()) {
      for (ClusterServiceEntity serviceEntity : clusterEntity.getClusterServiceEntities()) {
        services.put(serviceEntity.getServiceName(), serviceFactory.createExisting(this, serviceEntity));
      }
    }

    this.serviceComponentHosts = new HashMap<String,
        Map<String,Map<String,ServiceComponentHost>>>();
    this.serviceComponentHostsByHost = new HashMap<String,
        List<ServiceComponentHost>>();
    this.desiredStackVersion = new StackVersion("");
    this.configs = new HashMap<String, Map<String,Config>>();
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

  synchronized void addServiceComponentHost(ServiceComponentHost svcCompHost)
      throws AmbariException {
    final String hostname = svcCompHost.getHostName();
    final String serviceName = svcCompHost.getServiceName();
    final String componentName = svcCompHost.getServiceComponentName();
    Set<Cluster> cs = clusters.getClustersForHost(hostname);
    boolean clusterFound = false;
    for (Cluster c = cs.iterator().next(); ; cs.iterator().hasNext()) {
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
    return Collections.unmodifiableList(
        serviceComponentHostsByHost.get(hostname));
  }

  @Override
  public synchronized void addService(Service service)
      throws AmbariException {
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
    if (!services.containsKey(serviceName)) {
      throw new ServiceNotFoundException(getClusterName(), serviceName);
    }
    return services.get(serviceName);
  }

  @Override
  public synchronized Map<String, Service> getServices() {
    return Collections.unmodifiableMap(services);
  }

  @Override
  public synchronized StackVersion getDesiredStackVersion() {
    return desiredStackVersion;
  }

  @Override
  public synchronized void setDesiredStackVersion(StackVersion stackVersion) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Changing DesiredStackVersion of Cluster"
        + ", clusterName=" + getClusterName()
        + ", clusterId=" + getClusterId()
        + ", currentDesiredStackVersion=" + this.desiredStackVersion
        + ", newDesiredStackVersion=" + stackVersion);
    }
    this.desiredStackVersion = stackVersion;
  }

  @Override
  public synchronized Map<String, Config> getConfigsByType(String configType) {
    return Collections.unmodifiableMap(configs.get(configType));
  }

  @Override
  public synchronized Config getConfig(String configType, String versionTag) {
    if (!configs.containsKey(configType)
        || !configs.get(configType).containsKey(versionTag)) {
      // TODO throw error
    }
    return configs.get(configType).get(versionTag);
  }

  @Override
  public synchronized void addConfig(Config config) {
    if (config.getType() == null
        || config.getType().isEmpty()
        || config.getVersionTag() == null
        || config.getVersionTag().isEmpty()) {
      // TODO throw error
    }
    if (!configs.containsKey(config.getType())) {
      configs.put(config.getType(), new HashMap<String, Config>());
    }

    // TODO should we check for duplicates and throw an error?
    // if (configs.get(config.getType()).containsKey(config.getVersionTag()))

    configs.get(config.getType()).put(config.getVersionTag(), config);
  }

  @Override
  public synchronized ClusterResponse convertToResponse() {
    ClusterResponse r = new ClusterResponse(getClusterId(), getClusterName(),
        new HashSet<String>(serviceComponentHostsByHost.keySet()));
    return r;
  }

  public void debugDump(StringBuilder sb) {
    sb.append("Cluster={ clusterName=" + getClusterName()
        + ", clusterId=" + getClusterId()
        + ", desiredStackVersion=" + desiredStackVersion.getStackVersion()
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
  public synchronized void refresh() {
    clusterEntity = clusterDAO.findById(clusterEntity.getClusterId());
    clusterDAO.refresh(clusterEntity);
  }
}
