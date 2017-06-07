/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.configuration.spring;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariLocalUserProvider;
import org.apache.ambari.server.security.authorization.AmbariPamAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariUserAuthorizationFilter;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.apache.ambari.server.security.authorization.internal.AmbariInternalAuthenticationProvider;
import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Injector;

@Configuration
public class GuiceBeansConfig {

  @Autowired
  //ignore warning, inherited from parent context, injected as field to reduce number of warnings
  private Injector injector;

  @Bean
  public org.apache.ambari.server.configuration.Configuration ambariConfig() {
    return injector.getInstance(org.apache.ambari.server.configuration.Configuration.class);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return injector.getInstance(PasswordEncoder.class);
  }

  @Bean
  public AuditLogger auditLogger() {
    return injector.getInstance(AuditLogger.class);
  }

  @Bean
  public PermissionHelper permissionHelper() {
    return injector.getInstance(PermissionHelper.class);
  }

  @Bean
  public AmbariLdapAuthenticationProvider ambariLdapAuthenticationProvider() {
    return injector.getInstance(AmbariLdapAuthenticationProvider.class);
  }

  @Bean
  public AmbariLocalUserProvider localUserProvider() {
    return injector.getInstance(AmbariLocalUserProvider.class);
  }

  @Bean
  public AmbariLdapDataPopulator ambariLdapDataPopulator() {
    return injector.getInstance(AmbariLdapDataPopulator.class);
  }

  @Bean
  public AmbariUserAuthorizationFilter ambariUserAuthorizationFilter() {
    return injector.getInstance(AmbariUserAuthorizationFilter.class);
  }

  @Bean
  public AmbariInternalAuthenticationProvider ambariInternalAuthenticationProvider() {
    return injector.getInstance(AmbariInternalAuthenticationProvider.class);
  }

  @Bean
  public AmbariPamAuthenticationProvider ambariPamAuthenticationProvider() {
    return injector.getInstance(AmbariPamAuthenticationProvider.class);
  }

}
