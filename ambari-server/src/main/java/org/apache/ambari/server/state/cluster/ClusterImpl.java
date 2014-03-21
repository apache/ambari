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
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.orm.cache.ConfigGroupHostMapping;
import org.apache.ambari.server.orm.cache.HostConfigMapping;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupHostMappingDAO;
import org.apache.ambari.server.orm.dao.HostConfigMappingDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.RollbackException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClusterImpl implements Cluster {

  private static final Logger LOG =
    LoggerFactory.getLogger(ClusterImpl.class);

  @Inject
  private Clusters clusters;

  private StackId desiredStackVersion;

  private volatile Map<String, Service> services = null;

  /**
   * [ Config Type -> [ Config Version Tag -> Config ] ]
   */
  private Map<String, Map<String, Config>> allConfigs;

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

  /**
   * Map of existing config groups
   */
  private Map<Long, ConfigGroup> clusterConfigGroups;

  /**
   * Map of Request schedules for this cluster
   */
  private Map<Long, RequestExecution> requestExecutions;

  private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private Lock readLock = readWriteLock.readLock();
  private Lock writeLock = readWriteLock.writeLock();

  private final ReadWriteLock clusterGlobalLock = new ReentrantReadWriteLock();

  private ClusterEntity clusterEntity;

  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private ClusterStateDAO clusterStateDAO;
  @Inject
  private ServiceFactory serviceFactory;
  @Inject
  private ConfigFactory configFactory;
  @Inject
  private Gson gson;
  @Inject
  private HostConfigMappingDAO hostConfigMappingDAO;
  @Inject
  private ConfigGroupFactory configGroupFactory;
  @Inject
  private ConfigGroupHostMappingDAO configGroupHostMappingDAO;
  @Inject
  private RequestExecutionFactory requestExecutionFactory;

  private volatile boolean svcHostsLoaded = false;

  @Inject
  public ClusterImpl(@Assisted ClusterEntity clusterEntity,
                     Injector injector) {
    injector.injectMembers(this);
    this.clusterEntity = clusterEntity;

    this.serviceComponentHosts = new HashMap<String,
      Map<String, Map<String, ServiceComponentHost>>>();
    this.serviceComponentHostsByHost = new HashMap<String,
      List<ServiceComponentHost>>();
    this.desiredStackVersion = gson.fromJson(
      clusterEntity.getDesiredStackVersion(), StackId.class);
    allConfigs = new HashMap<String, Map<String, Config>>();
    if (!clusterEntity.getClusterConfigEntities().isEmpty()) {
      for (ClusterConfigEntity entity : clusterEntity.getClusterConfigEntities()) {

        if (!allConfigs.containsKey(entity.getType())) {
          allConfigs.put(entity.getType(), new HashMap<String, Config>());
        }

        Config config = configFactory.createExisting(this, entity);

        allConfigs.get(entity.getType()).put(entity.getTag(), config);
      }
    }
  }

  @Override
  public ReadWriteLock getClusterGlobalLock() {
    return clusterGlobalLock;
  }


  /**
   * Make sure we load all the service host components.
   * We need this for live status checks.
   */
  public void loadServiceHostComponents() {
    loadServices();
    if (svcHostsLoaded) return;
    clusterGlobalLock.writeLock().lock();
    try {
      writeLock.lock();
      try {
        LOG.info("Loading Service Host Components");
        if (svcHostsLoaded) return;
        if (services != null) {
          for (Entry<String, Service> serviceKV : services.entrySet()) {
          /* get all the service component hosts **/
            Service service = serviceKV.getValue();
            if (!serviceComponentHosts.containsKey(service.getName())) {
              serviceComponentHosts.put(service.getName(), new HashMap<String,
                Map<String, ServiceComponentHost>>());
            }
            for (Entry<String, ServiceComponent> svcComponent :
              service.getServiceComponents().entrySet()) {
              ServiceComponent comp = svcComponent.getValue();
              String componentName = svcComponent.getKey();
              if (!serviceComponentHosts.get(service.getName()).containsKey(componentName)) {
                serviceComponentHosts.get(service.getName()).put(componentName,
                  new HashMap<String, ServiceComponentHost>());
              }
              /** Get Service Host Components **/
              for (Entry<String, ServiceComponentHost> svchost :
                comp.getServiceComponentHosts().entrySet()) {
                String hostname = svchost.getKey();
                ServiceComponentHost svcHostComponent = svchost.getValue();
                if (!serviceComponentHostsByHost.containsKey(hostname)) {
                  serviceComponentHostsByHost.put(hostname,
                    new ArrayList<ServiceComponentHost>());
                }
                List<ServiceComponentHost> compList = serviceComponentHostsByHost.get(hostname);
                compList.add(svcHostComponent);

                if (!serviceComponentHosts.get(service.getName()).get(componentName)
                  .containsKey(hostname)) {
                  serviceComponentHosts.get(service.getName()).get(componentName)
                    .put(hostname, svcHostComponent);
                }
              }
            }
          }
        }
        svcHostsLoaded = true;
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

  }

  private void loadServices() {
    //logging here takes too much time
//    LOG.info("clusterEntity " + clusterEntity.getClusterServiceEntities() );
    if (services == null) {
      clusterGlobalLock.writeLock().lock();
      try {
        writeLock.lock();
        try {
          if (services == null) {
            services = new TreeMap<String, Service>();
            if (!clusterEntity.getClusterServiceEntities().isEmpty()) {
              for (ClusterServiceEntity serviceEntity : clusterEntity.getClusterServiceEntities()) {
                services.put(serviceEntity.getServiceName(), serviceFactory.createExisting(this, serviceEntity));
              }
            }
          }
        } finally {
          writeLock.unlock();
        }
      } finally {
        clusterGlobalLock.writeLock().unlock();
      }

    }
  }

  private void loadConfigGroups() {
    if (clusterConfigGroups == null) {
      clusterGlobalLock.writeLock().lock();
      try {
        writeLock.lock();
        try {
          if (clusterConfigGroups == null) {
            clusterConfigGroups = new HashMap<Long, ConfigGroup>();
            if (!clusterEntity.getConfigGroupEntities().isEmpty()) {
              for (ConfigGroupEntity configGroupEntity :
                clusterEntity.getConfigGroupEntities()) {
                clusterConfigGroups.put(configGroupEntity.getGroupId(),
                  configGroupFactory.createExisting(this, configGroupEntity));
              }
            }
          }
        } finally {
          writeLock.unlock();
        }
      } finally {
        clusterGlobalLock.writeLock().unlock();
      }
    }
  }

  private void loadRequestExecutions() {
    if (requestExecutions == null) {
      clusterGlobalLock.writeLock().lock();
      try {
        writeLock.lock();
        try {
          if (requestExecutions == null) {
            requestExecutions = new HashMap<Long, RequestExecution>();
            if (!clusterEntity.getRequestScheduleEntities().isEmpty()) {
              for (RequestScheduleEntity scheduleEntity : clusterEntity
                  .getRequestScheduleEntities()) {
                requestExecutions.put(scheduleEntity.getScheduleId(),
                  requestExecutionFactory.createExisting(this, scheduleEntity));
              }
            }
          }
        } finally {
          writeLock.unlock();
        }
      } finally {
        clusterGlobalLock.writeLock().unlock();
      }
    }
  }

  @Override
  public void addConfigGroup(ConfigGroup configGroup) throws AmbariException {
    loadConfigGroups();
    clusterGlobalLock.writeLock().lock();
    try {
      writeLock.lock();
      try {
        LOG.debug("Adding a new Config group"
          + ", clusterName = " + getClusterName()
          + ", groupName = " + configGroup.getName()
          + ", tag = " + configGroup.getTag());

        if (clusterConfigGroups.containsKey(configGroup.getId())) {
          // The loadConfigGroups will load all groups to memory
          LOG.debug("Config group already exists"
            + ", clusterName = " + getClusterName()
            + ", groupName = " + configGroup.getName()
            + ", groupId = " + configGroup.getId()
            + ", tag = " + configGroup.getTag());
        } else {
          clusterConfigGroups.put(configGroup.getId(), configGroup);
        }

      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public Map<Long, ConfigGroup> getConfigGroups() throws AmbariException {
    loadConfigGroups();
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        return Collections.unmodifiableMap(clusterConfigGroups);
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public Map<Long, ConfigGroup> getConfigGroupsByHostname(String hostname)
    throws AmbariException {
    Map<Long, ConfigGroup> configGroupMap = getConfigGroups();
    Map<Long, ConfigGroup> configGroups = new HashMap<Long, ConfigGroup>();

    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        Set<ConfigGroupHostMapping> hostMappingEntities =
          configGroupHostMappingDAO.findByHost(hostname);

        if (hostMappingEntities != null && !hostMappingEntities.isEmpty()) {
          for (ConfigGroupHostMapping entity : hostMappingEntities) {
            ConfigGroup configGroup = configGroupMap.get(entity.getConfigGroupId());
            if (configGroup != null && !configGroups.containsKey(configGroup.getId())) {
              configGroups.put(configGroup.getId(), configGroup);
            }
          }
        }
        return configGroups;

      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public void addRequestExecution(RequestExecution requestExecution) throws AmbariException {
    loadRequestExecutions();
    clusterGlobalLock.writeLock().lock();
    try {
      writeLock.lock();
      try {
        LOG.info("Adding a new request schedule"
            + ", clusterName = " + getClusterName()
            + ", id = " + requestExecution.getId()
            + ", description = " + requestExecution.getDescription());

        if (requestExecutions.containsKey(requestExecution.getId())) {
          LOG.debug("Request schedule already exists"
            + ", clusterName = " + getClusterName()
            + ", id = " + requestExecution.getId()
            + ", description = " + requestExecution.getDescription());
        } else {
          requestExecutions.put(requestExecution.getId(), requestExecution);
        }
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public Map<Long, RequestExecution> getAllRequestExecutions() {
    loadRequestExecutions();
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        return Collections.unmodifiableMap(requestExecutions);
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public void deleteRequestExecution(Long id) throws AmbariException {
    loadRequestExecutions();
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        RequestExecution requestExecution = requestExecutions.get(id);
        if (requestExecution == null) {
          throw new AmbariException("Request schedule does not exists, " +
            "id = " + id);
        }
        LOG.info("Deleting request schedule"
          + ", clusterName = " + getClusterName()
          + ", id = " + requestExecution.getId()
          + ", description = " + requestExecution.getDescription());

        requestExecution.delete();
        requestExecutions.remove(id);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public void deleteConfigGroup(Long id) throws AmbariException {
    loadConfigGroups();
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        ConfigGroup configGroup = clusterConfigGroups.get(id);
        if (configGroup == null) {
          throw new AmbariException("Config group does not exist, id = " + id);
        }
        LOG.debug("Deleting Config group"
          + ", clusterName = " + getClusterName()
          + ", groupName = " + configGroup.getName()
          + ", groupId = " + configGroup.getId()
          + ", tag = " + configGroup.getTag());

        configGroup.delete();
        clusterConfigGroups.remove(id);

      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  public ServiceComponentHost getServiceComponentHost(String serviceName,
                                                      String serviceComponentName, String hostname) throws AmbariException {
    loadServiceHostComponents();
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
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
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public String getClusterName() {
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        return clusterEntity.getClusterName();
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public void setClusterName(String clusterName) {
    clusterGlobalLock.readLock().lock();
    try {
      writeLock.lock();
      try {
        String oldName = clusterEntity.getClusterName();
        clusterEntity.setClusterName(clusterName);
        clusterDAO.merge(clusterEntity); //RollbackException possibility if UNIQUE constraint violated
        clusters.updateClusterName(oldName, clusterName);
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  public void addServiceComponentHost(
    ServiceComponentHost svcCompHost) throws AmbariException {
    loadServiceHostComponents();
    clusterGlobalLock.writeLock().lock();
    try {
      writeLock.lock();
      try {
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
            new HashMap<String, Map<String, ServiceComponentHost>>());
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
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

  }

  @Override
  public void removeServiceComponentHost(ServiceComponentHost svcCompHost)
    throws AmbariException {
    loadServiceHostComponents();
    clusterGlobalLock.writeLock().lock();
    try {
      writeLock.lock();
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Trying to remove ServiceComponentHost to ClusterHostMap cache"
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

        if (!serviceComponentHosts.containsKey(serviceName)
          || !serviceComponentHosts.get(serviceName).containsKey(componentName)
          || !serviceComponentHosts.get(serviceName).get(componentName).
          containsKey(hostname)) {
          throw new AmbariException("Invalid entry for ServiceComponentHost"
            + ", serviceName=" + serviceName
            + ", serviceComponentName" + componentName
            + ", hostname= " + hostname);
        }
        if (!serviceComponentHostsByHost.containsKey(hostname)) {
          throw new AmbariException("Invalid host entry for ServiceComponentHost"
            + ", serviceName=" + serviceName
            + ", serviceComponentName" + componentName
            + ", hostname= " + hostname);
        }

        ServiceComponentHost schToRemove = null;
        for (ServiceComponentHost sch : serviceComponentHostsByHost.get(hostname)) {
          if (sch.getServiceName().equals(serviceName)
            && sch.getServiceComponentName().equals(componentName)
            && sch.getHostName().equals(hostname)) {
            schToRemove = sch;
            break;
          }
        }

        if (schToRemove == null) {
          LOG.warn("Unavailable in per host cache. ServiceComponentHost"
            + ", serviceName=" + serviceName
            + ", serviceComponentName" + componentName
            + ", hostname= " + hostname);
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug("Removing a ServiceComponentHost"
            + ", clusterName=" + getClusterName()
            + ", clusterId=" + getClusterId()
            + ", serviceName=" + serviceName
            + ", serviceComponentName" + componentName
            + ", hostname= " + hostname);
        }

        serviceComponentHosts.get(serviceName).get(componentName).remove(hostname);
        if (schToRemove != null) {
          serviceComponentHostsByHost.get(hostname).remove(schToRemove);
        }
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

  }

  @Override
  public long getClusterId() {
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        return clusterEntity.getClusterId();
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public List<ServiceComponentHost> getServiceComponentHosts(
    String hostname) {
    loadServiceHostComponents();
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        if (serviceComponentHostsByHost.containsKey(hostname)) {
          return new CopyOnWriteArrayList<ServiceComponentHost>(serviceComponentHostsByHost.get(hostname));
        }
        return new ArrayList<ServiceComponentHost>();
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public void addService(Service service)
    throws AmbariException {
    loadServices();
    clusterGlobalLock.writeLock().lock();
    try {
      writeLock.lock();
      try {
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
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

  }

  @Override
  public Service addService(String serviceName) throws AmbariException {
    loadServices();
    clusterGlobalLock.writeLock().lock();
    try {
      writeLock.lock();
      try {
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
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

  }

  @Override
  public Service getService(String serviceName)
    throws AmbariException {
    loadServices();
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        if (!services.containsKey(serviceName)) {
          throw new ServiceNotFoundException(getClusterName(), serviceName);
        }
        return services.get(serviceName);
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public Map<String, Service> getServices() {
    loadServices();
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        return new HashMap<String, Service>(services);
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public StackId getDesiredStackVersion() {
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        return desiredStackVersion;
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public void setDesiredStackVersion(StackId stackVersion) {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
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
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public StackId getCurrentStackVersion() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        ClusterStateEntity clusterStateEntity = clusterEntity.getClusterStateEntity();
        if (clusterStateEntity != null) {
          String stackVersion = clusterStateEntity.getCurrentStackVersion();
          if (stackVersion != null && !stackVersion.isEmpty()) {
            return gson.fromJson(stackVersion, StackId.class);
          }
        }
        return null;
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }


  }

  @Override
  public void setCurrentStackVersion(StackId stackVersion)
    throws AmbariException {
    clusterGlobalLock.readLock().lock();
    try {
      writeLock.lock();
      try {
        ClusterStateEntity clusterStateEntity = clusterStateDAO.findByPK(clusterEntity.getClusterId());
        if (clusterStateEntity == null) {
          clusterStateEntity = new ClusterStateEntity();
          clusterStateEntity.setClusterId(clusterEntity.getClusterId());
          clusterStateEntity.setCurrentStackVersion(gson.toJson(stackVersion));
          clusterStateEntity.setClusterEntity(clusterEntity);
          clusterStateDAO.create(clusterStateEntity);
          clusterStateEntity = clusterStateDAO.merge(clusterStateEntity);
          clusterEntity.setClusterStateEntity(clusterStateEntity);
          clusterEntity = clusterDAO.merge(clusterEntity);
        } else {
          clusterStateEntity.setCurrentStackVersion(gson.toJson(stackVersion));
          clusterStateDAO.merge(clusterStateEntity);
          clusterEntity = clusterDAO.merge(clusterEntity);
        }
      } catch (RollbackException e) {
        LOG.warn("Unable to set version " + stackVersion + " for cluster " + getClusterName());
        throw new AmbariException("Unable to set"
          + " version=" + stackVersion
          + " for cluster " + getClusterName(), e);
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public Map<String, Config> getConfigsByType(String configType) {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        if (!allConfigs.containsKey(configType))
          return null;

        return Collections.unmodifiableMap(allConfigs.get(configType));
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public Config getConfig(String configType, String versionTag) {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        if (!allConfigs.containsKey(configType)
          || !allConfigs.get(configType).containsKey(versionTag)) {
          return null;
        }
        return allConfigs.get(configType).get(versionTag);
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public void addConfig(Config config) {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        if (config.getType() == null
          || config.getType().isEmpty()
          || config.getVersionTag() == null
          || config.getVersionTag().isEmpty()) {
          // TODO throw error
        }
        if (!allConfigs.containsKey(config.getType())) {
          allConfigs.put(config.getType(), new HashMap<String, Config>());
        }

        allConfigs.get(config.getType()).put(config.getVersionTag(), config);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public Collection<Config> getAllConfigs() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        List<Config> list = new ArrayList<Config>();
        for (Entry<String, Map<String, Config>> entry : allConfigs.entrySet()) {
          for (Config config : entry.getValue().values()) {
            list.add(config);
          }
        }
        return Collections.unmodifiableList(list);
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public ClusterResponse convertToResponse()
    throws AmbariException {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        ClusterResponse r = new ClusterResponse(getClusterId(), getClusterName(),
          clusters.getHostsForCluster(getClusterName()).keySet(),
          getDesiredStackVersion().getStackId());

        return r;
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public void debugDump(StringBuilder sb) {
    loadServices();
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        sb.append("Cluster={ clusterName=" + getClusterName()
          + ", clusterId=" + getClusterId()
          + ", desiredStackVersion=" + desiredStackVersion.getStackId()
          + ", services=[ ");
        boolean first = true;
        for (Service s : services.values()) {
          if (!first) {
            sb.append(" , ");
            first = false;
          }
          sb.append("\n    ");
          s.debugDump(sb);
          sb.append(" ");
        }
        sb.append(" ] }");
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  @Transactional
  public void refresh() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        clusterEntity = clusterDAO.findById(clusterEntity.getClusterId());
        clusterDAO.refresh(clusterEntity);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  @Transactional
  public void deleteAllServices() throws AmbariException {
    loadServices();
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
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
          service.delete();
        }

        services.clear();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

  }

  @Override
  public void deleteService(String serviceName)
    throws AmbariException {
    loadServices();
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
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
        service.delete();
        services.remove(serviceName);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

  }

  @Override
  public boolean canBeRemoved() {
    loadServices();
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
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
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  @Transactional
  public void delete() throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        refresh();
        deleteAllServices();
        removeEntities();
        allConfigs.clear();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

  }

  @Transactional
  protected void removeEntities() throws AmbariException {
    clusterDAO.removeByPK(getClusterId());
  }

  @Override
  @Transactional
  public boolean addDesiredConfig(String user, Config config) {
    if (null == user)
      throw new NullPointerException("User must be specified.");

    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        Config currentDesired = getDesiredConfigByType(config.getType());

        // do not set if it is already the current
        if (null != currentDesired && currentDesired.getVersionTag().equals(config.getVersionTag())) {
          return false;
        }

        Collection<ClusterConfigMappingEntity> entities = clusterEntity.getConfigMappingEntities();

        for (ClusterConfigMappingEntity e : entities) {
          if (e.isSelected() > 0 && e.getType().equals(config.getType())) {
            e.setSelected(0);
          }
        }

        ClusterConfigMappingEntity entity = new ClusterConfigMappingEntity();
        entity.setClusterEntity(clusterEntity);
        entity.setClusterId(clusterEntity.getClusterId());
        entity.setCreateTimestamp(Long.valueOf(System.currentTimeMillis()));
        entity.setSelected(1);
        entity.setUser(user);
        entity.setType(config.getType());
        entity.setVersion(config.getVersionTag());
        entities.add(entity);

        clusterDAO.merge(clusterEntity);

        return true;
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }


  }

  @Override
  public Map<String, DesiredConfig> getDesiredConfigs() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        Map<String, DesiredConfig> map = new HashMap<String, DesiredConfig>();
        Collection<String> types = new HashSet<String>();

        for (ClusterConfigMappingEntity e : clusterEntity.getConfigMappingEntities()) {
          if (e.isSelected() > 0) {
            DesiredConfig c = new DesiredConfig();
            c.setServiceName(null);
            c.setVersion(e.getVersion());
            c.setUser(e.getUser());

            map.put(e.getType(), c);
            types.add(e.getType());
          }
        }

        if (!map.isEmpty()) {
          Map<String, List<HostConfigMapping>> hostMappingsByType =
            hostConfigMappingDAO.findSelectedHostsByTypes(clusterEntity.getClusterId(), types);

          for (Entry<String, DesiredConfig> entry : map.entrySet()) {
            List<DesiredConfig.HostOverride> hostOverrides = new ArrayList<DesiredConfig.HostOverride>();
            for (HostConfigMapping mappingEntity : hostMappingsByType.get(entry.getKey())) {
              hostOverrides.add(new DesiredConfig.HostOverride(mappingEntity.getHostName(),
                mappingEntity.getVersion()));
            }
            entry.getValue().setHostOverrides(hostOverrides);
          }
        }

        return map;
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }


  }

  @Override
  public Config getDesiredConfigByType(String configType) {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        for (ClusterConfigMappingEntity e : clusterEntity.getConfigMappingEntities()) {
          if (e.isSelected() > 0 && e.getType().equals(configType)) {
            return getConfig(e.getType(), e.getVersion());
          }
        }

        return null;
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }


  @Override
  public Map<String, Map<String, DesiredConfig>> getHostsDesiredConfigs(Collection<String> hostnames) {

    if (hostnames == null || hostnames.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<HostConfigMapping> mappingEntities =
      hostConfigMappingDAO.findSelectedByHosts(clusterEntity.getClusterId(), hostnames);

    Map<String, Map<String, DesiredConfig>> desiredConfigsByHost = new HashMap<String, Map<String, DesiredConfig>>();

    for (String hostname : hostnames) {
      desiredConfigsByHost.put(hostname, new HashMap<String, DesiredConfig>());
    }

    for (HostConfigMapping mappingEntity : mappingEntities) {
      DesiredConfig desiredConfig = new DesiredConfig();
      desiredConfig.setVersion(mappingEntity.getVersion());
      desiredConfig.setServiceName(mappingEntity.getServiceName());
      desiredConfig.setUser(mappingEntity.getUser());

      desiredConfigsByHost.get(mappingEntity.getHostName()).put(mappingEntity.getType(), desiredConfig);
    }

    return desiredConfigsByHost;
  }

  @Override
  public Map<String, Map<String, DesiredConfig>> getAllHostsDesiredConfigs() {

    Collection<String> hostnames;
    try {
      hostnames = clusters.getHostsForCluster(clusterEntity.getClusterName()).keySet();
    } catch (AmbariException ignored) {
      return Collections.emptyMap();
    }

    return getHostsDesiredConfigs(hostnames);
  }

}
