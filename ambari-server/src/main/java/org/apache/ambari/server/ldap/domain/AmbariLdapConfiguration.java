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

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * This class is an immutable representation of all the LDAP related configurationMap entries.
 */
public class AmbariLdapConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapConfiguration.class);

  private final Map<String, Object> configurationMap;

  private Object configValue(AmbariLdapConfigKeys ambariLdapConfigKeys) {
    Object value = null;
    if (configurationMap.containsKey(ambariLdapConfigKeys.key())) {
      value = configurationMap.get(ambariLdapConfigKeys.key());
    } else {
      LOGGER.warn("Ldap configuration property [{}] hasn't been set", ambariLdapConfigKeys.key());
    }
    return value;
  }

  public void setValueFor(AmbariLdapConfigKeys ambariLdapConfigKeys, Object value) {
    configurationMap.put(ambariLdapConfigKeys.key(), value);
  }

  // intentionally package private, instances to be created through the factory
  @Inject
  AmbariLdapConfiguration(@Assisted Map<String, Object> configuration) {
    this.configurationMap = configuration;
  }

  public boolean ldapEnabled() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.LDAP_ENABLED));
  }

  public String serverHost() {
    return (String) configValue(AmbariLdapConfigKeys.SERVER_HOST);
  }

  public int serverPort() {
    return Integer.valueOf((String) configValue(AmbariLdapConfigKeys.SERVER_PORT));
  }

  public boolean useSSL() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.USE_SSL));
  }

  public String trustStore() {
    return (String) configValue(AmbariLdapConfigKeys.TRUST_STORE);
  }

  public String trustStoreType() {
    return (String) configValue(AmbariLdapConfigKeys.TRUST_STORE_TYPE);
  }

  public String trustStorePath() {
    return (String) configValue(AmbariLdapConfigKeys.TRUST_STORE_PATH);
  }

  public String trustStorePassword() {
    return (String) configValue(AmbariLdapConfigKeys.TRUST_STORE_PASSWORD);
  }

  public boolean anonymousBind() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.ANONYMOUS_BIND));
  }

  public String bindDn() {
    return (String) configValue(AmbariLdapConfigKeys.BIND_DN);
  }

  public String bindPassword() {
    return (String) configValue(AmbariLdapConfigKeys.BIND_PASSWORD);
  }

  public String attributeDetection() {
    return (String) configValue(AmbariLdapConfigKeys.ATTR_DETECTION);
  }

  public String dnAttribute() {
    return (String) configValue(AmbariLdapConfigKeys.DN_ATTRIBUTE);
  }

  public String userObjectClass() {
    return (String) configValue(AmbariLdapConfigKeys.USER_OBJECT_CLASS);
  }

  public String userNameAttribute() {
    return (String) configValue(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE);
  }

  public String userSearchBase() {
    return (String) configValue(AmbariLdapConfigKeys.USER_SEARCH_BASE);
  }

  public String groupObjectClass() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_OBJECT_CLASS);
  }

  public String groupNameAttribute() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_NAME_ATTRIBUTE);
  }

  public String groupMemberAttribute() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_MEMBER_ATTRIBUTE);
  }

  public String groupSearchBase() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_SEARCH_BASE);
  }

  public String userSearchFilter() {
    return (String) configValue(AmbariLdapConfigKeys.USER_SEARCH_FILTER);
  }

  public String userMemberReplacePattern() {
    return (String) configValue(AmbariLdapConfigKeys.USER_MEMBER_REPLACE_PATTERN);
  }

  public String userMemberFilter() {
    return (String) configValue(AmbariLdapConfigKeys.USER_MEMBER_FILTER);
  }

  public String groupSearchFilter() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_SEARCH_FILTER);
  }

  public String groupMemberReplacePattern() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_MEMBER_REPLACE_PATTERN);
  }

  public String groupMemberFilter() {
    return (String) configValue(AmbariLdapConfigKeys.GROUP_MEMBER_FILTER);
  }

  public boolean forceLowerCaseUserNames() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.FORCE_LOWERCASE_USERNAMES));
  }

  public boolean paginationEnabled() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfigKeys.PAGINATION_ENABLED));
  }

  public String referralHandling() {
    return (String) configValue(AmbariLdapConfigKeys.REFERRAL_HANDLING);
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

    return new EqualsBuilder()
        .append(configurationMap, that.configurationMap)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(configurationMap)
        .toHashCode();
  }
}
