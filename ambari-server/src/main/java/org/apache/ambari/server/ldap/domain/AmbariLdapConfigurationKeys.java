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

/**
 * Constants representing supported LDAP related property names
 * // todo extend this with validation information, description, defaults maybe
 */
public enum AmbariLdapConfigurationKeys {

  LDAP_ENABLED("ambari.ldap.authentication.enabled"),
  SERVER_HOST("ambari.ldap.connectivity.server.host"),
  SERVER_PORT("ambari.ldap.connectivity.server.port"),
  USE_SSL("ambari.ldap.connectivity.use_ssl"),

  TRUST_STORE("ambari.ldap.connectivity.trust_store"),
  TRUST_STORE_TYPE("ambari.ldap.connectivity.trust_store.type"),
  TRUST_STORE_PATH("ambari.ldap.connectivity.trust_store.path"),
  TRUST_STORE_PASSWORD("ambari.ldap.connectivity.trust_store.password"),
  ANONYMOUS_BIND("ambari.ldap.connectivity.anonymous_bind"),

  BIND_DN("ambari.ldap.connectivity.bind_dn"),
  BIND_PASSWORD("ambari.ldap.connectivity.bind_password"),

  ATTR_DETECTION("ambari.ldap.attributes.detection"), // manual | auto

  DN_ATTRIBUTE("ambari.ldap.attributes.dn_attr"),

  USER_OBJECT_CLASS("ambari.ldap.attributes.user.object_class"),
  USER_NAME_ATTRIBUTE("ambari.ldap.attributes.user.name_attr"),
  USER_GROUP_MEMBER_ATTRIBUTE("ambari.ldap.attributes.user.group_member_attr"),
  USER_SEARCH_BASE("ambari.ldap.attributes.user.search_base"),

  GROUP_OBJECT_CLASS("ambari.ldap.attributes.group.object_class"),
  GROUP_NAME_ATTRIBUTE("ambari.ldap.attributes.group.name_attr"),
  GROUP_MEMBER_ATTRIBUTE("ambari.ldap.attributes.group.member_attr"),
  GROUP_SEARCH_BASE("ambari.ldap.attributes.group.search_base"),

  USER_SEARCH_FILTER("ambari.ldap.advanced.user_search_filter"),
  USER_MEMBER_REPLACE_PATTERN("ambari.ldap.advanced.user_member_replace_pattern"),
  USER_MEMBER_FILTER("ambari.ldap.advanced.user_member_filter"),

  GROUP_SEARCH_FILTER("ambari.ldap.advanced.group_search_filter"),
  GROUP_MEMBER_REPLACE_PATTERN("ambari.ldap.advanced.group_member_replace_pattern"),
  GROUP_MEMBER_FILTER("ambari.ldap.advanced.group_member_filter"),

  FORCE_LOWERCASE_USERNAMES("ambari.ldap.advanced.force_lowercase_usernames"),
  REFERRAL_HANDLING("ambari.ldap.advanced.referrals"), // folow
  PAGINATION_ENABLED("ambari.ldap.advanced.pagination_enabled"); // true | false

  private String propertyName;

  AmbariLdapConfigurationKeys(String propName) {
    this.propertyName = propName;
  }

  public String key() {
    return this.propertyName;
  }

  public static AmbariLdapConfigurationKeys fromKeyStr(String keyStr) {
    for (AmbariLdapConfigurationKeys key : values()) {
      if (key.key().equals(keyStr)) {
        return key;
      }
    }

    throw new IllegalStateException("invalid konfiguration key found!");

  }
}
