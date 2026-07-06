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

package org.apache.ambari.server.api;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.util.proxy.MethodHandler;

/**
 * Wraps the given ErrorHandler to log the error stacks
 */
public class AmbariViewErrorHandlerProxy extends ErrorHandler implements MethodHandler {

  private final static Logger LOGGER = LoggerFactory.getLogger(AmbariViewErrorHandlerProxy.class);

  private final ErrorHandler webAppErrorHandler;
  private final AmbariErrorHandler ambariErrorHandler;

  public AmbariViewErrorHandlerProxy(ErrorHandler webAppErrorHandler, AmbariErrorHandler ambariErrorHandler) {
    this.webAppErrorHandler = webAppErrorHandler;
    this.ambariErrorHandler = ambariErrorHandler;
  }


  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

    if (isInternalError(request, response)) {
      //invoke the ambari error handler
      ambariErrorHandler.handle(target, baseRequest, request, response);
    } else {
      //invoke the original errorhandler
      webAppErrorHandler.handle(target, baseRequest, request, response);
    }
  }

  @Override
  public void doError(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

    if (isInternalError(request, response)) {
      //invoke the ambari error handler
      ambariErrorHandler.handle(target, baseRequest, request, response);
    } else {
      //invoke the original errorhandler
      webAppErrorHandler.doError(target, baseRequest, request, response);
    }
  }

  private boolean isInternalError(HttpServletRequest request, HttpServletResponse response) {
    Throwable th = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    return null != th && response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR;
  }

  @Override
  public void setShowStacks(boolean showStacks) {
    ambariErrorHandler.setShowStacks(showStacks);
    webAppErrorHandler.setShowStacks(showStacks);
  }

  @Override
  public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
    LOGGER.debug("invoked method: " + thisMethod.getName());
    Method m = findDeclaredMethod(this.getClass(), thisMethod);
    if (m != null) {
      return m.invoke(this, args);
    }
    m = findMethod(webAppErrorHandler.getClass(), thisMethod);
    if (m != null) {
      return m.invoke(webAppErrorHandler, args);
    }
    return null;
  }

  private Method findDeclaredMethod(Class<?> clazz, Method method) {
    try {
      return clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private Method findMethod(Class<?> clazz, Method method) {
    try {
      return clazz.getMethod(method.getName(), method.getParameterTypes());
    } catch (NoSuchMethodException e) {
      return null;
    }
  }


}
