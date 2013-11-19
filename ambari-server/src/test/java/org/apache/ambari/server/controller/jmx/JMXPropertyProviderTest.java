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

package org.apache.ambari.server.controller.jmx;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


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

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-0e-34-e1.compute-1.internal", "50070"), streamProvider.getLastSpec());

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

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-14-ee-b3.compute-1.internal", "50075"), streamProvider.getLastSpec());

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

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-14-ee-b3.compute-1.internal", "50030"), streamProvider.getLastSpec());

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

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-14-ee-b3.compute-1.internal", "50060"), streamProvider.getLastSpec());

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

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-14-ee-b3.compute-1.internal", "60010"), streamProvider.getLastSpec());

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

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/rpc/ReceivedBytes"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-0e-34-e1.compute-1.internal", "50070"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605,  resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
    Assert.assertNull(resource.getPropertyValue("metrics/dfs/namenode/CreateFileOps"));
  }

  @Test
  public void testPopulateResources_category() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

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

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-0e-34-e1.compute-1.internal", "50070"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(184320,  resource.getPropertyValue("metrics/dfs/FSNamesystem/CapacityUsed"));
    Assert.assertEquals(21,  resource.getPropertyValue("metrics/dfs/FSNamesystem/UnderReplicatedBlocks"));
    Assert.assertNull(resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
  }

  @Test
  public void testPopulateResourcesWithUnknownPort() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-0e-34-e1.compute-1.internal", "50070"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605,  resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28,      resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
    Assert.assertEquals(1006632960, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(473433016, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(23634400, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
  }

  @Test
  public void testPopulateResources_HDP2() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    // resourcemanager
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-0e-34-e1.compute-1.internal", "8088"), streamProvider.getLastSpec());

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

    //namenode
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-0e-34-e1" +
      ".compute-1.internal", "50070"), streamProvider.getLastSpec());
    Assert.assertEquals("active", resource.getPropertyValue(PropertyHelper
      .getPropertyId("metrics/dfs/FSNamesystem", "HAState")));
  }
  
  @Test
  public void testPopulateResources_HDP2_params() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("h1", "8088"), streamProvider.getLastSpec());

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

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue/root/AvailableMB"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("h1", "8088"), streamProvider.getLastSpec());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
  }

  @Test
  public void testPopulateResources_HDP2_params_category() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("h1", "8088"), streamProvider.getLastSpec());

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

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue/root/default"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("h1", "8088"), streamProvider.getLastSpec());

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
  public void testPopulateResourcesUnhealthyResource() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

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
    Set<Resource> resources = new HashSet<Resource>();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP1),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

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
  public void testPopulateResources_JournalNode() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    // journalnode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOURNALNODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<String>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-0e-34-e1.compute-1.internal", "8480"), streamProvider.getLastSpec());

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
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        null, // force use of the hostProvider, which returns null for this test
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

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
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.MetricsVersion.HDP2),
        streamProvider,
        hostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        Collections.singleton("STARTED"));

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "HBM2");
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
    
  public static class TestJMXHostProvider implements JMXHostProvider {
    private final boolean unknownPort;

    public TestJMXHostProvider(boolean unknownPort) {
      this.unknownPort = unknownPort;
    }

    @Override
    public String getHostName(String clusterName, String componentName) {
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
      else
        return null;
    }

    @Override
    public String getJMXProtocol(String clusterName, String componentName) {
      return "http";
    }
    
  }
}
