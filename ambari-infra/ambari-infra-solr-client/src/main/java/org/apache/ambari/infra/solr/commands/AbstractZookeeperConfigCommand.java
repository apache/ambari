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
import org.apache.solr.common.cloud.ZkConfigManager;

public abstract class AbstractZookeeperConfigCommand<RESPONSE> extends AbstractZookeeperRetryCommand<RESPONSE> {

  public AbstractZookeeperConfigCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  protected abstract RESPONSE executeZkConfigCommand(ZkConfigManager zkConfigManager, AmbariSolrCloudClient client)
    throws Exception;

  @Override
  protected RESPONSE executeZkCommand(AmbariSolrCloudClient client, SolrZkClient zkClient, SolrZooKeeper solrZooKeeper) throws Exception {
    ZkConfigManager zkConfigManager = createZkConfigManager(zkClient);
    return executeZkConfigCommand(zkConfigManager, client);
  }

  protected ZkConfigManager createZkConfigManager(SolrZkClient zkClient) {
    return new ZkConfigManager(zkClient);
  }
}
