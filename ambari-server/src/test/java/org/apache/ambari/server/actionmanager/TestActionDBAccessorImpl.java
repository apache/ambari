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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.util.Modules;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.BaseRequest;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.apache.ambari.server.orm.DBAccessor.DbType.ORACLE;

public class TestActionDBAccessorImpl {
  private static final Logger log = LoggerFactory.getLogger(TestActionDBAccessorImpl.class);

  private long requestId = 23;
  private long stageId = 31;
  private String hostName = "host1";
  private String clusterName = "cluster1";
  private String actionName = "validate_kerberos";
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
    InMemoryDefaultTestModule defaultTestModule = new InMemoryDefaultTestModule();
    injector  = Guice.createInjector(Modules.override(defaultTestModule)
      .with(new TestActionDBAccessorModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    clusters.addHost(hostName);
    clusters.getHost(hostName).persist();
    clusters.addCluster(clusterName);
    db = injector.getInstance(ActionDBAccessorImpl.class);

    am = new ActionManager(5000, 1200000, new ActionQueue(), clusters, db,
        new HostsMap((String) null), null, injector.getInstance(UnitOfWork.class),
		injector.getInstance(RequestFactory.class), null);
  }

  @After
  public void tearDown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testActionResponse() throws AmbariException {
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
    am.processTaskResponse(hostname, reports, stage.getOrderedHostRoleCommands());
    assertEquals(215,
        am.getAction(requestId, stageId).getExitCode(hostname, "HBASE_MASTER"));
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
        .getHostRoleStatus(hostname, "HBASE_MASTER"));
    Stage s = db.getAllStages(requestId).get(0);
    assertEquals(HostRoleStatus.COMPLETED,s.getHostRoleStatus(hostname, "HBASE_MASTER"));
  }

  @Test
  public void testCancelCommandReport() throws AmbariException {
    String hostname = "host1";
    populateActionDB(db, hostname, requestId, stageId);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(hostname, "HBASE_MASTER", HostRoleStatus.ABORTED);
    db.hostRoleScheduled(stage, hostname, "HBASE_MASTER");
    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setTaskId(1);
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setRole("HBASE_MASTER");
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(0);
    reports.add(cr);
    am.processTaskResponse(hostname, reports, stage.getOrderedHostRoleCommands());
    assertEquals(0,
            am.getAction(requestId, stageId).getExitCode(hostname, "HBASE_MASTER"));
    assertEquals("HostRoleStatus should remain ABORTED " +
            "(command report status should be ignored)",
            HostRoleStatus.ABORTED, am.getAction(requestId, stageId)
            .getHostRoleStatus(hostname, "HBASE_MASTER"));
    Stage s = db.getAllStages(requestId).get(0);
    assertEquals("HostRoleStatus should remain ABORTED " +
            "(command report status should be ignored)",
            HostRoleStatus.ABORTED,s.getHostRoleStatus(hostname, "HBASE_MASTER"));
  }
  
  @Test
  public void testGetStagesInProgress() throws AmbariException {
    String hostname = "host1";
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(createStubStage(hostname, requestId, stageId));
    stages.add(createStubStage(hostname, requestId, stageId + 1));
    Request request = new Request(stages, clusters);
    db.persistActions(request);
    assertEquals(2, stages.size());
  }
  
  @Test
  public void testGetStagesInProgressWithFailures() throws AmbariException {
    String hostname = "host1";
    populateActionDB(db, hostname, requestId, stageId);
    populateActionDB(db, hostname, requestId+1, stageId);
    db.abortOperation(requestId);
    List<Stage> stages = db.getStagesInProgress();
    assertEquals(1, stages.size());
    assertEquals(requestId+1, stages.get(0).getRequestId());
  }

