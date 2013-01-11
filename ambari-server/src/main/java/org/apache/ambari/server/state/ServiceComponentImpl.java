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

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.orm.dao.*;
import org.apache.ambari.server.orm.entities.*;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceComponentImpl implements ServiceComponent {

  private final static Logger LOG =
      LoggerFactory.getLogger(ServiceComponentImpl.class);

  private final Service service;

  @Inject
  private Gson gson;
  @Inject
  private ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;
  @Inject
  private ClusterServiceDAO clusterServiceDAO;
  @Inject
  private HostComponentStateDAO hostComponentStateDAO;
  @Inject
  private HostComponentDesiredStateDAO hostComponentDesiredStateDAO;
  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  @Inject
  private ComponentConfigMappingDAO componentConfigMappingDAO;

  boolean persisted = false;
  private ServiceComponentDesiredStateEntity desiredStateEntity;

  // [ type -> versionTag ]
  private Map<String, String>  desiredConfigs;

  private Map<String, ServiceComponentHost> hostComponents;

  private final boolean isClientComponent;



  private void init() {
    // TODO load during restart
    // initialize from DB
  }

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service,
      @Assisted String componentName, Injector injector) {
    injector.injectMembers(this);
    this.service = service;
    this.desiredStateEntity = new ServiceComponentDesiredStateEntity();
    desiredStateEntity.setComponentName(componentName);
    desiredStateEntity.setDesiredState(State.INIT);

    this.desiredConfigs = new HashMap<String, String>();
    setDesiredStackVersion(service.getDesiredStackVersion());

    this.hostComponents = new HashMap<String, ServiceComponentHost>();

    StackId stackId = service.getDesiredStackVersion();
    ComponentInfo compInfo = ambariMetaInfo.getComponentCategory(
        stackId.getStackName(), stackId.getStackVersion(), service.getName(),
        componentName);
    if (compInfo == null) {
      throw new RuntimeException("Trying to create a ServiceComponent"
          + " not recognized in stack info"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", serviceName=" + service.getName()
          + ", componentName=" + componentName
          + ", stackInfo=" + stackId.getStackId());
    }
    this.isClientComponent = compInfo.isClient();

    init();
  }

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service,
                              @Assisted ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity,
                              Injector injector) {
    injector.injectMembers(this);
    this.service = service;
    this.desiredStateEntity = serviceComponentDesiredStateEntity;


    this.desiredConfigs = new HashMap<String, String>();

    this.hostComponents = new HashMap<String, ServiceComponentHost>();
    for (HostComponentStateEntity hostComponentStateEntity : desiredStateEntity.getHostComponentStateEntities()) {
      HostComponentDesiredStateEntityPK pk = new HostComponentDesiredStateEntityPK();
      pk.setClusterId(hostComponentStateEntity.getClusterId());
      pk.setServiceName(hostComponentStateEntity.getServiceName());
      pk.setComponentName(hostComponentStateEntity.getComponentName());
      pk.setHostName(hostComponentStateEntity.getHostName());

      HostComponentDesiredStateEntity hostComponentDesiredStateEntity = hostComponentDesiredStateDAO.findByPK(pk);

      hostComponents.put(hostComponentStateEntity.getHostName(),
          serviceComponentHostFactory.createExisting(this,
              hostComponentStateEntity, hostComponentDesiredStateEntity));
   }

    for (ComponentConfigMappingEntity entity : desiredStateEntity.getComponentConfigMappingEntities()) {
      desiredConfigs.put(entity.getConfigType(), entity.getVersionTag());
    }

    StackId stackId = service.getDesiredStackVersion();
    ComponentInfo compInfo = ambariMetaInfo.getComponentCategory(
        stackId.getStackName(), stackId.getStackVersion(), service.getName(),
        getName());
    if (compInfo == null) {
      throw new RuntimeException("Trying to create a ServiceComponent"
          + " not recognized in stack info"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", serviceName=" + service.getName()
          + ", componentName=" + getName()
          + ", stackInfo=" + stackId.getStackId());
    }
    this.isClientComponent = compInfo.isClient();

    persisted = true;
  }

  @Override
  public synchronized String getName() {
    return desiredStateEntity.getComponentName();
  }

  @Override
  public synchronized String getServiceName() {
    return service.getName();
  }

  @Override
  public synchronized long getClusterId() {
    return this.service.getClusterId();
  }

  @Override
  public synchronized Map<String, ServiceComponentHost>
      getServiceComponentHosts() {
    return Collections.unmodifiableMap(hostComponents);
  }

  @Override
  public synchronized void addServiceComponentHosts(
      Map<String, ServiceComponentHost> hostComponents) throws AmbariException {
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
  }

  @Override
  public synchronized void addServiceComponentHost(
      ServiceComponentHost hostComponent) throws AmbariException {
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
    this.hostComponents.put(hostComponent.getHostName(), hostComponent);
  }

  @Override
  public synchronized ServiceComponentHost addServiceComponentHost(
      String hostName) throws AmbariException {
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
    ServiceComponentHost hostComponent =
        serviceComponentHostFactory.createNew(this, hostName, true);
    // FIXME need a better approach of caching components by host
    ClusterImpl clusterImpl = (ClusterImpl) service.getCluster();
    clusterImpl.addServiceComponentHost(hostComponent);

    this.hostComponents.put(hostComponent.getHostName(), hostComponent);

    return hostComponent;
  }

  @Override
  public ServiceComponentHost getServiceComponentHost(String hostname)
    throws AmbariException {
    if (!hostComponents.containsKey(hostname)) {
      throw new ServiceComponentHostNotFoundException(getClusterName(),
          getServiceName(), getName(), hostname);
    }
    return this.hostComponents.get(hostname);
  }

  @Override
  public synchronized State getDesiredState() {
    return desiredStateEntity.getDesiredState();
  }

  @Override
  public synchronized void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName()
          + ", oldDesiredState=" + getDesiredState()
          + ", newDesiredState=" + state);
    }
    desiredStateEntity.setDesiredState(state);
    saveIfPersisted();
  }

  @Override
  public synchronized Map<String, Config> getDesiredConfigs() {
    Map<String, Config> map = new HashMap<String, Config>();
    for (Entry<String, String> entry : desiredConfigs.entrySet()) {
      Config config = service.getCluster().getDesiredConfig(entry.getKey(), entry.getValue());
      if (null != config) {
        map.put(entry.getKey(), config);
      }
    }

    Map<String, Config> svcConfigs = service.getDesiredConfigs();
    for (Entry<String, Config> entry : svcConfigs.entrySet()) {
      if (!map.containsKey(entry.getKey())) {
        map.put(entry.getKey(), entry.getValue());
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

      for (ComponentConfigMappingEntity componentConfigMappingEntity : desiredStateEntity.getComponentConfigMappingEntities()) {
        if (entry.getKey().equals(componentConfigMappingEntity.getConfigType())) {
          contains = true;
          componentConfigMappingEntity.setTimestamp(new Date().getTime());
          componentConfigMappingEntity.setVersionTag(entry.getValue().getVersionTag());
          if (persisted) {
            componentConfigMappingDAO.merge(componentConfigMappingEntity);
          }
        }
      }

      if (!contains) {
        ComponentConfigMappingEntity newEntity = new ComponentConfigMappingEntity();
        newEntity.setClusterId(desiredStateEntity.getClusterId());
        newEntity.setServiceName(desiredStateEntity.getServiceName());
        newEntity.setComponentName(desiredStateEntity.getComponentName());
        newEntity.setConfigType(entry.getKey());
        newEntity.setVersionTag(entry.getValue().getVersionTag());
        newEntity.setTimestamp(new Date().getTime());
        newEntity.setServiceComponentDesiredStateEntity(desiredStateEntity);
        desiredStateEntity.getComponentConfigMappingEntities().add(newEntity);

      }


      this.desiredConfigs.put(entry.getKey(), entry.getValue().getVersionTag());
    }

    if (!deletedTypes.isEmpty()) {
      if (persisted) {
        List<ComponentConfigMappingEntity> deleteEntities =
            componentConfigMappingDAO.findByComponentAndType(
                desiredStateEntity.getClusterId(), desiredStateEntity.getServiceName(),
                desiredStateEntity.getComponentName(),
                deletedTypes);
        for (ComponentConfigMappingEntity deleteEntity : deleteEntities) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting desired config from ServiceComponent"
                + ", clusterId=" + desiredStateEntity.getClusterId()
                + ", serviceName=" + desiredStateEntity.getServiceName()
                + ", componentName=" + desiredStateEntity.getComponentName()
                + ", configType=" + deleteEntity.getConfigType()
                + ", configVersionTag=" + deleteEntity.getVersionTag());
          }
          desiredStateEntity.getComponentConfigMappingEntities().remove(
              deleteEntity);
          componentConfigMappingDAO.remove(deleteEntity);
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
    return gson.fromJson(desiredStateEntity.getDesiredStackVersion(), StackId.class);
  }

  @Override
  public synchronized void setDesiredStackVersion(StackId stackVersion) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredStackVersion of Service"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + getName()
          + ", oldDesiredStackVersion=" + getDesiredStackVersion()
          + ", newDesiredStackVersion=" + stackVersion);
    }
    desiredStateEntity.setDesiredStackVersion(gson.toJson(stackVersion));
    saveIfPersisted();
  }

  @Override
  public synchronized ServiceComponentResponse convertToResponse() {
    ServiceComponentResponse r  = new ServiceComponentResponse(
        getClusterId(), service.getCluster().getClusterName(),
        service.getName(), getName(), this.desiredConfigs,
        getDesiredStackVersion().getStackId(),
        getDesiredState().toString());
    return r;
  }

  @Override
  public String getClusterName() {
    return service.getCluster().getClusterName();
  }

  @Override
  public synchronized void debugDump(StringBuilder sb) {
    sb.append("ServiceComponent={ serviceComponentName=" + getName()
        + ", clusterName=" + service.getCluster().getClusterName()
        + ", clusterId=" + service.getCluster().getClusterId()
        + ", serviceName=" + service.getName()
        + ", desiredStackVersion=" + getDesiredStackVersion()
        + ", desiredState=" + getDesiredState().toString()
        + ", hostcomponents=[ ");
    boolean first = true;
    for(ServiceComponentHost sch : hostComponents.values()) {
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

  @Override
  public synchronized boolean isPersisted() {
      return persisted;
  }

  @Override
  public synchronized void persist() {
    if (!persisted) {
      persistEntities();
      refresh();
      service.refresh();
      persisted = true;
    } else {
      saveIfPersisted();
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
  public synchronized void refresh() {
    if (isPersisted()) {
      ServiceComponentDesiredStateEntityPK pk = new ServiceComponentDesiredStateEntityPK();
      pk.setComponentName(getName());
      pk.setClusterId(getClusterId());
      pk.setServiceName(getServiceName());
      // TODO: desiredStateEntity is assigned in unsynchronized way, may be a bug
      desiredStateEntity = serviceComponentDesiredStateDAO.findByPK(pk);
      serviceComponentDesiredStateDAO.refresh(desiredStateEntity);
    }
  }

  @Transactional
  private synchronized void saveIfPersisted() {
    if (isPersisted()) {
      serviceComponentDesiredStateDAO.merge(desiredStateEntity);
    }
  }

  @Override
  public boolean isClientComponent() {
    return this.isClientComponent;
  }

  @Override
  public synchronized boolean canBeRemoved() {
    State state = getDesiredState();
    if (state != State.INIT
        && state != State.UNINSTALLED) {
      return false;
    }

    boolean safeToRemove = true;
    for (ServiceComponentHost sch : hostComponents.values()) {
      if (!sch.canBeRemoved()) {
        safeToRemove = false;
        LOG.warn("Found non removable hostcomponent when trying to"
            + " delete service component"
            + ", clusterName=" + getClusterName()
            + ", serviceName=" + getServiceName()
            + ", componentName=" + getName()
            + ", hostname=" + sch.getHostName());
        break;
      }
    }
    return safeToRemove;
  }

  @Override
  public synchronized void removeAllServiceComponentHosts()
      throws AmbariException {
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
    hostComponents.clear();
    // FIXME update DB
  }

  @Override
  public synchronized void removeServiceComponentHosts(String hostname)
      throws AmbariException {
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
    hostComponents.remove(hostname);
    // FIXME update DB
  }

  @Override
  public synchronized void deleteDesiredConfigs(Set<String> configTypes) {
    for (String configType : configTypes) {
      desiredConfigs.remove(configType);
    }
    componentConfigMappingDAO.removeByType(configTypes);
  }

}
