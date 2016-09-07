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
package org.apache.ambari.logsearch.query.converter;

import org.apache.ambari.logsearch.model.request.impl.CommonSearchRequest;
import org.apache.ambari.logsearch.query.model.CommonSearchCriteria;
import org.apache.commons.lang.StringUtils;

import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_GLOBAL_END_TIME;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_GLOBAL_START_TIME;

public abstract class AbstractCommonSearchRequestConverter<SOURCE extends CommonSearchRequest, RESULT extends CommonSearchCriteria>
  extends AbstractConverterAware<SOURCE, RESULT> {

  @Override
  public RESULT convert(SOURCE source) {
    RESULT criteria = convertToSearchCriteria(source);
    addDefaultParams(source, criteria);
    return criteria;
  }

  public abstract RESULT convertToSearchCriteria(SOURCE source);

  private void addDefaultParams(SOURCE request, RESULT criteria) {
    criteria.setStartIndex(StringUtils.isNumeric(request.getStartIndex()) ? new Integer(request.getStartIndex()) : 0);
    criteria.setPage(StringUtils.isNumeric(request.getPage()) ? new Integer(request.getPage()) : 0);
    criteria.setMaxRows(StringUtils.isNumeric(request.getPageSize()) ? new Integer(request.getPageSize()) : 50);
    criteria.setSortBy(request.getSortBy());
    criteria.setSortType(request.getSortType());
    if (StringUtils.isNotEmpty(request.getStartTime())){
      criteria.setGlobalStartTime(request.getStartTime());
      criteria.getUrlParamMap().put(PARAM_GLOBAL_START_TIME, request.getStartTime());
    }
    if (StringUtils.isNotEmpty(request.getEndTime())){
      criteria.setGlobalEndTime(request.getEndTime());
      criteria.getUrlParamMap().put(PARAM_GLOBAL_END_TIME, request.getEndTime());
    }
  }
}
