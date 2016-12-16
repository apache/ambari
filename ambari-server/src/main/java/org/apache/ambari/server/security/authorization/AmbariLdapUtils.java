/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.security.authorization;

import java.util.regex.Pattern;

import javax.naming.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.security.ldap.LdapUtils;

import com.google.common.base.Preconditions;

/**
 * Provides utility methods for LDAP related functionality
 */
public class AmbariLdapUtils {

  private static final Logger LOG = LoggerFactory.getLogger(AmbariLdapUtils.class);

  /**
   * Regexp to verify if user login name beside user contains domain information as well (User principal name format).
   */
  private static final Pattern UPN_FORMAT = Pattern.compile(".+@\\w+(\\.\\w+)*");

  /**
   * Returns true if the given user name contains domain name as well (e.g. username@domain)
   * @param loginName the login name to verify if it contains domain information.
   * @return
   */
  public static boolean isUserPrincipalNameFormat(String loginName) {
    return UPN_FORMAT.matcher(loginName).matches();
  }


  /**
   * Determine that the full DN of an LDAP object is in/out of the base DN scope.
   * @param adapter used for get the full dn from the ldap query response
   * @param baseDn
   * @return
   */
  public static boolean isLdapObjectOutOfScopeFromBaseDn(DirContextAdapter adapter, String baseDn) {
    boolean isOutOfScope = true;
    try {
      Name dn = adapter.getDn();
      Preconditions.checkArgument(dn != null, "DN cannot be null in LDAP response object");

      DistinguishedName full = LdapUtils.getFullDn((DistinguishedName) dn, adapter);
      DistinguishedName base = new DistinguishedName(baseDn);
      if (full.startsWith(base)) {
        isOutOfScope = false;
      }
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return isOutOfScope;
  }
}
