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
import org.apache.ambari.logsearch.model.request.impl.ServiceLogTruncatedRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.SimpleQuery;

import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.SEQUENCE_ID;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.HOST;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LOGTIME;

public class ServiceLogTruncatedRequestQueryConverter extends AbstractServiceLogRequestQueryConverter<ServiceLogTruncatedRequest, SimpleQuery>{

  private String sequenceId;

  private String logTime;

  @Override
  public SimpleQuery extendLogQuery(ServiceLogTruncatedRequest request, SimpleQuery query) {
    addEqualsFilterQuery(query, COMPONENT, request.getComponentName());
    addEqualsFilterQuery(query, HOST, request.getHostName());
    String scrollType = request.getScrollType();
    if (LogSearchConstants.SCROLL_TYPE_BEFORE.equals(scrollType)) {
      Integer secuenceIdNum = Integer.parseInt(getSequenceId()) - 1;
      addRangeFilter(query, LOGTIME, null, getLogTime());
      addRangeFilter(query, SEQUENCE_ID, null, secuenceIdNum.toString());
    } else if (LogSearchConstants.SCROLL_TYPE_AFTER.equals(scrollType)) {
      Integer secuenceIdNum = Integer.parseInt(getSequenceId()) + 1;
      addRangeFilter(query, LOGTIME, getLogTime(), null);
      addRangeFilter(query, SEQUENCE_ID, secuenceIdNum.toString(), null);
    }
    query.setRows(request.getNumberRows());
    return query;
  }

  @Override
  public Sort sort(ServiceLogTruncatedRequest request) {
    String scrollType = request.getScrollType();
    Sort.Direction direction;
    if (LogSearchConstants.SCROLL_TYPE_AFTER.equals(scrollType)) {
      direction = Sort.Direction.ASC;
    } else {
      direction = Sort.Direction.DESC;
    }
    Sort.Order logtimeSortOrder = new Sort.Order(direction, LOGTIME);
    Sort.Order secuqnceIdSortOrder = new Sort.Order(direction, SEQUENCE_ID);
    return new Sort(logtimeSortOrder, secuqnceIdSortOrder);
  }

  @Override
  public SimpleQuery createQuery() {
    return new SimpleQuery();
  }

  @Override
  public LogType getLogType() {
    return LogType.SERVICE;
  }

  public String getSequenceId() {
    return sequenceId;
  }

  public void setSequenceId(String sequenceId) {
    this.sequenceId = sequenceId;
  }

  public String getLogTime() {
    return logTime;
  }

  public void setLogTime(String logTime) {
    this.logTime = logTime;
  }
}
