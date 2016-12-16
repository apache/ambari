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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Describes LDAP Server connection parameters
 */
public class LdapServerProperties {

  private String primaryUrl;
  private String secondaryUrl;
  private boolean useSsl;
  private boolean anonymousBind;
  private String managerDn;
  private String managerPassword;
  private String baseDN;
  private String dnAttribute;
  private String referralMethod;

  //LDAP group properties
  private String groupBase;
  private String groupObjectClass;
  private String groupMembershipAttr;
  private String groupNamingAttr;
  private String adminGroupMappingRules;
  private boolean groupMappingEnabled;

  //LDAP user properties
  private String userBase;
  private String userObjectClass;
  private String usernameAttribute;
  private boolean forceUsernameToLowercase = false;
  private String userSearchBase = "";

  private String syncGroupMemberReplacePattern = "";
  private String syncUserMemberReplacePattern = "";

  private String groupSearchFilter;
  private String userSearchFilter;
  private String alternateUserSearchFilter; // alternate user search filter to be used when users use their alternate login id (e.g. User Principal Name)

  private String syncUserMemberFilter = "";
  private String syncGroupMemberFilter = "";
  //LDAP pagination properties
  private boolean paginationEnabled = true;
  private String adminGroupMappingMemberAttr = ""; // custom group search filter for admin mappings

  public List<String> getLdapUrls() {
    String protocol = useSsl ? "ldaps://" : "ldap://";

    if (StringUtils.isEmpty(primaryUrl)) {
      return Collections.emptyList();
    } else {
      List<String> list = new ArrayList<String>();
      list.add(protocol + primaryUrl);
      if (!StringUtils.isEmpty(secondaryUrl)) {
        list.add(protocol + secondaryUrl);
      }
      return list;
    }
  }

  public String getPrimaryUrl() {
    return primaryUrl;
  }

  public void setPrimaryUrl(String primaryUrl) {
    this.primaryUrl = primaryUrl;
  }

  public String getSecondaryUrl() {
    return secondaryUrl;
  }

  public void setSecondaryUrl(String secondaryUrl) {
    this.secondaryUrl = secondaryUrl;
  }

  public boolean isUseSsl() {
    return useSsl;
  }

  public void setUseSsl(boolean useSsl) {
    this.useSsl = useSsl;
  }

  public boolean isAnonymousBind() {
    return anonymousBind;
  }

  public void setAnonymousBind(boolean anonymousBind) {
    this.anonymousBind = anonymousBind;
  }

  public String getManagerDn() {
    return managerDn;
  }

  public void setManagerDn(String managerDn) {
    this.managerDn = managerDn;
  }

  public String getManagerPassword() {
    return managerPassword;
  }

  public void setManagerPassword(String managerPassword) {
    this.managerPassword = managerPassword;
  }

  public String getBaseDN() {
    return baseDN;
  }

  public void setBaseDN(String baseDN) {
    this.baseDN = baseDN;
  }

  public String getUserSearchBase() {
    return userSearchBase;
  }

  public void setUserSearchBase(String userSearchBase) {
    this.userSearchBase = userSearchBase;
  }

  /**
   * Returns the LDAP filter to search users by.
   * @param useAlternateUserSearchFilter if true than return LDAP filter that expects user name in
   *                                  User Principal Name format to filter users constructed from {@value org.apache.ambari.server.configuration.Configuration#LDAP_ALT_USER_SEARCH_FILTER_KEY}.
   *                                  Otherwise the filter is constructed from {@value org.apache.ambari.server.configuration.Configuration#LDAP_USER_SEARCH_FILTER_KEY}
   * @return the LDAP filter string
   */
  public String getUserSearchFilter(boolean useAlternateUserSearchFilter) {
    String filter = useAlternateUserSearchFilter ? alternateUserSearchFilter : userSearchFilter;

    return resolveUserSearchFilterPlaceHolders(filter);
  }

