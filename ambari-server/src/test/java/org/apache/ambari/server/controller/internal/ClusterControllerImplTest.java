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
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cluster controller tests
 */
public class ClusterControllerImplTest {

  private static final Set<String> propertyProviderProperties = new HashSet<String>();

  private static final String UNSUPPORTED_PROPERTY = PropertyHelper.getPropertyId("c1", "unsupported");

  static {
    propertyProviderProperties.add(PropertyHelper.getPropertyId("c3", "p5"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("c3", "p6"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("c4", "p7"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("c4", "p8"));
  }

  private static final PropertyProvider propertyProvider = new PropertyProvider() {
    @Override
    public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) {

      int cnt = 0;
      for (Resource resource : resources){
        resource.setProperty(PropertyHelper.getPropertyId("c3", "p5"), cnt + 100);
        resource.setProperty(PropertyHelper.getPropertyId("c3", "p6"), cnt % 2);
        resource.setProperty(PropertyHelper.getPropertyId("c4", "p7"), "monkey");
        resource.setProperty(PropertyHelper.getPropertyId("c4", "p8"), "runner");
        ++cnt;
      }
      return resources;
    }

    @Override
    public Set<String> getPropertyIds() {
      return propertyProviderProperties;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      if (!propertyProviderProperties.containsAll(propertyIds)) {
        Set<String> unsupportedPropertyIds = new HashSet<String>(propertyIds);
        unsupportedPropertyIds.removeAll(propertyProviderProperties);
        return unsupportedPropertyIds;
      }
      return Collections.emptySet();
    }
  };

  private static final List<PropertyProvider> propertyProviders = new LinkedList<PropertyProvider>();

  static {
    propertyProviders.add(propertyProvider);
  }

  private static final Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

  static {
    keyPropertyIds.put(Resource.Type.Cluster, PropertyHelper.getPropertyId("c1", "p1"));
    keyPropertyIds.put(Resource.Type.Host, PropertyHelper.getPropertyId("c1", "p2"));
  }

  private static final Set<String> resourceProviderProperties = new HashSet<String>();

  static {
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p1"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p2"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p3"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c2", "p4"));
  }

