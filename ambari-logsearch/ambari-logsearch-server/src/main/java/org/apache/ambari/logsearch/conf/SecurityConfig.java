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

import com.google.common.collect.Lists;

import org.apache.ambari.logsearch.conf.global.LogSearchConfigState;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.web.authenticate.LogsearchAuthFailureHandler;
import org.apache.ambari.logsearch.web.authenticate.LogsearchAuthSuccessHandler;
import org.apache.ambari.logsearch.web.authenticate.LogsearchLogoutSuccessHandler;
import org.apache.ambari.logsearch.web.filters.LogsearchAuditLogsStateFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchAuthenticationEntryPoint;
import org.apache.ambari.logsearch.web.filters.LogsearchCorsFilter;
import org.apache.ambari.logsearch.web.filters.LogSearchConfigStateFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchKRBAuthenticationFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchJWTFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchSecurityContextFormationFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchServiceLogsStateFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchEventHistoryStateFilter;
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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_SESSION_ID;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Inject
  private AuthPropsConfig authPropsConfig;

  @Inject
  private LogSearchHttpHeaderConfig logSearchHttpHeaderConfig;

  @Inject
  private SolrServiceLogPropsConfig solrServiceLogPropsConfig;

  @Inject
  private SolrAuditLogPropsConfig solrAuditLogPropsConfig;

  @Inject
  private SolrEventHistoryPropsConfig solrEventHistoryPropsConfig;

  @Inject
  @Named("solrServiceLogsState")
  private SolrCollectionState solrServiceLogsState;

  @Inject
  @Named("solrAuditLogsState")
  private SolrCollectionState solrAuditLogsState;

  @Inject
  @Named("solrEventHistoryState")
  private SolrCollectionState solrEventHistoryState;

  @Inject
  private LogSearchConfigState logSearchConfigState;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
      .csrf().disable()
      .sessionManagement()
         .sessionFixation()
         .newSession()
         .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
      .and()
      .authorizeRequests()
        .requestMatchers(requestMatcher()).permitAll()
        .antMatchers("/**").authenticated()
      .and()
      .authenticationProvider(logsearchAuthenticationProvider())
      .httpBasic()
        .authenticationEntryPoint(logsearchAuthenticationEntryPoint())
      .and()
      .addFilterBefore(logsearchKRBAuthenticationFilter(), BasicAuthenticationFilter.class)
      .addFilterBefore(logsearchUsernamePasswordAuthenticationFilter(), LogsearchKRBAuthenticationFilter.class)
      .addFilterAfter(securityContextFormationFilter(), FilterSecurityInterceptor.class)
      .addFilterAfter(logsearchEventHistoryFilter(), LogsearchSecurityContextFormationFilter.class)
      .addFilterAfter(logsearchAuditLogFilter(), LogsearchSecurityContextFormationFilter.class)
      .addFilterAfter(logsearchServiceLogFilter(), LogsearchSecurityContextFormationFilter.class)
      .addFilterAfter(logSearchConfigStateFilter(), LogsearchSecurityContextFormationFilter.class)
      .addFilterBefore(logsearchCorsFilter(), LogsearchSecurityContextFormationFilter.class)
      .addFilterBefore(logsearchJwtFilter(), LogsearchSecurityContextFormationFilter.class)
      .logout()
        .logoutUrl("/logout")
        .deleteCookies(LOGSEARCH_SESSION_ID)
        .logoutSuccessHandler(new LogsearchLogoutSuccessHandler());
  }

  @Bean
  public LogsearchCorsFilter logsearchCorsFilter() {
    return new LogsearchCorsFilter(logSearchHttpHeaderConfig);
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
  public LogsearchJWTFilter logsearchJwtFilter() throws Exception {
    LogsearchJWTFilter filter = new LogsearchJWTFilter(requestMatcher(), authPropsConfig);
    filter.setAuthenticationManager(authenticationManagerBean());
    filter.setAuthenticationSuccessHandler(new LogsearchAuthSuccessHandler());
    filter.setAuthenticationFailureHandler(new LogsearchAuthFailureHandler());
    return filter;
  }

  @Bean
  public LogsearchAuthenticationEntryPoint logsearchAuthenticationEntryPoint() {
    LogsearchAuthenticationEntryPoint entryPoint = new LogsearchAuthenticationEntryPoint("/login");
    entryPoint.setForceHttps(false);
    entryPoint.setUseForward(authPropsConfig.isRedirectForward());
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

  @Bean
  public LogsearchServiceLogsStateFilter logsearchServiceLogFilter() {
    return new LogsearchServiceLogsStateFilter(serviceLogsRequestMatcher(), solrServiceLogsState, solrServiceLogPropsConfig);
  }

  @Bean
  public LogsearchAuditLogsStateFilter logsearchAuditLogFilter() {
    return new LogsearchAuditLogsStateFilter(auditLogsRequestMatcher(), solrAuditLogsState, solrAuditLogPropsConfig);
  }

  @Bean
  public LogsearchEventHistoryStateFilter logsearchEventHistoryFilter() {
    return new LogsearchEventHistoryStateFilter(eventHistoryRequestMatcher(), solrEventHistoryState, solrEventHistoryPropsConfig);
  }

  @Bean
  public LogSearchConfigStateFilter logSearchConfigStateFilter() {
    return new LogSearchConfigStateFilter(logsearchConfigRequestMatcher(), logSearchConfigState);
  }

  @Bean
  public RequestMatcher requestMatcher() {
    List<RequestMatcher> matchers = Lists.newArrayList();
    matchers.add(new AntPathRequestMatcher("/docs/**"));
    matchers.add(new AntPathRequestMatcher("/swagger-ui/**"));
    matchers.add(new AntPathRequestMatcher("/swagger.html"));
    matchers.add(new AntPathRequestMatcher("/"));
    matchers.add(new AntPathRequestMatcher("/login"));
    matchers.add(new AntPathRequestMatcher("/logout"));
    matchers.add(new AntPathRequestMatcher("/resources/**"));
    matchers.add(new AntPathRequestMatcher("/index.html"));
    matchers.add(new AntPathRequestMatcher("/favicon.ico"));
    matchers.add(new AntPathRequestMatcher("/assets/**"));
    matchers.add(new AntPathRequestMatcher("/templates/**"));
    matchers.add(new AntPathRequestMatcher("/api/v1/info/**"));
    matchers.add(new AntPathRequestMatcher("/api/v1/public/**"));
    matchers.add(new AntPathRequestMatcher("/api/v1/swagger.json"));
    matchers.add(new AntPathRequestMatcher("/api/v1/swagger.yaml"));
    return new OrRequestMatcher(matchers);
  }

  public RequestMatcher serviceLogsRequestMatcher() {
    return new AntPathRequestMatcher("/api/v1/service/logs/**");
  }

  public RequestMatcher auditLogsRequestMatcher() {
    return new AntPathRequestMatcher("/api/v1/audit/logs/**");
  }

  public RequestMatcher eventHistoryRequestMatcher() {
    return new AntPathRequestMatcher("/api/v1/history/**");
  }

  public RequestMatcher logsearchConfigRequestMatcher() {
    return new AntPathRequestMatcher("/api/v1/shipper/**");
  }

}
