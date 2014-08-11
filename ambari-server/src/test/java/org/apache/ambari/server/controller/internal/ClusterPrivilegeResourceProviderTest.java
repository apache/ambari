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

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * ClusterPrivilegeResourceProvider tests.
 */
public class ClusterPrivilegeResourceProviderTest {
  private final static PrivilegeDAO privilegeDAO = createStrictMock(PrivilegeDAO.class);
  private final static UserDAO userDAO = createStrictMock(UserDAO.class);
  private final static GroupDAO groupDAO = createStrictMock(GroupDAO.class);
  private final static PrincipalDAO principalDAO = createStrictMock(PrincipalDAO.class);
  private final static PermissionDAO permissionDAO = createStrictMock(PermissionDAO.class);
  private final static ResourceDAO resourceDAO = createStrictMock(ResourceDAO.class);
  private final static ClusterDAO clusterDAO = createStrictMock(ClusterDAO.class);

  @BeforeClass
  public static void initClass() {
    PrivilegeResourceProvider.init(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO);
    ClusterPrivilegeResourceProvider.init(clusterDAO);
  }

  @Before
  public void resetGlobalMocks() {
    reset(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO);
  }

  @Test
  public void testGetResources() throws Exception {

    List<PrivilegeEntity> privilegeEntities = new LinkedList<PrivilegeEntity>();

    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    ClusterEntity clusterEntity = createNiceMock(ClusterEntity.class);
    ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
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
    expect(resourceEntity.getId()).andReturn(20L).anyTimes();
    expect(principalEntity.getId()).andReturn(20L).anyTimes();
    expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(userEntity.getUserName()).andReturn("joe").anyTimes();
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.OPERATE").anyTimes();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).anyTimes();
    expect(principalTypeEntity.getName()).andReturn("USER").anyTimes();
    expect(clusterEntity.getResource()).andReturn(resourceEntity);

    List<ClusterEntity> clusterEntities = new LinkedList<ClusterEntity>();
    clusterEntities.add(clusterEntity);
    expect(clusterDAO.findAll()).andReturn(clusterEntities);

    expect(userDAO.findUsersByPrincipal(principalEntities)).andReturn(userEntities);
    expect(groupDAO.findGroupsByPrincipal(principalEntities)).andReturn(Collections.<GroupEntity>emptyList());

    expect(permissionDAO.findById(2)).andReturn(permissionEntity);
    expect(permissionDAO.findById(3)).andReturn(permissionEntity);

    replay(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, clusterDAO, privilegeEntity,
        clusterEntity, resourceEntity, userEntity, principalEntity, permissionEntity, principalTypeEntity);


    PrivilegeResourceProvider provider = new ClusterPrivilegeResourceProvider();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("CLUSTER.OPERATE", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_NAME_PROPERTY_ID));
    Assert.assertEquals("joe", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_NAME_PROPERTY_ID));
    Assert.assertEquals("USER", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_TYPE_PROPERTY_ID));

    verify(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, clusterDAO, privilegeEntity,
        resourceEntity, clusterEntity, userEntity, principalEntity, permissionEntity, principalTypeEntity);
    reset(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, clusterDAO);
  }

  @Test
  public void testUpdateResources() throws Exception {
    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);
    Request request = createNiceMock(Request.class);

    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.OPERATE").anyTimes();
    expect(permissionDAO.findById(2)).andReturn(permissionEntity);
    expect(permissionDAO.findById(3)).andReturn(permissionEntity);

    replay(permissionDAO, permissionEntity, request);

    PrivilegeResourceProvider provider = new ClusterPrivilegeResourceProvider();
    try {
      provider.updateResources(request, null);
    } catch (Exception ex) {
      // omit the exception, this method is from abstract class and tested in
      // AmbariPrivilegeResourceProvider#testUpdateResources
      // just check that permissions are okay
    }

    verify(permissionDAO, permissionEntity, request);
    reset(permissionDAO);
  }
}

