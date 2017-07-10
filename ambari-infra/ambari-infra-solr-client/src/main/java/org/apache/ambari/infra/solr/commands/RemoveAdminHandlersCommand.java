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
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.zookeeper.data.Stat;

public class RemoveAdminHandlersCommand extends AbstractZookeeperRetryCommand<Boolean> {

  public RemoveAdminHandlersCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  @Override
  protected Boolean executeZkCommand(AmbariSolrCloudClient client, SolrZkClient zkClient, SolrZooKeeper solrZooKeeper) throws Exception {
    String solrConfigXmlPath = String.format("/configs/%s/solrconfig.xml", client.getCollection());
    if (zkClient.exists(solrConfigXmlPath, true)) {
      Stat stat = new Stat();
      byte[] solrConfigXmlBytes = zkClient.getData(solrConfigXmlPath, null, stat, true);
      String solrConfigStr = new String(solrConfigXmlBytes);
      if (solrConfigStr.contains("class=\"solr.admin.AdminHandlers\"")) {
        byte[] newSolrConfigXmlBytes = new String(solrConfigXmlBytes).replaceAll("(?s)<requestHandler name=\"/admin/\".*?class=\"solr.admin.AdminHandlers\" />", "").getBytes();
        zkClient.setData(solrConfigXmlPath, newSolrConfigXmlBytes, stat.getVersion() + 1, true);
      }
    }
    return true;
  }
}
