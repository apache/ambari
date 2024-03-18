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
package org.apache.ambari.infra.solr.util;

import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AclUtils {

  public static List<ACL> mergeAcls(List<ACL> originalAcls, List<ACL> updateAcls) {
    Map<String, ACL> aclMap = new HashMap<>();
    List<ACL> acls = new ArrayList<>();
    if (originalAcls != null) {
      for (ACL acl : originalAcls) {
        aclMap.put(acl.getId().getId(), acl);
      }
    }

    if (updateAcls != null) {
      for (ACL acl : updateAcls) {
        aclMap.put(acl.getId().getId(), acl);
      }
    }

    for (Map.Entry<String, ACL> aclEntry : aclMap.entrySet()) {
      acls.add(aclEntry.getValue());
    }
    return acls;
  }

  public static List<ACL> createAclListFromSaslUsers(String[] saslUsers) {
    List<ACL> saslUserList = new ArrayList<>();
    for (String saslUser : saslUsers) {
      ACL acl = new ACL();
      acl.setId(new Id("sasl", saslUser));
      acl.setPerms(ZooDefs.Perms.ALL);
      saslUserList.add(acl);
    }
    return saslUserList;
  }

  public static void setRecursivelyOn(SolrZooKeeper solrZooKeeper, String node, List<ACL> acls) throws KeeperException, InterruptedException {
    setRecursivelyOn(solrZooKeeper, node, acls, new ArrayList<String>());
  }

  public static void setRecursivelyOn(SolrZooKeeper solrZooKeeper, String node, List<ACL> acls, List<String> excludePaths)
    throws KeeperException, InterruptedException {
    if (!excludePaths.contains(node)) {
      List<ACL> newAcls = AclUtils.mergeAcls(solrZooKeeper.getACL(node, new Stat()), acls);
      solrZooKeeper.setACL(node, newAcls, -1);
      for (String child : solrZooKeeper.getChildren(node, null)) {
        setRecursivelyOn(solrZooKeeper, path(node, child), acls, excludePaths);
      }
    }
  }

  private static String path(String node, String child) {
    return node.endsWith("/") ? node + child : node + "/" + child;
  }
}
