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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

  @Value("${logsearch.auth.file.enable:true}")
  boolean authFileEnabled;
  @Value("${logsearch.auth.ldap.enable:false}")
  boolean authLdapEnabled;
  @Value("${logsearch.auth.simple.enable:false}")
  boolean authSimpleEnabled;
  @Value("${logsearch.auth.external_auth.enable:false}")
  boolean authExternalEnabled;
  @Value("${logsearch.auth.external_auth.host_url:'http://ip:port'}")
  private String externalAuthHostUrl;
  @Value("${logsearch.auth.external_auth.login_url:/api/v1/users/$USERNAME/privileges?fields=*}")
  private String externalAuthLoginUrl;
  @Value("${logsearch.login.credentials.file:user_pass.json}")
  private String credentialsFile;

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
}
