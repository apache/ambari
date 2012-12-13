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

import java.util.ArrayList;
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
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import static org.junit.Assert.*;

public class TestActionDBAccessorImpl {
  private static final Logger log = LoggerFactory.getLogger(TestActionDBAccessorImpl.class);

  private long requestId = 23;
  private long stageId = 31;
  private String hostName = "host1";
  private String clusterName = "cluster1";
  private Injector injector;
  ActionDBAccessor db;
  ActionManager am;

  @Inject
  private Clusters clusters;
  @Inject
  private ExecutionCommandDAO executionCommandDAO;
  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Before
  public void setup() throws AmbariException {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    clusters.addHost(hostName);
    clusters.getHost(hostName).persist();
    clusters.addCluster(clusterName);
    db = injector.getInstance(ActionDBAccessorImpl.class);
    am = new ActionManager(5000, 1200000, new ActionQueue(), clusters, db);
  }

  @After
  public void tearDown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testActionResponse() {
    String hostname = "host1";
    populateActionDB(db, hostname, requestId, stageId);
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
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    reports.add(cr);
    am.processTaskResponse(hostname, reports);
    assertEquals(215,
        am.getAction(requestId, stageId).getExitCode(hostname, "HBASE_MASTER"));
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
        .getHostRoleStatus(hostname, "HBASE_MASTER"));
    Stage s = db.getAllStages(requestId).get(0);
    assertEquals(HostRoleStatus.COMPLETED,s.getHostRoleStatus(hostname, "HBASE_MASTER"));
  }
  
  @Test
  public void testGetStagesInProgress() {
    String hostname = "host1";
    populateActionDB(db, hostname, requestId, stageId);
    populateActionDB(db, hostname, requestId, stageId+1);
    List<Stage> stages = db.getStagesInProgress();
    assertEquals(2, stages.size());
  }
  
  @Test
  public void testGetStagesInProgressWithFailures() {
    String hostname = "host1";
    populateActionDB(db, hostname, requestId, stageId);
    populateActionDB(db, hostname, requestId+1, stageId);
    db.abortOperation(requestId);
    List<Stage> stages = db.getStagesInProgress();
    assertEquals(1, stages.size());
    assertEquals(requestId+1, stages.get(0).getRequestId());
  }

  @Test
  public void testPersistActions() {
    populateActionDB(db, hostName, requestId, stageId);
    for (Stage stage : db.getAllStages(requestId)) {
      log.info("taskId={}" + stage.getExecutionCommands(hostName).get(0).
          getExecutionCommand().getTaskId());
      assertTrue(stage.getExecutionCommands(hostName).get(0).
          getExecutionCommand().getTaskId() > 0);
      assertTrue(executionCommandDAO.findByPK(stage.getExecutionCommands(hostName).
          get(0).getExecutionCommand().getTaskId()) != null);
    }
  }

  @Test
  public void testHostRoleScheduled() throws InterruptedException {
    populateActionDB(db, hostName, requestId, stageId);
    Stage stage = db.getAction(StageUtils.getActionId(requestId, stageId));
    assertEquals(HostRoleStatus.PENDING, stage.getHostRoleStatus(hostName, Role.HBASE_MASTER.toString()));
    List<HostRoleCommandEntity> entities=
        hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER);

    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    stage.setHostRoleStatus(hostName, Role.HBASE_MASTER.toString(), HostRoleStatus.QUEUED);

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER);
    assertEquals(HostRoleStatus.QUEUED, stage.getHostRoleStatus(hostName, Role.HBASE_MASTER.toString()));
    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    db.hostRoleScheduled(stage, hostName, Role.HBASE_MASTER.toString());

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER);
    assertEquals(HostRoleStatus.QUEUED, entities.get(0).getStatus());

    Thread thread = new Thread(){
      @Override
      public void run() {
        Stage stage1 = db.getAction("23-31");
        stage1.setHostRoleStatus(hostName, Role.HBASE_MASTER.toString(), HostRoleStatus.COMPLETED);
        db.hostRoleScheduled(stage1, hostName, Role.HBASE_MASTER.toString());
      }
    };

    thread.start();
    thread.join();

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER);
    assertEquals("Concurrent update failed", HostRoleStatus.COMPLETED, entities.get(0).getStatus());

  }

  @Test
  public void testUpdateHostRole() throws Exception {
    populateActionDB(db, hostName, requestId, stageId);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 50000; i++) {
      sb.append("1234567890");
    }
    String largeString = sb.toString();

    CommandReport commandReport = new CommandReport();
    commandReport.setStatus(HostRoleStatus.COMPLETED.toString());
    commandReport.setStdOut(largeString);
    commandReport.setStdErr(largeString);
    commandReport.setExitCode(123);
    db.updateHostRoleState(hostName, requestId, stageId, Role.HBASE_MASTER.toString(), commandReport);

    List<HostRoleCommandEntity> commandEntities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER);
    assertEquals(1, commandEntities.size());
    HostRoleCommandEntity commandEntity = commandEntities.get(0);
    HostRoleCommand command = db.getTask(commandEntity.getTaskId());
    assertNotNull(command);

    assertEquals(largeString, command.getStdout());

  }

  private void populateActionDB(ActionDBAccessor db, String hostname,
      long requestId, long stageId) {
    Stage s = new Stage(requestId, "/a/b", "cluster1");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostname, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis(),
            new HashMap<String, String>()), "cluster1", "HBASE");
    s.addHostRoleExecutionCommand(
        hostname,
        Role.HBASE_REGIONSERVER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_REGIONSERVER
            .toString(), hostname, System.currentTimeMillis(),
            new HashMap<String, String>()), "cluster1", "HBASE");
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    db.persistActions(stages);
  }
}
