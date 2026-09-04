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
package org.apache.ambari.server.controller.utilities;

import java.util.Map;

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.stack.Metric;
import org.junit.Assert;
import org.junit.Test;

public class PropertyHelperTest {

  @Test
  public void testPropertyIdComposition() {
    Assert.assertEquals("metrics/jvm/HeapMemoryUsed",
        PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed"));
    Assert.assertEquals("metrics/jvm", PropertyHelper.getPropertyId("metrics/jvm/", null));
    Assert.assertEquals("HeapMemoryUsed", PropertyHelper.getPropertyId(null, "HeapMemoryUsed"));
  }

  @Test
  public void testJmxDefinitionsAreAvailable() {
    Map<String, Map<String, PropertyInfo>> metrics =
        PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent);

    Assert.assertNotNull(metrics);
    Assert.assertFalse(metrics.isEmpty());
  }

  @Test
  public void testNameNodeRpcDefinitionsExpandForJmxTags() {
    Metric metric = new Metric("Hadoop:service=NameNode,name=RpcActivity.RpcQueueTimeAvgTime",
        true, false, "milliseconds");

    Map<String, Metric> replacements = PropertyHelper.processRpcMetricDefinition(
        "jmx", "NAMENODE", "metrics/rpc/RpcQueueTime_avg_time", metric);

    Assert.assertNotNull(replacements);
    Assert.assertEquals(3, replacements.size());
    Assert.assertTrue(replacements.containsKey("metrics/rpc/client/RpcQueueTime_avg_time"));
    Assert.assertTrue(replacements.get("metrics/rpc/client/RpcQueueTime_avg_time")
        .getName().contains("tag=client"));
  }
}
