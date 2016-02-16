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

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.Group;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.springframework.ldap.control.PagedResultsCookie;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import javax.naming.directory.SearchControls;

import static junit.framework.Assert.*;
import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.createNiceMock;

public class AmbariLdapDataPopulatorTest {
  public static class AmbariLdapDataPopulatorTestInstance extends TestAmbariLdapDataPopulator {
    public AmbariLdapDataPopulatorTestInstance(Configuration configuration, Users users) {
      super(configuration, users);
    }

    @Override
    protected LdapTemplate loadLdapTemplate() {
      return ldapTemplate;
    }
  }

  public static class TestAmbariLdapDataPopulator extends AmbariLdapDataPopulator {

    protected LdapTemplate ldapTemplate;
    private LdapContextSource ldapContextSource;
    private PagedResultsDirContextProcessor processor;

    public TestAmbariLdapDataPopulator(Configuration configuration, Users users) {
      super(configuration, users);
    }

    @Override
    protected LdapContextSource createLdapContextSource() {
      return ldapContextSource;
    }

    @Override
    protected LdapTemplate createLdapTemplate(LdapContextSource ldapContextSource) {
      this.ldapContextSource = ldapContextSource;
      return ldapTemplate;
    }

    @Override
    protected PagedResultsDirContextProcessor createPagingProcessor() {
      return processor;
    }

    public void setLdapContextSource(LdapContextSource ldapContextSource) {
      this.ldapContextSource = ldapContextSource;
    }

    public void setProcessor(PagedResultsDirContextProcessor processor) {
      this.processor = processor;
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
      this.ldapTemplate = ldapTemplate;
    }

    public LdapServerProperties getLdapServerProperties() {
      return this.ldapServerProperties;
    }

    public void setLdapServerProperties(LdapServerProperties ldapServerProperties) {
      this.ldapServerProperties = ldapServerProperties;
    }

    public LdapContextSource getLdapContextSource() {
      return ldapContextSource;
    }
  }

