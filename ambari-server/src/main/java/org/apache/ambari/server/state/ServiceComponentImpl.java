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
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
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
  Gson gson;
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

  boolean persisted = false;
  private ServiceComponentDesiredStateEntity desiredStateEntity;

  // [ type -> versionTag ]
  private Map<String, String>  desiredConfigs;

  private Map<String, ServiceComponentHost> hostComponents;
  private Injector injector;

  private final boolean isClientComponent;

  private void init() {
    // TODO
    // initialize from DB
  }

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service,
      @Assisted String componentName, Injector injector) {
    this.injector = injector;
    injector.injectMembers(this);
    this.service = service;
    this.desiredStateEntity = new ServiceComponentDesiredStateEntity();
    desiredStateEntity.setComponentName(componentName);
    desiredStateEntity.setDesiredState(State.INIT);

    this.desiredConfigs = new HashMap<String, String>();
    setDesiredStackVersion(new StackVersion(""));

    this.hostComponents = new HashMap<String, ServiceComponentHost>();

    // FIXME use meta data library to decide client or not
    this.isClientComponent = false;

    init();
  }

  @AssistedInject
  public ServiceComponentImpl(@Assisted Service service,
                              @Assisted ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity,
                              Injector injector) {
    this.injector = injector;
    injector.injectMembers(this);
    this.service = service;
    this.desiredStateEntity = serviceComponentDesiredStateEntity;

    // FIXME use meta data library to decide client or not
    this.isClientComponent = false;

    this.desiredConfigs = new HashMap<String, String>();

    this.hostComponents = new HashMap<String, ServiceComponentHost>();
    for (HostComponentStateEntity hostComponentStateEntity : desiredStateEntity.getHostComponentStateEntities()) {
      HostComponentDesiredStateEntityPK pk = new HostComponentDesiredStateEntityPK();
      pk.setClusterId(hostComponentStateEntity.getClusterId());
      pk.setServiceName(hostComponentStateEntity.getServiceName());
      pk.setComponentName(hostComponentStateEntity.getComponentName());
      pk.setHostName(hostComponentStateEntity.getHostName());

      HostComponentDesiredStateEntity hostComponentDesiredStateEntity = hostComponentDesiredStateDAO.findByPK(pk);

      hostComponents.put(hostComponentStateEntity.getComponentName(),
          serviceComponentHostFactory.createExisting(this, hostComponentStateEntity, hostComponentDesiredStateEntity));
    }
    
    for (ComponentConfigMappingEntity entity : desiredStateEntity.getComponentConfigMappingEntities()) {
      desiredConfigs.put(entity.getConfigType(), entity.getVersionTag());
    }

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
    return Collections.unmodifiableMap(map);
  }

  @Override
  public synchronized void updateDesiredConfigs(Map<String, Config> configs) {
    for (Entry<String,Config> entry : configs.entrySet()) {
      ComponentConfigMappingEntity newEntity = new ComponentConfigMappingEntity();
      newEntity.setClusterId(desiredStateEntity.getClusterId());
      newEntity.setServiceName(desiredStateEntity.getServiceName());
      newEntity.setComponentName(desiredStateEntity.getComponentName());
      newEntity.setConfigType(entry.getKey());
      newEntity.setVersionTag(entry.getValue().getVersionTag());
      newEntity.setTimestamp(Long.valueOf(new java.util.Date().getTime()));
      
      if (!desiredStateEntity.getComponentConfigMappingEntities().contains(newEntity)) {
        newEntity.setServiceComponentDesiredStateEntity(desiredStateEntity);
        desiredStateEntity.getComponentConfigMappingEntities().add(newEntity);
      } else {
        for (ComponentConfigMappingEntity entity : desiredStateEntity.getComponentConfigMappingEntities()) {
          if (entity.equals(newEntity)) {
            entity.setVersionTag(newEntity.getVersionTag());
            entity.setTimestamp(newEntity.getTimestamp());
          }
        }
      }
        
      this.desiredConfigs.put(entry.getKey(), entry.getValue().getVersionTag());
    }
  }

  @Override
  public synchronized StackVersion getDesiredStackVersion() {
    return gson.fromJson(desiredStateEntity.getDesiredStackVersion(), StackVersion.class);
  }

  @Override
  public synchronized void setDesiredStackVersion(StackVersion stackVersion) {
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

  private synchronized Map<String, String> getConfigVersions() {
    Map<String, String> configVersions = new HashMap<String, String>();
//    for (Config c : desiredConfigs.values()) {
//      configVersions.put(c.getType(), c.getVersionTag());
//    }
//    return configVersions;
    return desiredConfigs;
  }

  @Override
  public synchronized ServiceComponentResponse convertToResponse() {
    ServiceComponentResponse r  = new ServiceComponentResponse(
        getClusterId(), service.getCluster().getClusterName(),
        service.getName(), getName(), getConfigVersions(),
        getDesiredStackVersion().getStackVersion(),
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
  @Transactional
  public synchronized void persist() {
    if (!persisted) {
      ClusterServiceEntityPK pk = new ClusterServiceEntityPK();
      pk.setClusterId(service.getClusterId());
      pk.setServiceName(service.getName());
      ClusterServiceEntity serviceEntity = clusterServiceDAO.findByPK(pk);

      desiredStateEntity.setClusterServiceEntity(serviceEntity);
      serviceComponentDesiredStateDAO.create(desiredStateEntity);
      clusterServiceDAO.merge(serviceEntity);
      desiredStateEntity = serviceComponentDesiredStateDAO.merge(desiredStateEntity);
      service.refresh();
      persisted = true;
    } else {
      saveIfPersisted();
    }
  }

  @Override
  public void refresh() {
    if (isPersisted()) {
      ServiceComponentDesiredStateEntityPK pk = new ServiceComponentDesiredStateEntityPK();
      pk.setComponentName(getName());
      pk.setClusterId(getClusterId());
      pk.setServiceName(getServiceName());
      desiredStateEntity = serviceComponentDesiredStateDAO.findByPK(pk);
      serviceComponentDesiredStateDAO.refresh(desiredStateEntity);
    }
  }

  @Transactional
  private void saveIfPersisted() {
    if (isPersisted()) {
      serviceComponentDesiredStateDAO.merge(desiredStateEntity);
    }
  }

  @Override
  public boolean isClientComponent() {
    return this.isClientComponent;
  }


}
