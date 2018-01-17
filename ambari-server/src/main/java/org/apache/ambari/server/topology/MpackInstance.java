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

package org.apache.ambari.server.topology;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.ambari.server.controller.internal.Stack;


public class MpackInstance {
  String mpackName;
  String mpackVersion;
  String uri;
  Stack stack;
  Configuration configuration;
  Collection<ServiceInstance> serviceInstances = new ArrayList<>();

  public MpackInstance(String mpackName, String mpackVersion, String uri, Stack stack, Configuration configuration) {
    this.mpackName = mpackName;
    this.mpackVersion = mpackVersion;
    this.uri = uri;
    this.stack = stack;
    this.configuration = configuration;
  }

  public MpackInstance() { }

  public String getMpackName() {
    return mpackName;
  }

  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  public Stack getStack() {
    return stack;
  }

  public void setStack(Stack stack) {
    this.stack = stack;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public Collection<ServiceInstance> getServiceInstances() {
    return serviceInstances;
  }

  public void setServiceInstances(Collection<ServiceInstance> serviceInstances) {
    this.serviceInstances = serviceInstances;
    serviceInstances.forEach(si -> si.setMpackInstance(this));
  }

  public void addServiceInstance(ServiceInstance serviceInstance) {
    serviceInstances.add(serviceInstance);
    serviceInstance.setMpackInstance(this);
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
}
