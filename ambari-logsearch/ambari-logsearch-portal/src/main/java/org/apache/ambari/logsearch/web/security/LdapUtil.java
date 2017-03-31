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

import java.io.IOException;
import java.util.Properties;

import org.apache.ambari.logsearch.common.PropertiesHelper;
import org.apache.ambari.logsearch.common.XMLPropertiesHelper;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

public class LdapUtil {

  private static Logger logger = Logger.getLogger(LdapUtil.class);

  /**
   * Gets parameters of LDAP server to connect to
   *
   * @return LdapServerProperties object representing connection parameters
   */
  public static LdapProperties getLdapServerProperties(Properties properties) {
    LdapProperties ldapServerProperties = new LdapProperties();

    ldapServerProperties.setPrimaryUrl(properties.getProperty(LdapPropertyName.LDAP_PRIMARY_URL_KEY,
      LdapPropertyName.LDAP_PRIMARY_URL_DEFAULT));
    ldapServerProperties.setSecondaryUrl(properties.getProperty(LdapPropertyName.LDAP_SECONDARY_URL_KEY));
    ldapServerProperties.setUseSsl("true".equalsIgnoreCase(properties
      .getProperty(LdapPropertyName.LDAP_USE_SSL_KEY)));
    ldapServerProperties.setAnonymousBind("true".equalsIgnoreCase(properties.getProperty(
      LdapPropertyName.LDAP_BIND_ANONYMOUSLY_KEY, LdapPropertyName.LDAP_BIND_ANONYMOUSLY_DEFAULT)));
    ldapServerProperties.setManagerDn(properties.getProperty(LdapPropertyName.LDAP_MANAGER_DN_KEY));
    String ldapPasswordProperty = properties.getProperty(LdapPropertyName.LDAP_MANAGER_PASSWORD_KEY);
    // TODO read password from password file
    ldapServerProperties.setManagerPassword(ldapPasswordProperty);
    ldapServerProperties.setBaseDN(properties.getProperty(LdapPropertyName.LDAP_BASE_DN_KEY,
      LdapPropertyName.LDAP_BASE_DN_DEFAULT));
    ldapServerProperties.setUsernameAttribute(properties.getProperty(LdapPropertyName.LDAP_USERNAME_ATTRIBUTE_KEY,
      LdapPropertyName.LDAP_USERNAME_ATTRIBUTE_DEFAULT));

    ldapServerProperties.setUserBase(properties.getProperty(LdapPropertyName.LDAP_USER_BASE_KEY,
      LdapPropertyName.LDAP_USER_BASE_DEFAULT));
    ldapServerProperties.setUserObjectClass(properties.getProperty(LdapPropertyName.LDAP_USER_OBJECT_CLASS_KEY,
      LdapPropertyName.LDAP_USER_OBJECT_CLASS_DEFAULT));
    ldapServerProperties.setDnAttribute(properties.getProperty(LdapPropertyName.LDAP_DN_ATTRIBUTE_KEY,
      LdapPropertyName.LDAP_DN_ATTRIBUTE_DEFAULT));

    ldapServerProperties.setGroupBase(properties.getProperty(LdapPropertyName.LDAP_GROUP_BASE_KEY,
      LdapPropertyName.LDAP_GROUP_BASE_DEFAULT));
    ldapServerProperties.setGroupObjectClass(properties.getProperty(LdapPropertyName.LDAP_GROUP_OBJECT_CLASS_KEY,
      LdapPropertyName.LDAP_GROUP_OBJECT_CLASS_DEFAULT));
    ldapServerProperties.setGroupMembershipAttr(properties.getProperty(
      LdapPropertyName.LDAP_GROUP_MEMEBERSHIP_ATTR_KEY, LdapPropertyName.LDAP_GROUP_MEMBERSHIP_ATTR_DEFAULT));
    ldapServerProperties.setGroupNamingAttr(properties.getProperty(LdapPropertyName.LDAP_GROUP_NAMING_ATTR_KEY,
      LdapPropertyName.LDAP_GROUP_NAMING_ATTR_DEFAULT));
    ldapServerProperties.setAdminGroupMappingRules(properties.getProperty(
      LdapPropertyName.LDAP_ADMIN_GROUP_MAPPING_RULES_KEY,
      LdapPropertyName.LDAP_ADMIN_GROUP_MAPPING_RULES_DEFAULT));
    ldapServerProperties.setGroupSearchFilter(properties.getProperty(LdapPropertyName.LDAP_GROUP_SEARCH_FILTER_KEY,
      LdapPropertyName.LDAP_GROUP_SEARCH_FILTER_DEFAULT));
    ldapServerProperties.setReferralMethod(properties.getProperty(LdapPropertyName.LDAP_REFERRAL_KEY,
      LdapPropertyName.LDAP_REFERRAL_DEFAULT));

    if (properties.containsKey(LdapPropertyName.LDAP_GROUP_BASE_KEY)
      || properties.containsKey(LdapPropertyName.LDAP_GROUP_OBJECT_CLASS_KEY)
      || properties.containsKey(LdapPropertyName.LDAP_GROUP_MEMEBERSHIP_ATTR_KEY)
      || properties.containsKey(LdapPropertyName.LDAP_GROUP_NAMING_ATTR_KEY)
      || properties.containsKey(LdapPropertyName.LDAP_ADMIN_GROUP_MAPPING_RULES_KEY)
      || properties.containsKey(LdapPropertyName.LDAP_GROUP_SEARCH_FILTER_KEY)) {
      ldapServerProperties.setGroupMappingEnabled(true);
    }

    return ldapServerProperties;
  }

  /**
   * @return
   */
  public static LdapProperties loadLdapProperties() {
    LdapProperties ldapServerProperties = null;
    String ldapConfigFileName = PropertiesHelper.getProperty("logsearch.login.ldap.config", "logsearch-admin-site.xml");
    Properties props = null;
    ClassPathResource resource = new ClassPathResource(ldapConfigFileName);
    if (resource != null) {
      try {
        props = new Properties();
        new XMLPropertiesHelper().loadFromXml(props, resource.getInputStream());
        ldapServerProperties = getLdapServerProperties(props);
      } catch (IOException e) {
        logger.error("Ldap configudation file loading failed : " + e.getMessage());
      }
    }
    if (ldapServerProperties == null) {
      logger.error("ldapServerProperties object is not created.");
    }
    return ldapServerProperties;
  }

}
