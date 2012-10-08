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

import org.apache.ambari.api.resource.*;
import org.apache.ambari.api.services.serializers.JsonSerializer;
import org.apache.ambari.api.services.serializers.ResultSerializer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;

/**
 * Request implementation.
 */
public class RequestImpl implements Request {

  /**
   * URI information
   */
  private UriInfo m_uriInfo;

  /**
   * Http headers
   */
  private HttpHeaders m_headers;

  /**
   * Http request type
   */
  private Type m_Type;

  /**
   * Associated resource definition
   */
  private ResourceDefinition m_resourceDefinition;

  /**
   * Constructor.
   *
   * @param headers            http headers
   * @param uriInfo            uri information
   * @param requestType        http request type
   * @param resourceDefinition associated resource definition
   */
  public RequestImpl(HttpHeaders headers, UriInfo uriInfo, Type requestType, ResourceDefinition resourceDefinition) {
    m_uriInfo = uriInfo;
    m_headers = headers;
    m_Type = requestType;
    m_resourceDefinition = resourceDefinition;
  }

  @Override
  public ResourceDefinition getResourceDefinition() {
    return m_resourceDefinition;
  }

  @Override
  public URI getURI() {
    return m_uriInfo.getRequestUri();
  }

  @Override
  public Type getRequestType() {
    return m_Type;
  }

  @Override
  public int getAPIVersion() {
    return 0;
  }

  @Override
  public Map<String, String> getQueryPredicates() {
    return null;
  }

  @Override
  public Set<String> getPartialResponseFields() {
    String partialResponseFields = m_uriInfo.getQueryParameters().getFirst("fields");
    if (partialResponseFields == null) {
      return Collections.emptySet();
    } else {
      return new HashSet<String>(Arrays.asList(partialResponseFields.split(",")));
    }
  }

  @Override
  public Map<String, List<String>> getHttpHeaders() {
    return m_headers.getRequestHeaders();
  }

  @Override
  public String getHttpBody() {
    return null;
  }

  @Override
  public ResultSerializer getResultSerializer() {
    return new JsonSerializer();
  }

  @Override
  public ResultPostProcessor getResultPostProcessor() {
    return new ResultPostProcessorImpl(this);
  }
}
