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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.List;
import java.util.Set;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;

/**
 * Defines how to build stages.
 */
public abstract class StageWrapperBuilder {

  /**
   * Adds a processing component that will be built into stage wrappers.
   *
   * @param hostsType   the hosts, along with their type
   * @param service     the service name
   * @param clientOnly  whether the service is client only, no service checks
   * @param pc          the ProcessingComponent derived from the upgrade pack
   */
  public abstract void add(HostsType hostsType, String service, boolean clientOnly, ProcessingComponent pc);

  /**
   * Builds the stage wrappers.
   */
  public abstract List<StageWrapper> build();

  /**
   * Consistently formats a string.
   * @param prefix
   * @param component
   * @param hosts
   * @return the prepared string
   */
  protected String getStageText(String prefix, String component, Set<String> hosts) {
    return String.format("%s %s on %s%s",
        prefix,
        component,
        1 == hosts.size() ? hosts.iterator().next() : Integer.valueOf(hosts.size()),
        1 == hosts.size() ? "" : " hosts");
  }


}
