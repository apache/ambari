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

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base provider tests.
 */
public class BaseProviderTest {
  @Test
  public void testGetProperties() {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("foo");
    propertyIds.add("bar");
    propertyIds.add("cat1/prop1");
    propertyIds.add("cat2/prop2");
    propertyIds.add("cat3/subcat3/prop3");

    BaseProvider provider = new TestProvider(propertyIds);

    Set<String> supportedPropertyIds = provider.getPropertyIds();
    Assert.assertTrue(supportedPropertyIds.containsAll(propertyIds));
  }

  @Test
  public void testCheckPropertyIds() {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("foo");
    propertyIds.add("bar");
    propertyIds.add("cat1/prop1");
    propertyIds.add("cat2/prop2");
    propertyIds.add("cat3/subcat3/prop3");
    propertyIds.add("cat4/subcat4/map");

    BaseProvider provider = new TestProvider(propertyIds);

    Assert.assertTrue(provider.checkPropertyIds(propertyIds).isEmpty());

    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat1")).isEmpty());
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat2")).isEmpty());
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat3")).isEmpty());
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat3/subcat3")).isEmpty());
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat4/subcat4/map")).isEmpty());

    // note that key is not in the set of known property ids.  We allow it if its parent is a known property.
    // this allows for Map type properties where we want to treat the entries as individual properties
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat4/subcat4/map/key")).isEmpty());

    propertyIds.add("badprop");
    propertyIds.add("badcat");

    Set<String> unsupportedPropertyIds = provider.checkPropertyIds(propertyIds);
    Assert.assertFalse(unsupportedPropertyIds.isEmpty());
    Assert.assertEquals(2, unsupportedPropertyIds.size());
    Assert.assertTrue(unsupportedPropertyIds.contains("badprop"));
    Assert.assertTrue(unsupportedPropertyIds.contains("badcat"));
  }

  @Test
  public void testGetRequestPropertyIds() {
    Set<String> providerPropertyIds = new HashSet<String>();
    providerPropertyIds.add("foo");
    providerPropertyIds.add("bar");
    providerPropertyIds.add("cat1/sub1");

    BaseProvider provider = new TestProvider(providerPropertyIds);

    Request request = PropertyHelper.getReadRequest("foo");

    Set<String> requestedPropertyIds = provider.getRequestPropertyIds(request, null);

    Assert.assertEquals(1, requestedPropertyIds.size());
    Assert.assertTrue(requestedPropertyIds.contains("foo"));

    request = PropertyHelper.getReadRequest("foo", "bar");

    requestedPropertyIds = provider.getRequestPropertyIds(request, null);

    Assert.assertEquals(2, requestedPropertyIds.size());
    Assert.assertTrue(requestedPropertyIds.contains("foo"));
    Assert.assertTrue(requestedPropertyIds.contains("bar"));

    request = PropertyHelper.getReadRequest("foo", "baz", "bar", "cat", "cat1/prop1");

    requestedPropertyIds = provider.getRequestPropertyIds(request, null);

    Assert.assertEquals(2, requestedPropertyIds.size());
    Assert.assertTrue(requestedPropertyIds.contains("foo"));
    Assert.assertTrue(requestedPropertyIds.contains("bar"));

    // ask for a property that isn't specified as supported, but its category is... the property
    // should end up in the returned set for the case where the category is a Map property
    request = PropertyHelper.getReadRequest("foo", "cat1/sub1/prop1");

    requestedPropertyIds = provider.getRequestPropertyIds(request, null);

    Assert.assertEquals(2, requestedPropertyIds.size());
    Assert.assertTrue(requestedPropertyIds.contains("foo"));
    Assert.assertTrue(requestedPropertyIds.contains("cat1/sub1/prop1"));
  }

