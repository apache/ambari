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
package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.Comparables;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.GreaterEqualsPredicate;
import org.apache.ambari.server.controller.predicate.GreaterPredicate;
import org.apache.ambari.server.controller.predicate.LessEqualsPredicate;
import org.apache.ambari.server.controller.predicate.LessPredicate;
import org.apache.ambari.server.controller.predicate.NotPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.PropertyId;

import java.util.LinkedList;
import java.util.List;

/**
 * Builder for predicates.
 */
public class PredicateBuilder {

  private PropertyId propertyId;
  private List<BasePredicate> predicates = new LinkedList<BasePredicate>();
  private Operator operator = null;
  private final PredicateBuilder outer;
  private boolean done = false;
  private boolean not = false;

  public PredicateBuilder() {
    this.outer = null;
  }

  private PredicateBuilder(PredicateBuilder outer) {
    this.outer = outer;
  }

  private enum Operator {
    And,
    Or
  }


  public PredicateBuilderWithProperty property(String property, String category, boolean temporal) {
    return property(new PropertyIdImpl(property, category, temporal));
  }

  public PredicateBuilderWithProperty property(String property, String category) {
    return property(property, category, false);
  }

  public PredicateBuilderWithProperty property(String property) {
    return property(property, null);
  }

  public PredicateBuilderWithProperty property(PropertyId id) {
    checkDone();
    propertyId = id;
    return new PredicateBuilderWithProperty();
  }

  public PredicateBuilder not() {
    not = true;
    return this;
  }


  public PredicateBuilder begin() {
    checkDone();
    return new PredicateBuilder(this);
  }

  public BasePredicate toPredicate() {
    return getPredicate();
  }

  private void checkDone() {
    if (done) {
      throw new IllegalStateException("Can't reuse a predicate builder.");
    }
  }

  private PredicateBuilderWithPredicate getPredicateBuilderWithPredicate() {
    return new PredicateBuilderWithPredicate();
  }

  private void addPredicate(BasePredicate predicate) {
    predicates.add(predicate);
  }

  private void handleComparator() {
    if (operator == null) {
      return;
    }

    if (predicates.size() == 0) {
      throw new IllegalStateException("No left operand.");
    }
    BasePredicate predicate;

    switch (operator) {
      case And:
        predicate = new AndPredicate(predicates.toArray(new BasePredicate[predicates.size()]));
        break;
      case Or:
        predicate = new OrPredicate(predicates.toArray(new BasePredicate[predicates.size()]));
        break;
      default:
        throw new IllegalStateException("Unknown operator " + this.operator);
    }
    predicates.clear();
    addPredicate(predicate);
  }

  private BasePredicate getPredicate() {
    handleComparator();

    if (predicates.size() == 1) {
      BasePredicate predicate = predicates.get(0);
      if (not) {
        predicate = new NotPredicate(predicate);
        not = false;
      }
      return predicate;
    }
    throw new IllegalStateException("Can't return a predicate.");
  }

  public class PredicateBuilderWithProperty {

    // ----- Equals -----
    public PredicateBuilderWithPredicate equals(Comparable<String> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new EqualsPredicate(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    public PredicateBuilderWithPredicate equals(Integer value) {
      return equals(Comparables.forInteger(value));
    }

    public PredicateBuilderWithPredicate equals(Float value) {
      return equals(Comparables.forFloat(value));
    }

    public PredicateBuilderWithPredicate equals(Double value) {
      return equals(Comparables.forDouble(value));
    }

    public PredicateBuilderWithPredicate equals(Long value) {
      return equals(Comparables.forLong(value));
    }

    // ----- Greater than -----
    public PredicateBuilderWithPredicate greaterThan(Comparable<String> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new GreaterPredicate(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    public PredicateBuilderWithPredicate greaterThan(Integer value) {
      return greaterThan(Comparables.forInteger(value));
    }

    public PredicateBuilderWithPredicate greaterThan(Float value) {
      return greaterThan(Comparables.forFloat(value));
    }

    public PredicateBuilderWithPredicate greaterThan(Double value) {
      return greaterThan(Comparables.forDouble(value));
    }

    public PredicateBuilderWithPredicate greaterThan(Long value) {
      return greaterThan(Comparables.forLong(value));
    }

    // ----- Greater than equal to -----
    public PredicateBuilderWithPredicate greaterThanEqualTo(Comparable<String> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new GreaterEqualsPredicate(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    public PredicateBuilderWithPredicate greaterThanEqualTo(Integer value) {
      return greaterThanEqualTo(Comparables.forInteger(value));
    }

    public PredicateBuilderWithPredicate greaterThanEqualTo(Float value) {
      return greaterThanEqualTo(Comparables.forFloat(value));
    }

    public PredicateBuilderWithPredicate greaterThanEqualTo(Double value) {
      return greaterThanEqualTo(Comparables.forDouble(value));
    }

    public PredicateBuilderWithPredicate greaterThanEqualTo(Long value) {
      return greaterThanEqualTo(Comparables.forLong(value));
    }

    // ----- Less than -----
    public PredicateBuilderWithPredicate lessThan(Comparable<String> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new LessPredicate(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    public PredicateBuilderWithPredicate lessThan(Integer value) {
      return lessThan(Comparables.forInteger(value));
    }

    public PredicateBuilderWithPredicate lessThan(Float value) {
      return lessThan(Comparables.forFloat(value));
    }

    public PredicateBuilderWithPredicate lessThan(Double value) {
      return lessThan(Comparables.forDouble(value));
    }

    public PredicateBuilderWithPredicate lessThan(Long value) {
      return lessThan(Comparables.forLong(value));
    }

    // ----- Less than equal to -----
    public PredicateBuilderWithPredicate lessThanEqualTo(Comparable<String> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new LessEqualsPredicate(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    public PredicateBuilderWithPredicate lessThanEqualTo(Integer value) {
      return lessThanEqualTo(Comparables.forInteger(value));
    }

    public PredicateBuilderWithPredicate lessThanEqualTo(Float value) {
      return lessThanEqualTo(Comparables.forFloat(value));
    }

    public PredicateBuilderWithPredicate lessThanEqualTo(Double value) {
      return lessThanEqualTo(Comparables.forDouble(value));
    }

    public PredicateBuilderWithPredicate lessThanEqualTo(Long value) {
      return lessThanEqualTo(Comparables.forLong(value));
    }
  }

  public class PredicateBuilderWithPredicate {
    public PredicateBuilder and() {

      if (operator != Operator.And) {
        handleComparator();
        operator = Operator.And;
      }
      return PredicateBuilder.this;
    }

    public PredicateBuilder or() {

      if (operator != Operator.Or) {
        handleComparator();
        operator = Operator.Or;
      }
      return PredicateBuilder.this;
    }

    public BasePredicate toPredicate() {
      if (outer != null) {
        throw new IllegalStateException("Unbalanced block - missing end.");
      }
      done = true;
      return getPredicate();
    }

    public PredicateBuilderWithPredicate end() {
      if (outer == null) {
        throw new IllegalStateException("Unbalanced block - missing begin.");
      }
      outer.addPredicate(getPredicate());
      return outer.getPredicateBuilderWithPredicate();
    }
  }
}
