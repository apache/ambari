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

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.model.request.impl.AuditComponentRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.SimpleFacetQuery;

import javax.inject.Named;

import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_COMPONENT;

@Named
public class AuditComponentsRequestQueryConverter extends AbstractSearchRequestQueryConverter<AuditComponentRequest, SimpleFacetQuery> {

  @Override
  public SimpleFacetQuery extendSolrQuery(AuditComponentRequest request, SimpleFacetQuery query) {
    FacetOptions facetOptions = new FacetOptions(); // TODO: check that date filtering is needed or not
    facetOptions.addFacetOnField(AUDIT_COMPONENT);
    facetOptions.setFacetSort(FacetOptions.FacetSort.INDEX);
    facetOptions.setFacetLimit(-1);
    query.setFacetOptions(facetOptions);
    return query;
  }

  @Override
  public Sort sort(AuditComponentRequest request) {
    Sort.Direction direction = StringUtils.equals(request.getSortType(), LogSearchConstants.DESCENDING_ORDER)
      ? Sort.Direction.DESC : Sort.Direction.ASC;
    return new Sort(new Sort.Order(direction, AUDIT_COMPONENT));
  }

  @Override
  public SimpleFacetQuery createQuery() {
    return new SimpleFacetQuery();
  }

  @Override
  public LogType getLogType() {
    return LogType.AUDIT;
  }
}
