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
package org.apache.ambari.server.security.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

public class AmbariLdapDataPopulatorTest {
  private static class AmbariLdapDataPopulatorTestInstance extends AmbariLdapDataPopulator {

    public AmbariLdapDataPopulatorTestInstance(Configuration configuration,
        Users users) {
      super(configuration, users);
      this.ldapServerProperties = EasyMock.createNiceMock(LdapServerProperties.class);
    }

    final LdapTemplate ldapTemplate = EasyMock.createNiceMock(LdapTemplate.class);

    @Override
    protected LdapTemplate loadLdapTemplate() {
      return ldapTemplate;
    }

    public LdapServerProperties getLdapServerProperties() {
      return this.ldapServerProperties;
    }
  }

  @Test
  public void testIsLdapEnabled_badConfiguration() {
    final Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    final Users users = EasyMock.createNiceMock(Users.class);

    final AmbariLdapDataPopulator populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);

    EasyMock.expect(configuration.isLdapConfigured()).andReturn(true);
    EasyMock.expect(populator.loadLdapTemplate().search(EasyMock. <String>anyObject(), EasyMock. <String>anyObject(), EasyMock. <AttributesMapper>anyObject())).andThrow(new NullPointerException()).once();
    EasyMock.replay(populator.loadLdapTemplate(), configuration);

    Assert.assertFalse(populator.isLdapEnabled());
    EasyMock.verify(populator.loadLdapTemplate(), configuration);
  }

  @Test
  public void testIsLdapEnabled_reallyEnabled() {
    final Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    final Users users = EasyMock.createNiceMock(Users.class);

    final AmbariLdapDataPopulator populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);

    EasyMock.expect(configuration.isLdapConfigured()).andReturn(true);
    EasyMock.expect(populator.loadLdapTemplate().search(EasyMock. <String>anyObject(), EasyMock. <String>anyObject(), EasyMock. <AttributesMapper>anyObject())).andReturn(Collections.emptyList()).once();
    EasyMock.replay(populator.loadLdapTemplate(), configuration);

    Assert.assertTrue(populator.isLdapEnabled());
    EasyMock.verify(populator.loadLdapTemplate(), configuration);
  }

  @Test
  public void testIsLdapEnabled_reallyDisabled() {
    final Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    final Users users = EasyMock.createNiceMock(Users.class);

    final AmbariLdapDataPopulator populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);

    EasyMock.expect(configuration.isLdapConfigured()).andReturn(false);
    EasyMock.replay(populator.loadLdapTemplate(), configuration);

    Assert.assertFalse(populator.isLdapEnabled());
    EasyMock.verify(populator.loadLdapTemplate(), configuration);
  }

  @Test
  @SuppressWarnings("serial")
  public void testCleanUpLdapUsersWithoutGroup() throws AmbariException {
    final Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    final Users users = EasyMock.createNiceMock(Users.class);

    final GroupEntity ldapGroup = new GroupEntity();
    ldapGroup.setGroupId(1);
    ldapGroup.setGroupName("ldapGroup");
    ldapGroup.setLdapGroup(true);
    ldapGroup.setMemberEntities(new HashSet<MemberEntity>());

    final User ldapUserWithoutGroup = createLdapUserWithoutGroup();
    final User ldapUserWithGroup = createLdapUserWithGroup(ldapGroup);
    final User localUserWithoutGroup = createLocalUserWithoutGroup();
    final User localUserWithGroup = createLocalUserWithGroup(ldapGroup);

    final List<User> allUsers = new ArrayList<User>() {
      {
        add(ldapUserWithoutGroup);
        add(ldapUserWithGroup);
        add(localUserWithoutGroup);
        add(localUserWithGroup);
      }
    };
    EasyMock.expect(users.getAllUsers()).andReturn(new ArrayList<User>(allUsers));

    final List<User> removedUsers = new ArrayList<User>();
    final Capture<User> userCapture = new Capture<User>();
    users.removeUser(EasyMock.capture(userCapture));
    EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        removedUsers.add(userCapture.getValue());
        allUsers.remove(userCapture.getValue());
        return null;
      }
    });

    EasyMock.replay(users);

    final AmbariLdapDataPopulator populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);
    populator.cleanUpLdapUsersWithoutGroup();

    Assert.assertEquals(removedUsers.size(), 1);
    Assert.assertEquals(allUsers.size(), 3);
    Assert.assertTrue(allUsers.contains(ldapUserWithGroup));
    Assert.assertTrue(allUsers.contains(localUserWithoutGroup));
    Assert.assertTrue(allUsers.contains(localUserWithGroup));
    Assert.assertEquals(removedUsers.get(0), ldapUserWithoutGroup);

    EasyMock.verify(users);
  }

  private static int userIdCounter = 1;

  private User createUser(String name, boolean ldapUser, GroupEntity group) {
    final UserEntity userEntity = new UserEntity();
    userEntity.setUserId(userIdCounter++);
    userEntity.setUserName(name);
    userEntity.setCreateTime(new Date());
    userEntity.setLdapUser(ldapUser);
    userEntity.setActive(true);
    userEntity.setMemberEntities(new HashSet<MemberEntity>());
    final PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrivileges(new HashSet<PrivilegeEntity>());
    userEntity.setPrincipal(principalEntity);
    if (group != null) {
      final MemberEntity member = new MemberEntity();
      member.setUser(userEntity);
      member.setGroup(group);
      group.getMemberEntities().add(member);
      userEntity.getMemberEntities().add(member);
    }
    return new User(userEntity);
  }

  private User createLdapUserWithoutGroup() {
    return createUser("LdapUserWithoutGroup", true, null);
  }

  private User createLocalUserWithoutGroup() {
    return createUser("LocalUserWithoutGroup", false, null);
  }

  private User createLdapUserWithGroup(GroupEntity group) {
    return createUser("LdapUserWithGroup", true, group);
  }

  private User createLocalUserWithGroup(GroupEntity group) {
    return createUser("LocalUserWithGroup", false, group);
  }
}
