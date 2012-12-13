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

import org.apache.ambari.server.controller.predicate.AlwaysPredicate;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.ArrayPredicate;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.ComparisonPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitor;
import org.apache.ambari.server.controller.predicate.UnaryPredicate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A predicate visitor used to simplify by doing the following ...
 *
 * 1) distribute across OR (e.g. A && ( B || C ) becomes ( A && B ) || ( A && C )).
 * This is done because an individual back end request object can not handle OR.  Instead
 * we need to break up the predicate to form multiple requests and take the union of all
 * the responses.
 * 2) convert predicates based on unsupported properties to AlwaysPredicate.
 * Unsupported properties are those not returned by the resource provider.  For these
 * properties we need to wait until the property provider that handles the property has
 * been called.
 * 3) convert predicates based on any operator other than == to AlwaysPredicate.
 * The back end requests can not handle any operator other than equals.  The complete predicate
 * will get applied further down the line if necessary.
 *
 * After visiting a predicate, the visitor should be able to supply a list of predicates that can be
 * used to generate requests to the backend, working around the restrictions above.
 *
 * Note that the results acquired using the generated predicates may be a super set of what is actually
 * desired given the original predicate, but the original predicate will be applied at a suitable time
 * down the line if required.
 */
public class SimplifyingPredicateVisitor implements PredicateVisitor {

  private final Set<String> supportedProperties;
  private BasePredicate lastVisited = null;

  public SimplifyingPredicateVisitor(Set<String> supportedProperties) {
    this.supportedProperties = supportedProperties;
  }

  public List<BasePredicate> getSimplifiedPredicates() {
    if (lastVisited == null) {
      return Collections.emptyList();
    }
    if (lastVisited instanceof OrPredicate) {
      return Arrays.asList(((OrPredicate) lastVisited).getPredicates());
    }
    return Collections.singletonList(lastVisited);
  }

  @Override
  public void acceptComparisonPredicate(ComparisonPredicate predicate) {
    if (predicate instanceof EqualsPredicate &&
        supportedProperties.contains(predicate.getPropertyId())) {
      lastVisited = predicate;
    }
    else {
      lastVisited = AlwaysPredicate.INSTANCE;
    }
  }

  @Override
  public void acceptArrayPredicate(ArrayPredicate arrayPredicate) {
    List<BasePredicate> predicateList = new LinkedList<BasePredicate>();
    boolean hasOrs = false;

    BasePredicate[] predicates = arrayPredicate.getPredicates();
    if (predicates.length > 0) {
      for (BasePredicate predicate : predicates) {
        predicate.accept(this);
        predicateList.add(lastVisited);
        if (lastVisited instanceof OrPredicate) {
          hasOrs = true;
        }
      }
    }
    // distribute so that A && ( B || C ) becomes ( A && B ) || ( A && C )
    if (hasOrs && arrayPredicate instanceof AndPredicate) {
      int size = predicateList.size();
      List<BasePredicate> andPredicateList = new LinkedList<BasePredicate>();

      for (int i = 0; i < size; ++i) {
        for (int j = i + 1; j < size; ++j) {
          andPredicateList.addAll(distribute(predicateList.get(i), predicateList.get(j)));
        }
      }
      lastVisited = OrPredicate.instance(andPredicateList.toArray(new BasePredicate[andPredicateList.size()]));
    }
    else {
      lastVisited = arrayPredicate.create(predicateList.toArray(new BasePredicate[predicateList.size()]));
    }
  }

  @Override
  public void acceptUnaryPredicate(UnaryPredicate predicate) {
    lastVisited = predicate;
  }

  @Override
  public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
    lastVisited = predicate;
  }

  private static List<BasePredicate> distribute(BasePredicate left, BasePredicate right) {

    if (left instanceof OrPredicate) {
      return distributeOr((OrPredicate) left, right);
    }

    if (right instanceof OrPredicate) {
      return distributeOr((OrPredicate) right, left);
    }
    return Collections.singletonList(left.equals(right) ?
        left : AndPredicate.instance(left, right));
  }

  private static List<BasePredicate> distributeOr(OrPredicate orPredicate, BasePredicate other) {
    List<BasePredicate> andPredicateList = new LinkedList<BasePredicate>();
    OrPredicate otherOr = null;

    if (other instanceof OrPredicate) {
      otherOr = (OrPredicate) other;
    }

    for (BasePredicate basePredicate : orPredicate.getPredicates()) {

      if (otherOr != null) {
        andPredicateList.addAll(distributeOr(otherOr, basePredicate));
      }
      else {
        andPredicateList.add(basePredicate.equals(other) ?
            basePredicate : AndPredicate.instance(basePredicate, other));
      }
    }
    return andPredicateList;
  }
}
