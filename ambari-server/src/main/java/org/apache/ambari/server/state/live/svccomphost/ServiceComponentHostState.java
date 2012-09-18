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

package org.apache.ambari.server.state.live.svccomphost;

import org.apache.ambari.server.state.ConfigVersion;
import org.apache.ambari.server.state.StackVersion;

public class ServiceComponentHostState {

  private final int hashCodePrime = 131;

  private ConfigVersion configVersion;
  private StackVersion stackVersion;
  private ServiceComponentHostLiveState liveState;

  public ServiceComponentHostState(ConfigVersion configVersion,
      StackVersion stackVersion, ServiceComponentHostLiveState state) {
    super();
    this.configVersion = configVersion;
    this.stackVersion = stackVersion;
    this.liveState = state;
  }

  public ServiceComponentHostState() {
    super();
    this.configVersion = null;
    this.stackVersion = null;
    this.liveState = ServiceComponentHostLiveState.INIT;
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
  public ServiceComponentHostLiveState getLiveState() {
    return liveState;
  }
  /**
   * @param state the state to set
   */
  public void setState(ServiceComponentHostLiveState state) {
    this.liveState = state;
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
    out += ", state=" + liveState;
    return out;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof ServiceComponentHostState)) {
      return false;
    }
    if (this == object) {
      return true;
    }
    ServiceComponentHostState s = (ServiceComponentHostState) object;

    if (configVersion != null ?
        !configVersion.equals(s.configVersion) : s.configVersion != null) {
      return false;
    }
    if (stackVersion != null ?
        !stackVersion.equals(s.stackVersion) : s.stackVersion != null) {
      return false;
    }
    return liveState.equals(s.liveState);
  }

  @Override
  public int hashCode() {
    int result = configVersion != null ? configVersion.hashCode() : 0;
    result += hashCodePrime * result +
        ( stackVersion != null ? stackVersion.hashCode() : 0 );
    result += liveState.hashCode();
    return result;
  }

}
