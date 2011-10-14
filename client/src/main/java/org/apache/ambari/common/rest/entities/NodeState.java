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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NodeState", propOrder = {
    "clusterID",
    "allocatedToCluster",
    "lastHeartbeatTime",
    "agentInstalled",
    "nodeRoleNames",
    "nodeServers"
})
@XmlRootElement
public class NodeState {

    @XmlElement
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastHeartbeatTime;
        
    /*
     * Associating the cluster name would reserve the node for a given cluster
     * 
     */
    @XmlElement
    protected String clusterID;

    @XmlElement
    protected Boolean agentInstalled = true;

    @XmlElement
    protected Boolean allocatedToCluster = false;
        
    /*
     * null indicates no roles associated with this node.
     */
    @XmlElement
    protected List<String> nodeRoleNames = null;
        
    @XmlElement
    protected List<NodeServer> nodeServers = new ArrayList<NodeServer>();

    /**
     * @return the nodeRoleNames
     */
    public List<String> getNodeRoleNames() {
      return nodeRoleNames;
    }

    /**
     * @param nodeRoleNames the nodeRoleNames to set
     */
    public void setNodeRoleNames(List<String> nodeRoleNames) {
      this.nodeRoleNames = nodeRoleNames;
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
    
    /**
     * @return the nodeServers
     */
    public List<NodeServer> getNodeServers() {
            return nodeServers;
    }

    /**
     * @param nodeServers the nodeServers to set
     */
    public void setNodeServers(List<NodeServer> nodeServers) {
            this.nodeServers = nodeServers;
    }
    
    /**
     * @return the clusterID
     */
    public String getClusterID() {
        return clusterID;
    }

    /**
     * @param clusterID the clusterID to set
     */
    public void setClusterID(String clusterID) {
        this.clusterID = clusterID;
    }

}
