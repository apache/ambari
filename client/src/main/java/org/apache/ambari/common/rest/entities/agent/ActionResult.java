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

package org.apache.ambari.common.rest.entities.agent;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.common.rest.entities.agent.Action.Kind;

/**
 * 
 * Data model for reporting server related commands.
 * for example:
 * 
 * role: hadoop.datanode
 * processState: RUNNING
 * id: COMMAND-123456
 * stdout: Pid 123 is alive.
 * stderr:
 * exitCode: 0
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class ActionResult {
  @XmlElement
  private String id;
  @XmlElement
  private Kind kind;
  @XmlElement
  private List<CommandResult> commandResults;
  @XmlElement
  private List<CommandResult> cleanUpCommandResults;

  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public Kind getKind() {
    return kind;
  }
  
  public void setKind(Kind kind) {
    this.kind = kind;
  }
  
  public List<CommandResult> getCommandResults() {
    return commandResults;
  }
  
  public void setCommandResults(List<CommandResult> commandResults) {
    this.commandResults = commandResults;
  }

  public List<CommandResult> getCleanUpCommandResults() {
    return cleanUpCommandResults;  
  }
  
  public void setCleanUpResults(List<CommandResult> cleanUpResults) {
    this.cleanUpCommandResults = cleanUpResults;
    
  }
  
}
