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

package org.apache.hms.common.entity.command;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD) 
@XmlType(name="", propOrder = {})
public class StatusCommand extends Command {
  @XmlElement
  protected String cmdId;
  @XmlElement
  protected String nodePath;

  public StatusCommand() {
    this.cmd = CmdType.STATUS;
  }
  
  public StatusCommand(boolean dryRun, String cmdId, String nodePath) {
    this.cmd = CmdType.STATUS;
    this.cmdId = cmdId;
    this.nodePath = nodePath;
  }
  
  public void setCmdId(String cmdId) {
    this.cmdId = cmdId;
  }
  
  public String getCmdId() {
    return cmdId;
  }
  
  public void setNodePath(String nodePath) {
    this.nodePath = nodePath;
  }
  
  public String getNodePath() {
    return nodePath;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString());
    sb.append(", cmdId=");
    sb.append(cmdId);
    sb.append(", nodePath=");
    sb.append(nodePath);
    return sb.toString();
  }
}
