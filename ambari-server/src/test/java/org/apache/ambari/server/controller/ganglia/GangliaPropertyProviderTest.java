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
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");

  @Test
  public void testGetResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider();
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent),
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

    Assert.assertEquals("http://ec2-23-23-71-42.compute-1.amazonaws.com/cgi-bin/rrd.py?c=HDPSlaves&h=domU-12-31-39-0E-34-E1.compute-1.internal&m=jvm.metrics.gcCount&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    Assert.assertEquals(3, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID));
  }


  @Test
  public void testGetManyResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider();
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostPropertyProvider(
        PropertyHelper.getGangliaPropertyIds(Resource.Type.Host),
        streamProvider,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID
    );

    Set<Resource> resources = new HashSet<Resource>();

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

    Assert.assertEquals("http://ec2-23-23-71-42.compute-1.amazonaws.com/cgi-bin/rrd.py?c=HDPSlaves&h=domU-12-31-39-0E-34-E3.compute-1.internal,domU-12-31-39-0E-34-E1.compute-1.internal,domU-12-31-39-0E-34-E2.compute-1.internal&m=jvm.metrics.gcCount&s=10&e=20&r=1",
        streamProvider.getLastSpec());

    for (Resource res : resources) {
      Assert.assertEquals(2, PropertyHelper.getProperties(res).size());
      Assert.assertNotNull(res.getPropertyValue(PROPERTY_ID));
    }
  }

  private static class TestGangliaHostProvider implements GangliaHostProvider {

    @Override
    public String getGangliaCollectorHostName(String clusterName) {
      return "ec2-23-23-71-42.compute-1.amazonaws.com";
    }
  }
}
