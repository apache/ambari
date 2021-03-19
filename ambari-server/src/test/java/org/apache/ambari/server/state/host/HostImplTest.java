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
package org.apache.ambari.server.state.host;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.util.Map;


import org.apache.ambari.server.events.HostStatusUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;

import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.gson.Gson;

public class HostImplTest extends EasyMockSupport {

  @Test
  public void testGetHostAttributes() throws Exception {

    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostStateEntity hostStateEntity = createNiceMock(HostStateEntity.class);
    HostDAO hostDAO  = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO  = createNiceMock(HostStateDAO.class);

    Gson gson = new Gson();

    expect(hostEntity.getHostAttributes()).andReturn("{\"foo\": \"aaa\", \"bar\":\"bbb\"}").anyTimes();
    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();
    expect(hostEntity.getHostName()).andReturn("host1").anyTimes();
    expect(hostEntity.getHostStateEntity()).andReturn(hostStateEntity).anyTimes();
    expect(hostDAO.findById(1L)).andReturn(hostEntity).atLeastOnce();

    replayAll();
    HostImpl host = new HostImpl(hostEntity, gson, hostDAO, hostStateDAO);

    Map<String, String> hostAttributes = host.getHostAttributes();
    assertEquals("aaa", hostAttributes.get("foo"));
    assertEquals("bbb", hostAttributes.get("bar"));

    host = new HostImpl(hostEntity, gson, hostDAO, hostStateDAO);

    hostAttributes = host.getHostAttributes();
    assertEquals("aaa", hostAttributes.get("foo"));
    assertEquals("bbb", hostAttributes.get("bar"));

    verifyAll();
  }

  @Test
  public void testGetHealthStatus() {

    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostStateEntity hostStateEntity = createNiceMock(HostStateEntity.class);
    HostDAO hostDAO  = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO  = createNiceMock(HostStateDAO.class);

    Gson gson = new Gson();

    expect(hostEntity.getHostAttributes()).andReturn("{\"foo\": \"aaa\", \"bar\":\"bbb\"}").anyTimes();
    expect(hostEntity.getHostName()).andReturn("host1").anyTimes();
    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();
    expect(hostEntity.getHostStateEntity()).andReturn(hostStateEntity).anyTimes();
    expect(hostDAO.findById(1L)).andReturn(hostEntity).anyTimes();
    expect(hostStateDAO.findByHostId(1L)).andReturn(hostStateEntity).atLeastOnce();

    replayAll();
    HostImpl host = new HostImpl(hostEntity, gson, hostDAO, hostStateDAO);

    host.getHealthStatus();

    host = new HostImpl(hostEntity, gson, hostDAO, hostStateDAO);

    host.getHealthStatus();

    verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetStatusWithNull() {
    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostDAO hostDAO = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO = createNiceMock(HostStateDAO.class);

    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();
    replayAll();

    Host host = new HostImpl(hostEntity, new Gson(), hostDAO, hostStateDAO);
    host.setStatus(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetStatusWithIllegalState() {
    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostDAO hostDAO = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO = createNiceMock(HostStateDAO.class);

    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();
    replayAll();

    Host host = new HostImpl(hostEntity, new Gson(), hostDAO, hostStateDAO);
    host.setStatus("illegal status");
  }

  @Test
  public void testSetStatusWithValidArgs() {
    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostDAO hostDAO = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO = createNiceMock(HostStateDAO.class);
    AmbariEventPublisher eventPublisher = createNiceMock(AmbariEventPublisher.class);

    expect(hostEntity.getHostName()).andReturn("host1").anyTimes();
    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();

    eventPublisher.publish(anyObject(HostStatusUpdateEvent.class));
    expectLastCall().times(4);

    replayAll();

    Host host = new HostImpl(hostEntity, new Gson(), hostDAO, hostStateDAO, eventPublisher);

    host.setStatus(HostHealthStatus.HealthStatus.UNHEALTHY.name());
    assertEquals(HostHealthStatus.HealthStatus.UNHEALTHY.name(), host.getStatus());

    host.setStatus(HostHealthStatus.HealthStatus.HEALTHY.name());
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), host.getStatus());

    host.setStatus(HostHealthStatus.HealthStatus.ALERT.name());
    assertEquals(HostHealthStatus.HealthStatus.ALERT.name(), host.getStatus());

    host.setStatus(HostHealthStatus.HealthStatus.UNKNOWN.name());
    assertEquals(HostHealthStatus.HealthStatus.UNKNOWN.name(), host.getStatus());

    verifyAll();
  }
}
