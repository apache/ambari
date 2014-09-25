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

import javax.persistence.RollbackException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.cache.ConfigGroupHostMapping;
import org.apache.ambari.server.orm.cache.HostConfigMapping;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupHostMappingDAO;
import org.apache.ambari.server.orm.dao.HostConfigMappingDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.ConfigGroupNotFoundException;

public class ClusterImpl implements Cluster {

  private static final Logger LOG =
    LoggerFactory.getLogger(ClusterImpl.class);
  private static final Logger configChangeLog =
    LoggerFactory.getLogger("configchange");

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
  
  private Set<Alert> clusterAlerts = new HashSet<Alert>();

  private final ConfigVersionHelper configVersionHelper;

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
  @Inject
  private ConfigHelper configHelper;
  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  private volatile boolean svcHostsLoaded = false;

  private volatile Multimap<String, String> serviceConfigTypes;

  @Inject
  public ClusterImpl(@Assisted ClusterEntity clusterEntity,
                     Injector injector) throws AmbariException {
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

    if (desiredStackVersion != null && !StringUtils.isEmpty(desiredStackVersion.getStackName()) && !
      StringUtils.isEmpty(desiredStackVersion.getStackVersion())) {
      loadServiceConfigTypes();
    }

    configVersionHelper = new ConfigVersionHelper(getConfigLastVersions());
  }


  @Override
  public ReadWriteLock getClusterGlobalLock() {
    return clusterGlobalLock;
  }


  private void loadServiceConfigTypes() throws AmbariException {
    try {
      serviceConfigTypes = collectServiceConfigTypesMapping();
    } catch (AmbariException e) {
      LOG.error("Cannot load stack info:", e);
      throw e;
    }
    LOG.info("Service config types loaded: {}", serviceConfigTypes);
  }

  /**
   * Construct config type to service name mapping
   * @throws AmbariException when stack or its part not found
   */
  private Multimap<String, String> collectServiceConfigTypesMapping() throws AmbariException {
    Multimap<String, String> serviceConfigTypes = HashMultimap.create();

    Map<String, ServiceInfo> serviceInfoMap = null;
    try {
      serviceInfoMap = ambariMetaInfo.getServices(desiredStackVersion.getStackName(), desiredStackVersion.getStackVersion());
    } catch (ParentObjectNotFoundException e) {
      LOG.error("Service config versioning disabled due to exception: ", e);
      return serviceConfigTypes;
    }
    for (Entry<String, ServiceInfo> entry : serviceInfoMap.entrySet()) {
      String serviceName = entry.getKey();
      ServiceInfo serviceInfo = entry.getValue();
      //collect config types for service
      Set<PropertyInfo> properties = ambariMetaInfo.getProperties(desiredStackVersion.getStackName(), desiredStackVersion.getStackVersion(), serviceName);
      for (PropertyInfo property : properties) {
        String configType = ConfigHelper.fileNameToConfigType(property.getFilename());
        if (serviceInfo.getExcludedConfigTypes() == null ||
          !serviceInfo.getExcludedConfigTypes().contains(configType)) {
          serviceConfigTypes.put(serviceName, configType);
        }
      }
    }

    return serviceConfigTypes;
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
                StackId stackId = getCurrentStackVersion();
                try {
                  if (ambariMetaInfo.getServiceInfo(stackId.getStackName(), stackId.getStackVersion(),
                          serviceEntity.getServiceName()) != null) {
                    services.put(serviceEntity.getServiceName(), serviceFactory.createExisting(this, serviceEntity));
                  }
                } catch (AmbariException e) {
                  LOG.error(String.format("Can not get service info: stackName=%s, stackVersion=%s, serviceName=%s",
                          stackId.getStackName(), stackId.getStackVersion(),
                          serviceEntity.getServiceName()));
                  e.printStackTrace();
                }
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
          configHelper.invalidateStaleConfigsCache();
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
    Map<Long, ConfigGroup> configGroups = new HashMap<Long, ConfigGroup>();
    Map<Long, ConfigGroup> configGroupMap = getConfigGroups();

    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        Set<ConfigGroupHostMapping> hostMappingEntities = configGroupHostMappingDAO.findByHost(hostname);

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
          throw new ConfigGroupNotFoundException(getClusterName(), id.toString());
        }
        LOG.debug("Deleting Config group"
          + ", clusterName = " + getClusterName()
          + ", groupName = " + configGroup.getName()
          + ", groupId = " + configGroup.getId()
          + ", tag = " + configGroup.getTag());

