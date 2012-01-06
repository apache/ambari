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
package org.apache.ambari.common.rest.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.ambari.common.rest.agent.CommandResult;

/**
 * Information about the Nodes.
  */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NodeState", propOrder = {
    "nodeRoles",
    "failedCommandStdouts",
    "failedCommandStderrs"
})
@XmlRootElement
public class NodeState {

    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastHeartbeatTime;
        
    /*
     * Associating the cluster name would reserve the node for a given cluster
     * 
     */
    @XmlAttribute
    protected String clusterName;
    
    @XmlAttribute
    protected Boolean agentInstalled = true;

    @XmlAttribute
    protected Boolean allocatedToCluster = false;
    
    @XmlAttribute
    protected Boolean health = NodeState.HEALTHY;
        
    /*
     * null indicates no roles associated with this node.
     */
    @XmlElement
    protected List<NodeRole> nodeRoles = null;
        
    @XmlElement
    protected List<String> failedCommandStdouts = null;
    
    @XmlElement
    protected List<String> failedCommandStderrs = null;
    
    public static final boolean HEALTHY = true;
    public static final boolean UNHEALTHY = false;

    /**
     * Get Node Roles names 
     */
    public List<String> getNodeRoleNames (String activeState) {
        if (this.getNodeRoles() == null) return null;
        List<String> rolenames = new ArrayList<String>();
        for (NodeRole x : this.getNodeRoles()) {
            if(activeState == null || activeState.equals("")) {
                rolenames.add(x.getName()); continue;
            }
            if (activeState.equals(NodeRole.NODE_SERVER_STATE_DOWN) && x.getState().equals(NodeRole.NODE_SERVER_STATE_DOWN)) {
                rolenames.add(x.getName()); continue;
            }
            if (activeState.equals(NodeRole.NODE_SERVER_STATE_UP) && x.getState().equals(NodeRole.NODE_SERVER_STATE_UP)) {
                rolenames.add(x.getName()); continue;
            }
        }
        return rolenames;
    }
    
    /**
     * Update role name. Add if it does not exists in the list
     */
    public void updateRoleState(NodeRole role) {
        if (this.getNodeRoles() == null) {
            this.setNodeRoles(new ArrayList<NodeRole>());
        }
        int i = 0;
        for (i=0; i<this.getNodeRoles().size(); i++) {
            if (this.getNodeRoles().get(i).getName().equals(role.getName())) {
                this.getNodeRoles().remove(i);
                this.getNodeRoles().add(i, role);
                return;
            }
        }
        if (i == this.getNodeRoles().size()) {
            this.getNodeRoles().add(role);
        }
    }
    
    /**
     * @return the nodeRoles
     */
    public List<NodeRole> getNodeRoles() {
        return nodeRoles;
    }

    /**
     * @param nodeRoles the nodeRoles to set
     */
    public void setNodeRoles(List<NodeRole> nodeRoles) {
        this.nodeRoles = nodeRoles;
    }
    
    /**
     * @return the clusterName
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @param clusterName the clusterName to set
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }


    /**
     * @return the allocatedToCluster
     */
    public Boolean getAllocatedToCluster() {
            return allocatedToCluster;
    }

    /**
     * @param allocatedToCluster the allocatedToCluster to set
     */
    public void setAllocatedToCluster(Boolean allocatedToCluster) {
            this.allocatedToCluster = allocatedToCluster;
    }
    
    /**
     * @return the agentInstalled
     */
    public Boolean getAgentInstalled() {
            return agentInstalled;
    }

    /**
     * @param agentInstalled the agentInstalled to set
     */
    public void setAgentInstalled(Boolean agentInstalled) {
            this.agentInstalled = agentInstalled;
    }
   
    /**
     * @return the health
     */
    public Boolean getHealth() {
            return health;
    }
    
    /**
     * @param health (true for healthy)
     */
    public void setHealth(Boolean health) {
            this.health = health;
    }
    
    /**
     * @param results list of results that failed
     */
    public void setFailedCommandResults(List<CommandResult> results) {
      if (results == null || results.size() == 0) {
        this.failedCommandStderrs = null;
        this.failedCommandStdouts = null;
        return;
      }
      for (CommandResult r : results) {
        if (r.getError() != null) {
          if (this.failedCommandStderrs == null) {
            this.failedCommandStderrs = new ArrayList<String>();
          }
          this.failedCommandStderrs.add(r.getError());
        }
        if (r.getOutput() != null) {
          if (this.failedCommandStdouts == null) {
            this.failedCommandStdouts = new ArrayList<String>();
          }
          this.failedCommandStdouts.add(r.getOutput());
        }
      }
    }
    
    /**
     * @return the stdouts of failed commands
     */
    public List<String> getFailedCommandStdouts() {
      return failedCommandStdouts;
    }
    
    /*
     * @return the stderrs of failed commands
     */
    public List<String> getFailedCommandStderrs() {
      return failedCommandStderrs;
    }
    
    /**
     * @return the lastHeartbeatTime
     */
    public XMLGregorianCalendar getLastHeartbeatTime() {
            return lastHeartbeatTime;
    }

    /**
     * @param lastHeartbeatTime the lastHeartbeatTime to set
     */
    public void setLastHeartbeatTime(XMLGregorianCalendar lastHeartbeatTime) {
            this.lastHeartbeatTime = lastHeartbeatTime;
    }

    /**
     * @param lastHeartbeatTime the lastHeartbeatTime to set
     */
    public void setLastHeartbeatTime(Date lastHeartbeatTime) throws Exception {
        if (lastHeartbeatTime == null) {
            this.lastHeartbeatTime = null;
        } else {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(lastHeartbeatTime);
            this.lastHeartbeatTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        }
    }

}
