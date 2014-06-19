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

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return createPostRequest(headers, body, uriInfo, resource);
      default:
        throw new IllegalArgumentException("Invalid request type: " + requestType);
    }
  }

  /**
   * Create a POST request.  This will either be a standard post request or a query post request.
   * A query post request first applies a query to a collection resource and then creates
   * sub-resources to all matches of the predicate.
   *
   * @param headers   http headers
   * @param uriInfo   uri information
   * @param resource  associated resource instance
   *
   * @return new post request
   */
  private Request createPostRequest(HttpHeaders headers, RequestBody body, UriInfo uriInfo, ResourceInstance resource) {
    boolean batchCreate = false;
    Map<String, String> queryParameters = getQueryParameters(uriInfo, body);
    if (! queryParameters.isEmpty()) {
      ResourceDefinition resourceDefinition = resource.getResourceDefinition();
      Collection<String> directives = resourceDefinition.getCreateDirectives();

      Map<String, String> requestInfoProperties = body.getRequestInfoProperties();
      for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
        if (directives.contains(entry.getKey())) {
          requestInfoProperties.put(entry.getKey(), entry.getValue());
        } else {
          batchCreate = true;
        }
      }
    }

    return (batchCreate) ?
        new QueryPostRequest(headers, body, uriInfo, resource) :
        new PostRequest(headers, body, uriInfo, resource);
  }

  /**
   * Gather query parameters from uri and body query string.
   *
   * @param uriInfo  contains uri info
   * @param body     request body
   *
   * @return map of query parameters or an empty map if no parameters are present
   */
  private Map<String, String> getQueryParameters(UriInfo uriInfo, RequestBody body) {
    Map<String, String> queryParameters = new HashMap<String, String>();
    for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
      queryParameters.put(entry.getKey(), entry.getValue().get(0));
    }

    String bodyQueryString = body.getQueryString();
    if (bodyQueryString != null && ! bodyQueryString.isEmpty()) {
      String[] toks = bodyQueryString.split("&");
      for (String tok : toks) {
        String[] keyVal = tok.split("=");
        queryParameters.put(keyVal[0], keyVal.length == 2 ? keyVal[1] : "");
      }
    }
    return queryParameters;
  }

}
