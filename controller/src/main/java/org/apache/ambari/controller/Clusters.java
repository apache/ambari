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
package org.apache.ambari.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.ambari.common.rest.entities.Cluster;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.RoleToNodesMap;
import org.apache.ambari.common.rest.entities.RoleToNodesMapEntry;

public class Clusters {

	/*
	 * Operational clusters include both active and inactive clusters
	 */
    protected ConcurrentHashMap<String, ClusterDefinition> operational_clusters = new ConcurrentHashMap<String, ClusterDefinition>();
    
    /* 
     * Attic clusters are just definitions left around. When cluster is retired i.e. all its nodes 
     * are released then the cluster entry is transferred to attic and deleted from active 
     * clusters list.
     * Cluster in attic list should be submitted as new cluster, if needs to be reactivated. 
     */
    protected ConcurrentHashMap<String, ClusterDefinition> attic_clusters = new ConcurrentHashMap<String, ClusterDefinition>();
    
    private static Clusters ClustersTypeRef=null;
	
    private Clusters() {}
    
    public static synchronized Clusters getInstance() {
    	if(ClustersTypeRef == null) {
    		ClustersTypeRef = new Clusters();
    	}
    	return ClustersTypeRef;
    }

    public Object clone() throws CloneNotSupportedException {
    	throw new CloneNotSupportedException();
    }


    /* 
     * Add new Cluster to cluster list 
     * Validate the cluster definition
     * Lock the cluster list
     *   -- Check if cluster with given name already exits?
     *   -- Set the cluster state and timestamps 
     *   -- Reserve the nodes. i.e. add the cluster and role referenes to Node
     *   -- Throw exception, if some nodes are already preallocated to other cluster.
     *   -- Persist the cluster definition as revision 0 and list of node names against cluster & service:role 
     *   -- Background daemon should trigger the agent installation on the new nodes (UNREGISTERED), if not done already. 
     *      (daemon can keep track of which nodes agent is already installed or check it by ssh to nodes, if nodes added
     *       are in UNREGISTERED state).  
     */   
    public ClusterDefinition addCluster(ClusterDefinition c) throws Exception {
    	/*
    	 * TODO: Validate the cluster definition
    	 */
    	if (c.getName() == null ||  c.getName().equals("")) {
    		Exception e = new Exception("Cluster Name must be specified and must be non-empty string");
    		throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    	}
    	
    	synchronized (operational_clusters) {
    		/* 
    		 * Check if cluster already exists
    		 */
    		if (operational_clusters.containsKey(c.getName())) {
    			Exception e = new Exception("Cluster ["+c.getName()+"] already exists");
    			throw new WebApplicationException(e, Response.Status.CONFLICT);
    		}
    		
    		/*
    		 * Add new cluster to cluster list
    		 */
    		Date requestTime = new Date();
    		Cluster cls = new Cluster();
    		ClusterState clsState = new ClusterState();
    		clsState.setCreationTime(requestTime);
    		clsState.setLastUpdateTime(requestTime);
    		clsState.setDeployTime((Date)null);
    		clsState.setRepresentativeState(ClusterState.CLUSTER_STATE_INACTIVE);
    		
    		cls.setID(UUID.randomUUID().toString());
    		cls.setClusterDefinition(c);
    		cls.setClusterState(clsState);
    		
			/*
			 * Update cluster nodes reservation.
			 * TODO: REST API should allow roleToNodesMap separately based on the node attributes 
			 */
			if (c.getNodeRangeExpressions() != null) {
				updateClusterNodesReservation (cls, c.getNodeRangeExpressions());
			}
			
			/*
			 * Update the Node to Roles association if specified
			 */
			if (c.getRoleToNodesMap() != null) {
				updateNodeToRolesAssociation(c.getName(), c.getRoleToNodesMap());
			}
			
    		/*
    		 * TODO: Persist the cluster definition to data store as a initial version r0. 
    		 * 		 Persist reserved nodes against the cluster & service/role
    		 */
    			
    		// Add the cluster to list, when definition is persisted
    		operational_clusters.put(c.getName(), c);
    	}
    	return null;
    } 
    
    
    /*
     * Update the nodes associated with cluster
     */
    private synchronized void updateClusterNodesReservation (Cluster cls, List<String> nodeRangeExpressions) throws Exception {
    	
    	String cname = cls.getClusterDefinition().getName();
    	
    	/*
		 * Reserve the nodes as specified in the node range expressions
		 * -- throw exception if any nodes are pre-associated with other cluster
		 */
    	ConcurrentHashMap<String, Node> all_nodes = Nodes.getInstance().getNodes();
    	List<String> specified_node_range = new ArrayList<String>();
    	for (String nodeRangeExpression : nodeRangeExpressions) {
    		specified_node_range.addAll(getHostnamesFromRangeExpression(nodeRangeExpression));
    	}
    	List<String> nodes_currently_allocated = new ArrayList<String>();
    	for (Node n : Nodes.getInstance().getNodes().values()) {
    		if (n.getNodeState().getClusterName().equals(cls.getClusterDefinition().getName())) {
    			nodes_currently_allocated.add(n.getName());
    		}
    	}
    	
    	List<String> nodes_to_allocate = new ArrayList<String>(specified_node_range);
    	nodes_to_allocate.removeAll(nodes_currently_allocated);
    	List<String> nodes_to_deallocate = new ArrayList<String>(nodes_currently_allocated);
    	nodes_to_deallocate.removeAll(specified_node_range);
    	
		/*
		 * Check for any nodes that are allocated to other cluster
		 */
    	List<String> preallocatedhosts = new ArrayList<String>();
    	for (String n : nodes_to_allocate) {
    		if (all_nodes.containsKey(n) && all_nodes.get(n).getNodeState().getClusterName() != null) {
    			preallocatedhosts.add(n);
    		}
    	}
    	
    	/* 
		 * Throw exception, if some of the hosts are already allocated to other cluster
		 */
		if (!preallocatedhosts.isEmpty()) {
			/*
			 * TODO: Return invalid request code and return list of preallocated nodes as a part of
			 *       response element
			 */
			Exception e = new Exception("Some of the nodes specified for the cluster roles are allocated to other cluster: ["+preallocatedhosts+"]");
    		throw new WebApplicationException(e, Response.Status.CONFLICT);
		}
		
		/*
		 * Allocate nodes to given cluster
		 */
		
		for (String node_name : nodes_to_allocate) {
			if (all_nodes.containsKey(node_name)) { 
				// Set the cluster name in the node 
				synchronized (all_nodes.get(node_name)) {
					all_nodes.get(node_name).reserveNodeForCluster(cname, true);
				}	
			} else {
				Node node = new Node(node_name);
				/*
				 * TODO: Set agentInstalled = true, unless controller uses SSH to setup the agent
				 */
				node.reserveNodeForCluster(cname, true);
				Nodes.getInstance().getNodes().put(node_name, node);
			}
		}
		
		/*
		 * deallocate nodes from a given cluster
		 * TODO: Node agent would check its been deallocated from the cluster and then shutdown any role/servers running it
		 *       then 
		 */
		for (String node_name : nodes_to_deallocate) {
			if (all_nodes.containsKey(node_name)) {
				synchronized (all_nodes.get(node_name)) {
					all_nodes.get(node_name).releaseNodeFromCluster();
				}
			}
		}
		
    }
    
