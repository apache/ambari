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
  // Log Levels
  public static final String INFO = "INFO";
  public static final String WARN = "WARN";
  public static final String DEBUG = "DEBUG";
  public static final String ERROR = "ERROR";
  public static final String TRACE = "TRACE";
  public static final String FATAL = "FATAL";
  public static final String UNKNOWN = "UNKNOWN";
  
  public static final String[] SUPPORTED_LOG_LEVEL ={FATAL,ERROR,WARN,INFO,DEBUG,TRACE,UNKNOWN};

  // Application Constants
  public static final String HOST = "H";
  public static final String SERVICE = "S";
  public static final String COMPONENT = "C";
  public static final String SCROLL_TYPE_AFTER = "after";
  public static final String SCROLL_TYPE_BEFORE = "before";

  // UserConfig Constants
  public static final String ID = "id";
  public static final String USER_NAME = "username";
  public static final String VALUES = "jsons";
  public static final String FILTER_NAME = "filtername";
  public static final String ROW_TYPE = "rowtype";
  public static final String USER_CONFIG_DASHBOARD = "dashboard";
  public static final String USER_CONFIG_HISTORY = "history";
  public static final String COMPOSITE_KEY = "composite_filtername-username";
  public static final String SHARE_NAME_LIST = "share_username_list";

  // SOLR Document Constants for ServiceLogs
  public static final String BUNDLE_ID = "bundle_id";
  public static final String LOGTIME = "logtime";
  public static final String SEQUNCE_ID = "seq_num";
  public static final String SOLR_COMPONENT = "type";
  public static final String SOLR_LOG_MESSAGE = "log_message";
  public static final String SOLR_KEY_LOG_MESSAGE = "key_log_message";
  public static final String SOLR_HOST = "host";
  public static final String SOLR_LEVEL = "level";
  public static final String SOLR_THREAD_NAME = "thread_name";
  public static final String SOLR_LOGGER_NAME = "logger_name";
  public static final String SOLR_FILE = "file";
  public static final String SOLR_LINE_NUMBER = "line_number";
  public static final String SOLR_PATH = "path";

  //SOLR Document Constant for audit log
  public static final String AUDIT_COMPONENT = "repo";
  public static final String AUDIT_EVTTIME = "evtTime";
  public static final String AUDIT_REQUEST_USER = "reqUser";

  // Operator's
  public static final String MINUS_OPERATOR = "-";
  public static final String NO_OPERATOR = "";


  //operation
  public static final String EXCLUDE_QUERY = "excludeQuery";
  public static final String INCLUDE_QUERY = "includeQuery";
  public static final String COLUMN_QUERY = "columnQuery";

  //URL PARAMS
  public static final String GLOBAL_START_TIME = "globalStartTime";
  public static final String GLOBAL_END_TIME = "globalEndTime";


  // Seprator's
  public static final String I_E_SEPRATOR = "\\|i\\:\\:e\\|";

  //SUFFIX
  public static final String UI_SUFFIX = "@UI@";
  public static final String SOLR_SUFFIX = "@Solr@";
  public static final String NGRAM_SUFFIX = "ngram_";
  public static final String DEFAULT_SERVICE_COLUMN_SUFFIX = "service"; 
  public static final String DEFAULT_AUDIT_COLUMN_SUFFIX = "audit";

  //Date Format for SOLR
  public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
  public static final String SOLR_DATE_FORMAT_PREFIX_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  //Solr Order By
  public static final String ASCENDING_ORDER = "asc";
  public static final String DESCENDING_ORDER = "desc";

  //Solr Facet Sort By
  public static final String FACET_INDEX = "index";
  public static final String FACET_COUNT = "count";

  // logfeeder 
  public static final String LOGFEEDER_FILTER_NAME = "log_feeder_config";
  public static final String LIST_SEPARATOR = ",";
  
  public static final String SORT = "sort";
  public static final String FL = "fl";
  
  //Facet Constant
  public static final String FACET_FIELD = "facet.field";
  public static final String FACET_MINCOUNT = "facet.mincount";
  public static final String FACET_JSON_FIELD = "json.facet";
  public static final String FACET_PIVOT = "facet.pivot";
  public static final String FACET_PIVOT_MINCOUNT = "facet.pivot.mincount";
  public static final String FACET_DATE = "facet.date";
  public static final String FACET_DATE_START = "facet.date.start";
  public static final String FACET_DATE_END = "facet.date.end";
  public static final String FACET_DATE_GAP = "facet.date.gap";
  public static final String FACET_RANGE = "facet.range";
  public static final String FACET_RANGE_START = "facet.range.start";
  public static final String FACET_RANGE_END = "facet.range.end";
  public static final String FACET_RANGE_GAP = "facet.range.gap";
  public static final String FACET_GROUP = "group";
  public static final String FACET_GROUP_MAIN = "group.main";
  public static final String FACET_GROUP_FIELD = "group.field"; 
  public static final String FACET_LIMIT = "facet.limit";
  

}