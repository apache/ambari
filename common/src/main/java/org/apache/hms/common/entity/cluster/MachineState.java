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

package org.apache.hms.common.entity.cluster;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.hms.common.entity.RestSource;
import org.apache.hms.common.entity.Status;
import org.apache.hms.common.entity.Status.StatusAdapter;

/**
 * MachineState defines a list of state entries of a node.
 * For example, a node can have HADOOP-0.20.206 installed, and 
 * namenode is started.  Both states are stored in the MachineState.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class MachineState extends RestSource {
  
  Set<StateEntry> stateEntries;
  
  public Set<StateEntry> getStates() {
    return stateEntries;
  }
  
  public void setStates(Set<StateEntry> stateEntries) {
    this.stateEntries = stateEntries;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (stateEntries != null) {
      for (StateEntry a : stateEntries) {
        sb.append(a);
        sb.append(" ");
      }
    }
    return sb.toString();
  }
  
  /**
   * A state entry compose of:
   * 
   * Type of state to record, valid type are: PACKAGE, DAEMON
   * A unique name field to identify the package or daemon
   * Status is the state that a node must maintain.
   * @author eyang
   *
   */
  @XmlRootElement
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class StateEntry {
    private static final int PRIME = 16777619;
    
    @XmlElement
    @XmlJavaTypeAdapter(StateTypeAdapter.class)
    protected StateType type;
    @XmlElement
    protected String name;
    @XmlElement
    @XmlJavaTypeAdapter(StatusAdapter.class)
    protected Status status;
    
    public StateEntry(){
    }
    
    public StateEntry(StateType type, String name, Status status) {
      this.type = type;
      this.name = name;
      this.status = status;
    }
    
    public StateType getType() {
      return type;
    }
    
    public String getName() {
      return name;
    }
    
    public Status getStatus() {
      return status;
    }
    
    public void setType(StateType type) {
      this.type = type;
    }
    
    public void setName(String name) {
      this.name = name;
    }
    
    public void setStatus(Status status) {
      this.status = status;
    }
    
    static boolean isEqual(Object a, Object b) {
      return a == null ? b == null : a.equals(b);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof StateEntry) {
        StateEntry that = (StateEntry) obj;
        return this.type == that.type
            && isEqual(this.name, that.name);
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      int result = 1;
      result = PRIME * result + ((type == null) ? 0 : type.hashCode());
      result = PRIME * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("(");
      sb.append(type);
      sb.append(":");
      sb.append(name);
      sb.append(":");
      sb.append(status);
      sb.append(")");
      return sb.toString();
    }
  }
  
  /**
   * Type of state that is recorded per node.
   */
  @XmlRootElement
  public enum StateType {
    PACKAGE, DAEMON;
  }

  public static class StateTypeAdapter extends XmlAdapter<String, StateType> {

    @Override
    public String marshal(StateType obj) throws Exception {
      return obj.toString();
    }

    @Override
    public StateType unmarshal(String str) throws Exception {
      for (StateType j : StateType.class.getEnumConstants()) {
        if (j.toString().equals(str)) {
          return j;
        }
      }
      throw new Exception("Can't convert " + str + " to "
          + StateType.class.getName());
    }
  }
}

