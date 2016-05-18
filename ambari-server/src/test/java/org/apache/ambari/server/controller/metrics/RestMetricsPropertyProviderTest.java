/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.metrics;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.StackDefinedPropertyProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.jmx.TestStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.Metric;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;


/**
 * Rest metrics property provider tests.
 */
public class RestMetricsPropertyProviderTest {

  public static final String WRAPPED_METRICS_KEY = "WRAPPED_METRICS_KEY";
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "state");
  protected static final Map<String, String> metricsProperties = new HashMap<String, String>();
  protected static final Map<String, Metric> componentMetrics = new HashMap<String, Metric>();
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  public static final int NUMBER_OF_RESOURCES = 400;
  private static Injector injector;
  private static Clusters clusters;
  private static Cluster c1;

  {
    metricsProperties.put("default_port", "8745");
    metricsProperties.put("port_config_type", "storm-site");
    metricsProperties.put("port_property_name", "storm.port");
    metricsProperties.put("protocol", "http");
    componentMetrics.put("metrics/api/cluster/summary/tasks.total", new Metric("/api/cluster/summary##tasks.total", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/slots.total", new Metric("/api/cluster/summary##slots.total", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/slots.free", new Metric("/api/cluster/summary##slots.free", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/supervisors", new Metric("/api/cluster/summary##supervisors", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/executors.total", new Metric("/api/cluster/summary##executors.total", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/slots.used", new Metric("/api/cluster/summary##slots.used", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/topologies", new Metric("/api/cluster/summary##topologies", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/nimbus.uptime", new Metric("/api/cluster/summary##nimbus.uptime", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/wrong.metric", new Metric(null, false, false, false, "unitless"));
  }


  @BeforeClass
  public static void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1", new StackId("HDP-2.1.1"));
    c1 = clusters.getCluster("c1");
    JMXPropertyProvider.init(injector.getInstance(Configuration.class));

    // Setting up Mocks for Controller, Clusters etc, queried as part of user's Role context
    // while fetching Metrics.
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);
    Clusters clustersMock = createNiceMock(Clusters.class);
    Cluster clusterMock = createNiceMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clustersMock).anyTimes();
    expect(clustersMock.getCluster(CLUSTER_NAME_PROPERTY_ID)).andReturn(clusterMock).anyTimes();
    expect(clusterMock.getResourceId()).andReturn(2L).anyTimes();
    try {
      expect(clustersMock.getCluster(anyObject(String.class))).andReturn(clusterMock).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    replay(amc, clustersMock, clusterMock);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testRestMetricsPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testRestMetricsPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testRestMetricsPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test(expected = AuthorizationException.class)
  public void testRestMetricsPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  public void testPopulateResources() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);

    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<String, Map<String, PropertyInfo>>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");


    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());
    Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "wrong.metric")));

    //STORM_REST_API
    Assert.assertEquals(28.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "tasks.total")));
    Assert.assertEquals(8.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.total")));
    Assert.assertEquals(5.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.free")));
    Assert.assertEquals(2.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "supervisors")));
    Assert.assertEquals(28.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "executors.total")));
    Assert.assertEquals(3.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.used")));
    Assert.assertEquals(1.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "topologies")));
    Assert.assertEquals(4637.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "nimbus.uptime")));
  }

  public void testPopulateResources_singleProperty() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<String, Map<String, PropertyInfo>>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");


    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(28.0, resource.getPropertyValue("metrics/api/cluster/summary/tasks.total"));
    Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/taskstotal"));
  }

  public void testPopulateResources_category() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<String, Map<String, PropertyInfo>>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");


    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/api/cluster"), temporalInfoMap);

    Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(28.0, resource.getPropertyValue("metrics/api/cluster/summary/tasks.total"));
    Assert.assertEquals(2.0, resource.getPropertyValue("metrics/api/cluster/summary/supervisors"));
    Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/taskstotal"));
  }

  public void testPopulateResourcesUnhealthyResource() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<String, Map<String, PropertyInfo>>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");


    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "INSTALLED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // Assert that the stream provider was never called.
    Assert.assertNull(streamProvider.getLastSpec());
  }

  public void testPopulateResourcesMany() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<String, Map<String, PropertyInfo>>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    Set<Resource> resources = new HashSet<Resource>();

    RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");

    for (int i = 0; i < NUMBER_OF_RESOURCES; ++i) {
      // strom_rest_api
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);

      resource.setProperty("HostRoles/cluster_name", "c1");
      resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
      resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
      resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
      resource.setProperty("unique_id", i);

      resources.add(resource);
    }

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Set<Resource> resourceSet = restMetricsPropertyProvider.populateResources(resources, request, null);

    Assert.assertEquals(NUMBER_OF_RESOURCES, resourceSet.size());

    for (Resource resource : resourceSet) {
      Assert.assertEquals(28.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "tasks.total")));
      Assert.assertEquals(8.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.total")));
      Assert.assertEquals(5.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.free")));
      Assert.assertEquals(2.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "supervisors")));
    }
  }

  public void testPopulateResourcesTimeout() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<String, Map<String, PropertyInfo>>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider(100L);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    Set<Resource> resources = new HashSet<Resource>();

    RestMetricsPropertyProvider restMetricsPropertyProvider = new RestMetricsPropertyProvider(
      injector,
      metricDefinition.getProperties(),
      componentMetrics,
      streamProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      "STORM_REST_API");

    // set the provider timeout to 50 millis
    restMetricsPropertyProvider.setPopulateTimeout(50L);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");

    resources.add(resource);

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Set<Resource> resourceSet = restMetricsPropertyProvider.populateResources(resources, request, null);

    // make sure that the thread running the stream provider has completed
    Thread.sleep(150L);

    Assert.assertEquals(0, resourceSet.size());

    // assert that properties never get set on the resource
    Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/tasks.total"));
    Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/supervisors"));
  }

  public static class TestMetricsHostProvider implements MetricHostProvider {

    @Override
    public String getCollectorHostName(String clusterName, MetricsService service) throws SystemException {
      return null;
    }

    @Override
    public String getHostName(String clusterName, String componentName) {
      return null;
    }

    @Override
    public String getCollectorPort(String clusterName, MetricsService service) throws SystemException {
      return null;
    }

    @Override
    public boolean isCollectorHostLive(String clusterName, MetricsService service) throws SystemException {
      return false;
    }

    @Override
    public boolean isCollectorComponentLive(String clusterName, MetricsService service) throws SystemException {
      return false;
    }
  }

}
