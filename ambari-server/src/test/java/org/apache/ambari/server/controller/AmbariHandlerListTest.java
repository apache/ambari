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

import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.view.ViewRegistry;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

  @Test
  public void testHandleNonFailSafe() throws Exception {
    TestHandler handler = new TestHandler();
    AmbariHandlerList.HandlerFactory handlerFactory = createNiceMock(AmbariHandlerList.HandlerFactory.class);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ClassLoader classLoader = createNiceMock(ClassLoader.class);

    Request baseRequest = createNiceMock(Request.class);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    HttpServletResponse response = createNiceMock(HttpServletResponse.class);

    List <Handler> handlers = new LinkedList<Handler>();
    handlers.add(handler);

    expect(viewRegistry.getDefinition("TEST", "1.0.0")).andReturn(viewEntity).anyTimes();
    expect(viewEntity.getClassLoader()).andReturn(classLoader).anyTimes();

    replay(viewRegistry, viewEntity);

    AmbariHandlerList handlerList = new AmbariHandlerList(handlerFactory);
    handlerList.viewRegistry = viewRegistry;

    handlerList.handleNonFailSafe("/api/v1/views/TEST/versions/1.0.0/instances/INSTANCE_1/resources/test",
        baseRequest, request, response, handlers);

    Assert.assertEquals("/api/v1/views/TEST/versions/1.0.0/instances/INSTANCE_1/resources/test", handler.getTarget());
    Assert.assertEquals(classLoader, handler.getClassLoader());

    verify(viewRegistry, viewEntity);
  }

  private static class TestHandler extends AbstractHandler {

    private ClassLoader classLoader = null;
    private String target = null;

    @Override
    public void handle(String target, Request request,
                       HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws IOException, ServletException {
      this.target = target;
      classLoader = Thread.currentThread().getContextClassLoader();
    }

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    public String getTarget() {
      return target;
    }
  }
}
