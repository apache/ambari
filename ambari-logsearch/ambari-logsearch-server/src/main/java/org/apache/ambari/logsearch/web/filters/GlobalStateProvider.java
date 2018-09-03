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
package org.apache.ambari.logsearch.web.filters;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;

public class GlobalStateProvider implements StatusProvider {

  private static final String ZNODE_NOT_READY_MSG = "ZNode is not available for %s. (connection string: %s, endpoint: %s)";
  private static final String ZK_CONFIG_NOT_READY_MSG = "Collection configuration has not uploaded yet for %s. (configuration name: %s, collection name: %s, endpoint: %s)";
  private static final String SOLR_COLLECTION_NOT_READY_MSG = "Solr has not accessible yet for %s collection. (endpoint: %s)";

  private final SolrCollectionState solrCollectionState;
  private final SolrPropsConfig solrPropsConfig;

  public GlobalStateProvider(SolrCollectionState solrCollectionState, SolrPropsConfig solrPropsConfig) {
    this.solrCollectionState = solrCollectionState;
    this.solrPropsConfig = solrPropsConfig;
  }

  @Override
  public StatusMessage getStatusMessage(String requestUri) {
    if (!solrCollectionState.isZnodeReady()) {
      return StatusMessage.with(SERVICE_UNAVAILABLE, String.format(ZNODE_NOT_READY_MSG,
              solrPropsConfig.getCollection(), solrPropsConfig.getZkConnectString(), requestUri));
    } else if (!solrCollectionState.isConfigurationUploaded()) {
      return StatusMessage.with(SERVICE_UNAVAILABLE, String.format(ZK_CONFIG_NOT_READY_MSG, solrPropsConfig.getCollection(),
              solrPropsConfig.getConfigName(), solrPropsConfig.getCollection(), requestUri));
    } else if (!solrCollectionState.isSolrCollectionReady()) {
      return StatusMessage.with(SERVICE_UNAVAILABLE, String.format(SOLR_COLLECTION_NOT_READY_MSG,
              solrPropsConfig.getCollection(), requestUri));
    }
    return null;
  }
}
