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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ganglia.GangliaPropertyProviderTest.TestGangliaHostProvider;
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
  private Injector injector = null;

  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();

    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    StackDefinedPropertyProvider.init(injector);
    
    Clusters clusters = injector.getInstance(Clusters.class);
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
   * Test for empty constructor.
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
   * Test map constructors.
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
   * Test singleton accessor.
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
  
  
}
