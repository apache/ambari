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

import org.apache.ambari.logsearch.config.api.LogLevelFilterManager;
import org.apache.ambari.logsearch.config.api.LogSearchConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class LogSearchConfigZK implements LogSearchConfig {
  private static final Logger LOG = LoggerFactory.getLogger(LogSearchConfigZK.class);

  protected Map<String, String> properties;
  protected CuratorFramework client;
  protected Gson gson;
  protected LogLevelFilterManager logLevelFilterManager;

  public void init(Map<String, String> properties) throws Exception {
    this.properties = properties;
    client = LogSearchConfigZKHelper.createZKClient(properties);
    client.start();
    gson = LogSearchConfigZKHelper.createGson();
  }

  @Override
  public void createInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception {
    String nodePath = String.format("/%s/input/%s", clusterName, serviceName);
    try {
      client.create().creatingParentContainersIfNeeded().withACL(LogSearchConfigZKHelper.getAcls(properties)).forPath(nodePath, inputConfig.getBytes());
      LOG.info("Uploaded input config for the service " + serviceName + " for cluster " + clusterName);
    } catch (NodeExistsException e) {
      LOG.debug("Did not upload input config for service " + serviceName + " as it was already uploaded by another Log Feeder");
    }
  }

  @Override
  public LogLevelFilterManager getLogLevelFilterManager() {
    return this.logLevelFilterManager;
  }

  @Override
  public void setLogLevelFilterManager(LogLevelFilterManager logLevelFilterManager) {
    this.logLevelFilterManager = logLevelFilterManager;
  }

  @Override
  public void close() {
    LOG.info("Closing ZooKeeper Connection");
    client.close();
  }
}