  @Test
  public void testSetResourceProperty() {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("p1");
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");
    propertyIds.add("cat5/sub5");

    Resource resource = new ResourceImpl(Resource.Type.Service);

    Assert.assertNull(resource.getPropertyValue("foo"));

    BaseProvider.setResourceProperty(resource, "foo", "value1", propertyIds);
    Assert.assertEquals("value1", resource.getPropertyValue("foo"));

    BaseProvider.setResourceProperty(resource, "cat2/bar", "value2", propertyIds);
    Assert.assertEquals("value2", resource.getPropertyValue("cat2/bar"));

    Assert.assertNull(resource.getPropertyValue("unsupported"));
    BaseProvider.setResourceProperty(resource, "unsupported", "valueX", propertyIds);
    Assert.assertNull(resource.getPropertyValue("unsupported"));

    // we should allow anything under the category cat5/sub5
    BaseProvider.setResourceProperty(resource, "cat5/sub5/prop5", "value5", propertyIds);
    Assert.assertEquals("value5", resource.getPropertyValue("cat5/sub5/prop5"));
    BaseProvider.setResourceProperty(resource, "cat5/sub5/sub5a/prop5a", "value5", propertyIds);
    Assert.assertEquals("value5", resource.getPropertyValue("cat5/sub5/sub5a/prop5a"));
    // we shouldn't allow anything under the category cat5/sub7
    BaseProvider.setResourceProperty(resource, "cat5/sub7/unsupported", "valueX", propertyIds);
    Assert.assertNull(resource.getPropertyValue("cat5/sub7/unsupported"));
  }