  @Test
  public void testPersistActions() throws AmbariException {
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
  public void testHostRoleScheduled() throws InterruptedException, AmbariException {
    populateActionDB(db, hostName, requestId, stageId);
    Stage stage = db.getStage(StageUtils.getActionId(requestId, stageId));
    assertEquals(HostRoleStatus.PENDING, stage.getHostRoleStatus(hostName, Role.HBASE_MASTER.toString()));
    List<HostRoleCommandEntity> entities=
        hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());

    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    stage.setHostRoleStatus(hostName, Role.HBASE_MASTER.toString(), HostRoleStatus.QUEUED);

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());
    assertEquals(HostRoleStatus.QUEUED, stage.getHostRoleStatus(hostName, Role.HBASE_MASTER.toString()));
    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    db.hostRoleScheduled(stage, hostName, Role.HBASE_MASTER.toString());

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());
    assertEquals(HostRoleStatus.QUEUED, entities.get(0).getStatus());

    Thread thread = new Thread(){
      @Override
      public void run() {
        Stage stage1 = db.getStage("23-31");
        stage1.setHostRoleStatus(hostName, Role.HBASE_MASTER.toString(), HostRoleStatus.COMPLETED);
        db.hostRoleScheduled(stage1, hostName, Role.HBASE_MASTER.toString());
      }
    };

    thread.start();
    thread.join();

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());
    assertEquals("Concurrent update failed", HostRoleStatus.COMPLETED, entities.get(0).getStatus());
  }

  @Test
  public void testCustomActionScheduled() throws InterruptedException, AmbariException {
    populateActionDBWithCustomAction(db, hostName, requestId, stageId);
    Stage stage = db.getStage(StageUtils.getActionId(requestId, stageId));
    assertEquals(HostRoleStatus.PENDING, stage.getHostRoleStatus(hostName, actionName));
    List<HostRoleCommandEntity> entities =
        hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, actionName);

    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    stage.setHostRoleStatus(hostName, actionName, HostRoleStatus.QUEUED);

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, actionName);
    assertEquals(HostRoleStatus.QUEUED, stage.getHostRoleStatus(hostName, actionName));
    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());

    long now = System.currentTimeMillis();
    db.hostRoleScheduled(stage, hostName, actionName);

    entities = hostRoleCommandDAO.findByHostRole(
        hostName, requestId, stageId, actionName);
    assertEquals(HostRoleStatus.QUEUED, entities.get(0).getStatus());


    Thread thread = new Thread() {
      @Override
      public void run() {
        Stage stage1 = db.getStage("23-31");
        stage1.setHostRoleStatus(hostName, actionName, HostRoleStatus.COMPLETED);
        db.hostRoleScheduled(stage1, hostName, actionName);
      }
    };

    thread.start();
    thread.join();

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, actionName);
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
    commandReport.setStructuredOut(largeString);
    commandReport.setExitCode(123);
    db.updateHostRoleState(hostName, requestId, stageId, Role.HBASE_MASTER.toString(), commandReport);

    List<HostRoleCommandEntity> commandEntities =
        hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());
    assertEquals(1, commandEntities.size());
    HostRoleCommandEntity commandEntity = commandEntities.get(0);
    HostRoleCommand command = db.getTask(commandEntity.getTaskId());
    assertNotNull(command);

    assertEquals(largeString, command.getStdout());
    assertEquals(largeString, command.getStructuredOut());

    //endTime for completed commands should be set
    assertTrue(command.getEndTime() != -1);

  }

  @Test
  public void testGetRequestsByStatus() throws AmbariException {
    List<Long> requestIds = new ArrayList<Long>();
    requestIds.add(requestId + 1);
    requestIds.add(requestId);
    populateActionDB(db, hostName, requestId, stageId);
    clusters.addHost("host2");
    clusters.getHost("host2").persist();
    populateActionDB(db, hostName, requestId + 1, stageId);
    List<Long> requestIdsResult =
      db.getRequestsByStatus(null, BaseRequest.DEFAULT_PAGE_SIZE, false);
    
    assertNotNull("List of request IDs is null", requestIdsResult);
    assertEquals("Request IDs not matches", requestIds, requestIdsResult);
  }

  @Test
  public void testGetRequestsByStatusWithParams() throws AmbariException {
    List<Long> ids = new ArrayList<Long>();

    for (long l = 0; l < 10; l++) {
      ids.add(l);
    }

    for (Long id : ids) {
      populateActionDB(db, hostName, id, stageId);
    }

    List<Long> expected = null;
    List<Long> actual = null;

    // Select all requests
    actual = db.getRequestsByStatus(null, BaseRequest.DEFAULT_PAGE_SIZE, false);
    expected = reverse(new ArrayList<Long>(ids));
    assertEquals("Request IDs not matches", expected, actual);

    actual = db.getRequestsByStatus(null, 4, false);
    expected = reverse(new ArrayList<Long>(ids.subList(ids.size() - 4, ids.size())));
    assertEquals("Request IDs not matches", expected, actual);

    actual = db.getRequestsByStatus(null, 7, true);
    expected = new ArrayList<Long>(ids.subList(0, 7));
    assertEquals("Request IDs not matches", expected, actual);
  }

  private <T> List<T> reverse(List<T> list) {
    List<T> result = new ArrayList<T>(list);

    Collections.reverse(result);

    return result;
  }

  @Test
  public void testAbortRequest() throws AmbariException {
    Stage s = new Stage(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
      "clusterHostInfo", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);

    clusters.addHost("host2");
    clusters.getHost("host2").persist();
    clusters.addHost("host3");
    clusters.getHost("host3").persist();
    clusters.addHost("host4");
    clusters.getHost("host4").persist();

    s.addHostRoleExecutionCommand("host1", Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            "host1", System.currentTimeMillis()), "cluster1", "HBASE");
    s.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            "host2", System.currentTimeMillis()), "cluster1", "HBASE");
    s.addHostRoleExecutionCommand(
        "host3",
        Role.HBASE_REGIONSERVER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_REGIONSERVER
            .toString(), "host3", System.currentTimeMillis()), "cluster1", "HBASE");
    s.addHostRoleExecutionCommand(
        "host4",
        Role.HBASE_REGIONSERVER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_REGIONSERVER
            .toString(), "host4", System.currentTimeMillis()), "cluster1", "HBASE");
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    s.getOrderedHostRoleCommands().get(0).setStatus(HostRoleStatus.PENDING);
    s.getOrderedHostRoleCommands().get(1).setStatus(HostRoleStatus.IN_PROGRESS);
    s.getOrderedHostRoleCommands().get(2).setStatus(HostRoleStatus.QUEUED);

    HostRoleCommand cmd = s.getOrderedHostRoleCommands().get(3);
    String hostName = cmd.getHostName();
    cmd.setStatus(HostRoleStatus.COMPLETED);

    Request request = new Request(stages, clusters);
    db.persistActions(request);
    db.abortOperation(requestId);

    List<HostRoleCommand> commands = db.getRequestTasks(requestId);
    for(HostRoleCommand command : commands) {
      if(command.getHostName().equals(hostName)) {
        assertEquals(HostRoleStatus.COMPLETED, command.getStatus());
      } else {
        assertEquals(HostRoleStatus.ABORTED, command.getStatus());
      }
    }
  }

  private static class TestActionDBAccessorModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(DBAccessor.class).to(TestDBAccessorImpl.class);
    }
  }

  @Singleton
  static class TestDBAccessorImpl extends DBAccessorImpl {
    private DbType dbTypeOverride = null;

    @Inject
    public TestDBAccessorImpl(Configuration configuration) {
      super(configuration);
    }

    @Override
    public DbType getDbType() {
      if (dbTypeOverride != null) {
        return dbTypeOverride;
      }

      return super.getDbType();
    }

    public void setDbTypeOverride(DbType dbTypeOverride) {
      this.dbTypeOverride = dbTypeOverride;
    }
  }

  @Test
  public void testGet1000TasksFromOracleDB() throws Exception {
    Stage s = new Stage(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
      "clusterHostInfo", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    for (int i = 1000; i < 2002; i++) {
      String host = "host" + i;

      clusters.addHost(host);
      clusters.getHost(host).persist();

      s.addHostRoleExecutionCommand("host" + i, Role.HBASE_MASTER,
        RoleCommand.START, null, "cluster1", "HBASE");
    }

    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    Request request = new Request(stages, clusters);
    db.persistActions(request);

    List<HostRoleCommandEntity> entities =
      hostRoleCommandDAO.findByRequest(request.getRequestId());

    assertEquals(1002, entities.size());
    List<Long> taskIds = new ArrayList<Long>();
    for (HostRoleCommandEntity entity : entities) {
      taskIds.add(entity.getTaskId());
    }

    TestDBAccessorImpl testDBAccessorImpl =
      (TestDBAccessorImpl) injector.getInstance(DBAccessor.class);

    testDBAccessorImpl.setDbTypeOverride(ORACLE);

    assertEquals(ORACLE, injector.getInstance(DBAccessor.class).getDbType());
    entities = hostRoleCommandDAO.findByPKs(taskIds);
    assertEquals("Tasks returned from DB match the ones created",
      taskIds.size(), entities.size());
  }

  private void populateActionDB(ActionDBAccessor db, String hostname,
      long requestId, long stageId) throws AmbariException {
    Stage s = createStubStage(hostname, requestId, stageId);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    Request request = new Request(stages, clusters);
    db.persistActions(request);
  }

  private Stage createStubStage(String hostname, long requestId, long stageId) {
    Stage s = new Stage(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
      "clusterHostInfo", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostname, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis()), "cluster1", "HBASE");
    s.addHostRoleExecutionCommand(
        hostname,
        Role.HBASE_REGIONSERVER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_REGIONSERVER
            .toString(), hostname, System.currentTimeMillis()), "cluster1", "HBASE");
    return s;
  }

  private void populateActionDBWithCustomAction(ActionDBAccessor db, String hostname,
                                long requestId, long stageId) throws AmbariException {
    Stage s = new Stage(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
      "", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostname, Role.valueOf(actionName),
        RoleCommand.ACTIONEXECUTE,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis()), "cluster1", "HBASE");
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    final RequestResourceFilter resourceFilter = new RequestResourceFilter("HBASE", "HBASE_MASTER", null);
    List<RequestResourceFilter> resourceFilters = new
      ArrayList<RequestResourceFilter>() {{ add(resourceFilter); }};
    ExecuteActionRequest executeActionRequest = new ExecuteActionRequest
      ("cluster1", null, actionName, resourceFilters, null, null, false);
    Request request = new Request(stages, clusters);
    db.persistActions(request);
  }
}
