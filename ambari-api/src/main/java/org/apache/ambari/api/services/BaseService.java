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

package org.apache.ambari.api.services;

import org.apache.ambari.api.handlers.DelegatingRequestHandler;
import org.apache.ambari.api.handlers.RequestHandler;
import org.apache.ambari.api.resources.ResourceDefinition;
import org.apache.ambari.api.services.serializers.ResultSerializer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Provides common functionality to all services.
 */
public abstract class BaseService {

  /**
   * All requests are funneled through this method so that common logic can be executed.
   * This consists of creating a {@link Request} instance, invoking the correct {@link RequestHandler} and
   * applying the proper {@link ResultSerializer} to the result.
   *
   *
   *
   * @param headers            http headers
   * @param body
   * @param uriInfo            uri information
   * @param requestType        http request type
   * @param resourceDefinition resource definition that is being acted on
   * @return the response of the operation in serialized form
   */
  protected Response handleRequest(HttpHeaders headers, String body, UriInfo uriInfo, Request.Type requestType,
                                   ResourceDefinition resourceDefinition) {

    Request request = getRequestFactory().createRequest(headers, body, uriInfo, requestType, resourceDefinition);
    Result result = getRequestHandler().handleRequest(request);

    return getResponseFactory().createResponse(request.getResultSerializer().serialize(result, uriInfo));
  }

  /**
   * Obtain the factory from which to create Request instances.
   *
   * @return the Request factory
   */
  RequestFactory getRequestFactory() {
    return new RequestFactory();
  }

  /**
   * Obtain the factory from which to create Response instances.
   *
   * @return the Response factory
   */
  ResponseFactory getResponseFactory() {
    return new ResponseFactory();
  }

  /**
   * Obtain the appropriate RequestHandler for the request.  At this time all requests are funneled through
   * a delegating request handler which will ultimately delegate the request to the appropriate concrete
   * request handler.
   *
   * @return the request handler to invoke
   */
  RequestHandler getRequestHandler() {
    return new DelegatingRequestHandler();
  }
}
