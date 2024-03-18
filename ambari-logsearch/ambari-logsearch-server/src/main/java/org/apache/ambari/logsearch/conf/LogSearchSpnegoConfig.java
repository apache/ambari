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

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
public class LogSearchSpnegoConfig {

  @LogSearchPropertyDescription(
    name = "logsearch.hadoop.security.auth_to_local",
    description = "Rules that will be applied on authentication names and map them into local usernames.",
    examples = {"RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//", "DEFAULT"},
    defaultValue = "DEFAULT",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.hadoop.security.auth_to_local:DEFAULT}")
  private String nameRules;

  @LogSearchPropertyDescription(
    name = "logsearch.admin.kerberos.token.valid.seconds",
    description = "Kerberos token validity in seconds.",
    examples = {"30"},
    defaultValue = "30",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.admin.kerberos.token.valid.seconds:30}")
  private String tokenValid;

  @LogSearchPropertyDescription(
    name = "logsearch.admin.kerberos.cookie.domain",
    description = "Domain for Kerberos cookie.",
    examples = {"c6401.ambari.apache.org", "localhost"},
    defaultValue = "localhost",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.admin.kerberos.cookie.domain:localhost}")
  private String cookieDomain;

  @LogSearchPropertyDescription(
    name = "logsearch.admin.kerberos.cookie.path",
    description = "Cookie path of the kerberos cookie",
    examples = {"/"},
    defaultValue = "/",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.admin.kerberos.cookie.path:/}")
  private String cookiePath;

  @LogSearchPropertyDescription(
    name = "logsearch.spnego.kerberos.principal",
    description = "Principal for SPNEGO authentication for Http requests",
    examples = {"myuser@EXAMPLE.COM"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.spnego.kerberos.principal:}")
  private String principal;

  @LogSearchPropertyDescription(
    name = "logsearch.spnego.kerberos.keytab",
    description = "Keytab for SPNEGO authentication for Http requests.",
    examples = {"/etc/security/keytabs/mykeytab.keytab"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.spnego.kerberos.keytab:}")
  private String keyTab;

  @LogSearchPropertyDescription(
    name = "logsearch.spnego.kerberos.host",
    description = "",
    examples = {"c6401.ambari.apache.org", "localhost"},
    defaultValue = "localhost",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.spnego.kerberos.host:localhost}")
  private String hostName;

  @LogSearchPropertyDescription(
    name = "logsearch.spnego.kerberos.enabled",
    description = "Enable SPNEGO based authentication for Log Search Server.",
    examples = {"true", "false"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.spnego.kerberos.enabled:false}")
  private boolean kerberosEnabled;

  public String getNameRules() {
    return nameRules;
  }

  public void setNameRules(String nameRules) {
    this.nameRules = nameRules;
  }

  public String getTokenValid() {
    return tokenValid;
  }

  public void setTokenValid(String tokenValid) {
    this.tokenValid = tokenValid;
  }

  public String getCookieDomain() {
    return cookieDomain;
  }

  public void setCookieDomain(String cookieDomain) {
    this.cookieDomain = cookieDomain;
  }

  public String getCookiePath() {
    return cookiePath;
  }

  public void setCookiePath(String cookiePath) {
    this.cookiePath = cookiePath;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public String getKeyTab() {
    return keyTab;
  }

  public void setKeyTab(String keyTab) {
    this.keyTab = keyTab;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public boolean isKerberosEnabled() {
    return kerberosEnabled;
  }

  public void setKerberosEnabled(boolean kerberosEnabled) {
    this.kerberosEnabled = kerberosEnabled;
  }
}
