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
import org.apache.ambari.logsearch.model.request.impl.BaseLogRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Query;

import java.util.List;

import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_EVTTIME;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.SEQUENCE_ID;

public abstract class AbstractAuditLogRequestQueryConverter<SOURCE extends BaseLogRequest, RESULT extends Query>
  extends AbstractLogRequestQueryConverter<SOURCE, RESULT>{

  @Override
  public Sort sort(SOURCE request) {
    String sortBy = request.getSortBy();
    String sortType = request.getSortType();
    Sort.Order defaultSortOrder;
    if (StringUtils.isNotBlank(sortBy)) {
      Sort.Direction direction = StringUtils.equals(sortType , LogSearchConstants.ASCENDING_ORDER) ? Sort.Direction.ASC : Sort.Direction.DESC;
      defaultSortOrder = new Sort.Order(direction, sortBy);
    } else {
      defaultSortOrder = new Sort.Order(Sort.Direction.DESC, AUDIT_EVTTIME);
    }
    Sort.Order sequenceIdOrder = new Sort.Order(Sort.Direction.DESC, SEQUENCE_ID);
    return new Sort(defaultSortOrder, sequenceIdOrder);
  }

  @Override
  public void addComponentFilters(SOURCE request, RESULT query) {
    List<String> includeTypes = splitValueAsList(request.getMustBe(), ",");
    List<String> excludeTypes = splitValueAsList(request.getMustNot(), ",");
    addInFilterQuery(query, AUDIT_COMPONENT, includeTypes);
    addInFilterQuery(query, AUDIT_COMPONENT, excludeTypes, true);
  }

}
