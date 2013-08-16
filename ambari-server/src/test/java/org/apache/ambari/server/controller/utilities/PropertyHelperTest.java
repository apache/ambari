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

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Property helper tests.
 */
public class PropertyHelperTest {

  @Test
  public void testGetPropertyId() {
    Assert.assertEquals("foo", PropertyHelper.getPropertyId("", "foo"));
    Assert.assertEquals("foo", PropertyHelper.getPropertyId(null, "foo"));
    Assert.assertEquals("foo", PropertyHelper.getPropertyId(null, "foo/"));

    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat", ""));
    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat", null));
    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat/", null));

    Assert.assertEquals("cat/foo", PropertyHelper.getPropertyId("cat", "foo"));
    Assert.assertEquals("cat/sub/foo", PropertyHelper.getPropertyId("cat/sub", "foo"));
    Assert.assertEquals("cat/sub/foo", PropertyHelper.getPropertyId("cat/sub", "foo/"));
  }

  @Test
  public void testGetJMXPropertyIds() {

    //version 1
    Map<String, Map<String, PropertyInfo>> metrics = PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1);
    Map<String, PropertyInfo> componentMetrics = metrics.get("HISTORYSERVER");
    Assert.assertNull(componentMetrics);
    componentMetrics = metrics.get("NAMENODE");
    Assert.assertNotNull(componentMetrics);
    PropertyInfo info = componentMetrics.get("metrics/jvm/memHeapUsedM");
    Assert.assertNotNull(info);
    Assert.assertEquals("Hadoop:service=NameNode,name=jvm.memHeapUsedM", info.getPropertyId());

    //version 2
    metrics = PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2);
    componentMetrics = metrics.get("HISTORYSERVER");
    Assert.assertNotNull(componentMetrics);
    componentMetrics = metrics.get("NAMENODE");
    Assert.assertNotNull(componentMetrics);
    info = componentMetrics.get("metrics/jvm/memHeapUsedM");
    Assert.assertNotNull(info);
    Assert.assertEquals("Hadoop:service=NameNode,name=JvmMetrics.MemHeapUsedM", info.getPropertyId());
  }

  @Test
  public void testGetPropertyCategory() {
    String propertyId = "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsRunning";

    String category = PropertyHelper.getPropertyCategory(propertyId);

    Assert.assertEquals("metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)", category);

    category = PropertyHelper.getPropertyCategory(category);

    Assert.assertEquals("metrics/yarn/Queue", category);

    category = PropertyHelper.getPropertyCategory(category);

    Assert.assertEquals("metrics/yarn", category);

    category = PropertyHelper.getPropertyCategory(category);

    Assert.assertEquals("metrics", category);

    category = PropertyHelper.getPropertyCategory(category);

    Assert.assertNull(category);
  }

  @Test
  public void testGetCategories() {
    String propertyId = "metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsRunning";

    Set<String> categories = PropertyHelper.getCategories(Collections.singleton(propertyId));

    Assert.assertTrue(categories.contains("metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)"));
    Assert.assertTrue(categories.contains("metrics/yarn/Queue"));
    Assert.assertTrue(categories.contains("metrics/yarn"));
    Assert.assertTrue(categories.contains("metrics"));

    String propertyId2 = "foo/bar/baz";
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(propertyId);
    propertyIds.add(propertyId2);

    categories = PropertyHelper.getCategories(propertyIds);

    Assert.assertTrue(categories.contains("metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)"));
    Assert.assertTrue(categories.contains("metrics/yarn/Queue"));
    Assert.assertTrue(categories.contains("metrics/yarn"));
    Assert.assertTrue(categories.contains("metrics"));
    Assert.assertTrue(categories.contains("foo/bar"));
    Assert.assertTrue(categories.contains("foo"));
  }

  @Test
  public void testContainsArguments() {
    Assert.assertFalse(PropertyHelper.containsArguments("foo"));
    Assert.assertFalse(PropertyHelper.containsArguments("foo/bar"));
    Assert.assertFalse(PropertyHelper.containsArguments("foo/bar/baz"));

    Assert.assertTrue(PropertyHelper.containsArguments("foo/bar/$1/baz"));
    Assert.assertTrue(PropertyHelper.containsArguments("foo/bar/$1/baz/$2"));
    Assert.assertTrue(PropertyHelper.containsArguments("$1/foo/bar/$2/baz"));
    Assert.assertTrue(PropertyHelper.containsArguments("$1/foo/bar/$2/baz/$3"));

    Assert.assertTrue(PropertyHelper.containsArguments("metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)"));

    Assert.assertFalse(PropertyHelper.containsArguments("$X/foo/bar/$Y/baz/$Z"));
  }
}

