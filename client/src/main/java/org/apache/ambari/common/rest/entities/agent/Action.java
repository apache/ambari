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
  public List<Command> commands;
  @XmlElement
  public List<Command> cleanUpCommands;
  @XmlElement
  public String bluePrintName;
  @XmlElement
  public String bluePrintRevision;
  
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
  
  public Signal getSignal() {
    return signal;
  }
  
  public void setSignal(Signal signal) {
    this.signal = signal;
  }
  
  public List<Command> getCommands() {
    return commands;
  }
  
  public void setCommands(List<Command> commands) {
    this.commands = commands;
  }
  
  public List<Command> getCleanUpCommands() {
    return cleanUpCommands;
  }
  
  public void setCleanUpCommands(List<Command> cleanUpCommands) {
    this.cleanUpCommands = cleanUpCommands;  
  }
  
  public String getBluePrintName() {
    return bluePrintName;
  }
  
  public void setBluePrintName(String bluePrintName) {
    this.bluePrintName = bluePrintName;
  }
  
  public String getBluePrintRevision() {
    return bluePrintRevision;
  }
  
  public void setBluePrintRevision(String bluePrintRevision) {
    this.bluePrintRevision = bluePrintRevision;
  }
  
  public static enum Kind {
    RUN_ACTION, START_ACTION, STOP_ACTION, STATUS_ACTION;
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
