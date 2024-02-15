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
import java.util.HashMap;
import java.util.Map;

import static org.apache.ambari.logsearch.solr.SolrConstants.CommonLogConstants.*;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.INT_DYNAMIC_FIELDS;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.LONG_DYNAMIC_FIELDS;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.BOOLEAN_DYNAMIC_FIELDS;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.STRING_DYNAMIC_FIELDS;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.KEY_DYNAMIC_FIELDS;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.STORED_TOKEN_DYNAMIC_FIELDS;
import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.WS_DYNAMIC_FIELDS;

public class SolrCommonLogData implements CommonLogData {

  @Field(ID)
  private String id;

  @Field(BUNDLE_ID)
  private String bundleId;

  @Field(CASE_ID)
  private String caseId;

  @Field(CLUSTER)
  private String cluster;

  @Field(SEQUENCE_ID)
  private Long seqNum;

  @Field(LOG_MESSAGE)
  private String logMessage;

  @Field(LOGFILE_LINE_NUMBER)
  private Integer logFileLineNumber;

  @Field(EVENT_DURATION_MD5)
  private Long eventDurationMs;

  @Field(FILE)
  private String file;

  @Field(TYPE)
  private String type;

  @Field(EVENT_COUNT)
  private Long eventCount;

  @Field(EVENT_MD5)
  private String eventMd5;

  @Field(MESSAGE_MD5)
  private String messageMd5;

  @Field(TTL)
  private String ttl;

  @Field(EXPIRE_AT)
  private Date expire;

  @Field(VERSION)
  private Long version;

  @Field(ROUTER_FIELD)
  private Integer routerField;

  @Field(STORED_TOKEN_DYNAMIC_FIELDS)
  private Map<String, Object> stdDynamicFields;

  @Field(KEY_DYNAMIC_FIELDS)
  private Map<String, Object> keyDynamicFields;

  @Field(WS_DYNAMIC_FIELDS)
  private Map<String, Object> wsDynamicFields;

  @Field(INT_DYNAMIC_FIELDS)
  private Map<String, Object> intDynamicFields;

  @Field(LONG_DYNAMIC_FIELDS)
  private Map<String, Object> longDynamicFields;

  @Field(STRING_DYNAMIC_FIELDS)
  private Map<String, Object> stringDynamicFields;

  @Field(BOOLEAN_DYNAMIC_FIELDS)
  private Map<String, Object> booleanDynamicFields;

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

  @Override
  public Map<String, Object> getAllDynamicFields() {
    Map<String, Object> allDynamicFields = new HashMap<>();

    if (stdDynamicFields != null) {
      allDynamicFields.putAll(stdDynamicFields);
    }
    if (keyDynamicFields != null) {
      allDynamicFields.putAll(keyDynamicFields);
    }
    if (wsDynamicFields != null) {
      allDynamicFields.putAll(wsDynamicFields);
    }

    if (intDynamicFields != null) {
      allDynamicFields.putAll(intDynamicFields);
    }

    if (longDynamicFields != null) {
      allDynamicFields.putAll(longDynamicFields);
    }

    if (stringDynamicFields != null) {
      allDynamicFields.putAll(stringDynamicFields);
    }

    if (booleanDynamicFields != null) {
      allDynamicFields.putAll(booleanDynamicFields);
    }
    
    return allDynamicFields;
  }

  public void setStdDynamicFields(Map<String, Object> stdDynamicFields) {
    this.stdDynamicFields = stdDynamicFields;
  }

  public void setKeyDynamicFields(Map<String, Object> keyDynamicFields) {
    this.keyDynamicFields = keyDynamicFields;
  }

  public void setWsDynamicFields(Map<String, Object> wsDynamicFields) {
    this.wsDynamicFields = wsDynamicFields;
  }

  public void setIntDynamicFields(Map<String, Object> intDynamicFields) {
    this.intDynamicFields = intDynamicFields;
  }

  public void setLongDynamicFields(Map<String, Object> longDynamicFields) {
    this.longDynamicFields = longDynamicFields;
  }

  public void setStringDynamicFields(Map<String, Object> stringDynamicFields) {
    this.stringDynamicFields = stringDynamicFields;
  }

  public void setBooleanDynamicFields(Map<String, Object> booleanDynamicFields) {
    this.booleanDynamicFields = booleanDynamicFields;
  }
}
