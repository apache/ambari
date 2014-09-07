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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ganglia.TestStreamProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Tests the nagios property provider
 */
public class NagiosPropertyProviderTest {

  private static final String HOST = "c6401.ambari.apache.org";

  private GuiceModule module = null;
  private Clusters clusters = null;
  private Injector injector = null;

  @Before
  public void setup() throws Exception {
    module = new GuiceModule();
    injector = Guice.createInjector(module);
    NagiosPropertyProvider.init(injector);
    
    clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    
    expect(cluster.getAlerts()).andReturn(Collections.<Alert>emptySet()).anyTimes();
    
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();
    

    Service nagiosService = createMock(Service.class);
    expect(cluster.getService("NAGIOS")).andReturn(nagiosService).anyTimes();
    
    ServiceComponent nagiosServiceComponent = createMock(ServiceComponent.class);
    expect(nagiosService.getServiceComponent("NAGIOS_SERVER")).andReturn(
        nagiosServiceComponent).anyTimes();
    
    ServiceComponentHost nagiosScHost = createMock(ServiceComponentHost.class);
    Map<String, ServiceComponentHost> map1 = new HashMap<String, ServiceComponentHost>();
    map1.put(HOST, nagiosScHost);
    expect(nagiosServiceComponent.getServiceComponentHosts()).andReturn(
        map1).anyTimes();
    
    replay(clusters, cluster, nagiosService, nagiosServiceComponent);
  }
  
  @Test
  public void testNoNagiosService() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    reset(cluster); // simulate an error that NAGIOS not with the cluster
    expect(cluster.getService("NAGIOS")).andThrow(new AmbariException("No Service"));
    replay(cluster);
    
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
    
