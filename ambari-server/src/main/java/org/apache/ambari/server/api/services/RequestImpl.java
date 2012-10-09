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

import org.apache.ambari.server.api.resources.*;
import org.apache.ambari.server.api.services.parsers.JsonPropertyParser;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.JsonSerializer;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.controller.spi.PropertyId;

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
   * Http Body
   */
  private String m_body;

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
   * @param body               http body
   * @param uriInfo            uri information
   * @param requestType        http request type
   * @param resourceDefinition associated resource definition
   */
  public RequestImpl(HttpHeaders headers, String body, UriInfo uriInfo, Type requestType,
                     ResourceDefinition resourceDefinition) {

    m_headers            = headers;
    m_body               = body;
    m_uriInfo            = uriInfo;
    m_Type               = requestType;
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
    return m_body;
  }

  @Override
  public Map<PropertyId, String> getHttpBodyProperties() {
    return m_body == null ? Collections.<PropertyId, String>emptyMap() :
        getHttpBodyParser().parse(getHttpBody());
  }

  @Override
  public ResultSerializer getResultSerializer() {
    return new JsonSerializer();
  }

  @Override
  public ResultPostProcessor getResultPostProcessor() {
    return new ResultPostProcessorImpl(this);
  }

  @Override
  public PersistenceManager getPersistenceManager() {
    switch (getRequestType()) {
      case PUT:
        return new CreatePersistenceManager();
      case POST:
        return new UpdatePersistenceManager();
      case DELETE:
        return new DeletePersistenceManager();
      case GET:
        throw new IllegalStateException("Tried to get persistence manager for get operation");
      default:
        throw new IllegalStateException("Tried to get persistence manager for unknown operation type");
    }
  }

  private  RequestBodyParser getHttpBodyParser() {
    return new JsonPropertyParser();
  }
}
