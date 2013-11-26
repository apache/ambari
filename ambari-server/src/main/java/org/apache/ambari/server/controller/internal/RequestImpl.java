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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.TemporalInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Default request implementation.
 */
public class RequestImpl implements Request {

  /**
   * The property ids associated with this request.  Used for requests that
   * get resource values.
   */
  private final Set<String> propertyIds;

  /**
   * The properties associated with this request.  Used for requests that create
   * resources or update resource values.
   */
  private final Set<Map<String, Object>> properties;

  /**
   * Request Info properties.  These are properties that are specific to the request
   * but not to any resource.
   */
  private Map<String, String> requestInfoProperties;

  /**
   * Map of property to temporal info.
   */
  private Map<String, TemporalInfo> m_mapTemporalInfo = new HashMap<String, TemporalInfo>();


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a request.
   *
   * @param propertyIds            property ids associated with the request; may be null
   * @param properties             resource properties associated with the request; may be null
   * @param requestInfoProperties  request properties; may be null
   * @param mapTemporalInfo        temporal info
   */
  public RequestImpl(Set<String> propertyIds, Set<Map<String, Object>> properties,
                     Map<String, String> requestInfoProperties, Map<String,
                     TemporalInfo> mapTemporalInfo) {
    this.propertyIds = propertyIds == null ?
        Collections.unmodifiableSet(new HashSet<String>()) :
        Collections.unmodifiableSet(propertyIds);

    this.properties = properties == null ?
        Collections.unmodifiableSet(new HashSet<Map<String, Object>>()) :
        Collections.unmodifiableSet(properties);

    this.requestInfoProperties = requestInfoProperties == null ?
        Collections.unmodifiableMap(new HashMap<String, String>()) :
        Collections.unmodifiableMap(requestInfoProperties);

    setTemporalInfo(mapTemporalInfo);
  }


  // ----- Request -----------------------------------------------------------

  @Override
  public Set<String> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public Set<Map<String, Object>> getProperties() {
    return properties;
  }

  @Override
  public Map<String, String> getRequestInfoProperties() {
    return requestInfoProperties;
  }

  @Override
  public TemporalInfo getTemporalInfo(String id) {
    return m_mapTemporalInfo == null ? null : m_mapTemporalInfo.get(id);
  }

  private void setTemporalInfo(Map<String, TemporalInfo> mapTemporalInfo) {
    m_mapTemporalInfo = mapTemporalInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestImpl request = (RequestImpl) o;

    return !(properties == null ? request.properties != null : !properties.equals(request.properties)) &&
        !(propertyIds == null ? request.propertyIds != null : !propertyIds.equals(request.propertyIds));
  }

  @Override
  public int hashCode() {
    int result = propertyIds != null ? propertyIds.hashCode() : 0;
    result = 31 * result + (properties != null ? properties.hashCode() : 0);
    return result;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Request:"
        + ", propertyIds=[");
    for (String pId : propertyIds) {
      sb.append(" { propertyName=").append(pId).append(" }, ");
    }
    sb.append(" ], properties=[ ");
    for (Map<String, Object> map : properties) {
      for (Entry<String, Object> entry : map.entrySet()) {
        sb.append(" { propertyName=").append(entry.getKey()).append(", propertyValue=").
            append(entry.getValue()==null?"NULL":entry.getValue().toString()).append(" }, ");
      }
    }
    sb.append(" ], temporalInfo=[");
    if (m_mapTemporalInfo == null) {
      sb.append("null");
    } else {
      for (Entry<String, TemporalInfo> entry :
        m_mapTemporalInfo.entrySet()) {
        sb.append(" { propertyName=").append(entry.getKey()).append(", temporalInfo=").
            append(entry.getValue()==null?"NULL":entry.getValue().toString());
      }
    }
    sb.append(" ]");
    return sb.toString();
  }

}
