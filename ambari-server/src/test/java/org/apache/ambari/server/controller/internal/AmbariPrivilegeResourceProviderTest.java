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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.view.ViewInstanceHandlerList;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.server.view.ViewRegistryTest;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * AmbariPrivilegeResourceProvider tests.
 */
public class AmbariPrivilegeResourceProviderTest {
  private final static PrivilegeDAO privilegeDAO = createStrictMock(PrivilegeDAO.class);
  private final static ClusterDAO clusterDAO = createStrictMock(ClusterDAO.class);
  private final static UserDAO userDAO = createStrictMock(UserDAO.class);
  private final static GroupDAO groupDAO = createStrictMock(GroupDAO.class);
  private final static PrincipalDAO principalDAO = createStrictMock(PrincipalDAO.class);
  private final static PermissionDAO permissionDAO = createStrictMock(PermissionDAO.class);
  private final static ResourceDAO resourceDAO = createStrictMock(ResourceDAO.class);
  private static final ViewDAO viewDAO = createMock(ViewDAO.class);
  private static final ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
  private static final MemberDAO memberDAO = createNiceMock(MemberDAO.class);
  private static final ResourceTypeDAO resourceTypeDAO = createNiceMock(ResourceTypeDAO.class);
  private static final SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
  private static final ViewInstanceHandlerList handlerList = createNiceMock(ViewInstanceHandlerList.class);

  @BeforeClass
  public static void initClass() {
    PrivilegeResourceProvider.init(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO);
    AmbariPrivilegeResourceProvider.init(clusterDAO);
  }

  @Before
  public void resetGlobalMocks() {
    ViewRegistry.initInstance(ViewRegistryTest.getRegistry(viewDAO, viewInstanceDAO, userDAO,
        memberDAO, privilegeDAO, resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, null, null));
    reset(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, clusterDAO, handlerList);
  }

  @Test
  public void testGetResources() throws Exception {

    List<PrivilegeEntity> privilegeEntities = new LinkedList<PrivilegeEntity>();

    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
    ResourceTypeEntity resourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    UserEntity userEntity = createNiceMock(UserEntity.class);
    PrincipalEntity principalEntity = createNiceMock(PrincipalEntity.class);
    PrincipalTypeEntity principalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);

    List<PrincipalEntity> principalEntities = new LinkedList<PrincipalEntity>();
    principalEntities.add(principalEntity);

    List<UserEntity> userEntities = new LinkedList<UserEntity>();
    userEntities.add(userEntity);

    privilegeEntities.add(privilegeEntity);

