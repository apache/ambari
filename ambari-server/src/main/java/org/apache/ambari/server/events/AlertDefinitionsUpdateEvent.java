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

package org.apache.ambari.server.events;

import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.Source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains info about alert definitions update. This update will be sent to all subscribed recipients.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AlertDefinitionsUpdateEvent extends AmbariUpdateEvent {

  @JsonProperty("clusterId")
  private Long clusterId;

  @JsonProperty("componentName")
  private String componentName;

  @JsonProperty("description")
  private String description;

  @JsonProperty("enabled")
  private Boolean enabled;

  @JsonProperty("helpUrl")
  private String helpURL;

  @JsonProperty("id")
  private Long definitionId;

  @JsonProperty("ignoreHost")
  private Boolean ignoreHost;

  @JsonProperty("interval")
  private Integer interval;

  @JsonProperty("label")
  private String label;

  @JsonProperty("name")
  private String name;

  @JsonProperty("repeatTolerance")
  private Integer repeatTolerance;

  @JsonProperty("repeatToleranceEnabled")
  private Boolean repeatToleranceEnabled;

  @JsonProperty("scope")
  private Scope scope;

  @JsonProperty("serviceName")
  private String serviceName;

  @JsonProperty("source")
  private Source source;

  public AlertDefinitionsUpdateEvent(Long clusterId, String componentName, String description, Boolean enabled,
                               String helpURL, Long definitionId, Boolean ignoreHost, Integer interval, String label,
                               String name, Integer repeatTolerance, Boolean repeatToleranceEnabled, Scope scope,
                               String serviceName, Source source) {
    super(Type.ALERT_DEFINITIONS);
    this.clusterId = clusterId;
    this.componentName = componentName;
    this.description = description;
    this.enabled = enabled;
    this.helpURL = helpURL;
    this.definitionId = definitionId;
    this.ignoreHost = ignoreHost;
    this.interval = interval;
    this.label = label;
    this.name = name;
    this.repeatTolerance = repeatTolerance;
    this.repeatToleranceEnabled = repeatToleranceEnabled;
    this.scope = scope;
    this.serviceName = serviceName;
    this.source = source;
  }

  public AlertDefinitionsUpdateEvent(AlertDefinition alertDefinition, Integer repeatTolerance, Boolean repeatToleranceEnabled) {
    this(alertDefinition.getClusterId(), alertDefinition.getComponentName(), alertDefinition.getDescription(),
        alertDefinition.isEnabled(), alertDefinition.getHelpURL(), alertDefinition.getDefinitionId(),
        alertDefinition.isHostIgnored(), alertDefinition.getInterval(), alertDefinition.getLabel(),
        alertDefinition.getName(), repeatTolerance, repeatToleranceEnabled, alertDefinition.getScope(),
        alertDefinition.getServiceName(), alertDefinition.getSource());
  }

  public AlertDefinitionsUpdateEvent(long definitionId) {
    super(Type.ALERT_DEFINITIONS);
    this.definitionId = definitionId;
  }

  public AlertDefinitionsUpdateEvent(AlertDefinitionEntity alertDefinitionEntity) {
    super(Type.ALERT_DEFINITIONS);
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getHelpURL() {
    return helpURL;
  }

  public void setHelpURL(String helpURL) {
    this.helpURL = helpURL;
  }

  public Long getDefinitionId() {
    return definitionId;
  }

  public void setDefinitionId(Long definitionId) {
    this.definitionId = definitionId;
  }

  public Boolean getIgnoreHost() {
    return ignoreHost;
  }

  public void setIgnoreHost(Boolean ignoreHost) {
    this.ignoreHost = ignoreHost;
  }

  public Integer getInterval() {
    return interval;
  }

  public void setInterval(Integer interval) {
    this.interval = interval;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getRepeatTolerance() {
    return repeatTolerance;
  }

  public void setRepeatTolerance(Integer repeatTolerance) {
    this.repeatTolerance = repeatTolerance;
  }

  public Boolean getRepeatToleranceEnabled() {
    return repeatToleranceEnabled;
  }

  public void setRepeatToleranceEnabled(Boolean repeatToleranceEnabled) {
    this.repeatToleranceEnabled = repeatToleranceEnabled;
  }

  public Scope getScope() {
    return scope;
  }

  public void setScope(Scope scope) {
    this.scope = scope;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public Source getSource() {
    return source;
  }

  public void setSource(Source source) {
    this.source = source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AlertDefinitionsUpdateEvent that = (AlertDefinitionsUpdateEvent) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (description != null ? !description.equals(that.description) : that.description != null) return false;
    if (enabled != null ? !enabled.equals(that.enabled) : that.enabled != null) return false;
    if (helpURL != null ? !helpURL.equals(that.helpURL) : that.helpURL != null) return false;
    if (definitionId != null ? !definitionId.equals(that.definitionId) : that.definitionId != null) return false;
    if (ignoreHost != null ? !ignoreHost.equals(that.ignoreHost) : that.ignoreHost != null) return false;
    if (interval != null ? !interval.equals(that.interval) : that.interval != null) return false;
    if (label != null ? !label.equals(that.label) : that.label != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (repeatTolerance != null ? !repeatTolerance.equals(that.repeatTolerance) : that.repeatTolerance != null)
      return false;
    if (repeatToleranceEnabled != null ? !repeatToleranceEnabled.equals(that.repeatToleranceEnabled) : that.repeatToleranceEnabled != null)
      return false;
    if (scope != that.scope) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
    return source != null ? source.equals(that.source) : that.source == null;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (enabled != null ? enabled.hashCode() : 0);
    result = 31 * result + (helpURL != null ? helpURL.hashCode() : 0);
    result = 31 * result + (definitionId != null ? definitionId.hashCode() : 0);
    result = 31 * result + (ignoreHost != null ? ignoreHost.hashCode() : 0);
    result = 31 * result + (interval != null ? interval.hashCode() : 0);
    result = 31 * result + (label != null ? label.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (repeatTolerance != null ? repeatTolerance.hashCode() : 0);
    result = 31 * result + (repeatToleranceEnabled != null ? repeatToleranceEnabled.hashCode() : 0);
    result = 31 * result + (scope != null ? scope.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (source != null ? source.hashCode() : 0);
    return result;
  }
}
