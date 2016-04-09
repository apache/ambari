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
package org.apache.ambari.logsearch.common;

import java.io.Serializable;

public class RequestContext implements Serializable {
  private static final long serialVersionUID = -7083383106845193385L;
  String ipAddress = null;
  String msaCookie = null;
  String userAgent = null;
  String requestURL = null;
  String serverRequestId = null;
  String clientTimeOffset;
  boolean isSync = true;
  long startTime = System.currentTimeMillis();

  /**
   * @return the ipAddress
   */
  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * @param ipAddress the ipAddress to set
   */
  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  /**
   * @return the msaCookie
   */
  public String getMsaCookie() {
    return msaCookie;
  }

  /**
   * @param msaCookie the msaCookie to set
   */
  public void setMsaCookie(String msaCookie) {
    this.msaCookie = msaCookie;
  }

  /**
   * @return the userAgent
   */
  public String getUserAgent() {
    return userAgent;
  }

  /**
   * @param userAgent the userAgent to set
   */
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  /**
   * @return the serverRequestId
   */
  public String getServerRequestId() {
    return serverRequestId;
  }

  /**
   * @param serverRequestId the serverRequestId to set
   */
  public void setServerRequestId(String serverRequestId) {
    this.serverRequestId = serverRequestId;
  }

  /**
   * @return the isSync
   */
  public boolean isSync() {
    return isSync;
  }

  /**
   * @param isSync the isSync to set
   */
  public void setSync(boolean isSync) {
    this.isSync = isSync;
  }

  /**
   * @return the requestURL
   */
  public String getRequestURL() {
    return requestURL;
  }

  /**
   * @param requestURL the requestURL to set
   */
  public void setRequestURL(String requestURL) {
    this.requestURL = requestURL;
  }

  /**
   * @return the startTime
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * @param startTime the startTime to set
   */
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "RequestContext [ipAddress=" + ipAddress + ", msaCookie=" + msaCookie + ", userAgent=" + userAgent
      + ", requestURL=" + requestURL + ",serverRequestId=" + serverRequestId + ", isSync=" + isSync
      + ", startTime=" + startTime + "]";
  }
}
