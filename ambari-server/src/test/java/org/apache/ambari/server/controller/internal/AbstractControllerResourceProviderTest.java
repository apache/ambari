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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Abstract controller resource provider test.
 */
public class AbstractControllerResourceProviderTest {
  @Test
  public void testGetResourceProvider() throws Exception {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");
    propertyIds.add("cat5/subcat5/map");

    Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);

    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    ResourceProvider serviceResourceProvider = new ServiceResourceProvider(propertyIds, keyPropertyIds, managementController, maintenanceStateHelper);
    expect(factory.getServiceResourceProvider(propertyIds, keyPropertyIds, managementController)).andReturn(serviceResourceProvider);

    AbstractControllerResourceProvider.init(factory);

    replay(managementController, factory, maintenanceStateHelper);

    AbstractResourceProvider provider =
        (AbstractResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
            Resource.Type.Service,
            propertyIds,
            keyPropertyIds,
            managementController);

    Assert.assertTrue(provider instanceof ServiceResourceProvider);
  }

  @Test
  public void testGetStackArtifactResourceProvider() {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.StackArtifact, null, null, managementController);

    assertEquals(StackArtifactResourceProvider.class, provider.getClass());
  }

  @Test
  public void testGetRoleAuthorizationResourceProvider() {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.RoleAuthorization, null, null, managementController);

    verify(managementController);

    assertEquals(RoleAuthorizationResourceProvider.class, provider.getClass());
  }

  @Test
  public void testGetUserAuthorizationResourceProvider() {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.UserAuthorization, null, null, managementController);

    verify(managementController);

    assertEquals(UserAuthorizationResourceProvider.class, provider.getClass());
  }

  @Test
  public void testGetClusterKerberosDescriptorResourceProvider() {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor, null, null, managementController);

    verify(managementController);

    assertEquals(ClusterKerberosDescriptorResourceProvider.class, provider.getClass());
  }
}
