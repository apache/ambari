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
import org.apache.ambari.logsearch.solr.util.AclUtils;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class SecureZNodeZkCommand extends AbstractZookeeperRetryCommand<Boolean> {

  public SecureZNodeZkCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  @Override
  protected Boolean executeZkCommand(AmbariSolrCloudClient client, SolrZkClient zkClient, SolrZooKeeper solrZooKeeper) throws Exception {
    List<ACL> acls = solrZooKeeper.getACL(client.getZnode(), new Stat());
    boolean isSecure = client.isSecure();
    boolean isPermissionDiffers = AclUtils.isPermissionDiffersForScheme(acls, "world", ZooDefs.Perms.READ);
    if (isSecure && isPermissionDiffers) {
      acls = AclUtils.updatePermissionForScheme(acls, "world", ZooDefs.Perms.READ);
      solrZooKeeper.setACL(client.getZnode(), acls, -1);
    }
    return isSecure;
  }
}
