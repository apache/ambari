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
package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;


/**
 * Property helper tests.
 */
public class PropertyHelperTest {

  @Test
  public void testGetPropertyId() {
    Assert.assertEquals("foo", PropertyHelper.getPropertyId("", "foo"));
    Assert.assertEquals("foo", PropertyHelper.getPropertyId(null, "foo"));
    Assert.assertEquals("foo", PropertyHelper.getPropertyId(null, "foo/"));

    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat", ""));
    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat", null));
    Assert.assertEquals("cat", PropertyHelper.getPropertyId("cat/", null));

    Assert.assertEquals("cat/foo", PropertyHelper.getPropertyId("cat", "foo"));
    Assert.assertEquals("cat/sub/foo", PropertyHelper.getPropertyId("cat/sub", "foo"));
    Assert.assertEquals("cat/sub/foo", PropertyHelper.getPropertyId("cat/sub", "foo/"));
  }

  @Test
  public void testGetJMXPropertyIds() {

    //version 1
    Map<String, Map<String, PropertyInfo>> metrics = PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.JMXMetricsVersion.One);
    Map<String, PropertyInfo> componentMetrics = metrics.get("HISTORYSERVER");
    Assert.assertNull(componentMetrics);
    componentMetrics = metrics.get("NAMENODE");
    Assert.assertNotNull(componentMetrics);
    PropertyInfo info = componentMetrics.get("metrics/jvm/memHeapUsedM");
    Assert.assertNotNull(info);
    Assert.assertEquals("Hadoop:service=NameNode,name=jvm.memHeapUsedM", info.getPropertyId());

    //version 2
    metrics = PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent, PropertyHelper.JMXMetricsVersion.Two);
    componentMetrics = metrics.get("HISTORYSERVER");
    Assert.assertNotNull(componentMetrics);
    componentMetrics = metrics.get("NAMENODE");
    Assert.assertNotNull(componentMetrics);
    info = componentMetrics.get("metrics/jvm/memHeapUsedM");
    Assert.assertNotNull(info);
    Assert.assertEquals("Hadoop:service=NameNode,name=JvmMetrics.MemHeapUsedM", info.getPropertyId());
  }
}

