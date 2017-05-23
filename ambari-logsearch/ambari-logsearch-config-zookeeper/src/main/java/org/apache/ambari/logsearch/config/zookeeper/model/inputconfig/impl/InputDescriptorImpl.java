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

package org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl;

import java.util.Map;

import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public abstract class InputDescriptorImpl implements InputDescriptor {
  @Expose
  private String type;

  @Expose
  private String rowtype;

  @Expose
  private String path;

  @Expose
  @SerializedName("add_fields")
  private Map<String, String> addFields;
  
  @Expose
  private String source;
  
  @Expose
  private Boolean tail;
  
  @Expose
  @SerializedName("gen_event_md5")
  private Boolean genEventMd5;
  
  @Expose
  @SerializedName("use_event_md5_as_id")
  private Boolean useEventMd5AsId;
  
  @Expose
  @SerializedName("start_position")
  private String startPosition;

  @Expose
  @SerializedName("cache_enabled")
  private Boolean cacheEnabled;

  @Expose
  @SerializedName("cache_key_field")
  private String cacheKeyField;

  @Expose
  @SerializedName("cache_last_dedup_enabled")
  private Boolean cacheLastDedupEnabled;

  @Expose
  @SerializedName("cache_size")
  private Integer cacheSize;

  @Expose
  @SerializedName("cache_dedup_interval")
  private Long cacheDedupInterval;

  @Expose
  @SerializedName("is_enabled")
  private Boolean isEnabled;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRowtype() {
    return rowtype;
  }

  public void setRowtype(String rowType) {
    this.rowtype = rowType;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Map<String, String> getAddFields() {
    return addFields;
  }

  public void setAddFields(Map<String, String> addFields) {
    this.addFields = addFields;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Boolean isTail() {
    return tail;
  }

  public void setTail(Boolean tail) {
    this.tail = tail;
  }

  public Boolean isGenEventMd5() {
    return genEventMd5;
  }

  public void setGenEventMd5(Boolean genEventMd5) {
    this.genEventMd5 = genEventMd5;
  }

  public Boolean isUseEventMd5AsId() {
    return useEventMd5AsId;
  }

  public void setUseEventMd5AsId(Boolean useEventMd5AsId) {
    this.useEventMd5AsId = useEventMd5AsId;
  }

  public String getStartPosition() {
    return startPosition;
  }

  public void setStartPosition(String startPosition) {
    this.startPosition = startPosition;
  }

  public Boolean isCacheEnabled() {
    return cacheEnabled;
  }

  public void setCacheEnabled(Boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
  }

  public String getCacheKeyField() {
    return cacheKeyField;
  }

  public void setCacheKeyField(String cacheKeyField) {
    this.cacheKeyField = cacheKeyField;
  }

  public Boolean getCacheLastDedupEnabled() {
    return cacheLastDedupEnabled;
  }

  public void setCacheLastDedupEnabled(Boolean cacheLastDedupEnabled) {
    this.cacheLastDedupEnabled = cacheLastDedupEnabled;
  }

  public Integer getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(Integer cacheSize) {
    this.cacheSize = cacheSize;
  }

  public Long getCacheDedupInterval() {
    return cacheDedupInterval;
  }

  public void setCacheDedupInterval(Long cacheDedupInterval) {
    this.cacheDedupInterval = cacheDedupInterval;
  }

  public Boolean isEnabled() {
    return isEnabled;
  }

  public void setIsEnabled(Boolean isEnabled) {
    this.isEnabled = isEnabled;
  }
}
