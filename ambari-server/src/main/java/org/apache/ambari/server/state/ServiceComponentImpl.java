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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ServiceComponentImpl implements ServiceComponent {

  private final static Logger LOG =
      LoggerFactory.getLogger(ServiceComponentImpl.class);
  private final Service service;
  private final ReadWriteLock clusterGlobalLock;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final String componentName;
  private final String displayName;
  private final boolean isClientComponent;
  private final boolean isMasterComponent;
  private final boolean isVersionAdvertised;
  volatile boolean persisted = false;
  @Inject
  private ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;
  @Inject
  private ClusterServiceDAO clusterServiceDAO;
  @Inject
  private HostComponentDesiredStateDAO hostComponentDesiredStateDAO;
  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  private ServiceComponentDesiredStateEntity desiredStateEntity;
  private ServiceComponentDesiredStateEntityPK desiredStateEntityPK;
  private Map<String, ServiceComponentHost> hostComponents;

  /**
   * Data access object used for lookup up stacks.
   */
  @Inject
  private StackDAO stackDAO;

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service,
                              @Assisted String componentName, Injector injector) throws AmbariException {
    injector.injectMembers(this);
    clusterGlobalLock = service.getClusterGlobalLock();
    this.service = service;
    desiredStateEntity = new ServiceComponentDesiredStateEntity(  );
    desiredStateEntity.setComponentName(componentName);
    desiredStateEntity.setDesiredState(State.INIT);
    desiredStateEntity.setServiceName(service.getName());
    desiredStateEntity.setClusterId(service.getClusterId());
    desiredStateEntityPK = getDesiredStateEntityPK(desiredStateEntity);
    setDesiredStackVersion(service.getDesiredStackVersion());

    hostComponents = new HashMap<String, ServiceComponentHost>();

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
    this.componentName = componentName;
  }

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service,
                              @Assisted ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity,
                              Injector injector) throws AmbariException {
    injector.injectMembers(this);
    clusterGlobalLock = service.getClusterGlobalLock();
    this.service = service;
    desiredStateEntity = serviceComponentDesiredStateEntity;
    this.componentName = serviceComponentDesiredStateEntity.getComponentName();

    StackId stackId = service.getDesiredStackVersion();
    try {
      ComponentInfo compInfo = ambariMetaInfo.getComponent(
        stackId.getStackName(), stackId.getStackVersion(), service.getName(),
        componentName);
      isClientComponent = compInfo.isClient();
      isMasterComponent = compInfo.isMaster();
      isVersionAdvertised = compInfo.isVersionAdvertised();
      displayName = compInfo.getDisplayName();
    } catch (ObjectNotFoundException e) {
      throw new AmbariException("Trying to create a ServiceComponent"
        + " not recognized in stack info"
        + ", clusterName=" + service.getCluster().getClusterName()
        + ", serviceName=" + service.getName()
        + ", componentName=" + componentName
        + ", stackInfo=" + stackId.getStackId());
    }

    hostComponents = new HashMap<String, ServiceComponentHost>();
    for (HostComponentStateEntity hostComponentStateEntity : desiredStateEntity.getHostComponentStateEntities()) {
      HostComponentDesiredStateEntityPK pk = new HostComponentDesiredStateEntityPK();
      pk.setClusterId(hostComponentStateEntity.getClusterId());
      pk.setServiceName(hostComponentStateEntity.getServiceName());
      pk.setComponentName(hostComponentStateEntity.getComponentName());
      pk.setHostId(hostComponentStateEntity.getHostId());

      HostComponentDesiredStateEntity hostComponentDesiredStateEntity = hostComponentDesiredStateDAO.findByPK(pk);
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

    desiredStateEntityPK = getDesiredStateEntityPK(desiredStateEntity);

    persisted = true;
  }

  @Override
  public ReadWriteLock getClusterGlobalLock() {
    return clusterGlobalLock;
  }

  @Override
  public String getName() {
    ServiceComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
    if (desiredStateEntity != null) {
      return desiredStateEntity.getComponentName();
    } else {
      LOG.warn("Trying to fetch a member from an entity object that may " +
        "have been previously deleted, serviceName = " + getServiceName() + ", " +
        "componentName = " + componentName);
    }
    return null;
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
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        return new HashMap<String, ServiceComponentHost>(hostComponents);
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public void addServiceComponentHosts(
      Map<String, ServiceComponentHost> hostComponents) throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        // TODO validation
        for (Entry<String, ServiceComponentHost> entry :
            hostComponents.entrySet()) {
          if (!entry.getKey().equals(entry.getValue().getHostName())) {
            throw new AmbariException("Invalid arguments in map"
                + ", hostname does not match the key in map");
          }
        }
        for (ServiceComponentHost sch : hostComponents.values()) {
          addServiceComponentHost(sch);
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public void addServiceComponentHost(
      ServiceComponentHost hostComponent) throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        // TODO validation
        // TODO ensure host belongs to cluster
        if (LOG.isDebugEnabled()) {
          LOG.debug("Adding a ServiceComponentHost to ServiceComponent"
              + ", clusterName=" + service.getCluster().getClusterName()
              + ", clusterId=" + service.getCluster().getClusterId()
              + ", serviceName=" + service.getName()
              + ", serviceComponentName=" + getName()
              + ", hostname=" + hostComponent.getHostName());
        }
        if (hostComponents.containsKey(hostComponent.getHostName())) {
          throw new AmbariException("Cannot add duplicate ServiceComponentHost"
              + ", clusterName=" + service.getCluster().getClusterName()
              + ", clusterId=" + service.getCluster().getClusterId()
              + ", serviceName=" + service.getName()
              + ", serviceComponentName=" + getName()
              + ", hostname=" + hostComponent.getHostName());
        }
        // FIXME need a better approach of caching components by host
        ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
        clusterImpl.addServiceComponentHost(hostComponent);
        hostComponents.put(hostComponent.getHostName(), hostComponent);
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public ServiceComponentHost addServiceComponentHost(String hostName) throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        // TODO validation
        // TODO ensure host belongs to cluster
        if (LOG.isDebugEnabled()) {
          LOG.debug("Adding a ServiceComponentHost to ServiceComponent"
              + ", clusterName=" + service.getCluster().getClusterName()
              + ", clusterId=" + service.getCluster().getClusterId()
              + ", serviceName=" + service.getName()
              + ", serviceComponentName=" + getName()
              + ", hostname=" + hostName);
        }
        if (hostComponents.containsKey(hostName)) {
          throw new AmbariException("Cannot add duplicate ServiceComponentHost"
              + ", clusterName=" + service.getCluster().getClusterName()
              + ", clusterId=" + service.getCluster().getClusterId()
              + ", serviceName=" + service.getName()
              + ", serviceComponentName=" + getName()
              + ", hostname=" + hostName);
        }
        ServiceComponentHost hostComponent = serviceComponentHostFactory.createNew(this, hostName);
        // FIXME need a better approach of caching components by host
        ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
        clusterImpl.addServiceComponentHost(hostComponent);

        hostComponents.put(hostComponent.getHostName(), hostComponent);

        return hostComponent;
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public ServiceComponentHost getServiceComponentHost(String hostname)
      throws AmbariException {
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        if (!hostComponents.containsKey(hostname)) {
          throw new ServiceComponentHostNotFoundException(getClusterName(),
              getServiceName(), getName(), hostname);
        }
        return hostComponents.get(hostname);
      } finally {
        readWriteLock.readLock().unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public State getDesiredState() {
    readWriteLock.readLock().lock();
    try {
      ServiceComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        return desiredStateEntity.getDesiredState();
      } else {
        LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + componentName);
      }

    } finally {
      readWriteLock.readLock().unlock();
    }
    return null;
  }

  @Override
  public void setDesiredState(State state) {
    readWriteLock.writeLock().lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting DesiredState of Service" + ", clusterName="
            + service.getCluster().getClusterName() + ", clusterId="
            + service.getCluster().getClusterId() + ", serviceName="
            + service.getName() + ", serviceComponentName=" + getName()
            + ", oldDesiredState=" + getDesiredState() + ", newDesiredState="
            + state);
      }
      ServiceComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        desiredStateEntity.setDesiredState(state);
        saveIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + (service != null ? service.getName() : ""));
      }

    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public StackId getDesiredStackVersion() {
    readWriteLock.readLock().lock();
    try {
      StackEntity stackEntity = getDesiredStateEntity().getDesiredStack();
      if (null != stackEntity) {
        return new StackId(stackEntity.getStackName(),
            stackEntity.getStackVersion());
      } else {
        return null;
      }
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public void setDesiredStackVersion(StackId stack) {
    readWriteLock.writeLock().lock();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting DesiredStackVersion of Service" + ", clusterName="
            + service.getCluster().getClusterName() + ", clusterId="
            + service.getCluster().getClusterId() + ", serviceName="
            + service.getName() + ", serviceComponentName=" + getName()
            + ", oldDesiredStackVersion=" + getDesiredStackVersion()
            + ", newDesiredStackVersion=" + stack);
      }

      StackEntity stackEntity = stackDAO.find(stack.getStackName(),
        stack.getStackVersion());

      ServiceComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        desiredStateEntity.setDesiredStack(stackEntity);
        saveIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + (service != null ? service.getName() : ""));
      }

    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public ServiceComponentResponse convertToResponse() {
    readWriteLock.readLock().lock();
    try {
      Cluster cluster = service.getCluster();
      ServiceComponentResponse r = new ServiceComponentResponse(getClusterId(),
          cluster.getClusterName(), service.getName(), getName(),
          getDesiredStackVersion().getStackId(), getDesiredState().toString(),
          getTotalCount(), getStartedCount(), getInstalledCount(), displayName);
      return r;
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public String getClusterName() {
    return service.getCluster().getClusterName();
  }

  @Override
  public void debugDump(StringBuilder sb) {
    readWriteLock.readLock().lock();
    try {
      sb.append("ServiceComponent={ serviceComponentName=" + getName()
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName() + ", desiredStackVersion="
          + getDesiredStackVersion() + ", desiredState="
          + getDesiredState().toString() + ", hostcomponents=[ ");
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
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPersisted() {
    // a lock around this internal state variable is not required since we
    // have appropriate locks in the persist() method and this member is
    // only ever false under the condition that the object is new
    return persisted;
  }

  /**
   * {@inheritDoc}
   * <p/>
   * This method uses Java locks and then delegates to internal methods which
   * perform the JPA merges inside of a transaction. Because of this, a
   * transaction is not necessary before this calling this method.
   */
  @Override
  public void persist() {
    boolean clusterWriteLockAcquired = false;
    if (!persisted) {
      clusterGlobalLock.writeLock().lock();
      clusterWriteLockAcquired = true;
    }

    try {
      readWriteLock.writeLock().lock();
      try {
        if (!persisted) {
          // persist the new cluster topology and then release the cluster lock
          // as it has no more bearing on the rest of this persist() method
          persistEntities();
          clusterGlobalLock.writeLock().unlock();
          clusterWriteLockAcquired = false;

          refresh();
          // There refresh calls are no longer needed with cached references
          // not used on getters/setters
          // service.refresh();
          persisted = true;
        } else {
          saveIfPersisted();
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      if (clusterWriteLockAcquired) {
        clusterGlobalLock.writeLock().unlock();
      }
    }
  }

  @Transactional
  protected void persistEntities() {
    ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
    pk.setClusterId(service.getClusterId());
    pk.setServiceName(service.getName());
    ClusterServiceEntity serviceEntity = clusterServiceDAO.findByPK(pk);

    desiredStateEntity.setClusterServiceEntity(serviceEntity);
    serviceComponentDesiredStateDAO.create(desiredStateEntity);
    clusterServiceDAO.merge(serviceEntity);
  }

  @Override
  @Transactional
  public void refresh() {
    readWriteLock.writeLock().lock();
    try {
      if (isPersisted()) {
        ServiceComponentDesiredStateEntityPK pk = new ServiceComponentDesiredStateEntityPK();
        pk.setComponentName(getName());
        pk.setClusterId(getClusterId());
        pk.setServiceName(getServiceName());
        serviceComponentDesiredStateDAO.refresh(getDesiredStateEntity());
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  /**
   * Merges the encapsulated {@link ServiceComponentDesiredStateEntity} inside
   * of a new transaction. This method assumes that the appropriate write lock
   * has already been acquired from {@link #readWriteLock}.
   */
  @Transactional
  void saveIfPersisted() {
    if (isPersisted()) {
      serviceComponentDesiredStateDAO.merge(desiredStateEntity);
    }
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
    clusterGlobalLock.readLock().lock();
    try {
      readWriteLock.readLock().lock();
      try {
        if (!getDesiredState().isRemovableState()) {
          return false;
        }

        for (ServiceComponentHost sch : hostComponents.values()) {
          if (!sch.canBeRemoved()) {
            LOG.warn("Found non removable hostcomponent when trying to"
                + " delete service component"
                + ", clusterName=" + getClusterName()
                + ", serviceName=" + getServiceName()
                + ", componentName=" + getName()
                + ", hostname=" + sch.getHostName());
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
  public void deleteAllServiceComponentHosts() throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        LOG.info("Deleting all servicecomponenthosts for component"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + getServiceName()
            + ", componentName=" + getName());
        for (ServiceComponentHost sch : hostComponents.values()) {
          if (!sch.canBeRemoved()) {
            throw new AmbariException("Found non removable hostcomponent "
                + " when trying to delete"
                + " all hostcomponents from servicecomponent"
                + ", clusterName=" + getClusterName()
                + ", serviceName=" + getServiceName()
                + ", componentName=" + getName()
                + ", hostname=" + sch.getHostName());
          }
        }

        for (ServiceComponentHost serviceComponentHost : hostComponents.values()) {
          serviceComponentHost.delete();
        }

        hostComponents.clear();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  public void deleteServiceComponentHosts(String hostname) throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        ServiceComponentHost sch = getServiceComponentHost(hostname);
        LOG.info("Deleting servicecomponenthost for cluster"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + getServiceName()
            + ", componentName=" + getName()
            + ", hostname=" + sch.getHostName());
        if (!sch.canBeRemoved()) {
          throw new AmbariException("Could not delete hostcomponent from cluster"
              + ", clusterName=" + getClusterName()
              + ", serviceName=" + getServiceName()
              + ", componentName=" + getName()
              + ", hostname=" + sch.getHostName());
        }
        sch.delete();
        hostComponents.remove(hostname);

      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }
  }

  @Override
  @Transactional
  public void delete() throws AmbariException {
    clusterGlobalLock.writeLock().lock();
    try {
      readWriteLock.writeLock().lock();
      try {
        deleteAllServiceComponentHosts();

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
    serviceComponentDesiredStateDAO.remove(getDesiredStateEntity());
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

  private int getStartedCount() {
    return getSCHCountByState(State.STARTED);
  }

  private int getInstalledCount() {
    return getSCHCountByState(State.INSTALLED);
  }

  private int getTotalCount() {
    return hostComponents.size();
  }

  // Refresh cached reference after ever setter
  private ServiceComponentDesiredStateEntity getDesiredStateEntity() {
    if (isPersisted()) {
      desiredStateEntity = serviceComponentDesiredStateDAO.findByPK(desiredStateEntityPK);
    }
    return desiredStateEntity;
  }

  private ServiceComponentDesiredStateEntityPK getDesiredStateEntityPK(ServiceComponentDesiredStateEntity desiredStateEntity) {
    ServiceComponentDesiredStateEntityPK pk = new ServiceComponentDesiredStateEntityPK();
    pk.setClusterId(desiredStateEntity.getClusterId());
    pk.setComponentName(desiredStateEntity.getComponentName());
    pk.setServiceName(desiredStateEntity.getServiceName());

    return pk;
  }
}
