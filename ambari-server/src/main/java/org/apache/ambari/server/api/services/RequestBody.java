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

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the http body of the request.
 */
public class RequestBody {

  /**
   * The associated query.
   */
  private String m_query;

  /**
   * The associated partial response fields.
   */
  private String m_fields;

  /**
   * The body properties.
   */
  private Set<NamedPropertySet> m_propertySets = new HashSet<NamedPropertySet>();

  /**
   * The request body.  Query and partial response data is stripped before setting.
   */
  private String m_body;


  /**
   * Set the query string.
   *
   * @param query the query string from the body
   */
  public void setQueryString(String query) {
    m_query = query;
  }

  /**
   * Obtain that query that was specified in the body.
   *
   * @return the query from the body or null if no query was present in the body
   */
  public String getQueryString() {
    return m_query;
  }

  /**
   * Set the partial response fields from the body.
   *
   * @param fields  the partial response fields
   */
  public void setPartialResponseFields(String fields) {
    m_fields = fields;
  }

  /**
   * Obtain the partial response fields that were specified in the body.
   *
   * @return  the partial response fields or null if not specified in the body
   */
  public String getPartialResponseFields() {
    return m_fields;
  }

  /**
   * Add a property set.
   * A property set is a set of related properties and values.
   * For example, if the body contained properties for three different resources to
   * be created, each would be represented as distinct property set.
   *
   * @param propertySet  the property set to add
   */
  public void addPropertySet(NamedPropertySet propertySet) {
    m_propertySets.add(propertySet);
  }

  /**
   * Obtain all property sets or an empty set if no properties were specified in the body.
   *
   * @return  all property sets or an empty set
   */
  public Set<NamedPropertySet> getPropertySets() {
    return m_propertySets;
  }

  /**
   * Set the body from the request.
   *
   * @param body the request body
   */
  public void setBody(String body) {
    m_body = body;
  }

  /**
   * Obtain the request body.
   *
   * @return the request body
   */
  public String getBody() {
    return m_body;
  }
}
