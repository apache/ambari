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
package org.apache.ambari.infra.conf.security;

import static java.util.Arrays.asList;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Value("${infra-manager.admin-user.username:admin}")
  private String adminUserName;

  @Value("${infra-manager.admin-user.password:@null}")
  private String adminUserPassword;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable().authorizeRequests()
            .requestMatchers(publicEndpoints())
            .permitAll()
            .antMatchers("/**")
            .hasRole("ADMIN")
            .and()
            .httpBasic();
  }

  private RequestMatcher publicEndpoints() {
    return new OrRequestMatcher(asList(
            new AntPathRequestMatcher("/docs/**"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/api/v1/swagger.yaml")
    ));
  }

  @Inject
  public void configureGlobal(
          AuthenticationManagerBuilder auth,
          PasswordEncoder passwordEncoder,
          HadoopCredentialStore hadoopCredentialStore) throws Exception {

    Secret adminPassword = new CompositeSecret(
            hadoopCredentialStore.getSecret("infra_manager_admin_user_password"),
            () -> Optional.ofNullable(adminUserPassword));

    auth.inMemoryAuthentication()
            .passwordEncoder(passwordEncoder)
            .withUser(adminUserName)
            .password(passwordEncoder.encode(adminPassword.get().orElseThrow(() -> new IllegalStateException("Password for admin not set!"))))
            .roles("ADMIN");
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}