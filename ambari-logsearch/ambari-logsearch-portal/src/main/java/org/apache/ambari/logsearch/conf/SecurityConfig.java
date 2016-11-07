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

import org.apache.ambari.logsearch.web.authenticate.LogsearchAuthFailureHandler;
import org.apache.ambari.logsearch.web.authenticate.LogsearchAuthSuccessHandler;
import org.apache.ambari.logsearch.web.authenticate.LogsearchLogoutSuccessHandler;
import org.apache.ambari.logsearch.web.filters.LogsearchAuthenticationEntryPoint;
import org.apache.ambari.logsearch.web.filters.LogsearchKRBAuthenticationFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchSecurityContextFormationFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchUsernamePasswordAuthenticationFilter;
import org.apache.ambari.logsearch.web.security.LogsearchAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
      .csrf().disable()
      .sessionManagement()
         .sessionFixation()
         .newSession()
         .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
      .and()
      .authorizeRequests()
        .antMatchers("/login.html").permitAll()
        .antMatchers("/styles/**").permitAll()
        .antMatchers("/fonts/**").permitAll()
        .antMatchers("/fonts/**").permitAll()
        .antMatchers("/scripts/**").permitAll()
        .antMatchers("/libs/**").permitAll()
        .antMatchers("/images/**").permitAll()
        .antMatchers("/templates/**").permitAll()
        .antMatchers("/favicon.ico").permitAll()
        .antMatchers("/api/v1/public/**").permitAll()
        .antMatchers("/api/v1/swagger.json").permitAll()
        .antMatchers("/**").authenticated()
      .and()
      .authenticationProvider(logsearchAuthenticationProvider())
        .formLogin()
        .loginPage("/login.html")
      .and()
      .httpBasic()
        .authenticationEntryPoint(logsearchAuthenticationEntryPoint())
      .and()
      .addFilterBefore(logsearchUsernamePasswordAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
      .addFilterBefore(new LogsearchKRBAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
      .addFilterAfter(securityContextFormationFilter(), FilterSecurityInterceptor.class)
      .logout()
        .logoutUrl("/logout.html")
        .deleteCookies("JSESSIONID")
        .logoutSuccessHandler(new LogsearchLogoutSuccessHandler());
  }

  @Bean
  public LogsearchSecurityContextFormationFilter securityContextFormationFilter() {
    return new LogsearchSecurityContextFormationFilter();
  }

  @Bean
  public LogsearchKRBAuthenticationFilter logsearchKRBAuthenticationFilter() {
    return new LogsearchKRBAuthenticationFilter();
  }

  @Bean
  public LogsearchAuthenticationProvider logsearchAuthenticationProvider() {
    return new LogsearchAuthenticationProvider();
  }

  @Bean
  public LogsearchAuthenticationEntryPoint logsearchAuthenticationEntryPoint() {
    LogsearchAuthenticationEntryPoint entryPoint = new LogsearchAuthenticationEntryPoint("/login.html");
    entryPoint.setForceHttps(false);
    return entryPoint;
  }

  @Bean
  public LogsearchUsernamePasswordAuthenticationFilter logsearchUsernamePasswordAuthenticationFilter() throws Exception {
    LogsearchUsernamePasswordAuthenticationFilter filter = new LogsearchUsernamePasswordAuthenticationFilter();
    filter.setAuthenticationSuccessHandler(new LogsearchAuthSuccessHandler());
    filter.setAuthenticationFailureHandler(new LogsearchAuthFailureHandler());
    filter.setAuthenticationManager(authenticationManagerBean());
    return filter;
  }

}
