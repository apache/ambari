package org.apache.ambari.datastore;

import java.util.LinkedHashMap;

import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;

public interface PersistentDataStore {
    
    
    /*
     * Shutdown the data store. It will stop the data store service
     */
    public void close () throws Exception;
    
    /**
     * Persist the cluster definition.
     * 
     * Create new cluster entry, if one does not exist already else add new revision to existing cluster
     * Return the revision number for each newly added cluster definition
     */
    public long storeClusterDefinition (ClusterDefinition clusterDef) throws Exception;
    
    /**
     * Retrieve the cluster definition given the cluster name and revision number
     * If revision number is less than zero, then return latest cluster definition
     */
    public ClusterDefinition retrieveClusterDefinition (String clusterName, long revision) throws Exception;
    
    /**
     * Retrieve all cluster definitions in a specified order of their revision numbers 
     * Return LinkedHashMap of <Revision, ClusterDefinition>. LinkedHashMap should preserve insertion order
     * 
     */
    public LinkedHashMap<String, ClusterDefinition> retrieveClusterDefinitions (boolean ascending) throws Exception;
    
    
    /**
     * Delete cluster entry
     */
    public void deleteCluster (String clusterName) throws Exception;
    
    /**
     * Delete cluster revision(s) less than specified version number 
     */
    public void deleteClusterDefinitionRevisions(String clusterName, long lessThanRevision) throws Exception;
    
    /**
     * Store/update the cluster state
     * Cluster entry should pre-exist else return error
     * Cluster state is updated in place i.e. only laster cluster state is preserved
     * Fields not to be updated should be initialized to null in the ClusterState object
     */
    public void updateClusterState (String clusterName, ClusterState newstate) throws Exception;
    
    
    /**
     * Store the stack configuration.
     * If stack does not exist, create new one else create new revision
     * Return the new stack revision 
     */
    public long storeStack (String stackName, Stack stack) throws Exception;
    
    /**
     * Retrieve stack with specified revision number
     * If revision number is less than zero, then return latest cluster definition
     */
    public Stack retrieveStack (String stackName, long revision) throws Exception;
    
    /**
     * Retrieve all stacks in a specified order of their revision numbers 
     * Return LinkedHashMap of <Revision, Stack>. LinkedHashMap should preserve the insertion order
     * 
     */
    public LinkedHashMap<String, Stack> retrieveStackRevisions (boolean ascending) throws Exception;
    
    /**
     * Delete stack
     */
    public long deleteStack(String stackName) throws Exception;
    
    /**
     * Delete stack revision(s) less than specified version number 
     */
    public void deleteStackRevisions(String stackName, long lessThanRevision) throws Exception;
    
    /**
     * Update the component state.
     * If component entry does not exist for given cluster it will be created 
     */
    public void updateComponentState (String clusterName, String componentName, String state) throws Exception;
    
    /**
     * Get the component state.
     * Returns null, if specific component is not associated with the cluster
     */
    public String getComponentState (String clusterName, String componentName) throws Exception;

    
    /**
     * Delete the component state for specified component 
     */
    public void deleteComponentState (String clusterName, String componentName) throws Exception;
 
    /**
     * Update the component role state.
     * If role entry does not exist for given cluster component it will be created 
     */
    public void updateRoleState (String clusterName, String componentName, String roleName, String state) throws Exception;
    
    /**
     * Get the role state.
     * Returns null, if specific component/role is not associated with the cluster
     */
    public String getRoleState (String clusterName, String componentName, String RoleName) throws Exception;

    
    /**
     * Delete the role state for specified component 
     */
    public void deleteRoleState (String clusterName, String componentName, String roleName) throws Exception;
}
