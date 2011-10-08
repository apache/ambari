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


import java.util.concurrent.ConcurrentHashMap;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;


public class Cluster {
        
    protected String ID;
    /*
     * Latest revision of cluster definition
     */
    protected long latestRevision;
    
    /**
     * @return the latestRevision
     */
    public long getLatestRevision() {
        return latestRevision;
    }

    /*
     * Map of cluster revision to cluster definition
     */
    protected ConcurrentHashMap<Long, ClusterDefinition> clusterDefinitionRevisionsList = null;
    protected ClusterState clusterState;
    
    
    /**
     * @return the iD
     */
    public String getID() {
            return ID;
    }
    
    /**
     * @param iD the iD to set
     */
    public void setID(String iD) {
            ID = iD;
    }
    
    /**
     * @return the clusterDefinition
     */
    public ClusterDefinition getClusterDefinition(long revision) {
            return clusterDefinitionRevisionsList.get(revision);
    }
    
    /**
     * @return the latest clusterDefinition
     */
    public ClusterDefinition getLatestClusterDefinition() {
        return clusterDefinitionRevisionsList.get(this.latestRevision);
    }
    
    /**
     * @return Add Cluster definition
     */
    public void addClusterDefinition(ClusterDefinition c) {
        if (clusterDefinitionRevisionsList == null) {
            clusterDefinitionRevisionsList = new ConcurrentHashMap<Long, ClusterDefinition>();
            clusterDefinitionRevisionsList.put((long)0, c);
            this.latestRevision = 0;
        } else {
            this.latestRevision++;
            clusterDefinitionRevisionsList.put((long)this.latestRevision, c);
        }
    }
    
    /**
     * @return the clusterDefinitionList
     */
    public ConcurrentHashMap<Long, ClusterDefinition> getClusterDefinitionRevisionsList() {
        return clusterDefinitionRevisionsList;
    }

    /**
     * @return the clusterState
     */
    public ClusterState getClusterState() {
            return clusterState;
    }
    
    /**
     * @param clusterState the clusterState to set
     */
    public void setClusterState(ClusterState clusterState) {
            this.clusterState = clusterState;
    }
        
}
