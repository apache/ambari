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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


public class ServiceImpl implements Service {
  private final ReadWriteLock clusterGlobalLock;
  private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private ClusterServiceEntity serviceEntity;
  private ServiceDesiredStateEntity serviceDesiredStateEntity;

  private static final Logger LOG =
      LoggerFactory.getLogger(ServiceImpl.class);

  private boolean persisted = false;
  private final Cluster cluster;
  private Map<String, ServiceComponent> components;
  private final boolean isClientOnlyService;

  @Inject
  Gson gson;
  @Inject
  private ClusterServiceDAO clusterServiceDAO;
  @Inject
  private ServiceDesiredStateDAO serviceDesiredStateDAO;
  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private ServiceComponentFactory serviceComponentFactory;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  private void init() {
    // TODO load from DB during restart?
  }

  @AssistedInject
  public ServiceImpl(@Assisted Cluster cluster, @Assisted String serviceName,
      Injector injector) throws AmbariException {
    injector.injectMembers(this);
    clusterGlobalLock = cluster.getClusterGlobalLock();
    serviceEntity = new ClusterServiceEntity();
    serviceEntity.setServiceName(serviceName);
    serviceDesiredStateEntity = new ServiceDesiredStateEntity();

    serviceDesiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);

    this.cluster = cluster;

    this.components = new HashMap<String, ServiceComponent>();

    StackId stackId = cluster.getDesiredStackVersion();
    setDesiredStackVersion(stackId);

    ServiceInfo sInfo = ambariMetaInfo.getServiceInfo(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);
    this.isClientOnlyService = sInfo.isClientOnlyService();

