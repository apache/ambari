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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ambari.logsearch.config.api.LogLevelFilterMonitor;
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility functions for handling ZK operation and monitor ZK data for Log Search configuration
 */
public class LogSearchConfigZKHelper {

  private static final Logger LOG = LoggerFactory.getLogger(LogSearchConfigZKHelper.class);

  private static final int DEFAULT_SESSION_TIMEOUT = 60000;
  private static final int DEFAULT_CONNECTION_TIMEOUT = 30000;
  private static final int RETRY_INTERVAL_MS = 10000;
  private static final String DEFAULT_ZK_ROOT = "/logsearch";
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

  @LogSearchPropertyDescription(
    name = "logsearch.config.zk_connect_string",
    description = "ZooKeeper connection string.",
    examples = {"localhost1:2181,localhost2:2181/znode"},
    sources = {"logsearch.properties", "logfeeder.properties"}
  )
  private static final String ZK_CONNECT_STRING_PROPERTY = "logsearch.config.zk_connect_string";

  @LogSearchPropertyDescription(
    name = "logsearch.config.zk_acls",
    description = "ZooKeeper ACLs for handling configs. (read & write)",
    examples = {"world:anyone:r,sasl:solr:cdrwa,sasl:logsearch:cdrwa"},
    sources = {"logsearch.properties", "logfeeder.properties"},
    defaultValue = "world:anyone:cdrwa"
  )
  private static final String ZK_ACLS_PROPERTY = "logsearch.config.zk_acls";

  @LogSearchPropertyDescription(
    name = "logsearch.config.zk_root",
    description = "ZooKeeper root node where the shippers are stored. (added to the connection string)",
    examples = {"/logsearch"},
    sources = {"logsearch.properties", "logfeeder.properties"}
  )
  private static final String ZK_ROOT_NODE_PROPERTY = "logsearch.config.zk_root";

  @LogSearchPropertyDescription(
    name = "logsearch.config.zk_session_time_out_ms",
    description = "ZooKeeper session timeout in milliseconds",
    examples = {"60000"},
    sources = {"logsearch.properties", "logfeeder.properties"}
  )
  private static final String ZK_SESSION_TIMEOUT_PROPERTY = "logsearch.config.zk_session_time_out_ms";

  @LogSearchPropertyDescription(
    name = "logsearch.config.zk_connection_time_out_ms",
    description = "ZooKeeper connection timeout in milliseconds",
    examples = {"30000"},
    sources = {"logsearch.properties", "logfeeder.properties"}
  )
  private static final String ZK_CONNECTION_TIMEOUT_PROPERTY = "logsearch.config.zk_connection_time_out_ms";

  @LogSearchPropertyDescription(
    name = "logsearch.config.zk_connection_retry_time_out_ms",
    description = "The maximum elapsed time for connecting to ZooKeeper in milliseconds. 0 means retrying forever.",
    examples = {"1200000"},
    sources = {"logsearch.properties", "logfeeder.properties"}
  )
  private static final String ZK_CONNECTION_RETRY_TIMEOUT_PROPERTY = "logsearch.config.zk_connection_retry_time_out_ms";

  private static final long WAIT_FOR_ROOT_SLEEP_SECONDS = 10;

  private LogSearchConfigZKHelper() {
  }

  /**
   * Create ZK curator client from a configuration (map holds the configs for that)
   */
  public static CuratorFramework createZKClient(Map<String, String> properties) {
    String root = MapUtils.getString(properties, ZK_ROOT_NODE_PROPERTY, DEFAULT_ZK_ROOT);
    LOG.info("Connecting to ZooKeeper at " + properties.get(ZK_CONNECT_STRING_PROPERTY) + root);
    return CuratorFrameworkFactory.builder()
      .connectString(properties.get(ZK_CONNECT_STRING_PROPERTY) + root)
      .retryPolicy(getRetryPolicy(properties.get(ZK_CONNECTION_RETRY_TIMEOUT_PROPERTY)))
      .connectionTimeoutMs(getIntProperty(properties, ZK_CONNECTION_TIMEOUT_PROPERTY, DEFAULT_CONNECTION_TIMEOUT))
      .sessionTimeoutMs(getIntProperty(properties, ZK_SESSION_TIMEOUT_PROPERTY, DEFAULT_SESSION_TIMEOUT))
      .build();
  }

  /**
   * Get ACLs from a property (get the value then parse and transform it as ACL objects)
   */
  public static List<ACL> getAcls(Map<String, String> properties) {
    String aclStr = properties.get(ZK_ACLS_PROPERTY);
    if (StringUtils.isBlank(aclStr)) {
      return ZooDefs.Ids.OPEN_ACL_UNSAFE;
    }

    List<ACL> acls = new ArrayList<>();
    List<String> aclStrList = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(aclStr);
    for (String unparcedAcl : aclStrList) {
      String[] parts = unparcedAcl.split(":");
      if (parts.length == 3) {
        acls.add(new ACL(parsePermission(parts[2]), new Id(parts[0], parts[1])));
      }
    }
    return acls;
  }

