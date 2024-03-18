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
package org.apache.ambari.logsearch.model.request;

import io.swagger.annotations.ApiParam;
import org.apache.ambari.logsearch.common.LogSearchConstants;

import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.START_TIME_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.END_TIME_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.START_INDEX_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.PAGE_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.PAGE_SIZE_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.SORT_BY_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.SORT_TYPE_D;


public interface CommonSearchParamDefinition {

  String getStartIndex();

  @ApiParam(value = START_INDEX_D, name = LogSearchConstants.REQUEST_PARAM_START_INDEX)
  void setStartIndex(String startIndex);

  String getPage();

  @ApiParam(value = PAGE_D, name = LogSearchConstants.REQUEST_PARAM_PAGE)
  void setPage(String page);

  String getPageSize();

  @ApiParam(value = PAGE_SIZE_D, name = LogSearchConstants.REQUEST_PARAM_PAGE_SIZE)
  void setPageSize(String pageSize);

  String getSortBy();

  @ApiParam(value = SORT_BY_D, name = LogSearchConstants.REQUEST_PARAM_SORT_BY)
  void setSortBy(String sortBy);

  String getSortType();

  @ApiParam(value = SORT_TYPE_D, name = LogSearchConstants.REQUEST_PARAM_SORT_TYPE)
  void setSortType(String sortType);

  String getStartTime();

  @ApiParam(value = START_TIME_D, name = LogSearchConstants.REQUEST_PARAM_START_TIME)
  void setStartTime(String startTime);

  String getEndTime();

  @ApiParam(value = END_TIME_D, name = LogSearchConstants.REQUEST_PARAM_END_TIME)
  void setEndTime(String endTime);
}
