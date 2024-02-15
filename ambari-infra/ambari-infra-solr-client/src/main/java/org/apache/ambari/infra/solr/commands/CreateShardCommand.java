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
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;

public class CreateShardCommand extends AbstractSolrRetryCommand<CollectionAdminRequest.CreateShard, String> {

  private final String shardName;

  public CreateShardCommand(String shardName, int maxRetries, int interval) {
    super(maxRetries, interval);
    this.shardName = shardName;
  }

  @Override
  public String handleResponse(CollectionAdminResponse response, AmbariSolrCloudClient client) throws Exception {
    return shardName;
  }

  @Override
  public CollectionAdminRequest.CreateShard createRequest(AmbariSolrCloudClient client) {
    return CollectionAdminRequest.createShard(client.getCollection(), shardName);
  }

  @Override
  public String errorMessage(AmbariSolrCloudClient client) {
    return String.format("Cannot add shard to collection '%s'", client.getCollection());
  }
}
