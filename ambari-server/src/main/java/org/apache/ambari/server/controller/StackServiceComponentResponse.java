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

package org.apache.ambari.server.controller;

public class StackServiceComponentResponse {

  private String stackName;
  private String stackVersion;
  private String serviceName;

  private String componentName;

  private String componentCategory;

  private boolean isClient;

  private boolean isMaster;

  public StackServiceComponentResponse(String componentName, String componentCategory,
      boolean isClient, boolean isMaster) {
    setComponentName(componentName);
    setComponentCategory(componentCategory);
    setClient(isClient);
    setMaster(isMaster);
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public String getStackVersion() {
    return stackVersion;
  }

  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getComponentCategory() {
    return componentCategory;
  }

  public void setComponentCategory(String componentCategory) {
    this.componentCategory = componentCategory;
  }

  public boolean isClient() {
    return isClient;
  }

  public void setClient(boolean isClient) {
    this.isClient = isClient;
  }

  public boolean isMaster() {
    return isMaster;
  }

  public void setMaster(boolean isMaster) {
    this.isMaster = isMaster;
  }

}
