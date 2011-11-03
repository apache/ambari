package org.apache.ambari.datastore;

import java.io.IOException;
import java.util.List;

import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.ClusterDefinition;

public interface PersistentDataStore {
    
    
    /*
     * Shutdown the data store. It will stop the data store service
     */
    public void close () throws IOException;
    
    /**
     * Check if cluster exists
     */
    public boolean clusterExists(String clusterName) throws IOException;
    
    /**
     * Get Latest cluster Revision Number
     */
    public int retrieveLatestClusterRevisionNumber(String clusterName) throws IOException;
    
    /**
     * Store the cluster state
     */
    public void storeClusterState (String clusterName, ClusterState clsState) throws IOException;
    
    /**
     * Store the cluster state
     */
    public ClusterState retrieveClusterState (String clusterName) throws IOException;

    /**
     * Store the cluster definition.
     *
     * Return the revision number for new or updated cluster definition
     * If cluster revision is not null then, check if existing revision being updated in the store is same.
     */
    public int storeClusterDefinition (ClusterDefinition clusterDef) throws IOException;
    
    /**
     * Retrieve the cluster definition given the cluster name and revision number
     * If revision number is less than zero, then return latest cluster definition
     */
    public ClusterDefinition retrieveClusterDefinition (String clusterName, int revision) throws IOException;
    
    public static class NameRevisionPair {
        public String name;
        public int maxRevision;
        public NameRevisionPair(String name, int maxRevision) {
            this.name = name;
            this.maxRevision = maxRevision;
        }
    }
  
    /**
     * Retrieve list of existing cluster names
     */
    public List<String> retrieveClusterList () throws IOException;
    
    
    /**
     * Delete cluster entry
     */
    public void deleteCluster (String clusterName) throws IOException;
    
    /**
     * Delete cluster revision(s) less than specified version number 
     */
    public void purgeClusterDefinitionRevisions(String clusterName, int lessThanRevision) throws IOException;
    
    /**
     * Store/update the cluster state
     * Cluster entry should pre-exist else return error
     * Cluster state is updated in place i.e. only laster cluster state is preserved
     * Fields not to be updated should be initialized to null in the ClusterState object
     */
    public void updateClusterState (String clusterName, ClusterState newstate) throws IOException;
    
    
    /**
     * Store the stack configuration.
     * If stack does not exist, create new one else create new revision
     * Return the new stack revision 
     */
    public int storeStack (String stackName, Stack stack) throws IOException;
    
    /**
     * Retrieve stack with specified revision number
     * If revision number is less than zero, then return latest cluster definition
     */
    public Stack retrieveStack (String stackName, int revision) throws IOException;
    
    /**
     * Retrieve all cluster definitions with their latest revisions
     * 
     */
    public List<NameRevisionPair> retrieveStackList () throws IOException;
    
    /**
     * Delete stack
     */
    public int deleteStack(String stackName) throws IOException;
    
    /**
     * Delete stack revision(s) less than specified version number 
     */
    public void deleteStackRevisions(String stackName, int lessThanRevision) throws IOException;
    
    /**
     * Update the component state.
     * If component entry does not exist for given cluster it will be created 
     */
    public void updateComponentState (String clusterName, String componentName, String state) throws IOException;
    
    /**
     * Get the component state.
     * Returns null, if specific component is not associated with the cluster
     */
    public String getComponentState (String clusterName, String componentName) throws IOException;

    
    /**
     * Delete the component state for specified component 
     */
    public void deleteComponentState (String clusterName, String componentName) throws IOException;
 
    /**
     * Update the component role state.
     * If role entry does not exist for given cluster component it will be created 
     */
    public void updateRoleState (String clusterName, String componentName, String roleName, String state) throws IOException;
    
    /**
     * Get the role state.
     * Returns null, if specific component/role is not associated with the cluster
     */
    public String getRoleState (String clusterName, String componentName, String RoleName) throws IOException;

    
    /**
     * Delete the role state for specified component 
     */
    public void deleteRoleState (String clusterName, String componentName, String roleName) throws IOException;
}