        configGroup.delete();
        clusterConfigGroups.remove(id);
        configHelper.invalidateStaleConfigsCache();
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
  public void setDesiredStackVersion(StackId stackVersion) throws AmbariException {
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
        loadServiceConfigTypes();
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
  public State getProvisioningState() {    
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      State provisioningState = null;
      try {
        provisioningState = clusterEntity.getProvisioningState();
        
        if( null == provisioningState )
          provisioningState = State.INIT;
        
        return provisioningState;
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }  

  @Override
  public void setProvisioningState(State provisioningState) {
    clusterGlobalLock.readLock().lock();
    try {
      writeLock.lock();
      try {
        clusterEntity.setProvisioningState(provisioningState);
        clusterDAO.merge(clusterEntity);        
      } finally {
        writeLock.unlock();
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
          || config.getType().isEmpty()) {
          throw new IllegalArgumentException("Config type cannot be empty");
        }
        if (!allConfigs.containsKey(config.getType())) {
          allConfigs.put(config.getType(), new HashMap<String, Config>());
        }

        allConfigs.get(config.getType()).put(config.getTag(), config);
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
        Map<String, Host> hosts = clusters.getHostsForCluster(getClusterName());

        return new ClusterResponse(getClusterId(),
          getClusterName(), getProvisioningState(), hosts.keySet(), hosts.size(),
          getDesiredStackVersion().getStackId(), getClusterHealthReport());
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
        sb.append("Cluster={ clusterName=").append(getClusterName())
          .append(", clusterId=").append(getClusterId())
          .append(", desiredStackVersion=").append(desiredStackVersion.getStackId())
          .append(", services=[ ");
        boolean first = true;
        for (Service s : services.values()) {
          if (!first) {
            sb.append(" , ");
          }
          first = false;
          sb.append("\n    ");
          s.debugDump(sb);
          sb.append(' ');
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
  public ServiceConfigVersionResponse addDesiredConfig(String user, Set<Config> configs) {
    return addDesiredConfig(user, configs, null);
  }

  @Override
  public ServiceConfigVersionResponse addDesiredConfig(String user, Set<Config> configs, String serviceConfigVersionNote) {
    if (null == user)
      throw new NullPointerException("User must be specified.");

    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        if (configs == null) {
          return null;
        }

        Iterator<Config> configIterator = configs.iterator();

        while (configIterator.hasNext()) {
          Config config = configIterator.next();
          if (config == null) {
            configIterator.remove();
            continue;
          }
          Config currentDesired = getDesiredConfigByType(config.getType());

          // do not set if it is already the current
          if (null != currentDesired && currentDesired.getTag().equals(config.getTag())) {
            configIterator.remove();
          }
        }

        ServiceConfigVersionResponse serviceConfigVersionResponse =
            applyConfigs(configs, user, serviceConfigVersionNote);

        configHelper.invalidateStaleConfigsCache();
        return serviceConfigVersionResponse;
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
            c.setTag(e.getTag());
            c.setUser(e.getUser());
            c.setVersion(allConfigs.get(e.getType()).get(e.getTag()).getVersion());

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
  public ServiceConfigVersionResponse createServiceConfigVersion(String serviceName, String user, String note,
                                                                 ConfigGroup configGroup) {

    //create next service config version
    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(serviceName);
    serviceConfigEntity.setClusterEntity(clusterEntity);
    serviceConfigEntity.setVersion(configVersionHelper.getNextVersion(serviceName));
    serviceConfigEntity.setUser(user);
    serviceConfigEntity.setNote(note);

    if (configGroup != null) {
      serviceConfigEntity.setGroupId(configGroup.getId());
      Collection<Config> configs = configGroup.getConfigurations().values();
      List<ClusterConfigEntity> configEntities = new ArrayList<ClusterConfigEntity>(configs.size());
      for (Config config : configs) {
        configEntities.add(clusterDAO.findConfig(getClusterId(), config.getType(), config.getTag()));
      }
      serviceConfigEntity.setClusterConfigEntities(configEntities);

      serviceConfigEntity.setHostNames(new ArrayList<String>(configGroup.getHosts().keySet()));

    } else {
      List<ClusterConfigEntity> configEntities = getClusterConfigEntitiesByService(serviceName);
      serviceConfigEntity.setClusterConfigEntities(configEntities);
    }

    serviceConfigDAO.create(serviceConfigEntity);

    configChangeLog.info("Cluster '{}' changed by: '{}'; service_name='{}' config_group='{}' config_group_id='{}' " +
      "version='{}'", getClusterName(), user, serviceName,
      configGroup==null?"default":configGroup.getName(),
      configGroup==null?"-1":configGroup.getId(),
      serviceConfigEntity.getVersion());

    ServiceConfigVersionResponse response = new ServiceConfigVersionResponse();
    response.setUserName(user);
    response.setClusterName(getClusterName());
    response.setVersion(serviceConfigEntity.getVersion());
    response.setServiceName(serviceConfigEntity.getServiceName());
    response.setCreateTime(serviceConfigEntity.getCreateTimestamp());
    response.setUserName(serviceConfigEntity.getUser());
    response.setNote(serviceConfigEntity.getNote());
    response.setGroupId(serviceConfigEntity.getGroupId());
    response.setHosts(serviceConfigEntity.getHostNames());
    response.setGroupName(configGroup != null ? configGroup.getName() : null);

    return response;
  }

  @Override
  public String getServiceForConfigTypes(Collection<String> configTypes) {
    //debug
    LOG.info("Looking for service for config types {}", configTypes);
    String serviceName = null;
    for (String configType : configTypes) {
      for (Entry<String, String> entry : serviceConfigTypes.entries()) {
        if (StringUtils.equals(entry.getValue(), configType)) {
          if (serviceName != null) {
            if (entry.getKey()!=null && !StringUtils.equals(serviceName, entry.getKey())) {
              throw new IllegalArgumentException("Config type {} belongs to {} service, " +
                "but config group qualified for {}");
            }
          } else {
            serviceName = entry.getKey();
          }
        }
      }
    }
    LOG.info("Service {} returning", serviceName);
    return serviceName;
  }

  public String getServiceByConfigType(String configType) {
    for (Entry<String, String> entry : serviceConfigTypes.entries()) {
      String serviceName = entry.getKey();
      String type = entry.getValue();
      if (StringUtils.equals(type, configType)) {
        return serviceName;
      }
    }
    return null;
  }

  @Override
  public ServiceConfigVersionResponse setServiceConfigVersion(String serviceName, Long version, String user, String note) throws AmbariException {
    if (null == user)
      throw new NullPointerException("User must be specified.");

    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        ServiceConfigVersionResponse serviceConfigVersionResponse = applyServiceConfigVersion(serviceName, version, user, note);
        configHelper.invalidateStaleConfigsCache();
        return serviceConfigVersionResponse;
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public Map<String, Collection<ServiceConfigVersionResponse>> getActiveServiceConfigVersions() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        Map<String, Collection<ServiceConfigVersionResponse>> map = new HashMap<String, Collection<ServiceConfigVersionResponse>>();

        Set<ServiceConfigVersionResponse> responses = getActiveServiceConfigVersionSet();
        for (ServiceConfigVersionResponse response : responses) {
          if (map.get(response.getServiceName()) == null) {
            map.put(response.getServiceName(), new ArrayList<ServiceConfigVersionResponse>());
          }
          map.get(response.getServiceName()).add(response);
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
  @RequiresSession
  public List<ServiceConfigVersionResponse> getServiceConfigVersions() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        List<ServiceConfigVersionResponse> serviceConfigVersionResponses = new ArrayList<ServiceConfigVersionResponse>();
        Set<Long> activeIds = getActiveServiceConfigVersionIds();

        for (ServiceConfigEntity serviceConfigEntity : serviceConfigDAO.getServiceConfigs(getClusterId())) {
          ServiceConfigVersionResponse serviceConfigVersionResponse =
            convertToServiceConfigVersionResponse(serviceConfigEntity);

          serviceConfigVersionResponse.setHosts(serviceConfigEntity.getHostNames());
          serviceConfigVersionResponse.setConfigurations(new ArrayList<ConfigurationResponse>());
          serviceConfigVersionResponse.setIsCurrent(activeIds.contains(serviceConfigEntity.getServiceConfigId()));

          List<ClusterConfigEntity> clusterConfigEntities = serviceConfigEntity.getClusterConfigEntities();
          for (ClusterConfigEntity clusterConfigEntity : clusterConfigEntities) {
            Config config = allConfigs.get(clusterConfigEntity.getType()).get(clusterConfigEntity.getTag());
            serviceConfigVersionResponse.getConfigurations().add(new ConfigurationResponse(getClusterName(),
              config.getType(), config.getTag(), config.getVersion(), config.getProperties(),
              config.getPropertiesAttributes()));
          }

          serviceConfigVersionResponses.add(serviceConfigVersionResponse);
        }

        return serviceConfigVersionResponses;
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  private Set<ServiceConfigVersionResponse> getActiveServiceConfigVersionSet() {
    Set<ServiceConfigVersionResponse> responses = new HashSet<ServiceConfigVersionResponse>();
    List<ServiceConfigEntity> activeServiceConfigVersions = getActiveServiceConfigVersionEntities();

    for (ServiceConfigEntity lastServiceConfig : activeServiceConfigVersions) {
      ServiceConfigVersionResponse response = convertToServiceConfigVersionResponse(lastServiceConfig);
      response.setIsCurrent(true); //mark these as current, as they are
      responses.add(response);
    }
    return responses;
  }

  private Set<Long> getActiveServiceConfigVersionIds() {
    Set<Long> idSet = new HashSet<Long>();
    for (ServiceConfigEntity entity : getActiveServiceConfigVersionEntities()) {
      idSet.add(entity.getServiceConfigId());
    }
    return idSet;
  }

  private List<ServiceConfigEntity> getActiveServiceConfigVersionEntities() {

    List<ServiceConfigEntity> activeServiceConfigVersions = new ArrayList<ServiceConfigEntity>();
    //for services
    activeServiceConfigVersions.addAll(serviceConfigDAO.getLastServiceConfigs(getClusterId()));
    //for config groups
    if (clusterConfigGroups != null) {
      activeServiceConfigVersions.addAll(
        serviceConfigDAO.getLastServiceConfigVersionsForGroups(clusterConfigGroups.keySet()));
    }

    return activeServiceConfigVersions;
  }

  @RequiresSession
  ServiceConfigVersionResponse getActiveServiceConfigVersion(String serviceName) {
    ServiceConfigEntity lastServiceConfig = serviceConfigDAO.getLastServiceConfig(getClusterId(), serviceName);
    if (lastServiceConfig == null) {
      LOG.debug("No service config version found for service {}", serviceName);
      return null;
    }
    return convertToServiceConfigVersionResponse(lastServiceConfig);
  }

  @RequiresSession
  ServiceConfigVersionResponse convertToServiceConfigVersionResponse(ServiceConfigEntity serviceConfigEntity) {
    ServiceConfigVersionResponse serviceConfigVersionResponse = new ServiceConfigVersionResponse();

    serviceConfigVersionResponse.setClusterName(getClusterName());
    serviceConfigVersionResponse.setServiceName(serviceConfigEntity.getServiceName());
    serviceConfigVersionResponse.setVersion(serviceConfigEntity.getVersion());
    serviceConfigVersionResponse.setCreateTime(serviceConfigEntity.getCreateTimestamp());    
    serviceConfigVersionResponse.setUserName(serviceConfigEntity.getUser());
    serviceConfigVersionResponse.setNote(serviceConfigEntity.getNote());

    Long groupId = serviceConfigEntity.getGroupId();

    if (groupId != null) {
      serviceConfigVersionResponse.setGroupId(groupId);
      ConfigGroup configGroup = null;
      if (clusterConfigGroups != null) {
        configGroup = clusterConfigGroups.get(groupId);
      }

      if (configGroup != null) {
        serviceConfigVersionResponse.setGroupName(configGroup.getName());
      } else {
        serviceConfigVersionResponse.setGroupName("deleted");
      }
    } else {
      serviceConfigVersionResponse.setGroupId(-1L); // -1 if no group
      serviceConfigVersionResponse.setGroupName("default");
    }

    return serviceConfigVersionResponse;
  }

  @Transactional
  ServiceConfigVersionResponse applyServiceConfigVersion(String serviceName, Long serviceConfigVersion, String user,
                                 String serviceConfigVersionNote) throws AmbariException {
    ServiceConfigEntity serviceConfigEntity = serviceConfigDAO.findByServiceAndVersion(serviceName, serviceConfigVersion);
    if (serviceConfigEntity == null) {
      throw new ObjectNotFoundException("Service config version with serviceName={} and version={} not found");
    }

    //disable all configs related to service
    if (serviceConfigEntity.getGroupId() == null) {
      Collection<String> configTypes = serviceConfigTypes.get(serviceName);
      for (ClusterConfigMappingEntity entity : clusterEntity.getConfigMappingEntities()) {
        if (configTypes.contains(entity.getType()) && entity.isSelected() > 0) {
          entity.setSelected(0);
        }
      }
      clusterDAO.merge(clusterEntity);

      for (ClusterConfigEntity configEntity : serviceConfigEntity.getClusterConfigEntities()) {
        selectConfig(configEntity.getType(), configEntity.getTag(), user);
      }
    } else {
      Long configGroupId = serviceConfigEntity.getGroupId();
      ConfigGroup configGroup = clusterConfigGroups.get(configGroupId);
      if (configGroup != null) {
        Map<String, Config> groupDesiredConfigs = new HashMap<String, Config>();
        for (ClusterConfigEntity entity : serviceConfigEntity.getClusterConfigEntities()) {
          Config config = allConfigs.get(entity.getType()).get(entity.getTag());
          groupDesiredConfigs.put(config.getType(), config);
        }
        configGroup.setConfigurations(groupDesiredConfigs);

        Map<String, Host> groupDesiredHosts = new HashMap<String, Host>();
        for (String hostname : serviceConfigEntity.getHostNames()) {
          Host host = clusters.getHost(hostname);
          if (host != null) {
            groupDesiredHosts.put(hostname, host);
          } else {
            LOG.warn("Host {} doesn't exist anymore, skipping", hostname);
          }
        }
        configGroup.setHosts(groupDesiredHosts);
        configGroup.persist();
      } else {
        throw new IllegalArgumentException("Config group {} doesn't exist");
      }
    }

    ServiceConfigEntity serviceConfigEntityClone = new ServiceConfigEntity();
    serviceConfigEntityClone.setCreateTimestamp(System.currentTimeMillis());
    serviceConfigEntityClone.setUser(user);
    serviceConfigEntityClone.setServiceName(serviceName);
    serviceConfigEntityClone.setClusterEntity(clusterEntity);
    serviceConfigEntityClone.setClusterConfigEntities(serviceConfigEntity.getClusterConfigEntities());
    serviceConfigEntityClone.setClusterId(serviceConfigEntity.getClusterId());
    serviceConfigEntityClone.setHostNames(serviceConfigEntity.getHostNames());
    serviceConfigEntityClone.setGroupId(serviceConfigEntity.getGroupId());
    serviceConfigEntityClone.setNote(serviceConfigVersionNote);
    serviceConfigEntityClone.setVersion(configVersionHelper.getNextVersion(serviceName));

    serviceConfigDAO.create(serviceConfigEntityClone);

    return convertToServiceConfigVersionResponse(serviceConfigEntityClone);
  }

  @Transactional
  void selectConfig(String type, String tag, String user) {
    Collection<ClusterConfigMappingEntity> entities = clusterEntity.getConfigMappingEntities();

    //disable previous config
    for (ClusterConfigMappingEntity e : entities) {
      if (e.isSelected() > 0 && e.getType().equals(type)) {
        e.setSelected(0);
      }
    }

    ClusterConfigMappingEntity entity = new ClusterConfigMappingEntity();
    entity.setClusterEntity(clusterEntity);
    entity.setClusterId(clusterEntity.getClusterId());
    entity.setCreateTimestamp(System.currentTimeMillis());
    entity.setSelected(1);
    entity.setUser(user);
    entity.setType(type);
    entity.setTag(tag);
    entities.add(entity);

    clusterDAO.merge(clusterEntity);

  }

  @Transactional
  ServiceConfigVersionResponse applyConfigs(Set<Config> configs, String user, String serviceConfigVersionNote) {

    String serviceName = null;
    for (Config config: configs) {

      selectConfig(config.getType(), config.getTag(), user);
      //find service name for config type
      for (Entry<String, String> entry : serviceConfigTypes.entries()) {
        if (StringUtils.equals(entry.getValue(), config.getType())) {
          if (serviceName != null && !serviceName.equals(entry.getKey())) {
            LOG.error("Updating configs for multiple services by a " +
              "single API request isn't supported, config version not created");
            return null;
          }
          serviceName = entry.getKey();
          break;
        }
      }
    }

    if (serviceName == null) {
      LOG.error("No service found for config type '{}', service config version not created");
      return null;
    } else {
      return createServiceConfigVersion(serviceName, user, serviceConfigVersionNote);
    }

  }

  private ServiceConfigVersionResponse createServiceConfigVersion(String serviceName, String user,
                                                                  String serviceConfigVersionNote) {
    //create next service config version
    return createServiceConfigVersion(serviceName, user, serviceConfigVersionNote, null);
  }

  private List<ClusterConfigEntity> getClusterConfigEntitiesByService(String serviceName) {
    List<ClusterConfigEntity> configEntities = new ArrayList<ClusterConfigEntity>();

    //add configs from this service
    Collection<String> configTypes = serviceConfigTypes.get(serviceName);
    for (ClusterConfigMappingEntity mappingEntity : clusterEntity.getConfigMappingEntities()) {
      if (mappingEntity.isSelected() > 0 && configTypes.contains(mappingEntity.getType())) {
        ClusterConfigEntity configEntity =
          clusterDAO.findConfig(getClusterId(), mappingEntity.getType(), mappingEntity.getTag());
        if (configEntity != null) {
          configEntities.add(configEntity);
        } else {
          LOG.error("Desired cluster config type={}, tag={} is not present in database," +
            " unable to add to service config version");
        }
      }
    }
    return configEntities;
  }

  @Override
  public Config getDesiredConfigByType(String configType) {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        for (ClusterConfigMappingEntity e : clusterEntity.getConfigMappingEntities()) {
          if (e.isSelected() > 0 && e.getType().equals(configType)) {
            return getConfig(e.getType(), e.getTag());
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
      desiredConfig.setTag(mappingEntity.getVersion());
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

  @Override
  public Long getNextConfigVersion(String type) {
    return configVersionHelper.getNextVersion(type);
  }

  private Map<String, Long> getConfigLastVersions() {
    Map<String, Long> maxVersions = new HashMap<String, Long>();
    //config versions
    for (Entry<String, Map<String, Config>> mapEntry : allConfigs.entrySet()) {
      String type = mapEntry.getKey();
      Long lastVersion = 0L;
      for (Entry<String, Config> configEntry : mapEntry.getValue().entrySet()) {
        Long version = configEntry.getValue().getVersion();
        if (version > lastVersion) {
          lastVersion = version;
        }
      }
      maxVersions.put(type, lastVersion);
    }

    //service config versions
    maxVersions.putAll(serviceConfigDAO.findMaxVersions(getClusterId()));

    return maxVersions;
  }

  @Transactional
  @Override
  public List<ServiceComponentHostEvent> processServiceComponentHostEvents(ListMultimap<String, ServiceComponentHostEvent> eventMap) {
    List<ServiceComponentHostEvent> failedEvents = new ArrayList<ServiceComponentHostEvent>();

    clusterGlobalLock.readLock().lock();
    try {
      for (Entry<String, ServiceComponentHostEvent> entry : eventMap.entries()) {
        String serviceName = entry.getKey();
        ServiceComponentHostEvent event = entry.getValue();
        try {
          Service service = getService(serviceName);
          ServiceComponent serviceComponent = service.getServiceComponent(event.getServiceComponentName());
          ServiceComponentHost serviceComponentHost = serviceComponent.getServiceComponentHost(event.getHostName());
          serviceComponentHost.handleEvent(event);
        } catch (AmbariException e) {
          LOG.error("ServiceComponentHost lookup exception ", e.getMessage());
          failedEvents.add(event);
        } catch (InvalidStateTransitionException e) {
          LOG.error("Invalid transition ", e);
          failedEvents.add(event);
        }
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

    return failedEvents;
  }

  private ClusterHealthReport getClusterHealthReport() throws AmbariException {

    int staleConfigsHosts = 0;
    int maintenanceStateHosts = 0;

    int healthyStateHosts = 0;
    int unhealthyStateHosts = 0;
    int initStateHosts = 0;
    int healthyStatusHosts = 0;

    int unhealthyStatusHosts = 0;
    int unknownStatusHosts = 0;
    int alertStatusHosts = 0;
    int heartbeatLostStateHosts = 0;

    Set<String> hostnames;

    try {
      hostnames = clusters.getHostsForCluster(clusterEntity.getClusterName()).keySet();
    } catch (AmbariException ignored) {
      hostnames = Collections.emptySet();
    }

    for (String hostname : hostnames) {

      Host host = clusters.getHost(hostname);

      switch (host.getState()) {
        case HEALTHY:
          healthyStateHosts++;
          break;
        case UNHEALTHY:
          unhealthyStateHosts++;
          break;
        case INIT:
          initStateHosts++;
          break;
        case HEARTBEAT_LOST:
          heartbeatLostStateHosts++;
          break;
      }

      switch (HostHealthStatus.HealthStatus.valueOf(host.getStatus())) {
        case HEALTHY:
          healthyStatusHosts++;
          break;
        case UNHEALTHY:
          unhealthyStatusHosts++;
          break;
        case UNKNOWN:
          unknownStatusHosts++;
          break;
        case ALERT:
          alertStatusHosts++;
          break;
      }

      boolean staleConfig = false;
      boolean maintenanceState = false;

      if (serviceComponentHostsByHost.containsKey(hostname)) {
        for (ServiceComponentHost sch : serviceComponentHostsByHost.get(hostname)) {
          staleConfig = staleConfig || configHelper.isStaleConfigs(sch);
          maintenanceState = maintenanceState ||
            maintenanceStateHelper.getEffectiveState(sch) != MaintenanceState.OFF;
        }
      }

      if (staleConfig) {
        staleConfigsHosts++;
      }
      if (maintenanceState) {
        maintenanceStateHosts++;
      }
    }

    ClusterHealthReport chr = new ClusterHealthReport();
    chr.setAlertStatusHosts(alertStatusHosts);
    chr.setHealthyStateHosts(healthyStateHosts);
    chr.setUnknownStatusHosts(unknownStatusHosts);
    chr.setUnhealthyStatusHosts(unhealthyStatusHosts);
    chr.setUnhealthyStateHosts(unhealthyStateHosts);
    chr.setStaleConfigsHosts(staleConfigsHosts);
    chr.setMaintenanceStateHosts(maintenanceStateHosts);
    chr.setInitStateHosts(initStateHosts);
    chr.setHeartbeatLostStateHosts(heartbeatLostStateHosts);
    chr.setHealthyStatusHosts(healthyStatusHosts);

    return chr;
  }
  
  @Override
  public void addAlerts(Collection<Alert> alerts) {
    try {
      writeLock.lock();
      
      for (final Alert alert : alerts) {
        if (clusterAlerts.size() > 0) {
          CollectionUtils.filter(clusterAlerts, new Predicate() {
            @Override
            public boolean evaluate(Object obj) {
              Alert collectedAlert = (Alert) obj;
              return !collectedAlert.almostEquals(alert);
            }
          });
        }
        
        if (LOG.isDebugEnabled()) {
          LOG.debug("Adding alert for name={} service={}, on host={}",
              alert.getName(), alert.getService(), alert.getHost());
        }
      }

      clusterAlerts.addAll(alerts);

    } finally {
      writeLock.unlock();
    }
  }
  
  @Override
  public Collection<Alert> getAlerts() {
    try {
      readLock.lock();
      
      return Collections.unmodifiableSet(clusterAlerts);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean checkPermission(PrivilegeEntity privilegeEntity, boolean readOnly) {
    ResourceEntity resourceEntity = clusterEntity.getResource();
    if (resourceEntity != null) {
      Integer permissionId = privilegeEntity.getPermission().getId();
      // CLUSTER.READ or CLUSTER.OPERATE for the given cluster resource.
      if (privilegeEntity.getResource().equals(resourceEntity)) {
        if ((readOnly && permissionId.equals(PermissionEntity.CLUSTER_READ_PERMISSION)) ||
            permissionId.equals(PermissionEntity.CLUSTER_OPERATE_PERMISSION)) {
          return true;
        }
      }
    }
    return false;
  }
}
