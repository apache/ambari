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
package org.apache.ambari.server.state.host;

import com.google.gson.Gson;
import com.google.inject.Injector;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.HostHealthStatus;
import org.junit.Test;

import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class HostImplTest {

  @Test
  public void testGetHostAttributes() throws Exception {

    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostStateEntity hostStateEntity = createNiceMock(HostStateEntity.class);
    HostDAO hostDAO  = createNiceMock(HostDAO.class);
    Injector injector = createNiceMock(Injector.class);
    HostStateDAO hostStateDAO  = createNiceMock(HostStateDAO.class);


    Gson gson = new Gson();

    expect(injector.getInstance(Gson.class)).andReturn(gson).anyTimes();
    expect(injector.getInstance(HostDAO.class)).andReturn(hostDAO).anyTimes();
    expect(injector.getInstance(HostStateDAO.class)).andReturn(hostStateDAO).anyTimes();
    expect(hostEntity.getHostAttributes()).andReturn("{\"foo\": \"aaa\", \"bar\":\"bbb\"}").anyTimes();
    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();
    expect(hostEntity.getHostName()).andReturn("host1").anyTimes();
    expect(hostEntity.getHostStateEntity()).andReturn(hostStateEntity).anyTimes();
    expect(hostDAO.findById(1L)).andReturn(hostEntity).once();
    expect(hostStateDAO.findByHostId(1L)).andReturn(hostStateEntity).once();

    replay(hostEntity, hostStateEntity, injector, hostDAO);
    HostImpl host = new HostImpl(hostEntity, false, injector);

    Map<String, String> hostAttributes = host.getHostAttributes();
    assertEquals("aaa", hostAttributes.get("foo"));
    assertEquals("bbb", hostAttributes.get("bar"));

    host = new HostImpl(hostEntity, true, injector);

    hostAttributes = host.getHostAttributes();
    assertEquals("aaa", hostAttributes.get("foo"));
    assertEquals("bbb", hostAttributes.get("bar"));

    verify(hostEntity, hostStateEntity, injector, hostDAO);
  }

  @Test
  public void testGetHealthStatus() throws Exception {

    HostEntity hostEntity = createNiceMock(HostEntity.class);
    HostStateEntity hostStateEntity = createNiceMock(HostStateEntity.class);
    HostDAO hostDAO  = createNiceMock(HostDAO.class);
    HostStateDAO hostStateDAO  = createNiceMock(HostStateDAO.class);
    Injector injector = createNiceMock(Injector.class);

    Gson gson = new Gson();

    expect(injector.getInstance(Gson.class)).andReturn(gson).anyTimes();
    expect(injector.getInstance(HostDAO.class)).andReturn(hostDAO).anyTimes();
    expect(injector.getInstance(HostStateDAO.class)).andReturn(hostStateDAO).anyTimes();
    expect(hostEntity.getHostAttributes()).andReturn("{\"foo\": \"aaa\", \"bar\":\"bbb\"}").anyTimes();
    expect(hostEntity.getHostName()).andReturn("host1").anyTimes();
    expect(hostEntity.getHostId()).andReturn(1L).anyTimes();
    expect(hostEntity.getHostStateEntity()).andReturn(hostStateEntity).anyTimes();
    expect(hostDAO.findById(1L)).andReturn(hostEntity).anyTimes();
    expect(hostStateDAO.findByHostId(1L)).andReturn(hostStateEntity).once();

    replay(hostEntity, hostStateEntity, injector, hostDAO);
    HostImpl host = new HostImpl(hostEntity, false, injector);

    host.getHealthStatus();

    host = new HostImpl(hostEntity, true, injector);

    host.getHealthStatus();

    verify(hostEntity, hostStateEntity, injector, hostDAO);
  }
}