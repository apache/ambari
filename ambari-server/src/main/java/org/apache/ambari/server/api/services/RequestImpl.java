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
import org.apache.ambari.server.controller.predicate.*;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  public String getURI() {
    return URLDecoder.decode(m_uriInfo.getRequestUri().toString());
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
  public Predicate getQueryPredicate() {
    //todo: parse during init
    //not using getQueryParameters because it assumes '=' operator
    String uri = getURI();
    int qsBegin = uri.indexOf("?");

    //todo: consider returning an AlwaysPredicate in this case.
    if (qsBegin == -1) return null;

    Pattern pattern = Pattern.compile("!=|>=|<=|=|>|<");
    String qs = uri.substring(qsBegin + 1);
    String[] tokens = qs.split("&");

    Set<Predicate> setPredicates = new HashSet<Predicate>();
    for (String outerToken : tokens) {
      Matcher m = pattern.matcher(outerToken);
      m.find();
      String field = outerToken.substring(0, m.start());
      if (! field.equals("fields")) {
        int tokEnd = m.end();
        String value = outerToken.substring(tokEnd);
        setPredicates.add(createPredicate(field, m.group(), value));
      }
    }

    if (setPredicates.size() == 1) {
      return setPredicates.iterator().next();
    } else if (setPredicates.size() > 1) {
      return new AndPredicate(setPredicates.toArray(new BasePredicate[setPredicates.size()]));
    } else {
      return null;
    }
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
    return getHttpBodyParser().parse(getHttpBody());
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

  private Predicate createPredicate(String field, String operator, String value) {
    PropertyId propertyId = PropertyHelper.getPropertyId(field);

    if (operator.equals("=")) {
      return new EqualsPredicate(propertyId, value);
    } else if (operator.equals("!=")) {
      return new NotPredicate(new EqualsPredicate(propertyId, value));
    } else if (operator.equals("<")) {
      return new LessPredicate(propertyId, value);
    } else if (operator.equals(">"))  {
      return new GreaterPredicate(propertyId, value);
    } else if (operator.equals("<=")) {
      return new LessEqualsPredicate(propertyId, value);
    } else if (operator.equals(">=")) {
      return new GreaterEqualsPredicate(propertyId, value);
    } else {
      throw new RuntimeException("Unknown operator provided in predicate: " + operator);
    }
  }

  private  RequestBodyParser getHttpBodyParser() {
    return new JsonPropertyParser();
  }
}
