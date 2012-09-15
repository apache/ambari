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

package org.apache.ambari.server.state.live;

import org.apache.ambari.server.state.ConfigVersion;
import org.apache.ambari.server.state.StackVersion;

public class ServiceComponentNodeState {

  private ConfigVersion configVersion;
  private StackVersion stackVersion;
  private ServiceComponentNodeLiveState state;

  public ServiceComponentNodeState(ConfigVersion configVersion,
      StackVersion stackVersion, ServiceComponentNodeLiveState state) {
    super();
    this.configVersion = configVersion;
    this.stackVersion = stackVersion;
    this.state = state;
  }

  public ServiceComponentNodeState() {
    super();
    this.configVersion = null;
    this.stackVersion = null;
    this.state = ServiceComponentNodeLiveState.INIT;
  }


  /**
   * @return the configVersion
   */
  public ConfigVersion getConfigVersion() {
    return configVersion;
  }
  /**
   * @param configVersion the configVersion to set
   */
  public void setConfigVersion(ConfigVersion configVersion) {
    this.configVersion = configVersion;
  }
  /**
   * @return the stackVersion
   */
  public StackVersion getStackVersion() {
    return stackVersion;
  }
  /**
   * @param stackVersion the stackVersion to set
   */
  public void setStackVersion(StackVersion stackVersion) {
    this.stackVersion = stackVersion;
  }
  /**
   * @return the state
   */
  public ServiceComponentNodeLiveState getLiveState() {
    return state;
  }
  /**
   * @param state the state to set
   */
  public void setState(ServiceComponentNodeLiveState state) {
    this.state = state;
  }


  public String toString() {
    String out = "[";
    out += " StackVersion=";
    if (stackVersion != null) {
      out += stackVersion;
    } else {
      out += "null";
    }
    out += ", ConfigVersion=";
    if (configVersion != null) {
      out += configVersion;
    } else {
      out += "null";
    }
    out += ", state=" + state;
    return out;
  }
}
