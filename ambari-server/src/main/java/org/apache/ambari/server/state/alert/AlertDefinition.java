/*
® * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.ambari.server.state.alert;

import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link AlertDefinition} class represents all of the necessary information
 * to schedule, run, and collect alerts.
 * <p/>
 * Although all members of this class must define a complex {@code equals} and
 * {@code hashCode} method, this class itself does not. Instead,
 * {@link #equals(Object)} is defined as a name comparison only since name is
 * unique to a definition. This allows us to easily insert instances of this
 * class into a {@link HashSet} if necessary.
 * <p/>
 * When making comparisons for equality for things like stack/database merging,
 * use {@link #deeplyEquals(Object)}.
 */
public class AlertDefinition {

  private long clusterId;
  private long definitionId;
  private String serviceName = null;
  private String componentName = null;

  private String name = null;
  private Scope scope = Scope.ANY;
  private int interval = 1;
  private boolean enabled = true;
  private Source source = null;
  private String label = null;
  private String description = null;
  private String uuid = null;

  @SerializedName("ignore_host")
  private boolean ignoreHost = false;

  @SerializedName("help_url")
  private String helpURL = null;

  /**
   * Gets the cluster ID for this definition.
   *
   * @return
   */
  public long getClusterId() {
    return clusterId;
  }

  /**
   * Sets the cluster ID for this definition.
   *
   * @param clusterId
   */
  public void setClusterId(long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * @return the definitionId
   */
  public long getDefinitionId() {
    return definitionId;
  }

  /**
   * @param definitionId
   *          the definitionId to set
   */
  public void setDefinitionId(long definitionId) {
    this.definitionId = definitionId;
  }

  /**
   * @return the service name
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param name the service name
   */
  public void setServiceName(String name) {
    serviceName = name;
  }

  /**
   * @return the component name
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   *
   * @param name the component name
   */
  public void setComponentName(String name) {
    componentName = name;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param definitionName
   *          the definition name.
   */
  public void setName(String definitionName) {
    name = definitionName;
  }

  /**
   * @return the scope
   */
  public Scope getScope() {
    return scope;
  }

  public void setScope(Scope definitionScope) {
    if (null == definitionScope) {
      definitionScope = Scope.ANY;
    }

    scope = definitionScope;
  }

  /**
   * @return the interval
   */
  public int getInterval() {
    return interval;
  }

  public void setInterval(int definitionInterval) {
    interval = definitionInterval;
  }

  /**
   * @return {@code true} if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean definitionEnabled) {
    enabled = definitionEnabled;
  }

  /**
   * @return {@code true} if the host is ignored.
   */
  public boolean isHostIgnored() {
    return ignoreHost;
  }

  public void setHostIgnored(boolean definitionHostIgnored) {
    ignoreHost = definitionHostIgnored;
  }

  public Source getSource() {
    return source;
  }

  public void setSource(Source definitionSource) {
    source = definitionSource;
  }

  /**
   * @return the label for the definition or {@code null} if none.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the label for this definition.
   *
   * @param definitionLabel
   */
  public void setLabel(String definitionLabel) {
    label = definitionLabel;
  }

  /**
   * @return the help url for this definition or {@code null} if none.
   */
  public String getHelpURL() {
    return helpURL;
  }

  /**
   * Sets the help url for this definition.
   *
   * @param helpURL
   */
  public void setHelpURL(String helpURL) {
    this.helpURL = helpURL;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description
   *          the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Sets the UUID of the definition
   *
   * @param definitionUuid
   */
  public void setUuid(String definitionUuid) {
    uuid = definitionUuid;
  }

  /**
   * Gets the UUID of the definition. The UUID is a unique identifier that is
   * generated every time the definition's state changes.
   *
   * @return the UUID
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Compares {@link #equals(Object)} of every field. This is used mainly for
   * reconciling the stack versus the database.
   *
   * @param obj
   * @return
   */
  public boolean deeplyEquals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    AlertDefinition other = (AlertDefinition) obj;
    if (componentName == null) {
      if (other.componentName != null) {
        return false;
      }
    } else if (!componentName.equals(other.componentName)) {
      return false;
    }

    if (enabled != other.enabled) {
      return false;
    }

    if (ignoreHost != other.ignoreHost) {
      return false;
    }

    if (interval != other.interval) {
      return false;
    }

    if (label == null) {
      if (other.label != null) {
        return false;
      }
    } else if (!label.equals(other.label)) {
      return false;
    }

    if (!StringUtils.equals(helpURL, other.helpURL)) {
      return false;
    }

    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }

    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }

    if (null == scope) {
      scope = Scope.ANY;
    }

    if (scope != other.scope) {
      return false;
    }

    if (serviceName == null) {
      if (other.serviceName != null) {
        return false;
      }
    } else if (!serviceName.equals(other.serviceName)) {
      return false;
    }

    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }

    return true;
  }

  /**
   * Gets equality based on name only.
   *
   * @see #deeplyEquals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (null == obj || !obj.getClass().equals(AlertDefinition.class)) {
      return false;
    }

    return name.equals(((AlertDefinition) obj).name);
  }

  /**
   * Gets a hash based on name only.
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
