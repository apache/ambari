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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static org.easymock.EasyMock.*;


/**
 * UserAuthorizationResourceProvider tests.
 */
public class UserAuthorizationResourceProviderTest extends EasyMockSupport {
  private Injector injector;

  @Before
  public void setup() {
    reset();

    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule())
        .with(new AbstractModule() {
          @Override
          protected void configure() {
            AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);

            bind(AmbariManagementController.class).toInstance(managementController);
            bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
            bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
            bind(PermissionDAO.class).toInstance(createMock(PermissionDAO.class));
            bind(ResourceTypeDAO.class).toInstance(createMock(ResourceTypeDAO.class));
          }
        }));
  }


  @Test
  public void testGetResources() throws Exception {

    Resource clusterResource = createMock(Resource.class);
    expect(clusterResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_PERMISSION_NAME_PROPERTY_ID))
        .andReturn("CLUSTER.DO_SOMETHING")
        .anyTimes();
    expect(clusterResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID))
        .andReturn("CLUSTER")
        .anyTimes();
    expect(clusterResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_CLUSTER_NAME_PROPERTY_ID))
        .andReturn("Cluster Name")
        .anyTimes();

    Resource viewResource = createMock(Resource.class);
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_PERMISSION_NAME_PROPERTY_ID))
        .andReturn("VIEW.DO_SOMETHING")
        .anyTimes();
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID))
        .andReturn("VIEW")
        .anyTimes();
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_VIEW_NAME_PROPERTY_ID))
        .andReturn("View Name")
        .anyTimes();
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_VIEW_VERSION_PROPERTY_ID))
        .andReturn("View Version")
        .anyTimes();
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_INSTANCE_NAME_PROPERTY_ID))
        .andReturn("View Instance Name")
        .anyTimes();

    Resource adminResource = createMock(Resource.class);
    expect(adminResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_PERMISSION_NAME_PROPERTY_ID))
        .andReturn("ADMIN.DO_SOMETHING")
        .anyTimes();
    expect(adminResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID))
        .andReturn("ADMIN")
        .anyTimes();
    expect(adminResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_CLUSTER_NAME_PROPERTY_ID))
        .andReturn(null)
        .anyTimes();

    Resource emptyResource = createMock(Resource.class);
    expect(emptyResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_PERMISSION_NAME_PROPERTY_ID))
        .andReturn("EMPTY.DO_SOMETHING")
        .anyTimes();
    expect(emptyResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID))
        .andReturn("ADMIN")
        .anyTimes();
    expect(emptyResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_CLUSTER_NAME_PROPERTY_ID))
        .andReturn(null)
        .anyTimes();

    Resource nullResource = createMock(Resource.class);
    expect(nullResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_PERMISSION_NAME_PROPERTY_ID))
        .andReturn("NULL.DO_SOMETHING")
        .anyTimes();
    expect(nullResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID))
        .andReturn("ADMIN")
        .anyTimes();
    expect(nullResource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_CLUSTER_NAME_PROPERTY_ID))
        .andReturn(null)
        .anyTimes();

    Set<Resource> userPrivilegeResources = new HashSet<Resource>();
    userPrivilegeResources.add(clusterResource);
    userPrivilegeResources.add(viewResource);
    userPrivilegeResources.add(adminResource);
    userPrivilegeResources.add(emptyResource);
    userPrivilegeResources.add(nullResource);

    ResourceProvider userPrivilegeProvider = createMock(ResourceProvider.class);
    expect(userPrivilegeProvider.getResources(anyObject(Request.class), anyObject(Predicate.class)))
        .andReturn(userPrivilegeResources);

    ClusterController clusterController = createMock(ClusterController.class);
    expect(clusterController.ensureResourceProvider(Resource.Type.UserPrivilege))
        .andReturn(userPrivilegeProvider)
        .anyTimes();

    ResourceTypeEntity clusterResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(clusterResourceTypeEntity.getId()).andReturn(1).anyTimes();

    ResourceTypeEntity viewResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(viewResourceTypeEntity.getId()).andReturn(2).anyTimes();

    ResourceTypeEntity adminResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(adminResourceTypeEntity.getId()).andReturn(3).anyTimes();

    ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    expect(resourceTypeDAO.findByName("CLUSTER")).andReturn(clusterResourceTypeEntity).anyTimes();
    expect(resourceTypeDAO.findByName("VIEW")).andReturn(viewResourceTypeEntity).anyTimes();
    expect(resourceTypeDAO.findByName("ADMIN")).andReturn(adminResourceTypeEntity).anyTimes();

    RoleAuthorizationEntity clusterRoleAuthorizationEntity = createMock(RoleAuthorizationEntity.class);
    expect(clusterRoleAuthorizationEntity.getAuthorizationId()).andReturn("CLUSTER.DO_SOMETHING").anyTimes();
    expect(clusterRoleAuthorizationEntity.getAuthorizationName()).andReturn("CLUSTER DO_SOMETHING").anyTimes();

    RoleAuthorizationEntity viewRoleAuthorizationEntity = createMock(RoleAuthorizationEntity.class);
    expect(viewRoleAuthorizationEntity.getAuthorizationId()).andReturn("VIEW.DO_SOMETHING").anyTimes();
    expect(viewRoleAuthorizationEntity.getAuthorizationName()).andReturn("VIEW DO_SOMETHING").anyTimes();

    RoleAuthorizationEntity adminRoleAuthorizationEntity = createMock(RoleAuthorizationEntity.class);
    expect(adminRoleAuthorizationEntity.getAuthorizationId()).andReturn("ADMIN.DO_SOMETHING").anyTimes();
    expect(adminRoleAuthorizationEntity.getAuthorizationName()).andReturn("ADMIN DO_SOMETHING").anyTimes();

    Collection<RoleAuthorizationEntity> clusterPermissionAuthorizations = Collections.singleton(clusterRoleAuthorizationEntity);
    Collection<RoleAuthorizationEntity> viewPermissionAuthorizations = Collections.singleton(viewRoleAuthorizationEntity);
    Collection<RoleAuthorizationEntity> adminPermissionAuthorizations = Collections.singleton(adminRoleAuthorizationEntity);

    PermissionEntity clusterPermissionEntity = createMock(PermissionEntity.class);
    expect(clusterPermissionEntity.getAuthorizations())
        .andReturn(clusterPermissionAuthorizations)
        .anyTimes();

    PermissionEntity viewPermissionEntity = createMock(PermissionEntity.class);
    expect(viewPermissionEntity.getAuthorizations())
        .andReturn(viewPermissionAuthorizations)
        .anyTimes();

    PermissionEntity adminPermissionEntity = createMock(PermissionEntity.class);
    expect(adminPermissionEntity.getAuthorizations())
        .andReturn(adminPermissionAuthorizations)
        .anyTimes();

    PermissionEntity emptyPermissionEntity = createMock(PermissionEntity.class);
    expect(emptyPermissionEntity.getAuthorizations())
        .andReturn(Collections.<RoleAuthorizationEntity>emptyList())
        .anyTimes();

    PermissionEntity nullPermissionEntity = createMock(PermissionEntity.class);
    expect(nullPermissionEntity.getAuthorizations())
        .andReturn(null)
        .anyTimes();

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.DO_SOMETHING", clusterResourceTypeEntity))
        .andReturn(clusterPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("VIEW.DO_SOMETHING", viewResourceTypeEntity))
        .andReturn(viewPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("ADMIN.DO_SOMETHING", adminResourceTypeEntity))
        .andReturn(adminPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("EMPTY.DO_SOMETHING", adminResourceTypeEntity))
        .andReturn(emptyPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("NULL.DO_SOMETHING", adminResourceTypeEntity))
        .andReturn(nullPermissionEntity)
        .anyTimes();

    replayAll();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    UserAuthorizationResourceProvider provider = new UserAuthorizationResourceProvider(managementController);
    setClusterController(provider, clusterController);

    Predicate predicate = new PredicateBuilder()
        .property(UserAuthorizationResourceProvider.USERNAME_PROPERTY_ID).equals("jdoe")
        .toPredicate();

    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);

    Assert.assertEquals(3, resources.size());

    LinkedList<String> expectedIds = new LinkedList<String>();
    expectedIds.add("CLUSTER.DO_SOMETHING");
    expectedIds.add("VIEW.DO_SOMETHING");
    expectedIds.add("ADMIN.DO_SOMETHING");

    for (Resource resource : resources) {
      String authorizationId = (String) resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_ID_PROPERTY_ID);

      switch (authorizationId) {
        case "CLUSTER.DO_SOMETHING":
          Assert.assertEquals("CLUSTER DO_SOMETHING", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
          Assert.assertEquals("CLUSTER", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID));
          Assert.assertEquals("Cluster Name", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_VERSION_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID));
          break;
        case "VIEW.DO_SOMETHING":
          Assert.assertEquals("VIEW DO_SOMETHING", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
          Assert.assertEquals("VIEW", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID));
          Assert.assertEquals("View Name", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_NAME_PROPERTY_ID));
          Assert.assertEquals("View Version", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_VERSION_PROPERTY_ID));
          Assert.assertEquals("View Instance Name", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID));
          break;
        case "ADMIN.DO_SOMETHING":
          Assert.assertEquals("ADMIN DO_SOMETHING", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
          Assert.assertEquals("ADMIN", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_VERSION_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID));
          break;
      }

      expectedIds.remove();
    }

    Assert.assertEquals(0, expectedIds.size());

    verifyAll();
  }

  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testUpdateResources() throws Exception {
    replayAll();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    UserAuthorizationResourceProvider provider = new UserAuthorizationResourceProvider(managementController);
    provider.updateResources(createNiceMock(Request.class), null);
  }

  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testDeleteResources() throws Exception {
    replayAll();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    UserAuthorizationResourceProvider provider = new UserAuthorizationResourceProvider(managementController);
    provider.deleteResources(null);
  }


  private void setClusterController(UserAuthorizationResourceProvider provider, ClusterController clusterController) throws Exception {
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("clusterController");
    f.setAccessible(true);
    f.set(provider, clusterController);
  }

}
