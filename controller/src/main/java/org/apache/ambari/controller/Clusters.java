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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.ConfigurationCategory;
import org.apache.ambari.common.rest.entities.Property;
import org.apache.ambari.common.rest.entities.Role;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterInformation;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.RoleToNodes;
import org.apache.ambari.datastore.DataStoreFactory;
import org.apache.ambari.datastore.PersistentDataStore;
import org.apache.ambari.resource.statemachine.ClusterFSM;
import org.apache.ambari.resource.statemachine.StateMachineInvoker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Clusters {
    // TODO: replace system.out.print by LOG
    private static Log LOG = LogFactory.getLog(Clusters.class);
    
    /*
     * Operational clusters include both active and inactive clusters
     */
    protected ConcurrentHashMap<String, Cluster> operational_clusters = new ConcurrentHashMap<String, Cluster>();
    protected PersistentDataStore dataStore = DataStoreFactory.getDataStore(DataStoreFactory.ZOOKEEPER_TYPE);
    
    private static Clusters ClustersTypeRef=null;
        
    private Clusters() {
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
     * Wrapper method over datastore API
     */
    public boolean clusterExists(String clusterName) throws IOException {
        if (!this.operational_clusters.containsKey(clusterName) &&
            dataStore.clusterExists(clusterName) == false) {
            return false;
        }
        return true;
    }
    
    /* 
     * Get the cluster by name
     * Wrapper over datastore API
     */
    public synchronized Cluster getClusterByName(String clusterName) throws Exception {
        if (clusterExists(clusterName)) {
            if (!this.operational_clusters.containsKey(clusterName)) {
                Cluster cls = new Cluster(clusterName);
                cls.init();
                this.operational_clusters.put(clusterName, cls);
            }
            return this.operational_clusters.get(clusterName);
        } else {
            return null;
        }
    }
    
    /*
     * Purge the cluster entry from memory and the data store
     */
    public synchronized void purgeClusterEntry (String clusterName) throws IOException {
        dataStore.deleteCluster(clusterName);
        this.operational_clusters.remove(clusterName);
    }
    
    /*
     * Add Cluster Entry into data store and memory cache
     */
    public synchronized Cluster addClusterEntry (ClusterDefinition cdef, ClusterState cs) throws Exception {
        Cluster cls = new Cluster (cdef, cs);
        this.operational_clusters.put(cdef.getName(), cls);
        return cls;
    }
    
    /*
     * Rename the cluster
     */
    public synchronized void renameCluster(String clusterName, String new_name) throws Exception {
        if (!clusterExists(clusterName)) {
            String msg = "Cluster ["+clusterName+"] does not exist";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        
        if (new_name == null || new_name.equals("")) {
            String msg = "New name of the cluster should be specified as query parameter, (?new_name=xxxx)";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        /*
         * Check if cluster state is ATTAIC, If yes update the name
         * don't make new revision of cluster definition as it is in ATTIC state
         */
        if (!getClusterByName(clusterName).getClusterState().getState().equals(ClusterState.CLUSTER_STATE_ATTIC)) {
            String msg = "Cluster state is not ATTIC. Cluster is only allowed to be renamed in ATTIC state";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_ACCEPTABLE)).get());
        }
        
        Cluster x = this.getClusterByName(clusterName);
        ClusterDefinition cdef = x.getClusterDefinition(-1);
        cdef.setName(new_name);
        ClusterState cs = x.getClusterState();
        this.addClusterEntry(cdef, cs);
        this.purgeClusterEntry(clusterName);
    }
    
    /*
     * Delete Cluster 
     * Delete operation will mark the cluster to_be_deleted and then set the goal state to ATTIC
     * Once cluster gets to ATTIC state, background daemon should purge the cluster entry.
     */
    public synchronized void deleteCluster(String clusterName) throws Exception { 
    
        if (!this.clusterExists(clusterName)) {
            System.out.println("Cluster ["+clusterName+"] does not exist!");
            return;
        }
        
        /*
         * Update the cluster definition with goal state to be ATTIC
         */
        Cluster cls = this.getClusterByName(clusterName);   
        ClusterDefinition cdf = new ClusterDefinition();
        cdf.setName(clusterName);
        cdf.setGoalState(ClusterState.CLUSTER_STATE_ATTIC);
        cls.updateClusterDefinition(cdf);
        
        /* 
         * Update cluster state, mark it "to be deleted"
         */
        ClusterState cs = cls.getClusterState();
        cs.setMarkForDeletionWhenInAttic(true); 
        cls.updateClusterState(cs);
    }

    /* 
     * Create/Update cluster definition 
     * TODO: As nodes or role to node association changes, validate key services nodes are not removed
    */
    public synchronized ClusterDefinition updateCluster(String clusterName, ClusterDefinition c, boolean dry_run) throws Exception {       
        /*
         * Add new cluster if cluster does not exist
         */
        if (!clusterExists(clusterName)) {
            return addCluster(clusterName, c, dry_run);
        }
        
        /*
         * Time being we will keep entire updated copy as new revision
         * TODO: Check if anything has really changed??
         */
        Cluster cls = getClusterByName(clusterName);
        ClusterDefinition newcd = new ClusterDefinition ();
        newcd.setName(clusterName);
        boolean clsDefChanged = false;
        boolean configChanged = false;
        if (c.getStackName() != null && !c.getStackName().equals(cls.getClusterDefinition(-1).getStackName())) {
            newcd.setStackName(c.getStackName());
            clsDefChanged = true;
            configChanged = true;
        } else {
            newcd.setStackName(cls.getClusterDefinition(-1).getStackName());
        }
        if (c.getStackRevision() != null && !c.getStackRevision().equals(cls.getClusterDefinition(-1).getStackRevision())) {
            newcd.setStackRevision(c.getStackRevision());
            clsDefChanged = true;
            configChanged = true;
        } else {
            newcd.setStackRevision(cls.getClusterDefinition(-1).getStackRevision());
        }
        if (c.getDescription() != null && !c.getDescription().equals(cls.getClusterDefinition(-1).getDescription())) {
            newcd.setDescription(c.getDescription());
            clsDefChanged = true;
        } else {
            newcd.setDescription(cls.getClusterDefinition(-1).getDescription());
        }
        if (c.getGoalState() != null && !c.getGoalState().equals(cls.getClusterDefinition(-1).getGoalState())) {
            newcd.setGoalState(c.getGoalState());
            clsDefChanged = true;
        } else {
            newcd.setGoalState(cls.getClusterDefinition(-1).getGoalState());
        }
        if (c.getActiveServices() != null && !c.getActiveServices().equals(cls.getClusterDefinition(-1).getActiveServices())) {
            newcd.setActiveServices(c.getActiveServices());
            clsDefChanged = true;
        } else {
            newcd.setActiveServices(cls.getClusterDefinition(-1).getActiveServices());
        }
        
        /*
         * TODO: What if controller is crashed after updateClusterNodesReservation 
         * before updating and adding new revision of cluster definition?
         */
        boolean updateNodesReservation = false;
        boolean updateNodeToRolesAssociation = false;
        if (c.getNodes() != null && !c.getNodes().equals(cls.getClusterDefinition(-1).getNodes())) {
            newcd.setNodes(c.getNodes());
            updateNodesReservation = true;
            clsDefChanged = true;
            
        } else {
            newcd.setNodes(cls.getClusterDefinition(-1).getNodes());
        }
        if (c.getRoleToNodesMap() != null && !c.getRoleToNodesMap().toString().equals(cls.getClusterDefinition(-1).getRoleToNodesMap().toString())) {
            newcd.setRoleToNodesMap(c.getRoleToNodesMap());
            updateNodeToRolesAssociation = true;
            clsDefChanged = true;
        } else {
            newcd.setRoleToNodesMap(cls.getClusterDefinition(-1).getRoleToNodesMap());
        }
        
        /*
         * If no change in the cluster definition then return
         */
        if (!clsDefChanged) {
            return cls.getClusterDefinition(-1);
        }
        
        /*
         * if Cluster goal state is ATTIC then no need to take any action other than
         * updating the cluster definition.
         */
        if (cls.getClusterState().getState().equals(ClusterState.CLUSTER_STATE_ATTIC) &&
            newcd.getGoalState().equals(ClusterState.CLUSTER_STATE_ATTIC)) {
            ClusterState cs = cls.getClusterState();
            cs.setLastUpdateTime(Util.getXMLGregorianCalendar(new Date()));
            cls.updateClusterDefinition(newcd);
            cls.updateClusterState(cs);
            return cls.getClusterDefinition(-1);
        }
        
        /*
         * Validate the updated cluster definition
         */
        validateClusterDefinition(clusterName, newcd);
        
        /*
         * If dry_run then return the newcd at this point
         */
        if (dry_run) {
            System.out.println ("Dry run for update cluster..");
            return newcd;
        }
        
        /*
         *  Update the new cluster definition and state
         *  Generate the config script for puppet
         */
        ClusterState cs = cls.getClusterState();
        cs.setLastUpdateTime(Util.getXMLGregorianCalendar(new Date()));
        cls.updateClusterDefinition(newcd);
        cls.updateClusterState(cs);
        
        /*
         * Create Puppet config
         
        if (configChanged || updateNodeToRolesAssociation || updateNodesReservation) {
            String puppetConfig = this.getPuppetConfigString (newcd);
            //cls.updatePuppetConfiguration(puppetConfig);
        }*/
        
        /*
         * Update the nodes reservation and node to roles association 
         */
        if (updateNodesReservation) {
            updateClusterNodesReservation (cls.getName(), c);   
        }
        if (updateNodeToRolesAssociation) {
            updateNodeToRolesAssociation(newcd.getNodes(), c.getRoleToNodesMap());
        }
        
        /*
         * Invoke state machine event
         */
        if(c.getGoalState().equals(ClusterState.CLUSTER_STATE_ACTIVE)) {
          StateMachineInvoker.startCluster(cls.getName());
        } else if(c.getGoalState().equals(ClusterState.CLUSTER_STATE_INACTIVE)) {
          StateMachineInvoker.stopCluster(cls.getName());
        } else if(c.getGoalState().equals(ClusterState.CLUSTER_STATE_ATTIC)) {
          StateMachineInvoker.deleteCluster(cls.getName());
        }
    
        return cls.getClusterDefinition(-1);
    }

    /* 
     * Add new Cluster to cluster list  
     */   
    private ClusterDefinition addCluster(String clusterName, ClusterDefinition cdef, boolean dry_run) throws Exception {
        
        /*
         * TODO: Validate the cluster definition and set the default
         * 
         */
        validateClusterDefinition(clusterName, cdef);
        
        /*
         * Add the defaults for optional values, if not set
         */
        setNewClusterDefaults(cdef);
        
        /*
         * Create new cluster object
         */
        Date requestTime = new Date();
        
        ClusterState clsState = new ClusterState();
        clsState.setCreationTime(Util.getXMLGregorianCalendar(requestTime));
        clsState.setLastUpdateTime(Util.getXMLGregorianCalendar(requestTime));
        clsState.setDeployTime(Util.getXMLGregorianCalendar((Date)null));          
        if (cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
            clsState.setState(ClusterState.CLUSTER_STATE_ATTIC);
        } else {
            clsState.setState(ClusterState.CLUSTER_STATE_INACTIVE);
        }
        
        /*
         * TODO: Derive the role to nodes map based on nodes attributes
         * then populate the node to roles association.
         */
        if (cdef.getRoleToNodesMap() == null) {
            List<RoleToNodes> role2NodesList = generateRoleToNodesListBasedOnNodeAttributes (cdef);
            cdef.setRoleToNodesMap(role2NodesList);
        }
        
        /*
         * If dry run then update roles to nodes map, if not specified explicitly
         * and return
         */
        if (dry_run) {
            return cdef;
        }
        
        /*
         * TODO: Create and update the puppet configuration
        */ 
        String puppetConfig = this.getPuppetConfigString (cdef);
        System.out.println("==============================");
        System.out.println(puppetConfig);
        System.out.println("==============================");
        
        
        /*
         * Persist the new cluster and add entry to cache
         */
        Cluster cls = this.addClusterEntry(cdef, clsState);
        
        /*
         * Update cluster nodes reservation. 
         */
        if (cdef.getNodes() != null 
            && !cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
            updateClusterNodesReservation (cls.getName(), cdef);
        }
        
        /*
         * Update the Node to Roles association
         */
        if (!cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
            updateNodeToRolesAssociation(cdef.getNodes(), cdef.getRoleToNodesMap());
        }
        
        /*
         * Create the cluster object with state machine & 
         * activate it if the goal state is ACTIVE
         * TODO: Make sure createCluster is idempotent (i.e. if object already exists
         * then return success)
        */
        ClusterFSM cs = StateMachineInvoker.createCluster(cls,cls.getLatestRevisionNumber(),
                                            cls.getClusterState());
        if(cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ACTIVE)) {          
            cs.activate();
        }
        return cdef;
    }

    /*
     * Add default values for new cluster definition 
     */
    private void setNewClusterDefaults(ClusterDefinition cdef) throws Exception {
        /* 
         * Populate the input cluster definition w/ default values
         */
        if (cdef.getDescription() == null) { cdef.setDescription("Ambari cluster : "+cdef.getName());
        }
        if (cdef.getGoalState() == null) { cdef.setGoalState(ClusterDefinition.GOAL_STATE_INACTIVE);
        }
        
        /*
         * If its new cluster, do not specify the revision, set it to null. A revision number is obtained
         * after persisting the definition
         */
        cdef.setRevision(null);
        
        // TODO: Add the list of active services by querying pluging component.
        if (cdef.getActiveServices() == null) {
            List<String> services = new ArrayList<String>();
            services.add("ALL");
            cdef.setActiveServices(services);
        }    
    }
    
    /*
     * Create RoleToNodes list based on node attributes
     * TODO: For now just pick some nodes randomly
     */
    private List<RoleToNodes> generateRoleToNodesListBasedOnNodeAttributes (ClusterDefinition cdef) {
        List<RoleToNodes> role2NodesList = new ArrayList<RoleToNodes>();
        return role2NodesList;
    }
    
    /*
     * Validate the cluster definition
     * TODO: Validate each role has enough nodes associated with it. 
     */
    private void validateClusterDefinition (String clusterName, ClusterDefinition cdef) throws Exception {
        /*
         * Check if name is not empty or null
         */
        if (cdef.getName() == null ||  cdef.getName().equals("")) {
            String msg = "Cluster Name must be specified and must be non-empty string";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        if (!cdef.getName().equals(clusterName)) {
            String msg = "Cluster Name specified in URL and cluster definition are not same";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        if (cdef.getNodes() == null || cdef.getNodes().equals("")) {
            String msg = "Cluster node range must be specified and must be non-empty string";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        if (cdef.getStackName() == null || cdef.getStackName().equals("")) {
            String msg = "Cluster stack must be specified and must be non-empty string";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        if (cdef.getStackRevision() == null || cdef.getStackRevision().equals("")) {
            String msg = "Cluster stack revision must be specified";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        /*
         * Check if the cluster stack and its parents exist
         * getStack would throw exception if it does not find the stack
         */
        Stack bp = Stacks.getInstance()
                       .getStack(cdef.getStackName(), Integer.parseInt(cdef.getStackRevision()));
        while (bp.getParentName() != null) {
            if (bp.getParentRevision() == null) {
                bp = Stacks.getInstance()
                    .getStack(bp.getParentName(), -1);
            } else {
                bp = Stacks.getInstance()
                .getStack(bp.getParentName(), Integer.parseInt(bp.getParentRevision()));
            }
        }
        
        
        /*
         * Check if nodes requested for cluster are not already allocated to other clusters
         */
        ConcurrentHashMap<String, Node> all_nodes = Nodes.getInstance().getNodes();
        List<String> cluster_node_range = new ArrayList<String>();
        cluster_node_range.addAll(getHostnamesFromRangeExpressions(cdef.getNodes()));
        List<String> preallocatedhosts = new ArrayList<String>();
        for (String n : cluster_node_range) {
            if (all_nodes.containsKey(n) && 
                    (all_nodes.get(n).getNodeState().getClusterName() != null || 
                     all_nodes.get(n).getNodeState().getAllocatedToCluster()
                    )
                ) {
                /* 
                 * Following check is for a very specific case 
                 * When controller starts w/ no persistent data in data store, it adds default clusters
                 * and down the road restart recovery code re-validates the cluster definition when
                 * it finds nodes already allocated. 
                if (all_nodes.get(n).getNodeState().getClusterName() != null && 
                    all_nodes.get(n).getNodeState().getClusterName().equals(clusterName)) { 
                    continue; 
                } */
                preallocatedhosts.add(n);
            }
        }

        if (!preallocatedhosts.isEmpty()) {
            String msg = "Some of the nodes specified for the cluster roles are allocated to other cluster: ["+preallocatedhosts+"]";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.CONFLICT)).get());
        }
        
        
        /*
         * Check if all the nodes explicitly specified in the RoleToNodesMap belong the cluster node range specified 
         */
        if (cdef.getRoleToNodesMap() != null) {
            List<String> nodes_specified_using_role_association = new ArrayList<String>();
            for (RoleToNodes e : cdef.getRoleToNodesMap()) {
                List<String> hosts = getHostnamesFromRangeExpressions(e.getNodes());
                nodes_specified_using_role_association.addAll(hosts);
                // TODO: Remove any duplicate nodes from nodes_specified_using_role_association
            }
            
            nodes_specified_using_role_association.removeAll(cluster_node_range);
            if (!nodes_specified_using_role_association.isEmpty()) {
                String msg = "Some nodes explicityly associated with roles using RoleToNodesMap do not belong in the " +
                             "golbal node range specified for the cluster : ["+nodes_specified_using_role_association+"]";
                throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
            }
        }
        

    }
    
    /*
     * Update the nodes associated with cluster
     */
    private synchronized void updateClusterNodesReservation (String clusterName, ClusterDefinition clsDef) throws Exception {
                
        ConcurrentHashMap<String, Node> all_nodes = Nodes.getInstance().getNodes();
        List<String> cluster_node_range = new ArrayList<String>();
        cluster_node_range.addAll(getHostnamesFromRangeExpressions(clsDef.getNodes()));
       
        /*
         * Reserve the nodes as specified in the node range expressions
         * -- throw exception, if any nodes are pre-associated with other cluster
         */    
        List<String> nodes_currently_allocated_to_cluster = new ArrayList<String>();
        for (Node n : Nodes.getInstance().getNodes().values()) {
            if ( n.getNodeState().getClusterName() != null &&
                 n.getNodeState().getClusterName().equals(clusterName)) {
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
                    all_nodes.get(node_name).reserveNodeForCluster(clusterName, true);
                }    
            } else {
                Date epoch = new Date(0);
                Nodes.getInstance().checkAndUpdateNode(node_name, epoch);
                Node node = Nodes.getInstance().getNode(node_name);
                /*
                 * TODO: Set agentInstalled = true, unless controller uses SSH to setup the agent
                 */
                node.reserveNodeForCluster(clusterName, true);
            }
        }
        
        /*
         * deallocate nodes from a given cluster
         * TODO: Node agent would asynchronously clean up the node and notify it through heartbeat which 
         * would reset the clusterID associated with node
         */
        for (String node_name : nodes_to_deallocate) {
            if (all_nodes.containsKey(node_name)) {
                synchronized (all_nodes.get(node_name)) {
                    all_nodes.get(node_name).releaseNodeFromCluster();
                }
            }
        }
    }

    /*
     * This function disassociate all the nodes from the cluster. The clsuterID associated w/
     * cluster will be reset by heart beat when node reports all clean.
     */
    private synchronized void releaseClusterNodes (String clusterName) throws Exception {
        for (Node clusterNode : Nodes.getInstance().getClusterNodes (clusterName, "", "")) {
            clusterNode.releaseNodeFromCluster();     
        }
    }
    
    /**
     * Update Node to Roles association.  
     * If role is not explicitly associated w/ any node, then assign it w/ default role
     * 
     * @param clusterNodes
     * @param roleToNodesList
     * @throws Exception
     */
    private synchronized void updateNodeToRolesAssociation (String clusterNodes, List<RoleToNodes> roleToNodesList) throws Exception {
        /*
         * Associate roles list with node
         */
        if (roleToNodesList == null) {
            return;
        }
        
        /*
         * Add list of roles to Node
         * If node is not explicitly associated with any role then assign it w/ default role
         */
        for (RoleToNodes e : roleToNodesList) {
            List<String> hosts = getHostnamesFromRangeExpressions(e.getNodes());
            for (String host : hosts) {
              if (Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames() == null) {
                Nodes.getInstance().getNodes().get(host).getNodeState().setNodeRoleNames((new ArrayList<String>()));
              } 
              Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames().add(e.getRoleName());
            }
        }
        
        
        /*
         * Get the list of specified global node list for the cluster and any nodes NOT explicitly specified in the
         * role to nodes map, assign them with default role 
         */
        List<String> specified_node_range = new ArrayList<String>();
        specified_node_range.addAll(getHostnamesFromRangeExpressions(clusterNodes));
        for (String host : specified_node_range) {
            if (Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames() == null) {
                Nodes.getInstance().getNodes().get(host).getNodeState().setNodeRoleNames((new ArrayList<String>()));
                String cid = Nodes.getInstance().getNodes().get(host).getNodeState().getClusterName();
                Nodes.getInstance().getNodes().get(host).getNodeState().getNodeRoleNames().add(getDefaultRoleName(cid));
            } 
        }
    }

    /*
     * Get Cluster stack
     */
    public Stack getClusterStack(String clusterName, boolean expanded) throws Exception {
        if (!this.clusterExists(clusterName)) {
            String msg = "Cluster ["+clusterName+"] does not exist";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        
        Cluster cls = this.getClusterByName(clusterName);
        String stackName = cls.getClusterDefinition(-1).getStackName();
        int stackRevision = Integer.parseInt(cls.getClusterDefinition(-1).getStackRevision());
        
        Stack bp;
        if (!expanded) {
            bp = Stacks.getInstance().getStack(stackName, stackRevision);
        } else {
            // TODO: Get the derived/expanded stack
            bp = Stacks.getInstance().getStack(stackName, stackRevision);
        }
        return bp;
    }
    
    
    /*
     * Get the deployment script for this clustername/revision combo
     */
    public String getInstallAndConfigureScript(String clusterName,
        int revision) {
      return ""; //TODO: fill
    }
    
    /*
     * Get the latest cluster definition
     */
    public ClusterDefinition getLatestClusterDefinition(String clusterName) throws Exception {
        return this.getClusterByName(clusterName).getClusterDefinition(-1);
    }
    
    /*
     * Get Cluster Definition given name and revision
     */
    public ClusterDefinition getClusterDefinition(String clusterName, int revision) throws Exception {
        return this.getClusterByName(clusterName).getClusterDefinition(revision);
    }
    
    /* 
     * Get the cluster Information by name
     */
    public ClusterInformation getClusterInformation (String clusterName) throws Exception  {
        if (!this.clusterExists(clusterName)) {
            String msg = "Cluster ["+clusterName+"] does not exist";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        ClusterInformation clsInfo = new ClusterInformation();
        clsInfo.setDefinition(this.getLatestClusterDefinition(clusterName));
        clsInfo.setState(this.getClusterByName(clusterName).getClusterState());
        return clsInfo;
    }
    
    
    /* 
     * Get the cluster state
    */
    public ClusterState getClusterState(String clusterName) throws Exception {
        if (!this.clusterExists(clusterName)) {
            String msg = "Cluster ["+clusterName+"] does not exist";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        return this.getClusterByName(clusterName).getClusterState();
    }
    
    
    /*
     * Get Cluster Information list i.e. cluster definition and cluster state
     */
    public List<ClusterInformation> getClusterInformationList(String state) throws Exception {
      List<ClusterInformation> list = new ArrayList<ClusterInformation>();
      List<String> clusterNames = dataStore.retrieveClusterList();
      for (String clsName : clusterNames) {
        Cluster cls = this.getClusterByName(clsName);
        if (state.equals("ALL")) {
          ClusterInformation clsInfo = new ClusterInformation();
          clsInfo.setDefinition(cls.getClusterDefinition(-1));
          clsInfo.setState(cls.getClusterState());
          list.add(clsInfo);
        } else {
          if (cls.getClusterState().getState().equals(state)) {
              ClusterInformation clsInfo = new ClusterInformation();
              clsInfo.setDefinition(cls.getClusterDefinition(-1));
              clsInfo.setState(cls.getClusterState());
              list.add(clsInfo);
          }
        }
      }
      return list;
    }
    
    /*
     * Get the list of clusters
     * TODO: Get the synchronized snapshot of each cluster definition? 
     */
    public List<Cluster> getClustersList(String state) throws Exception {
        List<Cluster> list = new ArrayList<Cluster>();
        List<String> clusterNames = dataStore.retrieveClusterList();
        for (String clsName : clusterNames) {
          Cluster cls = this.getClusterByName(clsName);
          if (state.equals("ALL")) {
            list.add(cls);
          } else {
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
        // TODO: find the default role from the clsuter stack 
        return "slaves-role";
    }
    
  /*
   * TODO: Implement proper range expression
   * TODO: Remove any duplicate nodes from the derived list
   */
  public List<String> getHostnamesFromRangeExpressions (String nodeRangeExpression) throws Exception {
      List<String> list = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(nodeRangeExpression, ",");
      while (st.hasMoreTokens()) {
        list.add(st.nextToken());
      }
      return list;
  }
  
  /*
   * Restart recovery for clusters
   */
  public void recoverClustersStateAfterRestart () throws Exception {
      for (Cluster cls : this.getClustersList("ALL")) {
          ClusterDefinition cdef = cls.getClusterDefinition(-1);
          this.validateClusterDefinition (cdef.getName(), cdef);
          /*
           * Update cluster nodes reservation. 
           */
          if (cdef.getNodes() != null 
              && !cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
              this.updateClusterNodesReservation (cls.getName(), cdef);
          }
          
          /*
           * Update the Node to Roles association
           */
          if (!cdef.getGoalState().equals(ClusterDefinition.GOAL_STATE_ATTIC)) {
              this.updateNodeToRolesAssociation(cdef.getNodes(), cdef.getRoleToNodesMap());
          }
      }
  }
  
  private String getPuppetConfigString (ClusterDefinition c) throws Exception {
      // TODO: ignore if comps or roles are not present in stack.
      Stacks stacksCtx = Stacks.getInstance();
      Stack stack = stacksCtx.getStack(c.getStackName(), Integer.parseInt(c.getStackRevision()));
      String config = "\n$hadoop_stack_conf = { ";
      if (stack.getComponents() != null) {
          for (Component comp : stack.getComponents()) {
              if (comp.getRoles() != null) {
                  for (Role role : comp.getRoles()) {
                      //config = config + comp.getName()+"_"+role.getName()+" => { ";
                      config = config+role.getName()+" => { ";
                      if (role.getConfiguration() != null && role.getConfiguration().getCategory() != null) {
                          for (ConfigurationCategory cat : role.getConfiguration().getCategory()) {
                               config = config+"\""+cat.getName()+"\" => { ";
                               if (cat.getProperty() != null) {
                                   for (Property p : cat.getProperty()) {
                                       config = config+p.getName()+" => "+p.getValue()+", ";
                                   }
                               }
                               config = config +" }, \n";
                          }
                      }
                      config = config + "}, \n";
                  } 
              }
          }
      }
      config = config + "} \n";
      
      config = config + "$role_to_nodes = { ";
      for (RoleToNodes roleToNodesEntry : c.getRoleToNodesMap()) {
          config = config + roleToNodesEntry.getRoleName()+ " => [";
          for (String host : this.getHostnamesFromRangeExpressions(roleToNodesEntry.getNodes())) {
              config = config + "\'"+host+"\',";
          }
          config = config + "], \n";
      }
      config = config + "} \n";
      
      return config;
  }
}
