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

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.common.rest.entities.Role;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.Configuration;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.components.ComponentPluginFactory;
import org.apache.ambari.datastore.PersistentDataStore;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;


public class Cluster {
        
    /*
     * Data Store 
     */
    private final PersistentDataStore dataStore;
   
    /*
     * Latest revision of cluster definition
     */
    private String clusterName = null;
    private int latestRevisionNumber = -1;
    private ClusterDefinition latestDefinition = null;
    
    /*
     * Map of cluster revision to cluster definition
     */
    private final Map<Integer, ClusterDefinition> 
      clusterDefinitionRevisionsList = 
        new ConcurrentHashMap<Integer, ClusterDefinition>();
    private final Map<String, ComponentInfo> components =
        new HashMap<String, ComponentInfo>();
    private final StackFlattener flattener;
    private final ComponentPluginFactory componentPluginFactory;

    private static class ComponentInfo {
      final ComponentPlugin plugin;
      final Map<String, RoleInfo> roles = new HashMap<String,RoleInfo>();
      ComponentInfo(ComponentPlugin plugin) {
        this.plugin = plugin;
      }
    }
    
    private static class RoleInfo {
      Configuration conf;
      RoleInfo(Configuration conf) {
        this.conf = conf;
      }
    }

    @AssistedInject
    public Cluster (StackFlattener flattener,
                    PersistentDataStore dataStore,
                    ComponentPluginFactory plugin,
                    @Assisted String clusterName) {
        this.flattener = flattener;
        this.dataStore = dataStore;
        this.componentPluginFactory = plugin;
        this.clusterName = clusterName;
    }
    
    @AssistedInject
    public Cluster (StackFlattener flattener,
                    PersistentDataStore dataStore,
                    ComponentPluginFactory plugin,
                    @Assisted ClusterDefinition c, 
                    @Assisted ClusterState cs) throws Exception {
        this(flattener, dataStore, plugin, c.getName());
        this.updateClusterDefinition(c);
        this.updateClusterState(cs);
    }
    
    public synchronized void init () throws Exception {
        this.latestRevisionNumber = dataStore.retrieveLatestClusterRevisionNumber(clusterName);
        this.latestDefinition = dataStore.retrieveClusterDefinition(clusterName, this.latestRevisionNumber);
        getComponents(this.latestDefinition);  
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
      getComponents(c);
    }
    
    private void getComponents(ClusterDefinition cluster
        ) throws NumberFormatException, WebApplicationException, IOException {
      Stack flattened = flattener.flattenStack(cluster.getStackName(), 
          Integer.parseInt(cluster.getStackRevision()));
      for (Component component: flattened.getComponents()) {
        ComponentPlugin plugin = 
            componentPluginFactory.getPlugin(component.getDefinition());
        ComponentInfo info = new ComponentInfo(plugin);
        components.put(component.getName(), info);
        for(Role role: component.getRoles()) {
          info.roles.put(role.getName(), new RoleInfo(role.getConfiguration()));
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
      return components.keySet();
    }
    
    public synchronized 
    ComponentPlugin getComponentDefinition(String component) {
      return components.get(component).plugin;
    }
    
    public synchronized
    Configuration getConfiguration(String component, String role) {
      return components.get(component).roles.get(role).conf;
    }
}
