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

import junit.framework.Assert;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for SimplifyingPredicateVisitor
 */
public class SimplifyingPredicateVisitorTest {

  private static final String PROPERTY_A = PropertyHelper.getPropertyId("category", "A");
  private static final String PROPERTY_B = PropertyHelper.getPropertyId("category", "B");
  private static final String PROPERTY_C = PropertyHelper.getPropertyId("category", "C");
  private static final String PROPERTY_D = PropertyHelper.getPropertyId("category", "D");

  private static final BasePredicate PREDICATE_1 = new PredicateBuilder().property(PROPERTY_A).equals("Monkey").toPredicate();
  private static final BasePredicate PREDICATE_2 = new PredicateBuilder().property(PROPERTY_B).equals("Runner").toPredicate();
  private static final BasePredicate PREDICATE_3 = new AndPredicate(PREDICATE_1, PREDICATE_2);
  private static final BasePredicate PREDICATE_4 = new OrPredicate(PREDICATE_1, PREDICATE_2);
  private static final BasePredicate PREDICATE_5 = new PredicateBuilder().property(PROPERTY_C).equals("Racer").toPredicate();
  private static final BasePredicate PREDICATE_6 = new OrPredicate(PREDICATE_5, PREDICATE_4);
  private static final BasePredicate PREDICATE_7 = new PredicateBuilder().property(PROPERTY_C).equals("Power").toPredicate();
  private static final BasePredicate PREDICATE_8 = new OrPredicate(PREDICATE_6, PREDICATE_7);
  private static final BasePredicate PREDICATE_9 = new AndPredicate(PREDICATE_1, PREDICATE_8);
  private static final BasePredicate PREDICATE_10 = new OrPredicate(PREDICATE_3, PREDICATE_5);
  private static final BasePredicate PREDICATE_11 = new AndPredicate(PREDICATE_4, PREDICATE_10);
  private static final BasePredicate PREDICATE_12 = new PredicateBuilder().property(PROPERTY_D).equals("Installer").toPredicate();
  private static final BasePredicate PREDICATE_13 = new AndPredicate(PREDICATE_1, PREDICATE_12);
  private static final BasePredicate PREDICATE_14 = new PredicateBuilder().property(PROPERTY_D).greaterThan(12).toPredicate();
  private static final BasePredicate PREDICATE_15 = new AndPredicate(PREDICATE_1, PREDICATE_14);

  @Test
  public void testVisit() {
    Set<String> supportedProperties = new HashSet<String>();
    supportedProperties.add(PROPERTY_A);
    supportedProperties.add(PROPERTY_B);
    supportedProperties.add(PROPERTY_C);

    SimplifyingPredicateVisitor visitor = new SimplifyingPredicateVisitor(supportedProperties);

    PREDICATE_1.accept(visitor);

    List<BasePredicate> simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(0));

    PREDICATE_3.accept(visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_3, simplifiedPredicates.get(0));


    PREDICATE_4.accept(visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(2, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(0));
    Assert.assertEquals(PREDICATE_2, simplifiedPredicates.get(1));

    PREDICATE_6.accept(visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(3, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_5, simplifiedPredicates.get(0));
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(1));
    Assert.assertEquals(PREDICATE_2, simplifiedPredicates.get(2));

    PREDICATE_8.accept(visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(4, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_5, simplifiedPredicates.get(0));
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(1));
    Assert.assertEquals(PREDICATE_2, simplifiedPredicates.get(2));
    Assert.assertEquals(PREDICATE_7, simplifiedPredicates.get(3));

    PREDICATE_9.accept(visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(4, simplifiedPredicates.size());
//    Assert.assertEquals(???, simplifiedPredicates.get(0));

    PREDICATE_11.accept(visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(4, simplifiedPredicates.size());
//    Assert.assertEquals(???, simplifiedPredicates.get(0));

    PREDICATE_13.accept(visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(0));

    PREDICATE_15.accept(visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(0));
  }
}