  private static int getIntProperty(Map<String, String> properties, String propertyKey, int defaultValue) {
    if (properties.get(propertyKey) == null)
      return defaultValue;
    return Integer.parseInt(properties.get(propertyKey));
  }

  private static RetryPolicy getRetryPolicy(String zkConnectionRetryTimeoutValue) {
    if (zkConnectionRetryTimeoutValue == null)
      return new RetryForever(RETRY_INTERVAL_MS);
    int maxElapsedTimeMs = Integer.parseInt(zkConnectionRetryTimeoutValue);
    if (maxElapsedTimeMs == 0)
      return new RetryForever(RETRY_INTERVAL_MS);
    return new RetryUntilElapsed(maxElapsedTimeMs, RETRY_INTERVAL_MS);
  }

  /**
   * Create listener for znode of log level filters - can be used for Log Feeder as it can be useful if it's monitoring the log level changes
   */
  public static TreeCacheListener createTreeCacheListener(String clusterName, Gson gson, LogLevelFilterMonitor logLevelFilterMonitor) {
    return new TreeCacheListener() {
      private final Set<TreeCacheEvent.Type> nodeEvents = ImmutableSet.of(TreeCacheEvent.Type.NODE_ADDED, TreeCacheEvent.Type.NODE_UPDATED, TreeCacheEvent.Type.NODE_REMOVED);
      public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        if (!nodeEvents.contains(event.getType())) {
          return;
        }
        String nodeName = ZKPaths.getNodeFromPath(event.getData().getPath());
        String nodeData = new String(event.getData().getData());
        TreeCacheEvent.Type eventType = event.getType();

        String configPathStab = String.format("/%s/", clusterName);

        if (event.getData().getPath().startsWith(configPathStab + "loglevelfilter/")) {
          handleLogLevelFilterChange(eventType, nodeName, nodeData, gson, logLevelFilterMonitor);
        }
      }
    };
  }

  /**
   * Create root + cluster name znode cache
   */
  public static TreeCache createClusterCache(CuratorFramework client, String clusterName) {
    return new TreeCache(client, String.format("/%s", clusterName));
  }

  /**
   * Assign listener to cluster cache and start to use that listener
   */
  public static void addAndStartListenersOnCluster(TreeCache clusterCache, TreeCacheListener listener) throws Exception {
    clusterCache.getListenable().addListener(listener);
    clusterCache.start();
  }

  public static void waitUntilRootAvailable(CuratorFramework client) throws Exception {
    while (client.checkExists().forPath("/") == null) {
      LOG.info("Root node is not present yet, going to sleep for " + WAIT_FOR_ROOT_SLEEP_SECONDS + " seconds");
      Thread.sleep(WAIT_FOR_ROOT_SLEEP_SECONDS * 1000);
    }
  }

  /**
   * Call log level filter monitor interface to handle node related operations (on update/remove)
   */
  public static void handleLogLevelFilterChange(final TreeCacheEvent.Type eventType, final String nodeName, final String nodeData,
                                                final Gson gson, final LogLevelFilterMonitor logLevelFilterMonitor) {
    switch (eventType) {
      case NODE_ADDED:
      case NODE_UPDATED:
        LOG.info("Node added/updated under loglevelfilter ZK node: " + nodeName);
        LogLevelFilter logLevelFilter = gson.fromJson(nodeData, LogLevelFilter.class);
        logLevelFilterMonitor.setLogLevelFilter(nodeName, logLevelFilter);
        break;
      case NODE_REMOVED:
        LOG.info("Node removed loglevelfilter input ZK node: " + nodeName);
        logLevelFilterMonitor.removeLogLevelFilter(nodeName);
        break;
      default:
        break;
    }
  }

  /**
   * Pares ZK ACL permission string and transform it to an integer
   */
  public static Integer parsePermission(String permission) {
    int permissionCode = 0;
    for (char each : permission.toLowerCase().toCharArray()) {
      switch (each) {
        case 'r':
          permissionCode |= ZooDefs.Perms.READ;
          break;
        case 'w':
          permissionCode |= ZooDefs.Perms.WRITE;
          break;
        case 'c':
          permissionCode |= ZooDefs.Perms.CREATE;
          break;
        case 'd':
          permissionCode |= ZooDefs.Perms.DELETE;
          break;
        case 'a':
          permissionCode |= ZooDefs.Perms.ADMIN;
          break;
        default:
          throw new IllegalArgumentException("Unsupported permission: " + permission);
      }
    }
    return permissionCode;
  }

  public static Gson createGson() {
    return new GsonBuilder().setDateFormat(DATE_FORMAT).create();
  }

}
