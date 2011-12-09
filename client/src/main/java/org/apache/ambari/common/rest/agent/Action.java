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

package org.apache.ambari.common.rest.agent;

import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class Action {
  @XmlElement
  public Kind kind;
  @XmlElement
  public String clusterId;
  @XmlElement
  public String user;
  @XmlElement
  public String id;
  @XmlElement
  public String component;
  @XmlElement
  public String role;
  @XmlElement
  public Signal signal;
  @XmlElement
  public Command command;
  @XmlElement
  public Command cleanUpCommand;
  @XmlElement
  public long clusterDefinitionRevision;
  @XmlElement
  public String workDirComponent;
  @XmlElement
  public ConfigFile file;
  
  private static AtomicLong globalId = new AtomicLong();
  
  public Action() {
    long id = globalId.incrementAndGet();
    this.id = new Long(id).toString();
  }
  
  public Kind getKind() {
    return kind;
  }
  
  public void setKind(Kind kind) {
    this.kind = kind;
  }  

  public String getClusterId() {
    return clusterId;
  }
  
  public void setClusterId(String clusterId) {
    this.clusterId = clusterId;
  }
  
  public String getUser() {
    return user;
  }
  
  public void setUser(String user) {
    this.user = user;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getComponent() {
    return component;
  }
  
  public String getRole() {
    return role;
  }
  
  public void setComponent(String component) {
    this.component = component;
  }
  
  public void setRole(String role) {
    this.role = role;
  }
  
  public void setWorkDirectoryComponent(String workDirComponent) {
    this.workDirComponent = workDirComponent;
  }
  
  public String getWorkDirectoryComponent() {
    return workDirComponent;
  }
  
  public Signal getSignal() {
    return signal;
  }
  
  public void setSignal(Signal signal) {
    this.signal = signal;
  }
  
  public Command getCommand() {
    return command;
  }
  
  public void setCommand(Command command) {
    this.command = command;
  }
  
  public Command getCleanUpCommand() {
    return cleanUpCommand;
  }
  
  public void setCleanUpCommand(Command cleanUpCommand) {
    this.cleanUpCommand = cleanUpCommand;  
  }
  
  public long getClusterDefinitionRevision() {
    return this.clusterDefinitionRevision;
  }
  
  public void setClusterDefinitionRevision(long clusterDefinitionRevision) {
    this.clusterDefinitionRevision = clusterDefinitionRevision;
  }
  
  public ConfigFile getFile() {
    return this.file;
  }
  
  public void setFile(ConfigFile file) {
    this.file = file;
  }
  
  public static enum Kind {
    RUN_ACTION, START_ACTION, STOP_ACTION, STATUS_ACTION, 
    CREATE_STRUCTURE_ACTION, DELETE_STRUCTURE_ACTION, WRITE_FILE_ACTION,
    INSTALL_AND_CONFIG_ACTION;
    public static class KindAdaptor extends XmlAdapter<String, Kind> {
      @Override
      public String marshal(Kind obj) throws Exception {
        return obj.toString();
      }

      @Override
      public Kind unmarshal(String str) throws Exception {
        for (Kind j : Kind.class.getEnumConstants()) {
          if (j.toString().equals(str)) {
            return j;
          }
        }
        throw new Exception("Can't convert " + str + " to "
          + Kind.class.getName());
      }

    }
  }

  public static enum Signal {
    TERM, KILL;
    public static class SignalAdaptor extends XmlAdapter<String, Signal> {
      @Override
      public String marshal(Signal obj) throws Exception {
        return obj.toString();
      }

      @Override
      public Signal unmarshal(String str) throws Exception {
        for (Signal j : Signal.class.getEnumConstants()) {
          if (j.toString().equals(str)) {
            return j;
          }
        }
        throw new Exception("Can't convert " + str + " to "
          + Signal.class.getName());
      }

    }
  }
}
