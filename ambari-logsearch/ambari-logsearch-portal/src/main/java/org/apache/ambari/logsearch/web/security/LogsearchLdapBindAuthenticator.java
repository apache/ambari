/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.web.security;

import org.apache.log4j.Logger;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.BindAuthenticator;

public class LogsearchLdapBindAuthenticator extends BindAuthenticator {
  private static Logger logger = Logger
    .getLogger(LogsearchLdapBindAuthenticator.class);

  LdapProperties ldapServerProperties;

  public LogsearchLdapBindAuthenticator(
    BaseLdapPathContextSource contextSource,
    LdapProperties ldapServerProperties) {
    super(contextSource);
    this.ldapServerProperties = ldapServerProperties;
    logger.info("LDAP properties=" + ldapServerProperties);
  }

  @Override
  public DirContextOperations authenticate(Authentication authentication) {

    DirContextOperations user = super.authenticate(authentication);

    return setAmbariAdminAttr(user);
  }

  /**
   * Checks whether user is a member of ambari administrators group in LDAP.
   * If yes, sets user's ambari_admin attribute to true
   *
   * @param user
   * @return
   */
  private DirContextOperations setAmbariAdminAttr(DirContextOperations user) {
    String baseDn = ldapServerProperties.getBaseDN().toLowerCase();
    String groupBase = ldapServerProperties.getGroupBase().toLowerCase();
    String groupObjectClass = ldapServerProperties.getGroupObjectClass();
    String groupMembershipAttr = ldapServerProperties
      .getGroupMembershipAttr();
    String adminGroupMappingRules = ldapServerProperties
      .getAdminGroupMappingRules();
    final String groupNamingAttribute = ldapServerProperties
      .getGroupNamingAttr();
    String groupSearchFilter = ldapServerProperties.getGroupSearchFilter();

    // If groupBase is set incorrectly or isn't set - search in BaseDn
    int indexOfBaseDn = groupBase.indexOf(baseDn);
    groupBase = indexOfBaseDn <= 0 ? "" : groupBase.substring(0,
      indexOfBaseDn - 1);

    StringBuilder filterBuilder = new StringBuilder();

    filterBuilder.append("(&(");
    filterBuilder.append(groupMembershipAttr);
    filterBuilder.append("=");
    filterBuilder.append(user.getNameInNamespace());// DN

    if ((groupSearchFilter == null) || groupSearchFilter.equals("")) {
      // If groupSearchFilter is not specified, build it from other
      // authorization
      // group properties
      filterBuilder.append(")(objectclass=");
      filterBuilder.append(groupObjectClass);
      filterBuilder.append(")(|");
      String[] adminGroupMappingRegexs = adminGroupMappingRules
        .split(",");
      for (String adminGroupMappingRegex : adminGroupMappingRegexs) {
        filterBuilder.append("(");
        filterBuilder.append(groupNamingAttribute);
        filterBuilder.append("=");
        filterBuilder.append(adminGroupMappingRegex);
        filterBuilder.append(")");
      }
      filterBuilder.append(")");
    } else {
      filterBuilder.append(")");
      filterBuilder.append(groupSearchFilter);
    }
    filterBuilder.append(")");

    logger.info("filter=" + filterBuilder);
    // TODO: Filter is not used anywhere
    return user;
  }

}
