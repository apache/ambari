/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.config.zookeeper;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.logsearch.config.api.LogLevelFilterManager;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.ambari.logsearch.config.json.JsonHelper;
import org.apache.ambari.logsearch.config.json.model.inputconfig.impl.InputConfigGson;
import org.apache.ambari.logsearch.config.json.model.inputconfig.impl.InputConfigImpl;
import org.apache.ambari.logsearch.config.api.InputConfigMonitor;
import org.apache.ambari.logsearch.config.api.LogLevelFilterMonitor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class LogSearchConfigLogFeederZK extends LogSearchConfigZK implements LogSearchConfigLogFeeder {
  private static final Logger LOG = LoggerFactory.getLogger(LogSearchConfigLogFeederZK.class);

  private TreeCache logFeederClusterCache;

  @Override
  public void init(Map<String, String> properties, String clusterName) throws Exception {
    super.init(properties);
    LogSearchConfigZKHelper.waitUntilRootAvailable(client);
    logFeederClusterCache = LogSearchConfigZKHelper.createClusterCache(client, clusterName);
    LogLevelFilterManager logLevelFilterManager = new LogLevelFilterManagerZK(client, null, LogSearchConfigZKHelper.getAcls(properties), gson);
    setLogLevelFilterManager(logLevelFilterManager);
  }

  @Override
  public boolean inputConfigExists(String serviceName) throws Exception {
    String nodePath = String.format("/input/%s", serviceName);
    return logFeederClusterCache.getCurrentData(nodePath) != null;
  }

  @Override
  public void monitorInputConfigChanges(final InputConfigMonitor inputConfigMonitor,
      final LogLevelFilterMonitor logLevelFilterMonitor, final String clusterName) throws Exception {
    final JsonParser parser = new JsonParser();
    final JsonArray globalConfigNode = new JsonArray();
    for (String globalConfigJsonString : inputConfigMonitor.getGlobalConfigJsons()) {
      JsonElement globalConfigJson = parser.parse(globalConfigJsonString);
      globalConfigNode.add(globalConfigJson.getAsJsonObject().get("global"));
    }
    
    createGlobalConfigNode(globalConfigNode, clusterName);
    
    TreeCacheListener listener = new TreeCacheListener() {
      private final Set<Type> nodeEvents = ImmutableSet.of(Type.NODE_ADDED, Type.NODE_UPDATED, Type.NODE_REMOVED);
      
      public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        if (!nodeEvents.contains(event.getType())) {
          return;
        }
        
        String nodeName = ZKPaths.getNodeFromPath(event.getData().getPath());
        String nodeData = new String(event.getData().getData());
        Type eventType = event.getType();
        
        String configPathStab = String.format("/%s/", clusterName);
        
        if (event.getData().getPath().startsWith(configPathStab + "input/")) {
          handleInputConfigChange(eventType, nodeName, nodeData);
        } else if (event.getData().getPath().startsWith(configPathStab + "loglevelfilter/")) {
          LogSearchConfigZKHelper.handleLogLevelFilterChange(eventType, nodeName, nodeData, gson, logLevelFilterMonitor);
        }
      }

      private void handleInputConfigChange(Type eventType, String nodeName, String nodeData) {
        switch (eventType) {
          case NODE_ADDED:
            LOG.info("Node added under input ZK node: " + nodeName);
            addInputs(nodeName, nodeData);
            break;
          case NODE_UPDATED:
            LOG.info("Node updated under input ZK node: " + nodeName);
            removeInputs(nodeName);
            addInputs(nodeName, nodeData);
            break;
          case NODE_REMOVED:
            LOG.info("Node removed from input ZK node: " + nodeName);
            removeInputs(nodeName);
            break;
          default:
            break;
        }
      }

      private void removeInputs(String serviceName) {
        inputConfigMonitor.removeInputs(serviceName);
      }

      private void addInputs(String serviceName, String inputConfig) {
        try {
          JsonElement inputConfigJson = parser.parse(inputConfig);
          for (Map.Entry<String, JsonElement> typeEntry : inputConfigJson.getAsJsonObject().entrySet()) {
            for (JsonElement e : typeEntry.getValue().getAsJsonArray()) {
              for (JsonElement globalConfig : globalConfigNode) {
                JsonHelper.merge(globalConfig.getAsJsonObject(), e.getAsJsonObject());
              }
            }
          }
          
          inputConfigMonitor.loadInputConfigs(serviceName, InputConfigGson.gson.fromJson(inputConfigJson, InputConfigImpl.class));
        } catch (Exception e) {
          LOG.error("Could not load input configuration for service " + serviceName + ":\n" + inputConfig, e);
        }
      }
    };
    logFeederClusterCache.getListenable().addListener(listener);
    logFeederClusterCache.start();
  }

  private void createGlobalConfigNode(JsonArray globalConfigNode, String clusterName) {
    String globalConfigNodePath = String.format("/%s/global", clusterName);
    String data = InputConfigGson.gson.toJson(globalConfigNode);
    
    try {
      if (logFeederClusterCache.getCurrentData(globalConfigNodePath) != null) {
        client.setData().forPath(globalConfigNodePath, data.getBytes());
      } else {
        client.create().creatingParentContainersIfNeeded().withACL(LogSearchConfigZKHelper.getAcls(properties)).forPath(globalConfigNodePath, data.getBytes());
      }
    } catch (Exception e) {
      LOG.warn("Exception during global config node creation/update", e);
    }
  }
}
