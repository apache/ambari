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
package org.apache.ambari.server.controller.ganglia;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.ComponentSSLConfigurationTest;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper.MetricsVersion;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.anyObject;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test the Ganglia property provider.
 */
@RunWith(Parameterized.class)
@PrepareForTest({ GangliaHostProvider.class })
public class GangliaPropertyProviderTest {

  private static final String PROPERTY_ID = PropertyHelper.getPropertyId("metrics/jvm", "gcCount");
  private static final String PROPERTY_ID2 = PropertyHelper.getPropertyId("metrics/cpu", "cpu_wio");
  private static final String FLUME_CHANNEL_CAPACITY_PROPERTY = "metrics/flume/flume/CHANNEL/c1/ChannelCapacity";
  private static final String FLUME_CATEGORY = "metrics/flume";
  private static final String FLUME_CATEGORY2 = "metrics/flume/flume";
  private static final String FLUME_CATEGORY3 = "metrics/flume/flume/CHANNEL";
  private static final String FLUME_CATEGORY4 = "metrics/flume/flume/CHANNEL/c1";
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  
  private static final String RM_CATEGORY_1 = "metrics/yarn/Queue/root/default";
  private static final String RM_AVAILABLE_MEMORY_PROPERTY = PropertyHelper.getPropertyId(RM_CATEGORY_1, "AvailableMB");
  
  private ComponentSSLConfiguration configuration;

  @Parameterized.Parameters
  public static Collection<Object[]> configs() {
    ComponentSSLConfiguration configuration1 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", false, false);

    ComponentSSLConfiguration configuration2 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", true, false);

    ComponentSSLConfiguration configuration3 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", false, true);

    return Arrays.asList(new Object[][]{
        {configuration1},
        {configuration2},
        {configuration3}
    });
  }

  public GangliaPropertyProviderTest(ComponentSSLConfiguration configuration) {
    this.configuration = configuration;
  }

