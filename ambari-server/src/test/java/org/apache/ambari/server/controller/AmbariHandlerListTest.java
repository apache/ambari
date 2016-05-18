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

import org.apache.ambari.server.api.AmbariPersistFilter;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.security.AmbariViewsSecurityHeaderFilter;
import org.apache.ambari.server.view.ViewRegistry;
import org.easymock.Capture;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * AmbariHandlerList tests.
 */
public class AmbariHandlerListTest {

  private final AmbariViewsSecurityHeaderFilter ambariViewsSecurityHeaderFilter = createNiceMock(AmbariViewsSecurityHeaderFilter.class);
  private final AmbariPersistFilter persistFilter = createNiceMock(AmbariPersistFilter.class);
  private final DelegatingFilterProxy springSecurityFilter = createNiceMock(DelegatingFilterProxy.class);


  @Test
  public void testAddViewInstance() throws Exception {

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    final WebAppContext handler = createNiceMock(WebAppContext.class);
    Server server = createNiceMock(Server.class);

    expect(handler.getServer()).andReturn(server);
    handler.setServer(null);

    Capture<FilterHolder> securityHeaderFilterCapture = new Capture<FilterHolder>();
    Capture<FilterHolder> persistFilterCapture = new Capture<FilterHolder>();
    Capture<FilterHolder> securityFilterCapture = new Capture<FilterHolder>();

    handler.addFilter(capture(securityHeaderFilterCapture), eq("/*"), eq(AmbariServer.DISPATCHER_TYPES));
    handler.addFilter(capture(persistFilterCapture), eq("/*"), eq(AmbariServer.DISPATCHER_TYPES));
    handler.addFilter(capture(securityFilterCapture), eq("/*"), eq(AmbariServer.DISPATCHER_TYPES));
    handler.setAllowNullPathInfo(true);

    replay(handler, server);

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);

    handlerList.addViewInstance(viewInstanceEntity);

    ArrayList<Handler> handlers = new ArrayList<Handler>(Arrays.asList(handlerList.getHandlers()));

    Assert.assertTrue(handlers.contains(handler));

    Assert.assertEquals(ambariViewsSecurityHeaderFilter, securityHeaderFilterCapture.getValue().getFilter());
    Assert.assertEquals(persistFilter, persistFilterCapture.getValue().getFilter());
    Assert.assertEquals(springSecurityFilter, securityFilterCapture.getValue().getFilter());

    verify(handler, server);
  }

  @Test
  public void testRemoveViewInstance() throws Exception {
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    final WebAppContext handler = createNiceMock(WebAppContext.class);
    Server server = createNiceMock(Server.class);

    expect(handler.getServer()).andReturn(server);
    handler.setServer(null);

    replay(handler, server);

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);

    handlerList.addViewInstance(viewInstanceEntity);

    ArrayList<Handler> handlers = new ArrayList<Handler>(Arrays.asList(handlerList.getHandlers()));

    Assert.assertTrue(handlers.contains(handler));

    handlerList.removeViewInstance(viewInstanceEntity);

    handlers = new ArrayList<Handler>(Arrays.asList(handlerList.getHandlers()));

    Assert.assertFalse(handlers.contains(handler));

    verify(handler, server);

  }

  @Test
  public void testHandle() throws Exception {
    final WebAppContext handler = createNiceMock(WebAppContext.class);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ClassLoader classLoader = createNiceMock(ClassLoader.class);

    Request baseRequest = createNiceMock(Request.class);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    HttpServletResponse response = createNiceMock(HttpServletResponse.class);

    expect(viewRegistry.getDefinition("TEST", "1.0.0")).andReturn(viewEntity).anyTimes();
    expect(viewEntity.getClassLoader()).andReturn(classLoader).anyTimes();

    expect(handler.isStarted()).andReturn(true).anyTimes();

    replay(handler, viewRegistry, viewEntity);
    handler.handle("/api/v1/views/TEST/versions/1.0.0/instances/INSTANCE_1/resources/test",
        baseRequest, request, response);

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);
    handlerList.viewRegistry = viewRegistry;

    handlerList.start();
    handlerList.addHandler(handler);
    handlerList.handle("/api/v1/views/TEST/versions/1.0.0/instances/INSTANCE_1/resources/test",
        baseRequest, request, response);

    verify(handler, viewRegistry, viewEntity);
  }

  private AmbariHandlerList getAmbariHandlerList(final WebAppContext handler) {

    AmbariHandlerList handlerList = new AmbariHandlerList();

    handlerList.webAppContextProvider = new HandlerProvider(handler);
    handlerList.ambariViewsSecurityHeaderFilter = ambariViewsSecurityHeaderFilter;
    handlerList.persistFilter = persistFilter;
    handlerList.springSecurityFilter = springSecurityFilter;

    return handlerList;
  }

  private static class HandlerProvider implements Provider<WebAppContext> {
    private final WebAppContext context;

    private HandlerProvider(WebAppContext context) {
      this.context = context;
    }

    @Override
    public WebAppContext get() {
      return context;
    }
  }
}
