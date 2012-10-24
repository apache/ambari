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
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 * JMX property provider tests.
 */
public class JMXPropertyProviderTest {

    @Test
  public void testGetResources() throws Exception {

    TestStreamProvider  streamProvider = new TestStreamProvider();
    Map<String, String> hostMap        = TestHostMappingProvider.getHostMap();

    JMXPropertyProvider propertyProvider = new JMXPropertyProvider(
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        hostMap);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(JMXPropertyProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(JMXPropertyProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.<PropertyId>emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("ec2-50-17-129-192.compute-1.amazonaws.com:50070"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(1084287,  resource.getPropertyValue(PropertyHelper.getPropertyId("ReceivedBytes", "metrics/rpc")));
    Assert.assertEquals(173,      resource.getPropertyValue(PropertyHelper.getPropertyId("CreateFileOps", "metrics/dfs/namenode")));
    Assert.assertEquals(405.8686, resource.getPropertyValue(PropertyHelper.getPropertyId("memHeapUsedM",  "metrics/jvm")));


    // datanode
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(JMXPropertyProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(JMXPropertyProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    // request with an empty set should get all supported properties
    request = PropertyHelper.getReadRequest(Collections.<PropertyId>emptySet());

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("ec2-23-23-71-42.compute-1.amazonaws.com:50075"), streamProvider.getLastSpec());

    // see test/resources/hdfs_datanode_jmx.json for values
    Assert.assertEquals(0,  resource.getPropertyValue(PropertyHelper.getPropertyId("ReceivedBytes", "metrics/rpc")));
    Assert.assertEquals(16.870667, resource.getPropertyValue(PropertyHelper.getPropertyId("memHeapUsedM",  "metrics/jvm")));


    // jobtracker
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(JMXPropertyProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(JMXPropertyProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOBTRACKER");

    // only ask for one property
    request = PropertyHelper.getReadRequest(Collections.singleton(PropertyHelper.getPropertyId("threadsWaiting", "metrics/jvm")));

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("ec2-23-23-71-42.compute-1.amazonaws.com:50030"), streamProvider.getLastSpec());

    // see test/resources/mapreduce_jobtracker_jmx.json for values
    // resource should now contain 3 properties... host name, component name, and jvm.threadsWaiting (from request)
    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(59, resource.getPropertyValue(PropertyHelper.getPropertyId("threadsWaiting", "metrics/jvm")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("gcCount", "metrics/jvm")));
  }
}
