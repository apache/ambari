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

package org.apache.ambari.server.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ambari.server.api.AmbariPersistFilter;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.security.AmbariViewsSecurityHeaderFilter;
import org.apache.ambari.server.view.ViewRegistry;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * AmbariHandlerList tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class AmbariHandlerListTest {

  @Mock private AmbariViewsSecurityHeaderFilter ambariViewsSecurityHeaderFilter;
  @Mock private AmbariPersistFilter persistFilter;
  @Mock private DelegatingFilterProxy springSecurityFilter;
  @Mock private SessionHandler sessionHandler;
  @Mock private SessionIdManager sessionIdManager;
  @Mock private SessionHandlerConfigurer sessionHandlerConfigurer;
  @Mock private SessionCache sessionCache;
  @Mock private Configuration configuration;
  @Mock private WebAppContext handler;
  @Mock private Server server;
  @Mock private ErrorHandler errorHandler;

  @Captor private ArgumentCaptor<FilterHolder> filterHolderCaptor;
  @Captor private ArgumentCaptor<Boolean> showStackCaptor;

  private AmbariHandlerList getAmbariHandlerList(WebAppContext ctx) {
    AmbariHandlerList list = new AmbariHandlerList();
    //doNothing().when(sessionHandler).setSessionIdManager(sessionIdManager);
    when(sessionHandler.getSessionCache()).thenReturn(sessionCache);
    list.webAppContextProvider = new HandlerProvider(ctx);
    list.ambariViewsSecurityHeaderFilter = ambariViewsSecurityHeaderFilter;
    list.persistFilter = persistFilter;
    list.springSecurityFilter = springSecurityFilter;
    list.sessionHandler = sessionHandler;
    list.sessionHandlerConfigurer = sessionHandlerConfigurer;
    list.configuration = configuration;
    return list;
  }

  @Test
  public void testAddViewInstance() throws Exception {
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    when(handler.getServer()).thenReturn(server);
    when(handler.getChildHandlers()).thenReturn(new Handler[]{});
    when(handler.getSessionHandler()).thenReturn(mock(SessionHandler.class));
    handler.setServer(null);

    final boolean showErrorStacks = true;
    when(configuration.isServerShowErrorStacks()).thenReturn(showErrorStacks);

    when(handler.getErrorHandler()).thenReturn(errorHandler);

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);
    handlerList.start();
    handlerList.start();
    handlerList.start();
    handlerList.addViewInstance(viewInstanceEntity);

    // capture all 3 filter additions
    verify(handler, times(3))
        .addFilter(filterHolderCaptor.capture(), eq("/*"), eq(AmbariServer.DISPATCHER_TYPES));
    List<FilterHolder> holders = filterHolderCaptor.getAllValues();

    // Verify filter classes by comparing class names
    assertEquals(ambariViewsSecurityHeaderFilter.getClass().getName(), holders.get(0).getClassName());
    assertEquals(persistFilter.getClass().getName(),              holders.get(1).getClassName());
    assertEquals(springSecurityFilter.getClass().getName(),       holders.get(2).getClassName());

    // verify allowNullPathInfo and error handler
    verify(handler).setAllowNullPathInfo(true);
    verify(handler, times(3)).getErrorHandler();
    verify(errorHandler).setShowStacks(showStackCaptor.capture());
    assertEquals(showErrorStacks, showStackCaptor.getValue());

    // assert handler registered
    List<Handler> registered = Arrays.asList(handlerList.getHandlers());
    assertTrue(registered.contains(handler));
  }

  @Test
  public void testRemoveViewInstance() throws Exception {
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    // Stub required for handlerList.addViewInstance to work
    when(handler.getServer()).thenReturn(server);
    when(handler.getChildHandlers()).thenReturn(new Handler[]{});
    when(handler.getSessionHandler()).thenReturn(mock(SessionHandler.class));
    handler.setServer(null);

    when(sessionHandler.getSessionCache()).thenReturn(sessionCache);

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);
    handlerList.addViewInstance(viewInstanceEntity);
    List<Handler> registered = Arrays.asList(handlerList.getHandlers());
    assertTrue(registered.contains(handler));

    handlerList.removeViewInstance(viewInstanceEntity);
    assertNull(handlerList.getHandlers());

    verify(handler).getServer();
    verify(handler).getChildHandlers();
    verify(handler).getSessionHandler();
  }

  @Test
  public void testHandle() throws Exception {
    ViewRegistry viewRegistry = mock(ViewRegistry.class);
    ViewEntity viewEntity = mock(ViewEntity.class);
    ClassLoader classLoader = mock(ClassLoader.class);
    Request baseRequest = mock(Request.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    when(viewRegistry.getDefinition("TEST", "1.0.0")).thenReturn(viewEntity);
    when(viewEntity.getClassLoader()).thenReturn(classLoader);

    when(handler.getChildHandlers()).thenReturn(new Handler[]{});

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);
    handlerList.viewRegistry = viewRegistry;

    handlerList.start();
    handlerList.addHandler(handler);
    handlerList.handle("/api/v1/views/TEST/versions/1.0.0/instances/INSTANCE_1/resources/test",
        baseRequest, request, response);

    verify(handler).handle("/api/v1/views/TEST/versions/1.0.0/instances/INSTANCE_1/resources/test",
        baseRequest, request, response);
    verify(viewRegistry, atLeastOnce()).getDefinition("TEST", "1.0.0");
    verify(viewEntity, atLeastOnce()).getClassLoader();
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
