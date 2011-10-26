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
 * <p>Java class for ClusterType complex type.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ClusterDefinition", propOrder = {
    "activeServices",
    "roleToNodesMap"
})
@XmlRootElement(name = "Cluster")
public class ClusterDefinition {
        
    public static final String GOAL_STATE_ACTIVE = "ACTIVE";
    public static final String GOAL_STATE_INACTIVE = "INACTIVE";
    public static final String GOAL_STATE_ATTIC = "ATTIC";
   
    @XmlAttribute
    protected String name = null;
    @XmlAttribute
    protected String description = null;
    @XmlAttribute
    protected String stackName = null;
    @XmlAttribute
    protected String stackRevision = null;
    @XmlAttribute
    protected String goalState = null;
    @XmlElement
    protected List<String> activeServices = null;
    @XmlAttribute
    protected String nodes = null;

    @XmlElement
    protected List<RoleToNodes> roleToNodesMap = null;
    
    
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
     * @return the activeServices
     */
    public List<String> getActiveServices() {
            return activeServices;
    }

    /**
     * @param activeServices the active3Services to set
     */
    public void setActiveServices(List<String> activeServices) {
            this.activeServices = activeServices;
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
     * @return the roleToNodesMap
     */
    public List<RoleToNodes> getRoleToNodes() {
            return roleToNodesMap;
    }

    /**
     * @param roleToNodesMap the roleToNodesMap to set
     */
    public void setRoleToNodesMap(List<RoleToNodes> roleToNodesMap) {
            this.roleToNodesMap = roleToNodesMap;
    }
}