  public String getUsernameAttribute() {
    return usernameAttribute;
  }

  public void setUsernameAttribute(String usernameAttribute) {
    this.usernameAttribute = usernameAttribute;
  }

  /**
   * Sets whether the username retrieved from the LDAP server during authentication is to be forced
   * to all lowercase characters before assigning to the authenticated user.
   *
   * @param forceUsernameToLowercase true to force the username to be lowercase; false to leave as
   *                                 it was when retrieved from the LDAP server
   */
  public void setForceUsernameToLowercase(boolean forceUsernameToLowercase) {
    this.forceUsernameToLowercase = forceUsernameToLowercase;
  }

  /**
   * Gets whether the username retrieved from the LDAP server during authentication is to be forced
   * to all lowercase characters before assigning to the authenticated user.
   *
   * @return true to force the username to be lowercase; false to leave as it was when retrieved from
   * the LDAP server
   */
  public boolean isForceUsernameToLowercase() {
    return forceUsernameToLowercase;
  }

  public String getGroupBase() {
    return groupBase;
  }

  public void setGroupBase(String groupBase) {
    this.groupBase = groupBase;
  }

  public String getGroupObjectClass() {
    return groupObjectClass;
  }

  public void setGroupObjectClass(String groupObjectClass) {
    this.groupObjectClass = groupObjectClass;
  }

  public String getGroupMembershipAttr() {
    return groupMembershipAttr;
  }

  public void setGroupMembershipAttr(String groupMembershipAttr) {
    this.groupMembershipAttr = groupMembershipAttr;
  }

  public String getGroupNamingAttr() {
    return groupNamingAttr;
  }

  public void setGroupNamingAttr(String groupNamingAttr) {
    this.groupNamingAttr = groupNamingAttr;
  }

  public String getAdminGroupMappingRules() {
    return adminGroupMappingRules;
  }

  public void setAdminGroupMappingRules(String adminGroupMappingRules) {
    this.adminGroupMappingRules = adminGroupMappingRules;
  }

  public String getGroupSearchFilter() {
    return groupSearchFilter;
  }

  public void setGroupSearchFilter(String groupSearchFilter) {
    this.groupSearchFilter = groupSearchFilter;
  }


  public void setUserSearchFilter(String userSearchFilter) {
    this.userSearchFilter = userSearchFilter;
  }

  public void setAlternateUserSearchFilter(String alternateUserSearchFilter) {
    this.alternateUserSearchFilter = alternateUserSearchFilter;
  }

  public boolean isGroupMappingEnabled() {
    return groupMappingEnabled;
  }

  public void setGroupMappingEnabled(boolean groupMappingEnabled) {
    this.groupMappingEnabled = groupMappingEnabled;
  }

  public void setUserBase(String userBase) {
    this.userBase = userBase;
  }

  public void setUserObjectClass(String userObjectClass) {
    this.userObjectClass = userObjectClass;
  }

  public String getUserBase() {
    return userBase;
  }

  public String getUserObjectClass() {
    return userObjectClass;
  }

  public String getDnAttribute() {
    return dnAttribute;
  }

  public void setDnAttribute(String dnAttribute) {
    this.dnAttribute = dnAttribute;
  }

  public void setReferralMethod(String referralMethod) {
    this.referralMethod = referralMethod;
  }

  public String getReferralMethod() {
    return referralMethod;
  }

  public boolean isPaginationEnabled() {
    return paginationEnabled;
  }

  public void setPaginationEnabled(boolean paginationEnabled) {
    this.paginationEnabled = paginationEnabled;
  }

  public String getSyncGroupMemberReplacePattern() {
    return syncGroupMemberReplacePattern;
  }

  public void setSyncGroupMemberReplacePattern(String syncGroupMemberReplacePattern) {
    this.syncGroupMemberReplacePattern = syncGroupMemberReplacePattern;
  }

