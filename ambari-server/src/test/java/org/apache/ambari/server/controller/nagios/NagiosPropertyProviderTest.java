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
package org.apache.ambari.server.controller.nagios;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ganglia.TestStreamProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests the nagios property provider
 */
public class NagiosPropertyProviderTest {

  private static final String HOST = "c6401.ambari.apache.org";

  private InMemoryDefaultTestModule module = null;
  private Clusters clusters = null;
  private Injector injector = null;

  @Before
  public void setup() throws Exception {
    module = new InMemoryDefaultTestModule();

    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    NagiosPropertyProvider.init(injector);
    
    clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1");
    
    
    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    
    clusters.addHost(HOST);
    Host host = clusters.getHost(HOST);
    host.setOsType("centos5");
    host.persist();
    
    clusters.mapHostToCluster(HOST, "c1");
  }
  
  @After
  public void teardown() throws Exception {
    injector.getInstance(PersistService.class).stop();    
  }
  
  @Test
  public void testNoNagiosService() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Service,
        streamProvider,
        "ServiceInfo/cluster_name",
        "ServiceInfo/service_name");
    
    Resource resource = new ResourceImpl(Resource.Type.Service);
    resource.setProperty("ServiceInfo/cluster_name", "c1");
    resource.setProperty("ServiceInfo/service_name", "HBASE");
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = npp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());
    
    Resource res = set.iterator().next();
    
    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    
    Assert.assertFalse("Expected no alerts", values.containsKey("alerts"));
  }
  
  @Test
  public void testNoNagiosServerCompoonent() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.addService("NAGIOS");
    service.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    service.persist();
    
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Service,
        streamProvider,
        "ServiceInfo/cluster_name",
        "ServiceInfo/service_name");
    
    Resource resource = new ResourceImpl(Resource.Type.Service);
    resource.setProperty("ServiceInfo/cluster_name", "c1");
    resource.setProperty("ServiceInfo/service_name", "HBASE");
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = npp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());
    
    Resource res = set.iterator().next();
    
    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    
    Assert.assertFalse("Expected no alerts", values.containsKey("alerts"));
  }
  
  @Test
  public void testNagiosServiceAlerts() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.addService("NAGIOS");
    service.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    service.persist();
    
    ServiceComponent sc = service.addServiceComponent("NAGIOS_SERVER");
    sc.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    sc.addServiceComponentHost(HOST);
    sc.persist();
    
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Service,
        streamProvider,
        "ServiceInfo/cluster_name",
        "ServiceInfo/service_name");
    npp.forceReset();
    
    Resource resource = new ResourceImpl(Resource.Type.Service);
    resource.setProperty("ServiceInfo/cluster_name", "c1");
    resource.setProperty("ServiceInfo/service_name", "HBASE");
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = npp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());
    
    Resource res = set.iterator().next();
    
    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    
    Assert.assertTrue(values.containsKey("alerts"));
    Assert.assertTrue(values.containsKey("alerts/summary"));
    Assert.assertTrue(values.get("alerts").containsKey("detail"));
    Assert.assertTrue(List.class.isInstance(values.get("alerts").get("detail")));
    
    List<?> list = (List<?>) values.get("alerts").get("detail");
    Assert.assertEquals(Integer.valueOf(3), Integer.valueOf(list.size()));
    for (Object o : list) {
      Assert.assertTrue(Map.class.isInstance(o));
      Map<?, ?> map = (Map<?, ?>) o;
      Assert.assertTrue(map.containsKey("service_name"));
      String serviceName = map.get("service_name").toString();
      Assert.assertTrue("expected HBASE", serviceName.equals("HBASE"));
    }
    
    Map<String, Object> summary = values.get("alerts/summary");
    Assert.assertTrue(summary.containsKey("OK"));
    Assert.assertTrue(summary.containsKey("WARNING"));
    Assert.assertTrue(summary.containsKey("CRITICAL"));
    
    Assert.assertTrue(summary.get("OK").equals(Integer.valueOf(1)));
    Assert.assertTrue(summary.get("WARNING").equals(Integer.valueOf(0)));
    Assert.assertTrue(summary.get("CRITICAL").equals(Integer.valueOf(2)));
  }  
  

  @Test
  public void testNagiosHostAlerts() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.addService("NAGIOS");
    service.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    service.persist();
    
    ServiceComponent sc = service.addServiceComponent("NAGIOS_SERVER");
    sc.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    sc.addServiceComponentHost(HOST);
    sc.persist();
    
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Host,
        streamProvider,
        "Hosts/cluster_name",
        "Hosts/host_name");
    npp.forceReset();
    
    Resource resource = new ResourceImpl(Resource.Type.Service);
    resource.setProperty("Hosts/cluster_name", "c1");
    resource.setProperty("Hosts/host_name", "c6403.ambari.apache.org");
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = npp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());
    
    Resource res = set.iterator().next();
    
    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    
    Assert.assertTrue(values.containsKey("alerts"));
    Assert.assertTrue(values.containsKey("alerts/summary"));
    Assert.assertTrue(values.get("alerts").containsKey("detail"));
    Assert.assertTrue(List.class.isInstance(values.get("alerts").get("detail")));
    
    List<?> list = (List<?>) values.get("alerts").get("detail");
    Assert.assertTrue(7 == list.size());
    for (Object o : list) {
      Assert.assertTrue(Map.class.isInstance(o));
      Map<?, ?> map = (Map<?, ?>) o;
      Assert.assertTrue(map.containsKey("host_name"));
      String host = map.get("host_name").toString();
      Assert.assertTrue("expected c6403.ambari.apache.org", host.equals("c6403.ambari.apache.org"));
    }
    
    Map<String, Object> summary = values.get("alerts/summary");
    Assert.assertTrue(summary.containsKey("OK"));
    Assert.assertTrue(summary.containsKey("WARNING"));
    Assert.assertTrue(summary.containsKey("CRITICAL"));
    
    Assert.assertTrue(summary.get("OK").equals(Integer.valueOf(6)));
    Assert.assertTrue(summary.get("WARNING").equals(Integer.valueOf(0)));
    Assert.assertTrue(summary.get("CRITICAL").equals(Integer.valueOf(1)));
  }
  
  @Test
  public void testNagiosHostAlertsWithIgnore() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.addService("NAGIOS");
    service.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    service.persist();
    
    ServiceComponent sc = service.addServiceComponent("NAGIOS_SERVER");
    sc.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    sc.addServiceComponentHost(HOST);
    sc.persist();
    
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Host,
        streamProvider,
        "Hosts/cluster_name",
        "Hosts/host_name");
    npp.forceReset();
    
    Resource resource = new ResourceImpl(Resource.Type.Service);
    resource.setProperty("Hosts/cluster_name", "c1");
    resource.setProperty("Hosts/host_name", "c6401.ambari.apache.org");
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = npp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());
    
    Resource res = set.iterator().next();
    
    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    
    Assert.assertTrue(values.containsKey("alerts"));
    Assert.assertTrue(values.containsKey("alerts/summary"));
    Assert.assertTrue(values.get("alerts").containsKey("detail"));
    Assert.assertTrue(List.class.isInstance(values.get("alerts").get("detail")));
    
    List<?> list = (List<?>) values.get("alerts").get("detail");
    Assert.assertEquals(Integer.valueOf(16), Integer.valueOf(list.size()));
    for (Object o : list) {
      Assert.assertTrue(Map.class.isInstance(o));
      Map<?, ?> map = (Map<?, ?>) o;
      Assert.assertTrue(map.containsKey("host_name"));
      String host = map.get("host_name").toString();
      Assert.assertEquals("c6401.ambari.apache.org", host);
    }
    
    Map<String, Object> summary = values.get("alerts/summary");
    Assert.assertTrue(summary.containsKey("OK"));
    Assert.assertTrue(summary.containsKey("WARNING"));
    Assert.assertTrue(summary.containsKey("CRITICAL"));
    
    Assert.assertEquals(summary.get("OK"), Integer.valueOf(15));
    Assert.assertEquals(summary.get("WARNING"), Integer.valueOf(0));
    Assert.assertEquals(summary.get("CRITICAL"), Integer.valueOf(1));
  }  
  
  @Test
  public void testNagiosServiceAlertsAddIgnore() throws Exception {
    module.getProperties().setProperty(Configuration.NAGIOS_IGNORE_FOR_SERVICES_KEY,
        "HBase Master process on c6401.ambari.apache.org");
    
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.addService("NAGIOS");
    service.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    service.persist();
    
    ServiceComponent sc = service.addServiceComponent("NAGIOS_SERVER");
    sc.setDesiredStackVersion(new StackId("HDP-2.0.5"));
    sc.addServiceComponentHost(HOST);
    sc.persist();
    
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Service,
        streamProvider,
        "ServiceInfo/cluster_name",
        "ServiceInfo/service_name");
    npp.forceReset();
    NagiosPropertyProvider.init(injector);
    
    Resource resource = new ResourceImpl(Resource.Type.Service);
    resource.setProperty("ServiceInfo/cluster_name", "c1");
    resource.setProperty("ServiceInfo/service_name", "HBASE");
    
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = npp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());
    
    Resource res = set.iterator().next();
    
    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    
    Assert.assertTrue(values.containsKey("alerts"));
    Assert.assertTrue(values.containsKey("alerts/summary"));
    Assert.assertTrue(values.get("alerts").containsKey("detail"));
    Assert.assertTrue(List.class.isInstance(values.get("alerts").get("detail")));
    
    List<?> list = (List<?>) values.get("alerts").get("detail");
    // removed an additional one
    Assert.assertEquals(Integer.valueOf(2), Integer.valueOf(list.size()));
    for (Object o : list) {
      Assert.assertTrue(Map.class.isInstance(o));
      Map<?, ?> map = (Map<?, ?>) o;
      Assert.assertTrue(map.containsKey("service_name"));
      String serviceName = map.get("service_name").toString();
      Assert.assertTrue("expected HBASE", serviceName.equals("HBASE"));
    }
    
    Map<String, Object> summary = values.get("alerts/summary");
    Assert.assertTrue(summary.containsKey("OK"));
    Assert.assertTrue(summary.containsKey("WARNING"));
    Assert.assertTrue(summary.containsKey("CRITICAL"));
    
    Assert.assertTrue(summary.get("OK").equals(Integer.valueOf(1)));
    Assert.assertTrue(summary.get("WARNING").equals(Integer.valueOf(0)));
    Assert.assertTrue(summary.get("CRITICAL").equals(Integer.valueOf(1)));
  }    
  
}
