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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class TestActionManager {

  private long requestId = 23;
  private long stageId = 31;
  private Injector injector;
  private String hostname = "host1";
  private String clusterName = "cluster1";

  private Clusters clusters;

  @Before
  public void setup() throws AmbariException {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    clusters.addHost(hostname);
    clusters.getHost(hostname).persist();
    clusters.addCluster(clusterName);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testActionResponse() {
    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);
    ActionManager am = new ActionManager(5000, 1200000, new ActionQueue(),
        clusters, db);
    populateActionDB(db, hostname);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(hostname, "HBASE_MASTER", HostRoleStatus.QUEUED);
    db.hostRoleScheduled(stage, hostname, "HBASE_MASTER");
    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setTaskId(1);
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setRole("HBASE_MASTER");
    cr.setStatus("COMPLETED");
    cr.setStdErr("ERROR");
    cr.setStdOut("OUTPUT");
    cr.setExitCode(215);
    reports.add(cr);
    am.processTaskResponse(hostname, reports);
    assertEquals(215,
        am.getAction(requestId, stageId).getExitCode(hostname, "HBASE_MASTER"));
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
        .getHostRoleStatus(hostname, "HBASE_MASTER"));
    assertEquals(
        "ERROR",
        am.getAction(requestId, stageId)
            .getHostRoleCommand(hostname, "HBASE_MASTER").getStderr());
    assertEquals(
        "OUTPUT",
        am.getAction(requestId, stageId)
            .getHostRoleCommand(hostname, "HBASE_MASTER").getStdout());
    
  }
  
  @Test
  public void testLargeLogs() {
    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);
    ActionManager am = new ActionManager(5000, 1200000, new ActionQueue(),
        clusters, db);
    populateActionDB(db, hostname);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(hostname, "HBASE_MASTER", HostRoleStatus.QUEUED);
    db.hostRoleScheduled(stage, hostname, "HBASE_MASTER");
    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setTaskId(1);
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setRole("HBASE_MASTER");
    cr.setStatus("COMPLETED");
    String errLog = Arrays.toString(new byte[100000]);
    String outLog = Arrays.toString(new byte[110000]);
    cr.setStdErr(errLog);
    cr.setStdOut(outLog);
    cr.setExitCode(215);
    reports.add(cr);
    am.processTaskResponse(hostname, reports);
    assertEquals(215,
        am.getAction(requestId, stageId).getExitCode(hostname, "HBASE_MASTER"));
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
        .getHostRoleStatus(hostname, "HBASE_MASTER"));
    assertEquals(
        errLog.length(),
        am.getAction(requestId, stageId)
            .getHostRoleCommand(hostname, "HBASE_MASTER").getStderr().length());
    assertEquals(
        outLog.length(),
        am.getAction(requestId, stageId)
            .getHostRoleCommand(hostname, "HBASE_MASTER").getStdout().length());
    
  }

  private void populateActionDB(ActionDBAccessor db, String hostname) {
    Stage s = new Stage(requestId, "/a/b", "cluster1");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostname, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis(),
            new HashMap<String, String>()), "cluster1", "HBASE");
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    db.persistActions(stages);
  }
}
