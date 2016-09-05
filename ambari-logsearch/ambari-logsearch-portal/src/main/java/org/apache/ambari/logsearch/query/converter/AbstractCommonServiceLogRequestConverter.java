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

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.impl.BaseServiceLogRequest;
import org.apache.ambari.logsearch.query.model.CommonSearchCriteria;
import org.apache.commons.lang.StringEscapeUtils;

public abstract class AbstractCommonServiceLogRequestConverter<SOURCE extends BaseServiceLogRequest, RESULT extends CommonSearchCriteria>
  extends AbstractCommonSearchRequestConverter<SOURCE, RESULT> {

  @Override
  public RESULT convertToSearchCriteria(SOURCE request) {
    RESULT criteria = createCriteria(request);
    criteria.addParam("advanceSearch", StringEscapeUtils.unescapeXml(request.getAdvancedSearch()));
    criteria.addParam("q", request.getQuery());
    criteria.addParam("treeParams", StringEscapeUtils.unescapeHtml(request.getTreeParams()));
    criteria.addParam("level", request.getLevel());
    criteria.addParam("gMustNot", request.getgMustNot());
    criteria.addParam("from", request.getFrom());
    criteria.addParam("to", request.getTo());
    criteria.addParam("selectComp", request.getMustBe());
    criteria.addParam("unselectComp", request.getMustNot());
    criteria.addParam("iMessage", StringEscapeUtils.unescapeXml(request.getiMessage()));
    criteria.addParam("gEMessage", StringEscapeUtils.unescapeXml(request.getgEMessage()));
    criteria.addParam("eMessage", StringEscapeUtils.unescapeXml(request.getgEMessage()));
    criteria.addParam(LogSearchConstants.BUNDLE_ID, request.getBundleId());
    criteria.addParam("host_name", request.getHostName());
    criteria.addParam("component_name", request.getComponentName());
    criteria.addParam("file_name", request.getFileName());
    criteria.addParam("startDate", request.getStartTime());
    criteria.addParam("endDate", request.getEndTime());
    criteria.addParam("excludeQuery", StringEscapeUtils.unescapeXml(request.getExcludeQuery()));
    criteria.addParam("includeQuery", StringEscapeUtils.unescapeXml(request.getIncludeQuery()));
    return criteria;
  }

  public abstract RESULT createCriteria(SOURCE request);
}
