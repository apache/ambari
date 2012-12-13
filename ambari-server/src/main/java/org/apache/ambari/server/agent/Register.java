/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.agent;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 * Data model for Ambari Agent to send heartbeat to Ambari Controller.
 *
 */
public class Register {
  private int responseId = -1;
  private long timestamp;
  private String hostname;
  private HostInfo hardwareProfile;
  private String publicHostname;

  @JsonProperty("responseId")
  public int getResponseId() {
    return responseId;
  }

  @JsonProperty("responseId")
  public void setResponseId(int responseId) {
    this.responseId=responseId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getHostname() {
    return hostname;
  }
  
  public HostInfo getHardwareProfile() {
    return hardwareProfile;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setHardwareProfile(HostInfo hardwareProfile) {
    this.hardwareProfile = hardwareProfile;
  }
  
  public String getPublicHostname() {
    return publicHostname;
  }
  
  public void setPublicHostname(String name) {
    publicHostname = name;
  }

  @Override
  public String toString() {
    String ret = "responseId=" + responseId + "\n" +
             "timestamp=" + timestamp + "\n" +
             "hostname="  + hostname + "\n";

    if (hardwareProfile != null)
      ret = ret + "hardwareprofile=" + this.hardwareProfile.toString();
    return ret;
  }
}
