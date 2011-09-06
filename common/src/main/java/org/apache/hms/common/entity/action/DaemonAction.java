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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Action class for describing a daemon related action.
 * The valid operation are: 
 * 
 * - start a daemon
 * - stop a daemon
 * - check daemon status
 *
 */
@XmlRootElement
@XmlType(propOrder = { "daemonName" })
public class DaemonAction extends Action {

  @XmlElement(name="daemon")
  private String daemonName;
  
  public String getDaemonName() {
    return daemonName;
  }
  
  public void setDaemonName(String daemonName) {
    this.daemonName = daemonName;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString());
    sb.append(", daemon=");
    sb.append(daemonName);
    return sb.toString();
  }
}
