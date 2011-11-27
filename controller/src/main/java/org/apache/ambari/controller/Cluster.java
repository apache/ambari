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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.components.impl.XmlComponentDefinition;
import org.apache.ambari.datastore.DataStoreFactory;
import org.apache.ambari.datastore.PersistentDataStore;


public class Cluster {
        
    /*
     * Data Store 
     */
    private PersistentDataStore dataStore = DataStoreFactory.getDataStore(DataStoreFactory.ZOOKEEPER_TYPE);
   
    /*
     * Latest revision of cluster definition
     */
    private String clusterName = null;
    private int latestRevisionNumber = -1;
    private ClusterDefinition latestDefinition = null;
    
    /*
     * Map of cluster revision to cluster definition
     */
    private final Map<Integer, ClusterDefinition> clusterDefinitionRevisionsList = 
                    new ConcurrentHashMap<Integer, ClusterDefinition>();
    private final Map<String, ComponentPlugin> plugins =
                  new HashMap<String, ComponentPlugin>();
    
    /*
     * Store cluster puppet configuration
     */
    private final Map<Integer, String> clusterConfigurationRevisionList = new HashMap<Integer, String>();
    
    
    public Cluster (String clusterName) {
        this.clusterName = clusterName;
    }
    
    public Cluster (ClusterDefinition c, ClusterState cs) throws Exception {
        this.clusterName = c.getName();
        this.updateClusterDefinition(c);
        this.updateClusterState(cs);
    }
    
    public synchronized void init () throws Exception {
        this.latestRevisionNumber = dataStore.retrieveLatestClusterRevisionNumber(clusterName);
        this.latestDefinition = dataStore.retrieveClusterDefinition(clusterName, this.latestRevisionNumber);
        loadPlugins(this.latestDefinition);  
        this.clusterDefinitionRevisionsList.put(this.latestRevisionNumber, this.latestDefinition);
    }
    
    /**
     * @return the clusterDefinition
     */
    public synchronized ClusterDefinition getClusterDefinition(int revision) throws IOException {
        ClusterDefinition cdef = null;
        if (revision < 0) {
            cdef = this.latestDefinition;
        } else {
            if (!this.clusterDefinitionRevisionsList.containsKey(revision)) {
                cdef = dataStore.retrieveClusterDefinition(clusterName, revision);
                if (!this.clusterDefinitionRevisionsList.containsKey(revision)) {
                    this.clusterDefinitionRevisionsList.put(revision, cdef);
                }
            } else {
                cdef = this.clusterDefinitionRevisionsList.get(revision);
            }
        }
        return cdef;
    }
    
    /**
     * @return the latestRevision
     */
    public int getLatestRevisionNumber() {
        return this.latestRevisionNumber;
    }
    
    /**
     * @return Add Cluster definition
     */
    public synchronized void updateClusterDefinition(ClusterDefinition c) throws Exception {
      this.latestRevisionNumber = dataStore.storeClusterDefinition(c);
      this.clusterDefinitionRevisionsList.put(this.latestRevisionNumber, c);
      this.latestDefinition = c;
      
      // find the plugins for the current definition of the cluster
      loadPlugins(c);
    }
    
    /**
     * @return Add puppet configuration
     */
    public synchronized void updatePuppetConfiguration(String puppetConfig) throws Exception {
      //this.latestRevisionNumber = dataStore.storeClusterDefinition(c);
      //this.clusterDefinitionRevisionsList.put(this.latestRevisionNumber, c);
      //this.latestDefinition = c;
    }


    /*
     * Load plugins for the current definition of the cluster
     */
    private void loadPlugins (ClusterDefinition c) throws Exception {
        
        Stacks context = Stacks.getInstance();
        Stack bp = context.getStack(c.getStackName(),
                                     Integer.parseInt(c.getStackRevision()));
        
        while (bp != null) {
          for(Component comp: bp.getComponents()) {
            String name = comp.getName();
            if (!plugins.containsKey(name) && comp.getDefinition() != null) {
              plugins.put(name, new XmlComponentDefinition(comp.getDefinition()));
            }
          }
          
          // go up to the parent
          if (bp.getParentName() != null) {
            bp = context.getStack(bp.getParentName(), 
                                    Integer.parseInt(bp.getParentRevision()));
          } else {
            bp = null;
          }
        }
    }
    
    /**
     * @return the clusterState
     */
    public ClusterState getClusterState() throws IOException {
        return dataStore.retrieveClusterState(this.clusterName);
    }
    
    /**
     * @param clusterState the clusterState to set
     */
    public void updateClusterState(ClusterState clusterState) throws IOException {
        dataStore.storeClusterState(this.clusterName, clusterState);
    }
    
    public String getName() {
        return this.latestDefinition.getName();
    }

    public synchronized Iterable<String> getComponents() {
        return this.plugins.keySet();
    }
    
    public synchronized 
    ComponentPlugin getComponentDefinition(String component) {
        return this.plugins.get(component);
    }
}
