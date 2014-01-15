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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;

import java.util.Map;

public interface AmbariCustomCommandExecutionHelper {
  void validateCustomCommand(ExecuteActionRequest actionRequest) throws AmbariException;

  void addAction(ExecuteActionRequest actionRequest, Stage stage,
                 HostsMap hostsMap, Map<String, String> hostLevelParams)
      throws AmbariException;

  void addServiceCheckActionImpl(Stage stage,
                                 String hostname, String smokeTestRole,
                                 long nowTimestamp,
                                 String serviceName,
                                 String componentName,
                                 Map<String, String> roleParameters,
                                 HostsMap hostsMap,
                                 Map<String, String> hostLevelParams)
              throws AmbariException;

  void createHostAction(Cluster cluster,
                        Stage stage, ServiceComponentHost scHost,
                        Map<String, Map<String, String>> configurations,
                        Map<String, Map<String, String>> configTags,
                        RoleCommand roleCommand,
                        Map<String, String> commandParams,
                        ServiceComponentHostEvent event)
                      throws AmbariException;
}
