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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
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
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

public class TestUsers {
  private Injector injector;

  @Inject
  protected Users users;
  @Inject
  protected UserDAO userDAO;
  @Inject
  protected GroupDAO groupDAO;
  @Inject
  protected MemberDAO memberDAO;
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
  @Inject
  Provider<EntityManager> entityManagerProvider;
  private Properties properties;

  @Before
  public void setup() throws AmbariException {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    properties = module.getProperties();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    Authentication auth = new UsernamePasswordAuthenticationToken("admin", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    // create admin permission
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceTypeEntity.AMBARI_RESOURCE_TYPE);
    resourceTypeEntity.setName(ResourceTypeEntity.AMBARI_RESOURCE_TYPE_NAME);
    resourceTypeDAO.create(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(ResourceEntity.AMBARI_RESOURCE_ID);
    resourceEntity.setResourceType(resourceTypeEntity);
    resourceDAO.create(resourceEntity);

    PermissionEntity adminPermissionEntity = new PermissionEntity();
    adminPermissionEntity.setId(PermissionEntity.AMBARI_ADMIN_PERMISSION);
    adminPermissionEntity.setPermissionName(PermissionEntity.AMBARI_ADMIN_PERMISSION_NAME);
    adminPermissionEntity.setResourceType(resourceTypeEntity);
    permissionDAO.create(adminPermissionEntity);
  }

  @After
  public void tearDown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testGetAllUsers() throws Exception {
    Authentication auth = new UsernamePasswordAuthenticationToken("user", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    users.createUser("user", "user");
    users.createUser("admin", "admin");

    List<User> userList = users.getAllUsers();

    assertEquals(2, userList.size());

    for (User user : userList) {
      assertEquals(false, user.isLdapUser());
    }

    assertEquals(2, userDAO.findAll().size());

    UserEntity userEntity = userDAO.findUserByName("user");
    assertNotNull("user", userEntity.getUserPassword());

    users.modifyPassword("user", "user", "resu");

    assertNotSame(userEntity.getUserPassword(), userDAO.findUserByName("user").getUserPassword());
  }

  @Test
  public void testGetAnyUser() throws Exception {
    users.createUser("user", "user", true, false, false);
    users.createUser("user_ldap", "user_ldap", true, false, true);

    assertEquals("user", users.getAnyUser("user").getUserName());
    assertEquals("user_ldap", users.getAnyUser("user_ldap").getUserName());
    Assert.assertNull(users.getAnyUser("non_existing"));
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
    users.createUser("user_ldap", "user_ldap", true, false, true);

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
    users.createGroup("group");

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
  public void testCreateGroup() throws Exception {
    final String groupName = "engineering";
    users.createGroup(groupName);
    assertNotNull(groupDAO.findGroupByName(groupName));
  }

  @Test
  public void testGetGroup() throws Exception {
    final String groupName = "engineering";
    users.createGroup(groupName);

    final Group group = users.getGroup(groupName);
    assertNotNull(group);
    assertEquals(false, group.isLdapGroup());
    assertEquals(groupName, group.getGroupName());

    assertNotNull(groupDAO.findGroupByName(groupName));
  }

  @Test
  public void testGetAllGroups() throws Exception {
    users.createGroup("one");
    users.createGroup("two");

    final List<Group> groupList = users.getAllGroups();

    assertEquals(2, groupList.size());
    assertEquals(2, groupDAO.findAll().size());
  }

  @Test
  public void testRemoveGroup() throws Exception {
    final String groupName = "engineering";
    users.createGroup(groupName);
    final Group group = users.getGroup(groupName);
    assertEquals(1, users.getAllGroups().size());
    users.removeGroup(group);
    assertEquals(0, users.getAllGroups().size());
  }

  @Test
  public void testAddMemberToGroup() throws Exception {
    final String groupName = "engineering";
    users.createGroup(groupName);
    users.createUser("user", "user");
    users.addMemberToGroup(groupName, "user");
    assertEquals(1, groupDAO.findGroupByName(groupName).getMemberEntities().size());
  }

  @Test
  public void testGetAllMembers() throws Exception {
    final String groupName = "engineering";
    users.createGroup(groupName);
    users.createUser("user1", "user1");
    users.createUser("user2", "user2");
    users.createUser("user3", "user3");
    users.addMemberToGroup(groupName, "user1");
    users.addMemberToGroup(groupName, "user2");
    assertEquals(2, users.getAllMembers(groupName).size());

    try {
      users.getAllMembers("non existing");
      Assert.fail("It shouldn't be possible to call getAllMembers() on non-existing group");
    } catch (Exception ex) {
    }
  }

  @Test
  public void testRemoveMemberFromGroup() throws Exception {
    final String groupName = "engineering";
    users.createGroup(groupName);
    users.createUser("user", "user");
    users.addMemberToGroup(groupName, "user");
    assertEquals(1, groupDAO.findGroupByName(groupName).getMemberEntities().size());
    users.removeMemberFromGroup(groupName, "user");
    assertEquals(0, groupDAO.findGroupByName(groupName).getMemberEntities().size());
  }

  @Test
  public void testGetGroupMembers() throws Exception {
    final String groupNameTwoMembers = "engineering";
    final String groupNameZeroMembers = "management";
    users.createGroup(groupNameTwoMembers);
    users.createGroup(groupNameZeroMembers);
    users.createUser("user", "user");
    users.createUser("admin", "admin");
    users.addMemberToGroup(groupNameTwoMembers, "user");
    users.addMemberToGroup(groupNameTwoMembers, "admin");

    assertEquals(users.getGroupMembers(groupNameTwoMembers).size(), 2);
    assertEquals(users.getGroupMembers(groupNameZeroMembers).size(), 0);
  }

  @Test
  public void testGetGroupMembersUnexistingGroup() throws Exception {
    assertEquals(users.getGroupMembers("unexisting"), null);
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
  public void testModifyPassword_UserByAdmin() throws Exception {
    users.createUser("admin", "admin", true, true, false);
    users.createUser("user", "user");

    UserEntity userEntity = userDAO.findUserByName("user");

    assertNotSame("user", userEntity.getUserPassword());
    assertTrue(passwordEncoder.matches("user", userEntity.getUserPassword()));

    users.modifyPassword("user", "admin", "user_new_password");
    assertTrue(passwordEncoder.matches("user_new_password", userDAO.findUserByName("user").getUserPassword()));
  }

  @Test
  public void testCreateUserTwoParams() throws Exception {
    users.createUser("user", "user");

    final User createdUser = users.getAnyUser("user");
    Assert.assertEquals("user", createdUser.getUserName());
    Assert.assertEquals(true, createdUser.isActive());
    Assert.assertEquals(false, createdUser.isLdapUser());
    Assert.assertEquals(false, createdUser.isAdmin());
  }

  @Test
  @Ignore // TODO @Transactional annotation breaks this test
  public void testCreateUserDefaultParams() throws Exception {
    final Users spy = Mockito.spy(users);
    spy.createUser("user", "user");
    Mockito.verify(spy).createUser("user", "user", true, false, false);
  }

  @Test
  public void testCreateUserFiveParams() throws Exception {
    users.createUser("user", "user", false, false, false);

    final User createdUser = users.getAnyUser("user");
    Assert.assertEquals("user", createdUser.getUserName());
    Assert.assertEquals(false, createdUser.isActive());
    Assert.assertEquals(false, createdUser.isLdapUser());
    Assert.assertEquals(false, createdUser.isAdmin());

    users.createUser("user2", "user2", true, true, true);
    final User createdUser2 = users.getAnyUser("user2");
    Assert.assertEquals("user2", createdUser2.getUserName());
    Assert.assertEquals(true, createdUser2.isActive());
    Assert.assertEquals(true, createdUser2.isLdapUser());
    Assert.assertEquals(true, createdUser2.isAdmin());
  }

  @Test(expected = AmbariException.class)
  public void testCreateUserDuplicate() throws Exception {
    users.createUser("user", "user");
    users.createUser("user", "user");
  }

  @Test
  public void testRemoveUser() throws Exception {
    users.createUser("user1", "user1");
    users.createUser("user2", "user2");
    users.createUser("user3", "user3");
    Assert.assertEquals(3, users.getAllUsers().size());

    users.removeUser(users.getAnyUser("user1"));

    Assert.assertNull(users.getAnyUser("user1"));
    Assert.assertEquals(2, users.getAllUsers().size());
  }

  @Test
  public void testGrantAdminPrivilege() throws Exception {
    users.createUser("user", "user");

    final User user = users.getAnyUser("user");
    users.grantAdminPrivilege(user.getUserId());

    Assert.assertTrue(users.getAnyUser("user").isAdmin());
  }

  @Test
  public void testRevokeAdminPrivilege() throws Exception {
    users.createUser("admin", "admin", true, true, false);

    final User admin = users.getAnyUser("admin");
    users.revokeAdminPrivilege(admin.getUserId());

    Assert.assertFalse(users.getAnyUser("admin").isAdmin());
  }

  @Test
  public void testIsUserCanBeRemoved() throws Exception {
    users.createUser("admin", "admin", true, true, false);
    users.createUser("admin2", "admin2", true, true, false);

    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));
    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin2")));

    users.removeUser(users.getAnyUser("admin"));

    Assert.assertFalse(users.isUserCanBeRemoved(userDAO.findUserByName("admin2")));
    users.createUser("user", "user");
    Assert.assertFalse(users.isUserCanBeRemoved(userDAO.findUserByName("admin2")));

    users.createUser("admin3", "admin3", true, true, false);
    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin2")));
    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin3")));
  }

}
