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

package org.apache.ambari.server.orm.entities;

import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.TargetHostType;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@NamedQueries({
    @NamedQuery(name = "allActions", query =
        "SELECT actions " +
            "FROM ActionEntity actions")
})
@Table(name = "action")
@Entity
public class ActionEntity {

  @Id
  @Column(name = "action_name")
  private String actionName;

  @Column(name = "action_type")
  @Enumerated(EnumType.STRING)
  private ActionType actionType = ActionType.DISABLED;

  @Column(name = "inputs")
  @Basic
  private String inputs;

  @Column(name = "target_service")
  @Basic
  private String targetService;

  @Column(name = "target_component")
  @Basic
  private String targetComponent;

  @Column(name = "description", nullable = false)
  @Basic
  private String description = "";

  @Column(name = "target_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private TargetHostType targetType = TargetHostType.ANY;

  @Basic
  @Column(name = "default_timeout", nullable = false)
  private Short defaultTimeout = 600;

  public String getActionName() {
    return actionName;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public ActionType getActionType() {
    return actionType;
  }

  public void setActionType(ActionType actionType) {
    this.actionType = actionType;
  }

  public String getInputs() {
    return inputs;
  }

  public void setInputs(String inputs) {
    this.inputs = inputs;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TargetHostType getTargetType() {
    return targetType;
  }

  public void setTargetType(TargetHostType targetType) {
    this.targetType = targetType;
  }

  public String getTargetService() {
    return targetService;
  }

  public void setTargetService(String targetService) {
    this.targetService = targetService;
  }

  public String getTargetComponent() {
    return targetComponent;
  }

  public void setTargetComponent(String targetComponent) {
    this.targetComponent = targetComponent;
  }

  public Short getDefaultTimeout() {
    return defaultTimeout;
  }

  public void setDefaultTimeout(Short defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ActionEntity that = (ActionEntity) o;

    if (actionName != null ? !actionName.equals(that.actionName) : that.actionName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = actionName != null ? actionName.hashCode() : 0;
    return result;
  }
}
