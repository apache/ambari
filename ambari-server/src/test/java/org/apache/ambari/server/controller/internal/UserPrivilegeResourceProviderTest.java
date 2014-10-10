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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.easymock.EasyMock;
import org.junit.Test;

/**
 * UserPrivilegeResourceProvider tests.
 */
public class UserPrivilegeResourceProviderTest {

  @Test(expected = SystemException.class)
  public void testCreateResources() throws Exception {
    final UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    resourceProvider.createResources(EasyMock.createNiceMock(Request.class));
  }

  @SuppressWarnings("serial")
  @Test
  public void testGetResources() throws Exception {
    final UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    final UserDAO userDAO = EasyMock.createNiceMock(UserDAO.class);
    final GroupDAO groupDAO = EasyMock.createNiceMock(GroupDAO.class);
    final ClusterDAO clusterDAO = EasyMock.createNiceMock(ClusterDAO.class);
    final ViewInstanceDAO viewInstanceDAO = EasyMock.createNiceMock(ViewInstanceDAO.class);
    final UserEntity userEntity = EasyMock.createNiceMock(UserEntity.class);
    final PrincipalEntity principalEntity = EasyMock.createNiceMock(PrincipalEntity.class);
    final PrivilegeEntity privilegeEntity = EasyMock.createNiceMock(PrivilegeEntity.class);
    final PermissionEntity permissionEntity = EasyMock.createNiceMock(PermissionEntity.class);
    final PrincipalTypeEntity principalTypeEntity = EasyMock.createNiceMock(PrincipalTypeEntity.class);
    final ResourceEntity resourceEntity = EasyMock.createNiceMock(ResourceEntity.class);
    final ResourceTypeEntity resourceTypeEntity = EasyMock.createNiceMock(ResourceTypeEntity.class);

    EasyMock.expect(userDAO.findLocalUserByName("user")).andReturn(userEntity).anyTimes();
    EasyMock.expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    EasyMock.expect(userEntity.getMemberEntities()).andReturn(Collections.<MemberEntity> emptySet()).anyTimes();
    EasyMock.expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    EasyMock.expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    EasyMock.expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).anyTimes();
    EasyMock.expect(principalTypeEntity.getName()).andReturn(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME).anyTimes();
    EasyMock.expect(principalEntity.getPrivileges()).andReturn(new HashSet<PrivilegeEntity>() {
      {
        add(privilegeEntity);
      }
    }).anyTimes();
    EasyMock.expect(userDAO.findUserByPrincipal(EasyMock.<PrincipalEntity>anyObject())).andReturn(userEntity).anyTimes();
    EasyMock.expect(userEntity.getUserName()).andReturn("user").anyTimes();
    EasyMock.expect(privilegeEntity.getResource()).andReturn(resourceEntity).anyTimes();
    EasyMock.expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    EasyMock.expect(resourceTypeEntity.getId()).andReturn(ResourceTypeEntity.AMBARI_RESOURCE_TYPE);

    EasyMock.replay(userDAO, userEntity, principalEntity, privilegeEntity, permissionEntity, principalTypeEntity, resourceEntity, resourceTypeEntity);

    UserPrivilegeResourceProvider.init(userDAO, clusterDAO, groupDAO, viewInstanceDAO);

    final Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(UserPrivilegeResourceProvider.PRIVILEGE_USER_NAME_PROPERTY_ID);
    //propertyIds.add(UserResourceProvider.USER_PASSWORD_PROPERTY_ID);

    final Predicate predicate = new PredicateBuilder().property(UserPrivilegeResourceProvider.PRIVILEGE_USER_NAME_PROPERTY_ID).equals("user").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = resourceProvider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String userName = (String) resource.getPropertyValue(UserPrivilegeResourceProvider.PRIVILEGE_USER_NAME_PROPERTY_ID);
      Assert.assertEquals("user", userName);
    }

    EasyMock.verify(userDAO, userEntity, principalEntity, privilegeEntity, permissionEntity, principalTypeEntity, resourceEntity, resourceTypeEntity);
  }

  @Test(expected = SystemException.class)
  public void testUpdateResources() throws Exception {
    final UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    resourceProvider.updateResources(EasyMock.createNiceMock(Request.class), EasyMock.createNiceMock(Predicate.class));
  }

  @Test(expected = SystemException.class)
  public void testDeleteResources() throws Exception {
    final UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    resourceProvider.deleteResources(EasyMock.createNiceMock(Predicate.class));
  }
}