  public String getSyncUserMemberReplacePattern() {
    return syncUserMemberReplacePattern;
  }

  public void setSyncUserMemberReplacePattern(String syncUserMemberReplacePattern) {
    this.syncUserMemberReplacePattern = syncUserMemberReplacePattern;
  }

  public String getSyncUserMemberFilter() {
    return syncUserMemberFilter;
  }

  public void setSyncUserMemberFilter(String syncUserMemberFilter) {
    this.syncUserMemberFilter = syncUserMemberFilter;
  }

  public String getSyncGroupMemberFilter() {
    return syncGroupMemberFilter;
  }

  public void setSyncGroupMemberFilter(String syncGroupMemberFilter) {
    this.syncGroupMemberFilter = syncGroupMemberFilter;
  }

  public String getAdminGroupMappingMemberAttr() {
    return adminGroupMappingMemberAttr;
  }

  public void setAdminGroupMappingMemberAttr(String adminGroupMappingMemberAttr) {
    this.adminGroupMappingMemberAttr = adminGroupMappingMemberAttr;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    LdapServerProperties that = (LdapServerProperties) obj;

    if (primaryUrl != null ? !primaryUrl.equals(that.primaryUrl) : that.primaryUrl != null) return false;
    if (secondaryUrl != null ? !secondaryUrl.equals(that.secondaryUrl) : that.secondaryUrl != null) return false;
    if (useSsl!=that.useSsl) return false;
    if (anonymousBind!=that.anonymousBind) return false;
    if (managerDn != null ? !managerDn.equals(that.managerDn) : that.managerDn != null) return false;
    if (managerPassword != null ? !managerPassword.equals(that.managerPassword) : that.managerPassword != null)
      return false;
    if (baseDN != null ? !baseDN.equals(that.baseDN) : that.baseDN != null) return false;
    if (userBase != null ? !userBase.equals(that.userBase) : that.userBase != null)
      return false;
    if (userObjectClass != null ? !userObjectClass.equals(that.userObjectClass) : that.userObjectClass != null)
      return false;
    if (usernameAttribute != null ? !usernameAttribute.equals(that.usernameAttribute) : that.usernameAttribute != null)
      return false;
    if (forceUsernameToLowercase != that.forceUsernameToLowercase)
      return false;
    if (groupBase != null ? !groupBase.equals(that.groupBase) :
        that.groupBase != null) return false;
    if (groupObjectClass != null ? !groupObjectClass.equals(that.groupObjectClass) :
        that.groupObjectClass != null) return false;
    if (groupMembershipAttr != null ? !groupMembershipAttr.equals(
        that.groupMembershipAttr) : that.groupMembershipAttr != null) return false;
    if (groupNamingAttr != null ? !groupNamingAttr.equals(that.groupNamingAttr) :
        that.groupNamingAttr != null) return false;
    if (adminGroupMappingRules != null ? !adminGroupMappingRules.equals(
        that.adminGroupMappingRules) : that.adminGroupMappingRules != null) return false;
    if (groupSearchFilter != null ? !groupSearchFilter.equals(
        that.groupSearchFilter) : that.groupSearchFilter != null) return false;
    if (dnAttribute != null ? !dnAttribute.equals(
        that.dnAttribute) : that.dnAttribute != null) return false;
    if (syncGroupMemberReplacePattern != null ? !syncGroupMemberReplacePattern.equals(
      that.syncGroupMemberReplacePattern) : that.syncGroupMemberReplacePattern != null) return false;
    if (syncUserMemberReplacePattern != null ? !syncUserMemberReplacePattern.equals(
      that.syncUserMemberReplacePattern) : that.syncUserMemberReplacePattern != null) return false;
    if (syncUserMemberFilter != null ? !syncUserMemberFilter.equals(
      that.syncUserMemberFilter) : that.syncUserMemberFilter != null) return false;
    if (syncGroupMemberFilter != null ? !syncGroupMemberFilter.equals(
      that.syncGroupMemberFilter) : that.syncGroupMemberFilter != null) return false;
    if (referralMethod != null ? !referralMethod.equals(that.referralMethod) : that.referralMethod != null) return false;

    if (groupMappingEnabled != that.isGroupMappingEnabled()) return false;

    if (paginationEnabled != that.isPaginationEnabled()) return false;

    if (userSearchFilter != null ? !userSearchFilter.equals(that.userSearchFilter) : that.userSearchFilter != null) return false;
    if (alternateUserSearchFilter != null ? !alternateUserSearchFilter.equals(that.alternateUserSearchFilter) : that.alternateUserSearchFilter != null) return false;
    if (adminGroupMappingMemberAttr != null ? !adminGroupMappingMemberAttr.equals(that.adminGroupMappingMemberAttr) : that.adminGroupMappingMemberAttr != null) return false;


    return true;
  }

