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
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * JMX property provider tests.
 */
public class JMXPropertyProviderTest {
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");

  @Test
  public void testGetResources() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider, PropertyHelper.getPropertyId("HostRoles", "cluster_name"), PropertyHelper.getPropertyId("HostRoles", "host_name"), PropertyHelper.getPropertyId("HostRoles", "component_name"));

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

    request = PropertyHelper.getReadRequest(properties);

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("domu-12-31-39-14-ee-b3.compute-1.internal", "50030"), streamProvider.getLastSpec());

    // see test/resources/mapreduce_jobtracker_jmx.json for values
    Assert.assertEquals(10, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(59, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsWaiting")));
    Assert.assertEquals(1052770304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(43580400, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(29602888, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_submitted")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_completed")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/mapred/jobtracker", "jobs_failed")));

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

    Assert.assertEquals(7, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(1069416448, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(4806976, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(28971240, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(3.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/load", "AverageLoad")));

    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcCount")));
  }

  @Test
  public void testGetResourcesWithUnknownPort() throws Exception {
    TestStreamProvider  streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostProvider, PropertyHelper.getPropertyId("HostRoles", "cluster_name"), PropertyHelper.getPropertyId("HostRoles", "host_name"), PropertyHelper.getPropertyId("HostRoles", "component_name"));

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

  private static class TestJMXHostProvider implements JMXHostProvider {
    private final boolean unknownPort;

    private TestJMXHostProvider(boolean unknownPort) {
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
        return "60010";
      else
        return null;
    }

  }
}
