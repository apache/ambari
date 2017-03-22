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
package org.apache.ambari.logsearch.model.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface CommonLogData extends LogData {

  @JsonProperty("id")
  String getId();

  void setId(String id);

  @JsonProperty("case_id")
  String getCaseId();

  void setCaseId(String caseId);

  @JsonProperty("log_message")
  String getLogMessage();

  void setLogMessage(String logMessage);

  @JsonProperty("bundle_id")
  String getBundleId();

  void setBundleId(String bundleId);

  @JsonProperty("logfile_line_number")
  Integer getLogFileLineNumber();

  void setLogFileLineNumber(Integer logFileLineNumber);

  @JsonProperty("file")
  String getFile();

  void setFile(String file);

  @JsonProperty("type")
  String getType();

  void setType(String type);

  @JsonProperty("seq_num")
  Long getSeqNum();

  void setSeqNum(Long seqNum);

  @JsonProperty("message_md5")
  String getMessageMd5();

  void setMessageMd5(String messageMd5);

  @JsonProperty("cluster")
  String getCluster();

  void setCluster(String cluster);

  @JsonProperty("event_count")
  Long getEventCount();

  void setEventCount(Long eventCount);

  @JsonProperty("event_md5")
  String getEventMd5();

  void setEventMd5(String eventMd5);

  @JsonProperty("event_dur_ms")
  Long getEventDurationMs();

  void setEventDurationMs(Long eventDurationMs);

  @JsonProperty("_ttl_")
  String getTtl();

  void setTtl(String ttl);

  @JsonProperty("_expire_at_")
  Date getExpire();

  void setExpire(Date expire);

  @JsonProperty("_version_")
  Long getVersion();

  void setVersion(Long version);

  @JsonProperty("_router_field_")
  Integer getRouterField();

  void setRouterField(Integer routerField);
  
  @JsonAnyGetter
  Map<String, Object> getAllDynamicFields();
}