    expect(privilegeDAO.findAll()).andReturn(privilegeEntities);
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).anyTimes();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    expect(resourceEntity.getId()).andReturn(1L).anyTimes();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    expect(resourceTypeEntity.getId()).andReturn(1).anyTimes();
    expect(principalEntity.getId()).andReturn(1L).anyTimes();
    expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(userEntity.getUserName()).andReturn("joe").anyTimes();
    expect(permissionEntity.getPermissionName()).andReturn("AMBARI.ADMIN").anyTimes();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).anyTimes();
    expect(principalTypeEntity.getName()).andReturn("USER").anyTimes();

    expect(userDAO.findUsersByPrincipal(principalEntities)).andReturn(userEntities);
    expect(clusterDAO.findAll()).andReturn(Collections.<ClusterEntity>emptyList());
    expect(groupDAO.findGroupsByPrincipal(principalEntities)).andReturn(Collections.<GroupEntity>emptyList());

    replay(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, clusterDAO,
        privilegeEntity, resourceEntity, resourceTypeEntity, userEntity, principalEntity,
        permissionEntity, principalTypeEntity);

    PrivilegeResourceProvider provider = new AmbariPrivilegeResourceProvider();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("AMBARI.ADMIN", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_NAME_PROPERTY_ID));
    Assert.assertEquals("joe", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_NAME_PROPERTY_ID));
    Assert.assertEquals("USER", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_TYPE_PROPERTY_ID));

    verify(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, privilegeEntity, resourceEntity,
        userEntity, principalEntity, permissionEntity, principalTypeEntity);
  }

  @Test
  public void testGetResources_allTypes() throws Exception {

    PrivilegeEntity ambariPrivilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity ambariResourceEntity = createNiceMock(ResourceEntity.class);
    ResourceTypeEntity ambariResourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    UserEntity ambariUserEntity = createNiceMock(UserEntity.class);
    PrincipalEntity ambariPrincipalEntity = createNiceMock(PrincipalEntity.class);
    PrincipalTypeEntity ambariPrincipalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    PermissionEntity ambariPermissionEntity = createNiceMock(PermissionEntity.class);
    expect(ambariPrivilegeEntity.getResource()).andReturn(ambariResourceEntity).anyTimes();
    expect(ambariPrivilegeEntity.getId()).andReturn(31).anyTimes();
    expect(ambariPrivilegeEntity.getPrincipal()).andReturn(ambariPrincipalEntity).anyTimes();
    expect(ambariPrivilegeEntity.getPermission()).andReturn(ambariPermissionEntity).anyTimes();
    expect(ambariResourceEntity.getResourceType()).andReturn(ambariResourceTypeEntity).anyTimes();
    expect(ambariResourceTypeEntity.getId()).andReturn(1).anyTimes();
    expect(ambariPrincipalEntity.getId()).andReturn(1L).anyTimes();
    expect(ambariUserEntity.getPrincipal()).andReturn(ambariPrincipalEntity).anyTimes();
    expect(ambariUserEntity.getUserName()).andReturn("joe").anyTimes();
    expect(ambariPermissionEntity.getPermissionName()).andReturn("AMBARI.ADMIN").anyTimes();
    expect(ambariPrincipalEntity.getPrincipalType()).andReturn(ambariPrincipalTypeEntity).anyTimes();
    expect(ambariPrincipalTypeEntity.getName()).andReturn("USER").anyTimes();

    PrivilegeEntity viewPrivilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity viewResourceEntity = createNiceMock(ResourceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ResourceTypeEntity viewResourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    UserEntity viewUserEntity = createNiceMock(UserEntity.class);
    PrincipalEntity viewPrincipalEntity = createNiceMock(PrincipalEntity.class);
    PrincipalTypeEntity viewPrincipalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    PermissionEntity viewPermissionEntity = createNiceMock(PermissionEntity.class);
    expect(viewPrivilegeEntity.getResource()).andReturn(viewResourceEntity).anyTimes();
    expect(viewPrivilegeEntity.getPrincipal()).andReturn(viewPrincipalEntity).anyTimes();
    expect(viewPrivilegeEntity.getPermission()).andReturn(viewPermissionEntity).anyTimes();
    expect(viewPrivilegeEntity.getId()).andReturn(33).anyTimes();
    expect(viewResourceEntity.getResourceType()).andReturn(viewResourceTypeEntity).anyTimes();
    expect(viewResourceTypeEntity.getId()).andReturn(3).anyTimes();
    expect(viewPrincipalEntity.getId()).andReturn(5L).anyTimes();
    expect(viewEntity.getInstances()).andReturn(Arrays.asList(viewInstanceEntity)).anyTimes();
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).anyTimes();
    expect(viewEntity.getCommonName()).andReturn("view").anyTimes();
    expect(viewEntity.getVersion()).andReturn("1.0.1").anyTimes();
    expect(viewEntity.isDeployed()).andReturn(true).anyTimes();
    expect(viewInstanceEntity.getName()).andReturn("inst1").anyTimes();
    expect(viewInstanceEntity.getResource()).andReturn(viewResourceEntity).anyTimes();
    expect(viewUserEntity.getPrincipal()).andReturn(viewPrincipalEntity).anyTimes();
    expect(viewUserEntity.getUserName()).andReturn("bob").anyTimes();
    expect(viewPermissionEntity.getPermissionName()).andReturn("VIEW.USE").anyTimes();
    expect(viewPrincipalEntity.getPrincipalType()).andReturn(viewPrincipalTypeEntity).anyTimes();
    expect(viewPrincipalTypeEntity.getName()).andReturn("USER").anyTimes();

    PrivilegeEntity clusterPrivilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity clusterResourceEntity = createNiceMock(ResourceEntity.class);
    ResourceTypeEntity clusterResourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    UserEntity clusterUserEntity = createNiceMock(UserEntity.class);
    PrincipalEntity clusterPrincipalEntity = createNiceMock(PrincipalEntity.class);
    PrincipalTypeEntity clusterPrincipalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    PermissionEntity clusterPermissionEntity = createNiceMock(PermissionEntity.class);
    ClusterEntity clusterEntity = createNiceMock(ClusterEntity.class);
    expect(clusterPrivilegeEntity.getResource()).andReturn(clusterResourceEntity).anyTimes();
    expect(clusterPrivilegeEntity.getPrincipal()).andReturn(clusterPrincipalEntity).anyTimes();
    expect(clusterPrivilegeEntity.getPermission()).andReturn(clusterPermissionEntity).anyTimes();
    expect(clusterPrivilegeEntity.getId()).andReturn(32).anyTimes();
    expect(clusterResourceEntity.getId()).andReturn(7L).anyTimes();
    expect(clusterResourceEntity.getResourceType()).andReturn(clusterResourceTypeEntity).anyTimes();
    expect(clusterResourceTypeEntity.getId()).andReturn(2).anyTimes();
    expect(clusterPrincipalEntity.getId()).andReturn(8L).anyTimes();
    expect(clusterUserEntity.getPrincipal()).andReturn(clusterPrincipalEntity).anyTimes();
    expect(clusterUserEntity.getUserName()).andReturn("jeff").anyTimes();
    expect(clusterPermissionEntity.getPermissionName()).andReturn("CLUSTER.READ").anyTimes();
    expect(clusterPrincipalEntity.getPrincipalType()).andReturn(clusterPrincipalTypeEntity).anyTimes();
    expect(clusterPrincipalTypeEntity.getName()).andReturn("USER").anyTimes();
    expect(clusterEntity.getResource()).andReturn(clusterResourceEntity).anyTimes();
    expect(clusterEntity.getClusterName()).andReturn("cluster1").anyTimes();

    List<UserEntity> userEntities = new LinkedList<UserEntity>();
    userEntities.add(ambariUserEntity);
    userEntities.add(viewUserEntity);
    userEntities.add(clusterUserEntity);

    List<PrivilegeEntity> privilegeEntities = new LinkedList<PrivilegeEntity>();
    privilegeEntities.add(ambariPrivilegeEntity);
    privilegeEntities.add(viewPrivilegeEntity);
    privilegeEntities.add(clusterPrivilegeEntity);

    List<ClusterEntity> clusterEntities = new LinkedList<ClusterEntity>();
    clusterEntities.add(clusterEntity);

    expect(clusterDAO.findAll()).andReturn(clusterEntities);
    expect(privilegeDAO.findAll()).andReturn(privilegeEntities);
    expect(userDAO.findUsersByPrincipal(anyObject(List.class))).andReturn(userEntities).anyTimes();
    expect(groupDAO.findGroupsByPrincipal(anyObject(List.class))).andReturn(Collections.<GroupEntity>emptyList()).anyTimes();

    replay(privilegeDAO, userDAO, principalDAO, permissionDAO, groupDAO, resourceDAO, clusterDAO, ambariPrivilegeEntity,
        ambariResourceEntity, ambariResourceTypeEntity, ambariUserEntity, ambariPrincipalEntity, ambariPermissionEntity, viewPrivilegeEntity,
        viewResourceEntity, viewResourceTypeEntity, viewUserEntity, viewPrincipalEntity, viewPrincipalTypeEntity, viewPermissionEntity, clusterPrivilegeEntity,
        clusterResourceEntity, clusterResourceTypeEntity, clusterUserEntity, clusterPrincipalEntity, clusterPermissionEntity,clusterPrincipalTypeEntity,
        ambariPrincipalTypeEntity, clusterEntity, viewEntity, viewInstanceEntity);

    ViewRegistry.getInstance().addDefinition(viewEntity);
    PrivilegeResourceProvider provider = new AmbariPrivilegeResourceProvider();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(3, resources.size());

    Map<Object, Resource> resourceMap = new HashMap<Object, Resource>();

    for (Resource resource : resources) {
      resourceMap.put(resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRIVILEGE_ID_PROPERTY_ID), resource);
    }

    Resource resource1 = resourceMap.get(31);

    Assert.assertEquals(5, resource1.getPropertiesMap().get("PrivilegeInfo").size());
    Assert.assertEquals("AMBARI.ADMIN", resource1.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_NAME_PROPERTY_ID));
    Assert.assertEquals("joe", resource1.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_NAME_PROPERTY_ID));
    Assert.assertEquals("USER", resource1.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_TYPE_PROPERTY_ID));
    Assert.assertEquals(31, resource1.getPropertyValue(AmbariPrivilegeResourceProvider.PRIVILEGE_ID_PROPERTY_ID));
    Assert.assertEquals("AMBARI", resource1.getPropertyValue(AmbariPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID));

    Resource resource2 = resourceMap.get(32);

    Assert.assertEquals(6, resource2.getPropertiesMap().get("PrivilegeInfo").size());
    Assert.assertEquals("CLUSTER.READ", resource2.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_NAME_PROPERTY_ID));
    Assert.assertEquals("jeff", resource2.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_NAME_PROPERTY_ID));
    Assert.assertEquals("USER", resource2.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_TYPE_PROPERTY_ID));
    Assert.assertEquals(32, resource2.getPropertyValue(AmbariPrivilegeResourceProvider.PRIVILEGE_ID_PROPERTY_ID));
    Assert.assertEquals("cluster1", resource2.getPropertyValue(ClusterPrivilegeResourceProvider.PRIVILEGE_CLUSTER_NAME_PROPERTY_ID));
    Assert.assertEquals("CLUSTER", resource2.getPropertyValue(AmbariPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID));

    Resource resource3 = resourceMap.get(33);

    Assert.assertEquals(8, resource3.getPropertiesMap().get("PrivilegeInfo").size());
    Assert.assertEquals("VIEW.USE", resource3.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_NAME_PROPERTY_ID));
    Assert.assertEquals("bob", resource3.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_NAME_PROPERTY_ID));
    Assert.assertEquals("USER", resource3.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_TYPE_PROPERTY_ID));
    Assert.assertEquals(33, resource3.getPropertyValue(AmbariPrivilegeResourceProvider.PRIVILEGE_ID_PROPERTY_ID));
    Assert.assertEquals("view", resource3.getPropertyValue(ViewPrivilegeResourceProvider.PRIVILEGE_VIEW_NAME_PROPERTY_ID));
    Assert.assertEquals("1.0.1", resource3.getPropertyValue(ViewPrivilegeResourceProvider.PRIVILEGE_VIEW_VERSION_PROPERTY_ID));
    Assert.assertEquals("inst1", resource3.getPropertyValue(ViewPrivilegeResourceProvider.PRIVILEGE_INSTANCE_NAME_PROPERTY_ID));
    Assert.assertEquals("VIEW", resource3.getPropertyValue(AmbariPrivilegeResourceProvider.PRIVILEGE_TYPE_PROPERTY_ID));

    verify(privilegeDAO, userDAO, principalDAO, permissionDAO, groupDAO, resourceDAO, clusterDAO, ambariPrivilegeEntity,
        ambariResourceEntity, ambariResourceTypeEntity, ambariUserEntity, ambariPrincipalEntity, ambariPermissionEntity, viewPrivilegeEntity,
        viewResourceEntity, viewResourceTypeEntity, viewUserEntity, viewPrincipalEntity, viewPrincipalTypeEntity, viewPermissionEntity, clusterPrivilegeEntity,
        clusterResourceEntity, clusterResourceTypeEntity, clusterUserEntity, clusterPrincipalEntity, clusterPermissionEntity,clusterPrincipalTypeEntity,
        ambariPrincipalTypeEntity, clusterEntity, viewEntity, viewInstanceEntity);
  }

  @Test
  public void testUpdateResources() throws Exception {
    PrivilegeResourceProvider provider = new AmbariPrivilegeResourceProvider();

    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
    ResourceTypeEntity resourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    Request request = createNiceMock(Request.class);
    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);
    PrincipalEntity principalEntity = createNiceMock(PrincipalEntity.class);
    UserEntity userEntity = createNiceMock(UserEntity.class);

    expect(privilegeDAO.findByResourceId(1L)).andReturn(Collections.singletonList(privilegeEntity)).anyTimes();
    privilegeDAO.remove(privilegeEntity);
    EasyMock.expectLastCall().anyTimes();
    expect(request.getProperties()).andReturn(new HashSet<Map<String,Object>>() {
      {
        add(new HashMap<String, Object>() {
          {
           put(PrivilegeResourceProvider.PERMISSION_NAME_PROPERTY_ID, "READ");
           put(PrivilegeResourceProvider.PRINCIPAL_NAME_PROPERTY_ID, "admin");
           put(PrivilegeResourceProvider.PRINCIPAL_TYPE_PROPERTY_ID, "user");
          }
        });
      }
    }).anyTimes();
    expect(clusterDAO.findAll()).andReturn(Collections.<ClusterEntity>emptyList());
    expect(permissionDAO.findPermissionByNameAndType(EasyMock.eq("READ"), EasyMock.<ResourceTypeEntity> anyObject())).andReturn(permissionEntity);
    expect(resourceDAO.findById(EasyMock.anyLong())).andReturn(resourceEntity);
    expect(userDAO.findUserByName("admin")).andReturn(userEntity);
    expect(principalDAO.findById(EasyMock.anyLong())).andReturn(principalEntity);
    expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(principalEntity.getId()).andReturn(2L).anyTimes();
    expect(permissionEntity.getPermissionName()).andReturn("READ").anyTimes();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    expect(resourceTypeEntity.getId()).andReturn(3).anyTimes();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    expect(permissionEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    privilegeDAO.create(EasyMock.<PrivilegeEntity> anyObject());
    EasyMock.expectLastCall().anyTimes();

    replay(privilegeEntity, privilegeDAO, request, permissionDAO, permissionEntity, resourceEntity, resourceDAO,
        principalEntity, principalDAO, userDAO, userEntity, resourceTypeEntity, clusterDAO);

    provider.updateResources(request, null);

    verify(privilegeEntity, privilegeDAO, request, permissionDAO, permissionEntity, resourceEntity, resourceDAO, principalEntity, principalDAO, userDAO, userEntity, resourceTypeEntity);
  }
}
