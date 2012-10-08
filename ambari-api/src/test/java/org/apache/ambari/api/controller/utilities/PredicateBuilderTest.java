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
package org.apache.ambari.api.controller.utilities;

import junit.framework.Assert;
import org.apache.ambari.api.controller.internal.PropertyIdImpl;
import org.apache.ambari.api.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

/**
 *
 */
public class PredicateBuilderTest {

  @Test
  public void testSimple() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("bar").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testSimpleNot() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");

    /*  ! p1 == "foo" */
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.not().property(p1).equals("foo").toPredicate();

    Assert.assertFalse(predicate1.evaluate(resource));

    /*  ! p1 == "bar" */
    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.not().property(p1).equals("bar").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));
  }

  @Test
  public void testDone() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property(p1).equals("foo").toPredicate();

    // can't reuse a builder after toPredicate is called.
    try {
      pb.property(p1).equals("foo").toPredicate();
      Assert.fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }

    Assert.assertSame(predicate, pb.toPredicate());
  }

  @Test
  public void testSimpleAnd() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").and().property(p2).equals("bar").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").and().property(p2).equals("car").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testSimpleAndNot() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");

    /* p1 == foo and !p2 == bar */
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").and().not().property(p2).equals("bar").toPredicate();

    Assert.assertFalse(predicate1.evaluate(resource));

    /* p1 == foo and !p2 == car */
    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").and().not().property(p2).equals("car").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));
  }

  @Test
  public void testLongAnd() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);
    PropertyId p3 = new PropertyIdImpl("prop3", "cat1", false);
    PropertyId p4 = new PropertyIdImpl("prop4", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");
    resource.setProperty(p4, "dog");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").and().property(p2).equals("bar").and().property(p3).equals("cat").and().property(p4).equals("dog").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").and().property(p2).equals("bar").and().property(p3).equals("cat").and().property(p4).equals("dot").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testSimpleOr() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").or().property(p2).equals("bar").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").or().property(p2).equals("car").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));

    PredicateBuilder pb3 = new PredicateBuilder();
    Predicate predicate3 = pb3.property(p1).equals("fun").or().property(p2).equals("car").toPredicate();

    Assert.assertFalse(predicate3.evaluate(resource));
  }

  @Test
  public void testLongOr() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);
    PropertyId p3 = new PropertyIdImpl("prop3", "cat1", false);
    PropertyId p4 = new PropertyIdImpl("prop4", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");
    resource.setProperty(p4, "dog");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").or().property(p2).equals("bar").or().property(p3).equals("cat").or().property(p4).equals("dog").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").or().property(p2).equals("car").or().property(p3).equals("cat").or().property(p4).equals("dog").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));

    PredicateBuilder pb3 = new PredicateBuilder();
    Predicate predicate3 = pb3.property(p1).equals("fun").or().property(p2).equals("car").or().property(p3).equals("bat").or().property(p4).equals("dot").toPredicate();

    Assert.assertFalse(predicate3.evaluate(resource));
  }

  @Test
  public void testAndOr() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);
    PropertyId p3 = new PropertyIdImpl("prop3", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");

    PredicateBuilder pb1 = new PredicateBuilder();
    Predicate predicate1 = pb1.property(p1).equals("foo").and().property(p2).equals("bar").or().property(p3).equals("cat").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));


    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").and().property(p2).equals("car").or().property(p3).equals("cat").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));


    PredicateBuilder pb3 = new PredicateBuilder();
    Predicate predicate3 = pb3.property(p1).equals("foo").and().property(p2).equals("bar").or().property(p3).equals("can").toPredicate();

    Assert.assertTrue(predicate3.evaluate(resource));


    PredicateBuilder pb4 = new PredicateBuilder();
    Predicate predicate4 = pb4.property(p1).equals("foo").and().property(p2).equals("bat").or().property(p3).equals("can").toPredicate();

    Assert.assertFalse(predicate4.evaluate(resource));
  }


  @Test
  public void testBlocks() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);
    PropertyId p3 = new PropertyIdImpl("prop3", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");


    /*   (p1==foo && p2==bar) || p3 == cat   */
    PredicateBuilder pb1 = new PredicateBuilder();
    Predicate predicate1 = pb1.begin().property(p1).equals("foo").and().property(p2).equals("bar").end().or().property(p3).equals("cat").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    /*   (p1==foo && p2==bat) || p3 == cat   */
    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.begin().property(p1).equals("foo").and().property(p2).equals("bat").end().or().property(p3).equals("cat").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));

    /*   (p1==foo && p2==bar) || p3 == can   */
    PredicateBuilder pb3 = new PredicateBuilder();
    Predicate predicate3 = pb3.begin().property(p1).equals("foo").and().property(p2).equals("bar").end().or().property(p3).equals("can").toPredicate();

    Assert.assertTrue(predicate3.evaluate(resource));

    /*   (p1==foo && p2==bat) || p3 == can   */
    PredicateBuilder pb4 = new PredicateBuilder();
    Predicate predicate4 = pb4.begin().property(p1).equals("foo").and().property(p2).equals("bat").end().or().property(p3).equals("can").toPredicate();

    Assert.assertFalse(predicate4.evaluate(resource));


    /*   p1==foo && (p2==bar || p3 == cat)   */
    PredicateBuilder pb5 = new PredicateBuilder();
    Predicate predicate5 = pb5.property(p1).equals("foo").and().begin().property(p2).equals("bar").or().property(p3).equals("cat").end().toPredicate();

    Assert.assertTrue(predicate5.evaluate(resource));

    /*   p1==foo && (p2==bat || p3 == cat)   */
    PredicateBuilder pb6 = new PredicateBuilder();
    Predicate predicate6 = pb6.property(p1).equals("foo").and().begin().property(p2).equals("bat").or().property(p3).equals("cat").end().toPredicate();

    Assert.assertTrue(predicate6.evaluate(resource));

    /*   p1==foo && (p2==bat || p3 == can)   */
    PredicateBuilder pb7 = new PredicateBuilder();
    Predicate predicate7 = pb7.property(p1).equals("foo").and().begin().property(p2).equals("bat").or().property(p3).equals("can").end().toPredicate();

    Assert.assertFalse(predicate7.evaluate(resource));

    /*   p1==fat && (p2==bar || p3 == cat)   */
    PredicateBuilder pb8 = new PredicateBuilder();
    Predicate predicate8 = pb8.property(p1).equals("fat").and().begin().property(p2).equals("bar").or().property(p3).equals("cat").end().toPredicate();

    Assert.assertFalse(predicate8.evaluate(resource));

    /*   p1==foo && !(p2==bar || p3 == cat)   */
    PredicateBuilder pb9 = new PredicateBuilder();
    Predicate predicate9 = pb9.property(p1).equals("foo").and().not().begin().property(p2).equals("bar").or().property(p3).equals("cat").end().toPredicate();

    Assert.assertFalse(predicate9.evaluate(resource));


    /*   p1==foo && !(p2==bat || p3 == car)   */
    PredicateBuilder pb10 = new PredicateBuilder();
    Predicate predicate10 = pb10.property(p1).equals("foo").and().not().begin().property(p2).equals("bat").or().property(p3).equals("car").end().toPredicate();

    Assert.assertTrue(predicate10.evaluate(resource));
  }

  @Test
  public void testNestedBlocks() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);
    PropertyId p3 = new PropertyIdImpl("prop3", "cat1", false);
    PropertyId p4 = new PropertyIdImpl("prop4", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");
    resource.setProperty(p4, "dog");

    /*   (p1==foo && (p2==bar || p3==cat)) || p4 == dog   */
    PredicateBuilder pb1 = new PredicateBuilder();
    Predicate predicate1 = pb1.
        begin().
        property(p1).equals("foo").and().
        begin().
        property(p2).equals("bar").or().property(p3).equals("cat").
        end().
        end().
        or().property(p4).equals("dog").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));


    /*   (p1==fat && (p2==bar || p3==cat)) || p4 == dot   */
    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.
        begin().
        property(p1).equals("fat").and().
        begin().
        property(p2).equals("bar").or().property(p3).equals("cat").
        end().
        end().
        or().property(p4).equals("dot").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }


  @Test
  public void testUnbalancedBlocks() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);
    PropertyId p3 = new PropertyIdImpl("prop3", "cat1", false);
    PropertyId p4 = new PropertyIdImpl("prop4", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");
    resource.setProperty(p4, "dog");

    /*   (p1==foo && (p2==bar || p3==cat) || p4 == dog   */
    PredicateBuilder pb1 = new PredicateBuilder();
    try {
      pb1.
          begin().
          property(p1).equals("foo").and().
          begin().
          property(p2).equals("bar").or().property(p3).equals("cat").
          end().
          or().property(p4).equals("dog").toPredicate();
      Assert.fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }

    /*   (p1==foo && p2==bar || p3==cat)) || p4 == dog   */
    PredicateBuilder pb2 = new PredicateBuilder();
    try {
      pb2.
          begin().
          property(p1).equals("foo").and().
          property(p2).equals("bar").or().property(p3).equals("cat").
          end().
          end().
          or().property(p4).equals("dog").toPredicate();
      Assert.fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void testAltProperty() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);
    PropertyId p2 = new PropertyIdImpl("prop2", "cat1", false);
    PropertyId p3 = new PropertyIdImpl("prop3", null, false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");


    /*   (p1==foo && p2==bar) || p3 == cat   */
    PredicateBuilder pb1 = new PredicateBuilder();
    Predicate predicate1 = pb1.begin().property("prop1", "cat1", false).equals("foo").and().property("prop2", "cat1").equals("bar").end().or().property("prop3").equals("cat").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));
  }


  @Test
  public void testEqualsString() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("bar").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testEqualsInteger() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 1);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals(1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals(99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testEqualsFloat() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 1);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals(Float.valueOf(1)).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals(Float.valueOf(99)).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testEqualsDouble() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 1.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals(Double.valueOf(1.999)).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals(Double.valueOf(99.998)).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testEqualsLong() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 1L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals(1L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals(99L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterInteger() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThan(1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThan(99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterFloat() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThan((float) 1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThan((float) 99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterDouble() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThan(1.999).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThan(99.998).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterLong() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThan(1L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThan(99L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterThanEqualToInteger() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThanEqualTo(1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThanEqualTo(99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterThanEqualToFloat() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThanEqualTo((float) 1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThanEqualTo((float) 99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterThanEqualToDouble() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThanEqualTo(1.999).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThanEqualTo(99.998).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterThanEqualToLong() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThanEqualTo(1L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThanEqualTo(99L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessInteger() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThan(99).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThan(1).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessFloat() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThan((float) 99).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThan((float) 1).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessDouble() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThan(99.999).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThan(1.998).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessLong() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThan(99L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThan(1L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessThanEqualToInteger() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThanEqualTo(99).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThanEqualTo(1).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessThanEqualToFloat() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThanEqualTo((float) 99).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThanEqualTo((float) 1).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessThanEqualToDouble() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThanEqualTo(99.999).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThanEqualTo(1.998).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessThanEqualToLong() {
    PropertyId p1 = new PropertyIdImpl("prop1", "cat1", false);

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThanEqualTo(99L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThanEqualTo(1L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }
}
