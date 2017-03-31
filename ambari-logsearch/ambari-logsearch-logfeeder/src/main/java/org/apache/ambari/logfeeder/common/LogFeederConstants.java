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
package org.apache.ambari.logfeeder.common;

public class LogFeederConstants {

  public static final String ALL = "all";
  public static final String LOGFEEDER_FILTER_NAME = "log_feeder_config";
  public static final String LOG_LEVEL_UNKNOWN = "UNKNOWN";
  
  // solr fields
  public static final String SOLR_LEVEL = "level";
  public static final String SOLR_COMPONENT = "type";
  public static final String SOLR_HOST = "host";

  // UserConfig Constants History
  public static final String VALUES = "jsons";
  public static final String ROW_TYPE = "rowtype";
  
  // S3 Constants
  public static final String S3_PATH_START_WITH = "s3://";
  public static final String S3_PATH_SEPARATOR = "/";
}
