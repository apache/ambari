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
import org.apache.ambari.api.resource.ResourceDefinition;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 */
public class BaseService {

  protected Response handleRequest(HttpHeaders headers, UriInfo uriInfo, Request.RequestType requestType,
                                   ResourceDefinition resourceDefinition) {

    Request req = getRequestFactory().createRequest(headers, uriInfo, requestType, resourceDefinition);
    Result result = getRequestHandler().handleRequest(req);
    Object formattedResult = resourceDefinition.getResultFormatter().format(result, uriInfo);
    return getResponseFactory().createResponse(req.getSerializer().serialize(formattedResult));
  }

  RequestFactory getRequestFactory() {
    return new RequestFactory();
  }

  ResponseFactory getResponseFactory() {
    return new ResponseFactory();
  }

  RequestHandler getRequestHandler() {
    return new DelegatingRequestHandler();
  }
}
