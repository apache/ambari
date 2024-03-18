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
package org.apache.ambari.logsearch.config.api.model.loglevelfilter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogLevelFilter {

  private String label;
  private List<String> hosts;
  private List<String> defaultLevels;
  private List<String> overrideLevels;
  private Date expiryTime;

  public LogLevelFilter() {
    hosts = new ArrayList<String>();
    defaultLevels = new ArrayList<String>();
    overrideLevels = new ArrayList<String>();
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  public List<String> getDefaultLevels() {
    return defaultLevels;
  }

  public void setDefaultLevels(List<String> defaultLevels) {
    this.defaultLevels = defaultLevels;
  }

  public List<String> getOverrideLevels() {
    return overrideLevels;
  }

  public void setOverrideLevels(List<String> overrideLevels) {
    this.overrideLevels = overrideLevels;
  }

  public Date getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(Date expiryTime) {
    this.expiryTime = expiryTime;
  }

}
