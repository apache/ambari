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

import org.apache.ambari.logsearch.model.request.impl.BaseAuditLogRequest;
import org.apache.ambari.logsearch.query.model.CommonSearchCriteria;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractCommonAuditLogRequestConverter<SOURCE extends BaseAuditLogRequest, RESULT extends CommonSearchCriteria>
  extends AbstractCommonSearchRequestConverter<SOURCE, RESULT> {

  @Override
  public RESULT convertToSearchCriteria(SOURCE request) {
    RESULT criteria = createCriteria(request);
    criteria.addParam("q", request.getQuery());
    criteria.addParam("columnQuery", StringEscapeUtils.unescapeXml(request.getColumnQuery()));
    criteria.addParam("gEMessage", StringEscapeUtils.unescapeXml(request.getgEMessage()));
    criteria.setIncludeMessage(StringEscapeUtils.unescapeXml(request.getiMessage()));
    criteria.setExcludeMessage(StringEscapeUtils.unescapeXml(request.getgEMessage()));
    criteria.setMustBe(request.getMustBe());
    criteria.setMustNot(request.getMustNot());
    criteria.setExcludeQuery(StringEscapeUtils.unescapeXml(request.getExcludeQuery()));
    criteria.setIncludeQuery(StringEscapeUtils.unescapeXml(request.getIncludeQuery()));
    criteria.setStartTime(request.getFrom());
    criteria.setEndTime(request.getTo());
    return criteria;
  }

  public abstract RESULT createCriteria(SOURCE request);
}
