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


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.components.impl.XmlComponentDefinition;


public class Cluster {
        
    /*
     * Latest revision of cluster definition
     */
    private long latestRevision = 0;
    
    /**
     * @return the latestRevision
     */
    public long getLatestRevision() {
        return latestRevision;
    }

    /*
     * Map of cluster revision to cluster definition
     */
    private final Map<Long, ClusterDefinition> clusterDefinitionRevisionsList = 
        new ConcurrentHashMap<Long, ClusterDefinition>();
    private ClusterState clusterState;
    private ClusterDefinition definition;
    private final Map<String, ComponentPlugin> plugins =
        new HashMap<String, ComponentPlugin>();
    
    
    /**
     * @return the clusterDefinition
     */
    public synchronized ClusterDefinition getClusterDefinition(long revision) {
        return clusterDefinitionRevisionsList.get(revision);
    }
    
    /**
     * @return the latest clusterDefinition
     */
    public synchronized ClusterDefinition getLatestClusterDefinition() {
        return definition;
    }
    
    /**
     * @return Add Cluster definition
     */
    public synchronized 
    void addClusterDefinition(ClusterDefinition c) throws Exception {
      this.latestRevision++;
      clusterDefinitionRevisionsList.put((long)this.latestRevision, c);
      definition = c;
      // find the plugins for the current definition of the cluster
      Blueprints context = Blueprints.getInstance();
      Blueprint bp = context.getBlueprint(c.getBlueprintName(),
                                   Integer.parseInt(c.getBlueprintRevision()));
      
      //while (!bp.getName().equals(bp.getParentName()) || !bp.getRevision().equals(bp.getParentRevision())) {    
      while (bp.getParentName() != null) {
        for(Component comp: bp.getComponents()) {
          String name = comp.getName();
          if (!plugins.containsKey(name) && comp.getDefinition() != null) {
            plugins.put(name, new XmlComponentDefinition(comp.getDefinition()));
          }
        }
        
        // go up to the parent
        bp = context.getBlueprint(bp.getParentName(), 
                                  Integer.parseInt(bp.getParentRevision()));
      }
    }
    
    /**
     * @return the clusterDefinitionList
     */
    public Map<Long, ClusterDefinition> getClusterDefinitionRevisionsList() {
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
    
    public synchronized String getName() {
      return definition.getName();
    }

    public synchronized Iterable<String> getComponents() {
      return plugins.keySet();
    }
    
    public synchronized 
    ComponentPlugin getComponentDefinition(String component) {
      return plugins.get(component);
    }
}
