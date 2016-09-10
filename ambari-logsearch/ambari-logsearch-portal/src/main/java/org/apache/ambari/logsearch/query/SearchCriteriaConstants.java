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
package org.apache.ambari.logsearch.query;

public class SearchCriteriaConstants {

  private SearchCriteriaConstants() {
  }

  public static final String PARAM_FIELD = "field";
  public static final String PARAM_UNIT = "unit";
  public static final String PARAM_INCLUDE_MESSAGE = "iMessage";
  public static final String PARAM_EXCLUDE_MESSAGE = "eMessage";
  public static final String PARAM_MUST_BE_STRING = "includeString";
  public static final String PARAM_MUST_NOT_STRING = "unselectComp";
  public static final String PARAM_EXCLUDE_QUERY = "excludeQuery";
  public static final String PARAM_INCLUDE_QUERY = "includeQuery";
  public static final String PARAM_START_TIME = "startTime";
  public static final String PARAM_END_TIME = "endTime";

  public static final String PARAM_IS_LAST_PAGE = "isLastPage";

  public static final String PARAM_GLOBAL_START_TIME = "globalStartTime";
  public static final String PARAM_GLOBAL_END_TIME = "globalEndTime";

  public static final String PARAM_X_AXIS = "xAxis";
  public static final String PARAM_Y_AXIS = "yAxis";
  public static final String PARAM_STACK_BY = "stackBy";
  public static final String PARAM_FROM = "from";
  public static final String PARAM_TO = "to";

  public static final String PARAM_COMPONENT_NAME = "component_name";
  public static final String PARAM_HOST_NAME = "host_name";
  public static final String PARAM_FILE_NAME = "file_name";
  public static final String PARAM_BUNDLE_ID = "bundle_id";
  public static final String PARAM_SELECT_COMP = "selectComp";
  public static final String PARAM_LEVEL = "level";

  public static final String PARAM_ID = "id";
  public static final String PARAM_SCROLL_TYPE = "scrollType";
  public static final String PARAM_NUMBER_ROWS = "numberRows";

  public static final String PARAM_FORMAT = "format";
  public static final String PARAM_UTC_OFFSET = "utcOffset";
  public static final String PARAM_KEYWORD = "keyword";
  public static final String PARAM_SOURCE_LOG_ID = "sourceLogId";
  public static final String PARAM_KEYWORD_TYPE = "keywordType";
  public static final String PARAM_TOKEN = "token";

  public static final String PARAM_USER_NAME = "username";
  public static final String PARAM_FILTER_NAME = "filtername";
  public static final String PARAM_ROW_TYPE = "rowtype";

}
