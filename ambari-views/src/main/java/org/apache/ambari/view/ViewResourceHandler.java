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

package org.apache.ambari.view;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Handler object available to the view components used to allow
 * the API framework to handle the request.
 */
public interface ViewResourceHandler {
  /**
   * Enum of request types.
   */
  public enum RequestType {
    GET,
    POST,
    PUT,
    DELETE,
    QUERY_POST
  }

  /**
   * Handle the API request.
   *
   * @param headers      the headers
   * @param ui           the URI info
   * @param requestType  the request type
   * @param resourceId   the resource id; may be null for collection resources
   *
   * @return the response
   */
  public Response handleRequest(HttpHeaders headers, UriInfo ui, RequestType requestType, String resourceId);

  /**
   * Handle the API request with a request type of GET. Same as
   * {@link ViewResourceHandler#handleRequest(HttpHeaders, UriInfo, ViewResourceHandler.RequestType, String)}
   * for {@link ViewResourceHandler.RequestType#GET}.
   *
   * @param headers     the headers
   * @param ui          the URI info
   * @param resourceId  the resource id; may be null for collection resources
   *
   * @return the response
   */
  public Response handleRequest(HttpHeaders headers, UriInfo ui, String resourceId);
}
