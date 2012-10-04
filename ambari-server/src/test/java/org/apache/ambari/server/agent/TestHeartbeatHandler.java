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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBInMemoryImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.HostStatus.Status;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.ClustersImpl;
import org.apache.ambari.server.state.live.host.HostImpl;
import org.junit.Test;

public class TestHeartbeatHandler {

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
    Host hostObject = new HostImpl(hostname);
    hostObject.setState(HostState.UNHEALTHY);
    when(fsm.getHost(hostname)).thenReturn(hostObject);
    handler.handleHeartBeat(hb);
    assertEquals(HostState.HEALTHY, hostObject.getState());
    assertEquals(0, aq.dequeueAll(hostname).size());
  }
  
  @Test
  public void testRegistration() throws AmbariException,
      InvalidStateTransitonException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = mock(Clusters.class);
    String hostname = "host1";
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am);
    Host hostObject = new HostImpl(hostname);
    when(fsm.getHost(hostname)).thenReturn(hostObject);
    
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setOS("MegaOperatingSystem");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals("MegaOperatingSystem", hostObject.getOsType());
  }
  
  @Test
  public void testRegisterNewNode() throws AmbariException, InvalidStateTransitonException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = new ClustersImpl();
    String hostname = "host1";
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setOS("MegaOperatingSystem");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    RegistrationResponse response = handler.handleRegistration(reg);
    Host hostObject = fsm.getHost(hostname);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals("MegaOperatingSystem", hostObject.getOsType());
    assertEquals(RegistrationStatus.OK, response.getResponseStatus());
    assertEquals(0, response.getResponseId());
    assertEquals(null, response.getCommands());
  }
}
