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

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.ambari.server.api.AmbariErrorHandler;
import org.apache.ambari.server.api.AmbariPersistFilter;
import org.apache.ambari.server.api.AmbariViewErrorHandlerProxy;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.AmbariViewsSecurityHeaderFilter;
import org.apache.ambari.server.view.ViewContextImpl;
import org.apache.ambari.server.view.ViewInstanceHandlerList;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.ViewContext;
import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.session.SessionCache;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * An Ambari specific extension of the FailsafeHandlerList that allows for the addition
 * of view instances as handlers.
 */
@Singleton
public class AmbariHandlerList extends Handler.Sequence implements ViewInstanceHandlerList {

  /**
   * The target pattern for a view resource request.
   */
  private static final Pattern VIEW_RESOURCE_TARGET_PATTERN =
    Pattern.compile("/api/(\\S+)/views/(\\S+)/versions/(\\S+)/instances/(\\S+)/resources/(\\S+)");

  /**
   * The view registry.
   */
  @Inject
  ViewRegistry viewRegistry;

  /**
   * Session manager.
   */
  @Inject
  SessionHandler sessionHandler;

  /**
   * The web app context provider.
   */
  @Inject
  Provider<WebAppContext> webAppContextProvider;

  /**
   * The persistence filter.
   */
  @Inject
  AmbariPersistFilter persistFilter;

  /**
   * The security filter.
   */
  @Inject
  DelegatingFilterProxy springSecurityFilter;

  /**
   * The security header filter - conditionally adds security-related headers to the HTTP response for Ambari Views requests.
   */
  @Inject
  AmbariViewsSecurityHeaderFilter ambariViewsSecurityHeaderFilter;

  @Inject
  SessionHandlerConfigurer sessionHandlerConfigurer;

  @Inject
  Configuration configuration;

  @Inject
  AmbariErrorHandler ambariErrorHandler;


  /**
   * Mapping of view instance entities to handlers.
   */
  private final Map<ViewInstanceEntity, WebAppContext> viewHandlerMap = new ConcurrentHashMap<>();

  /**
   * The non-view handlers.
   */
  private final java.util.Collection<Handler> nonViewHandlers = new HashSet<>();

  private static final Logger LOG = LoggerFactory.getLogger(AmbariHandlerList.class);


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an AmbariHandlerList.
   */
  public AmbariHandlerList() {
    super(true, Collections.emptyList());
  }


  // ----- HandlerCollection -------------------------------------------------

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    String target = URIUtil.decodePath(request.getHttpURI().getCanonicalPath());

    ViewEntity viewEntity = getTargetView(target);

    if (viewEntity == null) {
      return processHandlers(request, response, callback);
    }

    // View resources must run with their archive class loader before falling back to server handlers.
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      ClassLoader viewClassLoader = viewEntity.getClassLoader();
      if (viewClassLoader == null) {
        LOG.debug("No class loader associated with view {}.", viewEntity.getName());
      } else {
        Thread.currentThread().setContextClassLoader(viewClassLoader);
      }
      return processHandlers(request, response, callback);
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  @Override
  public void addHandler(Handler handler) {
    nonViewHandlers.add(handler);
    super.addHandler(handler);
  }

// ----- ViewInstanceHandler -----------------------------------------------

  @Override
  public void addViewInstance(ViewInstanceEntity viewInstanceDefinition) throws SystemException {
    WebAppContext handler = getHandler(viewInstanceDefinition);
    WebAppContext previousHandler = viewHandlerMap.get(viewInstanceDefinition);
    try {
      super.addHandler(handler);
      if (isRunning()) {
        if (!handler.isRunning()) {
          handler.start();
        }
        // Jetty initializes a cache with the owning context during startup.
        // Attach the server-owned cache only after the view's own startup,
        // matching shareSessionCacheToViews during initial server startup.
        handler.getSessionHandler().setSessionCache(sessionHandler.getSessionCache());
      }
      viewHandlerMap.put(viewInstanceDefinition, handler);
      if (previousHandler != null) {
        super.removeHandler(previousHandler);
      }
    } catch (Exception e) {
      super.removeHandler(handler);
      if (previousHandler == null) {
        viewHandlerMap.remove(viewInstanceDefinition);
      } else {
        viewHandlerMap.put(viewInstanceDefinition, previousHandler);
      }
      throw new SystemException("Caught exception adding a view instance.", e);
    }
  }

