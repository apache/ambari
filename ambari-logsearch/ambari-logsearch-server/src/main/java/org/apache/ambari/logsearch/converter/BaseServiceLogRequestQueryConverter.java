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
import org.apache.ambari.logsearch.model.request.impl.BaseServiceLogRequest;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.SimpleQuery;
import javax.inject.Named;
import java.util.List;

import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOGTIME;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LEVEL;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.KEY_LOG_MESSAGE;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.HOST;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.PATH;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.BUNDLE_ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.SEQUENCE_ID;

@Named
public class BaseServiceLogRequestQueryConverter extends AbstractServiceLogRequestQueryConverter<BaseServiceLogRequest, SimpleQuery> {

  @Override
  public SimpleQuery extendLogQuery(BaseServiceLogRequest request, SimpleQuery query) {
    List<String> levels = splitValueAsList(request.getLevel(), ",");
    addContainsFilterQuery(query, KEY_LOG_MESSAGE, SolrUtil.escapeForStandardTokenizer(request.getIncludeMessage()));
    addContainsFilterQuery(query, KEY_LOG_MESSAGE, SolrUtil.escapeForStandardTokenizer(request.getExcludeMessage()), true);
    addEqualsFilterQuery(query, HOST, SolrUtil.escapeQueryChars(request.getHostName()));
    addEqualsFilterQuery(query, PATH, SolrUtil.escapeQueryChars(request.getFileName()));
    addEqualsFilterQuery(query, COMPONENT, SolrUtil.escapeQueryChars(request.getComponentName()));
    addEqualsFilterQuery(query, BUNDLE_ID, request.getBundleId());
    if (CollectionUtils.isNotEmpty(levels)){
      addInFilterQuery(query, LEVEL, levels);
    }
    addInFiltersIfNotNullAndEnabled(query, request.getHostList(), HOST, org.apache.commons.lang.StringUtils.isEmpty(request.getHostName()));
    addRangeFilter(query, LOGTIME, request.getFrom(), request.getTo());
    return query;
  }

  @Override
  public Sort sort(BaseServiceLogRequest request) {
    String sortBy = request.getSortBy();
    String sortType = request.getSortType();
    Sort.Order defaultSortOrder;
    if (StringUtils.isNotBlank(sortBy)) {
      Sort.Direction direction = StringUtils.equals(sortType, LogSearchConstants.ASCENDING_ORDER) ? Sort.Direction.ASC : Sort.Direction.DESC;
      defaultSortOrder = new Sort.Order(direction, sortBy);
    } else {
      defaultSortOrder = new Sort.Order(Sort.Direction.DESC, LOGTIME);
    }
    Sort.Order sequenceIdOrder = new Sort.Order(Sort.Direction.DESC, SEQUENCE_ID);
    return new Sort(defaultSortOrder, sequenceIdOrder);
  }

  @Override
  public SimpleQuery createQuery() {
    return new SimpleQuery();
  }

  @Override
  public LogType getLogType() {
    return LogType.SERVICE;
  }
}
