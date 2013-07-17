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

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper.MetricsVersion;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test the Ganglia property provider.
 */
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

  @Test
  public void testPopulateResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
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

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=domU-12-31-39-0E-34-E1.compute-1.internal&m=jvm.metrics.gcCount&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID));


    // tasktracker
    resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "TASKTRACKER");

    // only ask for one property
    temporalInfoMap = new HashMap<String, TemporalInfo>();

    //http://ec2-174-129-152-147.compute-1.amazonaws.com/cgi-bin/rrd.py?c=HDPSlaves&m=jvm.metrics.gcCount,mapred.shuffleOutput.shuffle_exceptions_caught,mapred.shuffleOutput.shuffle_failed_outputs,mapred.shuffleOutput.shuffle_output_bytes,mapred.shuffleOutput.shuffle_success_outputs&s=10&e=20&r=1&h=ip-10-85-111-149.ec2.internal

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

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=domU-12-31-39-0E-34-E1.compute-1.internal&m=mapred.shuffleOutput.shuffle_output_bytes,mapred.shuffleOutput.shuffle_success_outputs,mapred.shuffleOutput.shuffle_failed_outputs,mapred.shuffleOutput.shuffle_exceptions_caught&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(6, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(shuffle_exceptions_caught));
    Assert.assertNotNull(resource.getPropertyValue(shuffle_failed_outputs));
    Assert.assertNotNull(resource.getPropertyValue(shuffle_output_bytes));
    Assert.assertNotNull(resource.getPropertyValue(shuffle_success_outputs));
  }


  @Test
  public void testPopulateManyResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.Host, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
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
    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPJobTracker,HDPHBaseMaster,HDPResourceManager,HDPSlaves,HDPHistoryServer,HDPNameNode&h=domU-12-31-39-0E-34-E3.compute-1.internal,domU-12-31-39-0E-34-E1.compute-1.internal,domU-12-31-39-0E-34-E2.compute-1.internal&m=jvm.metrics.gcCount&s=10&e=20&r=1",
        streamProvider.getLastSpec());

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
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID
    );

    Set<Resource> resources = new HashSet<Resource>();

    for (int i = 0; i < 150; ++i) {
      Resource resource = new ResourceImpl(Resource.Type.Host);
      resource.setProperty(HOST_NAME_PROPERTY_ID, "host" + i);
      resources.add(resource);
    }

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);

    Assert.assertEquals(150, propertyProvider.populateResources(resources, request, null).size());

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPJobTracker,HDPHBaseMaster,HDPResourceManager,HDPSlaves,HDPHistoryServer,HDPNameNode&m=jvm.metrics.gcCount&s=10&e=20&r=1",
        streamProvider.getLastSpec());

  }


  @Test
  public void testPopulateResources_params() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1),
        streamProvider,
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

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=ip-10-39-113-33.ec2.internal&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_paramsMixed() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1),
        streamProvider,
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

    Request  request = PropertyHelper.getReadRequest(ids, temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=ip-10-39-113-33.ec2.internal&e=now&pt=true",
        streamProvider.getLastSpec());

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

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=ip-10-39-113-33.ec2.internal&e=now&pt=true",
        streamProvider.getLastSpec());

    Assert.assertEquals(33, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_params_category1() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1),
        streamProvider,
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

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=ip-10-39-113-33.ec2.internal&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(21, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_params_category2() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1),
        streamProvider,
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

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=ip-10-39-113-33.ec2.internal&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(21, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_params_category3() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1),
        streamProvider,
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

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=ip-10-39-113-33.ec2.internal&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(11, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  @Test
  public void testPopulateResources_params_category4() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent, MetricsVersion.HDP1),
        streamProvider,
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

    Assert.assertEquals("http://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPSlaves&h=ip-10-39-113-33.ec2.internal&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(11, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  private static class TestGangliaHostProvider implements GangliaHostProvider {

    @Override
    public String getGangliaCollectorHostName(String clusterName) {
      return "domU-12-31-39-0E-34-E1.compute-1.internal";
    }
  }
}
