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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * AmbariHandlerList tests.
 */
public class AmbariHandlerListTest {
  @Test
  public void testAddViewInstance() throws Exception {

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    final Handler handler = createNiceMock(Handler.class);
    Server server = createNiceMock(Server.class);

    expect(handler.getServer()).andReturn(server);
    handler.setServer(null);

    replay(handler, server);

    AmbariHandlerList.HandlerFactory handlerFactory = new AmbariHandlerList.HandlerFactory() {
      @Override
      public Handler create(ViewInstanceEntity viewInstanceDefinition, String webApp, String contextPath) {
        return handler;
      }
    };

    AmbariHandlerList handlerList = new AmbariHandlerList(handlerFactory);

    handlerList.addViewInstance(viewInstanceEntity);

    ArrayList<Handler> handlers = new ArrayList<Handler>(Arrays.asList(handlerList.getHandlers()));

    Assert.assertTrue(handlers.contains(handler));

    verify(handler, server);
  }

  @Test
  public void testRemoveViewInstance() throws Exception {
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    final Handler handler = createNiceMock(Handler.class);
    Server server = createNiceMock(Server.class);

    expect(handler.getServer()).andReturn(server);
    handler.setServer(null);

    replay(handler, server);

    AmbariHandlerList.HandlerFactory handlerFactory = new AmbariHandlerList.HandlerFactory() {
      @Override
      public Handler create(ViewInstanceEntity viewInstanceDefinition, String webApp, String contextPath) {
        return handler;
      }
    };

    AmbariHandlerList handlerList = new AmbariHandlerList(handlerFactory);

    handlerList.addViewInstance(viewInstanceEntity);

    ArrayList<Handler> handlers = new ArrayList<Handler>(Arrays.asList(handlerList.getHandlers()));

    Assert.assertTrue(handlers.contains(handler));

    handlerList.removeViewInstance(viewInstanceEntity);

    handlers = new ArrayList<Handler>(Arrays.asList(handlerList.getHandlers()));

    Assert.assertFalse(handlers.contains(handler));

    verify(handler, server);

  }
}