	private synchronized void updateNodeToRolesAssociation (String clusterName, RoleToNodesMap roleToNodesMap) throws Exception {
		/*
		 * Associate roles with node
		 */
		if (roleToNodesMap != null) {
			/*
			 * Generate node to roles hash map 
			 
			HashMap<String, List<String>> nodeToRolesHashMap = new HashMap<String, List<String>>();
			for (RoleToNodesMapEntryType e : roleToNodesMap.getRoleToNodesMapEntry()) {
				List<String> hosts = getHostnamesFromRangeExpression(e.getNodeRangeExpression());
				for (String host : hosts) {
					if (!nodeToRolesHashMap.containsKey(host)) {
						List<String> x = new ArrayList<String>();
						x.add(e.getServiceName()+":"+e.getRoleName());
						nodeToRolesHashMap.put(host, x);
					} else {
						nodeToRolesHashMap.get(host).add(e.getServiceName()+":"+e.getRoleName());
					}
				}
			} */
				
			/*
			 * Replace the roles list in for each node
			 
			HashMap<String, NodeType> all_nodes = NodesType.getInstance().getNodes();
			for (String host : nodeToRolesHashMap.keySet()) {
				if (all_nodes.containsKey(host) && all_nodes.get(host).getClusterName().equals(clusterName)) { 
					synchronized (all_nodes.get(host)) {
						all_nodes.get(host).setNodeRoles(nodeToRolesHashMap.get(host));
					}
				}
			} */
		}
	}
    
