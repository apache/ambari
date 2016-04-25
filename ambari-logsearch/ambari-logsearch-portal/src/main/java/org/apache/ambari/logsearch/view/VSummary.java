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
package org.apache.ambari.logsearch.view;

import java.util.List;

public class VSummary {

  protected List<VHost> hosts;
  protected String levels;
  protected String format;
  protected String numberLogs;
  protected String from;
  protected String to;
  protected String includeString;
  protected String excludeString;
  
  public VSummary(){
    includeString = "-";
    excludeString = "-";
  }

  public String getIncludeString() {
    return includeString;
  }

  public void setIncludeString(String includeString) {
    this.includeString = includeString;
  }

  public String getExcludeString() {
    return excludeString;
  }

  public void setExcludeString(String excludeString) {
    this.excludeString = excludeString;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public List<VHost> getHosts() {
    return hosts;
  }

  public void setHosts(List<VHost> hosts) {
    this.hosts = hosts;
  }

  public String getLevels() {
    return levels;
  }

  public void setLevels(String levels) {
    this.levels = levels;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getNumberLogs() {
    return numberLogs;
  }

  public void setNumberLogs(String numberLogs) {
    this.numberLogs = numberLogs;
  }

}
