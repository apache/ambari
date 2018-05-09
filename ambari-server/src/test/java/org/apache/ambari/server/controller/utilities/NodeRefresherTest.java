/*
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

package org.apache.ambari.server.controller.utilities;

import static java.util.Collections.singleton;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class NodeRefresherTest extends EasyMockSupport {
  private NodeRefresher nodeRefresher;
  @Mock
  private Cluster cluster;
  @Mock
  private Service hdfs;
  @Mock
  private ServiceComponent namenode;
  private boolean namenodeWasRefreshed = false;

  @Before
  public void setUp() throws Exception {
    nodeRefresher = nodeRefresher();
    expect(cluster.getService("HDFS")).andReturn(hdfs).anyTimes();
    expect(hdfs.getServiceComponent("NAMENODE")).andReturn(namenode).anyTimes();
    replay(cluster, hdfs, namenode);
  }

  private NodeRefresher nodeRefresher() {
    return new NodeRefresher(null, null, null, null) {
      @Override
      protected void refresh(ServiceComponent namenode, Cluster cluster) throws AmbariException {
        namenodeWasRefreshed = true;
      }
    };
  }

  @Test
  public void testNotifiesNameNodeIfHostWithDataNodeWasDeleted() {
    HostsRemovedEvent event = new HostsRemovedEvent(
      singleton("host1"),
      singleton(cluster),
      new HashMap<String, Set<String>>() {{
        put("host1", singleton("DATANODE"));
      }});
    nodeRefresher.onHostRemoved(event);
    assertTrue(namenodeWasRefreshed);
  }

  @Test
  public void testSkipsNotifyingWhenDeletedHostHaveNoDataNode() {
    HostsRemovedEvent event = new HostsRemovedEvent(
      singleton("host1"),
      singleton(cluster),
      new HashMap<String, Set<String>>() {{
        put("host1", singleton("ZOOKEEPER_CLIENT"));
      }});
    nodeRefresher.onHostRemoved(event);
    assertFalse(namenodeWasRefreshed);
  }
}