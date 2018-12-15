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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class InPredicateTest {

  private static final String TEST_PROPERTY = PropertyHelper.getPropertyId("category", "property");

  @Test
  public void evaluate() {
    String valueInSet = "should be found";
    Predicate predicate = predicateWithSomeItemsIncluding(valueInSet);

    assertTrue(predicate.evaluate(resourceWithPropertyValue(valueInSet)));
    assertFalse(predicate.evaluate(resourceWithPropertyValue("value not in set")));
  }

  @Test
  public void nullSet() {
    Resource resource = resourceWithPropertyValue("any value");
    Predicate predicateWithNullSet = new InPredicate(TEST_PROPERTY, null);

    assertFalse(predicateWithNullSet.evaluate(resource));
  }

  @Test
  public void doesNotContainNullValue() {
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    assertNull(resource.getPropertyValue(TEST_PROPERTY));

    assertFalse(predicateWithSomeItemsIncluding().evaluate(resource));
  }

  @Test
  public void getProperty() {
    assertEquals(TEST_PROPERTY, predicateWithSomeItemsIncluding().getPropertyId());
  }

  @Test
  public void testToString() {
    assertEquals(TEST_PROPERTY + " IN (x, y)", predicateWithItems("x", "y").toString());
  }

  private static InPredicate predicateWithItems(String... items) {
    return predicateWithItems(ImmutableSet.<String>builder().add(items));
  }

  private static InPredicate predicateWithSomeItemsIncluding(String... including) {
    return predicateWithItems(ImmutableSet.<String>builder()
      .add("some value", "other value")
      .add(including));
  }

  private static InPredicate predicateWithItems(ImmutableSet.Builder<String> builder) {
    return new InPredicate(TEST_PROPERTY, builder.build());
  }

  private static Resource resourceWithPropertyValue(String valueInSet) {
    Resource matchingResource = new ResourceImpl(Resource.Type.HostComponent);
    matchingResource.setProperty(TEST_PROPERTY, valueInSet);
    return matchingResource;
  }
}
