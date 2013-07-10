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
package org.apache.ambari.server.security.authorization;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.RoleDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.RoleEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ldap.core.DirContextOperations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestAmbariLdapAuthoritiesPopulator extends EasyMockSupport {

  AuthorizationHelper helper = new AuthorizationHelper();
  Configuration configuration = createMock(Configuration.class);
  UserDAO userDAO = createMock(UserDAO.class);
  RoleDAO roleDAO = createMock(RoleDAO.class);
  LdapServerProperties ldapServerProperties = createMock(LdapServerProperties.class);
  DirContextOperations userData = createMock(DirContextOperations.class);
  UserEntity userEntity = createMock(UserEntity.class);

  Set<RoleEntity> roleSetStub = new HashSet<RoleEntity>();
  String username = "user";
  String adminRole = "role";
  String userRole = "userRole";
  Map<String, String> configs = new HashMap<String, String>();

  public TestAmbariLdapAuthoritiesPopulator() {
    configs.put(Configuration.ADMIN_ROLE_NAME_KEY, adminRole);
    configs.put(Configuration.USER_ROLE_NAME_KEY, userRole);

  }

  @Before
  public void setUp() throws Exception {
    resetAll();

    expect(configuration.getConfigsMap()).andReturn(configs).anyTimes();
  }

  @Test
  public void testGetGrantedAuthorities_mappingDisabled() throws Exception {
    String username = "user";

    AmbariLdapAuthoritiesPopulator populator = createMockBuilder(AmbariLdapAuthoritiesPopulator.class)
        .addMockedMethod("createLdapUser")
        .withConstructor(
            configuration, helper, userDAO, roleDAO
        ).createMock();


    expect(ldapServerProperties.isGroupMappingEnabled()).andReturn(false).atLeastOnce();

    expect(configuration.getLdapServerProperties()).andReturn(ldapServerProperties).atLeastOnce();

    expect(userEntity.getRoleEntities()).andReturn(roleSetStub);

    populator.createLdapUser(username);
    expectLastCall();

    expect(userDAO.findLdapUserByName(username)).andReturn(null).andReturn(userEntity);
    replayAll();


    populator.getGrantedAuthorities(userData, username);

    verifyAll();

  }

  @Test
  public void testGetGrantedAuthorities_mappingEnabled() throws Exception {


    AmbariLdapAuthoritiesPopulator populator = createMockBuilder(AmbariLdapAuthoritiesPopulator.class)
        .addMockedMethod("createLdapUser")
        .addMockedMethod("addRole")
        .addMockedMethod("removeRole")
        .withConstructor(
            configuration, helper, userDAO, roleDAO
        ).createMock();

    expect(userData.getObjectAttribute("ambari_admin")).andReturn(Boolean.TRUE).andReturn(Boolean.FALSE);

    expect(ldapServerProperties.isGroupMappingEnabled()).andReturn(true).atLeastOnce();

    expect(configuration.getLdapServerProperties()).andReturn(ldapServerProperties).atLeastOnce();



    expect(userEntity.getRoleEntities()).andReturn(roleSetStub).times(2);

    expect(userDAO.findLdapUserByName(username)).andReturn(null).andReturn(userEntity).times(2);

    populator.createLdapUser(username);
    expectLastCall();
    populator.addRole(userEntity, adminRole);
    expectLastCall();
    populator.removeRole(userEntity, adminRole);
    expectLastCall();

    replayAll();

    //test with admin user
    populator.getGrantedAuthorities(userData, username);
    //test with non-admin
    populator.getGrantedAuthorities(userData, username);

    verifyAll();
  }

  @Test
  public void testCreateLdapUser() throws Exception {
    AmbariLdapAuthoritiesPopulator populator = createMockBuilder(AmbariLdapAuthoritiesPopulator.class)
        .addMockedMethod("addRole")
        .addMockedMethod("removeRole")
        .withConstructor(
            configuration, helper, userDAO, roleDAO
        ).createMock();

    Capture<UserEntity> createEntity = new Capture<UserEntity>();
    Capture<UserEntity> addRoleEntity = new Capture<UserEntity>();

    userDAO.create(capture(createEntity));
    expectLastCall();

    populator.addRole(capture(addRoleEntity), eq(userRole));
    expectLastCall();

    replayAll();

    populator.createLdapUser(username);

    verifyAll();

    UserEntity capturedCreateEntity = createEntity.getValue();
    UserEntity capturedAddRoleEntity = addRoleEntity.getValue();

    assertTrue(capturedCreateEntity.getLdapUser());
    assertEquals(username, capturedCreateEntity.getUserName());

    assertEquals(capturedCreateEntity,capturedAddRoleEntity);

  }


  @Test
  public void testAddRole() throws Exception {
    AmbariLdapAuthoritiesPopulator populator =
        new AmbariLdapAuthoritiesPopulator(configuration, helper, userDAO, roleDAO);

    RoleEntity roleEntity = createMock(RoleEntity.class);
    Set<UserEntity> userEntities = createMock(Set.class);
    Set<RoleEntity> roleEntities = createMock(Set.class);

    Capture<RoleEntity> createdRole = new Capture<RoleEntity>();

    expect(roleDAO.findByName(adminRole)).andReturn(null).andReturn(roleEntity);
    expect(roleDAO.findByName(adminRole)).andReturn(roleEntity);

    roleDAO.create(capture(createdRole));
    expectLastCall();

    expect(userEntity.getUserName()).andReturn(username).anyTimes();
    expect(userEntity.getRoleEntities()).andReturn(roleEntities).anyTimes();

    expect(roleEntity.getUserEntities()).andReturn(userEntities).anyTimes();

    expect(roleEntities.contains(roleEntity)).andReturn(false);
    expect(roleEntities.contains(roleEntity)).andReturn(true);

    expect(userEntities.add(userEntity)).andReturn(true);
    expect(roleEntities.add(roleEntity)).andReturn(true);

    userDAO.merge(userEntity);
    expectLastCall().andReturn(userEntity);
    roleDAO.merge(roleEntity);
    expectLastCall().andReturn(roleEntity);

    expect(userDAO.findLdapUserByName(username)).andReturn(null).andReturn(userEntity);
    expect(userDAO.findLdapUserByName(username)).andReturn(userEntity);

    userDAO.create(userEntity);
    expectLastCall();

    replayAll();

    populator.addRole(userEntity, adminRole);
    populator.addRole(userEntity, adminRole);

    verifyAll();

    assertEquals(adminRole, createdRole.getValue().getRoleName());

  }


  @Test
  public void testRemoveRole() throws Exception {
    int userId = 123;

    AmbariLdapAuthoritiesPopulator populator =
        new AmbariLdapAuthoritiesPopulator(configuration, helper, userDAO, roleDAO);

    RoleEntity roleEntity = createMock(RoleEntity.class);
    Set<UserEntity> userEntities = createMock(Set.class);
    Set<RoleEntity> roleEntities = createMock(Set.class);

    expect(userEntity.getUserId()).andReturn(userId);

    expect(userDAO.findByPK(userId)).andReturn(userEntity);

    expect(roleDAO.findByName(adminRole)).andReturn(roleEntity);

    expect(userEntity.getRoleEntities()).andReturn(roleEntities);

    expect(roleEntities.contains(roleEntity)).andReturn(true);

    expect(userEntity.getUserName()).andReturn(username);

    expect(userEntity.getRoleEntities()).andReturn(roleEntities);
    expect(roleEntity.getUserEntities()).andReturn(userEntities);

    expect(userEntities.remove(userEntity)).andReturn(true);
    expect(roleEntities.remove(roleEntity)).andReturn(true);

    expect(userDAO.merge(userEntity)).andReturn(userEntity);
    expect(roleDAO.merge(roleEntity)).andReturn(roleEntity);

    replayAll();

    populator.removeRole(userEntity, adminRole);

    verifyAll();
  }
}
