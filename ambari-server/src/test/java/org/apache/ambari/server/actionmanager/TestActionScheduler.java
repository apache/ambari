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
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.AgentCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.live.Cluster;
import org.apache.ambari.server.state.live.Clusters;
import org.junit.Test;

public class TestActionScheduler {

  /**
   * This test sends a new action to the action scheduler and verifies that the action
   * shows up in the action queue.
   */
  @Test
  public void testActionSchedule() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    ActionDBAccessor db = mock(ActionDBAccessorImpl.class);
    List<Stage> stages = new ArrayList<Stage>();
    Stage s = new Stage(1, "/bogus", "clusterName");
    s.setStageId(977);
    stages.add(s);
    String hostname = "ahost.ambari.apache.org";
    HostAction ha = new HostAction(hostname);
    HostRoleCommand hrc = new HostRoleCommand(Role.DATANODE,
        null);
    ha.addHostRoleCommand(hrc);
   // ha.setManifest("1-977-manifest");
    s.addHostAction(hostname, ha);
    when(db.getStagesInProgress()).thenReturn(stages);
    
    //Keep large number of attempts so that the task is not expired finally
    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(1000, 100, db, aq, fsm, 10000);
    // Start the thread
    scheduler.start();

    Thread.sleep(1000);
    List<AgentCommand> ac = aq.dequeueAll(hostname);
    assertEquals(1, ac.size());
    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals("1-977", ((ExecutionCommand) (ac.get(0))).getCommandId());

    //The action status has not changed, it should be queued again.
    Thread.sleep(1000);
    ac = aq.dequeueAll(hostname);
    assertEquals(1, ac.size());
    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals("1-977", ((ExecutionCommand) (ac.get(0))).getCommandId());

    //Now change the action status
    hrc.setStatus(HostRoleStatus.COMPLETED);
    ac = aq.dequeueAll(hostname);

    //Wait for sometime, it shouldn't be scheduled this time.
    Thread.sleep(1000);
    ac = aq.dequeueAll(hostname);
    assertEquals(0, ac.size());
  }

  /**
   * Test whether scheduler times out an action
   */
  @Test
  public void testActionTimeout() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    ActionDBAccessorImpl db = mock(ActionDBAccessorImpl.class);
    List<Stage> stages = new ArrayList<Stage>();
    Stage s = new Stage(1, "/bogus", "clusterName");
    s.setStageId(977);
    stages.add(s);
    String hostname = "ahost.ambari.apache.org";
    HostAction ha = new HostAction(hostname);
    HostRoleCommand hrc = new HostRoleCommand(Role.DATANODE,
        null);
    ha.addHostRoleCommand(hrc);
    // ha.setManifest("1-977-manifest");
    s.addHostAction(hostname, ha);
    when(db.getStagesInProgress()).thenReturn(stages);
    
    //Keep large number of attempts so that the task is not expired finally
    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 100, db, aq, fsm, 3);
    // Start the thread
    scheduler.start();

    Thread.sleep(500);
    //TODO timeoutHostRole must be called exactly once but in this case the state
    //in the db continues to be pending therefore it is processed multiple times.
    verify(db, atLeastOnce()).timeoutHostRole(hostname, 1, 977, Role.DATANODE);
  }
}
