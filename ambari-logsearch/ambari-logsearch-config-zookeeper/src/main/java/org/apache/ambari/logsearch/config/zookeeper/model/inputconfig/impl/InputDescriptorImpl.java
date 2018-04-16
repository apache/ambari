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

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
  name = "Input",
  description = "The input element in the input configuration contains a list of input descriptions, each describing one source of input.\n" +
                "\n" +
                "The general elements in the json are the following:"
)
public abstract class InputDescriptorImpl implements InputDescriptor {
  @ShipperConfigElementDescription(
    path = "/input/[]/type",
    type = "string",
    description = "The log id for this source.",
    examples = {"zookeeper", "ambari_server"}
  )
  @Expose
  private String type;

  @ShipperConfigElementDescription(
    path = "/input/[]/rowtype",
    type = "string",
    description = "The type of the row.",
    examples = {"service", "audit"}
  )
  @Expose
  private String rowtype;

  @ShipperConfigElementDescription(
    path = "/input/[]/group",
    type = "string",
    description = "Group of the input type.",
    examples = {"Ambari", "Yarn"}
  )
  @Expose
  private String group;

  @ShipperConfigElementDescription(
    path = "/input/[]/path",
    type = "string",
    description = "The path of the source, may contain '*' characters too.",
    examples = {"/var/log/ambari-logsearch-logfeeder/logsearch-logfeeder.json", "/var/log/zookeeper/zookeeper*.log"}
  )
  @Expose
  private String path;

  @ShipperConfigElementDescription(
    path = "/input/[]/add_fields",
    type = "dictionary",
    description = "The element contains field_name: field_value pairs which will be added to each rows data.",
    examples = {"\"cluster\":\"cluster_name\""}
  )
  @Expose
  @SerializedName("add_fields")
  private Map<String, String> addFields;
  
  @ShipperConfigElementDescription(
    path = "/input/[]/source",
    type = "dictionary",
    description = "The type of the input source.",
    examples = {"file", "s3_file"}
  )
  @Expose
  private String source;
  
  @ShipperConfigElementDescription(
    path = "/input/[]/tail",
    type = "boolean",
    description = "The input should check for only the latest file matching the pattern, not all of them.",
    examples = {"true", "false"},
    defaultValue = "true"
  )
  @Expose
  private Boolean tail;
  
  @ShipperConfigElementDescription(
    path = "/input/[]/gen_event_md5",
    type = "boolean",
    description = "Generate an event_md5 field for each row by creating a hash of the row data.",
    examples = {"true", "false"},
    defaultValue = "true"
  )
  @Expose
  @SerializedName("gen_event_md5")
  private Boolean genEventMd5;
  
  @ShipperConfigElementDescription(
    path = "/input/[]/use_event_md5_as_id",
    type = "boolean",
    description = "Generate an id for each row by creating a hash of the row data.",
    examples = {"true", "false"},
    defaultValue = "false"
  )
  @Expose
  @SerializedName("use_event_md5_as_id")
  private Boolean useEventMd5AsId;

  @ShipperConfigElementDescription(
    path = "/input/[]/cache_enabled",
    type = "boolean",
    description = "Allows the input to use a cache to filter out duplications.",
    examples = {"true", "false"},
    defaultValue = "false"
  )
  @Expose
  @SerializedName("cache_enabled")
  private Boolean cacheEnabled;

  @ShipperConfigElementDescription(
    path = "/input/[]/cache_key_field",
    type = "string",
    description = "Specifies the field for which to use the cache to find duplications of.",
    examples = {"some_field_prone_to_repeating_value"},
    defaultValue = "log_message"
  )
  @Expose
  @SerializedName("cache_key_field")
  private String cacheKeyField;

  @ShipperConfigElementDescription(
    path = "/input/[]/cache_last_dedup_enabled",
    type = "boolean",
    description = "Allow to filter out entries which are same as the most recent one irrelevant of it's time.",
    examples = {"true", "false"},
    defaultValue = "false"
  )
  @Expose
  @SerializedName("cache_last_dedup_enabled")
  private Boolean cacheLastDedupEnabled;

  @ShipperConfigElementDescription(
    path = "/input/[]/cache_size",
    type = "integer",
    description = "The number of entries to store in the cache.",
    examples = {"50"},
    defaultValue = "100"
  )
  @Expose
  @SerializedName("cache_size")
  private Integer cacheSize;

  @ShipperConfigElementDescription(
    path = "/input/[]/cache_dedup_interval",
    type = "integer",
    description = "The maximum interval in ms which may pass between two identical log messages to filter the latter out.",
    examples = {"500"},
    defaultValue = "1000"
  )
  @Expose
  @SerializedName("cache_dedup_interval")
  private Long cacheDedupInterval;

  @ShipperConfigElementDescription(
    path = "/input/[]/is_enabled",
    type = "boolean",
    description = "A flag to show if the input should be used.",
    examples = {"true", "false"},
    defaultValue = "true"
  )
  @Expose
  @SerializedName("is_enabled")
  private Boolean isEnabled;


  @ShipperConfigElementDescription(
    path = "/input/[]/init_default_fields",
    type = "boolean",
    description = "Init default fields (ip, path etc.) before applying the filter.",
    examples = {"true", "false"},
    defaultValue = "false"
  )
  @Expose
  @SerializedName("init_default_fields")
  private Boolean initDefaultFields;

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

  @Override
  public String getGroup() {
    return this.group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  @Override
  public Boolean isInitDefaultFields() {
    return this.initDefaultFields;
  }

  public void setInitDefaultFields(Boolean initDefaultFields) {
    this.initDefaultFields = initDefaultFields;
  }
}
