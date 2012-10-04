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


public class ServiceComponentImpl implements ServiceComponent {

  private final Service service;
  
  private final String componentName;
  
  private DeployState state;
  
  private Map<String, Config> configs;

  private Map<String, ServiceComponentHost> hostComponents;
  
  private StackVersion stackVersion;

  private void init() {
    // TODO
    // initialize from DB 
  }
  
  public ServiceComponentImpl(Service service,
      String componentName, DeployState state, Map<String, Config> configs) {
    this.service = service;
    this.componentName = componentName;
    this.state = state;
    if (configs != null) {
      this.configs = configs;
    } else {
      this.configs = new HashMap<String, Config>();
    }
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
  public synchronized DeployState getState() {
    return state;
  }

  @Override
  public synchronized void setState(DeployState state) {
    this.state = state;
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
  public synchronized StackVersion getStackVersion() {
    return stackVersion;
  }

  @Override
  public synchronized void setStackVersion(StackVersion stackVersion) {
    this.stackVersion = stackVersion;
  }

  @Override
  public synchronized Map<String, ServiceComponentHost>
      getServiceComponentHosts() {
    return Collections.unmodifiableMap(hostComponents);
  }

  @Override
  public synchronized void addServiceComponentHosts(
      Map<String, ServiceComponentHost> hostComponents) {
    // TODO
    this.hostComponents.putAll(hostComponents);
  }

  @Override
  public ServiceComponentHost getServiceComponentHost(String hostname) {
    return this.hostComponents.get(hostname);
  }

}
