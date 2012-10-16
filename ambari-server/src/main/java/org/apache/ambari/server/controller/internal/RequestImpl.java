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

import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default request implementation.
 */
public class RequestImpl implements Request {

  /**
   * The property ids associated with this request.  Used for requests that
   * get resource values.
   */
  private final Set<PropertyId> propertyIds;

  /**
   * The properties associated with this request.  Used for requests that create
   * resources or update resource values.
   */
  private final Set<Map<PropertyId, Object>> properties;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a request.
   *
   * @param propertyIds  the property ids associated with the request; may be null
   * @param properties   the properties associated with the request; may be null
   */
  public RequestImpl(Set<PropertyId> propertyIds, Set<Map<PropertyId, Object>> properties) {
    this.propertyIds = propertyIds == null ?
        Collections.unmodifiableSet(new HashSet<PropertyId>()) :
        Collections.unmodifiableSet(propertyIds);

    this.properties = properties == null ?
        Collections.unmodifiableSet(new HashSet<Map<PropertyId, Object>>()) :
        Collections.unmodifiableSet(properties);
  }


  // ----- Request -----------------------------------------------------------

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public Set<Map<PropertyId, Object>> getProperties() {
    return properties;
  }

  @Override
  public TemporalInfo getTemporalInfo(PropertyId id) {
    return new TemporalInfoImpl();
  }


  public static class TemporalInfoImpl implements TemporalInfo {
    @Override
    public Long getStartTime() {
      return null;  //TODO
    }

    @Override
    public Long getEndTime() {
      return null;  //TODO
    }

    @Override
    public Long getStep() {
      return null;  //TODO
    }
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
}
