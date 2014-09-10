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
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Wraps standard wrapper for {@link HttpServletResponse} to cancel sending
 * errors on failed requests.
 */
public class FailsafeServletResponse extends HttpServletResponseWrapper {
  /**
   * Indicates that request failed.
   */
  private boolean error;

  /**
   * List of errors which should not be consumed by fail-safe handler.
   */
  private List<Integer> allowedErrors = Arrays.asList(HttpServletResponse.SC_FORBIDDEN);

  /**
   * Constructor.
   *
   * @param response response to be wrapped
   */
  public FailsafeServletResponse(HttpServletResponse response) {
    super(response);
  }

  @Override
  public void sendError(int sc) throws IOException {
    if (allowedErrors.contains(sc)) {
      super.sendError(sc);
    } else {
      error = true;
    }
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    if (allowedErrors.contains(sc)) {
      super.sendError(sc, msg);
    } else {
      error = true;
    }
  }

  /**
   * Indicates that request failed to execute.
   *
   * @return true if request failed
   */
  public boolean isRequestFailed() {
    return error;
  }
}
