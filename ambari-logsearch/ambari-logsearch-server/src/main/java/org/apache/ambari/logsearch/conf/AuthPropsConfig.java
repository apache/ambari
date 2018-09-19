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

import javax.inject.Inject;
import java.util.List;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
public class AuthPropsConfig {

  @Value("${logsearch.auth.file.enabled:true}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.file.enabled",
    description = "Enable file based authentication (in json file at logsearch configuration folder).",
    examples = {"true", "false"},
    defaultValue = "true",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  boolean authFileEnabled;

  @Value("${logsearch.auth.ldap.enabled:false}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.ldap.enabled",
    description = "Enable LDAP based authentication (currenty not supported).",
    examples = {"true", "false"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  boolean authLdapEnabled;

  @Value("${logsearch.auth.simple.enabled:false}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.simple.enabled",
    description = "Enable simple authentication. That means you won't require password to log in.",
    examples = {"true", "false"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  boolean authSimpleEnabled;

  @Value("${logsearch.auth.external_auth.enabled:false}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.external_auth.enabled",
    description = "Enable external authentication (currently Ambari acts as an external authentication server).",
    examples = {"true", "false"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  boolean authExternalEnabled;

  @Value("${logsearch.auth.external_auth.host_url:'http://ip:port'}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.external_auth.host_url",
    description = "External authentication server URL (host and port).",
    examples = {"https://c6401.ambari.apache.org:8080"},
    defaultValue = "http://ip:port",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String externalAuthHostUrl;

  @Value("${logsearch.auth.external_auth.login_url:/api/v1/users/$USERNAME/privileges?fields=*}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.external_auth.login_url",
    description = "Login URL for external authentication server ($USERNAME parameter is replaced with the Login username).",
    examples = {"/api/v1/users/$USERNAME/privileges?fields=*"},
    defaultValue = "/api/v1/users/$USERNAME/privileges?fields=*",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String externalAuthLoginUrl;

  @Value("${logsearch.login.credentials.file:user_pass.json}")
  @LogSearchPropertyDescription(
    name = "logsearch.login.credentials.file",
    description = "Name of the credential file which contains the users for file authentication (see: logsearch.auth.file.enabled).",
    examples = {"logsearch-admin.json"},
    defaultValue = "user_pass.json",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String credentialsFile;

  @Value("${logsearch.auth.jwt.enabled:false}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.jwt.enabled",
    description = "Enable JWT based authentication (e.g.: for KNOX).",
    examples = {"true", "false"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private boolean authJwtEnabled;
  @Value("${logsearch.auth.jwt.provider_url:}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.jwt.provider_url",
    description = "URL to the JWT authentication server.",
    examples = {"https://c6401.ambari.apache.org:8443/mypath"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String providedUrl;

  @Value("${logsearch.auth.jwt.public_key:}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.jwt.public_key",
    description = "PEM formatted public key for JWT token without the header and the footer.",
    examples = {"MIGfMA0GCSqGSIb3DQEBA..."},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String publicKey;

  @Value("${logsearch.auth.jwt.cookie.name:hadoop-jwt}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.jwt.cookie.name",
    description = "The name of the cookie that contains the JWT token.",
    examples = {"hadoop-jwt"},
    defaultValue = "hadoop-jwt",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String cookieName;
  @Value("${logsearch.auth.jwt.query.param.original_url:originalUrl}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.jwt.query.param.original_url",
    description = "Name of the original request URL which is used to redirect to Log Search Portal.",
    examples = {"myUrl"},
    defaultValue = "originalUrl",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String originalUrlQueryParam;

  @Value("#{'${logsearch.auth.jwt.audiances:}'.split(',')}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.jwt.audiances",
    description = "Comma separated list of acceptable audiences for the JWT token.",
    examples = {"audiance1,audiance2"},
    defaultValue = "",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> audiences;

  @Value("#{'${logsearch.auth.jwt.user.agents:Mozilla,Opera,Chrome}'.split(',')}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.jwt.user.agents",
    description = "Comma separated web user agent list. (Used as prefixes)",
    examples = {"Mozilla,Chrome"},
    defaultValue = "Mozilla,Opera,Chrome",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> userAgentList;

  @Value("#{'${logsearch.roles.allowed:AMBARI.ADMINISTRATOR,CLUSTER.ADMINISTRATOR}'.split(',')}")
  @LogSearchPropertyDescription(
    name = "logsearch.roles.allowed",
    description = "Comma separated roles for external authentication.",
    examples = {"AMBARI.ADMINISTRATOR"},
    defaultValue = "AMBARI.ADMINISTRATOR,CLUSTER.ADMINISTRATOR",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> allowedRoles;

  @Value("${logsearch.auth.redirect.forward:false}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.redirect.forward",
    description = "Forward redirects for HTTP calls. (useful in case of proxies)",
    examples = {"true"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private boolean redirectForward;

  @Value("${logsearch.auth.trusted.proxy:false}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.trusted.proxy",
    description = "A boolean property to enable/disable trusted-proxy 'knox' authentication",
    examples = {"true"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private boolean trustedProxy;

  @Value("#{propertiesSplitter.parseList('${logsearch.auth.proxyuser.users:knox}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.proxyuser.users",
    description = "List of users which the trusted-proxy user ‘knox’ can proxy for",
    examples = {"knox,hdfs"},
    defaultValue = "knox",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> proxyUsers;

  @Value("#{propertiesSplitter.parseList('${logsearch.auth.proxyuser.groups:*}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.proxyuser.groups",
    description = "List of user-groups which trusted-proxy user ‘knox’ can proxy for",
    examples = {"admin,user"},
    defaultValue = "*",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> proxyUserGroups;

  @Value("#{propertiesSplitter.parseList('${logsearch.auth.proxyuser.hosts:*}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.proxyuser.hosts",
    description = "List of hosts from which trusted-proxy user ‘knox’ can connect from",
    examples = {"host1,host2"},
    defaultValue = "*",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> proxyUserHosts;

  @Value("#{propertiesSplitter.parseList('${logsearch.auth.proxyserver.ip:}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.auth.proxyserver.ip",
    description = "IP of trusted Knox Proxy server(s) that Log Search will trust on",
    examples = {"192.168.0.1,192.168.0.2"},
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> proxyIp;

  @Value("${logsearch.authr.file.enabled:false}")
  @LogSearchPropertyDescription(
    name = "logsearch.authr.file.enabled",
    description = "A boolean property to enable/disable file based authorization",
    examples = {"true"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private boolean fileAuthorization;

  @Value("${logsearch.authr.role.file:roles.json}")
  @LogSearchPropertyDescription(
    name = "logsearch.authr.role.file",
    description = "Simple file that contains user/role mappings.",
    examples = {"logsearch-roles.json"},
    defaultValue = "roles.json",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String roleFile;

  @Inject
  private LogSearchLdapAuthConfig ldapAuthConfig;

  public boolean isAuthFileEnabled() {
    return authFileEnabled;
  }

  public void setAuthFileEnabled(boolean authFileEnabled) {
    this.authFileEnabled = authFileEnabled;
  }

  public boolean isAuthLdapEnabled() {
    return authLdapEnabled;
  }

  public void setAuthLdapEnabled(boolean authLdapEnabled) {
    this.authLdapEnabled = authLdapEnabled;
  }

  public boolean isAuthSimpleEnabled() {
    return authSimpleEnabled;
  }

  public void setAuthSimpleEnabled(boolean authSimpleEnabled) {
    this.authSimpleEnabled = authSimpleEnabled;
  }

  public String getCredentialsFile() {
    return credentialsFile;
  }

  public void setCredentialsFile(String credentialsFile) {
    this.credentialsFile = credentialsFile;
  }

  public String getExternalAuthHostUrl() {
    return externalAuthHostUrl;
  }

  public void setExternalAuthHostUrl(String externalAuthHostUrl) {
    this.externalAuthHostUrl = externalAuthHostUrl;
  }

  public String getExternalAuthLoginUrl() {
    return externalAuthLoginUrl;
  }

  public void setExternalAuthLoginUrl(String externalAuthLoginUrl) {
    this.externalAuthLoginUrl = externalAuthLoginUrl;
  }

  public boolean isAuthExternalEnabled() {
    return authExternalEnabled;
  }

  public void setAuthExternalEnabled(boolean authExternalEnabled) {
    this.authExternalEnabled = authExternalEnabled;
  }

  public boolean isAuthJwtEnabled() {
    return authJwtEnabled;
  }

  public void setAuthJwtEnabled(boolean authJwtEnabled) {
    this.authJwtEnabled = authJwtEnabled;
  }

  public String getProvidedUrl() {
    return providedUrl;
  }

  public void setProvidedUrl(String providedUrl) {
    this.providedUrl = providedUrl;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public String getCookieName() {
    return cookieName;
  }

  public void setCookieName(String cookieName) {
    this.cookieName = cookieName;
  }

  public String getOriginalUrlQueryParam() {
    return originalUrlQueryParam;
  }

  public void setOriginalUrlQueryParam(String originalUrlQueryParam) {
    this.originalUrlQueryParam = originalUrlQueryParam;
  }

  public List<String> getAudiences() {
    return audiences;
  }

  public void setAudiences(List<String> audiences) {
    this.audiences = audiences;
  }

  public List<String> getAllowedRoles() {
    return allowedRoles;
  }

  public void setAllowedRoles(List<String> allowedRoles) {
    this.allowedRoles = allowedRoles;
  }

  public boolean isRedirectForward() {
    return redirectForward;
  }

  public void setRedirectForward(boolean redirectForward) {
    this.redirectForward = redirectForward;
  }

  public List<String> getUserAgentList() {
    return this.userAgentList;
  }

  public void setUserAgentList(List<String> userAgentList) {
    this.userAgentList = userAgentList;
  }

  public boolean isTrustedProxy() {
    return trustedProxy;
  }

  public void setTrustedProxy(boolean trustedProxy) {
    this.trustedProxy = trustedProxy;
  }

  public List<String> getProxyUsers() {
    return proxyUsers;
  }

  public void setProxyUsers(List<String> proxyUsers) {
    this.proxyUsers = proxyUsers;
  }

  public List<String> getProxyUserGroups() {
    return proxyUserGroups;
  }

  public void setProxyUserGroups(List<String> proxyUserGroups) {
    this.proxyUserGroups = proxyUserGroups;
  }

  public List<String> getProxyUserHosts() {
    return proxyUserHosts;
  }

  public void setProxyUserHosts(List<String> proxyUserHosts) {
    this.proxyUserHosts = proxyUserHosts;
  }

  public List<String> getProxyIp() {
    return proxyIp;
  }

  public void setProxyIp(List<String> proxyIp) {
    this.proxyIp = proxyIp;
  }

  public boolean isFileAuthorization() {
    return fileAuthorization;
  }

  public void setFileAuthorization(boolean fileAuthorization) {
    this.fileAuthorization = fileAuthorization;
  }

  public String getRoleFile() {
    return roleFile;
  }

  public void setRoleFile(String roleFile) {
    this.roleFile = roleFile;
  }

  public LogSearchLdapAuthConfig getLdapAuthConfig() {
    return ldapAuthConfig;
  }

  public void setLdapAuthConfig(LogSearchLdapAuthConfig ldapAuthConfig) {
    this.ldapAuthConfig = ldapAuthConfig;
  }
}
