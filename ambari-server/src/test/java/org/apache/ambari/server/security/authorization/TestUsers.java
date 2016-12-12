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
package org.apache.ambari.server.security.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

public class TestUsers {
  private Injector injector;

  @Inject
  protected Users users;
  @Inject
  protected UserDAO userDAO;
  @Inject
  protected GroupDAO groupDAO;
  @Inject
  protected PermissionDAO permissionDAO;
  @Inject
  protected ResourceDAO resourceDAO;
  @Inject
  protected ResourceTypeDAO resourceTypeDAO;
  @Inject
  protected PrincipalTypeDAO principalTypeDAO;
  @Inject
  protected PrincipalDAO principalDAO;
  @Inject
  protected PasswordEncoder passwordEncoder;

  @Before
  public void setup() throws AmbariException {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    Authentication auth = new UsernamePasswordAuthenticationToken("admin", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    // create admin permission
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceType.AMBARI.getId());
    resourceTypeEntity.setName(ResourceType.AMBARI.name());
    resourceTypeDAO.create(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(ResourceEntity.AMBARI_RESOURCE_ID);
    resourceEntity.setResourceType(resourceTypeEntity);
    resourceDAO.create(resourceEntity);

    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();
    principalTypeEntity.setName("ROLE");
    principalTypeEntity = principalTypeDAO.merge(principalTypeEntity);

    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalEntity = principalDAO.merge(principalEntity);

    PermissionEntity adminPermissionEntity = new PermissionEntity();
    adminPermissionEntity.setId(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION);
    adminPermissionEntity.setPermissionName(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME);
    adminPermissionEntity.setPrincipal(principalEntity);
    adminPermissionEntity.setResourceType(resourceTypeEntity);
    permissionDAO.create(adminPermissionEntity);
  }

  @After
  public void tearDown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }


