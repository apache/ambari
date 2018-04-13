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
package org.apache.ambari.logfeeder.conf;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@Lazy
public class LogEntryCacheConfig {

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CACHE_ENABLED_PROPERTY,
    description = "Enables the usage of a cache to avoid duplications.",
    examples = {"true"},
    defaultValue = LogFeederConstants.DEFAULT_CACHE_ENABLED + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.CACHE_ENABLED_PROPERTY + ":" + LogFeederConstants.DEFAULT_CACHE_ENABLED + "}")
  private boolean cacheEnabled;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CACHE_KEY_FIELD_PROPERTY,
    description = "The field which's value should be cached and should be checked for repetitions.",
    examples = {"some_field_prone_to_repeating_value"},
    defaultValue = LogFeederConstants.DEFAULT_CACHE_KEY_FIELD,
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.CACHE_KEY_FIELD_PROPERTY + ":" + LogFeederConstants.DEFAULT_CACHE_KEY_FIELD + "}")
  private String cacheKeyField;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CACHE_SIZE_PROPERTY,
    description = "The number of log entries to cache in order to avoid duplications.",
    examples = {"50"},
    defaultValue = LogFeederConstants.DEFAULT_CACHE_SIZE + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.CACHE_SIZE_PROPERTY + ":" + LogFeederConstants.DEFAULT_CACHE_SIZE + "}")
  private Integer cacheSize;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CACHE_LAST_DEDUP_ENABLED_PROPERTY,
    description = "Enable filtering directly repeating log entries irrelevant of the time spent between them.",
    examples = {"true"},
    defaultValue = LogFeederConstants.DEFAULT_CACHE_LAST_DEDUP_ENABLED + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.CACHE_LAST_DEDUP_ENABLED_PROPERTY + ":" + LogFeederConstants.DEFAULT_CACHE_LAST_DEDUP_ENABLED + "}")
  private boolean cacheLastDedupEnabled;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CACHE_DEDUP_INTERVAL_PROPERTY,
    description = "Maximum number of milliseconds between two identical messages to be filtered out.",
    examples = {"500"},
    defaultValue = LogFeederConstants.DEFAULT_CACHE_DEDUP_INTERVAL + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.CACHE_DEDUP_INTERVAL_PROPERTY + ":" + LogFeederConstants.DEFAULT_CACHE_DEDUP_INTERVAL + "}")
  private String cacheDedupInterval;

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  public void setCacheEnabled(boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
  }

  public String getCacheKeyField() {
    return cacheKeyField;
  }

  public void setCacheKeyField(String cacheKeyField) {
    this.cacheKeyField = cacheKeyField;
  }

  public Integer getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(Integer cacheSize) {
    this.cacheSize = cacheSize;
  }

  public boolean isCacheLastDedupEnabled() {
    return this.cacheLastDedupEnabled;
  }

  public void setCacheLastDedupEnabled(boolean cacheLastDedupEnabled) {
    this.cacheLastDedupEnabled = cacheLastDedupEnabled;
  }

  public String getCacheDedupInterval() {
    return cacheDedupInterval;
  }

  public void setCacheDedupInterval(String cacheDedupInterval) {
    this.cacheDedupInterval = cacheDedupInterval;
  }
}
