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

public class LdapPropertyName {

  public static final String LDAP_USE_SSL_KEY = "authentication.ldap.useSSL";
  public static final String LDAP_PRIMARY_URL_KEY = "authentication.ldap.primaryUrl";
  public static final String LDAP_SECONDARY_URL_KEY = "authentication.ldap.secondaryUrl";
  public static final String LDAP_BASE_DN_KEY = "authentication.ldap.baseDn";
  public static final String LDAP_BIND_ANONYMOUSLY_KEY = "authentication.ldap.bindAnonymously";
  public static final String LDAP_MANAGER_DN_KEY = "authentication.ldap.managerDn";
  public static final String LDAP_MANAGER_PASSWORD_KEY = "authentication.ldap.managerPassword";
  public static final String LDAP_DN_ATTRIBUTE_KEY = "authentication.ldap.dnAttribute";
  public static final String LDAP_USERNAME_ATTRIBUTE_KEY = "authentication.ldap.usernameAttribute";
  public static final String LDAP_USER_BASE_KEY = "authentication.ldap.userBase";
  public static final String LDAP_USER_OBJECT_CLASS_KEY = "authentication.ldap.userObjectClass";
  public static final String LDAP_GROUP_BASE_KEY = "authentication.ldap.groupBase";
  public static final String LDAP_GROUP_OBJECT_CLASS_KEY = "authentication.ldap.groupObjectClass";
  public static final String LDAP_GROUP_NAMING_ATTR_KEY = "authentication.ldap.groupNamingAttr";
  public static final String LDAP_GROUP_MEMEBERSHIP_ATTR_KEY = "authentication.ldap.groupMembershipAttr";
  public static final String LDAP_ADMIN_GROUP_MAPPING_RULES_KEY = "authorization.ldap.adminGroupMappingRules";
  public static final String LDAP_GROUP_SEARCH_FILTER_KEY = "authorization.ldap.groupSearchFilter";
  public static final String LDAP_REFERRAL_KEY = "authentication.ldap.referral";

  // default
  public static final String LDAP_BIND_ANONYMOUSLY_DEFAULT = "true";
  public static final String LDAP_PRIMARY_URL_DEFAULT = "localhost:389";
  public static final String LDAP_BASE_DN_DEFAULT = "dc=example,dc=com";
  public static final String LDAP_USERNAME_ATTRIBUTE_DEFAULT = "uid";
  public static final String LDAP_DN_ATTRIBUTE_DEFAULT = "dn";
  public static final String LDAP_USER_BASE_DEFAULT = "ou=people,dc=example,dc=com";
  public static final String LDAP_USER_OBJECT_CLASS_DEFAULT = "person";
  public static final String LDAP_GROUP_BASE_DEFAULT = "ou=groups,dc=example,dc=com";
  public static final String LDAP_GROUP_OBJECT_CLASS_DEFAULT = "group";
  public static final String LDAP_GROUP_NAMING_ATTR_DEFAULT = "cn";
  public static final String LDAP_GROUP_MEMBERSHIP_ATTR_DEFAULT = "member";
  public static final String LDAP_ADMIN_GROUP_MAPPING_RULES_DEFAULT = "Logsearch Administrators";
  public static final String LDAP_GROUP_SEARCH_FILTER_DEFAULT = "";
  public static final String LDAP_REFERRAL_DEFAULT = "ignore";

}
