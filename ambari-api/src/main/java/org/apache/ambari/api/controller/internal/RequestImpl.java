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

package org.apache.ambari.api.controller.internal;

import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Request;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default request implementation.
 */
public class RequestImpl implements Request {

  private final Set<PropertyId> propertyIds;
  private final Set<Map<PropertyId, String>> properties;

  public RequestImpl(Set<PropertyId> propertyIds, Set<Map<PropertyId, String>> properties) {
    this.propertyIds = propertyIds == null ? Collections.unmodifiableSet(new HashSet<PropertyId>()) : Collections.unmodifiableSet(propertyIds);
    this.properties = properties == null ? Collections.unmodifiableSet(new HashSet<Map<PropertyId, String>>()) : Collections.unmodifiableSet(properties);
  }

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public Set<Map<PropertyId, String>> getProperties() {
    return properties;
  }

  @Override
  public TemporalInfo getTemporalInfo(PropertyId id) {
    return null;  //TODO
  }
}
