/*
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
package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.stack.Metric;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.junit.Assert;
import org.junit.Test;

public class StackDefinedPropertyProviderTest {

  @Test
  public void testGetPropertyInfoKeepsJmxControlPlaneMetadata() {
    Map<String, Metric> metrics = new LinkedHashMap<>();
    metrics.put("metrics/dfs/FSNamesystem/HAState",
        new Metric("Hadoop:service=NameNode,name=FSNamesystem.HAState", true, false, "unitless"));
    metrics.put("metrics/dfs/FSNamesystem/Missing", new Metric(null, true, false, "unitless"));

    MetricDefinition definition = new MetricDefinition("jmx", Collections.emptyMap(),
        Collections.singletonMap("Component", metrics));

    Map<String, PropertyInfo> result = StackDefinedPropertyProvider.getPropertyInfo(definition);

    Assert.assertEquals(1, result.size());
    PropertyInfo property = result.get("metrics/dfs/FSNamesystem/HAState");
    Assert.assertEquals("Hadoop:service=NameNode,name=FSNamesystem.HAState", property.getPropertyId());
    Assert.assertTrue(property.isPointInTime());
    Assert.assertFalse(property.isTemporal());
    Assert.assertEquals("unitless", property.getUnit());
  }

  @Test(expected = NullPointerException.class)
  public void testClusterPropertyIsRequired() {
    new StackDefinedPropertyProvider(Resource.Type.HostComponent, null, null, null,
        null, "HostRoles/host_name", "HostRoles/component_name", "HostRoles/state", null);
  }

  @Test(expected = NullPointerException.class)
  public void testComponentPropertyIsRequired() {
    new StackDefinedPropertyProvider(Resource.Type.HostComponent, null, null, null,
        "HostRoles/cluster_name", "HostRoles/host_name", null, "HostRoles/state", null);
  }
}
