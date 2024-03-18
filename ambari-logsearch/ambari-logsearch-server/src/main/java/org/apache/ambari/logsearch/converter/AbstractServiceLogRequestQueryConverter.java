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
import org.apache.ambari.logsearch.model.request.impl.BaseLogRequest;
import org.springframework.data.solr.core.query.Query;

import java.util.List;

import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.COMPONENT;

public abstract class AbstractServiceLogRequestQueryConverter<REQUEST_TYPE extends BaseLogRequest, QUERY_TYPE extends Query>
  extends AbstractLogRequestQueryConverter<REQUEST_TYPE, QUERY_TYPE>  {

  @Override
  public LogType getLogType() {
    return LogType.SERVICE;
  }

  @Override
  public void addComponentFilters(REQUEST_TYPE request, QUERY_TYPE query) {
    List<String> includeTypes = splitValueAsList(request.getMustBe(), ",");
    List<String> excludeTypes = splitValueAsList(request.getMustNot(), ",");
    addInFilterQuery(query, COMPONENT, includeTypes);
    addInFilterQuery(query, COMPONENT, excludeTypes, true);
  }
}
