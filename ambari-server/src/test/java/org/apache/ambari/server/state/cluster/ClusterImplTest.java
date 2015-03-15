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

package org.apache.ambari.server.state.cluster;

import org.apache.ambari.server.controller.AmbariSessionManager;
import org.junit.Test;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class ClusterImplTest {

  @Test
  public void testAddSessionAttributes() throws Exception {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("foo", "bar");

    AmbariSessionManager sessionManager = createMock(AmbariSessionManager.class);

    ClusterImpl cluster =
        createMockBuilder(ClusterImpl.class).
            addMockedMethod("getSessionManager").
            addMockedMethod("getClusterName").
            addMockedMethod("getSessionAttributes").
            createMock();

    expect(cluster.getSessionManager()).andReturn(sessionManager);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getSessionAttributes()).andReturn(attributes);
    sessionManager.setAttribute("cluster_session_attributes:c1", attributes);

    replay(sessionManager, cluster);

    cluster.addSessionAttributes(attributes);

    verify(sessionManager, cluster);
  }

  @Test
  public void testSetSessionAttribute() throws Exception {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("foo", "bar");
    attributes.put("foo2", "bar2");

    Map<String, Object> updatedAttributes = new HashMap<String, Object>(attributes);
    updatedAttributes.put("foo2", "updated value");

    Map<String, Object> addedAttributes = new HashMap<String, Object>(updatedAttributes);
    updatedAttributes.put("foo3", "added value");

    AmbariSessionManager sessionManager = createMock(AmbariSessionManager.class);

    ClusterImpl cluster =
      createMockBuilder(ClusterImpl.class).
        addMockedMethod("getSessionManager").
        addMockedMethod("getClusterName").
        addMockedMethod("getSessionAttributes").
        createMock();

    expect(cluster.getSessionManager()).andReturn(sessionManager);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getSessionAttributes()).andReturn(attributes);

    sessionManager.setAttribute("cluster_session_attributes:c1", updatedAttributes);
    expectLastCall().once();

    expect(cluster.getSessionManager()).andReturn(sessionManager);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getSessionAttributes()).andReturn(updatedAttributes);

    sessionManager.setAttribute("cluster_session_attributes:c1", addedAttributes);
    expectLastCall().once();

    replay(sessionManager, cluster);

    cluster.setSessionAttribute("foo2", "updated value");
    cluster.setSessionAttribute("foo3", "added value");

    verify(sessionManager, cluster);
  }

  @Test
  public void testRemoveSessionAttribute() throws Exception {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("foo", "bar");
    attributes.put("foo2", "bar2");

    Map<String, Object> trimmedAttributes = new HashMap<String, Object>(attributes);
    trimmedAttributes.remove("foo2");

    AmbariSessionManager sessionManager = createMock(AmbariSessionManager.class);

    ClusterImpl cluster =
      createMockBuilder(ClusterImpl.class).
        addMockedMethod("getSessionManager").
        addMockedMethod("getClusterName").
        addMockedMethod("getSessionAttributes").
        createMock();

    expect(cluster.getSessionManager()).andReturn(sessionManager);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getSessionAttributes()).andReturn(attributes);
    sessionManager.setAttribute("cluster_session_attributes:c1", trimmedAttributes);
    expectLastCall().once();

    replay(sessionManager, cluster);

    cluster.removeSessionAttribute("foo2");

    verify(sessionManager, cluster);
  }

  @Test
  public void testGetSessionAttributes() throws Exception {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("foo", "bar");

    AmbariSessionManager sessionManager = createMock(AmbariSessionManager.class);

    ClusterImpl cluster =
        createMockBuilder(ClusterImpl.class).
            addMockedMethod("getSessionManager").
            addMockedMethod("getClusterName").
            createMock();

    expect(cluster.getSessionManager()).andReturn(sessionManager).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(sessionManager.getAttribute("cluster_session_attributes:c1")).andReturn(attributes);
    expect(sessionManager.getAttribute("cluster_session_attributes:c1")).andReturn(null);

    replay(sessionManager, cluster);

    assertEquals(attributes, cluster.getSessionAttributes());
    assertEquals(Collections.<String, Object>emptyMap(), cluster.getSessionAttributes());

    verify(sessionManager, cluster);
  }
}