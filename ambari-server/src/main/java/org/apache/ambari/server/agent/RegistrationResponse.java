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

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 * Controller to Agent response data model.
 *
 */
public class RegistrationResponse {
  @JsonProperty("response")
  private RegistrationStatus response;
  
  //Response id to start with, usually zero.
  @JsonProperty("responseId")
  private long responseId;
  
  @JsonProperty("statusCommands")
  private List<StatusCommand> statusCommands = null;

  public RegistrationStatus getResponseStatus() {
    return response;
  }

  public void setResponseStatus(RegistrationStatus response) {
    this.response = response;
  }

  public List<StatusCommand> getStatusCommands() {
    return statusCommands;
  }

  public void setStatusCommands(List<StatusCommand> statusCommands) {
    this.statusCommands = statusCommands;
  }

  public long getResponseId() {
    return responseId;
  }

  public void setResponseId(long responseId) {
    this.responseId = responseId;
  }

  @Override
  public String toString() {
    return "RegistrationResponse{" +
            "response=" + response +
            ", responseId=" + responseId +
            ", statusCommands=" + statusCommands +
            '}';
  }
}
