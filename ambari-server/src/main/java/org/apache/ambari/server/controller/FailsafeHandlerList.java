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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This extension of {@link HandlerCollection} will call each contained handler
 * in turn until the response is committed, a positive response status is set or
 * an exception from important handler is thrown. Handlers added via
 * {@link #addFailsafeHandler(Handler)} do not stop the polling when they raise
 * an exception.
 *
 */
public class FailsafeHandlerList extends HandlerCollection {

  /**
   * Logger.
   */
  private static Logger LOG = LoggerFactory.getLogger(FailsafeHandlerList.class);

  /**
   * Fail-safe handlers do not stop the processing of the request if they raise an exception.
   */
  private final List<Handler> failsafeHandlers = new ArrayList<Handler>();


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a FailsafeHandlerList.
   */
  public FailsafeHandlerList() {
  }

  /**
   * Construct a FailsafeHandlerList.
   *
   * @param mutableWhenRunning allow for changes while running
   */
  public FailsafeHandlerList(boolean mutableWhenRunning) {
    super(mutableWhenRunning);
  }


  // ----- FailsafeHandlerList -----------------------------------------------

  /**
   * Adds handler to collection and marks it as fail-safe.
   *
   * @param handler fail-safe handler
   */
  public void addFailsafeHandler(Handler handler) {
    addHandler(handler);
    failsafeHandlers.add(handler);
  }


  // ----- HandlerCollection -------------------------------------------------

  @Override
  public void removeHandler(Handler handler) {
    super.removeHandler(handler);
    failsafeHandlers.remove(handler);
  }

  /**
   * @see Handler#handle(String, Request, HttpServletRequest,
   *      HttpServletResponse)
   */
  @Override
  public void handle(String target, Request baseRequest,
      HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    final Handler[] handlers = getHandlers();

    if (handlers != null && isStarted()) {

      List<Handler> nonFailsafeHandlers = new ArrayList<Handler>();

      for (int i = 0; i < handlers.length; i++) {
        final Handler handler = handlers[i];
        // Do all of the fail-safe handlers first...
        if (failsafeHandlers.contains(handler)) {
          try {
            final FailsafeServletResponse responseWrapper = new FailsafeServletResponse(response);
            handler.handle(target, baseRequest, request, responseWrapper);
            if (responseWrapper.isRequestFailed()) {
              response.reset();
              baseRequest.setHandled(false);
            }
          } catch (Exception ex) {
            LOG.warn("Fail-safe handler failed to process request, continuing handler polling", ex);
            continue;
          }
        } else {
          nonFailsafeHandlers.add(handler);
        }
        if (baseRequest.isHandled()) {
          return;
        }
      }

      for (Handler handler : nonFailsafeHandlers) {
        handler.handle(target, baseRequest, request, response);
        if (baseRequest.isHandled()) {
          return;
        }
      }
    }
  }
}
