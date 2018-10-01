/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.serveraction.upgrades;

import static java.util.Collections.emptyList;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.HashMap;

import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class FixTimelineReaderAddressTest extends EasyMockSupport {
  @Mock(type = MockType.NICE)
  private Cluster cluster;
  @Mock(type = MockType.NICE)
  private Config config;
  @Mock
  private Service yarn;
  @Mock
  private ServiceComponent timelineReader;
  private FixTimelineReaderAddress action;

  @Before
  public void setup() throws Exception {
    action = new FixTimelineReaderAddress();
    action.m_clusters = createMock(Clusters.class);
    action.agentConfigsHolder = createNiceMock(AgentConfigsHolder.class);
    action.setExecutionCommand(new ExecutionCommand());
    expect(action.m_clusters.getCluster(anyString())).andReturn(cluster).anyTimes();
    expect(cluster.getService("YARN")).andReturn(yarn).anyTimes();
    expect(cluster.getHosts()).andReturn(emptyList()).anyTimes();
    expect(cluster.getDesiredConfigByType("yarn-site")).andReturn(config).anyTimes();
    expect(config.getProperties()).andReturn(new HashMap<String, String>() {{
      put("yarn.timeline-service.reader.webapp.address", "localhost:8080");
      put("yarn.timeline-service.reader.webapp.https.address", "{{timeline_reader_address_https}}");
    }}).anyTimes();
    expect(yarn.getServiceComponent("TIMELINE_READER")).andReturn(timelineReader).anyTimes();
    expect(timelineReader.getServiceComponentHosts()).andReturn(new HashMap<String, ServiceComponentHost>(){{
      put("newhost", null);
    }}).anyTimes();
  }

  @Test
  public void testReplaceTimelineReaderHost() throws Exception {
    config.updateProperties(new HashMap<String, String>() {{
      put("yarn.timeline-service.reader.webapp.address", "newhost:8080");
    }});
    expectLastCall();
    config.updateProperties(new HashMap<String, String>() {{
      put("yarn.timeline-service.reader.webapp.https.address", "newhost:8199");
    }});
    expectLastCall();
    replayAll();
    action.execute(null);
    verifyAll();
  }
}