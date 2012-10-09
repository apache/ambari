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

package org.apache.ambari.api.controller.internal;

import junit.framework.Assert;
import org.apache.ambari.api.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.Schema;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class SchemaImplTest {

  private static final Set<PropertyId> resourceProviderProperties = new HashSet<PropertyId>();

  static {
    resourceProviderProperties.add(PropertyHelper.getPropertyId("p1", "c1"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("p2", "c1"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("p3", "c1"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("p4", "c2"));
  }

  private static final ResourceProvider resourceProvider = new ResourceProvider() {
    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) {
      return null;
    }

    @Override
    public void createResources(Request request) {

    }

    @Override
    public void updateResources(Request request, Predicate predicate) {

    }

    @Override
    public void deleteResources(Predicate predicate) {

    }

    @Override
    public Set<PropertyId> getPropertyIds() {
      return resourceProviderProperties;
    }

    @Override
    public List<PropertyProvider> getPropertyProviders() {
      return propertyProviders;
    }

    @Override
    public Schema getSchema() {
      return null;
    }
  };

  private static final Set<PropertyId> propertyProviderProperties = new HashSet<PropertyId>();

  static {
    propertyProviderProperties.add(PropertyHelper.getPropertyId("p5", "c3"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("p6", "c3"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("p7", "c4"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("p8", "c4"));
  }

  private static final PropertyProvider propertyProvider = new PropertyProvider() {
    @Override
    public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) {
      return null;
    }

    @Override
    public Set<PropertyId> getPropertyIds() {
      return propertyProviderProperties;
    }
  };

  private static final List<PropertyProvider> propertyProviders = new LinkedList<PropertyProvider>();

  static {
    propertyProviders.add(propertyProvider);
  }

  private static final Map<Resource.Type, PropertyId> keyPropertyIds = new HashMap<Resource.Type, PropertyId>();

  static {
    keyPropertyIds.put(Resource.Type.Cluster, PropertyHelper.getPropertyId("p1", "c1"));
    keyPropertyIds.put(Resource.Type.Host, PropertyHelper.getPropertyId("p2", "c1"));
    keyPropertyIds.put(Resource.Type.Component, PropertyHelper.getPropertyId("p3", "c1"));
  }

  @Test
  public void testGetKeyPropertyId() {
    Schema schema = new SchemaImpl(resourceProvider, keyPropertyIds);

    Assert.assertEquals(PropertyHelper.getPropertyId("p1", "c1"), schema.getKeyPropertyId(Resource.Type.Cluster));
    Assert.assertEquals(PropertyHelper.getPropertyId("p2", "c1"), schema.getKeyPropertyId(Resource.Type.Host));
    Assert.assertEquals(PropertyHelper.getPropertyId("p3", "c1"), schema.getKeyPropertyId(Resource.Type.Component));
  }

  @Test
  public void testGetCategories() {
    Schema schema = new SchemaImpl(resourceProvider, keyPropertyIds);

    Map<String, Set<String>> categories = schema.getCategories();
    Assert.assertEquals(4, categories.size());
    Assert.assertTrue(categories.containsKey("c1"));
    Assert.assertTrue(categories.containsKey("c2"));
    Assert.assertTrue(categories.containsKey("c3"));
    Assert.assertTrue(categories.containsKey("c4"));

    Set<String> properties = categories.get("c1");
    Assert.assertEquals(3, properties.size());
    Assert.assertTrue(properties.contains("p1"));
    Assert.assertTrue(properties.contains("p2"));
    Assert.assertTrue(properties.contains("p3"));

    properties = categories.get("c2");
    Assert.assertEquals(1, properties.size());
    Assert.assertTrue(properties.contains("p4"));

    properties = categories.get("c3");
    Assert.assertEquals(2, properties.size());
    Assert.assertTrue(properties.contains("p5"));
    Assert.assertTrue(properties.contains("p6"));

    properties = categories.get("c4");
    Assert.assertEquals(2, properties.size());
    Assert.assertTrue(properties.contains("p7"));
    Assert.assertTrue(properties.contains("p8"));
  }
}
