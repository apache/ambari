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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Provider;

import org.apache.ambari.server.api.AmbariErrorHandler;
import org.apache.ambari.server.api.AmbariPersistFilter;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.security.AmbariViewsSecurityHeaderFilter;
import org.apache.ambari.server.view.ViewRegistry;
import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.session.SessionCache;
import org.eclipse.jetty.session.SessionIdManager;
import org.eclipse.jetty.util.Callback;
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
  @Mock private ErrorHandler errorHandler;
  @Mock private AmbariErrorHandler ambariErrorHandler;
  @Mock private Response response;
  @Mock private Callback callback;

  @Captor private ArgumentCaptor<FilterHolder> filterHolderCaptor;
  @Captor private ArgumentCaptor<Boolean> showStackCaptor;

  private AmbariHandlerList getAmbariHandlerList(WebAppContext ctx) {
    AmbariHandlerList list = new AmbariHandlerList();
    //doNothing().when(sessionHandler).setSessionIdManager(sessionIdManager);
    lenient().when(sessionHandler.getSessionCache()).thenReturn(sessionCache);
    list.webAppContextProvider = new HandlerProvider(ctx);
    list.ambariViewsSecurityHeaderFilter = ambariViewsSecurityHeaderFilter;
    list.persistFilter = persistFilter;
    list.springSecurityFilter = springSecurityFilter;
    list.sessionHandler = sessionHandler;
    list.sessionHandlerConfigurer = sessionHandlerConfigurer;
    list.configuration = configuration;
    list.ambariErrorHandler = ambariErrorHandler;
    return list;
  }

  @Test
  public void testAddViewInstance() throws Exception {
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    lenient().when(handler.getSessionHandler()).thenReturn(mock(SessionHandler.class));

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

    // verify null path handling and error handler
    verify(handler).setAllowNullPathInContext(true);
    verify(handler, times(2)).getErrorHandler();
    verify(errorHandler).setShowStacks(showStackCaptor.capture());
    assertEquals(showErrorStacks, showStackCaptor.getValue());

    // assert handler registered
    List<Handler> registered = handlerList.getHandlers();
    assertTrue(registered.contains(handler));
  }

  @Test
  public void testRemoveViewInstance() throws Exception {
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    // Stub required for handlerList.addViewInstance to work
    lenient().when(handler.getSessionHandler()).thenReturn(mock(SessionHandler.class));

    lenient().when(sessionHandler.getSessionCache()).thenReturn(sessionCache);

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);
    handlerList.addViewInstance(viewInstanceEntity);
    List<Handler> registered = handlerList.getHandlers();
    assertTrue(registered.contains(handler));

    handlerList.removeViewInstance(viewInstanceEntity);
    assertTrue(handlerList.getHandlers().isEmpty());

  }

  @Test
  public void testAddViewInstanceRollsBackAfterStartFailureAndCanRetry() throws Exception {
    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();
    lenient().when(handler.getSessionHandler()).thenReturn(mock(SessionHandler.class));
    doThrow(new Exception("start failed")).doNothing().when(handler).start();

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);
    handlerList.start();

    try {
      handlerList.addViewInstance(viewInstanceEntity);
      fail("Expected view startup to fail");
    } catch (org.apache.ambari.view.SystemException expected) {
      assertTrue(expected.getMessage().contains("adding a view instance"));
    }
    assertTrue(handlerList.getHandlers().isEmpty());

    handlerList.addViewInstance(viewInstanceEntity);
    assertTrue(handlerList.getHandlers().contains(handler));
  }

  @Test
  public void testLiveViewReloadDoesNotReinitializeServerSessionCache() throws Exception {
    Path archive = Files.createTempDirectory("ambari-view-session-startup");
    Server server = new Server();
    SessionHandler serverSessions = new SessionHandler();
    ServletContextHandler mainContext = new ServletContextHandler();
    mainContext.setContextPath("/");
    mainContext.setSessionHandler(serverSessions);
    WebAppContext first = new WebAppContext();
    AmbariHandlerList handlerList = getAmbariHandlerList(first);
    handlerList.sessionHandler = serverSessions;
    handlerList.addHandler(mainContext);
    server.setHandler(handlerList);
    ViewEntity view = mock(ViewEntity.class);
    when(view.getArchive()).thenReturn(archive.toString());
    when(view.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(view.getConfiguration()).thenReturn(new org.apache.ambari.server.view.configuration.ViewConfig());
    ViewInstanceEntity instance = mock(ViewInstanceEntity.class);
    when(instance.getViewEntity()).thenReturn(view);
    when(instance.getContextPath()).thenReturn("/views/SESSION_TEST/1/INSTANCE");
    try {
      server.start();
      SessionCache sharedCache = serverSessions.getSessionCache();
      assertTrue(sharedCache.isStarted());
      handlerList.addViewInstance(instance);
      assertTrue(first.isAvailable());
      assertSame(sharedCache, first.getSessionHandler().getSessionCache());

      WebAppContext replacement = new WebAppContext();
      handlerList.webAppContextProvider = new HandlerProvider(replacement);
      handlerList.addViewInstance(instance);
      assertTrue(replacement.isAvailable());
      assertSame(sharedCache, replacement.getSessionHandler().getSessionCache());
      assertTrue(sharedCache.isStarted());
      assertTrue(!handlerList.getHandlers().contains(first));
    } finally {
      server.stop();
      // Jetty owns any work files beneath the temporary archive.
      archive.toFile().deleteOnExit();
    }
  }

  @Test
  public void testHandle() throws Exception {
    ViewRegistry viewRegistry = mock(ViewRegistry.class);
    ViewEntity viewEntity = mock(ViewEntity.class);
    ClassLoader classLoader = mock(ClassLoader.class);
    Request baseRequest = mock(Request.class);
    String target = "/api/v1/views/%54EST/versions/1%2E0%2E0/instances/INSTANCE_1/resources/test";

    when(viewRegistry.getDefinition("TEST", "1.0.0")).thenReturn(viewEntity);
    when(viewEntity.getClassLoader()).thenReturn(classLoader);
    when(baseRequest.getHttpURI()).thenReturn(HttpURI.from(target));
    when(handler.handle(baseRequest, response, callback)).thenAnswer(invocation -> {
      assertSame(classLoader, Thread.currentThread().getContextClassLoader());
      return true;
    });

    AmbariHandlerList handlerList = getAmbariHandlerList(handler);
    handlerList.viewRegistry = viewRegistry;

    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    handlerList.start();
    handlerList.addHandler(handler);
    assertTrue(handlerList.handle(baseRequest, response, callback));

    verify(handler).handle(baseRequest, response, callback);
    verify(viewRegistry, atLeastOnce()).getDefinition("TEST", "1.0.0");
    verify(viewEntity, atLeastOnce()).getClassLoader();
    assertSame(originalClassLoader, Thread.currentThread().getContextClassLoader());
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
