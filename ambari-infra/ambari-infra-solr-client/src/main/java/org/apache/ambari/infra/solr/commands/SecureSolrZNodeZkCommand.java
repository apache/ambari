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
package org.apache.ambari.infra.solr.commands;

import org.apache.ambari.infra.solr.AmbariSolrCloudClient;
import org.apache.ambari.infra.solr.util.AclUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecureSolrZNodeZkCommand extends AbstractZookeeperRetryCommand<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(SecureSolrZNodeZkCommand.class);

  public SecureSolrZNodeZkCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  @Override
  protected Boolean executeZkCommand(AmbariSolrCloudClient client, SolrZkClient zkClient, SolrZooKeeper solrZooKeeper) throws Exception {
    String zNode = client.getZnode();
    List<ACL> newAclList = new ArrayList<>();
    List<ACL> saslUserList = AclUtils.createAclListFromSaslUsers(client.getSaslUsers().split(","));
    newAclList.addAll(saslUserList);
    newAclList.add(new ACL(ZooDefs.Perms.READ, new Id("world", "anyone")));

    String configsPath = String.format("%s/%s", zNode, "configs");
    String collectionsPath = String.format("%s/%s", zNode, "collections");
    String aliasesPath = String.format("%s/%s", zNode, "aliases.json"); // TODO: protect this later somehow
    List<String> excludePaths = Arrays.asList(configsPath, collectionsPath, aliasesPath);

    createZnodeIfNeeded(configsPath, client.getSolrZkClient());
    createZnodeIfNeeded(collectionsPath, client.getSolrZkClient());

    AclUtils.setRecursivelyOn(client.getSolrZkClient().getSolrZooKeeper(), zNode, newAclList, excludePaths);

    List<ACL> commonConfigAcls = new ArrayList<>();
    commonConfigAcls.addAll(saslUserList);
    commonConfigAcls.add(new ACL(ZooDefs.Perms.READ | ZooDefs.Perms.CREATE, new Id("world", "anyone")));

    LOG.info("Set sasl users for znode '{}' : {}", client.getZnode(), StringUtils.join(saslUserList, ","));
    LOG.info("Skip {}/configs and {}/collections", client.getZnode(), client.getZnode());
    solrZooKeeper.setACL(configsPath, AclUtils.mergeAcls(solrZooKeeper.getACL(configsPath, new Stat()), commonConfigAcls), -1);
    solrZooKeeper.setACL(collectionsPath, AclUtils.mergeAcls(solrZooKeeper.getACL(collectionsPath, new Stat()), commonConfigAcls), -1);

    LOG.info("Set world:anyone to 'cr' on  {}/configs and {}/collections", client.getZnode(), client.getZnode());
    AclUtils.setRecursivelyOn(solrZooKeeper, configsPath, saslUserList);
    AclUtils.setRecursivelyOn(solrZooKeeper, collectionsPath, saslUserList);

    return true;
  }

  private void createZnodeIfNeeded(String configsPath, SolrZkClient zkClient) throws KeeperException, InterruptedException {
    if (!zkClient.exists(configsPath, true)) {
      LOG.info("'{}' does not exist. Creating it ...", configsPath);
      zkClient.makePath(configsPath, true);
    }
  }
}
