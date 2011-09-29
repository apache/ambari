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
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
    protected ConcurrentHashMap<String, Cluster> operational_clusters = new ConcurrentHashMap<String, Cluster>();
    
    /*
     * Operational clusters name to ID map
     */
    protected ConcurrentHashMap<String, String> operational_clusters_id_to_name = new ConcurrentHashMap<String, String>();
    
    /*
     * List of clusters to be deleted when they get into ATTIC state 
     * When cluster state is switched to ATTIC, if it is present in the tobe_deleted_list, 
     * it should be deleted from the persistence list and inmemory list. 
     */
    ConcurrentHashMap<String, String> tobe_deleted_clusters = new ConcurrentHashMap<String, String>();
    
    private static Clusters ClustersTypeRef=null;
        
    private Clusters() {
        /*
         * Cluster definition 
         */
        ClusterDefinition cluster123 = new ClusterDefinition();
        
        cluster123.setName("blue.dev.Cluster123");
        cluster123.setBlueprintName("cluster123-blueprint");
        cluster123.setDescription("cluster123 - development cluster");
        cluster123.setGoalState(ClusterState.CLUSTER_STATE_ACTIVE);
        List<String> activeServices = new ArrayList<String>();
        activeServices.add("hdfs");
        activeServices.add("mapred");
        cluster123.setActiveServices(activeServices);
        
        List<String> nodeRangeExpressions = new ArrayList<String>();
        String nodes1 = "jt-nodex";
        String nodes2 = "nn-nodex";
        String nodes3 = "hostname-1x hostname-2x hostname-3x hostname-4x";
        String nodes4 = "node-2x node-3x node-4x";  
        nodeRangeExpressions.add(nodes1);
        nodeRangeExpressions.add(nodes2);
        nodeRangeExpressions.add(nodes3);
        nodeRangeExpressions.add(nodes4);    
        cluster123.setNodeRangeExpressions(nodeRangeExpressions);
        
        RoleToNodesMap rnm = new RoleToNodesMap();
        
        RoleToNodesMapEntry rnme = new RoleToNodesMapEntry();
        rnme.setRoleName("jobtracker-role");
        nodeRangeExpressions = new ArrayList<String>();
        nodeRangeExpressions.add(nodes1);
        rnme.setNodeRangeExpressions(nodeRangeExpressions);
        rnm.getRoleToNodesMapEntry().add(rnme);
        
        rnme = new RoleToNodesMapEntry();
        rnme.setRoleName("namenode-role");
        nodeRangeExpressions = new ArrayList<String>();
        nodeRangeExpressions.add(nodes2);
        rnme.setNodeRangeExpressions(nodeRangeExpressions);
        rnm.getRoleToNodesMapEntry().add(rnme);
        
        rnme = new RoleToNodesMapEntry();
        rnme.setRoleName("slaves-role");
        nodeRangeExpressions = new ArrayList<String>();
        nodeRangeExpressions.add(nodes3);
        nodeRangeExpressions.add(nodes4);
        rnme.setNodeRangeExpressions(nodeRangeExpressions);
        rnm.getRoleToNodesMapEntry().add(rnme);
        
        cluster123.setRoleToNodesMap(rnm);
        
        /*
         * Cluster definition 
         */
        ClusterDefinition cluster124 = new ClusterDefinition();
        cluster124.setName("blue.research.Cluster124");
        cluster124.setBlueprintName("cluster124-blueprint");
        cluster124.setDescription("cluster124 - research cluster");
        cluster124.setGoalState(ClusterState.CLUSTER_STATE_INACTIVE);
        activeServices = new ArrayList<String>();
        activeServices.add("hdfs");
        activeServices.add("mapred");
        cluster124.setActiveServices(activeServices);
        
        nodeRangeExpressions = new ArrayList<String>();
        nodes1 = "jt-node";
        nodes2 = "nn-node";
        nodes3 = "hostname-1 hostname-2 hostname-3 hostname-4";
        nodes4 = "node-2 node-3 node-4";  
        nodeRangeExpressions.add(nodes1);
        nodeRangeExpressions.add(nodes2);
        nodeRangeExpressions.add(nodes3);
        nodeRangeExpressions.add(nodes4);    
        cluster124.setNodeRangeExpressions(nodeRangeExpressions);
        
        rnm = new RoleToNodesMap();
        
        rnme = new RoleToNodesMapEntry();
        rnme.setRoleName("jobtracker-role");
        nodeRangeExpressions = new ArrayList<String>();
        nodeRangeExpressions.add(nodes1);
        rnme.setNodeRangeExpressions(nodeRangeExpressions);
        rnm.getRoleToNodesMapEntry().add(rnme);
        
        rnme = new RoleToNodesMapEntry();
        rnme.setRoleName("namenode-role");
        nodeRangeExpressions = new ArrayList<String>();
        nodeRangeExpressions.add(nodes2);
        rnme.setNodeRangeExpressions(nodeRangeExpressions);
        rnm.getRoleToNodesMapEntry().add(rnme);
        
        rnme = new RoleToNodesMapEntry();
        rnme.setRoleName("slaves-role");
        nodeRangeExpressions = new ArrayList<String>();
        nodeRangeExpressions.add(nodes3);
        nodeRangeExpressions.add(nodes4);
        rnme.setNodeRangeExpressions(nodeRangeExpressions);
        rnm.getRoleToNodesMapEntry().add(rnme);
        
        cluster124.setRoleToNodesMap(rnm);
        
        try {
            addCluster(cluster123);
            addCluster(cluster124);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
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
    		String msg = "Cluster Name must be specified and must be non-empty string";
    		throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
    	}
    	
    	synchronized (operational_clusters) {
    		/* 
    		 * Check if cluster already exists
    		 */
    		if (operational_clusters.containsKey(c.getName())) {
    			String msg = "Cluster ["+c.getName()+"] already exists";
    			throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.CONFLICT)).get());
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
    		clsState.setState(ClusterState.CLUSTER_STATE_INACTIVE);
    		
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
			 * Create map of <Cluster - nodeToRolesMap>
			 * If role is not explicitly associated w/ any node then assign it w/ default role
			 * If RoleToNodes map is not specified then derive it based on the node attributes 
			 *  
			 */
			if (c.getRoleToNodesMap() != null) {
				updateNodeToRolesAssociation(c, c.getRoleToNodesMap());
			} else {
				/*
				 * TODO: Derive the role to nodes map based on nodes attributes
				 * then populate the node to roles association.
				 */
			}
			
    		/*
    		 * TODO: Persist the cluster definition to data store as a initial version r0. 
    		 * 		 Persist reserved nodes against the cluster & service/role
    		 */
    			
    		// Add the cluster to the list, when definition is persisted
    		this.operational_clusters.put(c.getName(), cls);
    		this.operational_clusters_id_to_name.put(cls.getID(), c.getName());
    	}
    	return c;
    } 
    
    
    /*
     * Update the nodes associated with cluster
     */
    private synchronized void updateClusterNodesReservation (Cluster cls, List<String> nodeRangeExpressions) throws Exception {
    	
    	String cname = cls.getClusterDefinition().getName();
    	
    	/*
    	 * Check if all the nodes explicitly specified in the RoleToNodesMap belong the cluster node range specified 
    	 */
    	ConcurrentHashMap<String, Node> all_nodes = Nodes.getInstance().getNodes();
        List<String> cluster_node_range = new ArrayList<String>();
        cluster_node_range.addAll(getHostnamesFromRangeExpressions(nodeRangeExpressions));
        
        List<String> nodes_specified_using_role_association = new ArrayList<String>();
        for (RoleToNodesMapEntry e : cls.getClusterDefinition().getRoleToNodesMap().getRoleToNodesMapEntry()) {
            List<String> hosts = getHostnamesFromRangeExpressions(e.getNodeRangeExpressions());
            nodes_specified_using_role_association.addAll(hosts);
            // TODO: Remove any duplicate nodes from nodes_specified_using_role_association
        }
        
        nodes_specified_using_role_association.removeAll(cluster_node_range);
        if (!nodes_specified_using_role_association.isEmpty()) {
            String msg = "Some nodes explicityly associated with roles using RoleToNodesMap do not belong in the " +
            		     "golbal node range specified for the cluster : ["+nodes_specified_using_role_association+"]";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
    	/*
		 * Reserve the nodes as specified in the node range expressions
		 * -- throw exception if any nodes are pre-associated with other cluster
		 */	
    	List<String> nodes_currently_allocated_to_cluster = new ArrayList<String>();
    	for (Node n : Nodes.getInstance().getNodes().values()) {
    		if (n.getNodeState().getClusterName().equals(cls.getClusterDefinition().getName())) {
    			nodes_currently_allocated_to_cluster.add(n.getName());
    		}
    	}
    	
    	List<String> nodes_to_allocate = new ArrayList<String>(cluster_node_range);
    	nodes_to_allocate.removeAll(nodes_currently_allocated_to_cluster);
    	List<String> nodes_to_deallocate = new ArrayList<String>(nodes_currently_allocated_to_cluster);
    	nodes_to_deallocate.removeAll(cluster_node_range);
    	
		/*
		 * Check for any nodes that are allocated to other cluster
		 */
    	List<String> preallocatedhosts = new ArrayList<String>();
    	for (String n : nodes_to_allocate) {
    		if (all_nodes.containsKey(n) && 
    				(all_nodes.get(n).getNodeState().getClusterName() != null || 
    				 all_nodes.get(n).getNodeState().getAllocatedToCluster()
    				)
    			) {
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
			String msg = "Some of the nodes specified for the cluster roles are allocated to other cluster: ["+preallocatedhosts+"]";
    		throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.CONFLICT)).get());
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
		 * TODO: Node agent would asynchronously clean up the node and notify it through heartbeat which 
		 * would set the allocatedtoCluster flag false
		 */
		for (String node_name : nodes_to_deallocate) {
			if (all_nodes.containsKey(node_name)) {
				synchronized (all_nodes.get(node_name)) {
					all_nodes.get(node_name).releaseNodeFromCluster();
				}
			}
		}
    }

	private synchronized void updateNodeToRolesAssociation (ClusterDefinition c, RoleToNodesMap roleToNodesMap) throws Exception {
		/*
		 * Associate roles list with node
		 */
		if (roleToNodesMap == null) {
			return;
		}
		
		/*
		 * Add list of roles to Node
		 * If node is not explicitly associated with any role then assign it w/ default role
		 */
		for (RoleToNodesMapEntry e : roleToNodesMap.getRoleToNodesMapEntry()) {
			List<String> hosts = getHostnamesFromRangeExpressions(e.getNodeRangeExpressions());
			for (String host : hosts) {
			  if (Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames() == null) {
			    Nodes.getInstance().getNodes().get(host).getNodeState().setNodeRoleNames((new ArrayList<String>()));
			  } 
			  Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames().add(e.getRoleName());
			}
		}
		
		
		/*
		 * Get the list of specified global node list for the cluster and any nodes not explicitly specified in the
		 * role to nodes map, assign them with default role defined in cluster blueprint
		 */
		List<String> specified_node_range = new ArrayList<String>();
    	specified_node_range.addAll(getHostnamesFromRangeExpressions(c.getNodeRangeExpressions()));
    	for (String host : specified_node_range) {
    	  if (Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames() == null) {
          Nodes.getInstance().getNodes().get(host).getNodeState().setNodeRoleNames((new ArrayList<String>()));
          String clusterName = Nodes.getInstance().getNodes().get(host).getNodeState().getClusterName();
          Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames().add(getDefaultRoleName(clusterName));
        } 
        
    	}
	}

    /* 
     * Update cluster definition
     * Always latest version is kept in memory
     * TODO: Need versions to be preserved in memory w/ link to cluster before update? 
    */
    public ClusterDefinition updateCluster(String clusterName, ClusterDefinition c) throws Exception {
        
        /*
         * Validate cluster definition
         */
        if (c.getName() == null || c.getName().equals("") || !c.getName().equals(clusterName)) {
            String msg = "Cluster name in resource URI ["+clusterName+"] does not match with one specified in update request element";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        /*
         * Check if cluster already exists
         */
        if (!this.operational_clusters.containsKey(clusterName)) {
            String msg = "Cluster ["+clusterName+"] does not exits";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        
        Cluster cls = this.operational_clusters.get(clusterName);
        synchronized (cls.getClusterDefinition()) {
            if (c.getBlueprintName() != null) cls.getClusterDefinition().setBlueprintName(c.getBlueprintName());
            if (c.getDescription() != null) cls.getClusterDefinition().setDescription(c.getDescription());
            if (c.getGoalState() != null) cls.getClusterDefinition().setGoalState(c.getGoalState());
            if (c.getActiveServices() != null) cls.getClusterDefinition().setActiveServices(c.getActiveServices());
            if (c.getNodeRangeExpressions() != null) {
                cls.getClusterDefinition().setNodeRangeExpressions(c.getNodeRangeExpressions());
                updateClusterNodesReservation (cls, c.getNodeRangeExpressions());
            }
            if (c.getRoleToNodesMap() != null) {
                cls.getClusterDefinition().setRoleToNodesMap(c.getRoleToNodesMap());
                updateNodeToRolesAssociation(cls.getClusterDefinition(), c.getRoleToNodesMap());
            }  
            
            /*
             *  Update the last update time & revision
             */
            cls.getClusterState().setLastUpdateTime(new Date());
            cls.setRevision(cls.getRevision()+1);
            
            /*
             * TODO: Persist the latest cluster definition under new revision
             */
        }
        return cls.getClusterDefinition();
    }
    
    /*
     * Delete Cluster 
     * Delete operation will bring the clsuter to ATTIC state and then remove the
     * cluster definition from the controller 
     * When cluster state transitions to ATTIC, it should check if the cluster definition is 
     * part of tobe_deleted_clusters map and then deleted the definition.
     */
    public void deleteCluster(String clusterName) throws Exception { 
        synchronized (operational_clusters) {
            for (Cluster cls : operational_clusters.values()) {
                if (cls.getClusterDefinition().getName().equals(clusterName)) {
                    synchronized (cls) {
                        cls.getClusterDefinition().setGoalState(ClusterState.CLUSTER_STATE_ATTIC);
                        this.tobe_deleted_clusters.put(clusterName, null);                    
                    }
                } 
            }
        }
    }   
     
    /* 
     * Get the cluster by name
     */
    public Cluster getClusterByName(String clusterName) {
        return this.operational_clusters.get(clusterName);
    }
    
    /* 
     * Get the cluster by ID
     */
    public Cluster getClusterByID(String clusterID) {
        String clusterName = this.operational_clusters_id_to_name.get(clusterID);
        if (clusterName != null) {
            return this.getClusterByName(clusterName);
        } else {
            return null;
        }
    }
    
    /* 
     * Get the cluster definition by name
     */
    public ClusterDefinition getClusterDefinition(String clusterName) throws Exception  {
        if (!this.operational_clusters.containsKey(clusterName)) {
            String msg = "Cluster ["+clusterName+"] does not exits";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        return this.operational_clusters.get(clusterName).getClusterDefinition();
    }
    
    
    /* 
     * Get the cluster state
    */
    public ClusterState getClusterState(String clusterName) throws WebApplicationException {
        if (!this.operational_clusters.containsKey(clusterName)) {
            String msg = "Cluster ["+clusterName+"] does not exits";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        return this.operational_clusters.get(clusterName).getClusterState();
    }
    
    
    /*
     * Get Cluster definition list
     */
    public List<ClusterDefinition> getClusterDefinitionsList(String state) {
      List<ClusterDefinition> list = new ArrayList<ClusterDefinition>();
      for (Cluster cls : this.operational_clusters.values()) {
        if (state.equals("ALL")) {
          list.add(cls.getClusterDefinition());
        } else {
          if (cls.getClusterState().getState().equals(state)) {
            list.add(cls.getClusterDefinition());
          }
        }
      }
      return list;
    }
    
    /*
     * Get the list of clusters
     * TODO: Get the synchronized snapshot of each cluster definition? 
     */
    public List<Cluster> getClustersList(String state) {
        List<Cluster> list = new ArrayList<Cluster>();
        if (state.equals("ALL")) {
          list.addAll(this.operational_clusters.values());
        } else {
          for (Cluster cls : this.operational_clusters.values()) {
            if (cls.getClusterState().getState().equals(state)) {
              list.add(cls);
            }
          }
        }
        return list;
    }
    
    /* 
     * UTIL methods on entities
     */
    
	/*
	 * Get the list of role names associated with node
	 */
	public List<String> getAssociatedRoleNames(String hostname) {
	  return Nodes.getInstance().getNodes().get(hostname).getNodeState().getNodeRoleNames();
	}
	
	/*
	 *  Return the default role name to be associated with specified cluster node that 
	 *  has no specific role to nodes association specified in the cluster definition
	 *  Throw exception if node is not associated to with any cluster
	 */
	public String getDefaultRoleName(String clusterName) throws Exception {
	    Cluster c = Clusters.getInstance().getClusterByName(clusterName);
	    // TODO: find the default role from the clsuter blueprint 
		return "slaves-role";
	}
	
	/*
   * TODO: Implement proper range expression
   * TODO: Remove any duplicate nodes from the derived list
   */
  public List<String> getHostnamesFromRangeExpressions (List<String> nodeRangeExpressions) throws Exception {
  	List<String> list = new ArrayList<String>();
  	for (String nodeRangeExpression : nodeRangeExpressions) {
    	StringTokenizer st = new StringTokenizer(nodeRangeExpression);
    	while (st.hasMoreTokens()) {
    		list.add(st.nextToken());
    	}
  	}
  	return list;
  }
}
