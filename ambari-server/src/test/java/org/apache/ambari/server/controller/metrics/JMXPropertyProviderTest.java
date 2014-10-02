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

package org.apache.ambari.server.controller.metrics;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.jmx.JMXHostProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.jmx.TestStreamProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;


/**
 * JMX property provider tests.
 */
public class JMXPropertyProviderTest {
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "state");

  public static final int NUMBER_OF_RESOURCES = 400;

  @Test
  public void testPopulateResources() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28,      resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
    Assert.assertEquals(1006632960, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(473433016, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(23634400, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(887717691390L, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/FSNamesystem", "CapacityTotal")));
    Assert.assertEquals(184320, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/FSNamesystem", "CapacityUsed")));
    Assert.assertEquals(842207944704L, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/FSNamesystem", "CapacityRemaining")));
    Assert.assertEquals(45509562366L, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/FSNamesystem", "CapacityNonDFSUsed")));

    // datanode
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    // request with an empty set should get all supported properties
    request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-14-ee-b3.compute-1.internal", "50075", "/jmx"), streamProvider.getLastSpec());

    // see test/resources/hdfs_datanode_jmx.json for values
    Assert.assertEquals(856,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(9772616, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(21933376, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));


    // jobtracker
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOBTRACKER");

    // only ask for specific properties
    Set<String> properties = new HashSet<String>();
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "threadsWaiting"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_submitted"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_completed"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_failed"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "maps_launched"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "maps_completed"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "maps_failed"));

    request = PropertyHelper.getReadRequest(properties);

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-14-ee-b3.compute-1.internal", "50030", "/jmx"), streamProvider.getLastSpec());

    // see test/resources/mapreduce_jobtracker_jmx.json for values
    Assert.assertEquals(13, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(59, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsWaiting")));
    Assert.assertEquals(1052770304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(43580400, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(29602888, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_submitted")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_completed")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_failed")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "maps_launched")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "maps_completed")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "maps_failed")));

    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcCount")));

    // tasktracker
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "TASKTRACKER");

    // only ask for specific properties
    properties = new HashSet<String>();
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_exceptions_caught"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_failed_outputs"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_output_bytes"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_success_outputs"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "maps_running"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "reduces_running"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "mapTaskSlots"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "reduceTaskSlots"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "failedDirs"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "tasks_completed"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "tasks_failed_timeout"));
    properties.add(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "tasks_failed_ping"));

    request = PropertyHelper.getReadRequest(properties);

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-14-ee-b3.compute-1.internal", "50060", "/jmx"), streamProvider.getLastSpec());

    Assert.assertEquals(18, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(18330984, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(24235104, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_exceptions_caught")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_failed_outputs")));
    Assert.assertEquals(1841, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_output_bytes")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_success_outputs")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "maps_running")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "reduces_running")));
    Assert.assertEquals(4, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "mapTaskSlots")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "reduceTaskSlots")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "failedDirs")));
    Assert.assertEquals(4, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "tasks_completed")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "tasks_failed_timeout")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/tasktracker", "tasks_failed_ping")));


    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcCount")));

    // hbase master
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_MASTER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for specific properties
    properties = new HashSet<String>();
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed"));
    properties.add(PropertyHelper.getPropertyId("metrics/load", "AverageLoad"));
    request = PropertyHelper.getReadRequest(properties);

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-14-ee-b3.compute-1.internal", "60010", "/jmx"), streamProvider.getLastSpec());

    Assert.assertEquals(8, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(1069416448, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(4806976, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(28971240, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(3.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/load", "AverageLoad")));

    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcCount")));
  }

  @Test
  public void testPopulateResources_singleProperty() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/rpc/ReceivedBytes"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605,  resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
    Assert.assertNull(resource.getPropertyValue("metrics/dfs/namenode/CreateFileOps"));
  }

  @Test
  public void testPopulateResources_category() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/dfs"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("http","domu-12-31-39-0e-34-e1.compute-1.internal", "50070","/jmx"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(184320,  resource.getPropertyValue("metrics/dfs/FSNamesystem/CapacityUsed"));
    Assert.assertEquals(21,  resource.getPropertyValue("metrics/dfs/FSNamesystem/UnderReplicatedBlocks"));
    Assert.assertNull(resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
  }

  @Test
  public void testPopulateResourcesWithUnknownPort() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("http","domu-12-31-39-0e-34-e1.compute-1.internal", "50070","/jmx"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28,      resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
    Assert.assertEquals(1006632960, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(473433016, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(23634400, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
  }

  @Test
  public void testPopulateResourcesUnhealthyResource() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "INSTALLED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // Assert that the stream provider was never called.
    Assert.assertNull(streamProvider.getLastSpec());
  }

  @Test
  public void testPopulateResourcesMany() throws Exception {
    // Set the provider to take 50 millis to return the JMX values
    TestStreamProvider  streamProvider = new TestStreamProvider(50L);
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();
    Set<Resource> resources = new HashSet<Resource>();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"));

    for (int i = 0; i < NUMBER_OF_RESOURCES; ++i) {
      // datanode
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);

      resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
      resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");
      resource.setProperty("unique_id", i);

      resources.add(resource);
    }

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Set<Resource> resourceSet = propertyProvider.populateResources(resources, request, null);

    Assert.assertEquals(NUMBER_OF_RESOURCES, resourceSet.size());

    for (Resource resource : resourceSet) {
      // see test/resources/hdfs_datanode_jmx.json for values
      Assert.assertEquals(856,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
      Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
      Assert.assertEquals(9772616, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
      Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
      Assert.assertEquals(21933376, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    }
  }

  @Test
  public void testPopulateResourcesTimeout() throws Exception {
    // Set the provider to take 100 millis to return the JMX values
    TestStreamProvider  streamProvider = new TestStreamProvider(100L);
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();
    Set<Resource> resources = new HashSet<Resource>();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider,
        metricsHostProvider,
        "HostRoles/cluster_name",
        "HostRoles/host_name",
        "HostRoles/component_name",
        "HostRoles/state");

    // set the provider timeout to 50 millis
    propertyProvider.setPopulateTimeout(50L);

    // datanode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    resources.add(resource);

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Set<Resource> resourceSet = propertyProvider.populateResources(resources, request, null);

    // make sure that the thread running the stream provider has completed
    Thread.sleep(150L);

    Assert.assertEquals(0, resourceSet.size());

    // assert that properties never get set on the resource
    Assert.assertNull(resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
    Assert.assertNull(resource.getPropertyValue("metrics/jvm/HeapMemoryMax"));
    Assert.assertNull(resource.getPropertyValue("metrics/jvm/HeapMemoryUsed"));
    Assert.assertNull(resource.getPropertyValue("metrics/jvm/NonHeapMemoryMax"));
    Assert.assertNull(resource.getPropertyValue("metrics/jvm/NonHeapMemoryUsed"));
  }

  public static class TestJMXHostProvider implements JMXHostProvider {
    private final boolean unknownPort;

    public TestJMXHostProvider(boolean unknownPort) {
      this.unknownPort = unknownPort;
    }

    @Override
    public Set<String> getHostNames(String clusterName, String componentName) {
      return null;
    }

    @Override
    public String getPort(String clusterName, String componentName) throws
        SystemException {

      if (unknownPort) {
        return null;
      }

      if (componentName.equals("NAMENODE"))
        return "50070";
      else if (componentName.equals("DATANODE"))
        return "50075";
      else if (componentName.equals("JOBTRACKER"))
        return "50030";
      else if (componentName.equals("TASKTRACKER"))
        return "50060";
      else if (componentName.equals("HBASE_MASTER"))
        return null == clusterName ? "60010" : "60011";
      else  if (componentName.equals("JOURNALNODE"))
        return "8480";
      else  if (componentName.equals("STORM_REST_API"))
        return "8745";
      else
        return null;
    }

    @Override
    public String getJMXProtocol(String clusterName, String componentName) {
      return "http";
    }

  }

  public static class TestMetricsHostProvider implements MetricsHostProvider {

    @Override
    public String getHostName(String clusterName, String componentName) {
      return null;
    }
  }
}
