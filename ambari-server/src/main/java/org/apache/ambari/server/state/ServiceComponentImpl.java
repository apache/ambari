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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.events.ServiceComponentRecoveryChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.ProvisionException;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ServiceComponentImpl implements ServiceComponent {

  private final static Logger LOG =
      LoggerFactory.getLogger(ServiceComponentImpl.class);
  private final Service service;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final String componentName;
  private String displayName;
  private boolean isClientComponent;
  private boolean isMasterComponent;
  private boolean isVersionAdvertised;

  private final ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;

  private final ClusterServiceDAO clusterServiceDAO;

  private final ServiceComponentHostFactory serviceComponentHostFactory;

  private final AmbariEventPublisher eventPublisher;

  private AmbariMetaInfo ambariMetaInfo;

  private final ConcurrentMap<String, ServiceComponentHost> hostComponents = new ConcurrentHashMap<String, ServiceComponentHost>();

  /**
   * The ID of the persisted {@link ServiceComponentDesiredStateEntity}.
   */
  private final long desiredStateEntityId;

  /**
   * Data access object used for lookup up stacks.
   */
  private final StackDAO stackDAO;

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service, @Assisted String componentName,
      AmbariMetaInfo ambariMetaInfo,
      ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO,
      ClusterServiceDAO clusterServiceDAO, ServiceComponentHostFactory serviceComponentHostFactory,
      StackDAO stackDAO, AmbariEventPublisher eventPublisher)
      throws AmbariException {

    this.ambariMetaInfo = ambariMetaInfo;
    this.service = service;
    this.componentName = componentName;
    this.serviceComponentDesiredStateDAO = serviceComponentDesiredStateDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceComponentHostFactory = serviceComponentHostFactory;
    this.stackDAO = stackDAO;
    this.eventPublisher = eventPublisher;

    StackId stackId = service.getDesiredStackVersion();
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    ServiceComponentDesiredStateEntity desiredStateEntity = new ServiceComponentDesiredStateEntity();
    desiredStateEntity.setComponentName(componentName);
    desiredStateEntity.setDesiredState(State.INIT);
    desiredStateEntity.setDesiredVersion(State.UNKNOWN.toString());
    desiredStateEntity.setServiceName(service.getName());
    desiredStateEntity.setClusterId(service.getClusterId());
    desiredStateEntity.setRecoveryEnabled(false);
    desiredStateEntity.setDesiredStack(stackEntity);

    updateComponentInfo();

    persistEntities(desiredStateEntity);
    desiredStateEntityId = desiredStateEntity.getId();
  }

  public void updateComponentInfo() throws AmbariException {
    StackId stackId = service.getDesiredStackVersion();
    try {
      ComponentInfo compInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
          stackId.getStackVersion(), service.getName(), componentName);
      isClientComponent = compInfo.isClient();
      isMasterComponent = compInfo.isMaster();
      isVersionAdvertised = compInfo.isVersionAdvertised();
      displayName = compInfo.getDisplayName();
    } catch (ObjectNotFoundException e) {
      throw new RuntimeException("Trying to create a ServiceComponent"
          + " not recognized in stack info"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", serviceName=" + service.getName()
          + ", componentName=" + componentName
          + ", stackInfo=" + stackId.getStackId());
    }
  }

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service,
      @Assisted ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity,
      AmbariMetaInfo ambariMetaInfo,
      ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO,
      ClusterServiceDAO clusterServiceDAO,
      HostComponentDesiredStateDAO hostComponentDesiredStateDAO,
      ServiceComponentHostFactory serviceComponentHostFactory, StackDAO stackDAO,
      AmbariEventPublisher eventPublisher)
      throws AmbariException {
    this.service = service;
    this.serviceComponentDesiredStateDAO = serviceComponentDesiredStateDAO;
    this.clusterServiceDAO = clusterServiceDAO;
    this.serviceComponentHostFactory = serviceComponentHostFactory;
    this.stackDAO = stackDAO;
    this.eventPublisher = eventPublisher;
    this.ambariMetaInfo = ambariMetaInfo;

    desiredStateEntityId = serviceComponentDesiredStateEntity.getId();
    componentName = serviceComponentDesiredStateEntity.getComponentName();

    updateComponentInfo();

    for (HostComponentStateEntity hostComponentStateEntity : serviceComponentDesiredStateEntity.getHostComponentStateEntities()) {

      HostComponentDesiredStateEntity hostComponentDesiredStateEntity = hostComponentDesiredStateDAO.findByIndex(
        hostComponentStateEntity.getClusterId(),
        hostComponentStateEntity.getServiceName(),
        hostComponentStateEntity.getComponentName(),
        hostComponentStateEntity.getHostId()
      );

      try {
        hostComponents.put(hostComponentStateEntity.getHostName(),
          serviceComponentHostFactory.createExisting(this,
            hostComponentStateEntity, hostComponentDesiredStateEntity));
      } catch(ProvisionException ex) {
        StackId currentStackId = service.getCluster().getCurrentStackVersion();
        LOG.error(String.format("Can not get host component info: stackName=%s, stackVersion=%s, serviceName=%s, componentName=%s, hostname=%s",
          currentStackId.getStackName(), currentStackId.getStackVersion(),
          service.getName(),serviceComponentDesiredStateEntity.getComponentName(), hostComponentStateEntity.getHostName()));
        ex.printStackTrace();
      }
    }
  }

  @Override
  public String getName() {
    return componentName;
  }

  /**
   * Get the recoveryEnabled value.
   *
   * @return true or false
   */
  @Override
  public boolean isRecoveryEnabled() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (desiredStateEntity != null) {
      return desiredStateEntity.isRecoveryEnabled();
    } else {
      LOG.warn("Trying to fetch a member from an entity object that may " +
              "have been previously deleted, serviceName = " + service.getName() + ", " +
              "componentName = " + componentName);
    }
    return false;
  }

  /**
   * Set the recoveryEnabled field in the entity object.
   *
   * @param recoveryEnabled - true or false
   */
  @Override
  public void setRecoveryEnabled(boolean recoveryEnabled) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting RecoveryEnabled of Component" + ", clusterName="
          + service.getCluster().getClusterName() + ", clusterId="
          + service.getCluster().getClusterId() + ", serviceName=" + service.getName()
          + ", componentName=" + getName() + ", oldRecoveryEnabled=" + isRecoveryEnabled()
          + ", newRecoveryEnabled=" + recoveryEnabled);
    }

    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (desiredStateEntity != null) {
      desiredStateEntity.setRecoveryEnabled(recoveryEnabled);
      desiredStateEntity = serviceComponentDesiredStateDAO.merge(desiredStateEntity);

      // broadcast the change
      ServiceComponentRecoveryChangedEvent event = new ServiceComponentRecoveryChangedEvent(
          getClusterName(), getServiceName(), getName(), isRecoveryEnabled());
      eventPublisher.publish(event);

    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + service.getName());
    }
  }

  @Override
  public String getServiceName() {
    return service.getName();
  }

  @Override
  public long getClusterId() {
    return service.getClusterId();
  }

  @Override
  public Map<String, ServiceComponentHost> getServiceComponentHosts() {
    return new HashMap<String, ServiceComponentHost>(hostComponents);
  }

  @Override
  public void addServiceComponentHosts(
      Map<String, ServiceComponentHost> hostComponents) throws AmbariException {
    // TODO validation
    for (Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      if (!entry.getKey().equals(entry.getValue().getHostName())) {
        throw new AmbariException(
            "Invalid arguments in map" + ", hostname does not match the key in map");
      }
    }

    for (ServiceComponentHost sch : hostComponents.values()) {
      addServiceComponentHost(sch);
    }
  }

  @Override
  public void addServiceComponentHost(
      ServiceComponentHost hostComponent) throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      // TODO validation
      // TODO ensure host belongs to cluster
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding a ServiceComponentHost to ServiceComponent" + ", clusterName="
            + service.getCluster().getClusterName() + ", clusterId="
            + service.getCluster().getClusterId() + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName() + ", hostname=" + hostComponent.getHostName()
            + ", recoveryEnabled=" + isRecoveryEnabled());
      }

      if (hostComponents.containsKey(hostComponent.getHostName())) {
        throw new AmbariException("Cannot add duplicate ServiceComponentHost" + ", clusterName="
            + service.getCluster().getClusterName() + ", clusterId="
            + service.getCluster().getClusterId() + ", serviceName=" + service.getName()
            + ", serviceComponentName=" + getName() + ", hostname=" + hostComponent.getHostName()
            + ", recoveryEnabled=" + isRecoveryEnabled());
      }
      // FIXME need a better approach of caching components by host
      ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
      clusterImpl.addServiceComponentHost(hostComponent);
      hostComponents.put(hostComponent.getHostName(), hostComponent);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public ServiceComponentHost addServiceComponentHost(String hostName) throws AmbariException {
    ServiceComponentHost hostComponent = serviceComponentHostFactory.createNew(this, hostName);
    addServiceComponentHost(hostComponent);
    return hostComponent;
  }

  @Override
  public ServiceComponentHost getServiceComponentHost(String hostname)
      throws AmbariException {

    if (!hostComponents.containsKey(hostname)) {
      throw new ServiceComponentHostNotFoundException(getClusterName(),
          getServiceName(), getName(), hostname);
    }

    return hostComponents.get(hostname);
  }

  @Override
  public State getDesiredState() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

      if (desiredStateEntity != null) {
        return desiredStateEntity.getDesiredState();
      } else {
        LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + componentName);
      }

    return null;
  }

  @Override
  public void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service" + ", clusterName="
          + service.getCluster().getClusterName() + ", clusterId="
          + service.getCluster().getClusterId() + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName() + ", oldDesiredState=" + getDesiredState()
          + ", newDesiredState=" + state);
    }

    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (desiredStateEntity != null) {
      desiredStateEntity.setDesiredState(state);
      desiredStateEntity = serviceComponentDesiredStateDAO.merge(desiredStateEntity);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + (service != null ? service.getName() : ""));
    }
  }

  @Override
  public StackId getDesiredStackVersion() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    StackEntity stackEntity = desiredStateEntity.getDesiredStack();
    if (null != stackEntity) {
      return new StackId(stackEntity.getStackName(), stackEntity.getStackVersion());
    } else {
      return null;
    }
  }

  @Override
  public void setDesiredStackVersion(StackId stack) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredStackVersion of Service" + ", clusterName="
          + service.getCluster().getClusterName() + ", clusterId="
          + service.getCluster().getClusterId() + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName() + ", oldDesiredStackVersion="
          + getDesiredStackVersion() + ", newDesiredStackVersion=" + stack);
    }

    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    if (desiredStateEntity != null) {
      StackEntity stackEntity = stackDAO.find(stack.getStackName(), stack.getStackVersion());
      desiredStateEntity.setDesiredStack(stackEntity);
      desiredStateEntity = serviceComponentDesiredStateDAO.merge(desiredStateEntity);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + (service != null ? service.getName() : ""));
    }
  }

  @Override
  public String getDesiredVersion() {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

    return desiredStateEntity.getDesiredVersion();
  }

  @Override
  public void setDesiredVersion(String version) {
    ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
        desiredStateEntityId);

      if (desiredStateEntity != null) {
        desiredStateEntity.setDesiredVersion(version);
      desiredStateEntity = serviceComponentDesiredStateDAO.merge(desiredStateEntity);
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + (service != null ? service.getName() : ""));
      }
  }

  @Override
  public ServiceComponentResponse convertToResponse() {
    Cluster cluster = service.getCluster();
    ServiceComponentResponse r = new ServiceComponentResponse(getClusterId(),
        cluster.getClusterName(), service.getName(), getName(),
        getDesiredStackVersion().getStackId(), getDesiredState().toString(),
        getServiceComponentStateCount(), isRecoveryEnabled(), displayName);
    return r;
  }

  @Override
  public String getClusterName() {
    return service.getCluster().getClusterName();
  }


  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("ServiceComponent={ serviceComponentName=" + getName() + ", recoveryEnabled="
        + isRecoveryEnabled() + ", clusterName=" + service.getCluster().getClusterName()
        + ", clusterId=" + service.getCluster().getClusterId() + ", serviceName="
        + service.getName() + ", desiredStackVersion=" + getDesiredStackVersion()
        + ", desiredState=" + getDesiredState().toString() + ", hostcomponents=[ ");
    boolean first = true;
    for (ServiceComponentHost sch : hostComponents.values()) {
      if (!first) {
        sb.append(" , ");
        first = false;
      }
      sb.append("\n        ");
      sch.debugDump(sb);
      sb.append(" ");
    }
    sb.append(" ] }");
  }

  @Transactional
  protected void persistEntities(ServiceComponentDesiredStateEntity desiredStateEntity) {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(service.getClusterId());
    pk.setServiceName(service.getName());
    ClusterServiceEntity serviceEntity = clusterServiceDAO.findByPK(pk);

    desiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceComponentDesiredStateDAO.create(desiredStateEntity);
    serviceEntity = clusterServiceDAO.merge(serviceEntity);
  }

  @Override
  public boolean isClientComponent() {
    return isClientComponent;
  }

  @Override
  public boolean isMasterComponent() {
    return isMasterComponent;
  }


  @Override
  public boolean isVersionAdvertised() {
    return isVersionAdvertised;
  }

  @Override
  public boolean canBeRemoved() {
    // A component can be deleted if all it's host components
    // can be removed, irrespective of the state of
    // the component itself
    for (ServiceComponentHost sch : hostComponents.values()) {
      if (!sch.canBeRemoved()) {
        LOG.warn("Found non removable hostcomponent when trying to" + " delete service component"
            + ", clusterName=" + getClusterName() + ", serviceName=" + getServiceName()
            + ", componentName=" + getName() + ", state=" + sch.getState() + ", hostname="
            + sch.getHostName());
        return false;
      }
    }
    return true;
  }

  @Override
  @Transactional
  public void deleteAllServiceComponentHosts() throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      LOG.info("Deleting all servicecomponenthosts for component" + ", clusterName="
          + getClusterName() + ", serviceName=" + getServiceName() + ", componentName=" + getName()
          + ", recoveryEnabled=" + isRecoveryEnabled());
      for (ServiceComponentHost sch : hostComponents.values()) {
        if (!sch.canBeRemoved()) {
          throw new AmbariException("Found non removable hostcomponent " + " when trying to delete"
              + " all hostcomponents from servicecomponent" + ", clusterName=" + getClusterName()
              + ", serviceName=" + getServiceName() + ", componentName=" + getName()
              + ", recoveryEnabled=" + isRecoveryEnabled() + ", hostname=" + sch.getHostName());
        }
      }

      for (ServiceComponentHost serviceComponentHost : hostComponents.values()) {
        serviceComponentHost.delete();
      }

      hostComponents.clear();
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void deleteServiceComponentHosts(String hostname) throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      ServiceComponentHost sch = getServiceComponentHost(hostname);
      LOG.info("Deleting servicecomponenthost for cluster" + ", clusterName=" + getClusterName()
          + ", serviceName=" + getServiceName() + ", componentName=" + getName()
          + ", recoveryEnabled=" + isRecoveryEnabled() + ", hostname=" + sch.getHostName());
      if (!sch.canBeRemoved()) {
        throw new AmbariException("Could not delete hostcomponent from cluster"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + getServiceName()
            + ", componentName=" + getName()
            + ", recoveryEnabled=" + isRecoveryEnabled()
            + ", hostname=" + sch.getHostName());
      }
      sch.delete();
      hostComponents.remove(hostname);

    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  @Transactional
  public void delete() throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      deleteAllServiceComponentHosts();

      ServiceComponentDesiredStateEntity desiredStateEntity = serviceComponentDesiredStateDAO.findById(
          desiredStateEntityId);

      serviceComponentDesiredStateDAO.remove(desiredStateEntity);

    } finally {
      readWriteLock.writeLock().unlock();
    }
  }


  private int getSCHCountByState(State state) {
    int count = 0;
    for (ServiceComponentHost sch : hostComponents.values()) {
      if (sch.getState() == state) {
        count++;
      }
    }
    return count;
  }

  private Map <String, Integer> getServiceComponentStateCount() {
    Map <String, Integer> serviceComponentStateCountMap = new HashMap <String, Integer>();
    serviceComponentStateCountMap.put("startedCount", getSCHCountByState(State.STARTED));
    serviceComponentStateCountMap.put("installedCount", getSCHCountByState(State.INSTALLED));
    serviceComponentStateCountMap.put("installFailedCount", getSCHCountByState(State.INSTALL_FAILED));
    serviceComponentStateCountMap.put("initCount", getSCHCountByState(State.INIT));
    serviceComponentStateCountMap.put("unknownCount", getSCHCountByState(State.UNKNOWN));
    serviceComponentStateCountMap.put("totalCount", hostComponents.size());
    return serviceComponentStateCountMap;
  }
}
