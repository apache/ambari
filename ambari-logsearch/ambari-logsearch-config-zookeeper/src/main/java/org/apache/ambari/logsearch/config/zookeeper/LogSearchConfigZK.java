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

import org.apache.ambari.logsearch.config.api.LogSearchConfig;
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LogSearchConfigZK implements LogSearchConfig {
  private static final Logger LOG = LoggerFactory.getLogger(LogSearchConfigZK.class);

  private static final int SESSION_TIMEOUT = 15000;
  private static final int CONNECTION_TIMEOUT = 30000;
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

  protected Map<String, String> properties;
  protected CuratorFramework client;
  protected TreeCache outputCache;
  protected Gson gson;

  public void init(Map<String, String> properties) throws Exception {
    this.properties = properties;
    
    String root = MapUtils.getString(properties, ZK_ROOT_NODE_PROPERTY, DEFAULT_ZK_ROOT);
    LOG.info("Connecting to ZooKeeper at " + properties.get(ZK_CONNECT_STRING_PROPERTY) + root);
    client = CuratorFrameworkFactory.builder()
        .connectString(properties.get(ZK_CONNECT_STRING_PROPERTY) + root)
        .retryPolicy(new ExponentialBackoffRetry(1000, 3))
        .connectionTimeoutMs(CONNECTION_TIMEOUT)
        .sessionTimeoutMs(SESSION_TIMEOUT)
        .build();
    client.start();

    outputCache = new TreeCache(client, "/output");
    outputCache.start();

    gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
  }

  @Override
  public void createInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception {
    String nodePath = String.format("/%s/input/%s", clusterName, serviceName);
    try {
      client.create().creatingParentContainersIfNeeded().withACL(getAcls()).forPath(nodePath, inputConfig.getBytes());
      LOG.info("Uploaded input config for the service " + serviceName + " for cluster " + clusterName);
    } catch (NodeExistsException e) {
      LOG.debug("Did not upload input config for service " + serviceName + " as it was already uploaded by another Log Feeder");
    }
  }

  @Override
  public void createLogLevelFilter(String clusterName, String logId, LogLevelFilter filter) throws Exception {
    String nodePath = String.format("/%s/loglevelfilter/%s", clusterName, logId);
    String logLevelFilterJson = gson.toJson(filter);
    try {
      client.create().creatingParentContainersIfNeeded().withACL(getAcls()).forPath(nodePath, logLevelFilterJson.getBytes());
      LOG.info("Uploaded log level filter for the log " + logId + " for cluster " + clusterName);
    } catch (NodeExistsException e) {
      LOG.debug("Did not upload log level filters for log " + logId + " as it was already uploaded by another Log Feeder");
    }
  }

  protected List<ACL> getAcls() {
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

  private Integer parsePermission(String permission) {
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

  @Override
  public void close() {
    LOG.info("Closing ZooKeeper Connection");
    client.close();
  }
}
