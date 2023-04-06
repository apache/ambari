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

import com.google.gson.Gson;
import org.apache.ambari.logsearch.config.api.LogLevelFilterManager;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilterMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LogLevelFilterManagerZK implements LogLevelFilterManager {

  private static final Logger LOG = LoggerFactory.getLogger(LogLevelFilterManagerZK.class);

  private final CuratorFramework client;
  private final TreeCache serverCache;
  private final Gson gson;
  private final List<ACL> aclList;

  public LogLevelFilterManagerZK(Map<String, String> properties) throws Exception {
    this.client = LogSearchConfigZKHelper.createZKClient(properties);
    this.serverCache = new TreeCache(client, "/");
    this.aclList = LogSearchConfigZKHelper.getAcls(properties);
    this.gson = LogSearchConfigZKHelper.createGson();
    this.serverCache.start();
  }

  public LogLevelFilterManagerZK(Map<String, String> properties, CuratorFramework client) throws Exception {
    this.client = client;
    this.serverCache = new TreeCache(client, "/");
    this.aclList = LogSearchConfigZKHelper.getAcls(properties);
    this.gson = LogSearchConfigZKHelper.createGson();
    this.serverCache.start();
  }

  public LogLevelFilterManagerZK(CuratorFramework client, TreeCache serverCache, List<ACL> aclList, Gson gson) {
    this.client = client;
    this.serverCache = serverCache;
    this.aclList = aclList;
    this.gson = gson;
  }

  @Override
  public void createLogLevelFilter(String clusterName, String logId, LogLevelFilter filter) throws Exception {
    String nodePath = String.format("/%s/loglevelfilter/%s", clusterName, logId);
    String logLevelFilterJson = gson.toJson(filter);
    try {
      client.create().creatingParentContainersIfNeeded().withACL(aclList).forPath(nodePath, logLevelFilterJson.getBytes());
      LOG.info("Uploaded log level filter for the log " + logId + " for cluster " + clusterName);
    } catch (KeeperException.NodeExistsException e) {
      LOG.debug("Did not upload log level filters for log " + logId + " as it was already uploaded by another Log Feeder");
    }
  }

  @Override
  public void setLogLevelFilters(String clusterName, LogLevelFilterMap filters) throws Exception {
    for (Map.Entry<String, LogLevelFilter> e : filters.getFilter().entrySet()) {
      String nodePath = String.format("/%s/loglevelfilter/%s", clusterName, e.getKey());
      String logLevelFilterJson = gson.toJson(e.getValue());
      ChildData childData = serverCache.getCurrentData(nodePath);
      String currentLogLevelFilterJson = childData != null ? new String(childData.getData()) : null;
      if (!logLevelFilterJson.equals(currentLogLevelFilterJson)) {
        client.setData().forPath(nodePath, logLevelFilterJson.getBytes());
        LOG.info("Set log level filter for the log " + e.getKey() + " for cluster " + clusterName);
      }
    }
  }

  @Override
  public LogLevelFilterMap getLogLevelFilters(String clusterName) {
    String parentPath = String.format("/%s/loglevelfilter", clusterName);
    TreeMap<String, LogLevelFilter> filters = new TreeMap<>();
    Map<String, ChildData> logLevelFilterNodes = serverCache.getCurrentChildren(parentPath);
    if (logLevelFilterNodes != null && !logLevelFilterNodes.isEmpty()) {
      for (Map.Entry<String, ChildData> e : logLevelFilterNodes.entrySet()) {
        LogLevelFilter logLevelFilter = gson.fromJson(new String(e.getValue().getData()), LogLevelFilter.class);
        filters.put(e.getKey(), logLevelFilter);
      }
    }
    LogLevelFilterMap logLevelFilters = new LogLevelFilterMap();
    logLevelFilters.setFilter(filters);
    return logLevelFilters;
  }

  public CuratorFramework getClient() {
    return client;
  }

  public TreeCache getServerCache() {
    return serverCache;
  }

  public Gson getGson() {
    return gson;
  }
}
