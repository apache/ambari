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

package org.apache.ambari.server.serveraction.kerberos;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.mockito.Matchers.anyBoolean;

import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class KerberosTopologyUpdateTriggerServerActionTest extends EasyMockSupport {

  private static TopologyHolder topologyHolder = EasyMock.createNiceMock(TopologyHolder.class);

  private KerberosTopologyUpdateTriggerServerAction action;

  @Before
  public void init() {
    final Injector injector = createInjector();
    action = injector.getInstance(KerberosTopologyUpdateTriggerServerAction.class);
  }

  @Test
  public void shouldUpdateTopology() throws Exception {
    final TopologyUpdateEvent event = createNiceMock(TopologyUpdateEvent.class);
    expect(topologyHolder.getCurrentData()).andReturn(event).once();
    expect(topologyHolder.updateData(event)).andReturn(anyBoolean()).once();
    replay(topologyHolder);
    action.execute(null);
    verify(topologyHolder);
  }

  static class TestTopologyHolderProvider implements Provider<TopologyHolder> {
    @Override
    public TopologyHolder get() {
      return topologyHolder;
    }
  }

  private Injector createInjector() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(TopologyHolder.class).toProvider(TestTopologyHolderProvider.class);
        PartialNiceMockBinder.newBuilder(KerberosTopologyUpdateTriggerServerActionTest.this).build().configure(binder());
      }
    });
  }

}
