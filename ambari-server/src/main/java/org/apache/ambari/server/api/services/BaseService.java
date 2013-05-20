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
import org.apache.ambari.server.api.resources.ResourceInstanceFactory;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.services.parsers.BodyParseException;
import org.apache.ambari.server.api.services.parsers.JsonRequestBodyParser;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.JsonSerializer;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.controller.spi.Resource;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provides common functionality to all services.
 */
public abstract class BaseService {

  /**
   * Factory for creating resource instances.
   */
  private ResourceInstanceFactory m_resourceFactory = new ResourceInstanceFactoryImpl();

  /**
   * Result serializer.
   */
  private ResultSerializer m_serializer = new JsonSerializer();


  /**
   * All requests are funneled through this method so that common logic can be executed.
   * Creates a request instance and invokes it's process method.
   *
   * @param headers      http headers
   * @param body         http body
   * @param uriInfo      uri information
   * @param requestType  http request type
   * @param resource     resource instance that is being acted on
   *
   * @return the response of the operation in serialized form
   */
  protected Response handleRequest(HttpHeaders headers, String body, UriInfo uriInfo,
                                   Request.Type requestType, ResourceInstance resource) {

    Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));
    try {
      Set<RequestBody> requestBodySet = getBodyParser().parse(body);

      Iterator<RequestBody> iterator = requestBodySet.iterator();
      while (iterator.hasNext() && result.getStatus().getStatus().equals(ResultStatus.STATUS.OK)) {
        RequestBody requestBody = iterator.next();

        Request request = getRequestFactory().createRequest(
            headers, requestBody, uriInfo, requestType, resource);

        result = request.process();
      }
    } catch (BodyParseException e) {
      result =  new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, e.getMessage()));
    }

    return Response.status(result.getStatus().getStatusCode()).entity(
        getResultSerializer().serialize(result)).build();
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

  protected ResultSerializer getResultSerializer() {
    return m_serializer;
  }

  protected RequestBodyParser getBodyParser() {
    return new JsonRequestBodyParser();
  }
}
