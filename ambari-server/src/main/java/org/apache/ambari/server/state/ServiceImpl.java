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
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.controller.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceImpl implements Service {

  private static final Logger LOG =
      LoggerFactory.getLogger(ServiceImpl.class);

  private final Cluster cluster;
  private final String serviceName;
  private State state;
  private State desiredState;
  private Map<String, Config> configs;
  private Map<String, Config> desiredConfigs;
  private Map<String, ServiceComponent> components;
  private StackVersion stackVersion;
  private StackVersion desiredStackVersion;

  private void init() {
    // TODO
    // initialize from DB
  }

  public ServiceImpl(Cluster cluster, String serviceName) {
    this.cluster = cluster;
    this.serviceName = serviceName;
    this.state = State.INIT;
    this.desiredState = State.INIT;
    this.configs = new HashMap<String, Config>();
    this.desiredConfigs = new HashMap<String, Config>();
    this.stackVersion = new StackVersion("");
    this.desiredStackVersion = new StackVersion("");
    this.components = new HashMap<String, ServiceComponent>();
    init();
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
  public synchronized Map<String, ServiceComponent> getServiceComponents() {
    return Collections.unmodifiableMap(components);
  }

  @Override
  public synchronized State getState() {
    return state;
  }

  @Override
  public synchronized void setState(State state) {
    if (state == null) {
      // TODO throw error?
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting state of Service"
          + ", oldState=" + this.state
          + ", newState=" + state);
    }
    this.state = state;
  }

  @Override
  public synchronized StackVersion getStackVersion() {
    return stackVersion;
  }

  @Override
  public synchronized void setStackVersion(StackVersion stackVersion) {
    if (stackVersion == null) {
      // TODO throw error?
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting StackVersion of Service"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + serviceName
          + ", oldStackVersion=" + this.stackVersion
          + ", newStackVersion=" + stackVersion);
    }
    this.stackVersion = stackVersion;
  }

  @Override
  public synchronized Map<String, Config> getConfigs() {
    return Collections.unmodifiableMap(configs);
  }

  @Override
  public synchronized void updateConfigs(Map<String, Config> configs) {
    this.configs.putAll(configs);
  }

  @Override
  public synchronized void addServiceComponents(
      Map<String, ServiceComponent> components) {
    for (ServiceComponent sc : components.values()) {
      addServiceComponent(sc);
    }
  }

  @Override
  public synchronized void addServiceComponent(ServiceComponent component) {
    // TODO validation
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding a ServiceComponent to Service"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + serviceName
          + ", serviceComponentName=" + component.getName());
    }
    this.components.put(component.getName(), component);
  }

  @Override
  public ServiceComponent getServiceComponent(String componentName)
      throws AmbariException {
    if (!components.containsKey(componentName)) {
      throw new ServiceComponentNotFoundException(cluster.getClusterName(),
          serviceName,
          componentName);
    }
    return this.components.get(componentName);
  }

  @Override
  public synchronized State getDesiredState() {
    return desiredState;
  }

  @Override
  public synchronized void setDesiredState(State state) {
    if (state == null) {
      // TODO throw error?
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredState of Service"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + serviceName
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
    if (stackVersion == null) {
      // TODO throw error?
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Setting DesiredStackVersion of Service"
          + ", clusterName=" + cluster.getClusterName()
          + ", clusterId=" + cluster.getClusterId()
          + ", serviceName=" + serviceName
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
  public synchronized ServiceResponse convertToResponse() {
    ServiceResponse r = new ServiceResponse(cluster.getClusterId(),
        cluster.getClusterName(),
        serviceName,
        stackVersion.getStackVersion(),
        getConfigVersions(),
        desiredStackVersion.getStackVersion(),
        desiredState.toString());
    return r;
  }

  @Override
  public Cluster getCluster() {
    return cluster;
  }

}
