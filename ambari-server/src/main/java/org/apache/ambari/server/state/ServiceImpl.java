/*
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.ServiceKey;
import org.apache.ambari.server.controller.ServiceDependencyResponse;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.ServiceRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.serveraction.kerberos.Component;
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
  private final ServiceGroup serviceGroup;
  private final ConcurrentMap<String, ServiceComponent> componentsByName = new ConcurrentHashMap<>();
  private final ConcurrentMap<Long, ServiceComponent> componentsById = new ConcurrentHashMap<>();
  private List<ServiceKey> serviceDependencies = new ArrayList<>();
  private boolean isClientOnlyService;
  private boolean isCredentialStoreSupported;
  private boolean isCredentialStoreRequired;
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private Clusters clusters;

  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  private final ClusterServiceDAO clusterServiceDAO;
  private final ServiceDesiredStateDAO serviceDesiredStateDAO;
  private final ClusterDAO clusterDAO;
  private final ServiceGroupDAO serviceGroupDAO;
  private final ServiceComponentFactory serviceComponentFactory;

  /**
   * Used to publish events relating to service CRUD operations.
   */
  private final AmbariEventPublisher eventPublisher;

  /**
   * The id of the service.
   */
  private Long serviceId;

  /**
   * The name of the service.
   */
  private final String serviceName;
  private final String displayName;

  /**
   * The stack service name.
   */
  private final String serviceType;

  @AssistedInject
  ServiceImpl(@Assisted Cluster cluster, @Assisted ServiceGroup serviceGroup,
              @Assisted List<ServiceKey> serviceDependencies,
              @Assisted("serviceName") String serviceName, @Assisted("serviceType") String serviceType,
              @Assisted RepositoryVersionEntity desiredRepositoryVersion,
              ClusterDAO clusterDAO, ServiceGroupDAO serviceGroupDAO,
              ClusterServiceDAO clusterServiceDAO, ServiceDesiredStateDAO serviceDesiredStateDAO,
              ServiceComponentFactory serviceComponentFactory, AmbariMetaInfo ambariMetaInfo,
              AmbariEventPublisher eventPublisher) throws AmbariException {
    this.cluster = cluster;
    this.serviceGroup = serviceGroup;
    this.clusterDAO = clusterDAO;
    this.serviceGroupDAO = serviceGroupDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceDesiredStateDAO = serviceDesiredStateDAO;
    this.serviceComponentFactory = serviceComponentFactory;
    this.eventPublisher = eventPublisher;
    this.serviceName = serviceName;
    this.serviceType = serviceType;
    this.ambariMetaInfo = ambariMetaInfo;


    ClusterServiceEntity serviceEntity = new ClusterServiceEntity();
    serviceEntity.setClusterId(cluster.getClusterId());
    serviceEntity.setServiceGroupId(serviceGroup.getServiceGroupId());
    serviceEntity.setServiceName(serviceName);
    serviceEntity.setServiceType(serviceType);

    if (serviceDependencies != null) {
      for (ServiceKey serviceKey : serviceDependencies) {

        Cluster dependencyCluster = null;

        for (Cluster cl : clusters.getClusters().values()) {
          if (cl.getServicesById().containsKey(serviceKey.getServiceId())) {
            dependencyCluster = cl;

            break;
          }
        }


        ClusterServiceEntity dependencyServiceEntity = clusterServiceDAO.findById(serviceKey.getClusterId(), serviceKey.getServiceGroupId(), serviceKey.getServiceId());
        ServiceDependencyEntity serviceDependencyEntity = new ServiceDependencyEntity();
        serviceDependencyEntity.setService(serviceEntity);
        serviceDependencyEntity.setServiceDependency(dependencyServiceEntity);

        clusterServiceDAO.createServiceDependency(serviceDependencyEntity);

        serviceEntity.getServiceDependencies().add(serviceDependencyEntity);

      }
    }

    ServiceDesiredStateEntity serviceDesiredStateEntity = new ServiceDesiredStateEntity();
    serviceDesiredStateEntity.setClusterId(cluster.getClusterId());
    serviceDesiredStateEntity.setServiceGroupId(serviceGroup.getServiceGroupId());
    serviceDesiredStateEntity.setDesiredRepositoryVersion(desiredRepositoryVersion);
    serviceDesiredStateEntityPK = getServiceDesiredStateEntityPK(serviceDesiredStateEntity);
    serviceEntityPK = getServiceEntityPK(serviceEntity);

    serviceDesiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);

    StackId stackId = desiredRepositoryVersion.getStackId();

    ServiceInfo sInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceType);

    displayName = sInfo.getDisplayName();
    isClientOnlyService = sInfo.isClientOnlyService();
    isCredentialStoreSupported = sInfo.isCredentialStoreSupported();
    isCredentialStoreRequired = sInfo.isCredentialStoreRequired();

    persist(serviceEntity);
  }

  @AssistedInject
  ServiceImpl(@Assisted Cluster cluster, @Assisted ServiceGroup serviceGroup,
              @Assisted ClusterServiceEntity serviceEntity,
              ClusterDAO clusterDAO, ServiceGroupDAO serviceGroupDAO,
              ClusterServiceDAO clusterServiceDAO, ServiceDesiredStateDAO serviceDesiredStateDAO,
              ServiceComponentFactory serviceComponentFactory, AmbariMetaInfo ambariMetaInfo,
              AmbariEventPublisher eventPublisher) throws AmbariException {
    this.cluster = cluster;
    this.serviceGroup = serviceGroup;
    this.clusterDAO = clusterDAO;
    this.serviceGroupDAO = serviceGroupDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceDesiredStateDAO = serviceDesiredStateDAO;
    this.serviceComponentFactory = serviceComponentFactory;
    this.eventPublisher = eventPublisher;
    serviceId = serviceEntity.getServiceId();
    serviceName = serviceEntity.getServiceName();
    serviceType = serviceEntity.getServiceType();
    this.ambariMetaInfo = ambariMetaInfo;
    serviceDependencies = getServiceDependencies(serviceEntity.getServiceDependencies());

    ServiceDesiredStateEntity serviceDesiredStateEntity = serviceEntity.getServiceDesiredStateEntity();
    serviceDesiredStateEntityPK = getServiceDesiredStateEntityPK(serviceDesiredStateEntity);
    serviceEntityPK = getServiceEntityPK(serviceEntity);

    if (!serviceEntity.getServiceComponentDesiredStateEntities().isEmpty()) {
      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity
          : serviceEntity.getServiceComponentDesiredStateEntities()) {
        try {
            ServiceComponent svcComponent = serviceComponentFactory.createExisting(this,
                    serviceComponentDesiredStateEntity);
            componentsByName.put(serviceComponentDesiredStateEntity.getComponentName(), svcComponent);
            componentsById.put(serviceComponentDesiredStateEntity.getId(), svcComponent);

        } catch(ProvisionException ex) {
            StackId stackId = new StackId(serviceComponentDesiredStateEntity.getDesiredStack());
            LOG.error(String.format("Can not get component info: stackName=%s, stackVersion=%s, serviceName=%s, componentName=%s",
              stackId.getStackName(), stackId.getStackVersion(),
              serviceEntity.getServiceName(), serviceComponentDesiredStateEntity.getComponentName()));
            ex.printStackTrace();
          }
      }
    }

    StackId stackId = getDesiredStackId();
    ServiceInfo sInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), getServiceType());
    isClientOnlyService = sInfo.isClientOnlyService();
    isCredentialStoreSupported = sInfo.isCredentialStoreSupported();
    isCredentialStoreRequired = sInfo.isCredentialStoreRequired();
    displayName = sInfo.getDisplayName();
  }




  /***
   * Refresh Service info due to current stack
   * @throws AmbariException
   */
  @Override
  public void updateServiceInfo() throws AmbariException {
    try {
      ServiceInfo serviceInfo = ambariMetaInfo.getService(this);

      isClientOnlyService = serviceInfo.isClientOnlyService();
      isCredentialStoreSupported = serviceInfo.isCredentialStoreSupported();
      isCredentialStoreRequired = serviceInfo.isCredentialStoreRequired();

    } catch (ObjectNotFoundException e) {
      throw new RuntimeException("Trying to create a ServiceInfo"
              + " not recognized in stack info"
              + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + getName()
              + ", stackInfo=" + getDesiredStackId().getStackName());
    }
  }

  @Override
  public String getName() { return serviceName; }

  @Override
  public String getDisplayName() { return displayName; }

  @Override
  public String getServiceType() {
    return serviceType;
  }

  @Override
  public Long getClusterId() {
    return cluster.getClusterId();
  }

  @Override
  public Long getServiceGroupId() {
    return serviceGroup.getServiceGroupId();
  }

  @Override
  public String getServiceGroupName() {
    return serviceGroup.getServiceGroupName();
  }

  @Override
  public Long getServiceId() { return serviceId; }

  @Override
  public Map<String, ServiceComponent> getServiceComponents() {
    return new HashMap<>(componentsByName);
  }

  @Override
  public void addServiceComponents(
      Map<String, ServiceComponent> componentsByName) throws AmbariException {
    for (ServiceComponent sc : componentsByName.values()) {
      addServiceComponent(sc);
    }
  }

  @Override
  public List<ServiceKey> getServiceDependencies() {
    return serviceDependencies;
  }

  public void setServiceDependencies(List<ServiceKey> serviceDependencies) {
    this.serviceDependencies = serviceDependencies;
  }

  @Override
  public void addServiceComponent(ServiceComponent component) throws AmbariException {
    if (componentsByName.containsKey(component.getName())) {
      throw new AmbariException("Cannot add duplicate ServiceComponent"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceType=" + getServiceType()
          + ", serviceName=" + getName()
          + ", serviceComponentName=" + component.getName());
    }

    componentsByName.put(component.getName(), component);
  }

  @Override
  public ServiceComponent addServiceComponent(String serviceComponentName, String serviceComponentType)
      throws AmbariException {
    ServiceComponent component = serviceComponentFactory.createNew(this, serviceComponentName, serviceComponentType);
    addServiceComponent(component);
    return component;
  }

  @Override
  public ServiceComponent getServiceComponent(String componentName)
      throws AmbariException {
    ServiceComponent serviceComponent = componentsByName.get(componentName);
    if (null == serviceComponent) {
      throw new ServiceComponentNotFoundException(cluster.getClusterName(),
          getName(), getServiceType(), serviceGroup.getServiceGroupName(), componentName);
    }

    return serviceComponent;
  }

  @Override
  public Set<ServiceDependencyResponse> getServiceDependencyResponses() {
    Set<ServiceDependencyResponse> responses = new HashSet<>();
    if (getServiceDependencies() != null) {
      for (ServiceKey sk : getServiceDependencies()) {
        responses.add(new ServiceDependencyResponse(cluster.getClusterId(), cluster.getClusterName(),
                 sk.getClusterId(), sk.getClusterName(), sk.getServiceGroupId(), sk.getServiceGroupName(),
                 sk.getServiceId(), sk.getServiceName(), getServiceGroupId(), getServiceGroupName(),
                 getServiceId(), getName(), sk.getDependencyId()));
      }
    }
    return responses;
  }

  public List<ServiceKey> getServiceDependencies(List<ServiceDependencyEntity> serviceDependencyEntities) throws AmbariException {
    List<ServiceKey> serviceDependenciesList = new ArrayList<>();

    if (serviceDependencyEntities != null) {
      for (ServiceDependencyEntity sde : serviceDependencyEntities) {
        ServiceKey serviceKey = new ServiceKey();
        ClusterServiceEntity dependencyService = sde.getServiceDependency();
        String clusterName = "";
        Long clusterId = null;
        if (dependencyService.getClusterId() == cluster.getClusterId()) {
          clusterName = cluster.getClusterName();
          clusterId = cluster.getClusterId();
        } else {
          ClusterEntity clusterEntity = clusterDAO.findById(dependencyService.getClusterId());
          if (clusterEntity != null) {
            clusterName = clusterEntity.getClusterName();
            clusterId = clusterEntity.getClusterId();
          } else {
            LOG.error("Unable to get cluster id for service " + dependencyService.getServiceName());
          }
        }


        Cluster dependencyCluster = cluster;
        ServiceGroup dependencyServiceGroup = dependencyCluster.getServiceGroup(dependencyService.getServiceGroupId());


        serviceKey.setServiceGroupName(dependencyServiceGroup.getServiceGroupName());
        serviceKey.setServiceGroupId(dependencyServiceGroup.getServiceGroupId());
        serviceKey.setClusterName(clusterName);
        serviceKey.setClusterId(clusterId);
        serviceKey.setServiceName(dependencyService.getServiceName());
        serviceKey.setServiceId(dependencyService.getServiceId());
        serviceKey.setDependencyId(sde.getServiceDependencyId());
        serviceDependenciesList.add(serviceKey);
      }
    }
    return serviceDependenciesList;
  }

  @Override
  public State getDesiredState() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    return serviceDesiredStateEntity.getDesiredState();
  }

  @Override
  public void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service, clusterName={}, clusterId={}, serviceName={}, oldDesiredState={}, newDesiredState={}",
        cluster.getClusterName(), cluster.getClusterId(), getName(), getDesiredState(), state);
    }

    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredState(state);
    serviceDesiredStateDAO.merge(serviceDesiredStateEntity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StackId getDesiredStackId() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();

    if (null == serviceDesiredStateEntity) {
      return null;
    } else {
      StackEntity desiredStackEntity = serviceDesiredStateEntity.getDesiredStack();
      return new StackId(desiredStackEntity);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public RepositoryVersionEntity getDesiredRepositoryVersion() {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    return serviceDesiredStateEntity.getDesiredRepositoryVersion();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public void setDesiredRepositoryVersion(RepositoryVersionEntity repositoryVersionEntity) {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredRepositoryVersion(repositoryVersionEntity);
    serviceDesiredStateDAO.merge(serviceDesiredStateEntity);

    Collection<ServiceComponent> componentsByName = getServiceComponents().values();
    for (ServiceComponent component : componentsByName) {
      component.setDesiredRepositoryVersion(repositoryVersionEntity);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  public RepositoryVersionState getRepositoryState() {
    if (componentsByName.isEmpty()) {
      return RepositoryVersionState.NOT_REQUIRED;
    }

    List<RepositoryVersionState> states = new ArrayList<>();
    for( ServiceComponent component : componentsByName.values() ){
      states.add(component.getRepositoryState());
    }

    return RepositoryVersionState.getAggregateState(states);
  }

  @Override
  public ServiceResponse convertToResponse() {
    RepositoryVersionEntity desiredRespositoryVersion = getDesiredRepositoryVersion();
    Mpack mpack = ambariMetaInfo.getMpack(serviceGroup.getMpackId());
    Module module = mpack.getModule(getName());

    ServiceResponse r = new ServiceResponse(cluster.getClusterId(), cluster.getClusterName(),
        serviceGroup.getServiceGroupId(), serviceGroup.getServiceGroupName(),
        getServiceId(), getName(), getServiceType(), serviceGroup.getStackId(), module.getVersion(),
        getRepositoryState(), getDesiredState().toString(), isCredentialStoreSupported(), isCredentialStoreEnabled());

    r.setDesiredRepositoryVersionId(desiredRespositoryVersion.getId());

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
    return isCredentialStoreSupported;
  }

  /**
   * Get a true or false value specifying whether
   * credential store is required by this service.
   *
   * @return true or false
   */
  @Override
  public boolean isCredentialStoreRequired() {
    return isCredentialStoreRequired;
  }

  @Override
  public String toString() {
    return "ServiceImpl{" +
        "serviceId=" + serviceId +
        ", serviceName='" + serviceName + '\'' +
        ", displayName='" + displayName + '\'' +
        ", serviceType='" + serviceType + '\'' +
        '}';
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
      LOG.debug("Setting CredentialStoreEnabled of Service, clusterName={}, clusterId={}, serviceName={}, oldCredentialStoreEnabled={}, newCredentialStoreEnabled={}",
        cluster.getClusterName(), cluster.getClusterId(), getName(), isCredentialStoreEnabled(), credentialStoreEnabled);
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
  public ClusterServiceEntity removeDependencyService(Long dependencyServiceId) {
    ClusterServiceEntity currentServiceEntity = clusterServiceDAO.findById(getClusterId(), getServiceGroupId(), getServiceId());

    ServiceDependencyEntity dependencyEntityToRemove = null;
    if (currentServiceEntity.getServiceDependencies() != null) {
      for (ServiceDependencyEntity sde : currentServiceEntity.getServiceDependencies()) {
        if (sde.getServiceDependency().getServiceId() == dependencyServiceId) {
          dependencyEntityToRemove = sde;
          break;
        }
      }
    }

    currentServiceEntity.getServiceDependencies().remove(dependencyEntityToRemove);
    ClusterServiceEntity updatedServiceEntity = removeServiceDependencyEntity(dependencyEntityToRemove, currentServiceEntity);
    currentServiceEntity.getServiceDependencies().remove(dependencyEntityToRemove);

    return updatedServiceEntity;
  }

  @Transactional
  protected ClusterServiceEntity removeServiceDependencyEntity(ServiceDependencyEntity dependencyEntityToRemove,
                                                               ClusterServiceEntity currentServiceEntity) {
    clusterServiceDAO.removeServiceDependency(dependencyEntityToRemove);
    ClusterServiceEntity updatedServiceEntity = clusterServiceDAO.merge(currentServiceEntity);
    return updatedServiceEntity;
  }

  @Override
  public ClusterServiceEntity addDependencyService(Long dependencyServiceId) throws AmbariException {
    Service dependentService = null;
    for (Cluster cl : clusters.getClusters().values()) {
      if (cl.getServicesById().containsKey(dependencyServiceId)) {
        dependentService = cl.getService(dependencyServiceId);
        break;
      }
    }

    ClusterServiceEntity currentServiceEntity = clusterServiceDAO.findById(getClusterId(), getServiceGroupId(), getServiceId());
    ClusterServiceEntity dependentServiceEntity = clusterServiceDAO.findById(dependentService.getClusterId(),
            dependentService.getServiceGroupId(), dependentService.getServiceId());

    ServiceDependencyEntity newServiceDependency = new ServiceDependencyEntity();
    newServiceDependency.setService(currentServiceEntity);
    newServiceDependency.setServiceDependency(dependentServiceEntity);

    return addServiceDependencyEntity(newServiceDependency, currentServiceEntity);
  }

  @Transactional
  protected ClusterServiceEntity addServiceDependencyEntity(ServiceDependencyEntity newServiceDependency,
                                                               ClusterServiceEntity currentServiceEntity) {
    clusterServiceDAO.createServiceDependency(newServiceDependency);
    currentServiceEntity.getServiceDependencies().add(newServiceDependency);
    ClusterServiceEntity updatedServiceEntity = clusterServiceDAO.merge(currentServiceEntity);
    return updatedServiceEntity;
  }

  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("Service={ serviceName=").append(getName())
      .append(", serviceType=").append(getServiceType())
      .append(", clusterName=").append(cluster.getClusterName())
      .append(", clusterId=").append(cluster.getClusterId())
      .append(", desiredStackVersion=").append(getDesiredStackId())
      .append(", desiredState=").append(getDesiredState())
      .append(", componentsByName=[ ");
    boolean first = true;
    for (ServiceComponent sc : componentsByName.values()) {
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

    // publish the service installed event
    StackId stackId = getDesiredStackId();
    cluster.addService(this);

    ServiceInstalledEvent event = new ServiceInstalledEvent(getClusterId(), stackId.getStackName(),
        stackId.getStackVersion(), getName(), serviceType, serviceGroup.getServiceGroupName());

    eventPublisher.publish(event);
  }

  @Transactional
  void persistEntities(ClusterServiceEntity serviceEntity) {
    long clusterId = cluster.getClusterId();
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByClusterAndServiceGroupIds(
      clusterId, serviceGroup.getServiceGroupId());
    serviceEntity.setServiceGroupEntity(serviceGroupEntity);
    serviceEntity.setClusterEntity(clusterEntity);
    clusterServiceDAO.create(serviceEntity);
    serviceId = serviceEntity.getServiceId();
    serviceEntityPK.setServiceId(serviceId);
    serviceDesiredStateEntityPK.setServiceId(serviceId);
    clusterEntity.getClusterServiceEntities().add(serviceEntity);
    serviceEntity.getServiceDesiredStateEntity().setServiceId(serviceId);
    clusterDAO.merge(clusterEntity);
    serviceGroupDAO.merge(serviceGroupEntity);
    clusterServiceDAO.merge(serviceEntity);
  }


  @Override
  public boolean canBeRemoved() {
    //
    // A service can be deleted if all it's componentsByName
    // can be removed, irrespective of the state of
    // the service itself.
    //
    for (ServiceComponent sc : componentsByName.values()) {
      if (!sc.canBeRemoved()) {
        LOG.warn("Found non-removable component when trying to delete service" + ", clusterName="
            + cluster.getClusterName() + ", serviceName=" + getName() + ", serviceType="
            + getServiceType() + ", componentName=" + sc.getName());
        return false;
      }
    }
    return true;
  }

  @Transactional
  void deleteAllServiceConfigs() throws AmbariException {
    long clusterId = getClusterId();
    ServiceConfigEntity lastServiceConfigEntity = serviceConfigDAO.findMaxVersion(clusterId, getServiceId());
    // de-select every configuration from the service
    if (lastServiceConfigEntity != null) {
      for (ClusterConfigEntity serviceConfigEntity : lastServiceConfigEntity.getClusterConfigEntities()) {
        LOG.info("Disabling configuration {}", serviceConfigEntity);
        serviceConfigEntity.setSelected(false);
        serviceConfigEntity.setUnmapped(true);
        clusterDAO.merge(serviceConfigEntity);
      }
    }

    LOG.info("Deleting all configuration associations for {} on cluster {}", getName(), cluster.getClusterName());

    List<ServiceConfigEntity> serviceConfigEntities =
      serviceConfigDAO.findByService(cluster.getClusterId(), getServiceId());

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
      LOG.info("Deleting all componentsByName for service" + ", clusterName=" + cluster.getClusterName()
          + ", serviceName=" + getName());
      // FIXME check dependencies from meta layer
      for (ServiceComponent component : componentsByName.values()) {
        if (!component.canBeRemoved()) {
          throw new AmbariException("Found non removable component when trying to"
              + " delete all componentsByName from service" + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + getName() + ", componentName=" + component.getName());
        }
      }

      for (ServiceComponent serviceComponent : componentsByName.values()) {
        serviceComponent.delete();
      }

      componentsByName.clear();
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
      componentsByName.remove(componentName);
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
    List<Component> componentsByName = getComponents(); // XXX temporal coupling, need to call this BEFORE deletingAllComponents
    deleteAllComponents();
    deleteAllServiceConfigs();

    StackId stackId = getDesiredStackId();

    removeEntities();

    // publish the service removed event
    if (null == stackId) {
      return;
    }

    ServiceRemovedEvent event = new ServiceRemovedEvent(getClusterId(), stackId.getStackName(), stackId.getStackVersion(),
                                                        getName(), getServiceType(),
                                                        serviceGroup.getServiceGroupName(), componentsByName);

    eventPublisher.publish(event);
  }

  private List<Component> getComponents() {
    List<Component> result = new ArrayList<>();
    for (ServiceComponent component : getServiceComponents().values()) {
      for (ServiceComponentHost host : component.getServiceComponentHosts().values()) {
        result.add(new Component(host.getHostName(), getName(), component.getName(), host.getHost().getHostId()));
      }
    }
    return result;
  }

  @Transactional
  protected void removeEntities() throws AmbariException {
    serviceDesiredStateDAO.removeByPK(serviceDesiredStateEntityPK);
    clusterServiceDAO.removeByPK(serviceEntityPK);
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

  private ClusterServiceEntityPK getServiceEntityPK(ClusterServiceEntity serviceEntity) {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(serviceEntity.getClusterId());
    pk.setServiceGroupId(serviceEntity.getServiceGroupId());
    pk.setServiceId(serviceEntity.getServiceId());
    return pk;
  }

  private ServiceDesiredStateEntityPK getServiceDesiredStateEntityPK(ServiceDesiredStateEntity serviceDesiredStateEntity) {
    ServiceDesiredStateEntityPK pk = new ServiceDesiredStateEntityPK();
    pk.setClusterId(serviceDesiredStateEntity.getClusterId());
    pk.setServiceGroupId(serviceDesiredStateEntity.getServiceGroupId());
    pk.setServiceId(serviceDesiredStateEntity.getServiceId());
    return pk;
  }

  // Refresh the cached reference on setters
  private ServiceDesiredStateEntity getServiceDesiredStateEntity() {
    return serviceDesiredStateDAO.findByPK(serviceDesiredStateEntityPK);
  }
}
