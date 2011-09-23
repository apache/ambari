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
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NodeState", propOrder = {
    "lastHeartbeatTime",
    "clusterName",
    "AllocatedToCluster",
    "agentInstalled",
	"nodeServers"
})
public class NodeState {

	@XmlElement(name = "lastHeartbeatTime", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastHeartbeatTime;
	
	/*
     * Associating the cluster name would reserve the node for a given cluster
     * 
     */
	@XmlElement(name = "ClusterName", required = true)
    protected String clusterName;

	@XmlElement(name = "AgentInstalled", required = true)
    protected Boolean agentInstalled = true;

	@XmlElement(name = "AllocatedToCluster", required = true)
    protected Boolean allocatedToCluster = false;
	
	@XmlElement(name = "NodeServers", required = true)
    protected List<NodeServer> nodeServers = new ArrayList<NodeServer>();

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
}
