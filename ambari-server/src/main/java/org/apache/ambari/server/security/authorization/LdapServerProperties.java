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

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
  private String userSearchBase = "";
  private String usernameAttribute;

  private static final String userSearchFilter = "({attribute}={0})";

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

  public String getUserSearchFilter() {
    return userSearchFilter.replace("{attribute}", usernameAttribute);
  }

  public String getUsernameAttribute() {
    return usernameAttribute;
  }

  public void setUsernameAttribute(String usernameAttribute) {
    this.usernameAttribute = usernameAttribute;
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
    if (userSearchBase != null ? !userSearchBase.equals(that.userSearchBase) : that.userSearchBase != null)
      return false;
    if (usernameAttribute != null ? !usernameAttribute.equals(that.usernameAttribute) : that.usernameAttribute != null)
      return false;

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
    result = 31 * result + (userSearchBase != null ? userSearchBase.hashCode() : 0);
    result = 31 * result + (usernameAttribute != null ? usernameAttribute.hashCode() : 0);
    return result;
  }

}