  @Override
  public int hashCode() {
    int result = primaryUrl != null ? primaryUrl.hashCode() : 0;
    result = 31 * result + (secondaryUrl != null ? secondaryUrl.hashCode() : 0);
    result = 31 * result + (useSsl ? 1 : 0);
    result = 31 * result + (anonymousBind ? 1 : 0);
    result = 31 * result + (managerDn != null ? managerDn.hashCode() : 0);
    result = 31 * result + (managerPassword != null ? managerPassword.hashCode() : 0);
    result = 31 * result + (baseDN != null ? baseDN.hashCode() : 0);
    result = 31 * result + (userBase != null ? userBase.hashCode() : 0);
    result = 31 * result + (userObjectClass != null ? userObjectClass.hashCode() : 0);
    result = 31 * result + (usernameAttribute != null ? usernameAttribute.hashCode() : 0);
    result = 31 * result + (forceUsernameToLowercase ? 1 : 0);
    result = 31 * result + (groupBase != null ? groupBase.hashCode() : 0);
    result = 31 * result + (groupObjectClass != null ? groupObjectClass.hashCode() : 0);
    result = 31 * result + (groupMembershipAttr != null ? groupMembershipAttr.hashCode() : 0);
    result = 31 * result + (groupNamingAttr != null ? groupNamingAttr.hashCode() : 0);
    result = 31 * result + (adminGroupMappingRules != null ? adminGroupMappingRules.hashCode() : 0);
    result = 31 * result + (groupSearchFilter != null ? groupSearchFilter.hashCode() : 0);
    result = 31 * result + (dnAttribute != null ? dnAttribute.hashCode() : 0);
    result = 31 * result + (syncUserMemberReplacePattern != null ? syncUserMemberReplacePattern.hashCode() : 0);
    result = 31 * result + (syncGroupMemberReplacePattern != null ? syncGroupMemberReplacePattern.hashCode() : 0);
    result = 31 * result + (syncUserMemberFilter != null ? syncUserMemberFilter.hashCode() : 0);
    result = 31 * result + (syncGroupMemberFilter != null ? syncGroupMemberFilter.hashCode() : 0);
    result = 31 * result + (referralMethod != null ? referralMethod.hashCode() : 0);
    result = 31 * result + (userSearchFilter != null ? userSearchFilter.hashCode() : 0);
    result = 31 * result + (alternateUserSearchFilter != null ? alternateUserSearchFilter.hashCode() : 0);
    result = 31 * result + (adminGroupMappingMemberAttr != null ? adminGroupMappingMemberAttr.hashCode() : 0);
    return result;
  }

  /**
   * Resolves known placeholders found within the given ldap user search ldap filter
   * @param filter
   * @return returns the filter with the resolved placeholders.
   */
  protected String resolveUserSearchFilterPlaceHolders(String filter) {
    return filter
      .replace("{usernameAttribute}", usernameAttribute)
      .replace("{userObjectClass}", userObjectClass);
  }
}
