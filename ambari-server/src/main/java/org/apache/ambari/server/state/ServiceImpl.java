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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.ServiceRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


public class ServiceImpl implements Service {
  private final Lock lock = new ReentrantLock();
  private ServiceDesiredStateEntityPK serviceDesiredStateEntityPK;
  private ClusterServiceEntityPK serviceEntityPK;

  private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);

  private final Cluster cluster;
  private final ConcurrentMap<String, ServiceComponent> components = new ConcurrentHashMap<>();
  private final boolean isClientOnlyService;

  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  private final ClusterServiceDAO clusterServiceDAO;
  private final ServiceDesiredStateDAO serviceDesiredStateDAO;
  private final ClusterDAO clusterDAO;
  private final ServiceComponentFactory serviceComponentFactory;

  /**
   * Data access object for retrieving stack instances.
   */
  private final StackDAO stackDAO;

  /**
   * Used to publish events relating to service CRUD operations.
   */
  private final AmbariEventPublisher eventPublisher;

  /**
   * The name of the service.
   */
  private final String serviceName;

  @AssistedInject
  ServiceImpl(@Assisted Cluster cluster, @Assisted String serviceName, ClusterDAO clusterDAO,
      ClusterServiceDAO clusterServiceDAO, ServiceDesiredStateDAO serviceDesiredStateDAO,
      ServiceComponentFactory serviceComponentFactory, StackDAO stackDAO,
      AmbariMetaInfo ambariMetaInfo, AmbariEventPublisher eventPublisher)
      throws AmbariException {
    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceDesiredStateDAO = serviceDesiredStateDAO;
    this.serviceComponentFactory = serviceComponentFactory;
    this.stackDAO = stackDAO;
    this.eventPublisher = eventPublisher;
    this.serviceName = serviceName;

    ClusterServiceEntity serviceEntity = new ClusterServiceEntity();
    serviceEntity.setClusterId(cluster.getClusterId());
    serviceEntity.setServiceName(serviceName);
    ServiceDesiredStateEntity serviceDesiredStateEntity = new ServiceDesiredStateEntity();
    serviceDesiredStateEntity.setServiceName(serviceName);
    serviceDesiredStateEntity.setClusterId(cluster.getClusterId());
    serviceDesiredStateEntityPK = getServiceDesiredStateEntityPK(serviceDesiredStateEntity);
    serviceEntityPK = getServiceEntityPK(serviceEntity);

    serviceDesiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);

    StackId stackId = cluster.getDesiredStackVersion();
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    serviceDesiredStateEntity.setDesiredStack(stackEntity);

    ServiceInfo sInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);

    isClientOnlyService = sInfo.isClientOnlyService();

    persist(serviceEntity);
  }

  @AssistedInject
  ServiceImpl(@Assisted Cluster cluster, @Assisted ClusterServiceEntity serviceEntity,
      ClusterDAO clusterDAO, ClusterServiceDAO clusterServiceDAO,
      ServiceDesiredStateDAO serviceDesiredStateDAO,
      ServiceComponentFactory serviceComponentFactory, StackDAO stackDAO,
      AmbariMetaInfo ambariMetaInfo, AmbariEventPublisher eventPublisher)
      throws AmbariException {
    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceDesiredStateDAO = serviceDesiredStateDAO;
    this.serviceComponentFactory = serviceComponentFactory;
    this.stackDAO = stackDAO;
    this.eventPublisher = eventPublisher;
    serviceName = serviceEntity.getServiceName();

    ServiceDesiredStateEntity serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();
    serviceDesiredStateEntityPK = getServiceDesiredStateEntityPK(serviceDesiredStateEntity);
    serviceEntityPK = getServiceEntityPK(serviceEntity);

    if (!serviceEntity.getServiceComponentDesiredStateEntities().isEmpty()) {
      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity
          : serviceEntity.getServiceComponentDesiredStateEntities()) {
        try {
            components.put(serviceComponentDesiredStateEntity.getComponentName(),
                serviceComponentFactory.createExisting(this,
                    serviceComponentDesiredStateEntity));
          } catch(ProvisionException ex) {
            StackId stackId = cluster.getCurrentStackVersion();
            LOG.error(String.format("Can not get component info: stackName=%s, stackVersion=%s, serviceName=%s, componentName=%s",
                stackId.getStackName(), stackId.getStackVersion(),
                serviceEntity.getServiceName(),serviceComponentDesiredStateEntity.getComponentName()));
            ex.printStackTrace();
          }
      }
    }

    StackId stackId = getDesiredStackVersion();
    ServiceInfo sInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), getName());
    isClientOnlyService = sInfo.isClientOnlyService();
  }

  @Override
  public String getName() {
    return serviceName;
  }

  @Override
  public long getClusterId() {
    return cluster.getClusterId();
  }

  @Override
  public Map<String, ServiceComponent> getServiceComponents() {
    return new HashMap<String, ServiceComponent>(components);
  }

  @Override
  public void addServiceComponents(
      Map<String, ServiceComponent> components) throws AmbariException {
    for (ServiceComponent sc : components.values()) {
      addServiceComponent(sc);
    }
  }

  @Override
  public void addServiceComponent(ServiceComponent component) throws AmbariException {
    if (components.containsKey(component.getName())) {
      throw new AmbariException("Cannot add duplicate ServiceComponent"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + getName()
          + ", serviceComponentName=" + component.getName());
    }

    components.put(component.getName(), component);
  }

  @Override
  public ServiceComponent addServiceComponent(String serviceComponentName)
      throws AmbariException {
    ServiceComponent component = serviceComponentFactory.createNew(this, serviceComponentName);
    addServiceComponent(component);
    return component;
  }

  @Override
  public ServiceComponent getServiceComponent(String componentName)
      throws AmbariException {
    ServiceComponent serviceComponent = components.get(componentName);
    if (null == serviceComponent) {
      throw new ServiceComponentNotFoundException(cluster.getClusterName(),
          getName(), componentName);
    }

    return serviceComponent;
  }

  @Override
  public State getDesiredState() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    return serviceDesiredStateEntity.getDesiredState();
  }

  @Override
  public void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service" + ", clusterName="
          + cluster.getClusterName() + ", clusterId="
          + cluster.getClusterId() + ", serviceName=" + getName()
          + ", oldDesiredState=" + getDesiredState() + ", newDesiredState="
          + state);
    }

    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredState(state);
    serviceDesiredStateDAO.merge(serviceDesiredStateEntity);
  }

  @Override
  public SecurityState getSecurityState() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    return serviceDesiredStateEntity.getSecurityState();
  }

  @Override
  public void setSecurityState(SecurityState securityState) throws AmbariException {
    if(!securityState.isEndpoint()) {
      throw new AmbariException("The security state must be an endpoint state");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredSecurityState of Service" + ", clusterName="
          + cluster.getClusterName() + ", clusterId="
          + cluster.getClusterId() + ", serviceName=" + getName()
          + ", oldDesiredSecurityState=" + getSecurityState()
          + ", newDesiredSecurityState=" + securityState);
    }
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setSecurityState(securityState);
    serviceDesiredStateDAO.merge(serviceDesiredStateEntity);
  }

  @Override
  public StackId getDesiredStackVersion() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    StackEntity desiredStackEntity = serviceDesiredStateEntity.getDesiredStack();
    if( null != desiredStackEntity ) {
      return new StackId(desiredStackEntity);
    } else {
      return null;
    }
  }

  @Override
  public void setDesiredStackVersion(StackId stack) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredStackVersion of Service" + ", clusterName="
          + cluster.getClusterName() + ", clusterId="
          + cluster.getClusterId() + ", serviceName=" + getName()
          + ", oldDesiredStackVersion=" + getDesiredStackVersion()
          + ", newDesiredStackVersion=" + stack);
    }

    StackEntity stackEntity = stackDAO.find(stack.getStackName(), stack.getStackVersion());
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredStack(stackEntity);
    serviceDesiredStateDAO.merge(serviceDesiredStateEntity);
  }

  @Override
  public ServiceResponse convertToResponse() {
    ServiceResponse r = new ServiceResponse(cluster.getClusterId(), cluster.getClusterName(),
        getName(), getDesiredStackVersion().getStackId(), getDesiredState().toString(),
        isCredentialStoreSupported(), isCredentialStoreEnabled());

    r.setMaintenanceState(getMaintenanceState().name());
    return r;
  }

  @Override
  public Cluster getCluster() {
    return cluster;
  }

  /**
   * Get a true or false value specifying whether
   * credential store is supported by this service.
   *
   * @return true or false
   */
  @Override
  public boolean isCredentialStoreSupported() {
    ServiceDesiredStateEntity desiredStateEntity = getServiceDesiredStateEntity();

    if (desiredStateEntity != null) {
      return desiredStateEntity.isCredentialStoreSupported();
    } else {
      LOG.warn("Trying to fetch a member from an entity object that may " +
              "have been previously deleted, serviceName = " + getName());
    }
    return false;
  }


  /**
   * Set a true or false value specifying whether this
   * service supports credential store.
   *
   * @param credentialStoreSupported - true or false
   */
  @Override
  public void setCredentialStoreSupported(boolean credentialStoreSupported) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting CredentialStoreEnabled of Service" + ", clusterName="
              + cluster.getClusterName() + ", clusterId="
              + cluster.getClusterId() + ", serviceName=" + getName()
              + ", oldCredentialStoreSupported=" + isCredentialStoreSupported()
              + ", newCredentialStoreSupported=" + credentialStoreSupported);
    }

    ServiceDesiredStateEntity desiredStateEntity = getServiceDesiredStateEntity();

    if (desiredStateEntity != null) {
      desiredStateEntity.setCredentialStoreSupported(credentialStoreSupported);
      desiredStateEntity = serviceDesiredStateDAO.merge(desiredStateEntity);

    } else {
      LOG.warn("Setting a member on an entity object that may have been "
              + "previously deleted, serviceName = " + getName());
    }
  }

  /**
   * Get a true or false value specifying whether
   * credential store use is enabled for this service.
   *
   * @return true or false
   */
  @Override
  public boolean isCredentialStoreEnabled() {
    ServiceDesiredStateEntity desiredStateEntity = getServiceDesiredStateEntity();

    if (desiredStateEntity != null) {
      return desiredStateEntity.isCredentialStoreEnabled();
    } else {
      LOG.warn("Trying to fetch a member from an entity object that may " +
              "have been previously deleted, serviceName = " + getName());
    }
    return false;
  }


  /**
   * Set a true or false value specifying whether this
   * service is to be enabled for credential store use.
   *
   * @param credentialStoreEnabled - true or false
   */
  @Override
  public void setCredentialStoreEnabled(boolean credentialStoreEnabled) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting CredentialStoreEnabled of Service" + ", clusterName="
              + cluster.getClusterName() + ", clusterId="
              + cluster.getClusterId() + ", serviceName=" + getName()
              + ", oldCredentialStoreEnabled=" + isCredentialStoreEnabled()
              + ", newCredentialStoreEnabled=" + credentialStoreEnabled);
    }

    ServiceDesiredStateEntity desiredStateEntity = getServiceDesiredStateEntity();

    if (desiredStateEntity != null) {
      desiredStateEntity.setCredentialStoreEnabled(credentialStoreEnabled);
      desiredStateEntity = serviceDesiredStateDAO.merge(desiredStateEntity);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
              + "previously deleted, serviceName = " + getName());
    }
  }

  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("Service={ serviceName=" + getName() + ", clusterName=" + cluster.getClusterName()
        + ", clusterId=" + cluster.getClusterId() + ", desiredStackVersion="
        + getDesiredStackVersion() + ", desiredState=" + getDesiredState().toString()
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
  }

  /**
   *
   */
  private void persist(ClusterServiceEntity serviceEntity) {
    persistEntities(serviceEntity);
    refresh();

    // publish the service installed event
    StackId stackId = cluster.getDesiredStackVersion();
    cluster.addService(this);

    ServiceInstalledEvent event = new ServiceInstalledEvent(getClusterId(), stackId.getStackName(),
        stackId.getStackVersion(), getName());

    eventPublisher.publish(event);
  }

  @Transactional
  void persistEntities(ClusterServiceEntity serviceEntity) {
    long clusterId = cluster.getClusterId();
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    serviceEntity.setClusterEntity(clusterEntity);
    clusterServiceDAO.create(serviceEntity);
    clusterEntity.getClusterServiceEntities().add(serviceEntity);
    clusterDAO.merge(clusterEntity);
    clusterServiceDAO.merge(serviceEntity);
  }

  @Transactional
  public void refresh() {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(getClusterId());
    pk.setServiceName(getName());
    ClusterServiceEntity serviceEntity = getServiceEntity();
    clusterServiceDAO.refresh(serviceEntity);
    serviceDesiredStateDAO.refresh(serviceEntity.getServiceDesiredStateEntity());
  }

  @Override
  public boolean canBeRemoved() {
    //
    // A service can be deleted if all it's components
    // can be removed, irrespective of the state of
    // the service itself.
    //
    for (ServiceComponent sc : components.values()) {
      if (!sc.canBeRemoved()) {
        LOG.warn("Found non removable component when trying to delete service" + ", clusterName="
            + cluster.getClusterName() + ", serviceName=" + getName() + ", componentName="
            + sc.getName());
        return false;
      }
    }
    return true;
  }

  @Transactional
  void deleteAllServiceConfigs() throws AmbariException {
    ArrayList<String> serviceConfigTypes = new ArrayList<>();
    ServiceConfigEntity lastServiceConfigEntity = serviceConfigDAO.findMaxVersion(getClusterId(), getName());
    //ensure service config version exist
    if (lastServiceConfigEntity != null) {
      for (ClusterConfigEntity configEntity : lastServiceConfigEntity.getClusterConfigEntities()) {
        serviceConfigTypes.add(configEntity.getType());
      }

      LOG.info("Deselecting config mapping for cluster, clusterId={}, configTypes={} ",
          getClusterId(), serviceConfigTypes);

      List<ClusterConfigMappingEntity> configMappingEntities =
          clusterDAO.getSelectedConfigMappingByTypes(getClusterId(), serviceConfigTypes);

      for (ClusterConfigMappingEntity configMappingEntity : configMappingEntities) {
        configMappingEntity.setSelected(0);
      }

      clusterDAO.mergeConfigMappings(configMappingEntities);
    }

    LOG.info("Deleting all serviceconfigs for service"
        + ", clusterName=" + cluster.getClusterName()
        + ", serviceName=" + getName());

    List<ServiceConfigEntity> serviceConfigEntities =
      serviceConfigDAO.findByService(cluster.getClusterId(), getName());

    for (ServiceConfigEntity serviceConfigEntity : serviceConfigEntities) {
      // Only delete the historical version information and not original
      // config data
      serviceConfigDAO.remove(serviceConfigEntity);
    }
  }

  @Override
  @Transactional
  public void deleteAllComponents() throws AmbariException {
    lock.lock();
    try {
      LOG.info("Deleting all components for service" + ", clusterName=" + cluster.getClusterName()
          + ", serviceName=" + getName());
      // FIXME check dependencies from meta layer
      for (ServiceComponent component : components.values()) {
        if (!component.canBeRemoved()) {
          throw new AmbariException("Found non removable component when trying to"
              + " delete all components from service" + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + getName() + ", componentName=" + component.getName());
        }
      }

      for (ServiceComponent serviceComponent : components.values()) {
        serviceComponent.delete();
      }

      components.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void deleteServiceComponent(String componentName)
      throws AmbariException {
    lock.lock();
    try {
      ServiceComponent component = getServiceComponent(componentName);
      LOG.info("Deleting servicecomponent for cluster" + ", clusterName=" + cluster.getClusterName()
          + ", serviceName=" + getName() + ", componentName=" + componentName);
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
      lock.unlock();
    }
  }

  @Override
  public boolean isClientOnlyService() {
    return isClientOnlyService;
  }

  @Override
  @Transactional
  public void delete() throws AmbariException {
    deleteAllComponents();
    deleteAllServiceConfigs();

    removeEntities();

    // publish the service removed event
    StackId stackId = cluster.getDesiredStackVersion();

    ServiceRemovedEvent event = new ServiceRemovedEvent(getClusterId(), stackId.getStackName(),
        stackId.getStackVersion(), getName());

    eventPublisher.publish(event);
  }

  @Transactional
  protected void removeEntities() throws AmbariException {
    serviceDesiredStateDAO.removeByPK(serviceDesiredStateEntityPK);

    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(getClusterId());
    pk.setServiceName(getName());

    clusterServiceDAO.removeByPK(pk);
  }

  @Override
  public void setMaintenanceState(MaintenanceState state) {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setMaintenanceState(state);
    serviceDesiredStateDAO.merge(serviceDesiredStateEntity);

    // broadcast the maintenance mode change
    MaintenanceModeEvent event = new MaintenanceModeEvent(state, this);
    eventPublisher.publish(event);
  }

  @Override
  public MaintenanceState getMaintenanceState() {
    return getServiceDesiredStateEntity().getMaintenanceState();
  }

  private ClusterServiceEntity getServiceEntity() {
    return clusterServiceDAO.findByPK(serviceEntityPK);
  }

  private ClusterServiceEntityPK getServiceEntityPK(ClusterServiceEntity serviceEntity) {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(serviceEntity.getClusterId());
    pk.setServiceName(serviceEntity.getServiceName());
    return pk;
  }

  private ServiceDesiredStateEntityPK getServiceDesiredStateEntityPK(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    ServiceDesiredStateEntityPK pk = new ServiceDesiredStateEntityPK();
    pk.setClusterId(serviceDesiredStateEntity.getClusterId());
    pk.setServiceName(serviceDesiredStateEntity.getServiceName());
    return pk;
  }

  // Refresh the cached reference on setters
  private ServiceDesiredStateEntity getServiceDesiredStateEntity() {
    return serviceDesiredStateDAO.findByPK(serviceDesiredStateEntityPK);
  }
}
