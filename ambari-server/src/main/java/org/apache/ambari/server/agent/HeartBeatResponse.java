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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Controller to Agent response data model.
 */
public class HeartBeatResponse {

  private long responseId;

  private List<ExecutionCommand> executionCommands = new ArrayList<ExecutionCommand>();
  private List<StatusCommand> statusCommands = new ArrayList<StatusCommand>();
  private List<CancelCommand> cancelCommands = new ArrayList<CancelCommand>();

  private RegistrationCommand registrationCommand;

  private boolean restartAgent = false;
  private boolean hasMappedComponents = false;

  /**
   * A mapping between cluster name and the alert defintion hash for that
   * cluster. The alert definition hash for a cluster is a hashed value of all
   * of the UUIDs for each alert definition that the agent host should be
   * scheduling. If any of the alert definitions change, their UUID will change
   * which will cause this hash value to change.
   */
  private Map<String, String> alertDefinitionHashes = null;

  @JsonProperty("responseId")
  public long getResponseId() {
    return responseId;
  }

  @JsonProperty("responseId")
  public void setResponseId(long responseId) {
    this.responseId=responseId;
  }

  @JsonProperty("executionCommands")
  public List<ExecutionCommand> getExecutionCommands() {
    return executionCommands;
  }

  @JsonProperty("executionCommands")
  public void setExecutionCommands(List<ExecutionCommand> executionCommands) {
    this.executionCommands = executionCommands;
  }

  @JsonProperty("statusCommands")
  public List<StatusCommand> getStatusCommands() {
    return statusCommands;
  }

  @JsonProperty("statusCommands")
  public void setStatusCommands(List<StatusCommand> statusCommands) {
    this.statusCommands = statusCommands;
  }

  @JsonProperty("cancelCommands")
  public List<CancelCommand> getCancelCommands() {
    return cancelCommands;
  }

  @JsonProperty("cancelCommands")
  public void setCancelCommands(List<CancelCommand> cancelCommands) {
    this.cancelCommands = cancelCommands;
  }

  @JsonProperty("registrationCommand")
  public RegistrationCommand getRegistrationCommand() {
    return registrationCommand;
  }

  @JsonProperty("registrationCommand")
  public void setRegistrationCommand(RegistrationCommand registrationCommand) {
    this.registrationCommand = registrationCommand;
  }

  @JsonProperty("restartAgent")
  public boolean isRestartAgent() {
    return restartAgent;
  }

  @JsonProperty("restartAgent")
  public void setRestartAgent(boolean restartAgent) {
    this.restartAgent = restartAgent;
  }

  @JsonProperty("hasMappedComponents")
  public boolean hasMappedComponents() {
    return hasMappedComponents;
  }

  @JsonProperty("hasMappedComponents")
  public void setHasMappedComponents(boolean hasMappedComponents) {
    this.hasMappedComponents = hasMappedComponents;
  }

  @JsonProperty("alertDefinitionHashes")
  public Map<String, String> getAlertDefinitionHash() {
    return alertDefinitionHashes;
  }

  @JsonProperty("alertDefinitionHashes")
  public void setAlertDefinitionHash(Map<String, String> alertDefinitionHashes) {
    this.alertDefinitionHashes = alertDefinitionHashes;
  }

  public void addExecutionCommand(ExecutionCommand execCmd) {
    executionCommands.add(execCmd);
  }

  public void addStatusCommand(StatusCommand statCmd) {
    statusCommands.add(statCmd);
  }

  public void addCancelCommand(CancelCommand cancelCmd) {
    cancelCommands.add(cancelCmd);
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("HeartBeatResponse{");
    buffer.append("responseId=").append(responseId);
    buffer.append(", executionCommands=").append(executionCommands);
    buffer.append(", statusCommands=").append(statusCommands);
    buffer.append(", cancelCommands=").append(cancelCommands);
    buffer.append(", registrationCommand=").append(registrationCommand);
    buffer.append(", restartAgent=").append(restartAgent);
    buffer.append(", alertDefinitionHashes=").append(alertDefinitionHashes);
    buffer.append('}');
    return buffer.toString();
  }
}
