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
import org.apache.ambari.logsearch.model.request.impl.AuditBarGraphRequest;
import org.apache.ambari.logsearch.solr.SolrConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;

import javax.inject.Named;

import java.util.Arrays;
import java.util.List;

import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_EVTTIME;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.CLUSTER;

@Named
public class AuditBarGraphRequestQueryConverter extends AbstractDateRangeFacetQueryConverter<AuditBarGraphRequest> {

  @Override
  public String getDateFieldName() {
    return AUDIT_EVTTIME;
  }

  @Override
  public String getTypeFieldName() {
    return AUDIT_COMPONENT;
  }

  @Override
  public LogType getLogType() {
    return LogType.AUDIT;
  }

  @Override
  public SolrQuery convert(AuditBarGraphRequest request) {
    SolrQuery query = super.convert(request);
    addListFilterToSolrQuery(query, CLUSTER, request.getClusters());
    addInFiltersIfNotNullAndEnabled(query, request.getUserList(),
      SolrConstants.AuditLogConstants.AUDIT_REQUEST_USER,
      StringUtils.isNotBlank(request.getUserList()));
    addIncludeFieldValues(query, request.getIncludeQuery());
    addExcludeFieldValues(query, request.getExcludeQuery());
    return query;
  }
}
