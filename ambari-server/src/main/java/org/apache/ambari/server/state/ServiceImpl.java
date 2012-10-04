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


public class ServiceImpl implements Service {

  private final Cluster cluster;
  private final String serviceName;
  private DeployState state;
  private Map<String, Config> configs;
  private Map<String, ServiceComponent> components;
  private StackVersion stackVersion;
  
  private void init() {
    // TODO
    // initialize from DB 
  }
  
  public ServiceImpl(Cluster cluster, String serviceName,
      DeployState state, Map<String, Config> configs) {
    this.cluster = cluster;
    this.serviceName = serviceName;
    this.state = state;
    if (configs != null) {
      this.configs = configs;
    } else {
      this.configs = new HashMap<String, Config>();
    }
    this.components = new HashMap<String, ServiceComponent>();
    init();
  }

  public ServiceImpl(Cluster cluster, String serviceName,
      Map<String, Config> configs) {
    this(cluster, serviceName, DeployState.INIT, configs);
  }
  
  public ServiceImpl(Cluster cluster, String serviceName) {
    this(cluster, serviceName, DeployState.INIT, null);
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
  public synchronized long getCurrentHostComponentMappingVersion() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public synchronized Map<String, ServiceComponent> getServiceComponents() {
    return Collections.unmodifiableMap(components);
  }

  @Override
  public synchronized DeployState getState() {
    return state;
  }

  @Override
  public synchronized void setState(DeployState state) {
    this.state = state;
  }

  @Override
  public synchronized StackVersion getStackVersion() {
    return stackVersion;
  }

  @Override
  public synchronized void setStackVersion(StackVersion stackVersion) {
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
  public synchronized void setCurrentHostComponentMappingVersion(long version) {
    // TODO Auto-generated method stub    
  }

  @Override
  public synchronized void addServiceComponents(
      Map<String, ServiceComponent> components) {
    this.components.putAll(components);
  }

  @Override
  public ServiceComponent getServiceComponent(String componentName) {
    return this.components.get(componentName);
  }

}
