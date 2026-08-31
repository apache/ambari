/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.agent;

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;

public class ComponentStatusAgentReport extends AgentReport<List<ComponentStatus>> {
  private final HeartBeatHandler hh;
  private final HostLevelParamsHolder hostLevelParamsHolder;
  private final RecoveryTopologyManager recoveryTopologyManager;
  private final Long hostId;
  private final String sessionId;
  private final boolean snapshotComplete;

  public ComponentStatusAgentReport(HeartBeatHandler hh, String hostName, List<ComponentStatus> componentStatuses,
      HostLevelParamsHolder hostLevelParamsHolder, RecoveryTopologyManager recoveryTopologyManager,
      Long hostId, String sessionId, boolean snapshotComplete) {
    super(hostName, componentStatuses);
    this.hh = hh;
    this.hostLevelParamsHolder = hostLevelParamsHolder;
    this.recoveryTopologyManager = recoveryTopologyManager;
    this.hostId = hostId;
    this.sessionId = sessionId;
    this.snapshotComplete = snapshotComplete;
  }

  @Override
  protected void process(List<ComponentStatus> report, String hostName) throws AmbariException {
    if (!recoveryTopologyManager.isActiveSession(hostId, sessionId)) {
      return;
    }

    hh.handleComponentReportStatus(report, hostName);
    if (!report.isEmpty() || snapshotComplete) {
      recoveryTopologyManager.componentStateUpdated();
      if (snapshotComplete) {
        recoveryTopologyManager.markSnapshotComplete(hostId, sessionId);
      }
      hostLevelParamsHolder.updateRecoveryTopology(hostName);
    }
  }
}
