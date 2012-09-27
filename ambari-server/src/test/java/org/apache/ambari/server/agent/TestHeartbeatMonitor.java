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

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.state.live.Clusters;
import org.apache.ambari.server.state.live.host.HostState;
import org.junit.Test;

public class TestHeartbeatMonitor {

  @Test
  public void testHeartbeatExpiry() throws Exception {
    Clusters fsm = mock(Clusters.class);
    ActionQueue aq = new ActionQueue();
    ActionManager am = mock(ActionManager.class);
    HostState hs = HostState.WAITING_FOR_HOST_STATUS_UPDATES;
    List<String> allHosts = new ArrayList<String>();
    allHosts.add("host1");
    when(fsm.getAllHosts()).thenReturn(allHosts);
    when(fsm.getHostState("host1")).thenReturn(hs);
    aq.enqueue("host1", new ExecutionCommand());
    HeartbeatMonitor hm = new HeartbeatMonitor(fsm, aq, am, 100);
    hm.start();
    Thread.sleep(120);
    //Heartbeat must have expired for host1, action queue must be flushed
    assertEquals(0, aq.dequeueAll("host1").size());
    verify(am, times(1)).handleLostHost("host1");
    verify(fsm, times(2)).updateStatus(eq("host1"), anyString());
    hm.shutdown();
  }

}
