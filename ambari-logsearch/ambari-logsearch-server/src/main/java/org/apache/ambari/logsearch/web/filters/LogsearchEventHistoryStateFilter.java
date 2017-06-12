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

import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.VResponse;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

public class LogsearchEventHistoryStateFilter extends AbstractLogsearchGlobalStateFilter {


  public LogsearchEventHistoryStateFilter(RequestMatcher requestMatcher, SolrCollectionState state, SolrPropsConfig solrPropsConfig) {
    super(requestMatcher, state, solrPropsConfig);
  }

  @Override
  public VResponse getErrorResponse(SolrCollectionState solrCollectionState, SolrPropsConfig solrPropsConfig, HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    if (!solrCollectionState.isZnodeReady()) {
      return RESTErrorUtil.createMessageResponse(String.format(ZNODE_NOT_READY_MSG,
        "history", solrPropsConfig.getZkConnectString(), requestUri), MessageEnums.ZNODE_NOT_READY);
    } else if (!solrCollectionState.isConfigurationUploaded()) {
      return RESTErrorUtil.createMessageResponse(String.format(ZK_CONFIG_NOT_READY_MSG, "history",
        solrPropsConfig.getConfigName(), solrPropsConfig.getCollection(), requestUri), MessageEnums.ZK_CONFIG_NOT_READY);
    } else if (!solrCollectionState.isSolrCollectionReady()) {
      return RESTErrorUtil.createMessageResponse(String.format(SOLR_COLLECTION_NOT_READY_MSG,
        solrPropsConfig.getCollection(), requestUri), MessageEnums.SOLR_COLLECTION_NOT_READY);
    }
    return null;
  }
}
