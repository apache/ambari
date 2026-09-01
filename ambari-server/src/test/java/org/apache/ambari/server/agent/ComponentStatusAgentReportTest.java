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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;

import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;
import org.junit.Test;

public class ComponentStatusAgentReportTest {
  @Test
  public void testReportFromSupersededSessionIsIgnored() throws Exception {
    HeartBeatHandler heartbeatHandler = mock(HeartBeatHandler.class);
    HostLevelParamsHolder hostLevelParamsHolder = mock(HostLevelParamsHolder.class);
    RecoveryTopologyManager recoveryTopologyManager = new RecoveryTopologyManager();
    Long hostId = 1L;
    recoveryTopologyManager.beginAgentSession(hostId, "current-session");

    ComponentStatusAgentReport report = new ComponentStatusAgentReport(heartbeatHandler, "host1",
        Collections.emptyList(), hostLevelParamsHolder, recoveryTopologyManager, hostId,
        "superseded-session", true);

    report.process();

    verifyNoInteractions(heartbeatHandler, hostLevelParamsHolder);
  }
}