    /*
     * TODO: Implement proper range expression
     */
    public List<String> getHostnamesFromRangeExpression (String nodeRangeExpression) throws Exception {
  
    	List<String> list = new ArrayList<String>();
    	StringTokenizer st = new StringTokenizer(nodeRangeExpression);
    	while (st.hasMoreTokens()) {
    		list.add(st.nextToken());
    	}
    	return list;
    }
    
    /* 
     * Update cluster 
    */
    public void updateCluster(String clusterName, ClusterDefinition c) throws Exception {
    	/*
    	 * Update the cluster definition. 
    	 * Always latest version of cluster definition is kept in memory 
    	 * Revisions for cluster definition is mainly need for agents to know
    	 * that something is changed in cluster definition?
    	 * TODO: 
    	 * 		Make update atomic? i.e. persist the new revision first and then update the 
    	 * 		definition in memory? make sure get cluster does not get partial definition.. 
    	 */
    	int i;
    	ClusterDefinition cls = null;
    	for (i=0; i<operational_clusters.size(); i++) {
    		if (operational_clusters.get(i).equals(clusterName)) {
    			cls = operational_clusters.get(i);
    			break;
    		}
    	}
    	
    	// Throw exception if cluster is not found
    	if (i == operational_clusters.size()) {
    		throw new Exception("Specified cluster ["+clusterName+"] does not exists");
    	}
    	
    	synchronized (cls) {
    		if (c.getDescription() != null) cls.setDescription(c.getDescription());
    		// Update the last update time
    		//cls.setLastUpdateTime(new Date());
    		//cls.setRevision(cls.getRevision()+1);
    		/*
    		 * TODO: Persist the latest cluster definition under new revision
    		*/
    	}
    	return;
    }
    
    /*
     * Update role to Nodes Map
    
    private void updateClusterRoleToNodesMap (RoleToNodesMapType s, RoleToNodesMapType t) throws Exception {
    	for (RoleToNodesMapEntryType se : s.getRoleToNodesMapEntry()) {
    		for (RoleToNodesMapEntryType te : t.getRoleToNodesMapEntry()) {
    			if (se.getServiceName().equals(te.getServiceName()) && se.getRoleName().equals(te.getRoleName())) {
    				te.setNodeRangeExpression(se.getNodeRangeExpression());
    			} else {
    				t.getRoleToNodesMapEntry().add(se);
    			}
    		}
    	}
    } */
    
    /*
     * Delete ClusterType from the list
     * Delete operation will only remove the entry from the controller 
     * Cluster must be in ATTIC state to be deleted from controller
     
    public void deleteCluster(ClusterType c) throws Exception { 
    	synchronized (operational_clusters) {
    		for (int i=0;i<operational_clusters.size();i++) {
    			if (operational_clusters.get(i).getName().equals(c.getName())) {
    				synchronized (operational_clusters.get(i)) {
    					if (operational_clusters.get(i).getCurrentState().equals(ClusterState.CLUSTER_STATE_ATTIC)) {
    						// TODO: remove the persistent entry from data store
    						
    						 // Remove the entry from the in-memory clsuter list
    						 
    						operational_clusters.remove(i);
    					} else {
    						throw new Exception ("Cluster ["+operational_clusters.get(i).getName()+"] not in ATTIC state");
    					}
    				}
    			}
    		} 
    	}
    	return;
    } */  
    
    
    /*
     * Get Cluster definition snapshot (whenever it is to be serialized over wire)
     * TODO: CHECK_IT
     
    public ClusterType getClusterSnapshot(String clusterName) throws Exception {
    	for (ClusterType cls : operational_clusters) {
    		ClusterType cls1 = null;
			if (cls.getName().equals(clusterName)) {
				synchronized (cls) {
					//cls1 = cls.clone();
				}
			}
			return cls1;
		}
    	throw new Exception ("Cluster:["+clusterName+"] does not exists");
    } */
    
    /* 
     * Get the cluster definition 
    */
    public ClusterDefinition getCluster(String clusterName) throws Exception {
    	/*for (ClusterDefinition cls : operational_clusters) {
			if (cls.getName().equals(clusterName)) {
				return cls;
			}
		}*/
    	throw new Exception ("Cluster:["+clusterName+"] does not exists");
    }
    
