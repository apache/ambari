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
package org.apache.ambari.server.controller.predicate;

import junit.framework.Assert;
import org.apache.ambari.server.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import java.util.Set;

/**
 *
 */
public class AndPredicateTest {

  @Test
  public void testApply() {
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    PropertyIdImpl propertyId1 = new PropertyIdImpl("property1", "category1", false);
    PropertyIdImpl propertyId2 = new PropertyIdImpl("property2", "category1", false);
    PropertyIdImpl propertyId3 = new PropertyIdImpl("property3", "category1", false);

    EqualsPredicate predicate1 = new EqualsPredicate(propertyId1, "v1");
    EqualsPredicate predicate2 = new EqualsPredicate(propertyId2, "v2");
    EqualsPredicate predicate3 = new EqualsPredicate(propertyId3, "v3");

    AndPredicate andPredicate = new AndPredicate(predicate1, predicate2, predicate3);

    resource.setProperty(propertyId1, "v1");
    resource.setProperty(propertyId2, "monkey");
    resource.setProperty(propertyId3, "v3");
    Assert.assertFalse(andPredicate.evaluate(resource));

    resource.setProperty(propertyId2, "v2");
    Assert.assertTrue(andPredicate.evaluate(resource));
  }

  @Test
  public void testGetProperties() {
    PropertyIdImpl propertyId1 = new PropertyIdImpl("property1", "category1", false);
    PropertyIdImpl propertyId2 = new PropertyIdImpl("property2", "category1", false);
    PropertyIdImpl propertyId3 = new PropertyIdImpl("property3", "category1", false);

    EqualsPredicate predicate1 = new EqualsPredicate(propertyId1, "v1");
    EqualsPredicate predicate2 = new EqualsPredicate(propertyId2, "v2");
    EqualsPredicate predicate3 = new EqualsPredicate(propertyId3, "v3");

    AndPredicate andPredicate = new AndPredicate(predicate1, predicate2, predicate3);

    Set<PropertyId> ids = andPredicate.getPropertyIds();

    Assert.assertEquals(3, ids.size());
    Assert.assertTrue(ids.contains(propertyId1));
    Assert.assertTrue(ids.contains(propertyId2));
    Assert.assertTrue(ids.contains(propertyId3));
  }

}
