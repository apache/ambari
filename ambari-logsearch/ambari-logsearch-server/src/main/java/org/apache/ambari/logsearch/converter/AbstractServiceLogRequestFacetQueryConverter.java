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
package org.apache.ambari.logsearch.converter;

import org.apache.ambari.logsearch.model.request.impl.BaseServiceLogRequest;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.solr.core.query.SimpleFacetQuery;

import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.BUNDLE_ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.HOST;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LEVEL;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.PATH;

public abstract class AbstractServiceLogRequestFacetQueryConverter<SOURCE extends BaseServiceLogRequest>
  extends AbstractLogRequestFacetQueryConverter<SOURCE> {

  @Override
  public void appendFacetQuery(SimpleFacetQuery facetQuery, SOURCE request) {
    addEqualsFilterQuery(facetQuery, HOST, SolrUtil.escapeQueryChars(request.getHostName()));
    addEqualsFilterQuery(facetQuery, PATH, SolrUtil.escapeQueryChars(request.getFileName()));
    addEqualsFilterQuery(facetQuery, COMPONENT, SolrUtil.escapeQueryChars(request.getComponentName()));
    addEqualsFilterQuery(facetQuery, BUNDLE_ID, request.getBundleId());
    addInFiltersIfNotNullAndEnabled(facetQuery, request.getLevel(), LEVEL, true);
    addInFiltersIfNotNullAndEnabled(facetQuery, request.getHostList(), HOST, StringUtils.isEmpty(request.getHostName()));
  }
}
