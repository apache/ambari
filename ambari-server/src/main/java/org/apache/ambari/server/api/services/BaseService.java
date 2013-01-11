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

import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.handlers.RequestHandlerFactory;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactory;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.controller.spi.Resource;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * Provides common functionality to all services.
 */
public abstract class BaseService {

  /**
   * Factory for creating resource instances.
   */
  private ResourceInstanceFactory m_resourceFactory = new ResourceInstanceFactoryImpl();

  /**
   * Factory for creating request handlers.
   */
  private RequestHandlerFactory m_handlerFactory = new RequestHandlerFactory();

  /**
   * All requests are funneled through this method so that common logic can be executed.
   * This consists of creating a {@link Request} instance, invoking the correct {@link RequestHandler} and
   * applying the proper {@link ResultSerializer} to the result.
   *
   * @param headers      http headers
   * @param body         http body
   * @param uriInfo      uri information
   * @param requestType  http request type
   * @param resource     resource instance that is being acted on
   *
   * @return the response of the operation in serialized form
   */
  protected Response handleRequest(HttpHeaders headers, String body, UriInfo uriInfo, Request.Type requestType,
                                   ResourceInstance resource) {

    Request request = getRequestFactory().createRequest(
        headers, body, uriInfo, requestType, resource);

    Result result = getRequestHandler(request.getRequestType()).handleRequest(request);
    if (! result.getStatus().isErrorState()) {
      request.getResultPostProcessor().process(result);
    }

    return Response.status(result.getStatus().getStatusCode()).entity(
        request.getResultSerializer().serialize(result)).build();
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
   * Obtain the appropriate RequestHandler for the request.
   *
   * @param requestType  the request type
   *
   * @return the request handler to invoke
   */
  RequestHandler getRequestHandler(Request.Type requestType) {
    return m_handlerFactory.getRequestHandler(requestType);
  }

  /**
   * Create a resource instance.
   *
   * @param type    the resource type
   * @param mapIds  all primary and foreign key properties and values
   *
   * @return a newly created resource instance
   */
  ResourceInstance createResource(Resource.Type type, Map<Resource.Type, String> mapIds) {
    return m_resourceFactory.createResource(type, mapIds);
  }
}
