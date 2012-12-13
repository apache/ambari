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

import java.util.*;
import java.util.Map.Entry;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.orm.dao.*;
import org.apache.ambari.server.orm.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


public class ServiceImpl implements Service {

  private ClusterServiceEntity serviceEntity;
  private ServiceDesiredStateEntity serviceDesiredStateEntity;

  private static final Logger LOG =
      LoggerFactory.getLogger(ServiceImpl.class);

  private boolean persisted = false;
  private final Cluster cluster;
  // [ String type -> Config Tag ], no need to hold the direct reference to the config
  private Map<String, String> desiredConfigs;
  private Map<String, ServiceComponent> components;
  private final boolean isClientOnlyService;

  @Inject
  Gson gson;
  @Inject
  private ClusterServiceDAO clusterServiceDAO;
  @Inject
  private Clusters clusters;
  @Inject
  private ServiceDesiredStateDAO serviceDesiredStateDAO;
  @Inject
  private ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;
  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private ServiceConfigMappingDAO serviceConfigMappingDAO;
  @Inject
  private ServiceComponentFactory serviceComponentFactory;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  private void init() {
    // TODO load from DB during restart?
  }

  @AssistedInject
  public ServiceImpl(@Assisted Cluster cluster, @Assisted String serviceName,
      Injector injector) {
    injector.injectMembers(this);
    serviceEntity = new ClusterServiceEntity();
    serviceEntity.setServiceName(serviceName);
    serviceDesiredStateEntity = new ServiceDesiredStateEntity();

    serviceDesiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);

