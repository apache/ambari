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

import org.apache.ambari.logsearch.model.request.impl.BaseServiceLogRequest;
import org.apache.ambari.logsearch.query.model.CommonServiceLogSearchCriteria;
import org.apache.commons.lang.StringEscapeUtils;

public abstract class AbstractCommonServiceLogRequestConverter<SOURCE extends BaseServiceLogRequest, RESULT extends CommonServiceLogSearchCriteria>
  extends AbstractCommonSearchRequestConverter<SOURCE, RESULT> {

  @Override
  public RESULT convertToSearchCriteria(SOURCE request) {
    RESULT criteria = createCriteria(request);
    // TODO: check are these used from the UI or not?
    criteria.addParam("q", request.getQuery());
    criteria.addParam("unselectComp", request.getMustNot());

    criteria.setLevel(request.getLevel());
    criteria.setFrom(request.getFrom());
    criteria.setTo(request.getTo());
    criteria.setSelectComp(request.getMustBe());
    criteria.setBundleId(request.getBundleId());
    criteria.setHostName(request.getHostName());
    criteria.setComponentName(request.getComponentName());
    criteria.setFileName(request.getFileName());
    criteria.setStartTime(request.getStartTime());
    criteria.setEndTime(request.getEndTime());
    criteria.setExcludeQuery(StringEscapeUtils.unescapeXml(request.getExcludeQuery()));
    criteria.setIncludeQuery(StringEscapeUtils.unescapeXml(request.getIncludeQuery()));
    return criteria;
  }

  public abstract RESULT createCriteria(SOURCE request);
}
