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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 * Abstract metric provider tests.
 */
public class AbstractPropertyProviderTest {


  @Test
  public void testGetComponentMetrics() {
    Map<String, Map<String, PropertyInfo>> componentMetrics = PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent);
    AbstractPropertyProvider provider = new TestPropertyProvider(componentMetrics);
    Assert.assertEquals(componentMetrics, provider.getComponentMetrics());
  }

  @Test
  public void testGetPropertyInfoMap() {
    AbstractPropertyProvider provider = new TestPropertyProvider(PropertyHelper.getGangliaPropertyIds(Resource.Type.HostComponent));

    // specific property
    Map<String, PropertyInfo> propertyInfoMap = provider.getPropertyInfoMap("NAMENODE", "metrics/cpu/cpu_aidle");
    Assert.assertEquals(1, propertyInfoMap.size());
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/cpu/cpu_aidle"));

    // category
    propertyInfoMap = provider.getPropertyInfoMap("NAMENODE", "metrics/disk");
    Assert.assertEquals(3, propertyInfoMap.size());
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/disk/disk_free"));
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/disk/disk_total"));
    Assert.assertTrue(propertyInfoMap.containsKey("metrics/disk/part_max_used"));
  }

  static class TestPropertyProvider extends AbstractPropertyProvider {

    public TestPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics) {
      super(componentMetrics);
    }

    @Override
    public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws SystemException {
      return null;
    }
  }
}
