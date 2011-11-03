/**
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
package org.apache.ambari.controller.rest.config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterInformation;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.NodeAttributes;
import org.apache.ambari.common.rest.entities.NodeState;
import org.apache.ambari.common.rest.entities.RoleToNodes;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.controller.Util;
import org.apache.ambari.common.rest.entities.StackInformation;

public class Examples {
	public static final ClusterInformation CLUSTER_INFORMATION = new ClusterInformation();
	public static final ClusterDefinition CLUSTER_DEFINITION = new ClusterDefinition();
	public static final ClusterState CLUSTER_STATE = new ClusterState();
    public static final List<String> activeServices = new ArrayList<String>();
    public static final List<RoleToNodes> rnm = new ArrayList<RoleToNodes>();
    public static final List<Node> NODES = new ArrayList<Node>();
    public static final Stack STACK = new Stack();
    public static final StackInformation STACK_INFORMATION = new StackInformation();
    public static final Node NODE = new Node();
	
	static {
		CLUSTER_DEFINITION.setName("example-name");
        CLUSTER_DEFINITION.setName("blue.dev.Cluster123");
        CLUSTER_DEFINITION.setStackName("cluster123");
        CLUSTER_DEFINITION.setStackRevision("0");
        CLUSTER_DEFINITION.setDescription("cluster123 - development cluster");
        CLUSTER_DEFINITION.setGoalState(ClusterState.CLUSTER_STATE_ATTIC);
        activeServices.add("hdfs");
        activeServices.add("mapred");
        CLUSTER_DEFINITION.setActiveServices(activeServices);
        
        String nodes = "jt-nodex,nn-nodex,hostname-1x,hostname-2x,hostname-3x,"+
                       "hostname-4x,node-2x,node-3x,node-4x";  
        CLUSTER_DEFINITION.setNodes(nodes);
		CLUSTER_INFORMATION.setDefinition(CLUSTER_DEFINITION);
        
		RoleToNodes rnme = new RoleToNodes();
        rnme.setRoleName("jobtracker-role");
        rnme.setNodes("jt-nodex");
        rnm.add(rnme);
        
        rnme = new RoleToNodes();
        rnme.setRoleName("namenode-role");
        rnme.setNodes("nn-nodex");
        rnm.add(rnme);
        
        rnme = new RoleToNodes();
        rnme.setRoleName("slaves-role");
        rnme.setNodes("hostname-1x,hostname-2x,hostname-3x,"+
                       "hostname-4x,node-2x,node-3x,node-4x");
        rnm.add(rnme);
        CLUSTER_DEFINITION.setRoleToNodesMap(rnm);
        
        CLUSTER_STATE.setState("ATTIC");
        try {
			CLUSTER_STATE.setCreationTime(Util.getXMLGregorianCalendar(new Date()));
			CLUSTER_STATE.setDeployTime(Util.getXMLGregorianCalendar(new Date()));
		} catch (Exception e) {
		}
        NODE.setName("localhost");
        NodeAttributes nodeAttributes = new NodeAttributes();
        nodeAttributes.setCPUCores((short)1);
        nodeAttributes.setDISKUnits((short)4);
        nodeAttributes.setRAMInGB(6);
        NODE.setNodeAttributes(nodeAttributes);
        NodeState nodeState = new NodeState();
        nodeState.setClusterName("cluster-123");
        List<String> roleNames = new ArrayList<String>();
        roleNames.add("jobtracker-role");
        roleNames.add("namenode-role");
        nodeState.setNodeRoleNames(roleNames);
        NODE.setNodeState(nodeState);
        NODES.add(NODE);
        
        STACK.setName("stack");
        STACK.setRevision("1");
        STACK_INFORMATION.setName("HDP");
        STACK_INFORMATION.setRevision("1");
        List<String> components = new ArrayList<String>();
        components.add("hdfs");
        components.add("mapreduce");
        STACK_INFORMATION.setComponent(components);
	}
}