  @Test
  public void testIsUserCanBeRemoved() throws Exception {
    users.createUser("admin", "admin", UserType.LOCAL, true, true);
    users.createUser("admin222", "admin222", UserType.LOCAL, true, true);

    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));
    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin222")));

    users.removeUser(users.getAnyUser("admin222"));

    Assert.assertFalse(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));
    users.createUser("user", "user");
    Assert.assertFalse(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));

    users.createUser("admin333", "admin333", UserType.LOCAL, true, true);
    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));
    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin333")));
  }

  @Test
  public void testModifyPassword_UserByAdmin() throws Exception {
    users.createUser("admin", "admin", UserType.LOCAL, true, true);
    users.createUser("user", "user");

    UserEntity userEntity = userDAO.findUserByName("user");

    assertNotSame("user", userEntity.getUserPassword());
    assertTrue(passwordEncoder.matches("user", userEntity.getUserPassword()));

    users.modifyPassword("user", "admin", "user_new_password");
    assertTrue(passwordEncoder.matches("user_new_password", userDAO.findUserByName("user").getUserPassword()));
  }

  @Test
  public void testRevokeAdminPrivilege() throws Exception {
    users.createUser("old_admin", "old_admin", UserType.LOCAL, true, true);

    final User admin = users.getAnyUser("old_admin");
    users.revokeAdminPrivilege(admin.getUserId());

    Assert.assertFalse(users.getAnyUser("old_admin").isAdmin());
  }

  @Test
  public void testGrantAdminPrivilege() throws Exception {
    users.createUser("user", "user");

    final User user = users.getAnyUser("user");
    users.grantAdminPrivilege(user.getUserId());

    Assert.assertTrue(users.getAnyUser("user").isAdmin());
  }

  @Test
  public void testCreateGetRemoveUser() throws Exception {
    users.createUser("user1", "user1");
    users.createUser("user", "user", UserType.LOCAL, false, false);
    users.createUser("user_ldap", "user_ldap", UserType.LDAP, true, true);
    User createdUser = users.getUser("user", UserType.LOCAL);
    User createdUser1 = users.getAnyUser("user1");
    User createdLdapUser = users.getUser("user_ldap", UserType.LDAP);

    Assert.assertEquals("user1", createdUser1.getUserName());
    Assert.assertEquals(true, createdUser1.isActive());
    Assert.assertEquals(false, createdUser1.isLdapUser());
    Assert.assertEquals(false, createdUser1.isAdmin());

    Assert.assertEquals("user", createdUser.getUserName());
    Assert.assertEquals(false, createdUser.isActive());
    Assert.assertEquals(false, createdUser.isLdapUser());
    Assert.assertEquals(false, createdUser.isAdmin());

    Assert.assertEquals("user_ldap", createdLdapUser.getUserName());
    Assert.assertEquals(true, createdLdapUser.isActive());
    Assert.assertEquals(true, createdLdapUser.isLdapUser());
    Assert.assertEquals(true, createdLdapUser.isAdmin());

    assertEquals("user", users.getAnyUser("user").getUserName());
    assertEquals("user_ldap", users.getAnyUser("user_ldap").getUserName());
    Assert.assertNull(users.getAnyUser("non_existing"));

    // create duplicate user
    try {
      users.createUser("user1", "user1");
      Assert.fail("It shouldn't be possible to create duplicate user");
    } catch (AmbariException e) {
    }

    try {
      users.createUser("USER1", "user1");
      Assert.fail("It shouldn't be possible to create duplicate user");
    } catch (AmbariException e) {
    }

    // test get all users
    List<User> userList = users.getAllUsers();

    Assert.assertEquals(3, userList.size());

    // check get any user case insensitive
    assertEquals("user", users.getAnyUser("USER").getUserName());
    assertEquals("user_ldap", users.getAnyUser("USER_LDAP").getUserName());
    Assert.assertNull(users.getAnyUser("non_existing"));

    // get user by id
    User userById = users.getUser(createdUser.getUserId());

    assertNotNull(userById);
    assertEquals(createdUser.getUserId(), userById.getUserId());

    // get user by invalid id
    User userByInvalidId = users.getUser(-1);

    assertNull(userByInvalidId);

    // get user if unique
    Assert.assertNotNull(users.getUserIfUnique("user"));

    users.createUser("user", "user", UserType.LDAP, true, false);

    Assert.assertNull(users.getUserIfUnique("user"));

    //remove user
    Assert.assertEquals(4, users.getAllUsers().size());

    users.removeUser(users.getAnyUser("user1"));

    Assert.assertNull(users.getAnyUser("user1"));
    Assert.assertEquals(3, users.getAllUsers().size());
  }

  @Test
  public void testSetUserActive() throws Exception {
    users.createUser("user", "user");

    users.setUserActive("user", false);
    Assert.assertEquals(false, users.getAnyUser("user").isActive());
    users.setUserActive("user", true);
    Assert.assertEquals(true, users.getAnyUser("user").isActive());

    try {
      users.setUserActive("fake user", true);
      Assert.fail("It shouldn't be possible to call setUserActive() on non-existing user");
    } catch (Exception ex) {
    }
  }

  @Test
  public void testSetUserLdap() throws Exception {
    users.createUser("user", "user");
    users.createUser("user_ldap", "user_ldap", UserType.LDAP, true, false);

    users.setUserLdap("user");
    Assert.assertEquals(true, users.getAnyUser("user").isLdapUser());

    try {
      users.setUserLdap("fake user");
      Assert.fail("It shouldn't be possible to call setUserLdap() on non-existing user");
    } catch (AmbariException ex) {
    }
  }

  @Test
  public void testSetGroupLdap() throws Exception {
    users.createGroup("group", GroupType.LOCAL);

    users.setGroupLdap("group");
    Assert.assertNotNull(users.getGroup("group"));
    Assert.assertTrue(users.getGroup("group").isLdapGroup());

    try {
      users.setGroupLdap("fake group");
      Assert.fail("It shouldn't be possible to call setGroupLdap() on non-existing group");
    } catch (AmbariException ex) {
    }
  }

  @Test
  public void testCreateGetRemoveGroup() throws Exception {
    final String groupName = "engineering1";
    final String groupName2 = "engineering2";
    users.createGroup(groupName, GroupType.LOCAL);
    users.createGroup(groupName2, GroupType.LOCAL);

    final Group group = users.getGroup(groupName);
    assertNotNull(group);
    assertEquals(false, group.isLdapGroup());
    assertEquals(groupName, group.getGroupName());

    assertNotNull(groupDAO.findGroupByName(groupName));

    // get all groups
    final List<Group> groupList = users.getAllGroups();

    assertEquals(2, groupList.size());
    assertEquals(2, groupDAO.findAll().size());

    // remove group
    users.removeGroup(group);
    assertNull(users.getGroup(group.getGroupName()));
    assertEquals(1, users.getAllGroups().size());
  }

  @Test
  public void testMembers() throws Exception {
    final String groupName = "engineering";
    final String groupName2 = "engineering2";
    users.createGroup(groupName, GroupType.LOCAL);
    users.createGroup(groupName2, GroupType.LOCAL);
    users.createUser("user1", "user1");
    users.createUser("user2", "user2");
    users.createUser("user3", "user3");
    users.addMemberToGroup(groupName, "user1");
    users.addMemberToGroup(groupName, "user2");
    assertEquals(2, users.getAllMembers(groupName).size());
    assertEquals(0, users.getAllMembers(groupName2).size());

    try {
      users.getAllMembers("non existing");
      Assert.fail("It shouldn't be possible to call getAllMembers() on non-existing group");
    } catch (Exception ex) {
    }

    // get members from not unexisting group
    assertEquals(users.getGroupMembers("unexisting"), null);

    // remove member from group
    users.removeMemberFromGroup(groupName, "user1");
    assertEquals(1, groupDAO.findGroupByName(groupName).getMemberEntities().size());
    assertEquals("user2", groupDAO.findGroupByName(groupName).getMemberEntities().iterator().next().getUser().getUserName());
  }

  @Test
  public void testModifyPassword_UserByHimselfPasswordOk() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("user", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    users.createUser("user", "user");

    UserEntity userEntity = userDAO.findUserByName("user");

    assertNotSame("user", userEntity.getUserPassword());
    assertTrue(passwordEncoder.matches("user", userEntity.getUserPassword()));

    users.modifyPassword("user", "user", "user_new_password");

    assertTrue(passwordEncoder.matches("user_new_password", userDAO.findUserByName("user").getUserPassword()));
  }

  @Test
  public void testModifyPassword_UserByHimselfPasswordNotOk() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("user", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    users.createUser("user", "user");

    UserEntity userEntity = userDAO.findUserByName("user");

    assertNotSame("user", userEntity.getUserPassword());
    assertTrue(passwordEncoder.matches("user", userEntity.getUserPassword()));

    try {
      users.modifyPassword("user", "admin", "user_new_password");
      Assert.fail("Exception should be thrown here as password is incorrect");
    } catch (AmbariException ex) {
    }
  }

  @Test
  public void testModifyPassword_UserByNonAdmin() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("user2", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    users.createUser("user", "user");
    users.createUser("user2", "user2");

    UserEntity userEntity = userDAO.findUserByName("user");

    assertNotSame("user", userEntity.getUserPassword());
    assertTrue(passwordEncoder.matches("user", userEntity.getUserPassword()));

    try {
      users.modifyPassword("user", "user2", "user_new_password");
      Assert.fail("Exception should be thrown here as user2 can't change password of user");
    } catch (AmbariException ex) {
    }
  }

  @Test
  @Ignore // TODO @Transactional annotation breaks this test
  public void testCreateUserDefaultParams() throws Exception {
    final Users spy = Mockito.spy(users);
    spy.createUser("user", "user");
    Mockito.verify(spy).createUser("user", "user", UserType.LOCAL, true, false);
  }


}
