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

/**
 * An alert represents a problem or notice for a cluster.
 */
public class Alert {
  private String name = null;
  private String instance = null;
  private String service = null;
  private String component = null;
  private String host = null;
  private AlertState state = AlertState.UNKNOWN;
  private String label = null;
  private String text = null;
  
 
  /**
   * Constructor.
   * @param alertName the name of the alert
   * @param alertInstance instance specific information in the event that two alert
   *    types can be run, ie Flume.
   * @param serviceName the service
   * @param componentName the component
   * @param hostName the host
   * @param alertState the state of the alertable event
   */
  public Alert(String alertName, String alertInstance, String serviceName,
      String componentName,  String hostName, AlertState alertState) {
    name = alertName;
    instance = alertInstance;
    service = serviceName;
    component = componentName;
    host = hostName;
    state = alertState;
  }
 
  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
 
  /**
   * @return the service
   */
  public String getService() {
    return service;
  }
  
  /**
   * @return the component
   */
  public String getComponent() {
    return component;
  }
 
  /**
   * @return the host
   */
  public String getHost() {
    return host;
  }
 
  /**
   * @return the state
   */
  public AlertState getState() {
    return state;
  }

  /**
   * @return a short descriptive label for the alert
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param alertLabel a short descriptive label for the alert
   */
  public void setLabel(String alertLabel) {
    label = alertLabel;
  }
 
  /**
   * @return detail text about the alert
   */
  public String getText() {
    return text;
  }
  
  /**
   * @param alertText detail text about the alert
   */
  public void setText(String alertText) {
    text = alertText;
  }
  
  @Override
  public int hashCode() {
    int result = 0;
    
    result += (null != name) ? name.hashCode() : 0;
    result += 31 * result + (null != instance ? instance.hashCode() : 0);
    result += 31 * result + (null != service ? service.hashCode() : 0);
    result += 31 * result + (null != component ? component.hashCode() : 0);
    result += 31 * result + (null != host ? host.hashCode() : 0);
    
    return result;
  }

  /**
   * An alert's uniqueness comes from a combination of name, instance, service,
   * component and host.
   */
  @Override
  public boolean equals(Object o) {
    if (null == o || !Alert.class.isInstance(o))
      return false;
    
    return hashCode() == o.hashCode();
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("state=").append(state).append(", ");
    sb.append("name=").append(name).append(", ");
    sb.append("service=").append(service).append(", ");
    sb.append("component=").append(component).append(", ");
    sb.append("host=").append(host).append(", ");
    sb.append("instance=").append(instance);
    sb.append('}');
    return sb.toString();
  }
  

}
