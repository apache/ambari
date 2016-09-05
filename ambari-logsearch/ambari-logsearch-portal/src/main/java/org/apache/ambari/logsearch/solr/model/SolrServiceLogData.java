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

public class SolrServiceLogData extends SolrCommonLogData implements ServiceLogData {

  @Field("level")
  private String level;

  @Field("line_number")
  private Integer lineNumber;

  @Field("logtime")
  private Date logTime;

  @Field("type")
  private String type;

  @Field("ip")
  private String ip;

  @Field("path")
  private String path;

  @Field("host")
  private String host;

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
}
