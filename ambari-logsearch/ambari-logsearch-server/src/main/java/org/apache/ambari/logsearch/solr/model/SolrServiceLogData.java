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

import org.apache.ambari.logsearch.model.response.ServiceLogData;
import org.apache.solr.client.solrj.beans.Field;

import java.util.Date;
import java.util.Map;

import static org.apache.ambari.logsearch.solr.SolrConstants.ServiceLogConstants.*;

public class SolrServiceLogData extends SolrCommonLogData implements ServiceLogData {

  @Field(LEVEL)
  private String level;

  @Field(LINE_NUMBER)
  private Integer lineNumber;

  @Field(LOGTIME)
  private Date logTime;

  @Field(COMPONENT)
  private String type;

  @Field(IP)
  private String ip;

  @Field(PATH)
  private String path;

  @Field(HOST)
  private String host;

  @Field(GROUP)
  private String group;

  @Field(LOGGER_NAME)
  private String loggerName;

  @Field(METHOD)
  private String method;

  @Field(SDI_DYNAMIC_FIELDS)
  private Map<String, Object> sdiDynamicFields;

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getIp() {
    return ip;
  }

  @Override
  public void setIp(String ip) {
    this.ip = ip;
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
  public String getHost() {
    return host;
  }

  @Override
  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public String getGroup() {
    return group;
  }

  @Override
  public void setGroup(String group) {
    this.group = group;
  }

  @Override
  public Date getLogTime() {
    return logTime;
  }

  @Override
  public void setLogTime(Date logTime) {
    this.logTime = logTime;
  }

  @Override
  public Integer getLineNumber() {
    return lineNumber;
  }

  @Override
  public void setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
  }

  @Override
  public String getLevel() {
    return level;
  }

  @Override
  public void setLevel(String level) {
    this.level = level;
  }

  @Override
  public String getLoggerName() {
    return loggerName;
  }

  @Override
  public void setLoggerName(String loggerName) {
    this.loggerName = loggerName;
  }

  @Override
  public String getMethod() {
    return method;
  }

  @Override
  public void setMethod(String method) {
    this.method = method;
  }

  public void setSdiDynamicFields(Map<String, Object> sdiDynamicFields) {
    this.sdiDynamicFields = sdiDynamicFields;
  }

  @Override
  public Map<String, Object> getAllDynamicFields() {
    Map<String, Object> dynamicFieldsMap = super.getAllDynamicFields();
    if (sdiDynamicFields != null) {
      dynamicFieldsMap.putAll(sdiDynamicFields);
    }
    return dynamicFieldsMap;
  }
}
