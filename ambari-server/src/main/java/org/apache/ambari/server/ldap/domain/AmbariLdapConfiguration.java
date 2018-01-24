/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.domain;

import static java.lang.Boolean.parseBoolean;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.configuration.LdapUsernameCollisionHandlingBehavior;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.ambari.server.utils.PasswordUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an immutable representation of all the LDAP related
 * configurationMap entries.
 */
public class AmbariLdapConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapConfiguration.class);

  private final Map<String, String> configurationMap;

  private String configValue(AmbariLdapConfigurationKeys ambariLdapConfigurationKeys) {
    final String value;
    if (configurationMap.containsKey(ambariLdapConfigurationKeys.key())) {
      value = configurationMap.get(ambariLdapConfigurationKeys.key());
    } else {
      LOGGER.warn("Ldap configuration property [{}] hasn't been set; using default value", ambariLdapConfigurationKeys.key());
      value = ambariLdapConfigurationKeys.getDefaultValue();
    }
    return value;
  }

  public void setValueFor(AmbariLdapConfigurationKeys ambariLdapConfigurationKeys, String value) {
    configurationMap.put(ambariLdapConfigurationKeys.key(), value);
  }
  
  public AmbariLdapConfiguration() {
    this(new HashMap<>());
  }

  public AmbariLdapConfiguration(Map<String, String> configuration) {
    this.configurationMap = configuration;
  }

  public boolean ldapEnabled() {
    return Boolean.valueOf(configValue(AmbariLdapConfigurationKeys.LDAP_ENABLED));
  }

  public String serverHost() {
    return configValue(AmbariLdapConfigurationKeys.SERVER_HOST);
  }

  public int serverPort() {
    return Integer.valueOf(configValue(AmbariLdapConfigurationKeys.SERVER_PORT));
  }

  public String serverUrl() {
    return serverHost() + ":" + serverPort();
  }

  public String secondaryServerHost() {
    return configValue(AmbariLdapConfigurationKeys.SECONDARY_SERVER_HOST);
  }

  public int secondaryServerPort() {
    final String secondaryServerPort = configValue(AmbariLdapConfigurationKeys.SECONDARY_SERVER_PORT);
    return secondaryServerPort == null ? 0 : Integer.valueOf(secondaryServerPort);
  }

  public String secondaryServerUrl() {
    return secondaryServerHost() + ":" + secondaryServerPort();
  }

  public boolean useSSL() {
    return Boolean.valueOf(configValue(AmbariLdapConfigurationKeys.USE_SSL));
  }

  public String trustStore() {
    return configValue(AmbariLdapConfigurationKeys.TRUST_STORE);
  }

  public String trustStoreType() {
    return configValue(AmbariLdapConfigurationKeys.TRUST_STORE_TYPE);
  }

  public String trustStorePath() {
    return configValue(AmbariLdapConfigurationKeys.TRUST_STORE_PATH);
  }

  public String trustStorePassword() {
    return configValue(AmbariLdapConfigurationKeys.TRUST_STORE_PASSWORD);
  }

  public boolean anonymousBind() {
    return Boolean.valueOf(configValue(AmbariLdapConfigurationKeys.ANONYMOUS_BIND));
  }

  public String bindDn() {
    return configValue(AmbariLdapConfigurationKeys.BIND_DN);
  }

  public String bindPassword() {
    return configValue(AmbariLdapConfigurationKeys.BIND_PASSWORD);
  }

  public String attributeDetection() {
    return configValue(AmbariLdapConfigurationKeys.ATTR_DETECTION);
  }

  public String dnAttribute() {
    return configValue(AmbariLdapConfigurationKeys.DN_ATTRIBUTE);
  }

  public String userObjectClass() {
    return configValue(AmbariLdapConfigurationKeys.USER_OBJECT_CLASS);
  }

  public String userNameAttribute() {
    return configValue(AmbariLdapConfigurationKeys.USER_NAME_ATTRIBUTE);
  }

  public String userSearchBase() {
    return configValue(AmbariLdapConfigurationKeys.USER_SEARCH_BASE);
  }

  public String groupObjectClass() {
    return configValue(AmbariLdapConfigurationKeys.GROUP_OBJECT_CLASS);
  }

  public String groupNameAttribute() {
    return configValue(AmbariLdapConfigurationKeys.GROUP_NAME_ATTRIBUTE);
  }

  public String groupMemberAttribute() {
    return configValue(AmbariLdapConfigurationKeys.GROUP_MEMBER_ATTRIBUTE);
  }

  public String groupSearchBase() {
    return configValue(AmbariLdapConfigurationKeys.GROUP_SEARCH_BASE);
  }

  public String groupMappingRules() {
    return configValue(AmbariLdapConfigurationKeys.GROUP_MAPPING_RULES);
  }

  public String userSearchFilter() {
    return configValue(AmbariLdapConfigurationKeys.USER_SEARCH_FILTER);
  }

  public String userMemberReplacePattern() {
    return configValue(AmbariLdapConfigurationKeys.USER_MEMBER_REPLACE_PATTERN);
  }

  public String userMemberFilter() {
    return configValue(AmbariLdapConfigurationKeys.USER_MEMBER_FILTER);
  }

  public String groupSearchFilter() {
    return configValue(AmbariLdapConfigurationKeys.GROUP_SEARCH_FILTER);
  }

  public String groupMemberReplacePattern() {
    return configValue(AmbariLdapConfigurationKeys.GROUP_MEMBER_REPLACE_PATTERN);
  }

  public String groupMemberFilter() {
    return configValue(AmbariLdapConfigurationKeys.GROUP_MEMBER_FILTER);
  }

  public boolean forceLowerCaseUserNames() {
    return Boolean.valueOf(configValue(AmbariLdapConfigurationKeys.FORCE_LOWERCASE_USERNAMES));
  }

  public boolean paginationEnabled() {
    return Boolean.valueOf(configValue(AmbariLdapConfigurationKeys.PAGINATION_ENABLED));
  }

  public String referralHandling() {
    return configValue(AmbariLdapConfigurationKeys.REFERRAL_HANDLING);
  }

  public Map<String, String> toMap() {
    return (configurationMap == null) ? Collections.emptyMap() : new HashMap<>(configurationMap);
  }

  @Override
  public String toString() {
    return configurationMap.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AmbariLdapConfiguration that = (AmbariLdapConfiguration) o;

    return new EqualsBuilder().append(configurationMap, that.configurationMap).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(configurationMap).toHashCode();
  }

  public boolean isLdapAlternateUserSearchEnabled() {
    return Boolean.valueOf(configValue(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_ENABLED));
  }

  public LdapServerProperties getLdapServerProperties() {
    final LdapServerProperties ldapServerProperties = new LdapServerProperties();

    ldapServerProperties.setPrimaryUrl(serverUrl());
    if (StringUtils.isNotBlank(secondaryServerHost())) {
      ldapServerProperties.setSecondaryUrl(secondaryServerUrl());
    }
    ldapServerProperties.setUseSsl(parseBoolean(configValue(AmbariLdapConfigurationKeys.USE_SSL)));
    ldapServerProperties.setAnonymousBind(parseBoolean(configValue(AmbariLdapConfigurationKeys.ANONYMOUS_BIND)));
    ldapServerProperties.setManagerDn(configValue(AmbariLdapConfigurationKeys.BIND_DN));
    ldapServerProperties.setManagerPassword(PasswordUtils.getInstance().readPassword(configValue(AmbariLdapConfigurationKeys.BIND_PASSWORD), AmbariLdapConfigurationKeys.BIND_PASSWORD.getDefaultValue()));
    ldapServerProperties.setBaseDN(configValue(AmbariLdapConfigurationKeys.USER_SEARCH_BASE));
    ldapServerProperties.setUsernameAttribute(configValue(AmbariLdapConfigurationKeys.USER_NAME_ATTRIBUTE));
    ldapServerProperties.setForceUsernameToLowercase(parseBoolean(configValue(AmbariLdapConfigurationKeys.FORCE_LOWERCASE_USERNAMES)));
    ldapServerProperties.setUserBase(configValue(AmbariLdapConfigurationKeys.USER_BASE));
    ldapServerProperties.setUserObjectClass(configValue(AmbariLdapConfigurationKeys.USER_OBJECT_CLASS));
    ldapServerProperties.setDnAttribute(configValue(AmbariLdapConfigurationKeys.DN_ATTRIBUTE));
    ldapServerProperties.setGroupBase(configValue(AmbariLdapConfigurationKeys.GROUP_BASE));
    ldapServerProperties.setGroupObjectClass(configValue(AmbariLdapConfigurationKeys.GROUP_OBJECT_CLASS));
    ldapServerProperties.setGroupMembershipAttr(configValue(AmbariLdapConfigurationKeys.GROUP_MEMBER_ATTRIBUTE));
    ldapServerProperties.setGroupNamingAttr(configValue(AmbariLdapConfigurationKeys.GROUP_NAME_ATTRIBUTE));
    ldapServerProperties.setAdminGroupMappingRules(configValue(AmbariLdapConfigurationKeys.GROUP_MAPPING_RULES));
    ldapServerProperties.setAdminGroupMappingMemberAttr("");
    ldapServerProperties.setUserSearchFilter(configValue(AmbariLdapConfigurationKeys.USER_SEARCH_FILTER));
    ldapServerProperties.setAlternateUserSearchFilter(configValue(AmbariLdapConfigurationKeys.ALTERNATE_USER_SEARCH_FILTER));
    ldapServerProperties.setGroupSearchFilter(configValue(AmbariLdapConfigurationKeys.GROUP_SEARCH_FILTER));
    ldapServerProperties.setReferralMethod(configValue(AmbariLdapConfigurationKeys.REFERRAL_HANDLING));
    ldapServerProperties.setSyncUserMemberReplacePattern(configValue(AmbariLdapConfigurationKeys.USER_MEMBER_REPLACE_PATTERN));
    ldapServerProperties.setSyncGroupMemberReplacePattern(configValue(AmbariLdapConfigurationKeys.GROUP_MEMBER_REPLACE_PATTERN));
    ldapServerProperties.setSyncUserMemberFilter(configValue(AmbariLdapConfigurationKeys.USER_MEMBER_FILTER));
    ldapServerProperties.setSyncGroupMemberFilter(configValue(AmbariLdapConfigurationKeys.GROUP_MEMBER_FILTER));
    ldapServerProperties.setPaginationEnabled(parseBoolean(configValue(AmbariLdapConfigurationKeys.PAGINATION_ENABLED)));

    if (hasAnyValueWithKey(AmbariLdapConfigurationKeys.GROUP_BASE, AmbariLdapConfigurationKeys.GROUP_OBJECT_CLASS, AmbariLdapConfigurationKeys.GROUP_MEMBER_ATTRIBUTE,
        AmbariLdapConfigurationKeys.GROUP_NAME_ATTRIBUTE, AmbariLdapConfigurationKeys.GROUP_MAPPING_RULES, AmbariLdapConfigurationKeys.GROUP_SEARCH_FILTER)) {
      ldapServerProperties.setGroupMappingEnabled(true);
    }

    return ldapServerProperties;
  }

  private boolean hasAnyValueWithKey(AmbariLdapConfigurationKeys... ambariLdapConfigurationKeys) {
    for (AmbariLdapConfigurationKeys key : ambariLdapConfigurationKeys) {
      if (configurationMap.containsKey(key.key())) {
        return true;
      }
    }
    return false;
  }

  public LdapUsernameCollisionHandlingBehavior syncCollisionHandlingBehavior() {
    if ("skip".equalsIgnoreCase(configValue(AmbariLdapConfigurationKeys.COLLISION_BEHAVIOR))) {
      return LdapUsernameCollisionHandlingBehavior.SKIP;
    }
    return LdapUsernameCollisionHandlingBehavior.CONVERT;
  }

}
