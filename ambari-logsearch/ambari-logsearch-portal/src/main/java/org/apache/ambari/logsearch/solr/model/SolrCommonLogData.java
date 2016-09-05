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
package org.apache.ambari.logsearch.solr.model;

import org.apache.ambari.logsearch.model.response.CommonLogData;
import org.apache.solr.client.solrj.beans.Field;

import java.util.Date;

public class SolrCommonLogData implements CommonLogData {

  @Field("id")
  private String id;

  @Field("bundle_id")
  private String bundleId;

  @Field("case_id")
  private String caseId;

  @Field("cluster")
  private String cluster;

  @Field("seq_num")
  private Long seqNum;

  @Field("log_message")
  private String logMessage;

  @Field("logfile_line_number")
  private Integer logFileLineNumber;

  @Field("event_dur_m5")
  private Long eventDurationMs;

  @Field("file")
  private String file;

  @Field("type")
  private String type;

  @Field("event_count")
  private Long eventCount;

  @Field("event_md5")
  private String eventMd5;

  @Field("message_md5")
  private String messageMd5;

  @Field("_ttl_")
  private String ttl;

  @Field("_expire_at_")
  private Date expire;

  @Field("_version_")
  private Long version;

  @Field("_router_field_")
  private Integer routerField;

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getCaseId() {
    return this.caseId;
  }

  @Override
  public void setCaseId(String caseId) {
    this.caseId = caseId;
  }

  @Override
  public String getLogMessage() {
    return this.logMessage;
  }

  @Override
  public String getBundleId() {
    return bundleId;
  }

  @Override
  public void setBundleId(String bundleId) {
    this.bundleId = bundleId;
  }

  @Override
  public Integer getLogFileLineNumber() {
    return logFileLineNumber;
  }

  @Override
  public void setLogFileLineNumber(Integer logFileLineNumber) {
    this.logFileLineNumber = logFileLineNumber;
  }

  @Override
  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  @Override
  public Long getEventDurationMs() {
    return eventDurationMs;
  }

  @Override
  public void setEventDurationMs(Long eventDurationMs) {
    this.eventDurationMs = eventDurationMs;
  }

  @Override
  public String getFile() {
    return file;
  }

  @Override
  public void setFile(String file) {
    this.file = file;
  }

  @Override
  public Long getSeqNum() {
    return seqNum;
  }

  @Override
  public void setSeqNum(Long seqNum) {
    this.seqNum = seqNum;
  }

  @Override
  public String getMessageMd5() {
    return messageMd5;
  }

  @Override
  public void setMessageMd5(String messageMd5) {
    this.messageMd5 = messageMd5;
  }

  @Override
  public String getEventMd5() {
    return eventMd5;
  }

  @Override
  public void setEventMd5(String eventMd5) {
    this.eventMd5 = eventMd5;
  }

  @Override
  public String getCluster() {
    return cluster;
  }

  @Override
  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  @Override
  public Long getEventCount() {
    return eventCount;
  }

  @Override
  public void setEventCount(Long eventCount) {
    this.eventCount = eventCount;
  }

  @Override
  public String getTtl() {
    return this.ttl;
  }

  @Override
  public void setTtl(String ttl) {
    this.ttl = ttl;
  }

  @Override
  public Date getExpire() {
    return expire;
  }

  @Override
  public void setExpire(Date expire) {
    this.expire = expire;
  }

  @Override
  public Long getVersion() {
    return version;
  }

  @Override
  public void setVersion(Long version) {
    this.version = version;
  }

  @Override
  public Integer getRouterField() {
    return this.routerField;
  }

  @Override
  public void setRouterField(Integer routerField) {
    this.routerField = routerField;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }
}