    this.cluster = cluster;
    this.desiredConfigs = new HashMap<String, String>();

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
      serviceEntity, Injector injector) {
    injector.injectMembers(this);
    this.serviceEntity = serviceEntity;
    this.cluster = cluster;

    //TODO check for null states?
    this.serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();

    this.desiredConfigs = new HashMap<String, String>();

    this.components = new HashMap<String, ServiceComponent>();

    if (!serviceEntity.getServiceComponentDesiredStateEntities().isEmpty()) {
      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity
          : serviceEntity.getServiceComponentDesiredStateEntities()) {
        components.put(serviceComponentDesiredStateEntity.getComponentName(),
            serviceComponentFactory.createExisting(this,
                serviceComponentDesiredStateEntity));
      }
    }

    for (ServiceConfigMappingEntity mappingEntity :
        serviceEntity.getServiceConfigMappings()) {
      desiredConfigs.put(mappingEntity.getConfigType(),
          mappingEntity.getVersionTag());
    }

    StackId stackId = getDesiredStackVersion();
    ServiceInfo sInfo = ambariMetaInfo.getServiceInfo(stackId.getStackName(),
        stackId.getStackVersion(), getName());
    this.isClientOnlyService = sInfo.isClientOnlyService();

    persisted = true;
  }

  @Override
  public String getName() {
      return serviceEntity.getServiceName();
  }

  @Override
  public long getClusterId() {
    return cluster.getClusterId();
  }

  @Override
  public synchronized Map<String, ServiceComponent> getServiceComponents() {
    return Collections.unmodifiableMap(components);
  }

  @Override
  public synchronized void addServiceComponents(
      Map<String, ServiceComponent> components) throws AmbariException {
    for (ServiceComponent sc : components.values()) {
      addServiceComponent(sc);
    }
  }

  @Override
  public synchronized void addServiceComponent(ServiceComponent component)
      throws AmbariException {
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
  }

  @Override
  public synchronized ServiceComponent addServiceComponent(
      String serviceComponentName) throws AmbariException {
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
  }

  @Override
  public ServiceComponent getServiceComponent(String componentName)
      throws AmbariException {
    if (!components.containsKey(componentName)) {
      throw new ServiceComponentNotFoundException(cluster.getClusterName(),
          getName(),
          componentName);
    }
    return this.components.get(componentName);
  }

  @Override
  public synchronized State getDesiredState() {
    return this.serviceDesiredStateEntity.getDesiredState();
  }

  @Override
  public synchronized void setDesiredState(State state) {
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
  }

  @Override
  public synchronized Map<String, Config> getDesiredConfigs() {
    Map<String, Config> map = new HashMap<String, Config>();
    for (Entry<String, String> entry : desiredConfigs.entrySet()) {
      Config config = cluster.getDesiredConfig(entry.getKey(), entry.getValue());
      if (null != config) {
        map.put(entry.getKey(), config);
      } else {
        // FIXME this is an error - should throw a proper exception
        throw new RuntimeException("Found an invalid config"
            + ", clusterName=" + getCluster().getClusterName()
            + ", serviceName=" + getName()
            + ", configType=" + entry.getKey()
            + ", configVersionTag=" + entry.getValue());
      }
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public synchronized void updateDesiredConfigs(Map<String, Config> configs) {

    Set<String> deletedTypes = new HashSet<String>();
    for (String type : this.desiredConfigs.keySet()) {
      if (!configs.containsKey(type)) {
        deletedTypes.add(type);
      }
    }

    for (Entry<String,Config> entry : configs.entrySet()) {
      boolean contains = false;

      for (ServiceConfigMappingEntity serviceConfigMappingEntity : serviceEntity.getServiceConfigMappings()) {
        if (entry.getKey().equals(serviceConfigMappingEntity.getConfigType())) {
          contains = true;
          serviceConfigMappingEntity.setTimestamp(new Date().getTime());
          serviceConfigMappingEntity.setVersionTag(entry.getValue().getVersionTag());
        }
      }

      if (!contains) {
        ServiceConfigMappingEntity newEntity = new ServiceConfigMappingEntity();
        newEntity.setClusterId(serviceEntity.getClusterId());
        newEntity.setServiceName(serviceEntity.getServiceName());
        newEntity.setConfigType(entry.getKey());
        newEntity.setVersionTag(entry.getValue().getVersionTag());
        newEntity.setTimestamp(new Date().getTime());
        newEntity.setServiceEntity(serviceEntity);
        serviceEntity.getServiceConfigMappings().add(newEntity);

      }


      this.desiredConfigs.put(entry.getKey(), entry.getValue().getVersionTag());
    }

    if (!deletedTypes.isEmpty()) {
      if (persisted) {
        List<ServiceConfigMappingEntity> deleteEntities =
            serviceConfigMappingDAO.findByServiceAndType(
                serviceEntity.getClusterId(), serviceEntity.getServiceName(),
                deletedTypes);
        for (ServiceConfigMappingEntity deleteEntity : deleteEntities) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting desired config from ServiceComponent"
                + ", clusterId=" + serviceEntity.getClusterId()
                + ", serviceName=" + serviceEntity.getServiceName()
                + ", configType=" + deleteEntity.getConfigType()
                + ", configVersionTag=" + deleteEntity.getVersionTag());
          }
          serviceEntity.getServiceConfigMappings().remove(
              deleteEntity);
          serviceConfigMappingDAO.remove(deleteEntity);
        }
      } else {
        for (String deletedType : deletedTypes) {
          desiredConfigs.remove(deletedType);
        }
      }
    }

    saveIfPersisted();

  }

  @Override
  public synchronized StackId getDesiredStackVersion() {
    return gson.fromJson(serviceDesiredStateEntity.getDesiredStackVersion(), StackId.class);
  }

  @Override
  public synchronized void setDesiredStackVersion(StackId stackVersion) {
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
  }

  @Override
  public synchronized ServiceResponse convertToResponse() {
    ServiceResponse r = new ServiceResponse(cluster.getClusterId(),
        cluster.getClusterName(),
        getName(),
        desiredConfigs,
        getDesiredStackVersion().getStackId(),
        getDesiredState().toString());
    return r;
  }

  @Override
  public Cluster getCluster() {
    return cluster;
  }

  @Override
  public synchronized void debugDump(StringBuilder sb) {
    sb.append("Service={ serviceName=" + getName()
        + ", clusterName=" + cluster.getClusterName()
        + ", clusterId=" + cluster.getClusterId()
        + ", desiredStackVersion=" + getDesiredStackVersion()
        + ", desiredState=" + getDesiredState().toString()
        + ", configs=[");
    boolean first = true;
    if (desiredConfigs != null) {
      for (Entry<String, String> entry : desiredConfigs.entrySet()) {
        if (!first) {
          sb.append(" , ");
        }
        first = false;
        sb.append("{ Config type=" + entry.getKey()
            + ", versionTag=" + entry.getValue() + "}");
      }
    }
    sb.append("], components=[ ");

    first = true;
    for(ServiceComponent sc : components.values()) {
      if (!first) {
        sb.append(" , ");
      }
      first = false;
      sb.append("\n      ");
      sc.debugDump(sb);
      sb.append(" ");
    }
    sb.append(" ] }");
  }

  @Override
  public synchronized boolean isPersisted() {
      return persisted;
  }

  @Override
  public synchronized void persist() {
    if (!persisted) {
      persistEntities();
      refresh();
      cluster.refresh();
      persisted = true;
    } else {
      saveIfPersisted();
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
    serviceEntity = clusterServiceDAO.merge(serviceEntity);
    serviceDesiredStateEntity = serviceDesiredStateDAO.merge(serviceDesiredStateEntity);
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
  public synchronized void refresh() {
    if (isPersisted()) {
      ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
      pk.setClusterId(getClusterId());
      pk.setServiceName(getName());
      serviceEntity = clusterServiceDAO.findByPK(pk);
      serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();
      clusterServiceDAO.refresh(serviceEntity);
      serviceDesiredStateDAO.refresh(serviceDesiredStateEntity);
    }
  }

  @Override
  public synchronized boolean canBeRemoved() {
    State state = getDesiredState();
    if (state != State.INIT
        && state != State.UNINSTALLED) {
      return false;
    }

    boolean safeToRemove = true;
    for (ServiceComponent sc : components.values()) {
      if (!sc.canBeRemoved()) {
        safeToRemove = false;
        LOG.warn("Found non removable component when trying to delete service"
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + getName()
            + ", componentName=" + sc.getName());
        break;
      }
    }
    return safeToRemove;
  }

  @Override
  public synchronized void removeAllComponents() throws AmbariException {
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
    for (ServiceComponent component : components.values()) {
      component.removeAllServiceComponentHosts();
    }
    components.clear();
    // FIXME update DB
  }

  @Override
  public synchronized void deleteServiceComponent(String componentName)
      throws AmbariException {
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
    component.removeAllServiceComponentHosts();
    components.remove(componentName);
    // FIXME update DB
  }

  @Override
  public boolean isClientOnlyService() {
    return isClientOnlyService;
  }

}
