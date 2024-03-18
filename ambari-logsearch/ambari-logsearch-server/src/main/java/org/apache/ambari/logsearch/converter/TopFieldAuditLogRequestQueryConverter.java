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

import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.model.request.impl.TopFieldAuditLogRequest;
import org.apache.ambari.logsearch.solr.SolrConstants;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.SimpleFacetQuery;

import javax.inject.Named;

import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_EVTTIME;

@Named
public class TopFieldAuditLogRequestQueryConverter extends AbstractLogRequestFacetQueryConverter<TopFieldAuditLogRequest> {

  @Override
  public void appendFacetOptions(FacetOptions facetOptions, TopFieldAuditLogRequest request) {
    facetOptions.addFacetOnPivot(request.getField(), AUDIT_COMPONENT);
    facetOptions.setFacetLimit(request.getTop());
  }

  @Override
  public FacetOptions.FacetSort getFacetSort() {
    return FacetOptions.FacetSort.COUNT;
  }

  @Override
  public String getDateTimeField() {
    return AUDIT_EVTTIME;
  }

  @Override
  public LogType getLogType() {
    return LogType.AUDIT;
  }

  @Override
  public void appendFacetQuery(SimpleFacetQuery facetQuery, TopFieldAuditLogRequest request) {
    addInFiltersIfNotNullAndEnabled(facetQuery, request.getUserList(),
      SolrConstants.AuditLogConstants.AUDIT_REQUEST_USER,
      StringUtils.isNotBlank(request.getUserList()));
  }
}