  @Test
  public void testGetResources() throws Exception{
    ClusterController controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));

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
  public void testGetResourcesEmptyRequest() throws Exception{
    ClusterController controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

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
  public void testGetResourcesWithPredicate() throws Exception{
    ClusterController controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));
    propertyIds.add(PropertyHelper.getPropertyId("c2", "p4"));

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    Iterable<Resource> iterable = controller.getResources(Resource.Type.Host, request, predicate);

    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());
      ++cnt;
    }
    Assert.assertEquals(2, cnt);
  }

  @Test
  public void testGetResourcesWithUnsupportedPropertyPredicate() throws Exception{
    ClusterController controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));
    propertyIds.add(PropertyHelper.getPropertyId("c2", "p4"));

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Predicate predicate = new PredicateBuilder().property(UNSUPPORTED_PROPERTY).equals(1).toPredicate();

    try {
      controller.getResources(Resource.Type.Host, request, predicate);
      Assert.fail("Expected an UnsupportedPropertyException for the unsupported properties.");
    } catch (UnsupportedPropertyException e) {
      // Expected
    }
  }

  @Test
  public void testGetResourcesWithUnsupportedPropertyRequest() throws Exception{
    ClusterController controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));
    propertyIds.add(UNSUPPORTED_PROPERTY);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    try {
      controller.getResources(Resource.Type.Host, request, predicate);
      Assert.fail("Expected an UnsupportedPropertyException for the unsupported properties.");
    } catch (UnsupportedPropertyException e) {
      // Expected
    }
  }

  @Test
  public void testCreateResources() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    TestResourceProvider resourceProvider = (TestResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Set<Map<String, Object>> properties = new HashSet<Map<String, Object>>();
    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(PropertyHelper.getPropertyId("c1", "p2"), 2);

    properties.add(propertyMap);

    Request request = PropertyHelper.getCreateRequest(properties);

    controller.createResources(Resource.Type.Host, request);

    Assert.assertEquals(TestResourceProvider.Action.Create, resourceProvider.getLastAction());
    Assert.assertSame(request, resourceProvider.getLastRequest());
    Assert.assertNull(resourceProvider.getLastPredicate());
  }

  @Test
  public void testCreateResourcesWithUnsupportedProperty() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Set<Map<String, Object>> properties = new HashSet<Map<String, Object>>();
    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(UNSUPPORTED_PROPERTY, 2);

    properties.add(propertyMap);

    Request request = PropertyHelper.getCreateRequest(properties);

    try {
      controller.createResources(Resource.Type.Host, request);
      Assert.fail("Expected an UnsupportedPropertyException for the unsupported properties.");
    } catch (UnsupportedPropertyException e) {
      // Expected
    }
  }

  @Test
  public void testUpdateResources() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    TestResourceProvider resourceProvider = (TestResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(PropertyHelper.getPropertyId("c1", "p2"), 2);

    Request request = PropertyHelper.getUpdateRequest(propertyMap);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    controller.updateResources(Resource.Type.Host, request, predicate);

    Assert.assertEquals(TestResourceProvider.Action.Update, resourceProvider.getLastAction());
    Assert.assertSame(request, resourceProvider.getLastRequest());
    Assert.assertSame(predicate, resourceProvider.getLastPredicate());
  }

  @Test
  public void testUpdateResourcesWithUnsupportedPropertyRequest() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(UNSUPPORTED_PROPERTY, 2);

    Request request = PropertyHelper.getUpdateRequest(propertyMap);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    try {
      controller.updateResources(Resource.Type.Host, request, predicate);
      Assert.fail("Expected an UnsupportedPropertyException for the unsupported properties.");
    } catch (UnsupportedPropertyException e) {
      // Expected
    }
  }

  @Test
  public void testUpdateResourcesWithUnsupportedPropertyPredicate() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(PropertyHelper.getPropertyId("c1", "p2"), 2);

    Request request = PropertyHelper.getUpdateRequest(propertyMap);

    Predicate predicate = new PredicateBuilder().property(UNSUPPORTED_PROPERTY).equals(1).toPredicate();

    try {
      controller.updateResources(Resource.Type.Host, request, predicate);
      Assert.fail("Expected an UnsupportedPropertyException for the unsupported properties.");
    } catch (UnsupportedPropertyException e) {
      // Expected
    }
  }

  @Test
  public void testUpdateResourcesResolvePredicate() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    TestResourceProvider resourceProvider = (TestResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(PropertyHelper.getPropertyId("c1", "p2"), 2);

    Request request = PropertyHelper.getUpdateRequest(propertyMap);

    Predicate predicate = new PredicateBuilder().property("c3/p6").equals(1).toPredicate();

    controller.updateResources(Resource.Type.Host, request, predicate);

    Assert.assertEquals(TestResourceProvider.Action.Update, resourceProvider.getLastAction());
    Assert.assertSame(request, resourceProvider.getLastRequest());
    Predicate lastPredicate = resourceProvider.getLastPredicate();
    Assert.assertFalse(predicate.equals(lastPredicate));
    Set<String> predicatePropertyIds = PredicateHelper.getPropertyIds(lastPredicate);
    Collection<String> keyPropertyIds = resourceProvider.getKeyPropertyIds().values();
    Assert.assertEquals(predicatePropertyIds.size(), keyPropertyIds.size());
    Assert.assertTrue(keyPropertyIds.containsAll(predicatePropertyIds));
  }

  @Test
  public void testDeleteResources() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    TestResourceProvider resourceProvider = (TestResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    controller.deleteResources(Resource.Type.Host, predicate);

    Assert.assertEquals(TestResourceProvider.Action.Delete, resourceProvider.getLastAction());
    Assert.assertNull(resourceProvider.getLastRequest());
    Assert.assertSame(predicate, resourceProvider.getLastPredicate());
  }

  @Test
  public void testDeleteResourcesWithUnsupportedProperty() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Predicate predicate = new PredicateBuilder().property(UNSUPPORTED_PROPERTY).equals(1).toPredicate();

    try {
      controller.deleteResources(Resource.Type.Host, predicate);
      Assert.fail("Expected an UnsupportedPropertyException for the unsupported properties.");
    } catch (UnsupportedPropertyException e) {
      // Expected
    }
  }

  @Test
  public void testDeleteResourcesResolvePredicate() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    TestResourceProvider resourceProvider = (TestResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Predicate predicate = new PredicateBuilder().property("c3/p6").equals(1).toPredicate();

    controller.deleteResources(Resource.Type.Host, predicate);

    Assert.assertEquals(TestResourceProvider.Action.Delete, resourceProvider.getLastAction());
    Assert.assertNull(resourceProvider.getLastRequest());
    Predicate lastPredicate = resourceProvider.getLastPredicate();
    Assert.assertFalse(predicate.equals(lastPredicate));
    Set<String> predicatePropertyIds = PredicateHelper.getPropertyIds(lastPredicate);
    Collection<String> keyPropertyIds = resourceProvider.getKeyPropertyIds().values();
    Assert.assertEquals(predicatePropertyIds.size(), keyPropertyIds.size());
    Assert.assertTrue(keyPropertyIds.containsAll(predicatePropertyIds));
  }

  @Test
  public void testGetSchema() {
    ProviderModule module = new TestProviderModule();

    ClusterController controller = new ClusterControllerImpl(module);
    Schema schema = controller.getSchema(Resource.Type.Host);

    ResourceProvider resourceProvider = module.getResourceProvider(Resource.Type.Host);

    Map<Resource.Type, String> keyPropertyIds = resourceProvider.getKeyPropertyIds();
    for (Map.Entry<Resource.Type, String> entry : keyPropertyIds.entrySet()) {
      Assert.assertEquals(entry.getValue(), schema.getKeyPropertyId(entry.getKey()));
    }

    Map<String, Set<String>> categories = schema.getCategoryProperties();
    for (String propertyId : resourceProvider.getPropertyIdsForSchema()) {
      String category = PropertyHelper.getPropertyCategory(propertyId);
      Set<String> properties = categories.get(category);
      Assert.assertNotNull(properties);
      Assert.assertTrue(properties.contains(PropertyHelper.getPropertyName(propertyId)));
    }

    List<PropertyProvider> propertyProviders = module.getPropertyProviders(Resource.Type.Host);

    for (PropertyProvider propertyProvider : propertyProviders) {
      for (String propertyId : propertyProvider.getPropertyIds()) {
        String category = PropertyHelper.getPropertyCategory(propertyId);
        Set<String> properties = categories.get(category);
        Assert.assertNotNull(properties);
        Assert.assertTrue(properties.contains(PropertyHelper.getPropertyName(propertyId)));
      }
    }
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
    private Action lastAction = null;
    private Request lastRequest = null;
    private Predicate lastPredicate = null;

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

      Set<Resource> resources = new HashSet<Resource>();

      for (int cnt = 0; cnt < 4; ++ cnt) {
        ResourceImpl resource = new ResourceImpl(Resource.Type.Host);

        resource.setProperty(PropertyHelper.getPropertyId("c1", "p1"), cnt);
        resource.setProperty(PropertyHelper.getPropertyId("c1", "p2"), cnt % 2);
        resource.setProperty(PropertyHelper.getPropertyId("c1", "p3"), "foo");
        resource.setProperty(PropertyHelper.getPropertyId("c2", "p4"), "bar");
        resources.add(resource);
      }

      return resources;
    }

    @Override
    public RequestStatus createResources(Request request)  {
      lastAction = Action.Create;
      lastRequest = request;
      lastPredicate = null;
      return new RequestStatusImpl(null);
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      lastAction = Action.Update;
      lastRequest = request;
      lastPredicate = predicate;
      return new RequestStatusImpl(null);
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      lastAction = Action.Delete;
      lastRequest = null;
      lastPredicate = predicate;
      return new RequestStatusImpl(null);
    }

    @Override
    public Set<String> getPropertyIdsForSchema() {
      return resourceProviderProperties;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      if (!resourceProviderProperties.containsAll(propertyIds)) {
        Set<String> unsupportedPropertyIds = new HashSet<String>(propertyIds);
        unsupportedPropertyIds.removeAll(resourceProviderProperties);
        return unsupportedPropertyIds;
      }
      return Collections.emptySet();
    }

    @Override
    public Map<Resource.Type, String> getKeyPropertyIds() {
      return keyPropertyIds;
    }

    public Action getLastAction() {
      return lastAction;
    }

    public Request getLastRequest() {
      return lastRequest;
    }

    public Predicate getLastPredicate() {
      return lastPredicate;
    }

    public enum Action {
      Create,
      Update,
      Delete
    }

  }

}


