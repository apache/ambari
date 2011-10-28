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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The information about each node.
 * 
 * <p>
 * The schema is
 * <pre>
 * element Node {
 *   element nodeAttributes {
 *     attribute cpuType { text }
 *     attribute cpuUnits { text }
 *     attribute cpuCores { text }
 *     attribute ramInGB { text }
 *     attribute diskSizeInGB { text }
 *     attribute diskUnits { text }
 *   }
 *   element nodeState {
 *     attribute lastHeartbeat { text }?
 *     attribute clusterName { text }?
 *     attribute agentInstalled { boolean }?
 *     attribute allocatedToCluster { boolean }?
 *     element nodeRoleNames { text }*
 *     element nodeServers {
 *       attribute name { text }
 *       attribute state { text }
 *       attribute lastStateUpdateTime { text }
 *     }*
 *   }
 * }
 * </pre>
 * </p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Node", propOrder = {
    "nodeAttributes",
    "nodeState"
})
@XmlRootElement(name = "Nodes")
public class Node {
    
    @XmlAttribute(required = true)
    protected String name;
    @XmlElement(name = "NodeAttributes")
    protected NodeAttributes nodeAttributes;
    @XmlElement(name = "NodeState", required = true)
    protected NodeState nodeState;
   
    public Node () {}
    
    public Node (String name) {
        this.name = name;
        this.nodeState = new NodeState();
    }
	
    /*
     * Marks the nodes associated w/ Cluster to be released
     */
    public void releaseNodeFromCluster() {
        /*
         * Cluster ID + NodeServers + NodeToRoleNames associated w/ cluster will be reset 
         * part of heartbeat when node stop services and does clean up.
         */
        this.nodeState.setAllocatedToCluster(false);
        this.getNodeState().setNodeServers(null);
        this.getNodeState().setNodeRoleNames(null);
    }
	
  	/*
  	 * Reserving node for cluster is done by associating cluster name w/ node
  	 */
  	public void reserveNodeForCluster (String clusterName, Boolean agentInstalled) {
  		this.getNodeState().setClusterName(clusterName);
  		this.getNodeState().setAgentInstalled(agentInstalled);
  		this.getNodeState().setAllocatedToCluster(true);
  	}
        
    /**
     * @return the name
     */
    public String getName() {
            return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
            this.name = name;
    }
    
    /**
     * @return the nodeMetrics
     */
    public NodeAttributes getNodeAttributes() {
            return nodeAttributes;
    }
    /**
     * @param nodeMetrics the nodeMetrics to set
     */
    public void setNodeAttributes(NodeAttributes nodeAttributes) {
            this.nodeAttributes = nodeAttributes;
    }
    /**
     * @return the nodeState
     */
    public NodeState getNodeState() {
            return nodeState;
    }
    
    /**
     * @param nodeState the nodeState to set
     */
    public void setNodeState(NodeState nodeState) {
            this.nodeState = nodeState;
    }
}
