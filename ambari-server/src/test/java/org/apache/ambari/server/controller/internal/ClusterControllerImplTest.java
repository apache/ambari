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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    keyPropertyIds.put(Resource.Type.Cluster, PropertyHelper.getPropertyId("Hosts", "cluster_name"));
    keyPropertyIds.put(Resource.Type.Host, PropertyHelper.getPropertyId("Hosts", "host_name"));
  }

  private static final Set<String> resourceProviderProperties = new HashSet<String>();

  static {
    resourceProviderProperties.add(PropertyHelper.getPropertyId("Hosts", "cluster_name"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("Hosts", "host_name"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p1"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p2"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p3"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c2", "p4"));
    // add the categories
    resourceProviderProperties.add("Hosts");
    resourceProviderProperties.add("c1");
    resourceProviderProperties.add("c2");
  }

  @Test
  public void testGetResources() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Iterable<Resource> iterable = controller.getResourceIterable(Resource.Type.Host, request, null);

    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());
      ++cnt;
    }
    Assert.assertEquals(4, cnt);
  }

  @Test
  public void testGetResourcesPageFromStart() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get the first two
    PageRequest pageRequest = new PageRequestImpl(PageRequest.StartingPoint.Beginning, 2, 0, null, null);
    PageResponse pageResponse = controller.getResources(Resource.Type.Host, request, null, pageRequest, null);

    Iterable<Resource> iterable = pageResponse.getIterable();
    List<Resource> list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(2, list.size());
    Assert.assertEquals("host:0", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());
    Assert.assertEquals("host:1", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(1).getType());
    //total hosts created in TestHostResourceProvider, not only on this page
    Assert.assertEquals(4, pageResponse.getTotalResourceCount().intValue());

    // get the first three
    pageRequest = new PageRequestImpl(PageRequest.StartingPoint.Beginning, 3, 0, null, null);
    pageResponse = controller.getResources(Resource.Type.Host, request, null, pageRequest, null);

    iterable = pageResponse.getIterable();
    list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(3, list.size());
    Assert.assertEquals("host:0", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());
    Assert.assertEquals("host:1", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(1).getType());
    Assert.assertEquals("host:2", (String) list.get(2).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(2).getType());
    //total hosts created in TestHostResourceProvider, not only on this page
    Assert.assertEquals(4, pageResponse.getTotalResourceCount().intValue());
  }

  @Test
  public void testGetResourcesSortedByProperty() throws Exception {
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));
    propertyIds.add(PropertyHelper.getPropertyId("c2", "p4"));

    Request request = PropertyHelper.getReadRequest(propertyIds);

    // Ascending
    List<SortRequestProperty> sortRequestProperties =
      Collections.singletonList(new SortRequestProperty("Hosts/host_name", SortRequest.Order.ASC));
    SortRequest sortRequest = new SortRequestImpl(sortRequestProperties);

    Iterable<Resource> iterable = controller.getResources(Resource.Type.Host,
      request, null, null, sortRequest).getIterable();

    List<Resource> list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }

    Assert.assertEquals(4, list.size());
    Assert.assertEquals("host:0", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:1", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:2", (String) list.get(2).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:3", (String) list.get(3).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));

    // Descending
    sortRequestProperties = Collections.singletonList(
      new SortRequestProperty("Hosts/host_name", SortRequest.Order.DESC));
    sortRequest = new SortRequestImpl(sortRequestProperties);

    iterable = controller.getResources(Resource.Type.Host,
      request, null, null, sortRequest).getIterable();

    list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }

    Assert.assertEquals(4, list.size());
    Assert.assertEquals("host:3", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:2", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:1", (String) list.get(2).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:0", (String) list.get(3).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
  }

  @Test
  public void testGetResourcesSortedByMultiProperty() throws Exception {
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));
    propertyIds.add(PropertyHelper.getPropertyId("c2", "p4"));

    Request request = PropertyHelper.getReadRequest(propertyIds);
    List<SortRequestProperty> sortRequestProperties =
      new ArrayList<SortRequestProperty>() {{
        add(new SortRequestProperty("c1/p2", SortRequest.Order.DESC));
        add(new SortRequestProperty("c1/p1", SortRequest.Order.DESC));
      }};
    SortRequest sortRequest = new SortRequestImpl(sortRequestProperties);

    Iterable<Resource> iterable = controller.getResources(Resource.Type.Host,
      request, null, null, sortRequest).getIterable();

    List<Resource> list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }

    Assert.assertEquals(4, list.size());
    Assert.assertEquals("host:3", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:1", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:2", (String) list.get(2).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:0", (String) list.get(3).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
  }

  @Test
  public void testGetResourcesPageFromOffset() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get the middle two (1 - 2)
    PageRequest pageRequest = new PageRequestImpl(PageRequest.StartingPoint.OffsetStart, 2, 1, null, null);
    PageResponse pageResponse = controller.getResources(Resource.Type.Host, request, null, pageRequest, null);

    Assert.assertEquals(1, pageResponse.getOffset());
    Assert.assertEquals("host:0", pageResponse.getPreviousResource().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:3", pageResponse.getNextResource().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));

    Iterable<Resource> iterable = pageResponse.getIterable();
    List<Resource> list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(2, list.size());
    Assert.assertEquals("host:1", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());
    Assert.assertEquals("host:2", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(1).getType());
    //total hosts created in TestHostResourceProvider, not only on this page
    Assert.assertEquals(4, pageResponse.getTotalResourceCount().intValue());

    // get the last three (0 - 2)
    pageRequest = new PageRequestImpl(PageRequest.StartingPoint.OffsetStart, 3, 0, null, null);
    pageResponse = controller.getResources(Resource.Type.Host, request, null, pageRequest, null);

    Assert.assertEquals(0, pageResponse.getOffset());
    Assert.assertNull(pageResponse.getPreviousResource());
    Assert.assertEquals("host:3", pageResponse.getNextResource().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));

    iterable = pageResponse.getIterable();
    list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(3, list.size());
    Assert.assertEquals("host:0", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());
    Assert.assertEquals("host:1", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(1).getType());
    Assert.assertEquals("host:2", (String) list.get(2).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(2).getType());
    // Check total count
    Assert.assertEquals(4, pageResponse.getTotalResourceCount().intValue());
  }

  @Test
  public void testGetResourcesPageToEnd() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get the last two
    PageRequest pageRequest = new PageRequestImpl(PageRequest.StartingPoint.End, 2, 0, null, null);
    PageResponse pageResponse = controller.getResources(Resource.Type.Host, request, null, pageRequest, null);

    Iterable<Resource> iterable = pageResponse.getIterable();
    List<Resource> list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(2, list.size());
    Assert.assertEquals("host:2", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());
    Assert.assertEquals("host:3", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(1).getType());
    //total hosts created in TestHostResourceProvider, not only on this page
    Assert.assertEquals(4, pageResponse.getTotalResourceCount().intValue());

    // get the last three
    pageRequest = new PageRequestImpl(PageRequest.StartingPoint.End, 3, 0, null, null);
    pageResponse = controller.getResources(Resource.Type.Host, request, null, pageRequest, null);

    iterable = pageResponse.getIterable();
    list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(3, list.size());
    Assert.assertEquals("host:1", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());
    Assert.assertEquals("host:2", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(1).getType());
    Assert.assertEquals("host:3", (String) list.get(2).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(2).getType());
    // Check total count
    Assert.assertEquals(4, pageResponse.getTotalResourceCount().intValue());
  }

  @Test
  public void testGetResourcesPageToOffset() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get the middle two (1 - 2)
    PageRequest pageRequest = new PageRequestImpl(PageRequest.StartingPoint.OffsetEnd, 2, 2, null, null);
    PageResponse pageResponse = controller.getResources(Resource.Type.Host, request, null, pageRequest, null);

    Assert.assertEquals(1, pageResponse.getOffset());
    Assert.assertEquals("host:0", pageResponse.getPreviousResource().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals("host:3", pageResponse.getNextResource().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    //total hosts created in TestHostResourceProvider, not only on this page
    Assert.assertEquals(4, pageResponse.getTotalResourceCount().intValue());

    Iterable<Resource> iterable = pageResponse.getIterable();
    List<Resource> list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(2, list.size());
    Assert.assertEquals("host:1", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());
    Assert.assertEquals("host:2", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(1).getType());

    // get the last three (0 - 2)
    pageRequest = new PageRequestImpl(PageRequest.StartingPoint.OffsetEnd, 3, 2, null, null);
    pageResponse = controller.getResources(Resource.Type.Host, request, null, pageRequest, null);

    Assert.assertEquals(0, pageResponse.getOffset());
    Assert.assertNull(pageResponse.getPreviousResource());
    Assert.assertEquals("host:3", pageResponse.getNextResource().getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));

    iterable = pageResponse.getIterable();
    list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(3, list.size());
    Assert.assertEquals("host:0", (String) list.get(0).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());
    Assert.assertEquals("host:1", (String) list.get(1).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(1).getType());
    Assert.assertEquals("host:2", (String) list.get(2).getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(2).getType());
    // Check total count
    Assert.assertEquals(4, pageResponse.getTotalResourceCount().intValue());
  }

  @Test
  public void testGetResourcesEmptyRequest() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Iterable<Resource> iterable = controller.getResourceIterable(Resource.Type.Host, request, null);

    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());
      ++cnt;
    }
    Assert.assertEquals(4, cnt);
  }

  @Test
  public void testGetResourcesCheckOrder() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Iterable<Resource> iterable = controller.getResourceIterable(Resource.Type.Host, request, null);

    String lastHostName = null;
    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());

      String hostName = (String) resource.getPropertyValue(PropertyHelper.getPropertyId("Hosts", "host_name"));

      if (lastHostName != null) {
        Assert.assertTrue(hostName.compareTo(lastHostName) > 0);
      }
      lastHostName = hostName;
      ++cnt;
    }
    Assert.assertEquals(4, cnt);
  }

  @Test
  public void testGetResourcesWithPredicate() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));
    propertyIds.add(PropertyHelper.getPropertyId("c2", "p4"));

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    Iterable<Resource> iterable = controller.getResourceIterable(Resource.Type.Host, request, predicate);

    int cnt = 0;
    for (Resource resource : iterable) {
      Assert.assertEquals(Resource.Type.Host, resource.getType());
      ++cnt;
    }
    Assert.assertEquals(2, cnt);
  }

  @Test
  public void testGetResourcesWithUnsupportedPropertyPredicate() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));
    propertyIds.add(PropertyHelper.getPropertyId("c2", "p4"));

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Predicate predicate = new PredicateBuilder().property(UNSUPPORTED_PROPERTY).equals(1).toPredicate();

    try {
      controller.getResourceIterable(Resource.Type.Host, request, predicate);
      Assert.fail("Expected an UnsupportedPropertyException for the unsupported properties.");
    } catch (UnsupportedPropertyException e) {
      // Expected
    }
  }

  @Test
  public void testGetResourcesWithUnsupportedPropertyRequest() throws Exception{
    ClusterControllerImpl controller = new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(PropertyHelper.getPropertyId("c1", "p1"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p2"));
    propertyIds.add(PropertyHelper.getPropertyId("c1", "p3"));
    propertyIds.add(UNSUPPORTED_PROPERTY);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    try {
      controller.getResourceIterable(Resource.Type.Host, request, predicate);
      Assert.fail("Expected an UnsupportedPropertyException for the unsupported properties.");
    } catch (UnsupportedPropertyException e) {
      // Expected
    }
  }

  @Test
  public void testGetResourcesSortedWithPredicateWithItemsTotal() throws Exception{

    ClusterControllerImpl controller =
      new ClusterControllerImpl(new TestProviderModule());

    Set<String> propertyIds = new HashSet<String>();

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Predicate predicate =
      new PredicateBuilder().property("c1/p2").equals(1).toPredicate();
    List<SortRequestProperty> sortRequestProperties = Collections.singletonList(
      new SortRequestProperty("Hosts/host_name", SortRequest.Order.DESC));
    SortRequest sortRequest = new SortRequestImpl(sortRequestProperties);

    // get the first one
    PageRequest pageRequest =
      new PageRequestImpl(PageRequest.StartingPoint.Beginning, 1, 0, null, null);
    PageResponse pageResponse =
      controller.getResources(Resource.Type.Host, request, predicate, pageRequest, sortRequest);

    Iterable<Resource> iterable = pageResponse.getIterable();
    List<Resource> list = new LinkedList<Resource>();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(1, list.size());
    //total hosts after applying the filter, not only on this page
    Assert.assertEquals(2, pageResponse.getTotalResourceCount().intValue());
    // DESC sorted
    Assert.assertEquals("host:3", (String) list.get(0).getPropertyValue(
      PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());

    pageRequest =
      new PageRequestImpl(PageRequest.StartingPoint.OffsetStart, 1, 1, null, null);
    pageResponse = controller.getResources(
      Resource.Type.Host, request, predicate, pageRequest, sortRequest);

    iterable = pageResponse.getIterable();
    list.clear();

    for (Resource resource : iterable) {
      list.add(resource);
    }
    Assert.assertEquals(1, list.size());
    //total hosts after applying the filter, not only on this page
    Assert.assertEquals(2, pageResponse.getTotalResourceCount().intValue());
    // DESC sorted
    Assert.assertEquals("host:1", (String) list.get(0).getPropertyValue(
      PropertyHelper.getPropertyId("Hosts", "host_name")));
    Assert.assertEquals(Resource.Type.Host, list.get(0).getType());

  }

  @Test
  public void testCreateResources() throws Exception{
    TestProviderModule providerModule = new TestProviderModule();
    TestHostResourceProvider resourceProvider = (TestHostResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Set<Map<String, Object>> properties = new HashSet<Map<String, Object>>();
    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(PropertyHelper.getPropertyId("c1", "p2"), 2);

    properties.add(propertyMap);

    Request request = PropertyHelper.getCreateRequest(properties, null);

    controller.createResources(Resource.Type.Host, request);

    Assert.assertEquals(TestHostResourceProvider.Action.Create, resourceProvider.getLastAction());
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

    Request request = PropertyHelper.getCreateRequest(properties, null);

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
    TestHostResourceProvider resourceProvider = (TestHostResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(PropertyHelper.getPropertyId("c1", "p2"), 2);

    Request request = PropertyHelper.getUpdateRequest(propertyMap, null);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    controller.updateResources(Resource.Type.Host, request, predicate);

    Assert.assertEquals(TestHostResourceProvider.Action.Update, resourceProvider.getLastAction());
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

    Request request = PropertyHelper.getUpdateRequest(propertyMap, null);

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

    Request request = PropertyHelper.getUpdateRequest(propertyMap, null);

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
    TestHostResourceProvider resourceProvider = (TestHostResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(PropertyHelper.getPropertyId("c1", "p1"), 99);
    propertyMap.put(PropertyHelper.getPropertyId("c1", "p2"), 2);

    Request request = PropertyHelper.getUpdateRequest(propertyMap, null);

    Predicate predicate = new PredicateBuilder().property("c3/p6").equals(1).toPredicate();

    controller.updateResources(Resource.Type.Host, request, predicate);

    Assert.assertEquals(TestHostResourceProvider.Action.Update, resourceProvider.getLastAction());
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
    TestHostResourceProvider resourceProvider = (TestHostResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Predicate predicate = new PredicateBuilder().property("c1/p2").equals(1).toPredicate();

    controller.deleteResources(Resource.Type.Host, predicate);

    Assert.assertEquals(TestHostResourceProvider.Action.Delete, resourceProvider.getLastAction());
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
    TestHostResourceProvider resourceProvider = (TestHostResourceProvider) providerModule.getResourceProvider(Resource.Type.Host);
    ClusterController controller = new ClusterControllerImpl(providerModule);

    Predicate predicate = new PredicateBuilder().property("c3/p6").equals(1).toPredicate();

    controller.deleteResources(Resource.Type.Host, predicate);

    Assert.assertEquals(TestHostResourceProvider.Action.Delete, resourceProvider.getLastAction());
    Assert.assertNull(resourceProvider.getLastRequest());
    Predicate lastPredicate = resourceProvider.getLastPredicate();
    Assert.assertFalse(predicate.equals(lastPredicate));
    Set<String> predicatePropertyIds = PredicateHelper.getPropertyIds(lastPredicate);
    Collection<String> keyPropertyIds = resourceProvider.getKeyPropertyIds().values();
    Assert.assertEquals(predicatePropertyIds.size(), keyPropertyIds.size());
    Assert.assertTrue(keyPropertyIds.containsAll(predicatePropertyIds));
  }

  @Test
  public void testComparator() {

    TestProviderModule providerModule = new TestProviderModule();
    ClusterControllerImpl controller = new ClusterControllerImpl(providerModule);

    Comparator<Resource> comparator = controller.getComparator();

    Resource resource1 = new ResourceImpl(Resource.Type.Host);
    Resource resource2 = new ResourceImpl(Resource.Type.Host);
    Resource resource3 = new ResourceImpl(Resource.Type.Service);

    Assert.assertEquals(0, comparator.compare(resource1, resource2));
    Assert.assertEquals(0, comparator.compare(resource2, resource1));
    Assert.assertTrue(comparator.compare(resource1, resource3) < 0);
    Assert.assertTrue(comparator.compare(resource3, resource1) > 0);

    resource1.setProperty(PropertyHelper.getPropertyId("Hosts", "cluster_name"), "c1");
    resource1.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "h1");

    resource2.setProperty(PropertyHelper.getPropertyId("Hosts", "cluster_name"), "c1");
    resource2.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "h1");

    Assert.assertEquals(0, comparator.compare(resource1, resource2));
    Assert.assertEquals(0, comparator.compare(resource2, resource1));

    resource2.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "h2");

    Assert.assertTrue(comparator.compare(resource1, resource2) < 0);
    Assert.assertTrue(comparator.compare(resource2, resource1) > 0);

    resource2.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "h1");

    resource1.setProperty("p1", "foo");
    resource2.setProperty("p1", "foo");

    Assert.assertEquals(0, comparator.compare(resource1, resource2));
    Assert.assertEquals(0, comparator.compare(resource2, resource1));

    resource2.setProperty("p1", "bar");

    Assert.assertFalse(comparator.compare(resource1, resource2) == 0);
    Assert.assertFalse(comparator.compare(resource2, resource1) == 0);
  }

  @Test
  public void testPopulateResources_allTypes() throws Exception {

    TestProviderModule providerModule = new TestProviderModule();
    ClusterControllerImpl controller = new ClusterControllerImpl(providerModule);

    Request request = PropertyHelper.getReadRequest();
    Predicate predicate = new PredicateBuilder().property("c3/p6").equals(1).toPredicate();

    for (Resource.Type type : Resource.Type.values()) {
      Resource resource = new ResourceImpl(type);
      // verify that we can call populateResources for all resource types even if there
      // are no property providers defined for that type.
      controller.populateResources(type, Collections.singleton(resource), request, predicate);
    }
  }

  public static class TestProviderModule implements ProviderModule {
    private Map<Resource.Type, ResourceProvider> providers = new HashMap<Resource.Type, ResourceProvider>();

    public TestProviderModule() {

      for (Resource.Type type : Resource.Type.values()) {
        providers.put(type, new TestResourceProvider(type));
      }
      providers.put(Resource.Type.Cluster, new TestClusterResourceProvider());
      providers.put(Resource.Type.Host, new TestHostResourceProvider());


      providers.put(Resource.Type.Stack, new TestStackResourceProvider());
      providers.put(Resource.Type.StackVersion, new TestStackVersionResourceProvider());
      providers.put(Resource.Type.OperatingSystem, new TestOperatingSystemResourceProvider());
      providers.put(Resource.Type.Repository, new TestRepositoryResourceProvider());



    }

    @Override
    public ResourceProvider getResourceProvider(Resource.Type type) {
      return providers.get(type);
    }

    @Override
    public List<PropertyProvider> getPropertyProviders(Resource.Type type) {
      if (type.equals(Resource.Type.Configuration)) {
        // simulate a resource type with no property providers.
        return null;
      }
      return propertyProviders;
    }
  }

  private static class TestResourceProvider extends AbstractResourceProvider {

    private TestResourceProvider(Resource.Type type) {
      super(PropertyHelper.getPropertyIds(type), PropertyHelper.getKeyPropertyIds(type));
    }

    @Override
    public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
      throw new UnsupportedOperationException(); // not needed for testing
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return Collections.emptySet();
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      throw new UnsupportedOperationException(); // not needed for testing
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      throw new UnsupportedOperationException(); // not needed for testing
    }

    @Override
    protected Set<String> getPKPropertyIds() {
      return Collections.emptySet();
    }

    protected Set<Resource> getResources(Resource.Type type, Predicate predicate, String keyPropertyId, Set<String> keyPropertyValues)
        throws SystemException, UnsupportedPropertyException, NoSuchParentResourceException, NoSuchResourceException {
      Set<Resource> resources = new HashSet<Resource>();

      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {

        Set<Resource> resources2 = new HashSet<Resource>();

        if (!propertyMap.containsKey(keyPropertyId)) {
          for (String keyPropertyValue : keyPropertyValues) {
            ResourceImpl resource = new ResourceImpl(type);
            resource.setProperty(keyPropertyId, keyPropertyValue);
            resources2.add(resource);
          }
        } else {
          resources2.add(new ResourceImpl(type));
        }

        for (Resource resource : resources2) {
          for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
            resource.setProperty(entry.getKey(), entry.getValue());
          }
        }
        resources.addAll(resources2);
      }
      return resources;
    }
  }

  private static class TestClusterResourceProvider extends TestResourceProvider {
    private TestClusterResourceProvider() {
      super(Resource.Type.Cluster);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      ResourceImpl resource = new ResourceImpl(Resource.Type.Cluster);

      resource.setProperty(PropertyHelper.getPropertyId("Clusters", "cluster_name"), "cluster");

      return Collections.<Resource>singleton(resource);
    }
  }

  private static class TestHostResourceProvider extends TestResourceProvider {
    private Action lastAction = null;
    private Request lastRequest = null;
    private Predicate lastPredicate = null;

    private TestHostResourceProvider() {
      super(Resource.Type.Host);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

      Set<Resource> resources = new HashSet<Resource>();

      for (int cnt = 0; cnt < 4; ++ cnt) {
        ResourceImpl resource = new ResourceImpl(Resource.Type.Host);

        resource.setProperty(PropertyHelper.getPropertyId("Hosts", "cluster_name"), "cluster");
        resource.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "host:" + (4 - cnt));

        resource.setProperty(PropertyHelper.getPropertyId("Hosts", "cluster_name"), "cluster");
        resource.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "host:" + cnt);

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


  private static class TestStackResourceProvider extends TestResourceProvider {
    private TestStackResourceProvider() {
      super(Resource.Type.Stack);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

      Set<String> keyPropertyValues = new HashSet<String>();

      return getResources(Resource.Type.Stack, predicate, "Stacks/stack_name", keyPropertyValues);
    }
  }


  private static class TestStackVersionResourceProvider extends TestResourceProvider {
    private TestStackVersionResourceProvider() {
      super(Resource.Type.StackVersion);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      Set<String> keyPropertyValues = new LinkedHashSet<String>();

      keyPropertyValues.add("1.2.1");
      keyPropertyValues.add("1.2.2");
      keyPropertyValues.add("2.0.1");

      return getResources(Resource.Type.StackVersion, predicate, "Versions/stack_version", keyPropertyValues);
    }
  }

  private static class TestOperatingSystemResourceProvider extends TestResourceProvider {
    private TestOperatingSystemResourceProvider() {
      super(Resource.Type.OperatingSystem);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      Set<String> keyPropertyValues = new LinkedHashSet<String>();

      keyPropertyValues.add("centos5");
      keyPropertyValues.add("centos6");
      keyPropertyValues.add("oraclelinux5");

      return getResources(Resource.Type.OperatingSystem, predicate, "OperatingSystems/os_type", keyPropertyValues);
    }
  }

  private static class TestRepositoryResourceProvider extends TestResourceProvider {
    private TestRepositoryResourceProvider() {
      super(Resource.Type.Repository);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      Set<String> keyPropertyValues = new LinkedHashSet<String>();

      keyPropertyValues.add("repo1");
      keyPropertyValues.add("repo2");

      return getResources(Resource.Type.Repository, predicate, "Repositories/repo_id", keyPropertyValues);
    }
  }

}


