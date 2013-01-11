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
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.predicate.*;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.TemporalInfo;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request implementation.
 */
public abstract class BaseRequest implements Request {

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
   * Predicate operators.
   */
  private Pattern m_pattern = Pattern.compile("!=|>=|<=|=|>|<");

  /**
   * Associated resource definition
   */
  private ResourceInstance m_resource;


  /**
   * Constructor.
   *
   * @param headers      http headers
   * @param body         http body
   * @param uriInfo      uri information
   * @param resource     associated resource definition
   */
  public BaseRequest(HttpHeaders headers, String body, UriInfo uriInfo, ResourceInstance resource) {

    m_headers  = headers;
    m_body     = body;
    m_uriInfo  = uriInfo;
    m_resource = resource;
  }

  @Override
  public ResourceInstance getResource() {
    return m_resource;
  }

  @Override
  public String getURI() {
    try {
      return URLDecoder.decode(m_uriInfo.getRequestUri().toASCIIString(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unable to decode URI: " + e, e);
    }
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

    if (qsBegin == -1) return null;

    String[] tokens = uri.substring(qsBegin + 1).split("&");

    Set<BasePredicate> setPredicates = new HashSet<BasePredicate>();
    for (String outerToken : tokens) {
      if (outerToken != null &&  !outerToken.startsWith("fields")) {
        setPredicates.add(outerToken.contains("|") ?
            handleOrPredicate(outerToken) : createPredicate(outerToken));
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
  public Map<String, TemporalInfo> getFields() {
    Map<String, TemporalInfo> mapProperties;
    String partialResponseFields = m_uriInfo.getQueryParameters().getFirst("fields");
    if (partialResponseFields == null) {
      mapProperties = Collections.emptyMap();
    } else {
      Set<String> setMatches = new HashSet<String>();
      // Pattern basically splits a string using ',' as the deliminator unless ',' is between '[' and ']'.
      // Actually, captures char sequences between ',' and all chars between '[' and ']' including ','.
      Pattern re = Pattern.compile("[^,\\[]*?\\[[^\\]]*?\\]|[^,]+");
      Matcher m = re.matcher(partialResponseFields);
      while (m.find()){
        for (int groupIdx = 0; groupIdx < m.groupCount() + 1; groupIdx++) {
          setMatches.add(m.group(groupIdx));
        }
      }

      mapProperties = new HashMap<String, TemporalInfo>(setMatches.size());
      for (String field : setMatches) {
        TemporalInfo temporalInfo = null;
        if (field.contains("[")) {
          String[] temporalData = field.substring(field.indexOf('[') + 1,
              field.indexOf(']')).split(",");
          field = field.substring(0, field.indexOf('['));
          long start = Long.parseLong(temporalData[0].trim());
          long end   = -1;
          long step  = -1;
          if (temporalData.length >= 2) {
            end = Long.parseLong(temporalData[1].trim());
            if (temporalData.length == 3) {
              step = Long.parseLong(temporalData[2].trim());
            }
          }
          temporalInfo = new TemporalInfoImpl(start, end, step);
        }
        mapProperties.put(field, temporalInfo);
      }
    }

    return mapProperties;
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
  public Set<Map<String, Object>> getHttpBodyProperties() {
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

  private BasePredicate createPredicate(String token) {

    Matcher m = m_pattern.matcher(token);
    m.find();

    String propertyId = token.substring(0, m.start());
    String     value      = token.substring(m.end());
    String     operator   = m.group();

    if (operator.equals("=")) {
      return new EqualsPredicate<String>(propertyId, value);
    } else if (operator.equals("!=")) {
      return new NotPredicate(new EqualsPredicate<String>(propertyId, value));
    } else if (operator.equals("<")) {
      return new LessPredicate<String>(propertyId, value);
    } else if (operator.equals(">"))  {
      return new GreaterPredicate<String>(propertyId, value);
    } else if (operator.equals("<=")) {
      return new LessEqualsPredicate<String>(propertyId, value);
    } else if (operator.equals(">=")) {
      return new GreaterEqualsPredicate<String>(propertyId, value);
    } else {
      throw new RuntimeException("Unknown operator provided in predicate: " + operator);
    }
  }

  private BasePredicate handleOrPredicate(String predicate) {
    Set<BasePredicate> setPredicates = new HashSet<BasePredicate>();
    String[] tokens = predicate.split("\\|");
    for (String tok : tokens) {
      setPredicates.add(createPredicate(tok));
    }

    return new OrPredicate(setPredicates.toArray(new BasePredicate[setPredicates.size()]));
  }

  protected RequestBodyParser getHttpBodyParser() {
    return new JsonPropertyParser();
  }
}
