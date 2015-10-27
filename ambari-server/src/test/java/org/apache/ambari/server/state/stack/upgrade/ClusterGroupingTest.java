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

package org.apache.ambari.server.state.stack.upgrade;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.UpgradeContext;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class ClusterGroupingTest {

  @Test
  public void testGetHostsForExecuteStage() throws Exception {
    UpgradeContext ctx = createNiceMock(UpgradeContext.class);
    ClusterGrouping.ExecuteStage execution = new ClusterGrouping.ExecuteStage();
    MasterHostResolver resolverMock = createMock(MasterHostResolver.class);
    expect(ctx.getResolver()).andReturn(resolverMock).anyTimes();
    replay(ctx);

    // Check case when execution stage defines no service/component requirements
    replay(resolverMock);
    Set<String> realHosts = ClusterGrouping.getHostsForExecuteStage(ctx, execution);
    assertNotNull(realHosts);
    assertTrue(realHosts.isEmpty());
    verify(resolverMock);

    // Check case when execution stage defines service/component requirements,
    // but no hosts match them
    execution.service = "HBASE";
    execution.component = "HBASE_MASTER";
    reset(resolverMock);
    expect(resolverMock.getMasterAndHosts(anyString(), anyString())).andReturn(null).once();
    replay(resolverMock);
    realHosts = ClusterGrouping.getHostsForExecuteStage(ctx, execution);
    assertNull(realHosts);
    verify(resolverMock);

    // Check case when execution stage defines service/component requirements,
    // and some hosts match them
    execution.service = "HBASE";
    execution.component = "HBASE_MASTER";
    reset(resolverMock);
    HostsType hostsType = new HostsType();
    hostsType.hosts.add("host1");
    hostsType.hosts.add("host2");
    expect(resolverMock.getMasterAndHosts(anyString(), anyString())).andReturn(hostsType).once();
    replay(resolverMock);
    realHosts = ClusterGrouping.getHostsForExecuteStage(ctx, execution);
    assertNotNull(realHosts);
    assertArrayEquals(new String [] {"host1", "host2"}, realHosts.toArray());
    verify(resolverMock);
  }
}