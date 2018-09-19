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
package org.apache.ambari.logsearch.common;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Class to map multiple LDAP groups to Log Search authorities. (definied in a map)
 * Examples:
 * LDAP person -> ROLE_USER
 * LDAP user -> ROLE_USER
 * LDAP admin -> ROLE_ADMIN
 * ROLE_LDAP_ADMIN -> ROLE_ADMIN
 */
public class LogSearchLdapAuthorityMapper implements GrantedAuthoritiesMapper {

  private static final String ROLE_PREFIX = "ROLE_";

  private final Map<String, String> groupRoleMap;

  public LogSearchLdapAuthorityMapper(Map<String, String> groupRoleMap) {
    this.groupRoleMap = groupRoleMap;
  }

  @Override
  public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
    if (!groupRoleMap.isEmpty() && !authorities.isEmpty()) {
      List<SimpleGrantedAuthority> newAuthorities = new ArrayList<>();
      for (GrantedAuthority authority : authorities) {
        String withoutRoleStringLowercase = StringUtils.removeStart(authority.toString(), ROLE_PREFIX).toLowerCase();
        String withoutRoleStringUppercase = StringUtils.removeStart(authority.toString(), ROLE_PREFIX).toUpperCase();
        String simpleRoleLowercaseString = authority.toString().toLowerCase();
        String simpleRoleUppercaseString = authority.toString().toUpperCase();
        if (addAuthoritiy(newAuthorities, withoutRoleStringLowercase))
          continue;
        if (addAuthoritiy(newAuthorities, withoutRoleStringUppercase))
          continue;
        if (addAuthoritiy(newAuthorities, simpleRoleLowercaseString))
          continue;
        addAuthoritiy(newAuthorities, simpleRoleUppercaseString);
      }
      return newAuthorities;
    }
    return authorities;
  }

  private boolean addAuthoritiy(List<SimpleGrantedAuthority> newAuthorities, String roleKey) {
    if (groupRoleMap.containsKey(roleKey)) {
      String role = groupRoleMap.get(roleKey);
      if (role.contains(ROLE_PREFIX)) {
        if (!containsAuthority(role.toUpperCase(), newAuthorities)) {
          newAuthorities.add(new SimpleGrantedAuthority(role.toUpperCase()));
        }
      } else {
        String finalRole = ROLE_PREFIX + role.toUpperCase();
        if (!containsAuthority(finalRole, newAuthorities)) {
          newAuthorities.add(new SimpleGrantedAuthority(finalRole));
        }
      }
      return true;
    }
    return false;
  }

  private boolean containsAuthority(String authorityStr, List<SimpleGrantedAuthority> authorities) {
    boolean result = false;
    for (SimpleGrantedAuthority authority : authorities) {
      if (authorityStr.equals(authority.toString())) {
        result = true;
        break;
      }
    }
    return result;
  }
}
