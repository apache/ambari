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
package org.apache.ambari.server.actionmanager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.live.ClustersImpl;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.Test;

public class TestActionManager {

  private long requestId = 23;
  private long stageId = 31;
  
  @Test
  public void testActionResponse() {
    ActionDBAccessor db = new ActionDBInMemoryImpl();
    ActionManager am = new ActionManager(5000, 1200000, new ActionQueue(),
        new ClustersImpl(), db);
    String hostname = "host1";
    populateActionDB(db, hostname);
    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setRole("HBASE_MASTER");
    cr.setStatus("COMPLETED");
    cr.setExitCode(215);
    reports.add(cr);
    am.actionResponse(hostname, reports);
    assertEquals(215, am.getAction(requestId, stageId).getHostAction(hostname)
        .getRoleCommands().get(0).getExitCode());
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
        .getHostAction(hostname).getRoleCommands().get(0).getStatus());
  }

  private void populateActionDB(ActionDBAccessor db, String hostname) {
    Stage s = new Stage(requestId, "/a/b", "cluster1");
    s.setStageId(stageId);
    HostAction ha = new HostAction(hostname);
    HostRoleCommand cmd = new HostRoleCommand(Role.HBASE_MASTER,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis()));
    ha.addHostRoleCommand(cmd);
    s.addHostAction(hostname, ha);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    db.persistActions(stages);
  }
}
