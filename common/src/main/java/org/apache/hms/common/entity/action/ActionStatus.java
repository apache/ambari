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

package org.apache.hms.common.entity.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.hms.common.entity.Response;
import org.apache.hms.common.entity.Status;
import org.apache.hms.common.entity.Status.StatusAdapter;
import org.apache.hms.common.entity.action.Action;

/**
 * ActionStatus record the execution result of an action.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD) 
@XmlType(name="", propOrder = {})
public class ActionStatus extends Response{
  @XmlElement
  @XmlJavaTypeAdapter(StatusAdapter.class)
  protected Status status;

  @XmlElement
  protected String host;
  @XmlElement
  protected String cmdPath;
  @XmlElement
  protected int actionId;
  @XmlElement
  public Action action;
  @XmlElement
  private String actionPath;
  
  public Status getStatus() {
    return this.status;
  }
  
  public void setStatus(Status status) {
    this.status = status;
  }
  
  public String getHost() {
    return host;
  }
  
  public void setHost(String host) {
    this.host = host;
  }
  
  public String getCmdPath() {
    return cmdPath;
  }
  
  public void setCmdPath(String cmdPath) {
    this.cmdPath = cmdPath;
  }
  
  public int getActionId() {
    return this.actionId;
  }
  
  public void setActionId(int actionId) {
    this.actionId = actionId;
  }
  
  public Action getAction() {
    return this.action;
  }
  
  public void setAction(Action action) {
    this.action = action;
  }
  
  public String getActionPath() {
    return this.actionPath;
  }
  
  public void setActionPath(String actionPath) {
    this.actionPath = actionPath;
  }
}
