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
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.junit.Ignore;
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

      int cnt = 0;
      for (Resource resource : resources){
        resource.setProperty(PropertyHelper.getPropertyId("p5", "c3"), cnt + 100);
        resource.setProperty(PropertyHelper.getPropertyId("p6", "c3"), cnt % 2);
        resource.setProperty(PropertyHelper.getPropertyId("p7", "c4"), "monkey");
        resource.setProperty(PropertyHelper.getPropertyId("p8", "c4"), "runner");
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

  private static final Map<Resource.Type, PropertyId> keyPropertyIds = new HashMap<Resource.Type, PropertyId>();

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

      Set<Resource> resources = new HashSet<Resource>();

      for (int cnt = 0; cnt < 4; ++ cnt) {
        ResourceImpl resource = new ResourceImpl(Resource.Type.Host);

        resource.setProperty(PropertyHelper.getPropertyId("p1", "c1"), cnt);
        resource.setProperty(PropertyHelper.getPropertyId("p2", "c1"), cnt % 2);
        resource.setProperty(PropertyHelper.getPropertyId("p3", "c1"), "foo");
        resource.setProperty(PropertyHelper.getPropertyId("p4", "c2"), "bar");
        resources.add(resource);
      }

      return resources;
    }

    @Override
    public RequestStatus createResources(Request request) {
      return new RequestStatusImpl(null);
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) {
      return new RequestStatusImpl(null);
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) {
      return new RequestStatusImpl(null);
    }

    @Override
    public Set<PropertyId> getPropertyIds() {
      return resourceProviderProperties;
    }

    @Override
    public Map<Resource.Type, PropertyId> getKeyPropertyIds() {
      return keyPropertyIds;
    }
  };

  private static final Set<PropertyId> propertyIds = new HashSet<PropertyId>();

  static {
    propertyIds.add(PropertyHelper.getPropertyId("p1", "c1"));
    propertyIds.add(PropertyHelper.getPropertyId("p2", "c1"));
    propertyIds.add(PropertyHelper.getPropertyId("p3", "c2"));
    propertyIds.add(PropertyHelper.getPropertyId("p4", "c3"));
    propertyIds.add(PropertyHelper.getPropertyId("p5", "c3"));
    propertyIds.add(PropertyHelper.getPropertyId("p7", "c4"));
  }

  @Test
  public void testGetResources() throws Exception{
    ClusterController controller = new ClusterControllerImpl(new TestProviderModule());

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Iterable<Resource> iterable = controller.getResources(Resource.Type.Host, request, null);

    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());
      ++cnt;
    }
    Assert.assertEquals(4, cnt);
  }

  @Test
  @Ignore
  public void testGetResourcesWithPredicate() throws Exception{
    ClusterController controller = new ClusterControllerImpl(new TestProviderModule());

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Predicate predicate = new PredicateBuilder().property("p2", "c1").equals(1).toPredicate();

    Iterable<Resource> iterable = controller.getResources(Resource.Type.Host, request, predicate);

    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());
      ++cnt;
    }
    Assert.assertEquals(2, cnt);
  }

  @Ignore
  @Test
  public void testGetSchema() {
    ProviderModule module = new TestProviderModule();
    ClusterController controller = new ClusterControllerImpl(module);

//    Assert.assertEquals(, controller.getSchema(Resource.Type.Host));
  }

  private static class TestProviderModule implements ProviderModule {
    private Map<Resource.Type, ResourceProvider> providers = new HashMap<Resource.Type, ResourceProvider>();

    private TestProviderModule() {
      providers.put(Resource.Type.Cluster, new TestResourceProvider());
      providers.put(Resource.Type.Service, new TestResourceProvider());
      providers.put(Resource.Type.Component, new TestResourceProvider());
      providers.put(Resource.Type.Host, new TestResourceProvider());
      providers.put(Resource.Type.HostComponent, new TestResourceProvider());
    }

    @Override
    public ResourceProvider getResourceProvider(Resource.Type type) {
      return providers.get(type);
    }

    @Override
    public List<PropertyProvider> getPropertyProviders(Resource.Type type) {
      return propertyProviders;
    }
  }

  private static class TestResourceProvider implements ResourceProvider {
    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) {

      Set<Resource> resources = new HashSet<Resource>();

      for (int cnt = 0; cnt < 4; ++ cnt) {
        ResourceImpl resource = new ResourceImpl(Resource.Type.Host);

        resource.setProperty(PropertyHelper.getPropertyId("p1", "c1"), cnt);
        resource.setProperty(PropertyHelper.getPropertyId("p2", "c1"), cnt % 2);
        resource.setProperty(PropertyHelper.getPropertyId("p3", "c1"), "foo");
        resource.setProperty(PropertyHelper.getPropertyId("p4", "c2"), "bar");
        resources.add(resource);
      }

      return resources;
    }

    @Override
    public RequestStatus createResources(Request request) {
      return new RequestStatusImpl(null);
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) {
      return new RequestStatusImpl(null);
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) {
      return new RequestStatusImpl(null);
    }

    @Override
    public Set<PropertyId> getPropertyIds() {
      return resourceProviderProperties;
    }

    @Override
    public Map<Resource.Type, PropertyId> getKeyPropertyIds() {
      return keyPropertyIds;
    }
  }

}