  @Test
  public void testSetResourcePropertyWithMaps() {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("cat1/emptyMapProperty");
    propertyIds.add("cat1/mapProperty");
    propertyIds.add("cat2/mapMapProperty");
    propertyIds.add("cat3/mapProperty3/key2");
    propertyIds.add("cat4/mapMapProperty4/subMap1/key3");
    propertyIds.add("cat4/mapMapProperty4/subMap2");

    Resource resource = new ResourceImpl(Resource.Type.Service);

    // Adding an empty Map as a property should add the actual Map as a property
    Map<String, String> emptyMapProperty = new HashMap<String, String>();
    BaseProvider.setResourceProperty(resource, "cat1/emptyMapProperty", emptyMapProperty, propertyIds);
    Assert.assertTrue(resource.getPropertiesMap().containsKey("cat1/emptyMapProperty"));

    Map<String, String> mapProperty = new HashMap<String, String>();
    mapProperty.put("key1", "value1");
    mapProperty.put("key2", "value2");
    mapProperty.put("key3", "value3");

    // Adding a property of type Map should add all of its keys as sub properties
    // if the map property was requested
    BaseProvider.setResourceProperty(resource, "cat1/mapProperty", mapProperty, propertyIds);
    Assert.assertNull(resource.getPropertyValue("cat1/mapProperty"));
    Assert.assertEquals("value1", resource.getPropertyValue("cat1/mapProperty/key1"));
    Assert.assertEquals("value2", resource.getPropertyValue("cat1/mapProperty/key2"));
    Assert.assertEquals("value3", resource.getPropertyValue("cat1/mapProperty/key3"));

    Map<String, Map<String, String>> mapMapProperty = new HashMap<String, Map<String, String>>();
    Map<String, String> mapSubProperty1 = new HashMap<String, String>();
    mapSubProperty1.put("key1", "value11");
    mapSubProperty1.put("key2", "value12");
    mapSubProperty1.put("key3", "value13");
    mapMapProperty.put("subMap1", mapSubProperty1);
    Map<String, String> mapSubProperty2 = new HashMap<String, String>();
    mapSubProperty2.put("key1", "value21");
    mapSubProperty2.put("key2", "value22");
    mapSubProperty2.put("key3", "value23");
    mapMapProperty.put("subMap2", mapSubProperty2);
    Map<String, String> mapSubProperty3 = new HashMap<String, String>();
    mapMapProperty.put("subMap3", mapSubProperty3);

    // Map of maps ... adding a property of type Map should add all of its keys as sub properties
    // if the map property was requested
    BaseProvider.setResourceProperty(resource, "cat2/mapMapProperty", mapMapProperty, propertyIds);
    Assert.assertNull(resource.getPropertyValue("cat2/mapMapProperty"));
    Assert.assertNull(resource.getPropertyValue("cat2/mapMapProperty/subMap1"));
    Assert.assertNull(resource.getPropertyValue("cat2/mapMapProperty/subMap2"));
    Assert.assertTrue(resource.getPropertiesMap().containsKey("cat2/mapMapProperty/subMap3"));
    Assert.assertEquals("value11", resource.getPropertyValue("cat2/mapMapProperty/subMap1/key1"));
    Assert.assertEquals("value12", resource.getPropertyValue("cat2/mapMapProperty/subMap1/key2"));
    Assert.assertEquals("value13", resource.getPropertyValue("cat2/mapMapProperty/subMap1/key3"));
    Assert.assertEquals("value21", resource.getPropertyValue("cat2/mapMapProperty/subMap2/key1"));
    Assert.assertEquals("value22", resource.getPropertyValue("cat2/mapMapProperty/subMap2/key2"));
    Assert.assertEquals("value23", resource.getPropertyValue("cat2/mapMapProperty/subMap2/key3"));

    Map<String, String> mapProperty3 = new HashMap<String, String>();
    mapProperty3.put("key1", "value1");
    mapProperty3.put("key2", "value2");
    mapProperty3.put("key3", "value3");

    // Adding a property of type Map shouldn't add the map if it wasn't requested and
    // should only add requested keys as sub properties ...
    // only "cat3/mapProperty3/key2" was requested
    BaseProvider.setResourceProperty(resource, "cat3/mapProperty3", mapProperty3, propertyIds);
    Assert.assertNull(resource.getPropertyValue("cat3/mapProperty3"));
    Assert.assertNull(resource.getPropertyValue("cat3/mapProperty3/key1"));
    Assert.assertEquals("value2", resource.getPropertyValue("cat3/mapProperty3/key2"));
    Assert.assertNull(resource.getPropertyValue("cat3/mapProperty3/key3"));

    Map<String, Map<String, String>> mapMapProperty4 = new HashMap<String, Map<String, String>>();
    mapMapProperty4.put("subMap1", mapSubProperty1);
    mapMapProperty4.put("subMap2", mapSubProperty2);
    // Map of maps ... adding a property of type Map shouldn't add the map if it wasn't requested and
    // should only add requested keys as sub properties ...
    // only "cat4/mapMapProperty4/subMap1/key3" and "cat4/mapMapProperty4/subMap2" are requested
    BaseProvider.setResourceProperty(resource, "cat4/mapMapProperty4", mapMapProperty4, propertyIds);
    Assert.assertNull(resource.getPropertyValue("cat4/mapMapProperty4"));
    Assert.assertNull(resource.getPropertyValue("cat4/mapMapProperty4/subMap1"));
    Assert.assertNull(resource.getPropertyValue("cat4/mapMapProperty4/subMap2"));
    Assert.assertNull(resource.getPropertyValue("cat4/mapMapProperty4/subMap1/key1"));
    Assert.assertNull(resource.getPropertyValue("cat4/mapMapProperty4/subMap1/key2"));
    Assert.assertEquals("value13", resource.getPropertyValue("cat4/mapMapProperty4/subMap1/key3"));
    Assert.assertEquals("value21", resource.getPropertyValue("cat4/mapMapProperty4/subMap2/key1"));
    Assert.assertEquals("value22", resource.getPropertyValue("cat4/mapMapProperty4/subMap2/key2"));
    Assert.assertEquals("value23", resource.getPropertyValue("cat4/mapMapProperty4/subMap2/key3"));
  }

  @Test
  public void testRegexpMethods() {
    Set<String> propertyIds = new HashSet<String>();
    String regexp = "cat/$1.replaceAll(\\\"([.])\\\",\\\"/\\\")/key";
    String propertyId = "cat/sub/key";
    String regexp2 = "cat/$1.replaceAll(\\\"([.])\\\",\\\"/\\\")/something/$2/key";
    String propertyId2 = "cat/sub/something/sub2/key";

    String incorrectPropertyId = "some/property/id";
    propertyIds.add(regexp);
    propertyIds.add(regexp2);

    BaseProvider provider = new TestProvider(propertyIds);
    Assert.assertEquals(regexp, provider.getRegExpKey(propertyId));
    Assert.assertNull(provider.getRegExpKey(incorrectPropertyId));
    Assert.assertEquals("sub", provider.getRegexGroups(regexp, propertyId).get(0));
    Assert.assertEquals("sub2", provider.getRegexGroups(regexp2, propertyId2).get(1));
    Assert.assertTrue(provider.getRegexGroups(regexp, incorrectPropertyId).isEmpty());
  }

  static class TestProvider extends BaseProvider {

    public TestProvider(Set<String> propertyIds) {
      super(propertyIds);
    }
  }
}
