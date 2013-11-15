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


public class StackConfigurationResponse {

  
  public StackConfigurationResponse(String propertyName, String propertyValue, String propertyDescription, String type) {
    setPropertyName(propertyName);
    setPropertyValue(propertyValue);
    setPropertyDescription(propertyDescription);
    setType(type);
  }

  private String stackName;
  private String stackVersion;
  private String serviceName;
  private String propertyName;
  private String propertyValue;
  private String propertyDescription;
  private String type;

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

  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public String getPropertyValue() {
    return propertyValue;
  }

  public void setPropertyValue(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  public String getPropertyDescription() {
    return propertyDescription;
  }

  public void setPropertyDescription(String propertyDescription) {
    this.propertyDescription = propertyDescription;
  }

  public String getType() {
    return type;
  }
  
  public void setType(String type) {
    this.type = type;
  }
}
