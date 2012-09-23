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

package org.apache.ambari.api.controller.predicate;

import org.apache.ambari.api.controller.spi.PropertyId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Predicate which evaluates an array of predicates.
 */
public abstract class ArrayPredicate implements BasePredicate {
  private final BasePredicate[] predicates;
  private final Set<PropertyId> propertyIds = new HashSet<PropertyId>();

  public ArrayPredicate(BasePredicate... predicates) {
    this.predicates = predicates;
    for (BasePredicate predicate : predicates) {
      propertyIds.addAll(predicate.getPropertyIds());
    }
  }

  public BasePredicate[] getPredicates() {
    return predicates;
  }

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArrayPredicate)) return false;

    ArrayPredicate that = (ArrayPredicate) o;

    if (propertyIds != null ? !propertyIds.equals(that.propertyIds) : that.propertyIds != null) return false;

    // don't care about array order
    List<BasePredicate> listPredicates = Arrays.asList(predicates);
    if (listPredicates.size() != that.predicates.length) return false;
    for (BasePredicate predicate : predicates) {
      if (!listPredicates.contains(predicate)) return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = predicates != null ? Arrays.hashCode(predicates) : 0;
    result = 31 * result + (propertyIds != null ? propertyIds.hashCode() : 0);
    return result;
  }

  @Override
  public void accept(PredicateVisitor visitor) {
    visitor.acceptArrayPredicate(this);
  }

  public abstract String getOperator();
}
