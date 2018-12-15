/*
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
package org.apache.ambari.server.controller.predicate;

import static java.util.stream.Collectors.joining;

import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * Predicate that checks whether a {@link Resource} property is included in a set of values.
 */
public class InPredicate extends CategoryPredicate {

  private final Set<?> values;

  public InPredicate(String propertyId, Set<?> values) {
    super(propertyId);
    this.values = values != null ? ImmutableSet.copyOf(values) : ImmutableSet.of();
  }

  @Override
  public boolean evaluate(Resource resource) {
    Object propertyValue  = resource.getPropertyValue(getPropertyId());
    return values.contains(propertyValue);
  }

  @Override
  public String toString() {
    return String.format("%s IN (%s)", getPropertyId(),
      values.stream().map(Objects::toString).collect(joining(", ")));
  }
}