  @Test
  public void testIsLdapEnabled_badConfiguration() {
    final Configuration configuration = createNiceMock(Configuration.class);
    final Users users = createNiceMock(Users.class);

    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(configuration.isLdapConfigured()).andReturn(true);
    expect(ldapTemplate.search(EasyMock.<String>anyObject(), EasyMock.<String>anyObject(), EasyMock.<AttributesMapper>anyObject())).andThrow(new NullPointerException()).once();
    replay(ldapTemplate, configuration, ldapServerProperties);

    final AmbariLdapDataPopulatorTestInstance populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);
    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    assertFalse(populator.isLdapEnabled());
    verify(populator.loadLdapTemplate(), configuration);
  }

  @Test
  public void testReferralMethod() {
    final Configuration configuration = createNiceMock(Configuration.class);
    final Users users = createNiceMock(Users.class);
    LdapContextSource ldapContextSource = createNiceMock(LdapContextSource.class);

    List<String> ldapUrls = Collections.singletonList("url");

    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(configuration.getLdapServerProperties()).andReturn(ldapServerProperties).anyTimes();
    expect(ldapServerProperties.getLdapUrls()).andReturn(ldapUrls).anyTimes();
    expect(ldapServerProperties.getReferralMethod()).andReturn("follow");
    ldapContextSource.setReferral("follow");
    ldapTemplate.setIgnorePartialResultException(true);

    replay(ldapTemplate, configuration, ldapServerProperties, ldapContextSource);

    final TestAmbariLdapDataPopulator populator = new TestAmbariLdapDataPopulator(configuration, users);
    populator.setLdapContextSource(ldapContextSource);
    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    populator.loadLdapTemplate();

    verify(ldapTemplate, configuration, ldapServerProperties, ldapContextSource);
  }

  @Test
  public void testIsLdapEnabled_reallyEnabled() {
    final Configuration configuration = createNiceMock(Configuration.class);
    final Users users = createNiceMock(Users.class);

    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(configuration.isLdapConfigured()).andReturn(true);
    expect(ldapTemplate.search(EasyMock.<String>anyObject(), EasyMock.<String>anyObject(), EasyMock.<AttributesMapper>anyObject())).andReturn(Collections.emptyList()).once();
    replay(ldapTemplate, configuration);

    final AmbariLdapDataPopulatorTestInstance populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);
    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    assertTrue(populator.isLdapEnabled());
    verify(populator.loadLdapTemplate(), configuration);
  }

  @Test
  public void testIsLdapEnabled_reallyDisabled() {
    final Configuration configuration = createNiceMock(Configuration.class);
    final Users users = createNiceMock(Users.class);

    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(configuration.isLdapConfigured()).andReturn(false);
    expect(configuration.getLdapServerProperties()).andReturn(ldapServerProperties);
    replay(ldapTemplate, ldapServerProperties, configuration);

    final AmbariLdapDataPopulatorTestInstance populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);
    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    assertFalse(populator.isLdapEnabled());
    verify(populator.loadLdapTemplate(), populator.getLdapServerProperties(), configuration);
  }

  private <T> Set<T> createSet(T...elements) {
    return new HashSet<T>(Arrays.asList(elements));
  }

  @Test
  public void synchronizeExistingLdapGroups() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    Group group3 = createNiceMock(Group.class);
    Group group4 = createNiceMock(Group.class);
    Group group5 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group3.getGroupName()).andReturn("group3").anyTimes();
    expect(group4.getGroupName()).andReturn("group4").anyTimes();
    expect(group5.getGroupName()).andReturn("group5").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();
    expect(group3.isLdapGroup()).andReturn(false).anyTimes();
    expect(group4.isLdapGroup()).andReturn(true).anyTimes();
    expect(group5.isLdapGroup()).andReturn(true).anyTimes();

    List<Group> groupList = Arrays.asList(group1, group2, group3, group4, group5);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(groupList);
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2, group3, group4, group5);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapGroups")
        .addMockedMethod("refreshGroupMembers")
        .withConstructor(configuration, users)
        .createNiceMock();

    expect(populator.getLdapGroups("group2")).andReturn(Collections.EMPTY_SET);
    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    LdapBatchDto batchInfo = new LdapBatchDto();
    populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup1), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
    expectLastCall();
    expect(populator.getLdapGroups("group4")).andReturn(Collections.singleton(externalGroup1));
    expect(populator.getLdapGroups("group5")).andReturn(Collections.EMPTY_SET);
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeExistingLdapGroups(batchInfo);

    assertEquals(2, result.getGroupsToBeRemoved().size());
    assertTrue(result.getGroupsToBeRemoved().contains("group2"));
    assertTrue(result.getGroupsToBeRemoved().contains("group5"));
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeExistingLdapGroups_removeDuringIteration() throws Exception {
    // GIVEN
    Group group1 = createNiceMock(Group.class);
    expect(group1.getGroupId()).andReturn(1).anyTimes();
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group1.isLdapGroup()).andReturn(true).anyTimes();

    Group group2 = createNiceMock(Group.class);
    expect(group2.getGroupId()).andReturn(2).anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    expect(users.getAllGroups()).andReturn(Arrays.asList(group1, group2));
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);
    expect(configuration.getLdapServerProperties()).andReturn(new LdapServerProperties()).anyTimes();

    Set<LdapGroupDto> groupDtos = Sets.newHashSet();
    LdapGroupDto group1Dto = new LdapGroupDto();
    group1Dto.setGroupName("group1");
    group1Dto.setMemberAttributes(Sets.newHashSet("group2"));

    LdapGroupDto group2Dto = new LdapGroupDto();
    group2Dto.setGroupName("group2");
    group2Dto.setMemberAttributes(Collections.EMPTY_SET);
    groupDtos.add(group1Dto);
    groupDtos.add(group2Dto);

    LdapBatchDto batchInfo = new LdapBatchDto();
    replay(configuration, users, group1, group2);
    AmbariLdapDataPopulator dataPopulator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
      .withConstructor(configuration, users)
      .addMockedMethod("getLdapGroups")
      .addMockedMethod("getLdapUserByMemberAttr")
      .addMockedMethod("getLdapGroupByMemberAttr")
      .createNiceMock();

    expect(dataPopulator.getLdapUserByMemberAttr(anyString())).andReturn(null).anyTimes();
    expect(dataPopulator.getLdapGroupByMemberAttr("group2")).andReturn(group2Dto);
    expect(dataPopulator.getLdapGroups("group1")).andReturn(groupDtos).anyTimes();
    expect(dataPopulator.getLdapGroups("group2")).andReturn(groupDtos).anyTimes();

    replay(dataPopulator);
    // WHEN
    dataPopulator.synchronizeExistingLdapGroups(batchInfo);
    // THEN
    verify(dataPopulator, group1, group2);

  }

  @Test
  public void testSynchronizeLdapGroups_allExist() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    Group group3 = createNiceMock(Group.class);
    Group group4 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group3.getGroupName()).andReturn("group3").anyTimes();
    expect(group4.getGroupName()).andReturn("group4").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();
    expect(group3.isLdapGroup()).andReturn(true).anyTimes();
    expect(group4.isLdapGroup()).andReturn(false).anyTimes();

    List<Group> groupList = Arrays.asList(group1, group2, group3, group4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(groupList);
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2, group3, group4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapGroups")
        .addMockedMethod("refreshGroupMembers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup2 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup3 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup4 = createNiceMock(LdapGroupDto.class);
    expect(externalGroup1.getGroupName()).andReturn("group1").anyTimes();
    expect(externalGroup2.getGroupName()).andReturn("group2").anyTimes();
    expect(externalGroup3.getGroupName()).andReturn("xgroup1").anyTimes();
    expect(externalGroup4.getGroupName()).andReturn("xgroup2").anyTimes();
    replay(externalGroup1, externalGroup2, externalGroup3, externalGroup4);

    LdapBatchDto batchInfo = new LdapBatchDto();
    Set<LdapGroupDto> externalGroups = createSet(externalGroup3, externalGroup4);
    for (LdapGroupDto externalGroup : externalGroups) {
      populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
      expectLastCall();
    }
    populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup1), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
    expectLastCall();
    populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup2), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
    expectLastCall();
    expect(populator.getLdapGroups("x*")).andReturn(externalGroups);
    expect(populator.getLdapGroups("group1")).andReturn(Collections.singleton(externalGroup1));
    expect(populator.getLdapGroups("group2")).andReturn(Collections.singleton(externalGroup2));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeLdapGroups(createSet("x*", "group1", "group2"), batchInfo);

    assertEquals(1, result.getGroupsToBecomeLdap().size());
    assertTrue(result.getGroupsToBecomeLdap().contains("group1"));
    assertEquals(2, result.getGroupsToBeCreated().size());
    assertTrue(result.getGroupsToBeCreated().contains("xgroup1"));
    assertTrue(result.getGroupsToBeCreated().contains("xgroup2"));
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    assertTrue(result.getGroupsProcessedInternal().contains("group1"));
    assertTrue(result.getGroupsProcessedInternal().contains("group2"));
    assertTrue(!result.getGroupsProcessedInternal().contains("xgroup1"));
    assertTrue(!result.getGroupsProcessedInternal().contains("xgroup2"));
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeLdapGroups_add() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    Group group3 = createNiceMock(Group.class);
    Group group4 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group3.getGroupName()).andReturn("group3").anyTimes();
    expect(group4.getGroupName()).andReturn("group4").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();
    expect(group3.isLdapGroup()).andReturn(true).anyTimes();
    expect(group4.isLdapGroup()).andReturn(false).anyTimes();

    List<Group> groupList = Arrays.asList(group1, group2, group3, group4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(groupList);
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2, group3, group4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapGroups")
        .addMockedMethod("refreshGroupMembers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup2 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup3 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup4 = createNiceMock(LdapGroupDto.class);
    expect(externalGroup1.getGroupName()).andReturn("group1").anyTimes();
    expect(externalGroup2.getGroupName()).andReturn("group2").anyTimes();
    expect(externalGroup3.getGroupName()).andReturn("xgroup1").anyTimes();
    expect(externalGroup4.getGroupName()).andReturn("xgroup2").anyTimes();
    replay(externalGroup1, externalGroup2, externalGroup3, externalGroup4);


    LdapBatchDto batchInfo = new LdapBatchDto();
    Set<LdapGroupDto> externalGroups = createSet(externalGroup3, externalGroup4);
    for (LdapGroupDto externalGroup : externalGroups) {
      populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
      expectLastCall();
    }
    populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup2), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
    expectLastCall();
    expect(populator.getLdapGroups("x*")).andReturn(externalGroups);
    expect(populator.getLdapGroups("group2")).andReturn(Collections.singleton(externalGroup2));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeLdapGroups(createSet("x*", "group2"), batchInfo);

    assertEquals(2, result.getGroupsToBeCreated().size());
    assertTrue(result.getGroupsToBeCreated().contains("xgroup1"));
    assertTrue(result.getGroupsToBeCreated().contains("xgroup2"));
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeLdapGroups_update() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    Group group3 = createNiceMock(Group.class);
    Group group4 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group3.getGroupName()).andReturn("group3").anyTimes();
    expect(group4.getGroupName()).andReturn("group4").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();
    expect(group3.isLdapGroup()).andReturn(true).anyTimes();
    expect(group4.isLdapGroup()).andReturn(false).anyTimes();

    List<Group> groupList = Arrays.asList(group1, group2, group3, group4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(groupList);
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2, group3, group4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapGroups")
        .addMockedMethod("refreshGroupMembers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup2 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup3 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup4 = createNiceMock(LdapGroupDto.class);
    expect(externalGroup1.getGroupName()).andReturn("group1").anyTimes();
    expect(externalGroup2.getGroupName()).andReturn("group2").anyTimes();
    expect(externalGroup3.getGroupName()).andReturn("group3").anyTimes();
    expect(externalGroup4.getGroupName()).andReturn("group4").anyTimes();
    replay(externalGroup1, externalGroup2, externalGroup3, externalGroup4);

    LdapBatchDto batchInfo = new LdapBatchDto();
    Set<LdapGroupDto> externalGroups = createSet(externalGroup1, externalGroup2, externalGroup3, externalGroup4);
    for (LdapGroupDto externalGroup : externalGroups) {
      populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
      expectLastCall();
    }
    expect(populator.getLdapGroups("group*")).andReturn(externalGroups);
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeLdapGroups(createSet("group*"), batchInfo);

    assertEquals(2, result.getGroupsToBecomeLdap().size());
    assertTrue(result.getGroupsToBecomeLdap().contains("group1"));
    assertTrue(result.getGroupsToBecomeLdap().contains("group4"));
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test(expected = AmbariException.class)
  public void testSynchronizeLdapGroups_absent() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    Group group3 = createNiceMock(Group.class);
    Group group4 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group3.getGroupName()).andReturn("group3").anyTimes();
    expect(group4.getGroupName()).andReturn("group4").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();
    expect(group3.isLdapGroup()).andReturn(true).anyTimes();
    expect(group4.isLdapGroup()).andReturn(false).anyTimes();

    List<Group> groupList = Arrays.asList(group1, group2, group3, group4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(groupList);
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2, group3, group4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapGroups")
        .addMockedMethod("refreshGroupMembers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup2 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup3 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup4 = createNiceMock(LdapGroupDto.class);
    expect(externalGroup1.getGroupName()).andReturn("group1").anyTimes();
    expect(externalGroup2.getGroupName()).andReturn("group2").anyTimes();
    expect(externalGroup3.getGroupName()).andReturn("xgroup1").anyTimes();
    expect(externalGroup4.getGroupName()).andReturn("xgroup2").anyTimes();
    replay(externalGroup1, externalGroup2, externalGroup3, externalGroup4);

    LdapBatchDto batchInfo = new LdapBatchDto();
    Set<LdapGroupDto> externalGroups = createSet(externalGroup3, externalGroup4);
    expect(populator.getLdapGroups("x*")).andReturn(externalGroups);
    expect(populator.getLdapGroups("group1")).andReturn(Collections.EMPTY_SET);
    expect(populator.getLdapGroups("group2")).andReturn(Collections.singleton(externalGroup2));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    populator.synchronizeLdapGroups(createSet("x*", "group1", "group2"), batchInfo);
  }

  @Test
  public void testSynchronizeAllLdapGroups() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    Group group3 = createNiceMock(Group.class);
    Group group4 = createNiceMock(Group.class);
    Group group5 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group3.getGroupName()).andReturn("group3").anyTimes();
    expect(group4.getGroupName()).andReturn("group4").anyTimes();
    expect(group5.getGroupName()).andReturn("group5").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();
    expect(group3.isLdapGroup()).andReturn(false).anyTimes();
    expect(group4.isLdapGroup()).andReturn(true).anyTimes();
    expect(group5.isLdapGroup()).andReturn(false).anyTimes();

    List<Group> groupList = Arrays.asList(group1, group2, group3, group4, group5);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(groupList);
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2, group3, group4, group5);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getExternalLdapGroupInfo")
        .addMockedMethod("refreshGroupMembers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup2 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup3 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup4 = createNiceMock(LdapGroupDto.class);
    expect(externalGroup1.getGroupName()).andReturn("group4").anyTimes();
    expect(externalGroup2.getGroupName()).andReturn("group3").anyTimes();
    expect(externalGroup3.getGroupName()).andReturn("group6").anyTimes();
    expect(externalGroup4.getGroupName()).andReturn("group7").anyTimes();

    LdapBatchDto batchInfo = new LdapBatchDto();
    Set<LdapGroupDto> externalGroups = createSet(externalGroup1, externalGroup2, externalGroup3, externalGroup4);
    for (LdapGroupDto externalGroup : externalGroups) {
      populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
      expectLastCall();
    }

    expect(populator.getExternalLdapGroupInfo()).andReturn(externalGroups);
    replay(externalGroup1, externalGroup2, externalGroup3, externalGroup4);
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeAllLdapGroups(batchInfo);

    assertEquals(1, result.getGroupsToBeRemoved().size());
    assertTrue(result.getGroupsToBeRemoved().contains("group2"));
    assertEquals(1, result.getGroupsToBecomeLdap().size());
    assertTrue(result.getGroupsToBecomeLdap().contains("group3"));
    assertEquals(2, result.getGroupsToBeCreated().size());
    assertTrue(result.getGroupsToBeCreated().contains("group6"));
    assertTrue(result.getGroupsToBeCreated().contains("group7"));
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeAllLdapGroups_add() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(false).anyTimes();

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(Arrays.asList(group1, group2));
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("refreshGroupMembers")
        .addMockedMethod("getExternalLdapGroupInfo")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup2 = createNiceMock(LdapGroupDto.class);
    expect(externalGroup1.getGroupName()).andReturn("group4").anyTimes();
    expect(externalGroup2.getGroupName()).andReturn("group3").anyTimes();
    LdapBatchDto batchInfo = new LdapBatchDto();
    Set<LdapGroupDto> externalGroups = createSet(externalGroup1, externalGroup2);
    for (LdapGroupDto externalGroup : externalGroups) {
      populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
      expectLastCall();
    }
    expect(populator.getExternalLdapGroupInfo()).andReturn(externalGroups);
    replay(externalGroup1, externalGroup2);
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeAllLdapGroups(batchInfo);

    assertEquals(2, result.getGroupsToBeCreated().size());
    assertTrue(result.getGroupsToBeCreated().contains("group3"));
    assertTrue(result.getGroupsToBeCreated().contains("group4"));
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeAllLdapGroups_remove() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    Group group3 = createNiceMock(Group.class);
    Group group4 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group3.getGroupName()).andReturn("group3").anyTimes();
    expect(group4.getGroupName()).andReturn("group4").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();
    expect(group3.isLdapGroup()).andReturn(true).anyTimes();
    expect(group4.isLdapGroup()).andReturn(true).anyTimes();

    List<Group> groupList = Arrays.asList(group1, group2, group3, group4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(groupList);
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2, group3, group4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("refreshGroupMembers")
        .addMockedMethod("getExternalLdapGroupInfo")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    expect(externalGroup1.getGroupName()).andReturn("group3").anyTimes();
    LdapBatchDto batchInfo = new LdapBatchDto();
    Set<LdapGroupDto> externalGroups = createSet(externalGroup1);
    for (LdapGroupDto externalGroup : externalGroups) {
      populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
      expectLastCall();
    }
    expect(populator.getExternalLdapGroupInfo()).andReturn(externalGroups);
    replay(populator);
    replay(externalGroup1);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeAllLdapGroups(batchInfo);

    assertEquals(2, result.getGroupsToBeRemoved().size());
    assertTrue(result.getGroupsToBeRemoved().contains("group2"));
    assertTrue(result.getGroupsToBeRemoved().contains("group4"));
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeAllLdapGroups_update() throws Exception {

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    Group group3 = createNiceMock(Group.class);
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    expect(group3.getGroupName()).andReturn("group3").anyTimes();
    expect(group1.isLdapGroup()).andReturn(false).anyTimes();
    expect(group2.isLdapGroup()).andReturn(false).anyTimes();
    expect(group3.isLdapGroup()).andReturn(false).anyTimes();

    List<Group> groupList = Arrays.asList(group1, group2, group3);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllGroups()).andReturn(groupList);
    expect(users.getAllUsers()).andReturn(Collections.EMPTY_LIST);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(group1, group2, group3);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("refreshGroupMembers")
        .addMockedMethod("getExternalLdapGroupInfo")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapGroupDto externalGroup1 = createNiceMock(LdapGroupDto.class);
    LdapGroupDto externalGroup2 = createNiceMock(LdapGroupDto.class);
    expect(externalGroup1.getGroupName()).andReturn("group2").anyTimes();
    expect(externalGroup2.getGroupName()).andReturn("group3").anyTimes();
    LdapBatchDto batchInfo = new LdapBatchDto();
    Set<LdapGroupDto> externalGroups = createSet(externalGroup1, externalGroup2);
    for (LdapGroupDto externalGroup : externalGroups) {
      populator.refreshGroupMembers(eq(batchInfo), eq(externalGroup), anyObject(Map.class), anyObject(Map.class), anyObject(Set.class));
      expectLastCall();
    }
    expect(populator.getExternalLdapGroupInfo()).andReturn(externalGroups);
    replay(populator);
    replay(externalGroup1, externalGroup2);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeAllLdapGroups(batchInfo);

    assertEquals(2, result.getGroupsToBecomeLdap().size());
    assertTrue(result.getGroupsToBecomeLdap().contains("group2"));
    assertTrue(result.getGroupsToBecomeLdap().contains("group3"));
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeAllLdapUsers() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    User user3 = createNiceMock(User.class);
    User user4 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("synced_user1").anyTimes();
    expect(user2.getUserName()).andReturn("synced_user2").anyTimes();
    expect(user3.getUserName()).andReturn("unsynced_user1").anyTimes();
    expect(user4.getUserName()).andReturn("unsynced_user2").anyTimes();
    expect(user1.isLdapUser()).andReturn(true).anyTimes();
    expect(user2.isLdapUser()).andReturn(true).anyTimes();
    expect(user3.isLdapUser()).andReturn(false).anyTimes();
    expect(user4.isLdapUser()).andReturn(false).anyTimes();

    List<User> userList = Arrays.asList(user1, user2, user3, user4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllUsers()).andReturn(userList);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user3, user2, user4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getExternalLdapUserInfo")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapUserDto externalUser1 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser2 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser3 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser4 = createNiceMock(LdapUserDto.class);
    expect(externalUser1.getUserName()).andReturn("synced_user2").anyTimes();
    expect(externalUser2.getUserName()).andReturn("unsynced_user2").anyTimes();
    expect(externalUser3.getUserName()).andReturn("external_user1").anyTimes();
    expect(externalUser4.getUserName()).andReturn("external_user2").anyTimes();
    replay(externalUser1, externalUser2, externalUser3, externalUser4);

    expect(populator.getExternalLdapUserInfo()).andReturn(
        createSet(externalUser1, externalUser2, externalUser3, externalUser4));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeAllLdapUsers(new LdapBatchDto());

    assertEquals(1, result.getUsersToBeRemoved().size());
    assertTrue(result.getUsersToBeRemoved().contains("synced_user1"));
    assertEquals(2, result.getUsersToBeCreated().size());
    assertTrue(result.getUsersToBeCreated().contains("external_user1"));
    assertTrue(result.getUsersToBeCreated().contains("external_user2"));
    assertEquals(1, result.getUsersToBecomeLdap().size());
    assertTrue(result.getUsersToBecomeLdap().contains("unsynced_user2"));
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeAllLdapUsers_add() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("user1").anyTimes();
    expect(user2.getUserName()).andReturn("user2").anyTimes();
    expect(user1.isLdapUser()).andReturn(false).anyTimes();
    expect(user2.isLdapUser()).andReturn(false).anyTimes();

    List<User> userList = Arrays.asList(user1, user2);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllUsers()).andReturn(userList);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user2);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getExternalLdapUserInfo")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapUserDto externalUser1 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser2 = createNiceMock(LdapUserDto.class);
    expect(externalUser1.getUserName()).andReturn("user3").anyTimes();
    expect(externalUser2.getUserName()).andReturn("user4").anyTimes();
    replay(externalUser1, externalUser2);

    expect(populator.getExternalLdapUserInfo()).andReturn(
        createSet(externalUser1, externalUser2));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeAllLdapUsers(new LdapBatchDto());

    assertEquals(2, result.getUsersToBeCreated().size());
    assertTrue(result.getUsersToBeCreated().contains("user3"));
    assertTrue(result.getUsersToBeCreated().contains("user4"));
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeAllLdapUsers_remove() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    User user3 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("user1").anyTimes();
    expect(user2.getUserName()).andReturn("user2").anyTimes();
    expect(user3.getUserName()).andReturn("user3").anyTimes();
    expect(user1.isLdapUser()).andReturn(true).anyTimes();
    expect(user2.isLdapUser()).andReturn(false).anyTimes();
    expect(user3.isLdapUser()).andReturn(true).anyTimes();

    List<User> userList = Arrays.asList(user1, user2, user3);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllUsers()).andReturn(userList);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user2, user3);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getExternalLdapUserInfo")
        .withConstructor(configuration, users)
        .createNiceMock();


    expect(populator.getExternalLdapUserInfo()).andReturn(Collections.EMPTY_SET);
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeAllLdapUsers(new LdapBatchDto());

    assertEquals(2, result.getUsersToBeRemoved().size());
    assertTrue(result.getUsersToBeRemoved().contains("user3"));
    assertTrue(result.getUsersToBeRemoved().contains("user1"));
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeAllLdapUsers_update() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    User user3 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("user1").anyTimes();
    expect(user2.getUserName()).andReturn("user2").anyTimes();
    expect(user3.getUserName()).andReturn("user3").anyTimes();
    expect(user1.isLdapUser()).andReturn(true).anyTimes();
    expect(user2.isLdapUser()).andReturn(false).anyTimes();
    expect(user3.isLdapUser()).andReturn(false).anyTimes();

    List<User> userList = Arrays.asList(user1, user2, user3);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllUsers()).andReturn(userList);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user2, user3);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getExternalLdapUserInfo")
        .withConstructor(configuration, users)
        .createNiceMock();


    LdapUserDto externalUser1 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser2 = createNiceMock(LdapUserDto.class);
    expect(externalUser1.getUserName()).andReturn("user1").anyTimes();
    expect(externalUser2.getUserName()).andReturn("user3").anyTimes();
    replay(externalUser1, externalUser2);

    expect(populator.getExternalLdapUserInfo()).andReturn(
        createSet(externalUser1, externalUser2));

    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeAllLdapUsers(new LdapBatchDto());

    assertEquals(1, result.getUsersToBecomeLdap().size());
    assertTrue(result.getUsersToBecomeLdap().contains("user3"));
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeExistingLdapUsers() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    User user3 = createNiceMock(User.class);
    User user4 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("synced_user1").anyTimes();
    expect(user2.getUserName()).andReturn("synced_user2").anyTimes();
    expect(user3.getUserName()).andReturn("unsynced_user1").anyTimes();
    expect(user4.getUserName()).andReturn("unsynced_user2").anyTimes();
    expect(user1.isLdapUser()).andReturn(true).anyTimes();
    expect(user2.isLdapUser()).andReturn(true).anyTimes();
    expect(user3.isLdapUser()).andReturn(false).anyTimes();
    expect(user4.isLdapUser()).andReturn(false).anyTimes();

    List<User> userList = Arrays.asList(user1, user2, user3, user4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllUsers()).andReturn(userList);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user2, user3, user4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapUsers")
        .withConstructor(configuration, users)
        .createNiceMock();

    expect(populator.getLdapUsers("synced_user1")).andReturn(Collections.EMPTY_SET);
    expect(populator.getLdapUsers("synced_user2")).andReturn(Collections.singleton(createNiceMock(LdapUserDto.class)));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeExistingLdapUsers(new LdapBatchDto());

    assertEquals(1, result.getUsersToBeRemoved().size());
    assertTrue(result.getUsersToBeRemoved().contains("synced_user1"));
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeLdapUsers_allExist() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    User user3 = createNiceMock(User.class);
    User user4 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("user1").anyTimes();
    expect(user2.getUserName()).andReturn("user2").anyTimes();
    expect(user3.getUserName()).andReturn("user5").anyTimes();
    expect(user4.getUserName()).andReturn("user6").anyTimes();
    expect(user1.isLdapUser()).andReturn(false).anyTimes();
    expect(user2.isLdapUser()).andReturn(true).anyTimes();
    expect(user3.isLdapUser()).andReturn(true).anyTimes();
    expect(user4.isLdapUser()).andReturn(false).anyTimes();

    List<User> userList = Arrays.asList(user1, user2, user3, user4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllUsers()).andReturn(userList);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user2, user3, user4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapUsers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapUserDto externalUser1 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser2 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser3 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser4 = createNiceMock(LdapUserDto.class);
    expect(externalUser1.getUserName()).andReturn("user1").anyTimes();
    expect(externalUser2.getUserName()).andReturn("user2").anyTimes();
    expect(externalUser3.getUserName()).andReturn("xuser3").anyTimes();
    expect(externalUser4.getUserName()).andReturn("xuser4").anyTimes();
    replay(externalUser1, externalUser2, externalUser3, externalUser4);

    expect(populator.getLdapUsers("xuser*")).andReturn(
        createSet(externalUser3, externalUser4));
    expect(populator.getLdapUsers("user1")).andReturn(Collections.singleton(externalUser1));
    expect(populator.getLdapUsers("user2")).andReturn(Collections.singleton(externalUser2));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeLdapUsers(createSet("user1", "user2", "xuser*"), new LdapBatchDto());

    assertEquals(2, result.getUsersToBeCreated().size());
    assertTrue(result.getUsersToBeCreated().contains("xuser3"));
    assertTrue(result.getUsersToBeCreated().contains("xuser4"));
    assertEquals(1, result.getUsersToBecomeLdap().size());
    assertTrue(result.getUsersToBecomeLdap().contains("user1"));
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeLdapUsers_add() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    User user3 = createNiceMock(User.class);
    User user4 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("user1").anyTimes();
    expect(user2.getUserName()).andReturn("user2").anyTimes();
    expect(user3.getUserName()).andReturn("user5").anyTimes();
    expect(user4.getUserName()).andReturn("user6").anyTimes();
    expect(user1.isLdapUser()).andReturn(false).anyTimes();
    expect(user2.isLdapUser()).andReturn(true).anyTimes();
    expect(user3.isLdapUser()).andReturn(true).anyTimes();
    expect(user4.isLdapUser()).andReturn(false).anyTimes();

    List<User> userList = Arrays.asList(user1, user2, user3, user4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllUsers()).andReturn(userList);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user2, user3, user4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapUsers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapUserDto externalUser2 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser3 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser4 = createNiceMock(LdapUserDto.class);
    expect(externalUser2.getUserName()).andReturn("user2").anyTimes();
    expect(externalUser3.getUserName()).andReturn("xuser3").anyTimes();
    expect(externalUser4.getUserName()).andReturn("xuser4").anyTimes();
    replay(externalUser2, externalUser3, externalUser4);

    expect(populator.getLdapUsers("xuser*")).andReturn(
        createSet(externalUser3, externalUser4));
    expect(populator.getLdapUsers("user2")).andReturn(Collections.singleton(externalUser2));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeLdapUsers(createSet("user2", "xuser*"), new LdapBatchDto());

    assertEquals(2, result.getUsersToBeCreated().size());
    assertTrue(result.getUsersToBeCreated().contains("xuser3"));
    assertTrue(result.getUsersToBeCreated().contains("xuser4"));
    assertTrue(result.getUsersToBecomeLdap().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  public void testSynchronizeLdapUsers_update() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    User user3 = createNiceMock(User.class);
    User user4 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("user1").anyTimes();
    expect(user2.getUserName()).andReturn("user2").anyTimes();
    expect(user3.getUserName()).andReturn("user5").anyTimes();
    expect(user4.getUserName()).andReturn("user6").anyTimes();
    expect(user1.isLdapUser()).andReturn(false).anyTimes();
    expect(user2.isLdapUser()).andReturn(true).anyTimes();
    expect(user3.isLdapUser()).andReturn(true).anyTimes();
    expect(user4.isLdapUser()).andReturn(false).anyTimes();

    List<User> userList = Arrays.asList(user1, user2, user3, user4);

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(users.getAllUsers()).andReturn(userList);

    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user2, user3, user4);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapUsers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapUserDto externalUser1 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser2 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser3 = createNiceMock(LdapUserDto.class);
    expect(externalUser1.getUserName()).andReturn("user1").anyTimes();
    expect(externalUser2.getUserName()).andReturn("user2").anyTimes();
    expect(externalUser3.getUserName()).andReturn("user6").anyTimes();
    replay(externalUser2, externalUser3, externalUser1);

    expect(populator.getLdapUsers("user1")).andReturn(
        Collections.singleton(externalUser1));
    expect(populator.getLdapUsers("user2")).andReturn(Collections.singleton(externalUser2));
    expect(populator.getLdapUsers("user6")).andReturn(Collections.singleton(externalUser3));
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    LdapBatchDto result = populator.synchronizeLdapUsers(createSet("user2", "user1", "user6"), new LdapBatchDto());

    assertEquals(2, result.getUsersToBecomeLdap().size());
    assertTrue(result.getUsersToBecomeLdap().contains("user1"));
    assertTrue(result.getUsersToBecomeLdap().contains("user6"));
    assertTrue(result.getUsersToBeCreated().isEmpty());
    assertTrue(result.getUsersToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeRemoved().isEmpty());
    assertTrue(result.getGroupsToBeCreated().isEmpty());
    assertTrue(result.getGroupsToBecomeLdap().isEmpty());
    assertTrue(result.getMembershipToAdd().isEmpty());
    assertTrue(result.getMembershipToRemove().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test(expected = AmbariException.class)
  public void testSynchronizeLdapUsers_absent() throws Exception {

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);

    replay(ldapTemplate, ldapServerProperties, users, configuration);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
        .addMockedMethod("getLdapUsers")
        .withConstructor(configuration, users)
        .createNiceMock();

    LdapUserDto externalUser1 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser2 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser3 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser4 = createNiceMock(LdapUserDto.class);
    expect(externalUser1.getUserName()).andReturn("user1").anyTimes();
    expect(externalUser2.getUserName()).andReturn("user2").anyTimes();
    expect(externalUser3.getUserName()).andReturn("xuser3").anyTimes();
    expect(externalUser4.getUserName()).andReturn("xuser4").anyTimes();
    replay(externalUser1, externalUser2, externalUser3, externalUser4);

    expect(populator.getLdapUsers("xuser*")).andReturn(createSet(externalUser3, externalUser4));
    expect(populator.getLdapUsers("user1")).andReturn(Collections.singleton(externalUser1));
    expect(populator.getLdapUsers("user2")).andReturn(Collections.EMPTY_SET);
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    populator.synchronizeLdapUsers(createSet("user1", "user2", "xuser*"), new LdapBatchDto());
  }

  @Test
  public void testRefreshGroupMembers() throws Exception {

    User user1 = createNiceMock(User.class);
    User user2 = createNiceMock(User.class);
    User user3 = createNiceMock(User.class);
    User user4 = createNiceMock(User.class);
    expect(user1.getUserName()).andReturn("user1").anyTimes();
    expect(user2.getUserName()).andReturn("user2").anyTimes();
    expect(user3.getUserName()).andReturn("user3").anyTimes();
    expect(user4.getUserName()).andReturn("user4").anyTimes();
    expect(user1.isLdapUser()).andReturn(false).anyTimes();
    expect(user2.isLdapUser()).andReturn(true).anyTimes();
    expect(user3.isLdapUser()).andReturn(true).anyTimes();
    expect(user4.isLdapUser()).andReturn(false).anyTimes();

    Group group1 = createNiceMock(Group.class);
    Group group2 = createNiceMock(Group.class);
    expect(group1.isLdapGroup()).andReturn(true).anyTimes();
    expect(group2.isLdapGroup()).andReturn(true).anyTimes();
    expect(group1.getGroupName()).andReturn("group1").anyTimes();
    expect(group2.getGroupName()).andReturn("group2").anyTimes();
    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(ldapServerProperties.getGroupNamingAttr()).andReturn("cn").anyTimes();
    expect(ldapServerProperties.getUsernameAttribute()).andReturn("uid").anyTimes();
    replay(ldapTemplate, ldapServerProperties, users, configuration);
    replay(user1, user2, user3, user4);
    replay(group1, group2);

    AmbariLdapDataPopulatorTestInstance populator = createMockBuilder(AmbariLdapDataPopulatorTestInstance.class)
      .addMockedMethod("getLdapUserByMemberAttr")
      .addMockedMethod("getLdapGroupByMemberAttr")
      .addMockedMethod("getInternalMembers")
      .withConstructor(configuration, users)
      .createNiceMock();

    LdapGroupDto externalGroup = createNiceMock(LdapGroupDto.class);
    expect(externalGroup.getGroupName()).andReturn("group1").anyTimes();
    expect(externalGroup.getMemberAttributes()).andReturn(createSet("user1", "user2", "user4", "user6")).anyTimes();
    replay(externalGroup);

    Map<String, User> internalMembers = new HashMap<String, User>();
    internalMembers.put("user1", user1);
    internalMembers.put("user3", user3);
    internalMembers.put("user4", user4);

    LdapBatchDto batchInfo = new LdapBatchDto();
    LdapUserDto externalUser1 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser2 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser3 = createNiceMock(LdapUserDto.class);
    LdapUserDto externalUser4 = createNiceMock(LdapUserDto.class);
    expect(externalUser1.getUserName()).andReturn("user1").anyTimes();
    expect(externalUser2.getUserName()).andReturn("user2").anyTimes();
    expect(externalUser3.getUserName()).andReturn("user4").anyTimes();
    expect(externalUser4.getUserName()).andReturn("user6").anyTimes();
    replay(externalUser1, externalUser2, externalUser3, externalUser4);
    expect(populator.getLdapUserByMemberAttr("user1")).andReturn(externalUser1).anyTimes();
    expect(populator.getLdapUserByMemberAttr("user2")).andReturn(externalUser2).anyTimes();
    expect(populator.getLdapUserByMemberAttr("user4")).andReturn(null).anyTimes();
    expect(populator.getLdapGroupByMemberAttr("user4")).andReturn(externalGroup).anyTimes();
    expect(populator.getLdapUserByMemberAttr("user6")).andReturn(externalUser4).anyTimes();
    expect(populator.getInternalMembers("group1")).andReturn(internalMembers).anyTimes();
    replay(populator);

    populator.setLdapTemplate(ldapTemplate);
    populator.setLdapServerProperties(ldapServerProperties);

    Map<String, User> internalUsers = new HashMap<String, User>();
    internalUsers.putAll(internalMembers);
    internalUsers.put("user2", user2);
    Map<String, Group> internalGroups = new HashMap<String, Group>();
    internalGroups.put("group2", group2);

    populator.refreshGroupMembers(batchInfo, externalGroup, internalUsers, internalGroups, null);

    Set<String> groupMembersToAdd = new HashSet<String>();
    for (LdapUserGroupMemberDto ldapUserGroupMemberDto : batchInfo.getMembershipToAdd()) {
      groupMembersToAdd.add(ldapUserGroupMemberDto.getUserName());
    }
    assertEquals(3, groupMembersToAdd.size());
    assertTrue(groupMembersToAdd.contains("user2"));
    assertTrue(groupMembersToAdd.contains("user6"));
    Set<String> groupMembersToRemove = new HashSet<String>();
    for (LdapUserGroupMemberDto ldapUserGroupMemberDto : batchInfo.getMembershipToRemove()) {
      groupMembersToRemove.add(ldapUserGroupMemberDto.getUserName());
    }
    assertEquals(2, groupMembersToRemove.size());
    assertTrue(groupMembersToRemove.contains("user3"));
    assertEquals(1, batchInfo.getUsersToBeCreated().size());
    assertTrue(batchInfo.getUsersToBeCreated().contains("user6"));
    assertEquals(1, batchInfo.getUsersToBecomeLdap().size());
    assertTrue(batchInfo.getUsersToBecomeLdap().contains("user1"));
    assertTrue(!batchInfo.getUsersToBecomeLdap().contains("user4"));
    assertTrue(batchInfo.getGroupsToBecomeLdap().isEmpty());
    assertEquals(1, batchInfo.getGroupsToBeCreated().size());
    assertTrue(batchInfo.getGroupsToBeRemoved().isEmpty());
    assertTrue(batchInfo.getUsersToBeRemoved().isEmpty());
    verify(populator.loadLdapTemplate(), populator);
  }

  @Test
  @SuppressWarnings("serial")
  public void testCleanUpLdapUsersWithoutGroup() throws AmbariException {
    final Configuration configuration = createNiceMock(Configuration.class);
    final Users users = createNiceMock(Users.class);

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
    expect(users.getAllUsers()).andReturn(new ArrayList<User>(allUsers));

    final List<User> removedUsers = new ArrayList<User>();
    final Capture<User> userCapture = new Capture<User>();
    users.removeUser(capture(userCapture));
    expectLastCall().andAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        removedUsers.add(userCapture.getValue());
        allUsers.remove(userCapture.getValue());
        return null;
      }
    });

    replay(users);

    final AmbariLdapDataPopulatorTestInstance populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);
    populator.setLdapTemplate(createNiceMock(LdapTemplate.class));
    populator.setLdapServerProperties(createNiceMock(LdapServerProperties.class));
    populator.cleanUpLdapUsersWithoutGroup();

    assertEquals(removedUsers.size(), 1);
    assertEquals(allUsers.size(), 3);
    assertTrue(allUsers.contains(ldapUserWithGroup));
    assertTrue(allUsers.contains(localUserWithoutGroup));
    assertTrue(allUsers.contains(localUserWithGroup));
    assertEquals(removedUsers.get(0), ldapUserWithoutGroup);

    verify(users);
  }

  @Test
  public void testGetLdapUserByMemberAttr() throws Exception {

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    Capture<ContextMapper> contextMapperCapture = new Capture<ContextMapper>();
    Capture<SearchControls> searchControlsCapture = new Capture<SearchControls>();
    PagedResultsDirContextProcessor processor = createNiceMock(PagedResultsDirContextProcessor.class);
    PagedResultsCookie cookie = createNiceMock(PagedResultsCookie.class);
    LdapUserDto dto = new LdapUserDto();

    List<LdapUserDto> list = new LinkedList<LdapUserDto>();
    list.add(dto);

    expect(configuration.getLdapServerProperties()).andReturn(ldapServerProperties).anyTimes();
    expect(ldapServerProperties.isPaginationEnabled()).andReturn(true).anyTimes();
    expect(ldapServerProperties.getUserObjectClass()).andReturn("objectClass").anyTimes();
    expect(ldapServerProperties.getDnAttribute()).andReturn("dn").anyTimes();
    expect(ldapServerProperties.getBaseDN()).andReturn("baseDN").anyTimes();
    expect(ldapServerProperties.getUsernameAttribute()).andReturn("uid").anyTimes();
    expect(processor.getCookie()).andReturn(cookie).anyTimes();
    expect(cookie.getCookie()).andReturn(null).anyTimes();

    expect(ldapTemplate.search(eq("baseDN"), eq("(&(objectClass=objectClass)(uid=foo))"), anyObject(SearchControls.class), capture(contextMapperCapture), eq(processor))).andReturn(list);

    replay(ldapTemplate, ldapServerProperties, users, configuration, processor, cookie);

    AmbariLdapDataPopulatorTestInstance populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);

    populator.setLdapTemplate(ldapTemplate);
    populator.setProcessor(processor);

    assertEquals(dto, populator.getLdapUserByMemberAttr("foo"));

    verify(ldapTemplate, ldapServerProperties, users, configuration, processor, cookie);
  }

  @Test
  public void testGetLdapUserByMemberAttrNoPagination() throws Exception {

    Configuration configuration = createNiceMock(Configuration.class);
    Users users = createNiceMock(Users.class);
    LdapTemplate ldapTemplate = createNiceMock(LdapTemplate.class);
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    Capture<ContextMapper> contextMapperCapture = new Capture<ContextMapper>();
    Capture<SearchControls> searchControlsCapture = new Capture<SearchControls>();
    PagedResultsDirContextProcessor processor = createNiceMock(PagedResultsDirContextProcessor.class);
    PagedResultsCookie cookie = createNiceMock(PagedResultsCookie.class);
    LdapUserDto dto = new LdapUserDto();

    List<LdapUserDto> list = new LinkedList<LdapUserDto>();
    list.add(dto);

    expect(configuration.getLdapServerProperties()).andReturn(ldapServerProperties).anyTimes();
    expect(ldapServerProperties.isPaginationEnabled()).andReturn(false).anyTimes();
    expect(ldapServerProperties.getUserObjectClass()).andReturn("objectClass").anyTimes();
    expect(ldapServerProperties.getUsernameAttribute()).andReturn("uid").anyTimes();
    expect(ldapServerProperties.getDnAttribute()).andReturn("dn").anyTimes();
    expect(ldapServerProperties.getBaseDN()).andReturn("baseDN").anyTimes();

    expect(ldapTemplate.search(eq("baseDN"), eq("(&(objectClass=objectClass)(uid=foo))"), anyObject(SearchControls.class), capture(contextMapperCapture))).andReturn(list);

    replay(ldapTemplate, ldapServerProperties, users, configuration, processor, cookie);

    AmbariLdapDataPopulatorTestInstance populator = new AmbariLdapDataPopulatorTestInstance(configuration, users);

    populator.setLdapTemplate(ldapTemplate);
    populator.setProcessor(processor);

    assertEquals(dto, populator.getLdapUserByMemberAttr("foo"));

    verify(ldapTemplate, ldapServerProperties, users, configuration, processor, cookie);
  }

  @Test
  public void testLdapUserContextMapper_uidIsNull() throws Exception {
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(ldapServerProperties.getUsernameAttribute()).andReturn("cn").once();
    DirContextAdapter adapter = createNiceMock(DirContextAdapter.class);
    expect(adapter.getStringAttribute("cn")).andReturn("testUser");
    expect(adapter.getStringAttribute("uid")).andReturn(null);
    expect(adapter.getNameInNamespace()).andReturn("cn=testUser,ou=Ambari,dc=SME,dc=support,dc=com");

    replay(ldapServerProperties, adapter);

    AmbariLdapDataPopulator.LdapUserContextMapper ldapUserContextMapper = new AmbariLdapDataPopulator.LdapUserContextMapper(ldapServerProperties);
    LdapUserDto userDto = (LdapUserDto) ldapUserContextMapper.mapFromContext(adapter);

    assertNotNull(userDto);
    assertNull(userDto.getUid());
    assertEquals("testuser", userDto.getUserName());
    assertEquals("cn=testuser,ou=ambari,dc=sme,dc=support,dc=com", userDto.getDn());
  }

  @Test
  public void testLdapUserContextMapper_uidAndUsernameAreNull() throws Exception {
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(ldapServerProperties.getUsernameAttribute()).andReturn("cn").once();
    DirContextAdapter adapter = createNiceMock(DirContextAdapter.class);
    expect(adapter.getStringAttribute("cn")).andReturn(null);
    expect(adapter.getStringAttribute("uid")).andReturn(null);

    replay(ldapServerProperties, adapter);

    AmbariLdapDataPopulator.LdapUserContextMapper ldapUserContextMapper = new AmbariLdapDataPopulator.LdapUserContextMapper(ldapServerProperties);

    assertNull(ldapUserContextMapper.mapFromContext(adapter));
  }

  @Test
  public void testLdapUserContextMapper() throws Exception {
    LdapServerProperties ldapServerProperties = createNiceMock(LdapServerProperties.class);
    expect(ldapServerProperties.getUsernameAttribute()).andReturn("cn").once();
    DirContextAdapter adapter = createNiceMock(DirContextAdapter.class);
    expect(adapter.getStringAttribute("cn")).andReturn("testUser");
    expect(adapter.getStringAttribute("uid")).andReturn("UID1");
    expect(adapter.getNameInNamespace()).andReturn("cn=testUser,ou=Ambari,dc=SME,dc=support,dc=com");

    replay(ldapServerProperties, adapter);

    AmbariLdapDataPopulator.LdapUserContextMapper ldapUserContextMapper = new AmbariLdapDataPopulator.LdapUserContextMapper(ldapServerProperties);
    LdapUserDto userDto = (LdapUserDto) ldapUserContextMapper.mapFromContext(adapter);

    assertNotNull(userDto);
    assertEquals("uid1", userDto.getUid());
    assertEquals("testuser", userDto.getUserName());
    assertEquals("cn=testuser,ou=ambari,dc=sme,dc=support,dc=com", userDto.getDn());
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
