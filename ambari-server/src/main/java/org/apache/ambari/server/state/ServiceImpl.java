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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.ServiceKey;
import org.apache.ambari.server.collections.Predicate;
import org.apache.ambari.server.collections.PredicateUtils;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ServiceDependencyResponse;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.internal.AmbariServerSSOConfigurationHandler;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceCredentialStoreUpdateEvent;
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
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.serveraction.kerberos.Component;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


public class ServiceImpl implements Service {
  private final Lock lock = new ReentrantLock();
  private ClusterServiceEntity serviceEntity;

  private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);

  private final Cluster cluster;
  private final ServiceGroup serviceGroup;
  private final ConcurrentMap<String, ServiceComponent> componentsByName = new ConcurrentHashMap<>();
  private final ConcurrentMap<Long, ServiceComponent> componentsById = new ConcurrentHashMap<>();
  private List<ServiceKey> serviceDependencies = new ArrayList<>();
  private boolean isClientOnlyService;
  private boolean isCredentialStoreSupported;
  private boolean isCredentialStoreRequired;
  private final boolean ssoIntegrationSupported;
  private final Predicate ssoEnabledTest;
  private final boolean ssoRequiresKerberos;
  private final Predicate kerberosEnabledTest;
  private AmbariMetaInfo ambariMetaInfo;
  private AtomicReference<MaintenanceState> maintenanceState = new AtomicReference<>();

  @Inject
  private Clusters clusters;

  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  @Inject
  private AmbariManagementController ambariManagementController;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private AmbariServerSSOConfigurationHandler ambariServerConfigurationHandler;

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
      @Assisted List<ServiceKey> serviceDependencies, @Assisted("serviceName") String serviceName,
      @Assisted("serviceType") String serviceType, ClusterDAO clusterDAO,
      ServiceGroupDAO serviceGroupDAO, ClusterServiceDAO clusterServiceDAO,
      ServiceDesiredStateDAO serviceDesiredStateDAO,
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


    serviceEntity = new ClusterServiceEntity();
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


        ClusterServiceEntity dependencyServiceEntity = clusterServiceDAO.findByPK(serviceKey.getServiceId());
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

    serviceDesiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);

    StackId stackId = serviceGroup.getStackId();

    ServiceInfo sInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceType);

    displayName = sInfo.getDisplayName();
    isClientOnlyService = sInfo.isClientOnlyService();
    isCredentialStoreSupported = sInfo.isCredentialStoreSupported();
    isCredentialStoreRequired = sInfo.isCredentialStoreRequired();
    ssoIntegrationSupported = sInfo.isSingleSignOnSupported();
    ssoEnabledTest = compileSsoEnabledPredicate(sInfo);
    ssoRequiresKerberos = sInfo.isKerberosRequiredForSingleSignOnIntegration();
    kerberosEnabledTest = compileKerberosEnabledPredicate(sInfo);

    if (ssoIntegrationSupported && ssoRequiresKerberos && (kerberosEnabledTest == null)) {
      LOG.warn("The service, {}, requires Kerberos to be enabled for SSO integration support; " +
              "however, the kerberosEnabledTest specification has not been specified in the metainfo.xml file. " +
              "Automated SSO integration will not be allowed for this service.",
          serviceName);
    }

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

    if (!serviceEntity.getServiceComponentDesiredStateEntities().isEmpty()) {
      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity
          : serviceEntity.getServiceComponentDesiredStateEntities()) {
        try {
            ServiceComponent svcComponent = serviceComponentFactory.createExisting(this,
                    serviceComponentDesiredStateEntity);
            componentsByName.put(serviceComponentDesiredStateEntity.getComponentName(), svcComponent);
            componentsById.put(serviceComponentDesiredStateEntity.getId(), svcComponent);

        } catch(ProvisionException ex) {
            StackId stackId = getStackId();
            LOG.error(String.format("Can not get component info: stackName=%s, stackVersion=%s, serviceName=%s, componentName=%s",
              stackId.getStackName(), stackId.getStackVersion(),
              serviceEntity.getServiceName(), serviceComponentDesiredStateEntity.getComponentName()));
          ex.printStackTrace();
        }
      }
    }

    StackId stackId = serviceGroup.getStackId();
    ServiceInfo sInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), getServiceType());
    isClientOnlyService = sInfo.isClientOnlyService();
    isCredentialStoreSupported = sInfo.isCredentialStoreSupported();
    isCredentialStoreRequired = sInfo.isCredentialStoreRequired();
    displayName = sInfo.getDisplayName();
    ssoIntegrationSupported = sInfo.isSingleSignOnSupported();
    ssoEnabledTest = compileSsoEnabledPredicate(sInfo);
    ssoRequiresKerberos = sInfo.isKerberosRequiredForSingleSignOnIntegration();
    kerberosEnabledTest = compileKerberosEnabledPredicate(sInfo);

    if (ssoIntegrationSupported && ssoRequiresKerberos && (kerberosEnabledTest == null)) {
      LOG.warn("The service, {}, requires Kerberos to be enabled for SSO integration support; " +
              "however, the kerberosEnabledTest specification has not been specified in the metainfo.xml file. " +
              "Automated SSO integration will not be allowed for this service.",
          serviceName);
    }
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
              + ", serviceName=" + getName());
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
  public Set<String> getServiceHosts() {
    Set<String> hostNames = new HashSet<>();
    for (ServiceComponent serviceComponent : getServiceComponents().values()) {
      hostNames.addAll(serviceComponent.getServiceComponentsHosts());
    }
    return hostNames;
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
  public StackId getStackId() {
    return serviceGroup.getStackId();
  }

  @Override
  public ServiceResponse convertToResponse() {
    Mpack mpack = ambariMetaInfo.getMpack(serviceGroup.getMpackId());
    Module module = mpack.getModule(getServiceType());

    Map<String, Map<String, String>> existingConfigurations;

    try {
      existingConfigurations = configHelper.calculateExistingConfigurations(ambariManagementController, cluster);
    } catch (AmbariException e) {
      LOG.warn("Failed to get the existing configurations for the cluster.  Predicate calculations may not be correct due to missing data.");
      existingConfigurations = Collections.emptyMap();
    }

    ServiceResponse r = new ServiceResponse(cluster.getClusterId(), cluster.getClusterName(),
        serviceGroup.getServiceGroupId(), serviceGroup.getServiceGroupName(),
        getServiceId(), getName(), getServiceType(), serviceGroup.getStackId(), module.getVersion(),
        getDesiredState().toString(), isCredentialStoreSupported(), isCredentialStoreEnabled(),
        ssoIntegrationSupported, isSsoIntegrationDesired(), isSsoIntegrationEnabled(existingConfigurations),
        isKerberosRequiredForSsoIntegration(), isKerberosEnabled(existingConfigurations));

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
      ServiceCredentialStoreUpdateEvent serviceCredentialStoreUpdateEvent = null;
      //create event only if the value changed
      if (desiredStateEntity.isCredentialStoreEnabled() != credentialStoreEnabled) {
        StackId stackId = serviceGroup.getStackId();
        serviceCredentialStoreUpdateEvent =
            new ServiceCredentialStoreUpdateEvent(getClusterId(), stackId.getStackName(),
                stackId.getStackVersion(), getName(), getServiceType(), getServiceGroupName());
      }
      desiredStateEntity.setCredentialStoreEnabled(credentialStoreEnabled);
      desiredStateEntity = serviceDesiredStateDAO.merge(desiredStateEntity);

      //publish event after the value has changed
      if (serviceCredentialStoreUpdateEvent != null) {
        eventPublisher.publish(serviceCredentialStoreUpdateEvent);
      }
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getName());
    }
  }

  @Override
  public ClusterServiceEntity removeDependencyService(Long dependencyServiceId) {
    ClusterServiceEntity currentServiceEntity = clusterServiceDAO.findByPK(getServiceId());

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

    ClusterServiceEntity currentServiceEntity = clusterServiceDAO.findByPK(getServiceId());
    ClusterServiceEntity dependentServiceEntity = clusterServiceDAO.findByPK(dependentService.getServiceId());

    ServiceDependencyEntity newServiceDependency = new ServiceDependencyEntity();
    newServiceDependency.setService(currentServiceEntity);
    newServiceDependency.setServiceGroupId(currentServiceEntity.getServiceGroupId());
    newServiceDependency.setServiceClusterId(currentServiceEntity.getClusterId());

    newServiceDependency.setServiceDependency(dependentServiceEntity);
    newServiceDependency.setDependentServiceGroupId(dependentServiceEntity.getServiceGroupId());
    newServiceDependency.setDependentServiceClusterId(dependentServiceEntity.getClusterId());

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
      .append(", serviceGroup=").append(getServiceGroupName())
      .append(", serviceType=").append(getServiceType())
      .append(", clusterName=").append(cluster.getClusterName())
      .append(", clusterId=").append(cluster.getClusterId())
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
    StackId stackId = serviceGroup.getStackId();
    cluster.addService(this);

    ServiceInstalledEvent event = new ServiceInstalledEvent(getClusterId(), stackId.getStackName(),
        stackId.getStackVersion(), getName(), serviceType, serviceGroup.getServiceGroupName());

    eventPublisher.publish(event);
  }

  @Transactional
  void persistEntities(ClusterServiceEntity serviceEntity) {
    long clusterId = cluster.getClusterId();
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(serviceGroup.getServiceGroupId());
    serviceEntity.setServiceGroupEntity(serviceGroupEntity);
    serviceEntity.setClusterEntity(clusterEntity);
    serviceEntity.setClusterId(clusterId);
    clusterServiceDAO.create(serviceEntity);
    serviceId = serviceEntity.getServiceId();
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
  public void deleteAllComponents(DeleteHostComponentStatusMetaData deleteMetaData) {
    lock.lock();
    try {
      LOG.info("Deleting all componentsByName for service" + ", clusterName=" + cluster.getClusterName()
          + ", serviceName=" + getName());
      // FIXME check dependencies from meta layer
      for (ServiceComponent component : componentsByName.values()) {
        if (!component.canBeRemoved()) {
          deleteMetaData.setAmbariException(new AmbariException("Found non removable component when trying to"
              + " delete all componentsByName from service" + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + getName() + ", componentName=" + component.getName()));
          return;
        }
      }

      for (ServiceComponent serviceComponent : componentsByName.values()) {
        serviceComponent.delete(deleteMetaData);
        if (deleteMetaData.getAmbariException() != null) {
          return;
        }
      }

      componentsByName.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void deleteServiceComponent(String componentName, DeleteHostComponentStatusMetaData deleteMetaData)
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

      component.delete(deleteMetaData);
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
  public void delete(DeleteHostComponentStatusMetaData deleteMetaData) {
    List<Component> componentsByName = getComponents(); // XXX temporal coupling, need to call this BEFORE deletingAllComponents
    deleteAllComponents(deleteMetaData);
    if (deleteMetaData.getAmbariException() != null) {
      return;
    }

    StackId stackId = serviceGroup.getStackId();
    try {
      deleteAllServiceConfigs();

      removeEntities();
    } catch (AmbariException e) {
      deleteMetaData.setAmbariException(e);
      return;
    }

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
    serviceDesiredStateDAO.removeByServiceId(serviceEntity.getServiceId());
    clusterServiceDAO.removeByPK(serviceEntity.getServiceId());
  }

  @Override
  public void setMaintenanceState(MaintenanceState state) {
    ServiceDesiredStateEntity serviceDesiredStateEntity = getServiceDesiredStateEntity();
    serviceDesiredStateEntity.setMaintenanceState(state);
    maintenanceState.set(serviceDesiredStateDAO.merge(serviceDesiredStateEntity).getMaintenanceState());

    // broadcast the maintenance mode change
    MaintenanceModeEvent event = new MaintenanceModeEvent(state, this);
    eventPublisher.publish(event);
  }

  @Override
  public MaintenanceState getMaintenanceState() {
    if (maintenanceState.get() == null) {
      maintenanceState.set(getServiceDesiredStateEntity().getMaintenanceState());
    }
    return maintenanceState.get();
  }

  @Override
  public boolean isKerberosEnabled() {
    if (kerberosEnabledTest != null) {
      Map<String, Map<String, String>> existingConfigurations;

      try {
        existingConfigurations = configHelper.calculateExistingConfigurations(ambariManagementController, cluster);
      } catch (AmbariException e) {
        LOG.warn("Failed to get the existing configurations for the cluster.  Predicate calculations may not be correct due to missing data.");
        existingConfigurations = Collections.emptyMap();
      }
      return isKerberosEnabled(existingConfigurations);
    }

    return false;
  }

  @Override
  public boolean isKerberosEnabled(Map<String, Map<String, String>> configurations) {
    return kerberosEnabledTest != null && kerberosEnabledTest.evaluate(configurations);
  }

  // Refresh the cached reference on setters
  private ServiceDesiredStateEntity getServiceDesiredStateEntity() {
    return serviceDesiredStateDAO.findByServiceId(serviceId);
  }

  private Predicate compileSsoEnabledPredicate(ServiceInfo sInfo) {
    if (StringUtils.isNotBlank(sInfo.getSingleSignOnEnabledTest())) {
      if (StringUtils.isNotBlank(sInfo.getSingleSignOnEnabledConfiguration())) {
        LOG.warn("Both <ssoEnabledTest> and <enabledConfiguration> have been declared within <sso> for {}; using <ssoEnabledTest>", serviceName);
      }
      return PredicateUtils.fromJSON(sInfo.getSingleSignOnEnabledTest());
    } else if (StringUtils.isNotBlank(sInfo.getSingleSignOnEnabledConfiguration())) {
      LOG.warn("Only <enabledConfiguration> have been declared  within <sso> for {}; converting its value to an equals predicate", serviceName);
      final String equalsPredicateJson = "{\"equals\": [\"" + sInfo.getSingleSignOnEnabledConfiguration() + "\", \"true\"]}";
      return PredicateUtils.fromJSON(equalsPredicateJson);
    }
    return null;
  }

  private Predicate compileKerberosEnabledPredicate(ServiceInfo sInfo) {
    if (StringUtils.isNotBlank(sInfo.getKerberosEnabledTest())) {
      return PredicateUtils.fromJSON(sInfo.getKerberosEnabledTest());
    }
    return null;
  }

  private boolean isSsoIntegrationDesired() {
    return ambariServerConfigurationHandler.getSSOEnabledServices().contains(serviceName);
  }

  private boolean isSsoIntegrationEnabled(Map<String, Map<String, String>> existingConfigurations) {
    return ssoIntegrationSupported && ssoEnabledTest != null && ssoEnabledTest.evaluate(existingConfigurations);
  }

  private boolean isKerberosRequiredForSsoIntegration() {
    return ssoRequiresKerberos;
  }
}
