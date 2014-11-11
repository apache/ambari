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

package org.apache.ambari.server.controller.gsinstaller;

import java.util.HashMap;
import java.util.Set;

import junit.framework.Assert;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

/**
 *
 */
public class GSInstallerHostComponentProviderTest {

  @Test
  public void testGetResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestGSInstallerStateProvider());
    GSInstallerResourceProvider provider = new GSInstallerHostComponentProvider(clusterDefinition);
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    Assert.assertEquals(32, resources.size());
  }

  @Test
  public void testGetResourcesWithPredicate() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestGSInstallerStateProvider());
    GSInstallerResourceProvider provider = new GSInstallerHostComponentProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(GSInstallerHostComponentProvider.HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID).equals("MAPREDUCE").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(5, resources.size());

    predicate = new PredicateBuilder().property(GSInstallerHostComponentProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID).equals("UnknownHost").toPredicate();
    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertTrue(resources.isEmpty());
  }

  @Test
  public void testGetResourcesCheckState() throws Exception {
    TestGSInstallerStateProvider stateProvider = new TestGSInstallerStateProvider();
    ClusterDefinition clusterDefinition = new ClusterDefinition(stateProvider, 500);
    GSInstallerResourceProvider provider = new GSInstallerHostComponentProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(GSInstallerHostComponentProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("HBASE_REGIONSERVER").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(3, resources.size());

    for (Resource resource : resources) {
      Assert.assertEquals("STARTED", resource.getPropertyValue(GSInstallerHostComponentProvider.HOST_COMPONENT_STATE_PROPERTY_ID));
    }

    stateProvider.setHealthy(false);

    // need to wait for old state value to expire
    Thread.sleep(501);

    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(3, resources.size());

    for (Resource resource : resources) {
      Assert.assertEquals("INIT", resource.getPropertyValue(GSInstallerHostComponentProvider.HOST_COMPONENT_STATE_PROPERTY_ID));
    }
  }

  @Test
  public void testGetResourcesCheckStateFromCategory() throws Exception {
    TestGSInstallerStateProvider stateProvider = new TestGSInstallerStateProvider();
    ClusterDefinition clusterDefinition = new ClusterDefinition(stateProvider, 500);
    GSInstallerResourceProvider provider = new GSInstallerHostComponentProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(GSInstallerHostComponentProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("HBASE_REGIONSERVER").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest("HostRoles"), predicate);
    Assert.assertEquals(3, resources.size());

    for (Resource resource : resources) {
      Assert.assertEquals("STARTED", resource.getPropertyValue(GSInstallerHostComponentProvider.HOST_COMPONENT_STATE_PROPERTY_ID));
    }

    stateProvider.setHealthy(false);

    // need to wait for old state value to expire
    Thread.sleep(501);

    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(3, resources.size());

    for (Resource resource : resources) {
      Assert.assertEquals("INIT", resource.getPropertyValue(GSInstallerHostComponentProvider.HOST_COMPONENT_STATE_PROPERTY_ID));
    }
  }

  @Test
  public void testCreateResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestGSInstallerStateProvider());
    GSInstallerResourceProvider provider = new GSInstallerHostComponentProvider(clusterDefinition);

    try {
      provider.createResources(PropertyHelper.getReadRequest());
      Assert.fail("Expected UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      //expected
    }
  }

  @Test
  public void testUpdateResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestGSInstallerStateProvider());
    GSInstallerResourceProvider provider = new GSInstallerHostComponentProvider(clusterDefinition);

    try {
      provider.updateResources(PropertyHelper.getUpdateRequest(new HashMap<String, Object>(), null), null);
      Assert.fail("Expected UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      //expected
    }
  }

  @Test
  public void testDeleteResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestGSInstallerStateProvider());
    GSInstallerResourceProvider provider = new GSInstallerHostComponentProvider(clusterDefinition);

    try {
      provider.deleteResources(null);
      Assert.fail("Expected UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      //expected
    }
  }
}
