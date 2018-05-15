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

  // Event History Constants History
  public static final String VALUES = "jsons";
  public static final String ROW_TYPE = "rowtype";
  
  // S3 Constants
  public static final String S3_PATH_START_WITH = "s3://";
  public static final String S3_PATH_SEPARATOR = "/";

  public static final String IN_MEMORY_TIMESTAMP = "in_memory_timestamp";

  public static final String LOGFEEDER_PROPERTIES_FILE = "logfeeder.properties";
  public static final String CLUSTER_NAME_PROPERTY = "cluster.name";
  public static final String TMP_DIR_PROPERTY = "logfeeder.tmp.dir";

  public static final String METRICS_COLLECTOR_PROTOCOL_PROPERTY = "logfeeder.metrics.collector.protocol";
  public static final String METRICS_COLLECTOR_PORT_PROPERTY = "logfeeder.metrics.collector.port";
  public static final String METRICS_COLLECTOR_HOSTS_PROPERTY = "logfeeder.metrics.collector.hosts";
  public static final String METRICS_COLLECTOR_PATH_PROPERTY = "logfeeder.metrics.collector.path";

  public static final String LOG_FILTER_ENABLE_PROPERTY = "logfeeder.log.filter.enable";
  public static final String INCLUDE_DEFAULT_LEVEL_PROPERTY = "logfeeder.include.default.level";
  public static final String SOLR_IMPLICIT_ROUTING_PROPERTY = "logfeeder.solr.implicit.routing";

  public static final String CONFIG_DIR_PROPERTY = "logfeeder.config.dir";
  public static final String CONFIG_FILES_PROPERTY = "logfeeder.config.files";

  public static final String SIMULATE_INPUT_NUMBER_PROPERTY = "logfeeder.simulate.input_number";
  public static final int DEFAULT_SIMULATE_INPUT_NUMBER = 0;
  public static final String SIMULATE_LOG_LEVEL_PROPERTY = "logfeeder.simulate.log_level";
  public static final String DEFAULT_SIMULATE_LOG_LEVEL = "WARN";
  public static final String SIMULATE_NUMBER_OF_WORDS_PROPERTY = "logfeeder.simulate.number_of_words";
  public static final int DEFAULT_SIMULATE_NUMBER_OF_WORDS = 1000;
  public static final String SIMULATE_MIN_LOG_WORDS_PROPERTY = "logfeeder.simulate.min_log_words";
  public static final int DEFAULT_SIMULATE_MIN_LOG_WORDS = 5;
  public static final String SIMULATE_MAX_LOG_WORDS_PROPERTY = "logfeeder.simulate.max_log_words";
  public static final int DEFAULT_SIMULATE_MAX_LOG_WORDS = 5;
  public static final String SIMULATE_SLEEP_MILLISECONDS_PROPERTY = "logfeeder.simulate.sleep_milliseconds";
  public static final int DEFAULT_SIMULATE_SLEEP_MILLISECONDS = 10000;
  public static final String SIMULATE_LOG_IDS_PROPERTY = "logfeeder.simulate.log_ids";

  public static final String SOLR_KERBEROS_ENABLE_PROPERTY = "logfeeder.solr.kerberos.enable";
  public static final boolean DEFAULT_SOLR_KERBEROS_ENABLE = false;
  public static final String DEFAULT_SOLR_JAAS_FILE = "/etc/security/keytabs/logsearch_solr.service.keytab";
  public static final String SOLR_JAAS_FILE_PROPERTY = "logfeeder.solr.jaas.file";

  public static final String CACHE_ENABLED_PROPERTY = "logfeeder.cache.enabled";
  public static final boolean DEFAULT_CACHE_ENABLED = false;
  public static final String CACHE_KEY_FIELD_PROPERTY = "logfeeder.cache.key.field";
  public static final String DEFAULT_CACHE_KEY_FIELD = "log_message";
  public static final String CACHE_SIZE_PROPERTY = "logfeeder.cache.size";
  public static final int DEFAULT_CACHE_SIZE = 100;
  public static final String CACHE_LAST_DEDUP_ENABLED_PROPERTY = "logfeeder.cache.last.dedup.enabled";
  public static final boolean DEFAULT_CACHE_LAST_DEDUP_ENABLED = false;
  public static final String CACHE_DEDUP_INTERVAL_PROPERTY = "logfeeder.cache.dedup.interval";
  public static final long DEFAULT_CACHE_DEDUP_INTERVAL = 1000;

  public static final String CHECKPOINT_FOLDER_PROPERTY = "logfeeder.checkpoint.folder";
  public static final String CHECKPOINT_EXTENSION_PROPERTY = "logfeeder.checkpoint.extension";
  public static final String DEFAULT_CHECKPOINT_EXTENSION = ".cp";

}
