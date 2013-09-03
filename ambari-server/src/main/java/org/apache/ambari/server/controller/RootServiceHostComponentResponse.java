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

public class RootServiceHostComponentResponse {

  private String hostName;
  private String componentName;
  private String componentState;
  private String componentVersion;
  private Map<String, String> properties;


  public RootServiceHostComponentResponse(String hostName, String componentName, String componentState,
      String componentVersion,
      Map<String, String> properties) {
    this.hostName = hostName;
    this.componentName = componentName;
    this.componentState = componentState;
    this.componentVersion = componentVersion;
    this.properties = properties;
  }

  public String getHostName() {
    return hostName;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RootServiceHostComponentResponse that = (RootServiceHostComponentResponse) o;

    if (hostName != null ?
        !hostName.equals(that.hostName) : that.hostName != null) {
      return false;
    }
    
    if (componentName != null ?
        !componentName.equals(that.componentName) : that.componentName != null) {
      return false;
    }
    
    if (componentState != null ?
        !componentState.equals(that.componentState) : that.componentState != null) {
      return false;
    }
    
    if (componentVersion != null ?
        !componentVersion.equals(that.componentVersion) : that.componentVersion != null) {
      return false;
    }
    
    if (properties != null ?
        !properties.equals(that.properties) : that.properties != null) {
      return false;
    }
    
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 + (hostName != null ? hostName.hashCode() : 0);
    result = result + (componentName != null ? componentName.hashCode() : 0);
    result = result + (componentState != null ? componentState.hashCode() : 0);
    result = result + (componentVersion != null ? componentVersion.hashCode() : 0);
    return result;
  }

  public String getComponentState() {
    return componentState;
  }

  public void setComponentState(String componentState) {
    this.componentState = componentState;
  }

  public String getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(String componentVersion) {
    this.componentVersion = componentVersion;
  }
}