    Assert.assertFalse("Expected no alerts", values.containsKey("alerts"));
  }


  @Test
  public void testClusterDoesNotExistNPE() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Service,
        streamProvider,
        "ServiceInfo/cluster_name",
        "ServiceInfo/service_name");
    npp.forceReset();

    Resource resource = new ResourceImpl(Resource.Type.Service);
    resource.setProperty("ServiceInfo/cluster_name", null);
    resource.setProperty("ServiceInfo/service_name", "HBASE");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(),
            new HashMap<String, TemporalInfo>());

    Set<Resource> set = npp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());

  }

  @Test
  public void testNagiosClusterAlerts() throws Exception {

    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Cluster,
      streamProvider,
      "Clusters/cluster_name",
      "Clusters/version");
    npp.forceReset();

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty("Clusters/cluster_name", "c1");
    resource.setProperty("Clusters/version", "HDP-2.0.6");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), new HashMap<String, TemporalInfo>());

    Set<Resource> set = npp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());

    Resource res = set.iterator().next();

    Map<String, Map<String, Object>> values = res.getPropertiesMap();

    Assert.assertTrue(values.containsKey("alerts/summary"));

    Map<String, Object> summary = values.get("alerts/summary");
    Assert.assertEquals(4L, summary.size());
    Assert.assertTrue(summary.containsKey("OK"));
    Assert.assertTrue(summary.containsKey("WARNING"));
    Assert.assertTrue(summary.containsKey("CRITICAL"));
    Assert.assertTrue(summary.containsKey("PASSIVE"));
    Assert.assertFalse(summary.containsKey("detail"));

    //Totally 4 hosts, no hosts with no alerts
    Assert.assertTrue(summary.get("OK").equals(Integer.valueOf(0)));
    Assert.assertTrue(summary.get("WARNING").equals(Integer.valueOf(1)));
    Assert.assertTrue(summary.get("CRITICAL").equals(Integer.valueOf(2)));
    Assert.assertTrue(summary.get("PASSIVE").equals(Integer.valueOf(1)));

  }

  @Test
  public void testNoNagiosServerComponent() throws Exception {

    Cluster cluster = clusters.getCluster("c1");
    reset(cluster);

    Service nagiosService = createMock(Service.class);
    expect(cluster.getService("NAGIOS")).andReturn(nagiosService);

    ServiceComponent nagiosServiceComponent = createMock(ServiceComponent.class);
    expect(nagiosService.getServiceComponent("NAGIOS_SERVER")).andThrow(new AmbariException("No Component"));

    replay(cluster, nagiosService);

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
    
    Assert.assertFalse("Expected no alerts", values.containsKey("alerts"));
  }
  
  @Test
  public void testNagiosServiceAlerts() throws Exception {
    module.properties.remove(Configuration.NAGIOS_IGNORE_FOR_SERVICES_KEY); // make sure NAGIOS_IGNORE_FOR_SERVICES_KEY is not set, which could be set by testNagiosServiceAlertsAddIgnore

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
    Assert.assertTrue(summary.containsKey("PASSIVE"));
    
    Assert.assertTrue(summary.get("OK").equals(Integer.valueOf(1)));
    Assert.assertTrue(summary.get("WARNING").equals(Integer.valueOf(0)));
    Assert.assertTrue(summary.get("CRITICAL").equals(Integer.valueOf(2)));
  }  
  

  @Test
  public void testNagiosHostAlerts() throws Exception {    
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
    Assert.assertTrue(summary.containsKey("PASSIVE"));
    
    Assert.assertTrue(summary.get("OK").equals(Integer.valueOf(6)));
    Assert.assertTrue(summary.get("WARNING").equals(Integer.valueOf(0)));
    Assert.assertTrue(summary.get("CRITICAL").equals(Integer.valueOf(1)));
  }
  
  @Test
  public void testNagiosHostAlertsWithIgnore() throws Exception {
    
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
    Assert.assertTrue(summary.containsKey("PASSIVE"));
    
    Assert.assertEquals(summary.get("OK"), Integer.valueOf(14));
    Assert.assertEquals(summary.get("WARNING"), Integer.valueOf(0));
    Assert.assertEquals(summary.get("CRITICAL"), Integer.valueOf(1));
    Assert.assertEquals(Integer.valueOf(1), summary.get("PASSIVE"));
  }  
  
  @Test
  public void testNagiosServiceAlertsAddIgnore() throws Exception {
    module.properties.setProperty(Configuration.NAGIOS_IGNORE_FOR_SERVICES_KEY,
        "HBase Master process on c6401.ambari.apache.org");
    
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
    Assert.assertTrue(summary.containsKey("PASSIVE"));
    
    Assert.assertTrue(summary.get("OK").equals(Integer.valueOf(1)));
    Assert.assertTrue(summary.get("WARNING").equals(Integer.valueOf(0)));
    Assert.assertTrue(summary.get("CRITICAL").equals(Integer.valueOf(1)));
  }

  @Test
  public void testNagiosServiceAlertsWithPassive() throws Exception {
    Injector inj = Guice.createInjector(new GuiceModule());
    
    Clusters clusters = inj.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getAlerts()).andReturn(Collections.<Alert>emptySet()).anyTimes();
    expect(clusters.getCluster("c1")).andReturn(cluster);

    Service nagiosService = createMock(Service.class);
    expect(cluster.getService("NAGIOS")).andReturn(nagiosService);
    
    ServiceComponent nagiosServiceComponent = createMock(ServiceComponent.class);
    expect(nagiosService.getServiceComponent("NAGIOS_SERVER")).andReturn(nagiosServiceComponent);
    
    ServiceComponentHost nagiosScHost = createMock(ServiceComponentHost.class);
    Map<String, ServiceComponentHost> map1 = new HashMap<String, ServiceComponentHost>();
    map1.put(HOST, nagiosScHost);
    expect(nagiosServiceComponent.getServiceComponentHosts()).andReturn(map1);
    
    replay(clusters, cluster, nagiosService, nagiosServiceComponent);

    
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Service,
        streamProvider,
        "ServiceInfo/cluster_name",
        "ServiceInfo/service_name");
    npp.forceReset();
    NagiosPropertyProvider.init(inj);
    
    Resource resource = new ResourceImpl(Resource.Type.Service);
    resource.setProperty("ServiceInfo/cluster_name", "c1");
    resource.setProperty("ServiceInfo/service_name", "GANGLIA");
    
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
    Assert.assertEquals(Integer.valueOf(4), Integer.valueOf(list.size()));
    for (Object o : list) {
      Assert.assertTrue(Map.class.isInstance(o));
      Map<?, ?> map = (Map<?, ?>) o;
      Assert.assertTrue(map.containsKey("service_name"));
      String serviceName = map.get("service_name").toString();
      Assert.assertEquals(serviceName, "GANGLIA");
    }
    
    Map<String, Object> summary = values.get("alerts/summary");
    Assert.assertTrue(summary.containsKey("OK"));
    Assert.assertTrue(summary.containsKey("WARNING"));
    Assert.assertTrue(summary.containsKey("CRITICAL"));
    Assert.assertTrue(summary.containsKey("PASSIVE"));
    
    Assert.assertEquals(Integer.valueOf(3), summary.get("OK"));
    Assert.assertEquals(Integer.valueOf(0), summary.get("WARNING"));
    Assert.assertEquals(Integer.valueOf(0), summary.get("CRITICAL"));
    Assert.assertEquals(Integer.valueOf(1), summary.get("PASSIVE"));
  }
  
  @Test
  public void testNagiosHostAlertsSubstringPassiveMarker() throws Exception {
    
    TestStreamProvider streamProvider = new TestStreamProvider("nagios_alerts.txt");

    NagiosPropertyProvider npp = new NagiosPropertyProvider(Resource.Type.Host,
        streamProvider,
        "Hosts/cluster_name",
        "Hosts/host_name");
    npp.forceReset();
    
    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty("Hosts/cluster_name", "c1");
    resource.setProperty("Hosts/host_name", "c6404.ambari.apache.org");
    
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
    Assert.assertEquals(Integer.valueOf(1), Integer.valueOf(list.size()));
    for (Object o : list) {
      Assert.assertTrue(Map.class.isInstance(o));
      Map<?, ?> map = (Map<?, ?>) o;
      Assert.assertTrue(map.containsKey("host_name"));
      String host = map.get("host_name").toString();
      Assert.assertEquals("c6404.ambari.apache.org", host);
    }
    
    Map<String, Object> summary = values.get("alerts/summary");
    Assert.assertTrue(summary.containsKey("OK"));
    Assert.assertTrue(summary.containsKey("WARNING"));
    Assert.assertTrue(summary.containsKey("CRITICAL"));
    Assert.assertTrue(summary.containsKey("PASSIVE"));
    
    Assert.assertEquals(Integer.valueOf(0), summary.get("OK"));
    Assert.assertEquals(Integer.valueOf(0), summary.get("WARNING"));
    Assert.assertEquals(Integer.valueOf(0), summary.get("CRITICAL"));
    Assert.assertEquals(Integer.valueOf(1), summary.get("PASSIVE"));
  }   
  
  private static class GuiceModule implements Module {

    private Properties properties = new Properties();
    
    @Override
    public void configure(Binder binder) {
     binder.bind(Clusters.class).toInstance(createMock(Clusters.class));
     binder.bind(Configuration.class).toInstance(new Configuration(properties));
    }
  }

}
