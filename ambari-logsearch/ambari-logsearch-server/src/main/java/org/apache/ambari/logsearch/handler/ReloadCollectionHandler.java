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
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReloadCollectionHandler implements SolrZkRequestHandler<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(ReloadCollectionHandler.class);

  @Override
  public Boolean handle(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) throws Exception {
    boolean result = false;
    try {
      LOG.info("Reload collection - '{}'", solrPropsConfig.getCollection());
      CollectionAdminRequest.Reload request = CollectionAdminRequest.reloadCollection(solrPropsConfig.getCollection());
      request.process(solrClient);
      result = true;
    } catch (Exception e) {
      LOG.error(String.format("Reload collection ('%s') failed.", solrPropsConfig.getCollection()), e);
    }
    return result;
  }
}
