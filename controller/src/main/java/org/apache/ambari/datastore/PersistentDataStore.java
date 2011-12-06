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
package org.apache.ambari.datastore;

import java.io.IOException;
import java.util.List;

import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.ClusterDefinition;

/**
 * Abstraction that stores the Ambari state.
 */
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
    
    /**
     * Retrieve list of existing cluster names
     */
    public List<String> retrieveClusterList () throws IOException;
      
    /**
     * Delete cluster entry
     */
    public void deleteCluster (String clusterName) throws IOException;
    
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
     * Retrieve list of stack names
     * @return
     * @throws IOException
     */
    public List<String> retrieveStackList() throws IOException;
    
    /**
     * Get Latest stack Revision Number
     */
    public int retrieveLatestStackRevisionNumber(String stackName) throws IOException;
    
    /**
     * Delete stack
     */
    public void deleteStack(String stackName) throws IOException;

    /**
     * Check if stack exists
     */
    boolean stackExists(String stackName) throws IOException;
    
}
