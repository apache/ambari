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

package org.apache.ambari.server.api.handlers;

import org.apache.ambari.server.api.services.Request;

/**
 * Factory for {@link RequestHandler}
 * Returns the appropriate request handler based on the request.
 */
public class RequestHandlerFactory {
  /**
   * Return an instance of the correct request handler based on the request type.
   *
   * @param requestType the request type.  Is one of {@link Request.Type}
   * @return a request handler for the request
   */
  public RequestHandler getRequestHandler(Request.Type requestType) {
    switch (requestType) {
      case GET:
        return new ReadHandler();
      case POST:
        return new CreateHandler();
      case PUT:
        return new UpdateHandler();
      case DELETE:
        return new DeleteHandler();
      case QUERY_POST:
        return new QueryCreateHandler();
      default:
        //todo:
        throw new UnsupportedOperationException("Unsupported Request Type: " + requestType);
    }
  }
}
