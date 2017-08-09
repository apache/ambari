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

package org.apache.ambari.logsearch.common;

public class LogSearchConstants {

  public static final String LOGSEARCH_APPLICATION_NAME = "logsearch";
  public static final String LOGSEARCH_PROPERTIES_FILE = "logsearch.properties";
  public static final String LOGSEARCH_SESSION_ID = "LOGSEARCHSESSIONID";

  // Log Levels
  public static final String INFO = "INFO";
  public static final String WARN = "WARN";
  public static final String DEBUG = "DEBUG";
  public static final String ERROR = "ERROR";
  public static final String TRACE = "TRACE";
  public static final String FATAL = "FATAL";
  public static final String UNKNOWN = "UNKNOWN";
  
  public static final String[] SUPPORTED_LOG_LEVELS = {FATAL, ERROR, WARN, INFO, DEBUG, TRACE, UNKNOWN};

  // Application Constants
  public static final String HOST = "H";
  public static final String COMPONENT = "C";
  public static final String SCROLL_TYPE_AFTER = "after";
  public static final String SCROLL_TYPE_BEFORE = "before";

  // Seprator's
  public static final String I_E_SEPRATOR = "\\|i\\:\\:e\\|";

  //SUFFIX
  public static final String NGRAM_PREFIX = "ngram_";

  //Date Format for SOLR
  public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
  public static final String SOLR_DATE_FORMAT_PREFIX_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  //Solr Order By
  public static final String ASCENDING_ORDER = "asc";
  public static final String DESCENDING_ORDER = "desc";

  // logfeeder 
  public static final String LOGFEEDER_FILTER_NAME = "log_feeder_config";

  public static final String SORT = "sort";
  
  //Facet Constant
  public static final String FACET_FIELD = "facet.field";
  public static final String FACET_PIVOT = "facet.pivot";
  public static final String FACET_PIVOT_MINCOUNT = "facet.pivot.mincount";
  public static final String FACET_INDEX = "index";

  // Request params
  public static final String REQUEST_PARAM_XAXIS = "xAxis";
  public static final String REQUEST_PARAM_YAXIS = "yAxis";
  public static final String REQUEST_PARAM_STACK_BY = "stackBy";
  public static final String REQUEST_PARAM_UNIT = "unit";
  public static final String REQUEST_PARAM_TOP = "top";
  public static final String REQUEST_PARAM_CLUSTER_NAMES = "clusters";
  public static final String REQUEST_PARAM_BUNDLE_ID = "bundle_id";
  public static final String REQUEST_PARAM_START_INDEX = "startIndex";
  public static final String REQUEST_PARAM_PAGE = "page";
  public static final String REQUEST_PARAM_PAGE_SIZE = "pageSize";
  public static final String REQUEST_PARAM_SORT_BY = "sortBy";
  public static final String REQUEST_PARAM_SORT_TYPE = "sortType";
  public static final String REQUEST_PARAM_START_TIME = "start_time";
  public static final String REQUEST_PARAM_END_TIME = "end_time";
  public static final String REQUEST_PARAM_FROM = "from";
  public static final String REQUEST_PARAM_TO = "to";
  public static final String REQUEST_PARAM_FIELD = "field";
  public static final String REQUEST_PARAM_FORMAT = "format";
  public static final String REQUEST_PARAM_LAST_PAGE = "lastPage";
  public static final String REQUEST_PARAM_I_MESSAGE = "iMessage";
  public static final String REQUEST_PARAM_E_MESSAGE = "eMessage";
  public static final String REQUEST_PARAM_MUST_BE = "mustBe";
  public static final String REQUEST_PARAM_MUST_NOT = "mustNot";
  public static final String REQUEST_PARAM_INCLUDE_QUERY = "includeQuery";
  public static final String REQUEST_PARAM_EXCLUDE_QUERY = "excludeQuery";
  public static final String REQUEST_PARAM_ID = "id";
  public static final String REQUEST_PARAM_SCROLL_TYPE = "scrollType";
  public static final String REQUEST_PARAM_NUMBER_ROWS = "numberRows";
  public static final String REQUEST_PARAM_LEVEL = "level";
  public static final String REQUEST_PARAM_HOST_NAME = "host_name";
  public static final String REQUEST_PARAM_COMPONENT_NAME = "component_name";
  public static final String REQUEST_PARAM_FILE_NAME = "file_name";
  public static final String REQUEST_PARAM_KEYWORD = "find";
  public static final String REQUEST_PARAM_SOURCE_LOG_ID = "sourceLogId";
  public static final String REQUEST_PARAM_KEYWORD_TYPE = "keywordType";
  public static final String REQUEST_PARAM_TOKEN = "token";
  public static final String REQUEST_PARAM_FILTER_NAME = "filterName";
  public static final String REQUEST_PARAM_ROW_TYPE = "rowType";
  public static final String REQUEST_PARAM_UTC_OFFSET = "utcOffset";
  public static final String REQUEST_PARAM_HOSTS = "hostList";


}