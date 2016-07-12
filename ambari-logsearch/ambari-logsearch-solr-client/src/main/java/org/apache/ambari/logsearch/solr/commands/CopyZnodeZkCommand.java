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
package org.apache.ambari.logsearch.solr.commands;

import org.apache.ambari.logsearch.solr.AmbariSolrCloudClient;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CopyZnodeZkCommand extends AbstractZookeeperRetryCommand<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(CopyZnodeZkCommand.class);

  public CopyZnodeZkCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  @Override
  protected Boolean executeZkCommand(AmbariSolrCloudClient client, SolrZkClient zkClient, SolrZooKeeper solrZooKeeper)
    throws Exception {
    String znode = client.getZnode();
    String znodeCopyFromZnode = client.getCopyFromZnode();
    List<String> children = solrZooKeeper.getChildren(znodeCopyFromZnode, true);
    children = filterRootChildren(children);
    copyConent(znodeCopyFromZnode, znode, children, zkClient);
    return true;
  }

  private void copyConent(String srcParentPath, String destParentPath, List<String> children, SolrZkClient zkClient)
    throws Exception {
    if (!children.isEmpty()) {
      for (String child : children) {
        String srcPath = String.format("%s/%s", srcParentPath, child);
        String destPath = String.format("%s/%s", destParentPath, child);
        byte[] data = zkClient.getData(srcPath, null, null, true);
        if (zkClient.exists(destPath, true)) {
          zkClient.setData(destPath, data, true);
        } else {
          zkClient.create(destPath, data, CreateMode.PERSISTENT, true);
        }
        LOG.info("Copy file from '{}' to '{}'", srcPath, destPath);
        copyConent(srcPath, destPath, zkClient.getChildren(srcPath, null, true), zkClient);
      }
    }
  }

  private List<String> filterRootChildren(List<String> children) {
    List<String> filteredResult = new ArrayList<>();
    if (!children.isEmpty()) {
      for (String child : children) {
        if (!child.equals("security.json") && !child.equals(AbstractStateFileZkCommand.STATE_FILE)){
          filteredResult.add(child);
        }
      }
    }
    return filteredResult;
  }
}
