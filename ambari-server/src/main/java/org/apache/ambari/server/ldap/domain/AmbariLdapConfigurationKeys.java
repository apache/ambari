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

import static org.apache.ambari.server.configuration.ConfigurationPropertyType.PASSWORD;
import static org.apache.ambari.server.configuration.ConfigurationPropertyType.PLAINTEXT;

import org.apache.ambari.server.configuration.ConfigurationPropertyType;

/**
 * Constants representing supported LDAP related property names
 * // TODO: extend this with validation information, description, defaults maybe
 */
public enum AmbariLdapConfigurationKeys {

  LDAP_ENABLED("ambari.ldap.authentication.enabled", PLAINTEXT),
  SERVER_HOST("ambari.ldap.connectivity.server.host", PLAINTEXT),
  SERVER_PORT("ambari.ldap.connectivity.server.port", PLAINTEXT),
  USE_SSL("ambari.ldap.connectivity.use_ssl", PLAINTEXT),

  TRUST_STORE("ambari.ldap.connectivity.trust_store", PLAINTEXT),
  TRUST_STORE_TYPE("ambari.ldap.connectivity.trust_store.type", PLAINTEXT),
  TRUST_STORE_PATH("ambari.ldap.connectivity.trust_store.path", PLAINTEXT),
  TRUST_STORE_PASSWORD("ambari.ldap.connectivity.trust_store.password", PASSWORD),
  ANONYMOUS_BIND("ambari.ldap.connectivity.anonymous_bind", PLAINTEXT),

  BIND_DN("ambari.ldap.connectivity.bind_dn", PLAINTEXT),
  BIND_PASSWORD("ambari.ldap.connectivity.bind_password", PASSWORD),

  ATTR_DETECTION("ambari.ldap.attributes.detection", PLAINTEXT), // manual | auto

  DN_ATTRIBUTE("ambari.ldap.attributes.dn_attr", PLAINTEXT),

  USER_OBJECT_CLASS("ambari.ldap.attributes.user.object_class", PLAINTEXT),
  USER_NAME_ATTRIBUTE("ambari.ldap.attributes.user.name_attr", PLAINTEXT),
  USER_GROUP_MEMBER_ATTRIBUTE("ambari.ldap.attributes.user.group_member_attr", PLAINTEXT),
  USER_SEARCH_BASE("ambari.ldap.attributes.user.search_base", PLAINTEXT),

  GROUP_OBJECT_CLASS("ambari.ldap.attributes.group.object_class", PLAINTEXT),
  GROUP_NAME_ATTRIBUTE("ambari.ldap.attributes.group.name_attr", PLAINTEXT),
  GROUP_MEMBER_ATTRIBUTE("ambari.ldap.attributes.group.member_attr", PLAINTEXT),
  GROUP_SEARCH_BASE("ambari.ldap.attributes.group.search_base", PLAINTEXT),

  USER_SEARCH_FILTER("ambari.ldap.advanced.user_search_filter", PLAINTEXT),
  USER_MEMBER_REPLACE_PATTERN("ambari.ldap.advanced.user_member_replace_pattern", PLAINTEXT),
  USER_MEMBER_FILTER("ambari.ldap.advanced.user_member_filter", PLAINTEXT),

  GROUP_SEARCH_FILTER("ambari.ldap.advanced.group_search_filter", PLAINTEXT),
  GROUP_MEMBER_REPLACE_PATTERN("ambari.ldap.advanced.group_member_replace_pattern", PLAINTEXT),
  GROUP_MEMBER_FILTER("ambari.ldap.advanced.group_member_filter", PLAINTEXT),

  FORCE_LOWERCASE_USERNAMES("ambari.ldap.advanced.force_lowercase_usernames", PLAINTEXT),
  REFERRAL_HANDLING("ambari.ldap.advanced.referrals", PLAINTEXT), // folow
  PAGINATION_ENABLED("ambari.ldap.advanced.pagination_enabled", PLAINTEXT); // true | false

  private String propertyName;
  private ConfigurationPropertyType configurationPropertyType;

  AmbariLdapConfigurationKeys(String propName, ConfigurationPropertyType configurationPropertyType) {
    this.propertyName = propName;
    this.configurationPropertyType = configurationPropertyType;
  }

  public String key() {
    return this.propertyName;
  }

  public ConfigurationPropertyType getConfigurationPropertyType() {
    return configurationPropertyType;
  }

  public static AmbariLdapConfigurationKeys fromKeyStr(String keyStr) {
    for (AmbariLdapConfigurationKeys key : values()) {
      if (key.key().equals(keyStr)) {
        return key;
      }
    }

    throw new IllegalStateException("invalid configuration key found!");

  }
}
