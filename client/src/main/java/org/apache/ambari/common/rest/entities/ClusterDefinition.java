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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;



/**
 * Definition of a cluster.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ClusterDefinition", propOrder = {
    "enabledServices",
    "roleToNodesMap"
})
@XmlRootElement(name = "cluster")
public class ClusterDefinition {
        
    public static final String GOAL_STATE_ACTIVE = "ACTIVE";
    public static final String GOAL_STATE_INACTIVE = "INACTIVE";
    public static final String GOAL_STATE_ATTIC = "ATTIC";
   
    /**
     * The name of the cluster.
     */
    @XmlAttribute
    protected String name = null;
    
    /**
     * Every cluster update creates a new revision and returned through this field. 
     * This field can be optionally be set durint the update to latest revision 
     * (currently checked out revision) of the cluster being updated and if so,
     * Ambari will prevent the update, if the latest revision of the cluster changed 
     * in the background before update. If not specified update will over-write current
     * latest revision.
     */
    @XmlAttribute
    protected String revision = null;
  
    /**
     * A user-facing comment about the cluster about what it is intended for.
     */
    @XmlAttribute
    protected String description = null;
    
    /**
     * The name of the stack that defines the cluster.
     */
    @XmlAttribute
    protected String stackName = null;
    
    /**
     * The revision of the stack that this cluster is based on.
     */
    @XmlAttribute
    protected String stackRevision = null;
    
    /**
     * The goal state of the cluster. Valid states are:
     * ACTIVE - deploy and start the cluster
     * INACTIVE - the cluster should be stopped, but the nodes reserved
     * ATTIC - the cluster's nodes should be released
     */
    @XmlAttribute
    protected String goalState = null;
    
    /**
     * The list of components that should be running if the cluster is ACTIVE.
     */
    @XmlElement
    protected List<String> enabledServices = null;
    
    /**
     * A node expression giving the entire set of nodes for this cluster.
     */
    @XmlAttribute
    protected String nodes = null;

    /**
     * A map from roles to the nodes associated with each role.
     */
    @XmlElement
    protected List<RoleToNodes> roleToNodesMap = null;
    

    /**
     * @return the roleToNodesMap
     */
    public List<RoleToNodes> getRoleToNodesMap() {
        return roleToNodesMap;
    }

    /**
     * @param roleToNodesMap the roleToNodesMap to set
     */
    public void setRoleToNodesMap(List<RoleToNodes> roleToNodesMap) {
        this.roleToNodesMap = roleToNodesMap;
    }

    /**
     * @return the stackRevision
     */
    public String getStackRevision() {
        return stackRevision;
    }

    /**
     * @param stackRevision the stackRevision to set
     */
    public void setStackRevision(String stackRevision) {
        this.stackRevision = stackRevision;
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
     * @return the description
     */
    public String getDescription() {
            return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
            this.description = description;
    }

    /**
     * @return the stackName
     */
    public String getStackName() {
            return stackName;
    }

    /**
     * @param stackName the stackName to set
     */
    public void setStackName(String stackName) {
            this.stackName = stackName;
    }

    /**
     * @return the goalState
     */
    public String getGoalState() {
            return goalState;
    }

    /**
     * @param goalState the goalState to set
     */
    public void setGoalState(String goalState) {
            this.goalState = goalState;
    }

    /**
     * @return the enabledServices
     */
    public List<String> getEnabledServices() {
            return enabledServices;
    }

    /**
     * @param enabledServices the enabledServices to set
     */
    public void setEnabledServices(List<String> enabledServices) {
            this.enabledServices = enabledServices;
    }

    /**
     * @return the nodeRangeExpressions
     */
    public String getNodes() {
            return nodes;
    }

    /**
     * @param nodeRangeExpressions the nodeRangeExpressions to set
     */
    public void setNodes(String nodeRangeExpressions) {
            this.nodes = nodeRangeExpressions;
    }
    
    
    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * @param revision the revision to set
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

}
