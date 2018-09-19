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
package org.apache.ambari.logsearch.conf;

import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
public class LogSearchLdapAuthConfig {
  @Value("${logsearch.auth.ldap.url:ldap://localhost:389}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.url",
    description = "URL of LDAP database.",
    examples = {"ldap://localhost:389"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapUrl;

  @Value("${logsearch.auth.ldap.manager.dn:cn=admin,dc=planetexpress,dc=com}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.manager.dn",
    description = "DN of the LDAP manger user (it is a must if LDAP groups are used).",
    examples = {"cn=admin,dc=apache,dc=org"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapManagerDn;

  @Value("${logsearch.auth.ldap.manager.password:}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.manager.password",
    description = "Password of the LDAP manager user.",
    examples = {"mypassword"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapManagerPassword;

  @Value("${logsearch.auth.ldap.base.dn:}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.base.dn",
    description = "Base DN of LDAP database.",
    examples = {"dc=apache,dc=org"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapBaseDn;

  @Value("${logsearch.auth.ldap.user.dn.pattern:}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.user.dn.pattern",
    description = "DN pattern that is used during login (dn should contain the username), can be used instead of user filter",
    examples = {"uid={0},ou=people"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapUserDnPattern;

  @Value("${logsearch.auth.ldap.user.search.base:}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.user.search.base",
    description = "User search base for user search filter",
    examples = {"ou=people"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapUserSearchBase;

  @Value("${logsearch.auth.ldap.user.search.filter:}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.user.search.filter",
    description = "Used for get a user based on on LDAP search (username is the input), if it is empty, user dn pattern is used.",
    examples = {"uid={0}"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapUserSearchFilter;

  @Value("${logsearch.auth.ldap.group.search.base:ou=people}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.group.search.base",
    description = "Group search base - defines where to find LDAP groups. Won't do any authority/role mapping if this field is empty.",
    examples = {"ou=people"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapGroupSearchBase;

  @Value("${logsearch.auth.ldap.group.search.filter:(member={0})}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.group.search.filter",
    description = "Group search filter which is used to get membership data for a specific user",
    examples = {"(memberUid={0})"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapGroupSearchFilter;

  @Value("${logsearch.auth.ldap.group.role.attribute:cn}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.group.role.attribute",
    description = "Attribute for identifying LDAP groups (group name)",
    examples = {"cn"},
    defaultValue = "cn",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapGroupRoleAttribute;

  @Value("${logsearch.auth.ldap.role.prefix:ROLE_}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.role.prefix",
    description = "Role prefix that is added for LDAP groups (as authorities)",
    examples = {"ROLE_"},
    defaultValue = "ROLE_",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapRolePrefix;

  @Value("${logsearch.auth.ldap.password.attribute:userPassword}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.password.attribute",
    description = "Password attribute for LDAP authentication",
    examples = {"password"},
    defaultValue = "userPassword",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String ldapPasswordAttribute;

  @Value("#{propertiesSplitter.parseMap('${logsearch.auth.ldap.group.role.map:ship_crew:ROLE_USER}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.group.role.map",
    description = "Map of LDAP groups to Log Search roles",
    examples = {"ROLE_CUSTOM1:ROLE_USER,ROLE_CUSTOM2:ROLE_ADMIN"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, String> ldapGroupRoleMap;

  @Value("${logsearch.auth.ldap.referral.method:ignore}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.referral.method",
    description = "Set the method to handle referrals for LDAP",
    examples = {"follow"},
    defaultValue = "ignore",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String referralMethod;

  public String getLdapUrl() {
    return ldapUrl;
  }

  public void setLdapUrl(String ldapUrl) {
    this.ldapUrl = ldapUrl;
  }

  public String getLdapBaseDn() {
    return ldapBaseDn;
  }

  public void setLdapBaseDn(String ldapBaseDn) {
    this.ldapBaseDn = ldapBaseDn;
  }

  public String getLdapUserDnPattern() {
    return ldapUserDnPattern;
  }

  public void setLdapUserDnPattern(String ldapUserDnPattern) {
    this.ldapUserDnPattern = ldapUserDnPattern;
  }

  public String getLdapUserSearchBase() {
    return ldapUserSearchBase;
  }

  public void setLdapUserSearchBase(String ldapUserSearchBase) {
    this.ldapUserSearchBase = ldapUserSearchBase;
  }

  public String getLdapUserSearchFilter() {
    return ldapUserSearchFilter;
  }

  public void setLdapUserSearchFilter(String ldapUserSearchFilter) {
    this.ldapUserSearchFilter = ldapUserSearchFilter;
  }

  public String getLdapGroupSearchBase() {
    return ldapGroupSearchBase;
  }

  public void setLdapGroupSearchBase(String ldapGroupSearchBase) {
    this.ldapGroupSearchBase = ldapGroupSearchBase;
  }

  public String getLdapGroupSearchFilter() {
    return ldapGroupSearchFilter;
  }

  public void setLdapGroupSearchFilter(String ldapGroupSearchFilter) {
    this.ldapGroupSearchFilter = ldapGroupSearchFilter;
  }

  public String getLdapGroupRoleAttribute() {
    return ldapGroupRoleAttribute;
  }

  public void setLdapGroupRoleAttribute(String ldapGroupRoleAttribute) {
    this.ldapGroupRoleAttribute = ldapGroupRoleAttribute;
  }

  public String getLdapRolePrefix() {
    return ldapRolePrefix;
  }

  public void setLdapRolePrefix(String ldapRolePrefix) {
    this.ldapRolePrefix = ldapRolePrefix;
  }

  public String getLdapPasswordAttribute() {
    return ldapPasswordAttribute;
  }

  public void setLdapPasswordAttribute(String ldapPasswordAttribute) {
    this.ldapPasswordAttribute = ldapPasswordAttribute;
  }

  public String getLdapManagerDn() {
    return ldapManagerDn;
  }

  public void setLdapManagerDn(String ldapManagerDn) {
    this.ldapManagerDn = ldapManagerDn;
  }

  public String getLdapManagerPassword() {
    return ldapManagerPassword;
  }

  public void setLdapManagerPassword(String ldapManagerPassword) {
    this.ldapManagerPassword = ldapManagerPassword;
  }

  public Map<String, String> getLdapGroupRoleMap() {
    return ldapGroupRoleMap;
  }

  public void setLdapGroupRoleMap(Map<String, String> ldapGroupRoleMap) {
    this.ldapGroupRoleMap = ldapGroupRoleMap;
  }

  public String getReferralMethod() {
    return referralMethod;
  }

  public void setReferralMethod(String referralMethod) {
    this.referralMethod = referralMethod;
  }
}
