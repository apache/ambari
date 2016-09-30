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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
/**
 * An alert represents a problem or notice for a cluster.
 */
public class Alert {
  private String cluster = null;
  private String name = null;
  private String instance = null;
  private String service = null;
  private String component = null;
  private String hostName = null;
  private AlertState state = AlertState.UNKNOWN;
  private String label = null;
  private String text = null;
  private long timestamp = 0L;

  // Maximum string size for MySql TEXT (utf8) column data type
  protected final static int MAX_ALERT_TEXT_SIZE = 32617;


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
    this.hostName = hostName;
    state = alertState;
  }

  public Alert() {
  }

  /**
   * @return the name
   */

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @return the service
   */
  @JsonProperty("service")
  public String getService() {
    return service;
  }

  /**
   * @return the component
   */
  @JsonProperty("component")
  public String getComponent() {
    return component;
  }

  /**
   * @return the host
   */
  @JsonProperty("host")
  public String getHostName() {
    return hostName;
  }

  /**
   * @return the state
   */
  @JsonProperty("state")
  public AlertState getState() {
    return state;
  }

  /**
   * @return a short descriptive label for the alert
   */
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  /**
   * @param alertLabel a short descriptive label for the alert
   */
  @JsonProperty("label")
  public void setLabel(String alertLabel) {
    label = alertLabel;
  }

  /**
   * @return detail text about the alert
   */
  @JsonProperty("text")
  public String getText() {
    return text;
  }

  /**
   * @param alertText detail text about the alert
   */
  @JsonProperty("text")
  public void setText(String alertText) {
    // middle-ellipsize the text to reduce the size to 32617 characters
    text = StringUtils.abbreviateMiddle(alertText, "…", MAX_ALERT_TEXT_SIZE);
  }

  @JsonProperty("instance")
  public String getInstance() {
    return instance;
  }

  @JsonProperty("instance")
  public void setInstance(String instance) {
    this.instance = instance;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("service")
  public void setService(String service) {
    this.service = service;
  }

  @JsonProperty("component")
  public void setComponent(String component) {
    this.component = component;
  }

  @JsonProperty("host")
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @JsonProperty("state")
  public void setState(AlertState state) {
    this.state = state;
  }

  @JsonProperty("timestamp")
  public void setTimestamp(long ts) {
    timestamp = ts;
  }

  @JsonProperty("timestamp")
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @return
   */
  @JsonProperty("cluster")
  public String getCluster() {
    return cluster;
  }

  @JsonProperty("cluster")
  public void setCluster(String cluster){
    this.cluster = cluster;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((service == null) ? 0 : service.hashCode());
    result = prime * result + ((component == null) ? 0 : component.hashCode());
    result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
    result = prime * result + ((cluster == null) ? 0 : cluster.hashCode());
    result = prime * result + ((instance == null) ? 0 : instance.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    Alert other = (Alert) obj;

    if (state != other.state) {
      return false;
    }

    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }

    if (service == null) {
      if (other.service != null) {
        return false;
      }
    } else if (!service.equals(other.service)) {
      return false;
    }

    if (component == null) {
      if (other.component != null) {
        return false;
      }
    } else if (!component.equals(other.component)) {
      return false;
    }

    if (hostName == null) {
      if (other.hostName != null) {
        return false;
      }
    } else if (!hostName.equals(other.hostName)) {
      return false;
    }

    if (cluster == null) {
      if (other.cluster != null) {
        return false;
      }
    } else if (!cluster.equals(other.cluster)) {
      return false;
    }

    if (instance == null) {
      if (other.instance != null) {
        return false;
      }
    } else if (!instance.equals(other.instance)) {
      return false;
    }


    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("cluster=").append(cluster).append(", ");
    sb.append("state=").append(state).append(", ");
    sb.append("name=").append(name).append(", ");
    sb.append("service=").append(service).append(", ");
    sb.append("component=").append(component).append(", ");
    sb.append("host=").append(hostName).append(", ");
    sb.append("instance=").append(instance).append(", ");
    sb.append("text='").append(text).append("'");
    sb.append('}');
    return sb.toString();
  }
}
