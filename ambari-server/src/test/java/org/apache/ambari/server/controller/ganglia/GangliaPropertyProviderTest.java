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
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Test the Ganglia property provider.
 */
public class GangliaPropertyProviderTest {

  private static final PropertyId PROPERTY_ID = PropertyHelper.getPropertyId("bytes_out", "metrics", true);
  private static final PropertyId HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("host_name", "HostRoles");
  private static final PropertyId COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "HostRoles");

  @Test
  public void testGetResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider();

    GangliaPropertyProvider propertyProvider = new GangliaPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        "ec2-23-23-71-42.compute-1.amazonaws.com",
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "NAMENODE");

    // only ask for one property
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID));

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals("http://ec2-23-23-71-42.compute-1.amazonaws.com/ganglia/graph.php?c=HDPNameNode&h=domU-12-31-39-0E-34-E1.compute-1.internal&m=bytes_out&json=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID));
  }
}