  @Override
  public void shareSessionCacheToViews(SessionCache serverSessionCache) {
    for (WebAppContext webAppContext : viewHandlerMap.values()) {
      webAppContext.getSessionHandler().setSessionCache(serverSessionCache);
    }
  }

  @Override
  public void removeViewInstance(ViewInstanceEntity viewInstanceDefinition) {
    Handler handler = viewHandlerMap.get(viewInstanceDefinition);
    if (handler != null) {
      viewHandlerMap.remove(viewInstanceDefinition);
      removeHandler(handler);
    }
  }


  // ----- helper methods ----------------------------------------------------

  // call the handlers until the request is handled
  private boolean processHandlers(Request request, Response response, Callback callback) throws Exception {
    if (!isStarted()) {
      return false;
    }
    return processHandlers(viewHandlerMap.values(), request, response, callback)
        || processHandlers(nonViewHandlers, request, response, callback);
  }

  // call the given handlers until the request is handled; return true if the request is handled
  private boolean processHandlers(java.util.Collection<? extends Handler> handlers, Request request,
      Response response, Callback callback) throws Exception {
    for (Handler handler : handlers) {
      if (handler.handle(request, response, callback)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get a Handler for the given view instance.
   *
   * @param viewInstanceDefinition the view instance definition
   * @return a handler
   * @throws org.apache.ambari.view.SystemException if an handler can not be obtained for the given view instance
   */
  private WebAppContext getHandler(ViewInstanceEntity viewInstanceDefinition)
    throws SystemException {

    ViewEntity viewDefinition = viewInstanceDefinition.getViewEntity();
    WebAppContext webAppContext = webAppContextProvider.get();

    // WebAppContext otherwise records startup failure but returns from start(),
    // which would publish an unavailable handler and discard the working view.
    webAppContext.setThrowUnavailableOnStartupException(true);
    webAppContext.setWar(viewDefinition.getArchive());
    webAppContext.setContextPath(viewInstanceDefinition.getContextPath());
    webAppContext.setClassLoader(viewInstanceDefinition.getViewEntity().getClassLoader());
    webAppContext.setAttribute(ViewContext.CONTEXT_ATTRIBUTE, new ViewContextImpl(viewInstanceDefinition, viewRegistry));
    webAppContext.setSessionHandler(new SharedSessionHandler(sessionHandler));
    webAppContext.addFilter(new FilterHolder(ambariViewsSecurityHeaderFilter), "/*", AmbariServer.DISPATCHER_TYPES);
    webAppContext.addFilter(new FilterHolder(persistFilter), "/*", AmbariServer.DISPATCHER_TYPES);
    webAppContext.addFilter(new FilterHolder(springSecurityFilter), "/*", AmbariServer.DISPATCHER_TYPES);
    webAppContext.setAllowNullPathInContext(true);

    if (webAppContext.getErrorHandler() instanceof ErrorHandler) {
      ErrorHandler errorHandler = (ErrorHandler) webAppContext.getErrorHandler();
      AmbariViewErrorHandlerProxy errorHandlerProxy = new AmbariViewErrorHandlerProxy(errorHandler, ambariErrorHandler);
      errorHandlerProxy.setShowStacks(configuration.isServerShowErrorStacks());
      webAppContext.setErrorHandler(errorHandlerProxy);
    }

    return webAppContext;
  }

  /**
   * Get the view that is the target of the request; null if not a view request.
   *
   * @param target the target of the request
   * @return the view target; null if none
   */
  private ViewEntity getTargetView(String target) {
    Matcher matcher = VIEW_RESOURCE_TARGET_PATTERN.matcher(target);

    return matcher.matches() ? viewRegistry.getDefinition(matcher.group(2), matcher.group(3)) : null;
  }


  // ----- inner class : SharedSessionHandler --------------------------------

  /**
   * A session handler that shares its session manager with another app.
   * This handler DOES NOT attempt stop the shared session manager.
   */
  private class SharedSessionHandler extends SessionHandler {

    // ----- Constructors ----------------------------------------------------

    /**
     * Construct a SharedSessionHandler.
     *
     * @param sessionHandler the shared session manager.
     */
    public SharedSessionHandler(SessionHandler sessionHandler) {
      setSessionIdManager(sessionHandler.getSessionIdManager());
      sessionHandlerConfigurer.configureSessionHandler(this);
    }


    // ----- SessionHandler --------------------------------------------------

    @Override
    protected void doStop() throws Exception {
      // do nothing...
    }
  }
}
