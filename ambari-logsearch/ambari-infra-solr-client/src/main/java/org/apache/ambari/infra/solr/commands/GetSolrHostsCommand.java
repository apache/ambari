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
import org.apache.zookeeper.ZooKeeper;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GetSolrHostsCommand extends AbstractRetryCommand<Collection<String>> {

  public GetSolrHostsCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  @Override
  public Collection<String> createAndProcessRequest(AmbariSolrCloudClient solrCloudClient) throws Exception {
    List<String> solrHosts = new ArrayList<>();

    ZooKeeper zk = new ZooKeeper(solrCloudClient.getZkConnectString(), 10000, null);
    List<String> ids = zk.getChildren("/live_nodes", false);
    for (String id : ids) {
      if (id.endsWith("_solr")) {
        String hostAndPort = id.substring(0, id.length() - 5);
        String[] tokens = hostAndPort.split(":");
        String host = InetAddress.getByName(tokens[0]).getHostName();

        solrHosts.add(host);
      }
    }

    return solrHosts;
  }
}
