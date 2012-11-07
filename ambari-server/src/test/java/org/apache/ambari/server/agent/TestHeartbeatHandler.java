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
package org.apache.ambari.server.agent;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionDBInMemoryImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.HostStatus.Status;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestHeartbeatHandler {

  private Injector injector;
  private Clusters clusters;
  long requestId = 23;
  long stageId = 31;

  @Before
  public void setup() throws AmbariException{
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testHeartbeat() throws AmbariException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = mock(Clusters.class);
    String hostname = "host1";
    ActionQueue aq = new ActionQueue();
    ExecutionCommand execCmd = new ExecutionCommand();
    execCmd.setCommandId("2-34");
    execCmd.setHostname(hostname);
    aq.enqueue(hostname, new ExecutionCommand());
    HeartBeatHandler handler = new HeartBeatHandler(fsm, aq, am);
    HeartBeat hb = new HeartBeat();
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, "I am ok"));
    hb.setHostname(hostname);
    clusters.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setState(HostState.UNHEALTHY);
    when(fsm.getHost(hostname)).thenReturn(hostObject);
    handler.handleHeartBeat(hb);
    assertEquals(HostState.HEALTHY, hostObject.getState());
    assertEquals(0, aq.dequeueAll(hostname).size());
  }

  @Test
  public void testCommandReport() throws AmbariException {
    String hostname = "host1";
    String clusterName = "cluster1";
    injector.injectMembers(this);
    clusters.addHost(hostname);
    clusters.getHost(hostname).persist();
    clusters.addCluster(clusterName);
    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);
    ActionManager am = new ActionManager(5000, 1200000, new ActionQueue(), clusters, db);
    populateActionDB(db, hostname);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(hostname, "HBASE_MASTER", HostRoleStatus.QUEUED);
    db.hostRoleScheduled(stage, hostname, "HBASE_MASTER");
    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
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
    assertEquals(HostRoleStatus.COMPLETED,
        s.getHostRoleStatus(hostname, "HBASE_MASTER"));
    assertEquals(215,
        s.getExitCode(hostname, "HBASE_MASTER"));
  }

  private void populateActionDB(ActionDBAccessor db, String hostname) {
    Stage s = new Stage(requestId, "/a/b", "cluster1");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostname, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis()), "cluster1", "HBASE");
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    db.persistActions(stages);
  }

  @Test
  public void testRegistration() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = clusters;
    String hostname = "host1";
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am);
    clusters.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("MegaOperatingSystem");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals("megaoperatingsystem", hostObject.getOsType());
  }

  @Test
  public void testRegisterNewNode() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = clusters;
    String hostname = "host1";
    fsm.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("MegaOperatingSystem");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    RegistrationResponse response = handler.handleRegistration(reg);

    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals("megaoperatingsystem", hostObject.getOsType());
    assertEquals(RegistrationStatus.OK, response.getResponseStatus());
    assertEquals(0, response.getResponseId());
    assertEquals(null, response.getCommands());
  }
}
