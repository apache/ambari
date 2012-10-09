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

import org.apache.ambari.api.resources.ResourceDefinition;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * Factory for {@link Request} instances.
 */
public class RequestFactory {
  /**
   * Create a request instance.
   *
   * @param headers            http headers
   * @param uriInfo            uri information
   * @param requestType        http request type
   * @param resourceDefinition associated resource definition
   * @return a new Request instance
   */
  public Request createRequest(HttpHeaders headers, String body, UriInfo uriInfo, Request.Type requestType,
                               ResourceDefinition resourceDefinition) {

    return new RequestImpl(headers, body, uriInfo, requestType, resourceDefinition);
  }
}
