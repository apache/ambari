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
import org.apache.ambari.api.controller.spi.ClusterController;
import org.apache.ambari.api.controller.spi.Predicate;
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.PropertyProvider;
import org.apache.ambari.api.controller.spi.Request;
import org.apache.ambari.api.controller.spi.Resource;
import org.apache.ambari.api.controller.spi.ResourceProvider;
import org.apache.ambari.api.controller.spi.Schema;
import org.apache.ambari.api.controller.utilities.PredicateBuilder;
import org.apache.ambari.api.controller.utilities.PredicateHelper;
import org.apache.ambari.api.controller.utilities.Properties;
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
public class ClusterControllerImplTest {

  private static final Set<PropertyId> resourceProviderProperties = new HashSet<PropertyId>();

  static {
    resourceProviderProperties.add(Properties.getPropertyId("p1", "c1"));
    resourceProviderProperties.add(Properties.getPropertyId("p2", "c1"));
    resourceProviderProperties.add(Properties.getPropertyId("p3", "c1"));
    resourceProviderProperties.add(Properties.getPropertyId("p4", "c2"));
  }

  private static final ResourceProvider resourceProvider = new ResourceProvider() {
    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) {

      Set<Resource> resources = new HashSet<Resource>();

      for (int cnt = 0; cnt < 4; ++ cnt) {
        ResourceImpl resource = new ResourceImpl(Resource.Type.Host);

        resource.setProperty(Properties.getPropertyId("p1", "c1"), cnt);
        resource.setProperty(Properties.getPropertyId("p2", "c1"), cnt % 2);
        resource.setProperty(Properties.getPropertyId("p3", "c1"), "foo");
        resource.setProperty(Properties.getPropertyId("p4", "c2"), "bar");
        resources.add(resource);
      }

      return resources;
    }

    @Override
    public Set<PropertyId> getPropertyIds() {
      return resourceProviderProperties;
    }
  };

  private static final Set<PropertyId> propertyProviderProperties = new HashSet<PropertyId>();

  static {
    propertyProviderProperties.add(Properties.getPropertyId("p5", "c3"));
    propertyProviderProperties.add(Properties.getPropertyId("p6", "c3"));
    propertyProviderProperties.add(Properties.getPropertyId("p7", "c4"));
    propertyProviderProperties.add(Properties.getPropertyId("p8", "c4"));
  }

  private static final PropertyProvider propertyProvider = new PropertyProvider() {
    @Override
    public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) {

      int cnt = 0;
      for (Resource resource : resources){
        resource.setProperty(Properties.getPropertyId("p5", "c3"), cnt + 100);
        resource.setProperty(Properties.getPropertyId("p6", "c3"), cnt % 2);
        resource.setProperty(Properties.getPropertyId("p7", "c4"), "monkey");
        resource.setProperty(Properties.getPropertyId("p8", "c4"), "runner");
        ++cnt;
      }
      return resources;
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

  private static final Map<String, PropertyId> keyPropertyIds = new HashMap<String, PropertyId>();

  private static Map<Resource.Type, Schema> schemas = new HashMap<Resource.Type, Schema>();

  static {
    schemas.put(Resource.Type.Host, new SchemaImpl(Resource.Type.HostComponent, resourceProvider, propertyProviders, keyPropertyIds));
  }

  private static final Set<PropertyId> propertyIds = new HashSet<PropertyId>();

  static {
    propertyIds.add(Properties.getPropertyId("p1", "c1"));
    propertyIds.add(Properties.getPropertyId("p2", "c1"));
    propertyIds.add(Properties.getPropertyId("p3", "c2"));
    propertyIds.add(Properties.getPropertyId("p4", "c3"));
    propertyIds.add(Properties.getPropertyId("p5", "c3"));
    propertyIds.add(Properties.getPropertyId("p7", "c4"));
  }

  @Test
  public void testGetResources() {
    ClusterController controller = new ClusterControllerImpl(schemas);

    Request request = new RequestImpl(propertyIds);

    Iterable<Resource> iterable = controller.getResources(Resource.Type.Host, request, null);

    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());
      ++cnt;
    }
    Assert.assertEquals(4, cnt);
  }

  @Test
  public void testGetResourcesWithPredicate() {
    ClusterController controller = new ClusterControllerImpl(schemas);

    Request request = new RequestImpl(propertyIds);

    Predicate predicate = new PredicateBuilder().property("p2", "c1").equals(1).toPredicate();

    Iterable<Resource> iterable = controller.getResources(Resource.Type.Host, request, predicate);

    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());
      ++cnt;
    }
    Assert.assertEquals(2, cnt);
  }
}


