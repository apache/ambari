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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ganglia.GangliaHostComponentPropertyProvider;
import org.apache.ambari.server.controller.ganglia.GangliaPropertyProvider;
import org.apache.ambari.server.controller.ganglia.GangliaPropertyProviderTest.TestGangliaHostProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.jmx.TestStreamProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProviderTest.TestJMXHostProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.Metric;
import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests the stack defined property provider.
 */
public class StackDefinedPropertyProviderTest {
  private static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = "HostRoles/host_name";
  private static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = "HostRoles/component_name";
  private static final String HOST_COMPONENT_STATE_PROPERTY_ID = "HostRoles/state";    

  
  private Clusters clusters = null;
  private Injector injector = null;

  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();

    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    StackDefinedPropertyProvider.init(injector);
    
    clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1");
    
    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    
    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    host.setOsType("centos5");
    host.persist();
    
    clusters.mapHostToCluster("h1", "c1");
  }
  
  @After
  public void teardown() throws Exception {
    injector.getInstance(PersistService.class).stop();    
  }
  
  @Test
  public void testPopulateHostComponentResources() throws Exception {
    TestJMXHostProvider tj = new TestJMXHostProvider(true);
    TestGangliaHostProvider tg = new TestGangliaHostProvider();
    
    StackDefinedPropertyProvider sdpp = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent, tj, tg, new CombinedStreamProvider(),
        "HostRoles/cluster_name", "HostRoles/host_name", "HostRoles/component_name", "HostRoles/state", null, null);
    
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/host_name", "h1");
    resource.setProperty("HostRoles/component_name", "NAMENODE");
    resource.setProperty("HostRoles/state", "STARTED");
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = sdpp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());
    
    Resource res = set.iterator().next();
    
    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    
    Assert.assertTrue("Expected JMX metric 'metrics/dfs/FSNamesystem'", values.containsKey("metrics/dfs/FSNamesystem"));
    Assert.assertTrue("Expected JMX metric 'metrics/dfs/namenode'", values.containsKey("metrics/dfs/namenode"));
    Assert.assertTrue("Expected Ganglia metric 'metrics/rpcdetailed'", values.containsKey("metrics/rpcdetailed"));
  }
  
  
  @Test
  public void testCustomProviders() throws Exception {
    
    StackDefinedPropertyProvider sdpp = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent, null, null, new CombinedStreamProvider(),
        "HostRoles/cluster_name", "HostRoles/host_name", "HostRoles/component_name", "HostRoles/state",
        new EmptyPropertyProvider(), new EmptyPropertyProvider());
    
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty("HostRoles/host_name", "h1");
    resource.setProperty("HostRoles/component_name", "DATANODE");
    resource.setProperty("HostRoles/state", "STARTED");
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = sdpp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());
    
    Resource res = set.iterator().next();
    
    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    Assert.assertTrue(values.containsKey("foo/type1"));
    Assert.assertTrue(values.containsKey("foo/type2"));
    Assert.assertTrue(values.containsKey("foo/type3"));
    Assert.assertFalse(values.containsKey("foo/type4"));
    
    Assert.assertTrue(values.get("foo/type1").containsKey("name"));
    Assert.assertTrue(values.get("foo/type2").containsKey("name"));
    Assert.assertTrue(values.get("foo/type3").containsKey("name"));

    Assert.assertEquals("value1", values.get("foo/type1").get("name"));
    Assert.assertEquals("value2", values.get("foo/type2").get("name"));
    Assert.assertEquals("value3", values.get("foo/type3").get("name"));
    
  }
  

  
  private static class CombinedStreamProvider implements StreamProvider {

    @Override
    public InputStream readFrom(String spec) throws IOException {
      if (spec.indexOf ("jmx") > -1) {
        // jmx
        return ClassLoader.getSystemResourceAsStream("hdfs_namenode_jmx.json");
      } else {
        // ganglia
        return ClassLoader.getSystemResourceAsStream("temporal_ganglia_data.txt");
      }
    }

    @Override
    public InputStream readFrom(String spec, String requestMethod, String params)
        throws IOException {
      return readFrom(spec);
    }
  }
  
  private static class EmptyPropertyProvider implements PropertyProvider {

    @Override
    public Set<Resource> populateResources(Set<Resource> resources,
        Request request, Predicate predicate) throws SystemException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      // TODO Auto-generated method stub
      return null;
    }

  }
  
  /**
   * Test for empty constructor.  Public since instantiated via reflection.
   */
  public static class CustomMetricProvider1 implements PropertyProvider {
    @Override
    public Set<Resource> populateResources(Set<Resource> resources,
        Request request, Predicate predicate) throws SystemException {
      
      for (Resource r : resources) {
        r.setProperty("foo/type1/name", "value1");
      }
      
      return resources;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      return Collections.emptySet();
    }
  }

  /**
   * Test map constructors.  Public since instantiated via reflection.
   */
  public static class CustomMetricProvider2 implements PropertyProvider {
    private Map<String, String> providerProperties = null;
    
    public CustomMetricProvider2(Map<String, String> properties, Map<String, Metric> metrics) {
      providerProperties = properties;
    }

    @Override
    public Set<Resource> populateResources(Set<Resource> resources,
        Request request, Predicate predicate) throws SystemException {
      for (Resource r : resources) {
        r.setProperty("foo/type2/name", providerProperties.get("Type2.Metric.Name"));
      }
      return resources;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      return Collections.emptySet();
    }
  }

  /**
   * Test singleton accessor.  Public since instantiated via reflection.
   */
  public static class CustomMetricProvider3 implements PropertyProvider {
    private static CustomMetricProvider3 instance = null;
    private Map<String, String> providerProperties = new HashMap<String, String>();
    
    public static CustomMetricProvider3 getInstance(Map<String, String> properties, Map<String, Metric> metrics) {
      if (null == instance) {
        instance = new CustomMetricProvider3();
        instance.providerProperties.putAll(properties);
      }
      return instance;
    }
    
    @Override
    public Set<Resource> populateResources(Set<Resource> resources,
        Request request, Predicate predicate) throws SystemException {
      for (Resource r : resources) {
        r.setProperty("foo/type3/name", providerProperties.get("Type3.Metric.Name"));
      }
      return resources;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      return Collections.emptySet();
    }
  }
  
  @Test
  public void testPopulateResources_HDP2() throws Exception {
    
    StreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        hostProvider,
        gangliaHostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    // resourcemanager
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(6,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersAllocated")));
    Assert.assertEquals(6,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersReleased")));
    Assert.assertEquals(8192,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertEquals(1,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
    Assert.assertEquals(2,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AppsSubmitted")));
    
    Assert.assertEquals(1,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumActiveNMs")));
    Assert.assertEquals(0,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumDecommissionedNMs")));
    Assert.assertEquals(0,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumLostNMs")));
    Assert.assertEquals(0,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumUnhealthyNMs")));
    Assert.assertEquals(0,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumRebootedNMs")));

    Assert.assertEquals(932118528,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));

    //namenode
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
  }  
  
  @Test
  public void testPopulateResources_HDP2_params() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        hostProvider,
        gangliaHostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(6,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersAllocated")));
    Assert.assertEquals(6,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
    Assert.assertEquals(2,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AppsSubmitted")));
    
    Assert.assertEquals(15,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersAllocated")));
    Assert.assertEquals(12,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableMB")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableVCores")));
    Assert.assertEquals(47,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AppsSubmitted")));
    
    Assert.assertEquals(4,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersAllocated")));
    Assert.assertEquals(4,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersReleased")));
    Assert.assertEquals(6048, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableMB")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableVCores")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AppsSubmitted")));
  }  


  @Test
  public void testPopulateResources_HDP2_params_singleProperty() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        hostProvider,
        gangliaHostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue/root/AvailableMB"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
  }
  
  @Test
  public void testPopulateResources_HDP2_params_category() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        hostProvider,
        gangliaHostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(6,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersAllocated")));
    Assert.assertEquals(6,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
    Assert.assertEquals(2,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AppsSubmitted")));

    Assert.assertEquals(15,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersAllocated")));
    Assert.assertEquals(12,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableMB")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableVCores")));
    Assert.assertEquals(47,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AppsSubmitted")));

    Assert.assertEquals(4,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersAllocated")));
    Assert.assertEquals(4,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersReleased")));
    Assert.assertEquals(6048, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableMB")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableVCores")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AppsSubmitted")));
  }

  @Test
  public void testPopulateResources_HDP2_params_category2() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        hostProvider,
        gangliaHostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue/root/default"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersAllocated")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersReleased")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AppsSubmitted")));

    Assert.assertEquals(15,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersAllocated")));
    Assert.assertEquals(12,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableMB")));
    Assert.assertEquals(1,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableVCores")));
    Assert.assertEquals(47,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AppsSubmitted")));

    Assert.assertEquals(99,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AggregateContainersAllocated")));
    Assert.assertEquals(98,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AggregateContainersReleased")));
    Assert.assertEquals(9898, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AvailableMB")));
    Assert.assertEquals(2,    resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AvailableVCores")));
    Assert.assertEquals(97,   resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AppsSubmitted")));

    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersAllocated")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersReleased")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableMB")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableVCores")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AppsSubmitted")));
  }  

  @Test
  public void testPopulateResources_jmx_JournalNode() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        hostProvider,
        gangliaHostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOURNALNODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/hdfs_journalnode_jmx.json for values
    Assert.assertEquals(1377795104272L, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "startTime")));
    Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(14569736, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(24993392, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(9100, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcCount")));
    Assert.assertEquals(31641, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcTimeMillis")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "logError")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "logFatal")));
    Assert.assertEquals(4163, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "logInfo")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "logWarn")));
    Assert.assertEquals(29.8125, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "memHeapCommittedM")));
    Assert.assertEquals(13.894783, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "memHeapUsedM")));
    Assert.assertEquals(24.9375, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "memNonHeapCommittedM")));
    Assert.assertEquals(23.835556, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "memNonHeapUsedM")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsBlocked")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsNew")));
    Assert.assertEquals(6, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsRunnable")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsTerminated")));
    Assert.assertEquals(3, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsTimedWaiting")));
    Assert.assertEquals(8, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsWaiting")));

    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "NumOpenConnections")));
    Assert.assertEquals(4928861, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(13.211112159230245, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "RpcProcessingTime_avg_time")));
    Assert.assertEquals(25067, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "RpcProcessingTime_num_ops")));
    Assert.assertEquals(0.19686821997924706, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "RpcQueueTime_avg_time")));
    Assert.assertEquals(25067, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "RpcQueueTime_num_ops")));
    Assert.assertEquals(6578899, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "SentBytes")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "callQueueLen")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "rpcAuthenticationFailures")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "rpcAuthenticationSuccesses")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "rpcAuthorizationFailures")));
    Assert.assertEquals(12459, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "rpcAuthorizationSuccesses")));

    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "getJournalState_num_ops")));
    Assert.assertEquals(0.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "getJournalState_avg_time")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "newEpoch_num_ops")));
    Assert.assertEquals(60.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "newEpoch_avg_time")));
    Assert.assertEquals(4129, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "startLogSegment_num_ops")));
    Assert.assertEquals(38.25951359084413, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "startLogSegment_avg_time")));
    Assert.assertEquals(8265, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "journal_num_ops")));
    Assert.assertEquals(2.1832618025751187, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "journal_avg_time")));
    Assert.assertEquals(4129, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "finalizeLogSegment_num_ops")));
    Assert.assertEquals(11.575679542203101, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "finalizeLogSegment_avg_time")));
    Assert.assertEquals(8536, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "getEditLogManifest_num_ops")));
    Assert.assertEquals(12.55427859318747, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "getEditLogManifest_avg_time")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "prepareRecovery_num_ops")));
    Assert.assertEquals(10.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "prepareRecovery_avg_time")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "acceptRecovery_num_ops")));
    Assert.assertEquals(30.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "acceptRecovery_avg_time")));

    Assert.assertEquals(0.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/ugi", "loginFailure_avg_time")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/ugi", "loginFailure_num_ops")));
    Assert.assertEquals(0.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/ugi", "loginSuccess_avg_time")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/ugi", "loginSuccess_num_ops")));

    Assert.assertEquals("{\"mycluster\":{\"Formatted\":\"true\"}}", resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode", "journalsStatus")));

    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s_num_ops")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s50thPercentileLatencyMicros")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s75thPercentileLatencyMicros")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s90thPercentileLatencyMicros")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s95thPercentileLatencyMicros")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s99thPercentileLatencyMicros")));
    Assert.assertEquals(4, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s_num_ops")));
    Assert.assertEquals(1027, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s50thPercentileLatencyMicros")));
    Assert.assertEquals(1037, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s75thPercentileLatencyMicros")));
    Assert.assertEquals(1037, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s90thPercentileLatencyMicros")));
    Assert.assertEquals(1037, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s95thPercentileLatencyMicros")));
    Assert.assertEquals(1037, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s99thPercentileLatencyMicros")));
    Assert.assertEquals(60, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s_num_ops")));
    Assert.assertEquals(1122, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s50thPercentileLatencyMicros")));
    Assert.assertEquals(1344, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s75thPercentileLatencyMicros")));
    Assert.assertEquals(1554, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s90thPercentileLatencyMicros")));
    Assert.assertEquals(1980, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s95thPercentileLatencyMicros")));
    Assert.assertEquals(8442, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s99thPercentileLatencyMicros")));
    Assert.assertEquals(8265, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "batchesWritten")));
    Assert.assertEquals(8265, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "txnsWritten")));
    Assert.assertEquals(107837, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "bytesWritten")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "batchesWrittenWhileLagging")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "lastPromisedEpoch")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "lastWriterEpoch")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "currentLagTxns")));
    Assert.assertEquals(8444, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "lastWrittenTxId")));
  }  

  @Test
  public void testPopulateResources_NoRegionServer() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        hostProvider,
        gangliaHostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        null,
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_REGIONSERVER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    int preSize = resource.getPropertiesMap().size();
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(preSize, resource.getPropertiesMap().size());
  }

  @Test
  public void testPopulateResources_HBaseMaster2() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        hostProvider,
        gangliaHostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    
    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_MASTER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Set<Resource> res = propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, res.size());
    
    Map<String, Map<String, Object>> map = res.iterator().next().getPropertiesMap();

    Assert.assertTrue(map.containsKey("metrics/hbase/master"));
    // uses 'tag.isActiveMaster' (name with a dot)
    Assert.assertTrue(map.get("metrics/hbase/master").containsKey("IsActiveMaster"));
  }    

  
  @Test
  public void testPopulateResources_params_category5() throws Exception {
    org.apache.ambari.server.controller.ganglia.TestStreamProvider streamProvider =
        new org.apache.ambari.server.controller.ganglia.TestStreamProvider("temporal_ganglia_data_yarn_queues.txt");

    TestJMXHostProvider jmxHostProvider = new TestJMXHostProvider(true);
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
    
    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        jmxHostProvider,
        hostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());
    
    
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "dev01.ambari.apache.org");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");

    String RM_CATEGORY_1 = "metrics/yarn/Queue/root/default";
    String RM_AVAILABLE_MEMORY_PROPERTY = PropertyHelper.getPropertyId(RM_CATEGORY_1, "AvailableMB");
    
    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(RM_CATEGORY_1, new TemporalInfoImpl(10L, 20L, 1L));
    
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(RM_CATEGORY_1), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
    
    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/");

    Assert.assertTrue(PropertyHelper.getProperties(resource).size() > 2);
    Assert.assertNotNull(resource.getPropertyValue(RM_AVAILABLE_MEMORY_PROPERTY));
  }  

  @Test
  public void testPopulateResources_ganglia_JournalNode() throws Exception {
    org.apache.ambari.server.controller.ganglia.TestStreamProvider streamProvider =
        new org.apache.ambari.server.controller.ganglia.TestStreamProvider("journalnode_ganglia_data.txt");

    TestJMXHostProvider jmxHostProvider = new TestJMXHostProvider(true);
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
    
    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        jmxHostProvider,
        hostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());


    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOURNALNODE");


    Object[][] testData = {
      {"metrics", "boottime", 1378290058.0},
      {"metrics/cpu", "cpu_aidle", 0.0},
      {"metrics/cpu", "cpu_idle", 88.2},
      {"metrics/cpu", "cpu_nice", 0.0},
      {"metrics/cpu", "cpu_num", 2.0},
      {"metrics/cpu", "cpu_speed", 3583.0},
      {"metrics/cpu", "cpu_system", 8.4},
      {"metrics/cpu", "cpu_user", 3.3},
      {"metrics/cpu", "cpu_wio", 0.1},
      {"metrics/disk", "disk_free", 92.428},
      {"metrics/disk", "disk_total", 101.515},
      {"metrics/disk", "part_max_used", 12.8},
      {"metrics/load", "load_fifteen", 0.026},
      {"metrics/load", "load_five", 0.114},
      {"metrics/load", "load_one", 0.226},
      {"metrics/memory", "mem_buffers", 129384.0},
      {"metrics/memory", "mem_cached", 589576.0},
      {"metrics/memory", "mem_free", 1365496.0},
      {"metrics/memory", "mem_shared", 0.0},
      {"metrics/memory", "mem_total", 4055144.0},
      {"metrics/memory", "swap_free", 4128760.0},
      {"metrics/memory", "swap_total", 4128760.0},
      {"metrics/network", "bytes_in", 22547.48},
      {"metrics/network", "bytes_out", 5772.33},
      {"metrics/network", "pkts_in", 24.0},
      {"metrics/network", "pkts_out", 35.4},
      {"metrics/process", "proc_run", 4.0},
      {"metrics/process", "proc_total", 657.0},
      {"metrics/dfs/journalNode", "batchesWritten", 0.0},
      {"metrics/dfs/journalNode", "batchesWrittenWhileLagging", 0.0},
      {"metrics/dfs/journalNode", "bytesWritten", 0.0},
      {"metrics/dfs/journalNode", "currentLagTxns", 0.0},
      {"metrics/dfs/journalNode", "lastPromisedEpoch", 5.0},
      {"metrics/dfs/journalNode", "lastWriterEpoch", 5.0},
      {"metrics/dfs/journalNode", "lastWrittenTxId", 613.0},
      {"metrics/dfs/journalNode", "syncs60s50thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s75thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s90thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s95thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s99thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s_num_ops", 0.0},
      {"metrics/dfs/journalNode", "syncs300s50thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s75thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s90thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s95thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s99thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s_num_ops", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s50thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s75thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s90thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s95thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s99thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s_num_ops", 0.0},
      {"metrics/dfs/journalNode", "txnsWritten", 0.0}
    };

    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Set<String> properties = new LinkedHashSet<String>();

    for (Object[] row : testData) {
      properties.add(PropertyHelper.getPropertyId(row[0].toString(), row[1].toString()));
    }

    Request request = PropertyHelper.getReadRequest(properties, temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Map<String, Object> p = PropertyHelper.getProperties(resource);

    for (String key : p.keySet()) {
      if (!properties.contains(key)) {
        System.out.printf(key);
      }
    }

    // size + properties defined before "Object[][] testData ... " above 
    Assert.assertEquals(properties.size() + 3, PropertyHelper.getProperties(resource).size());

    int i = 0;
    for (String property : properties) {
      Assert.assertEquals(testData[i++][2], resource.getPropertyValue(property));
    }
  }  

  @Test
  public void testPopulateResources_resourcemanager_clustermetrics() throws Exception {
    
    String[] metrics = new String[] {
      "metrics/yarn/ClusterMetrics/NumActiveNMs",
      "metrics/yarn/ClusterMetrics/NumDecommissionedNMs",
      "metrics/yarn/ClusterMetrics/NumLostNMs",
      "metrics/yarn/ClusterMetrics/NumUnhealthyNMs",
      "metrics/yarn/ClusterMetrics/NumRebootedNMs"
    };
    
    org.apache.ambari.server.controller.ganglia.TestStreamProvider streamProvider =
        new org.apache.ambari.server.controller.ganglia.TestStreamProvider("yarn_ganglia_data.txt");

    TestJMXHostProvider jmxHostProvider = new TestJMXHostProvider(true);
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
    
    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent,
        jmxHostProvider,
        hostProvider,
        streamProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        new EmptyPropertyProvider(),
        new EmptyPropertyProvider());

    for (String metric : metrics) {
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);

      resource.setProperty("HostRoles/cluster_name", "c1");
      resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
      resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
      
      // only ask for one property
      Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
      temporalInfoMap.put(metric, new TemporalInfoImpl(10L, 20L, 1L));
      Request  request = PropertyHelper.getReadRequest(Collections.singleton(metric), temporalInfoMap);

      Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
      
      Assert.assertEquals(4, PropertyHelper.getProperties(resource).size());
      Assert.assertNotNull(resource.getPropertyValue(metric));
      
    }
    
  }  
  
}
