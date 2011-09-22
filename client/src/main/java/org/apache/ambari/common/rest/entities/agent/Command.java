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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * Data model for Ambari Controller to issue command to Ambari Agent.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class Command {
  public Command() {
  }
  
  public Command(String id, String user, String[] cmd) {
    this.id = id;
    this.cmd = cmd;
    this.user = user;
  }
  
  @XmlElement  
  private String id;
  
  @XmlElement
  private String[] cmd;

  @XmlElement
  private String user;
  
  public String getId() {
    return id;
  }
  
  public String[] getCmd() {
    return cmd;
  }
  
  public String getUser() {
    return user;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public void setCmd(String[] cmd) {
    this.cmd = cmd;
  }
  
  public void setUser(String user) {
    this.user = user;
  }
}
