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

package org.apache.ambari.msi;

import junit.framework.Assert;
import org.apache.ambari.scom.TestClusterDefinitionProvider;
import org.apache.ambari.scom.TestHostInfoProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;

/**
 *
 */
public class HostProviderTest {

  @Test
  public void testGetResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    HostProvider provider = new HostProvider(clusterDefinition);
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    Assert.assertEquals(13, resources.size());
  }

  @Test
  public void testGetResourcesWithPredicate() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    HostProvider provider = new HostProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(HostProvider.HOST_NAME_PROPERTY_ID).equals("NAMENODE_MASTER.acme.com").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    predicate = new PredicateBuilder().property(HostProvider.HOST_NAME_PROPERTY_ID).equals("HBASE_MASTER.acme.com").or().
        property(HostProvider.HOST_NAME_PROPERTY_ID).equals("slave3.acme.com").toPredicate();
    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(2, resources.size());

    predicate = new PredicateBuilder().property(HostProvider.HOST_NAME_PROPERTY_ID).equals("unknownHost").toPredicate();
    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertTrue(resources.isEmpty());
  }

  @Test
  public void testGetResourcesHostIP() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    HostProvider provider = new HostProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(HostProvider.HOST_NAME_PROPERTY_ID).equals("NAMENODE_MASTER.acme.com").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    String ip = (String) resource.getPropertyValue(HostProvider.HOST_IP_PROPERTY_ID);

    Assert.assertEquals("127.0.0.1", ip);
  }

  @Test
  public void testGetResourcesCheckState() throws Exception {
    TestStateProvider stateProvider = new TestStateProvider();
    ClusterDefinition clusterDefinition = new ClusterDefinition(stateProvider, new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    HostProvider provider = new HostProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(HostProvider.HOST_NAME_PROPERTY_ID).equals("slave3.acme.com").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("HEALTHY", resource.getPropertyValue(HostProvider.HOST_STATE_PROPERTY_ID));

    stateProvider.setState(StateProvider.State.Unknown);

    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    resource = resources.iterator().next();
    Assert.assertEquals("UNHEALTHY", resource.getPropertyValue(HostProvider.HOST_STATE_PROPERTY_ID));
  }

  @Test
  public void testGetResourcesCheckStateFromCategory() throws Exception {
    TestStateProvider stateProvider = new TestStateProvider();
    ClusterDefinition clusterDefinition = new ClusterDefinition(stateProvider, new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    HostProvider provider = new HostProvider(clusterDefinition);
    Predicate predicate = new PredicateBuilder().property(HostProvider.HOST_NAME_PROPERTY_ID).equals("slave3.acme.com").toPredicate();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest("Hosts"), predicate);
    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("HEALTHY", resource.getPropertyValue(HostProvider.HOST_STATE_PROPERTY_ID));

    stateProvider.setState(StateProvider.State.Unknown);

    resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);
    Assert.assertEquals(1, resources.size());

    resource = resources.iterator().next();
    Assert.assertEquals("UNHEALTHY", resource.getPropertyValue(HostProvider.HOST_STATE_PROPERTY_ID));
  }

  @Test
  public void testCreateResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    HostProvider provider = new HostProvider(clusterDefinition);

    try {
      provider.createResources(PropertyHelper.getReadRequest());
      Assert.fail("Expected UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      //expected
    }
  }

  @Test
  public void testUpdateResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    HostProvider provider = new HostProvider(clusterDefinition);

    provider.updateResources(PropertyHelper.getUpdateRequest(new HashMap<String, Object>(), null), null);
  }

  @Test
  public void testDeleteResources() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());
    HostProvider provider = new HostProvider(clusterDefinition);

    try {
      provider.deleteResources(null);
      Assert.fail("Expected UnsupportedOperationException.");
    } catch (UnsupportedOperationException e) {
      //expected
    }
  }
}

