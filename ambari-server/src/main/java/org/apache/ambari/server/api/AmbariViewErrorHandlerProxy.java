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

import jakarta.servlet.RequestDispatcher;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 * Wraps the given ErrorHandler to log the error stacks
 */
public class AmbariViewErrorHandlerProxy extends ErrorHandler {

  private final ErrorHandler webAppErrorHandler;
  private final AmbariErrorHandler ambariErrorHandler;

  public AmbariViewErrorHandlerProxy(ErrorHandler webAppErrorHandler, AmbariErrorHandler ambariErrorHandler) {
    this.webAppErrorHandler = webAppErrorHandler;
    this.ambariErrorHandler = ambariErrorHandler;
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    if (isInternalError(request, response)) {
      return ambariErrorHandler.handle(request, response, callback);
    }
    return webAppErrorHandler.handle(request, response, callback);
  }

  private boolean isInternalError(Request request, Response response) {
    Throwable th = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    return null != th && response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR;
  }

  @Override
  public void setShowStacks(boolean showStacks) {
    ambariErrorHandler.setShowStacks(showStacks);
    webAppErrorHandler.setShowStacks(showStacks);
  }
}
