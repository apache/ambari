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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceInstance;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * Factory for {@link Request} instances.
 */
public class RequestFactory {
  /**
   * Create a request instance.
   *
   * @param headers      http headers
   * @param uriInfo      uri information
   * @param requestType  http request type
   * @param resource     associated resource instance
   *
   * @return a new Request instance
   */
  public Request createRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, Request.Type requestType,
                               ResourceInstance resource) {
    switch (requestType) {
      case GET:
        return new GetRequest(headers, body, uriInfo, resource);
      case PUT:
        return new PutRequest(headers, body, uriInfo, resource);
      case DELETE:
        return new DeleteRequest(headers, body, uriInfo, resource);
      case POST:
        return ((uriInfo.getQueryParameters().isEmpty() && body.getQueryString() == null) || body == null) ?
            new PostRequest(headers, body, uriInfo, resource) :
            new QueryPostRequest(headers, body, uriInfo, resource);
      default:
        throw new IllegalArgumentException("Invalid request type: " + requestType);
    }
  }

}
