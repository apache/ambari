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
import org.apache.ambari.logsearch.config.api.InputConfigMonitor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.log4j.Logger;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;

import com.google.common.base.Splitter;

public class LogSearchConfigZK implements LogSearchConfig {
  private static final Logger LOG = Logger.getLogger(LogSearchConfigZK.class);

  private static final int SESSION_TIMEOUT = 15000;
  private static final int CONNECTION_TIMEOUT = 30000;
  private static final String DEFAULT_ZK_ROOT = "/logsearch";
  private static final long WAIT_FOR_ROOT_SLEEP_SECONDS = 10;

  private static final String CLUSTER_NAME_PROPERTY = "cluster.name";
  private static final String ZK_CONNECT_STRING_PROPERTY = "logsearch.config.zk_connect_string";
  private static final String ZK_ACLS_PROPERTY = "logsearch.config.zk_acls";
  private static final String ZK_ROOT_NODE_PROPERTY = "logsearch.config.zk_root";

  private Map<String, String> properties;
  private String root;
  private CuratorFramework client;
  private TreeCache cache;

  @Override
  public void init(Component component, Map<String, String> properties) throws Exception {
    this.properties = properties;
    
    LOG.info("Connecting to ZooKeeper at " + properties.get(ZK_CONNECT_STRING_PROPERTY));
    client = CuratorFrameworkFactory.builder()
        .connectString(properties.get(ZK_CONNECT_STRING_PROPERTY))
        .retryPolicy(new ExponentialBackoffRetry(1000, 3))
        .connectionTimeoutMs(CONNECTION_TIMEOUT)
        .sessionTimeoutMs(SESSION_TIMEOUT)
        .build();
    client.start();

    root = MapUtils.getString(properties, ZK_ROOT_NODE_PROPERTY, DEFAULT_ZK_ROOT);

    if (component == Component.SERVER) {
      if (client.checkExists().forPath(root) == null) {
        client.create().creatingParentContainersIfNeeded().forPath(root);
      }
      cache = new TreeCache(client, root);
      cache.start();
    } else {
      while (client.checkExists().forPath(root) == null) {
        LOG.info("Root node is not present yet, going to sleep for " + WAIT_FOR_ROOT_SLEEP_SECONDS + " seconds");
        Thread.sleep(WAIT_FOR_ROOT_SLEEP_SECONDS * 1000);
      }

      cache = new TreeCache(client, String.format("%s/%s", root, properties.get(CLUSTER_NAME_PROPERTY)));
    }
  }

  @Override
  public boolean inputConfigExists(String clusterName, String serviceName) throws Exception {
    String nodePath = root + "/" + clusterName + "/input/" + serviceName;
    return cache.getCurrentData(nodePath) != null;
  }

  @Override
  public void setInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception {
    String nodePath = String.format("%s/%s/input/%s", root, clusterName, serviceName);
    client.create().creatingParentContainersIfNeeded().withACL(getAcls()).forPath(nodePath, inputConfig.getBytes());
    LOG.info("Set input config for the service " + serviceName + " for cluster " + clusterName);
  }

  private List<ACL> getAcls() {
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
  public void monitorInputConfigChanges(final InputConfigMonitor configMonitor) throws Exception {
    TreeCacheListener listener = new TreeCacheListener() {
      public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
        if (!event.getData().getPath().startsWith(String.format("%s/%s/input/", root, properties.get(CLUSTER_NAME_PROPERTY)))) {
          return;
        }
        
        String nodeName = ZKPaths.getNodeFromPath(event.getData().getPath());
        String nodeData = new String(event.getData().getData());
        switch (event.getType()) {
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
        configMonitor.removeInputs(serviceName);
      }

      private void addInputs(String serviceName, String inputConfig) {
        try {
          configMonitor.loadInputConfigs(serviceName, inputConfig);
        } catch (Exception e) {
          LOG.error("Could not load input configuration for service " + serviceName + ":\n" + inputConfig, e);
        }
      }
    };
    cache.getListenable().addListener(listener);
    cache.start();
  }

  @Override
  public List<String> getServices(String clusterName) {
    String parentPath = String.format("%s/%s/input", root, clusterName);
    Map<String, ChildData> serviceNodes = cache.getCurrentChildren(parentPath);
    return new ArrayList<String>(serviceNodes.keySet());
  }

  @Override
  public String getInputConfig(String clusterName, String serviceName) {
    ChildData childData = cache.getCurrentData(String.format("%s/%s/input/%s", root, clusterName, serviceName));
    return childData == null ? null : new String(childData.getData());
  }

  @Override
  public void close() {
    LOG.info("Closing ZooKeeper Connection");
    client.close();
  }
}
