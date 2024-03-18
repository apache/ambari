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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.common.VResponse;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

abstract public class AbstractLogsearchGlobalStateFilter implements Filter {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractLogsearchGlobalStateFilter.class);

  protected static final String ZNODE_NOT_READY_MSG = "ZNode is not available for %s. (connection string: %s, endpoint: %s)";
  protected static final String ZK_CONFIG_NOT_READY_MSG = "Collection configuration has not uploaded yet for %s. (configuration name: %s, collection name: %s, endpoint: %s)";
  protected static final String SOLR_COLLECTION_NOT_READY_MSG = "Solr has not accessible yet for %s collection. (endpoint: %s)";

  private SolrCollectionState solrCollectionState;
  private RequestMatcher requestMatcher;
  private SolrPropsConfig solrPropsConfig;

  public AbstractLogsearchGlobalStateFilter(RequestMatcher requestMatcher, SolrCollectionState state, SolrPropsConfig solrPropsConfig) {
    this.requestMatcher = requestMatcher;
    this.solrCollectionState = state;
    this.solrPropsConfig = solrPropsConfig;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    if (requestMatcher.matches(request)) {
      VResponse errorResponse = getErrorResponse(solrCollectionState, solrPropsConfig, request);
      if (errorResponse != null) {
        LOG.info("{} request is filtered out: {}", request.getRequestURL(), errorResponse.getMsgDesc());
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        resp.setStatus(500);
        resp.setContentType("application/json");
        resp.getWriter().print(createStringFromErrorMessageObject(errorResponse));
        return;
      }
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {
  }

  /**
   * Fill the error message with data in case of any condition based on collection state.
   * If return value is null, that means there were no any errors
   * @param solrCollectionState object to store solr state (e.g. : zookeeper/solr collection availability)
   * @param solrPropsConfig object to store details for solr collection (e.g. : collection name, connection string)
   */
  public abstract VResponse getErrorResponse(SolrCollectionState solrCollectionState, SolrPropsConfig solrPropsConfig, HttpServletRequest request);

  private String createStringFromErrorMessageObject(VResponse responseObject) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(responseObject);
    } catch (Exception e) {
      throw RESTErrorUtil.createRESTException("Cannot parse response object on backend", MessageEnums.ERROR_CREATING_OBJECT);
    }
  }
}
