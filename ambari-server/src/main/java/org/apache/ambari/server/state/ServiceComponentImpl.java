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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceComponentImpl implements ServiceComponent {

  private final static Logger LOG =
      LoggerFactory.getLogger(ServiceComponentImpl.class);

  private final Service service;
  private final String componentName;

  private Map<String, Config> configs;

  private State desiredState;
  private Map<String, Config>  desiredConfigs;
  private StackVersion desiredStackVersion;

  private Map<String, ServiceComponentHost> hostComponents;


  private void init() {
    // TODO
    // initialize from DB
  }

  public ServiceComponentImpl(Service service,
      String componentName) {
    this.service = service;
    this.componentName = componentName;
    this.desiredState = State.INIT;
    this.configs = new HashMap<String, Config>();
    this.desiredConfigs = new HashMap<String, Config>();
    this.desiredStackVersion = new StackVersion("");
    this.hostComponents = new HashMap<String, ServiceComponentHost>();
    init();
  }


  @Override
  public synchronized String getName() {
    return componentName;
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
  public synchronized Map<String, Config> getConfigs() {
    return Collections.unmodifiableMap(configs);
  }

  @Override
  public synchronized void updateConfigs(Map<String, Config> configs) {
    this.configs = configs;
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
          + ", serviceComponentName=" + componentName
          + ", hostname=" + hostComponent.getHostName());
    }
    if (hostComponents.containsKey(hostComponent.getHostName())) {
      throw new AmbariException("Cannot add duplicate ServiceComponentHost"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + componentName
          + ", hostname=" + hostComponent.getHostName());
    }
    this.hostComponents.put(hostComponent.getHostName(),
        hostComponent);
  }

  @Override
  public ServiceComponentHost getServiceComponentHost(String hostname)
    throws AmbariException {
    if (!hostComponents.containsKey(hostname)) {
      throw new ServiceComponentHostNotFoundException(getClusterName(),
          getServiceName(), componentName, hostname);
    }
    return this.hostComponents.get(hostname);
  }

  @Override
  public synchronized State getDesiredState() {
    return desiredState;
  }

  @Override
  public synchronized void setDesiredState(State state) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + componentName
          + ", oldDesiredState=" + this.desiredState
          + ", newDesiredState=" + state);
    }
    this.desiredState = state;
  }

  @Override
  public synchronized Map<String, Config> getDesiredConfigs() {
    return Collections.unmodifiableMap(desiredConfigs);
  }

  @Override
  public synchronized void updateDesiredConfigs(Map<String, Config> configs) {
    this.desiredConfigs.putAll(configs);
  }

  @Override
  public synchronized StackVersion getDesiredStackVersion() {
    return desiredStackVersion;
  }

  @Override
  public synchronized void setDesiredStackVersion(StackVersion stackVersion) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredStackVersion of Service"
          + ", clusterName=" + service.getCluster().getClusterName()
          + ", clusterId=" + service.getCluster().getClusterId()
          + ", serviceName=" + service.getName()
          + ", serviceComponentName=" + componentName
          + ", oldDesiredStackVersion=" + this.desiredStackVersion
          + ", newDesiredStackVersion=" + stackVersion);
    }
    this.desiredStackVersion = stackVersion;
  }

  private synchronized Map<String, String> getConfigVersions() {
    Map<String, String> configVersions = new HashMap<String, String>();
    for (Config c : configs.values()) {
      configVersions.put(c.getType(), c.getVersionTag());
    }
    return configVersions;
  }

  @Override
  public synchronized ServiceComponentResponse convertToResponse() {
    ServiceComponentResponse r  = new ServiceComponentResponse(
        getClusterId(), service.getCluster().getClusterName(),
        service.getName(), componentName, getConfigVersions(),
        getDesiredStackVersion().getStackVersion(),
        getDesiredState().toString());
    return r;
  }

  @Override
  public String getClusterName() {
    return service.getCluster().getClusterName();
  }

}
