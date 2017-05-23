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

package org.apache.ambari.logsearch.model.common;

import java.util.Map;

import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(Include.NON_NULL)
public abstract class LSServerInput {
  private final String type;
  private final String rowtype;
  private final String path;
  
  @JsonProperty("add_fields")
  private final Map<String, String> addFields;
  
  private final String source;
  private final Boolean tail;
  
  @JsonProperty("gen_event_md5")
  private final Boolean genEventMd5;
  
  @JsonProperty("use_event_md5_as_id")
  private final Boolean useEventMd5AsId;
  
  @JsonProperty("start_position")
  private final String startPosition;
  
  @JsonProperty("cache_enabled")
  private final Boolean cacheEnabled;
  
  @JsonProperty("cache_key_field")
  private final String cacheKeyField;
  
  @JsonProperty("cache_last_dedup_enabled")
  private final Boolean cacheLastDedupEnabled;
  
  @JsonProperty("cache_size")
  private final Integer cacheSize;
  
  @JsonProperty("cache_dedup_interval")
  private final Long cacheDedupInterval;
  
  @JsonProperty("is_enabled")
  private final Boolean isEnabled;
  
  public LSServerInput(InputDescriptor inputDescriptor) {
    this.type = inputDescriptor.getType();
    this.rowtype = inputDescriptor.getRowtype();
    this.path = inputDescriptor.getPath();
    this.addFields = inputDescriptor.getAddFields();
    this.source = inputDescriptor.getSource();
    this.tail = inputDescriptor.isTail();
    this.genEventMd5 = inputDescriptor.isGenEventMd5();
    this.useEventMd5AsId = inputDescriptor.isUseEventMd5AsId();
    this.startPosition = inputDescriptor.getStartPosition();
    this.cacheEnabled = inputDescriptor.isCacheEnabled();
    this.cacheKeyField = inputDescriptor.getCacheKeyField();
    this.cacheLastDedupEnabled = inputDescriptor.getCacheLastDedupEnabled();
    this.cacheSize = inputDescriptor.getCacheSize();
    this.cacheDedupInterval = inputDescriptor.getCacheDedupInterval();
    this.isEnabled = inputDescriptor.isEnabled();
  }

  public String getType() {
    return type;
  }

  public String getRowtype() {
    return rowtype;
  }

  public String getPath() {
    return path;
  }

  public Map<String, String> getAddFields() {
    return addFields;
  }

  public String getSource() {
    return source;
  }

  public Boolean getTail() {
    return tail;
  }

  public Boolean getGenEventMd5() {
    return genEventMd5;
  }

  public Boolean getUseEventMd5AsId() {
    return useEventMd5AsId;
  }

  public String getStartPosition() {
    return startPosition;
  }

  public Boolean getCacheEnabled() {
    return cacheEnabled;
  }

  public String getCacheKeyField() {
    return cacheKeyField;
  }

  public Boolean getCacheLastDedupEnabled() {
    return cacheLastDedupEnabled;
  }

  public Integer getCacheSize() {
    return cacheSize;
  }

  public Long getCacheDedupInterval() {
    return cacheDedupInterval;
  }

  public Boolean getIsEnabled() {
    return isEnabled;
  }
}