  @Test
  public void testPopulateResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());


    String expected = (configuration.isGangliaSSL() ? "https" : "http") +
        "://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=domU-12-31-39-0E-34-E1.compute-1.internal&m=jvm.metrics.gcCount&s=10&e=20&r=1";
    Assert.assertEquals(expected, streamProvider.getLastSpec());

    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID));


    // tasktracker
    resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "TASKTRACKER");

    // only ask for one property
    temporalInfoMap = new HashMap<String, TemporalInfo>();

    Set<String> properties = new HashSet<String>();
    String shuffle_exceptions_caught = PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_exceptions_caught");
    String shuffle_failed_outputs    = PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_failed_outputs");
    String shuffle_output_bytes      = PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_output_bytes");
    String shuffle_success_outputs   = PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_success_outputs");

    properties.add(shuffle_exceptions_caught);
    properties.add(shuffle_failed_outputs);
    properties.add(shuffle_output_bytes);
    properties.add(shuffle_success_outputs);
    request = PropertyHelper.getReadRequest(properties, temporalInfoMap);

    temporalInfoMap.put(shuffle_exceptions_caught, new TemporalInfoImpl(10L, 20L, 1L));
    temporalInfoMap.put(shuffle_failed_outputs, new TemporalInfoImpl(10L, 20L, 1L));
    temporalInfoMap.put(shuffle_output_bytes, new TemporalInfoImpl(10L, 20L, 1L));
    temporalInfoMap.put(shuffle_success_outputs, new TemporalInfoImpl(10L, 20L, 1L));

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    
    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add("metrics/mapred/shuffleOutput/shuffle_exceptions_caught");
    metricsRegexes.add("metrics/mapred/shuffleOutput/shuffle_failed_outputs");
    metricsRegexes.add("metrics/mapred/shuffleOutput/shuffle_output_bytes");
    metricsRegexes.add("metrics/mapred/shuffleOutput/shuffle_success_outputs");
    
    
    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "TASKTRACKER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPSlaves");
    expectedUri.setParameter("h", "domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    

    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));


    Assert.assertEquals(6, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(shuffle_exceptions_caught));
    Assert.assertNotNull(resource.getPropertyValue(shuffle_failed_outputs));
    Assert.assertNotNull(resource.getPropertyValue(shuffle_output_bytes));
    Assert.assertNotNull(resource.getPropertyValue(shuffle_success_outputs));
  }
  
  @Test
  public void testPopulateResources_checkHostComponent() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    GangliaHostProvider hostProvider =  PowerMock.createPartialMock(GangliaHostProvider.class,
        "isGangliaCollectorHostLive", "isGangliaCollectorComponentLive");

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // datanode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);
    
    expect(hostProvider.getGangliaCollectorHostName(anyObject(String.class))).andReturn("ganglia-host");
    expect(hostProvider.isGangliaCollectorComponentLive(anyObject(String.class))).andReturn(true).once();
    expect(hostProvider.isGangliaCollectorHostLive(anyObject(String.class))).andReturn(true).once();
    
    
    PowerMock.replay(hostProvider);
    
    Set<Resource> populateResources = propertyProvider.populateResources(Collections.singleton(resource), request, null);
    
    PowerMock.verify(hostProvider);
    
    Assert.assertEquals(1, populateResources.size());
    
  }


  @Test
  public void testPopulateManyResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.Host, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID
    );

    Set<Resource> resources = new HashSet<Resource>();

    // host
    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resources.add(resource);

    resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E2.compute-1.internal");
    resources.add(resource);

    resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E3.compute-1.internal");
    resources.add(resource);

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);

    Assert.assertEquals(3, propertyProvider.populateResources(resources, request, null).size());
    
    URIBuilder uriBuilder = new URIBuilder();

    uriBuilder.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    uriBuilder.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    uriBuilder.setPath("/cgi-bin/rrd.py");
    uriBuilder.setParameter("c", "HDPJobTracker,HDPHBaseMaster,HDPResourceManager,HDPSlaves,HDPHistoryServer,HDPNameNode");
    uriBuilder.setParameter("h", "domU-12-31-39-0E-34-E3.compute-1.internal,domU-12-31-39-0E-34-E1.compute-1.internal,domU-12-31-39-0E-34-E2.compute-1.internal");
    uriBuilder.setParameter("m", "jvm.metrics.gcCount");
    uriBuilder.setParameter("s", "10");
    uriBuilder.setParameter("e", "20");
    uriBuilder.setParameter("r", "1");

    String expected = uriBuilder.toString();
    
    
    Assert.assertEquals(expected, streamProvider.getLastSpec());

    for (Resource res : resources) {
      Assert.assertEquals(2, PropertyHelper.getProperties(res).size());
      Assert.assertNotNull(res.getPropertyValue(PROPERTY_ID));
    }
  }

  @Test
  public void testPopulateResources__LargeNumberOfHostResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.Host, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID
    );

    Set<Resource> resources = new HashSet<Resource>();

    StringBuilder hostsList = new StringBuilder();
    
    for (int i = 0; i < 150; ++i) {
      Resource resource = new ResourceImpl(Resource.Type.Host);
      resource.setProperty(HOST_NAME_PROPERTY_ID, "host" + i);
      resources.add(resource);
      
      if (hostsList.length() != 0)
        hostsList.append("," + "host" + i );
      else
        hostsList.append("host" + i); 
    }

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);

    Assert.assertEquals(150, propertyProvider.populateResources(resources, request, null).size());

    
    URIBuilder expectedUri = new URIBuilder();
    
    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPJobTracker,HDPHBaseMaster,HDPResourceManager,HDPSlaves,HDPHistoryServer,HDPNameNode");
   
    expectedUri.setParameter("h", hostsList.toString());
    expectedUri.setParameter("m", "jvm.metrics.gcCount");
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());
    
    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));
  }
  
  class PopulateResourceManagerResourcesTest{
    public void testProperty(String property, String jmxMetricName) throws SystemException{
      TestStreamProvider streamProvider  = new TestStreamProvider("yarn_ganglia_data.txt");
      TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

      GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
          PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP2),
          streamProvider,
          configuration,
          hostProvider,
          CLUSTER_NAME_PROPERTY_ID,
          HOST_NAME_PROPERTY_ID,
          COMPONENT_NAME_PROPERTY_ID);

      // flume
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);

      resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
      resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");

      // only ask for one property
      Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
      temporalInfoMap.put(property, new TemporalInfoImpl(10L, 20L, 1L));
      Request  request = PropertyHelper.getReadRequest(Collections.singleton(property), temporalInfoMap);

      Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

      String expected = (configuration.isGangliaSSL() ? "https" : "http") +
          String.format("://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPResourceManager&h=ip-10-39-113-33.ec2.internal&m=%s&s=10&e=20&r=1",jmxMetricName);
      Assert.assertEquals(expected, streamProvider.getLastSpec());

      Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
      Assert.assertNotNull(resource.getPropertyValue(property));
    }
  }
  
  @Test
  public void testPopulateResources_resourcemanager_clustermetrics() throws Exception {
	  PopulateResourceManagerResourcesTest tester = new PopulateResourceManagerResourcesTest();
	  tester.testProperty("metrics/yarn/ClusterMetrics/NumActiveNMs","yarn.ClusterMetrics.NumActiveNMs");
	  tester.testProperty("metrics/yarn/ClusterMetrics/NumDecommissionedNMs","yarn.ClusterMetrics.NumDecommissionedNMs");
	  tester.testProperty("metrics/yarn/ClusterMetrics/NumLostNMs", "yarn.ClusterMetrics.NumLostNMs");
	  tester.testProperty("metrics/yarn/ClusterMetrics/NumUnhealthyNMs", "yarn.ClusterMetrics.NumUnhealthyNMs");
	  tester.testProperty("metrics/yarn/ClusterMetrics/NumRebootedNMs", "yarn.ClusterMetrics.NumRebootedNMs");
  }


  @Test
  public void testPopulateResources_params() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_SERVER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(FLUME_CHANNEL_CAPACITY_PROPERTY, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CHANNEL_CAPACITY_PROPERTY), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add(FLUME_CHANNEL_CAPACITY_PROPERTY);

    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_SERVER");
    
    URIBuilder expectedUri = new URIBuilder();
    
    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
    
    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_paramsMixed() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);
    
    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_SERVER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();

    Set<String> ids = new HashSet<String>();
    ids.add(FLUME_CATEGORY2);
    ids.add(PROPERTY_ID2);

    Request request = PropertyHelper.getReadRequest(ids, temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add("metrics/flume");
    metricsRegexes.add("metrics/cpu/cpu_wio");
    
    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_SERVER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("e", "now");
    expectedUri.setParameter("pt", "true");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));
       
    Assert.assertEquals(22, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID2));
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_paramsAll() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_SERVER");

    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.<String>emptySet(), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    String expected = (configuration.isGangliaSSL() ? "https" : "http") +
        "://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=ip-10-39-113-33.ec2.internal&m=";
    Assert.assertTrue(streamProvider.getLastSpec().startsWith(expected));

    Assert.assertEquals(33, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_params_category1() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_SERVER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(FLUME_CATEGORY, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CATEGORY), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add("metrics/flume");
    
    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_SERVER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    

    Assert.assertEquals(21, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_params_category2() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_SERVER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(FLUME_CATEGORY2, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CATEGORY2), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add("metrics/flume/");
    
    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_SERVER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));

    Assert.assertEquals(21, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_params_category3() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_SERVER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(FLUME_CATEGORY3, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CATEGORY3), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
    
    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add("metrics/flume/$1/CHANNEL/$2/");
    metricsRegexes.add(FLUME_CHANNEL_CAPACITY_PROPERTY);

    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_SERVER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    

    Assert.assertEquals(11, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_params_category4() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_SERVER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(FLUME_CATEGORY4, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CATEGORY4), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
    
    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add("metrics/flume/$1/CHANNEL/$2");
    metricsRegexes.add(FLUME_CHANNEL_CAPACITY_PROPERTY);

    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_SERVER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
    
    Assert.assertEquals(11, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }
  
  @Test
  public void testPopulateResources_params_category5() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data_yarn_queues.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP2);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);
    
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "dev01.ambari.apache.org");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(RM_CATEGORY_1, new TemporalInfoImpl(10L, 20L, 1L));
    
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(RM_CATEGORY_1), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
    
    List<String> metricsRegexes = new ArrayList<String>();
    
    metricsRegexes.add("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/");

    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "RESOURCEMANAGER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isGangliaSSL() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPResourceManager");
    expectedUri.setParameter("h", "dev01.ambari.apache.org");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
    
    
    Assert.assertTrue(PropertyHelper.getProperties(resource).size() > 2);
    Assert.assertNotNull(resource.getPropertyValue(RM_AVAILABLE_MEMORY_PROPERTY));
  }

  @Test
  public void testPopulateResources_journalNode() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider("journalnode_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP2),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "JOURNALNODE");


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




    Assert.assertEquals(properties.size() + 2, PropertyHelper.getProperties(resource).size());

    int i = 0;
    for (String property : properties) {
      Assert.assertEquals(testData[i++][2], resource.getPropertyValue(property));
    }
  }
  
  private boolean isUrlParamsEquals(URIBuilder actualUri, URIBuilder expectedUri) {
    for (final NameValuePair expectedParam : expectedUri.getQueryParams()) {
      NameValuePair actualParam = (NameValuePair) CollectionUtils.find(actualUri.getQueryParams(), new Predicate() {
        
        @Override
        public boolean evaluate(Object arg0) {
          if (!(arg0 instanceof NameValuePair))
            return false;
          
          NameValuePair otherObj = (NameValuePair) arg0;
          return otherObj.getName().equals(expectedParam.getName());
        }
      });
      

      List<String> actualParamList = new ArrayList<String>(Arrays.asList(actualParam.getValue().split(",")));
      List<String> expectedParamList = new ArrayList<String>(Arrays.asList(expectedParam.getValue().split(",")));
      
      Collections.sort(actualParamList);
      Collections.sort(expectedParamList);
      
      if (!actualParamList.equals(expectedParamList))
        return false;
    }
    
    return true;
  }
  
  private String getMetricsRegexes(List<String> metricsRegexes,
      Map<String, Map<String, PropertyInfo>> gangliaPropertyIds,
      String componentName) {
    
    StringBuilder metricsBuilder = new StringBuilder();
    
    for (Map.Entry<String, PropertyInfo> entry : gangliaPropertyIds.get(componentName).entrySet())
    {
      for (String metricRegex: metricsRegexes)
      {
        if (entry.getKey().startsWith(metricRegex)) {
          metricsBuilder.append(entry.getValue().getPropertyId() + ",");
        }
      }
    }
    return metricsBuilder.toString();
  }

  public static class TestGangliaHostProvider implements GangliaHostProvider {

    private boolean isHostLive;
    private boolean isComponentLive;
    
    public TestGangliaHostProvider() {
      this(true, true);
    }

    public TestGangliaHostProvider(boolean isHostLive, boolean isComponentLive) {
      this.isHostLive = isHostLive;
      this.isComponentLive = isComponentLive;
    }

    @Override
    public String getGangliaCollectorHostName(String clusterName) {
      return "domU-12-31-39-0E-34-E1.compute-1.internal";
    }

    @Override
    public boolean isGangliaCollectorHostLive(String clusterName)
        throws SystemException {
      return isHostLive;
    }

    @Override
    public boolean isGangliaCollectorComponentLive(String clusterName)
        throws SystemException {
      return isComponentLive;
    }
  }
}
