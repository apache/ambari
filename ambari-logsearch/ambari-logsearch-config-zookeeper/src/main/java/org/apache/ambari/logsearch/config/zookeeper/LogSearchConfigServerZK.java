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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.logsearch.config.api.LogSearchConfigServer;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilterMap;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputSolrProperties;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputAdapter;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputConfigGson;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputConfigImpl;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class LogSearchConfigServerZK extends LogSearchConfigZK implements LogSearchConfigServer {
  private static final Logger LOG = LoggerFactory.getLogger(LogSearchConfigServerZK.class);

  private TreeCache serverCache;

  @Override
  public void init(Map<String, String> properties) throws Exception {
    super.init(properties);

    if (client.checkExists().forPath("/") == null) {
      client.create().creatingParentContainersIfNeeded().forPath("/");
    }
    if (client.checkExists().forPath("/output") == null) {
      client.create().creatingParentContainersIfNeeded().forPath("/output");
    }
    serverCache = new TreeCache(client, "/");
    serverCache.start();
  }

  @Override
  public boolean inputConfigExists(String clusterName, String serviceName) throws Exception {
    String nodePath = String.format("/%s/input/%s", clusterName, serviceName);
    return serverCache.getCurrentData(nodePath) != null;
  }

  @Override
  public void setInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception {
    String nodePath = String.format("/%s/input/%s", clusterName, serviceName);
    client.setData().forPath(nodePath, inputConfig.getBytes());
    LOG.info("Set input config for the service " + serviceName + " for cluster " + clusterName);
  }

  @Override
  public List<String> getServices(String clusterName) {
    String parentPath = String.format("/%s/input", clusterName);
    Map<String, ChildData> serviceNodes = serverCache.getCurrentChildren(parentPath);
    return serviceNodes == null ?
        new ArrayList<>() :
        new ArrayList<>(serviceNodes.keySet());
  }

  @Override
  public String getGlobalConfigs(String clusterName) {
    String globalConfigNodePath = String.format("/%s/global", clusterName);
    return new String(serverCache.getCurrentData(globalConfigNodePath).getData());
  }

  @Override
  public InputConfig getInputConfig(String clusterName, String serviceName) {
    String globalConfigData = getGlobalConfigs(clusterName);
    JsonArray globalConfigs = (JsonArray) new JsonParser().parse(globalConfigData);
    InputAdapter.setGlobalConfigs(globalConfigs);
    
    ChildData childData = serverCache.getCurrentData(String.format("/%s/input/%s", clusterName, serviceName));
    return childData == null ? null : InputConfigGson.gson.fromJson(new String(childData.getData()), InputConfigImpl.class);
  }

  @Override
  public void setLogLevelFilters(String clusterName, LogLevelFilterMap filters) throws Exception {
    for (Map.Entry<String, LogLevelFilter> e : filters.getFilter().entrySet()) {
      String nodePath = String.format("/%s/loglevelfilter/%s", clusterName, e.getKey());
      String logLevelFilterJson = gson.toJson(e.getValue());
      String currentLogLevelFilterJson = new String(serverCache.getCurrentData(nodePath).getData());
      if (!logLevelFilterJson.equals(currentLogLevelFilterJson)) {
        client.setData().forPath(nodePath, logLevelFilterJson.getBytes());
        LOG.info("Set log level filter for the log " + e.getKey() + " for cluster " + clusterName);
      }
    }
  }

  @Override
  public LogLevelFilterMap getLogLevelFilters(String clusterName) {
    String parentPath = String.format("/%s/loglevelfilter", clusterName);
    Map<String, ChildData> logLevelFilterNodes = serverCache.getCurrentChildren(parentPath);
    TreeMap<String, LogLevelFilter> filters = new TreeMap<>();
    for (Map.Entry<String, ChildData> e : logLevelFilterNodes.entrySet()) {
      LogLevelFilter logLevelFilter = gson.fromJson(new String(e.getValue().getData()), LogLevelFilter.class);
      filters.put(e.getKey(), logLevelFilter);
    }
    
    LogLevelFilterMap logLevelFilters = new LogLevelFilterMap();
    logLevelFilters.setFilter(filters);
    return logLevelFilters;
  }

  @Override
  public void saveOutputSolrProperties(String type, OutputSolrProperties outputSolrProperties) throws Exception {
    String nodePath = String.format("/output/solr/%s", type);
    String data = gson.toJson(outputSolrProperties);
    if (outputCache.getCurrentData(nodePath) == null) {
      client.create().creatingParentContainersIfNeeded().withACL(getAcls()).forPath(nodePath, data.getBytes());
    } else {
      client.setData().forPath(nodePath, data.getBytes());
    }
  }
}
