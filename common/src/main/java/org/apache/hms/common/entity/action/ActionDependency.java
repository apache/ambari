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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.hms.common.entity.RestSource;
import org.apache.hms.common.entity.cluster.MachineState.StateEntry;
import org.apache.hms.common.entity.manifest.Role;

/**
 * Defines the list of states that a action depends on.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD) 
@XmlType(name="", propOrder = {})
public class ActionDependency extends RestSource {
    @XmlElement
    protected List<String> hosts;
    @XmlElement
    protected List<StateEntry> states;
    @XmlElement
    protected Set<String> roles;
    
    public ActionDependency(){
    }
    
    public ActionDependency(Set<String> roles, List<StateEntry> states) {
      this.roles = roles;
      this.states = states;
    }
    
    public ActionDependency(List<String> hosts, List<StateEntry> states) {
      this.hosts = hosts;
      this.states = states;
    }
    
    public List<String> getHosts() {
      return hosts;
    }
    
    public List<StateEntry> getStates() {
      return states;
    }
    
    public Set<String> getRoles() {
      return roles;
    }
    
    public void setHosts(List<String> hosts) {
      this.hosts = hosts;
    }
    
    public void setStates(List<StateEntry> states) {
      this.states = states;
    }
    
    public void setRoles(Set<String> roles) {
      this.roles = roles;
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[hosts={");
      if (hosts != null) {
        for(String a : hosts) {
          sb.append(a);
          sb.append(", ");
        }
      }
      sb.append("}, states={");
      if (states != null) {
        for(StateEntry a : states) {
          sb.append(a);
          sb.append(", ");
        }
      }
      sb.append("}]");
      return sb.toString();
    }
}