    init();
  }

  @AssistedInject
  public ServiceImpl(@Assisted Cluster cluster, @Assisted ClusterServiceEntity
      serviceEntity, Injector injector) throws AmbariException {
    injector.injectMembers(this);
    clusterGlobalLock = cluster.getClusterGlobalLock();
    this.serviceEntity = serviceEntity;
    this.cluster = cluster;

    //TODO check for null states?
    this.serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();

    this.components = new HashMap<String, ServiceComponent>();

    if (!serviceEntity.getServiceComponentDesiredStateEntities().isEmpty()) {
      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity
          : serviceEntity.getServiceComponentDesiredStateEntities()) {
        components.put(serviceComponentDesiredStateEntity.getComponentName(),
            serviceComponentFactory.createExisting(this,
                serviceComponentDesiredStateEntity));
      }
    }

    StackId stackId = getDesiredStackVersion();
    ServiceInfo sInfo = ambariMetaInfo.getServiceInfo(stackId.getStackName(),
        stackId.getStackVersion(), getName());
    this.isClientOnlyService = sInfo.isClientOnlyService();

    persisted = true;
  }

  @Override
  public ReadWriteLock getClusterGlobalLock() {
    return clusterGlobalLock;
  }

  @Override
  public String getName() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        return serviceEntity.getServiceName();
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public long getClusterId() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        return cluster.getClusterId();
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public Map<String, ServiceComponent> getServiceComponents() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        return new HashMap<String, ServiceComponent>(components);
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public void addServiceComponents(
      Map<String, ServiceComponent> components) throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        for (ServiceComponent sc : components.values()) {
          addServiceComponent(sc);
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public void addServiceComponent(ServiceComponent component)
      throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        // TODO validation
        if (LOG.isDebugEnabled()) {
          LOG.debug("Adding a ServiceComponent to Service"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + getName()
              + ", serviceComponentName=" + component.getName());
        }
        if (components.containsKey(component.getName())) {
          throw new AmbariException("Cannot add duplicate ServiceComponent"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + getName()
              + ", serviceComponentName=" + component.getName());
        }
        this.components.put(component.getName(), component);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public ServiceComponent addServiceComponent(
      String serviceComponentName) throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Adding a ServiceComponent to Service"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + getName()
              + ", serviceComponentName=" + serviceComponentName);
        }
        if (components.containsKey(serviceComponentName)) {
          throw new AmbariException("Cannot add duplicate ServiceComponent"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + getName()
              + ", serviceComponentName=" + serviceComponentName);
        }
        ServiceComponent component = serviceComponentFactory.createNew(this, serviceComponentName);
        this.components.put(component.getName(), component);
        return component;
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }


  }

  @Override
  public ServiceComponent getServiceComponent(String componentName)
      throws AmbariException {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        if (!components.containsKey(componentName)) {
          throw new ServiceComponentNotFoundException(cluster.getClusterName(),
              getName(),
              componentName);
        }
        return this.components.get(componentName);
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }


  }

  @Override
  public State getDesiredState() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        return this.serviceDesiredStateEntity.getDesiredState();
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }


  }

  @Override
  public void setDesiredState(State state) {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Setting DesiredState of Service"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + getName()
              + ", oldDesiredState=" + this.getDesiredState()
              + ", newDesiredState=" + state);
        }
        this.serviceDesiredStateEntity.setDesiredState(state);
        saveIfPersisted();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public StackId getDesiredStackVersion() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        return gson.fromJson(serviceDesiredStateEntity.getDesiredStackVersion(), StackId.class);
      } finally {
        readWriteLock.readLock().unlock();
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
          LOG.debug("Setting DesiredStackVersion of Service"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + getName()
              + ", oldDesiredStackVersion=" + getDesiredStackVersion()
              + ", newDesiredStackVersion=" + stackVersion);
        }
        serviceDesiredStateEntity.setDesiredStackVersion(gson.toJson(stackVersion));
        saveIfPersisted();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }


  }

  @Override
  public ServiceResponse convertToResponse() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        ServiceResponse r = new ServiceResponse(cluster.getClusterId(),
            cluster.getClusterName(),
            getName(),
            getDesiredStackVersion().getStackId(),
            getDesiredState().toString());
        
        r.setMaintenanceState(getMaintenanceState().name());
        return r;
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public Cluster getCluster() {
    return cluster;
  }

  @Override
  public void debugDump(StringBuilder sb) {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        sb.append("Service={ serviceName=" + getName()
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", desiredStackVersion=" + getDesiredStackVersion()
            + ", desiredState=" + getDesiredState().toString()
            + ", components=[ ");
        boolean first = true;
        for (ServiceComponent sc : components.values()) {
          if (!first) {
            sb.append(" , ");
          }
          first = false;
          sb.append("\n      ");
          sc.debugDump(sb);
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
  public boolean isPersisted() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        return persisted;
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Override
  public void persist() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        if (!persisted) {
          persistEntities();
          refresh();
          cluster.refresh();
          persisted = true;
        } else {
          saveIfPersisted();
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

  }

  @Transactional
  protected void persistEntities() {
    ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());
    serviceEntity.setClusterEntity(clusterEntity);
    clusterServiceDAO.create(serviceEntity);
    serviceDesiredStateDAO.create(serviceDesiredStateEntity);
    clusterEntity.getClusterServiceEntities().add(serviceEntity);
    clusterDAO.merge(clusterEntity);
//    serviceEntity =
        clusterServiceDAO.merge(serviceEntity);
//    serviceDesiredStateEntity =
        serviceDesiredStateDAO.merge(serviceDesiredStateEntity);
  }

  @Transactional
  private void saveIfPersisted() {
    if (isPersisted()) {
      clusterServiceDAO.merge(serviceEntity);
      serviceDesiredStateDAO.merge(serviceDesiredStateEntity);
    }
  }

  @Override
  @Transactional
  public void refresh() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        if (isPersisted()) {
          ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
          pk.setClusterId(getClusterId());
          pk.setServiceName(getName());
          serviceEntity = clusterServiceDAO.findByPK(pk);
          serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();
          clusterServiceDAO.refresh(serviceEntity);
          serviceDesiredStateDAO.refresh(serviceDesiredStateEntity);
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }


  }

  @Override
  public boolean canBeRemoved() {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        if (!getDesiredState().isRemovableState()) {
          return false;
        }

        for (ServiceComponent sc : components.values()) {
          if (!sc.canBeRemoved()) {
            LOG.warn("Found non removable component when trying to delete service"
                + ", clusterName=" + cluster.getClusterName()
                + ", serviceName=" + getName()
                + ", componentName=" + sc.getName());
            return false;
          }
        }
        return true;
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }


  }

  @Override
  @Transactional
  public void deleteAllComponents() throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        LOG.info("Deleting all components for service"
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + getName());
        // FIXME check dependencies from meta layer
        for (ServiceComponent component : components.values()) {
          if (!component.canBeRemoved()) {
            throw new AmbariException("Found non removable component when trying to"
                + " delete all components from service"
                + ", clusterName=" + cluster.getClusterName()
                + ", serviceName=" + getName()
                + ", componentName=" + component.getName());
          }
        }

        for (ServiceComponent serviceComponent : components.values()) {
          serviceComponent.delete();
        }

        components.clear();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public void deleteServiceComponent(String componentName)
      throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        ServiceComponent component = getServiceComponent(componentName);
        LOG.info("Deleting servicecomponent for cluster"
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + getName()
            + ", componentName=" + componentName);
        // FIXME check dependencies from meta layer
        if (!component.canBeRemoved()) {
          throw new AmbariException("Could not delete component from cluster"
              + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + getName()
              + ", componentName=" + componentName);
        }

        component.delete();
        components.remove(componentName);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }


  }

  @Override
  public boolean isClientOnlyService() {
    return isClientOnlyService;
  }

  @Override
  @Transactional
  public void delete() throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        deleteAllComponents();

        if (persisted) {
          removeEntities();
          persisted = false;
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }


  }

  @Transactional
  protected void removeEntities() throws AmbariException {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(getClusterId());
    pk.setServiceName(getName());

    clusterServiceDAO.removeByPK(pk);
  }
  
  @Override
  public void setMaintenanceState(MaintenanceState state) {
    clusterGlobalLock.readLock().lock();
    try {
      try {
        readWriteLock.writeLock().lock();
        serviceDesiredStateEntity.setMaintenanceState(state);
        saveIfPersisted();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }
  
  @Override
  public MaintenanceState getMaintenanceState() {
    return serviceDesiredStateEntity.getMaintenanceState();
  }

}