    /*
     * Get the list of deployed clusters
     * TODO: return the synchronized snapshot of the deployed cluster list
     */
    public List<ClusterDefinition> getDeployedClusterList(String type) {
    	List<ClusterDefinition> list = new ArrayList<ClusterDefinition>();
    	if (type.equals("ALL")) {
    		//list.addAll(this.operational_clusters);
    		return list;
    	} else {
    		//for (ClusterDefinition cls : operational_clusters) {
    			//if (cls.gcurrentState.equals(type)) {
    			//	list.add(cls);
    			//}
    		//}
    	}
        return list;
    } 
    
    /*
     * Get the list of retired cluster definitions
     * TODO: return synchronized snapshot of the retired cluster list
     */
    public List<ClusterDefinition> getRetiredClusterList() {
    	List<ClusterDefinition> list = new ArrayList<ClusterDefinition>();
    	//list.addAll(this.attic_clusters);
        return list;
    }
    
    /*
     * Get the list of clusters
     * TODO: Get the synchronized snapshot of each cluster definition? 
     
    public List<ClusterType> getClusterList(String type) {
    	List<ClusterType> list = new ArrayList<ClusterType>();
    	if (type.equals("ALL")) {
    		list.addAll(getRetiredClusterList());
    		list.addAll(getDeployedClusterList("ALL"));
    	} else if (type.equals("ATTIC")) {
    		list.addAll(getRetiredClusterList());
    	} else {
    		for (ClusterType cls : operational_clusters ) {
    			if (cls.currentState.equals(type)) {
    				list.add(cls);
    			}
    		}
    	}
    	return list;
    } */
    
    /*
     * Change the cluster goal state
     * TODO: Change the argument goalState from string to enum
     * TODO: Use state machine to trigger the state change events  
     */
    public void changeClusterGoalState (String clusterName, String goalState, Date requestTime) throws Exception {
    	int i;
    	ClusterDefinition cls = null;
    	for (i=0; i<operational_clusters.size(); i++) {
    		if (operational_clusters.get(i).equals(clusterName)) {
    			cls = operational_clusters.get(i);
    			break;
    		}
    	}
    	
    	// Throw exception if cluster is not found
    	if (i == operational_clusters.size()) {
    		throw new Exception("Specified cluster ["+clusterName+"] does not exists");
    	}
    	
    	synchronized (cls) {
    		// Set the goal state
    		//cls.setLastRequestedGoalState(goalState);
			//cls.setTimeOflastRequestedGoalState(requestTime);
    		
    		/*
    		 * TODO: Persist the latest cluster definition under new revision
    		*/
    	}
    	
    	/*
    	 * send state change event to cluster state machine 
    	 */
    	//changeClusterState(this.getName());
    }
    
    /*
     * Cluster state change event Handler
     * 
	 * TODO: 
	 * 		 -- Make sure cluster definition is complete else throw exception 
	 * 			-- Check all ClusterType fields have valid values
	 * 			-- Check if roles have required number of nodes associated with it.
	 * 		 -- Add the nodes to NodesType list w/ name, cluster name, deployment state etc.
	 * 		 -- Trigger agent installation on the nodes, if not already done 
	 * 
	 * 		 -- Once agent is installed it should register itself w/ controller, get the 
	 * 			associated latest cluster definition and deploy the stack. If goal state is active
	 * 			it should get the associated services up. Once sync-ed, it should
	 * 			update its state w/ controller through heartbeat.
	 * 
	 *   	-- Once required number of service nodes are up cluster state should be changed accordingly
	*/
    public void stateChangeEventHandler (String currentState, String goalState) {
    	synchronized(this) {
    		/*
    		 * ATTIC to ACTIVE or INACTIVE
    		 */
			if (currentState.equals(ClusterState.CLUSTER_STATE_ATTIC) && goalState.equals(ClusterState.CLUSTER_STATE_ACTIVE)) {
				
			}
    	}
    }
    
    /* 
     * Util methods on entities
     */
	/*
	 * 
	 */
	public List<String> getAssociatedRoleNames(Node n) throws Exception {
		List<String> list = new ArrayList<String>();
		if (n.getNodeState().getClusterName() != null) {
			for (RoleToNodesMapEntry rnme : Clusters.getInstance().getCluster(n.getNodeState().getClusterName()).getRoleToNodesMap().getRoleToNodesMapEntry()) {
				list.add(rnme.getRoleName());
			}
		}
		return list;
	}
}
