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

package org.apache.ambari.api.handlers;

import org.apache.ambari.api.services.Request;
import org.apache.ambari.api.services.Result;

/**
 * Request handler implementation that all requests are funneled through.
 * Provides common handler functionality and delegates to concrete handler.
 */
public class DelegatingRequestHandler implements RequestHandler {
  @Override
  public Result handleRequest(Request request) {
    Result result = getRequestHandlerFactory().getRequestHandler(request.getRequestType()).handleRequest(request);
    request.getResultPostProcessor().process(result);

    return result;
  }

  /**
   * Obtain a factory for the request specific concrete request handlers.
   *
   * @return A request handler factory
   */
  RequestHandlerFactory getRequestHandlerFactory() {
    return new RequestHandlerFactory();
  }
}
