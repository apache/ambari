/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import java.util.Objects;

import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/** Runtime state for one component instance that blocks recovery. */
public class RecoveryConfigDependency {
  @SerializedName("component_name")
  @JsonProperty("component_name")
  private final String componentName;

  @SerializedName("service_name")
  @JsonProperty("service_name")
  private final String serviceName;

  @SerializedName("host_name")
  @JsonProperty("host_name")
  private final String hostName;

  @SerializedName("current_state")
  @JsonProperty("current_state")
  private final State currentState;

  @SerializedName("desired_state")
  @JsonProperty("desired_state")
  private final State desiredState;

  @SerializedName("required_state")
  @JsonProperty("required_state")
  private final State requiredState;

  private final boolean fresh;

  public RecoveryConfigDependency(ServiceComponentHost sch, State requiredState, boolean fresh) {
    componentName = sch.getServiceComponentName();
    serviceName = sch.getServiceName();
    hostName = sch.getHostName();
    currentState = sch.getState();
    desiredState = sch.getDesiredState();
    this.requiredState = requiredState;
    this.fresh = fresh;
  }

  public String getComponentName() {
    return componentName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getHostName() {
    return hostName;
  }

  public State getCurrentState() {
    return currentState;
  }

  public State getDesiredState() {
    return desiredState;
  }

  public State getRequiredState() {
    return requiredState;
  }

  public boolean isFresh() {
    return fresh;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecoveryConfigDependency that = (RecoveryConfigDependency) o;
    return fresh == that.fresh && Objects.equals(componentName, that.componentName)
        && Objects.equals(serviceName, that.serviceName) && Objects.equals(hostName, that.hostName)
        && currentState == that.currentState && desiredState == that.desiredState
        && requiredState == that.requiredState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, serviceName, hostName, currentState, desiredState, requiredState, fresh);
  }
}
