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
package org.apache.ambari.logsearch.handler;

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ACLHandler implements SolrZkRequestHandler<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(ACLHandler.class);

  @Override
  public Boolean handle(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) throws Exception {
    List<ACL> aclsToSetList = solrPropsConfig.getZkAcls();
    if (CollectionUtils.isNotEmpty(aclsToSetList)) {
      LOG.info("Setting acls for '{}' collection...", solrPropsConfig.getCollection());
      SolrZkClient zkClient = solrClient.getZkStateReader().getZkClient();
      SolrZooKeeper solrZooKeeper = zkClient.getSolrZooKeeper();
      String collectionPath = String.format("/collections/%s", solrPropsConfig.getCollection());
      String configsPath = String.format("/configs/%s", solrPropsConfig.getConfigName());
      List<ACL> collectionAcls = solrZooKeeper.getACL(collectionPath, new Stat());
      if (isRefreshAclsNeeded(aclsToSetList, collectionAcls)) {
        LOG.info("Acls differs for {}, update acls.", collectionPath);
        setRecursivelyOn(solrZooKeeper, collectionPath, aclsToSetList);
      }
      List<ACL> configsAcls = solrZooKeeper.getACL(configsPath, new Stat());
      if (isRefreshAclsNeeded(aclsToSetList, configsAcls)) {
        LOG.info("Acls differs for {}, update acls.", configsPath);
        setRecursivelyOn(solrZooKeeper, configsPath, aclsToSetList);
      }
    }
    return true;
  }

  private boolean isRefreshAclsNeeded(List<ACL> acls, List<ACL> newAcls) {
    boolean result = false;
    if (acls != null) {
      if (acls.size() != newAcls.size()) {
        return true;
      }
      result = aclDiffers(acls, newAcls);
      if (!result) {
        result = aclDiffers(newAcls, acls);
      }
    }
    return result;
  }

  private boolean aclDiffers(List<ACL> aclList1, List<ACL> aclList2) {
    for (ACL acl : aclList1) {
      for (ACL newAcl : aclList2) {
        if (acl.getId() != null && acl.getId().getId().equals(newAcl.getId().getId())
          && acl.getPerms() != newAcl.getPerms()) {
          LOG.info("ACL for '{}' differs: '{}' on znode, should be '{}'",
            acl.getId().getId(), acl.getPerms(), newAcl.getPerms());
          return true;
        }
      }
    }
    return false;
  }

  private void setRecursivelyOn(SolrZooKeeper solrZooKeeper, String node, List<ACL> acls)
    throws KeeperException, InterruptedException {
    solrZooKeeper.setACL(node, acls, -1);
    for (String child : solrZooKeeper.getChildren(node, null)) {
      String path = node.endsWith("/") ? node + child : node + "/" + child;
      setRecursivelyOn(solrZooKeeper, path, acls);
    }
  }
}
