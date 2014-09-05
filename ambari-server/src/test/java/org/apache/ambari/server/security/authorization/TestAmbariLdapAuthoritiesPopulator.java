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

import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ldap.core.DirContextOperations;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import static org.easymock.EasyMock.*;

public class TestAmbariLdapAuthoritiesPopulator extends EasyMockSupport {

  AuthorizationHelper helper = new AuthorizationHelper();
  UserDAO userDAO = createMock(UserDAO.class);
  MemberDAO memberDAO = createMock(MemberDAO.class);
  PrivilegeDAO privilegeDAO = createMock(PrivilegeDAO.class);
  DirContextOperations userData = createMock(DirContextOperations.class);
  UserEntity userEntity = createMock(UserEntity.class);
  PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
  PrincipalEntity groupPrincipalEntity = createMock(PrincipalEntity.class);
  MemberEntity memberEntity = createMock(MemberEntity.class);
  GroupEntity groupEntity = createMock(GroupEntity.class);
  PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);

  @Before
  public void setUp() throws Exception {
    resetAll();
  }

  @Test
  public void testGetGrantedAuthorities_mappingDisabled() throws Exception {
    String username = "user";

    AmbariLdapAuthoritiesPopulator populator = createMockBuilder(AmbariLdapAuthoritiesPopulator.class)
        .withConstructor(helper, userDAO, memberDAO, privilegeDAO).createMock();

    expect(userEntity.getPrincipal()).andReturn(principalEntity);
    expect(userEntity.getActive()).andReturn(true);
    expect(memberDAO.findAllMembersByUser(userEntity)).andReturn(Collections.singletonList(memberEntity));
    expect(memberEntity.getGroup()).andReturn(groupEntity);
    expect(groupEntity.getPrincipal()).andReturn(groupPrincipalEntity);
    List<PrincipalEntity> principalEntityList = new LinkedList<PrincipalEntity>();
    principalEntityList.add(principalEntity);
    principalEntityList.add(groupPrincipalEntity);
    expect(privilegeDAO.findAllByPrincipal(principalEntityList)).andReturn(Collections.singletonList(privilegeEntity));

    expect(userDAO.findLdapUserByName(username)).andReturn(userEntity);
    replayAll();

    populator.getGrantedAuthorities(userData, username);

    verifyAll();

  }

  @Test
  public void testGetGrantedAuthorities_mappingEnabled() throws Exception {

    AmbariLdapAuthoritiesPopulator populator = createMockBuilder(AmbariLdapAuthoritiesPopulator.class)
        .withConstructor(helper, userDAO, memberDAO, privilegeDAO).createMock();

    expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(userEntity.getActive()).andReturn(true);
    expect(memberDAO.findAllMembersByUser(userEntity)).andReturn(Collections.singletonList(memberEntity)).anyTimes();
    expect(memberEntity.getGroup()).andReturn(groupEntity).anyTimes();
    expect(groupEntity.getPrincipal()).andReturn(groupPrincipalEntity).anyTimes();
    List<PrincipalEntity> principalEntityList = new LinkedList<PrincipalEntity>();
    principalEntityList.add(principalEntity);
    principalEntityList.add(groupPrincipalEntity);
    expect(privilegeDAO.findAllByPrincipal(principalEntityList)).andReturn(Collections.singletonList(privilegeEntity)).anyTimes();

    expect(userDAO.findLdapUserByName(EasyMock.<String> anyObject())).andReturn(null).andReturn(userEntity).once();

    replayAll();

    //test with admin user
    populator.getGrantedAuthorities(userData, "admin");
    //test with non-admin
    populator.getGrantedAuthorities(userData, "user");

    verifyAll();
  }

}
