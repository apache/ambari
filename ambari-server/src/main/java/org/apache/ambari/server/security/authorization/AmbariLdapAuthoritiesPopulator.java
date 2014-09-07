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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import com.google.inject.Inject;

/**
 * Provides authorities population for LDAP user from LDAP catalog
 */
public class AmbariLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
  private static final Logger log = LoggerFactory.getLogger(AmbariLdapAuthoritiesPopulator.class);

  private AuthorizationHelper authorizationHelper;
  UserDAO userDAO;
  MemberDAO memberDAO;
  PrivilegeDAO privilegeDAO;

  @Inject
  public AmbariLdapAuthoritiesPopulator(AuthorizationHelper authorizationHelper,
                                        UserDAO userDAO, MemberDAO memberDAO, PrivilegeDAO privilegeDAO) {
    this.authorizationHelper = authorizationHelper;
    this.userDAO = userDAO;
    this.memberDAO = memberDAO;
    this.privilegeDAO = privilegeDAO;
  }

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
    log.info("Get authorities for user " + username + " from local DB");

    UserEntity user;

    user = userDAO.findLdapUserByName(username);
    
    if (user == null) {
      log.error("Can't get authorities for user " + username + ", he is not present in local DB");
      return Collections.emptyList();
    }
    if(!user.getActive()){
      throw new DisabledException("User is disabled");
    }
    // get all of the privileges for the user
    List<PrincipalEntity> principalEntities = new LinkedList<PrincipalEntity>();

    principalEntities.add(user.getPrincipal());

    List<MemberEntity> memberEntities = memberDAO.findAllMembersByUser(user);

    for (MemberEntity memberEntity : memberEntities) {
      principalEntities.add(memberEntity.getGroup().getPrincipal());
    }

    List<PrivilegeEntity> privilegeEntities = privilegeDAO.findAllByPrincipal(principalEntities);

    return authorizationHelper.convertPrivilegesToAuthorities(privilegeEntities);
  }
}
