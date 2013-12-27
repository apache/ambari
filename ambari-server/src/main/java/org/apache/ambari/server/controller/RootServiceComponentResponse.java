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

import java.util.Map;

public class RootServiceComponentResponse {

  private String serviceName;
  private String componentName;
  private Map<String, String> properties;
  private String componentVersion;

  public RootServiceComponentResponse(String componentName, String componentVersion, Map<String, String> properties) {
    this.componentName = componentName;
    this.setComponentVersion(componentVersion); 
    this.setProperties(properties);
    
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

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
  
  public String getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(String componentVersion) {
    this.componentVersion = componentVersion;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RootServiceComponentResponse that = (RootServiceComponentResponse) o;

    return !(componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) &&
        !(componentVersion != null ? !componentVersion.equals(that.componentVersion) : that.componentVersion != null) &&
        !(properties != null ? !properties.equals(that.properties) : that.properties != null);

  }

  @Override
  public int hashCode() {
    int result = 31 + (componentName != null ? componentName.hashCode() : 0);
    result += (componentVersion != null ? componentVersion.hashCode() : 0);
    return result;
  }
}
