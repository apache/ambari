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

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_SESSION_ID;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.conf.global.LogLevelFilterManagerState;
import org.apache.ambari.logsearch.conf.global.LogSearchConfigState;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.web.authenticate.LogsearchAuthFailureHandler;
import org.apache.ambari.logsearch.web.authenticate.LogsearchAuthSuccessHandler;
import org.apache.ambari.logsearch.web.authenticate.LogsearchLogoutSuccessHandler;
import org.apache.ambari.logsearch.web.filters.ConfigStateProvider;
import org.apache.ambari.logsearch.web.filters.GlobalStateProvider;
import org.apache.ambari.logsearch.web.filters.LogsearchAuthenticationEntryPoint;
import org.apache.ambari.logsearch.web.filters.LogsearchCorsFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchJWTFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchKRBAuthenticationFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchSecurityContextFormationFilter;
import org.apache.ambari.logsearch.web.filters.LogsearchUsernamePasswordAuthenticationFilter;
import org.apache.ambari.logsearch.web.security.LogsearchAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.google.common.collect.Lists;

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
  @Named("logLevelFilterManagerState")
  private LogLevelFilterManagerState logLevelFilterManagerState;

  @Inject
  private LogSearchConfigState logSearchConfigState;

  @Inject
  private LogSearchConfigApiConfig logSearchConfigApiConfig;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http
      .csrf().disable()
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
        .deleteCookies(getCookies())
        .logoutSuccessHandler(new LogsearchLogoutSuccessHandler());

    if ((logSearchConfigApiConfig.isSolrFilterStorage() || logSearchConfigApiConfig.isZkFilterStorage())
            && !logSearchConfigApiConfig.isConfigApiEnabled())
      http.addFilterAfter(logSearchLogLevelFilterManagerFilter(), LogsearchSecurityContextFormationFilter.class);
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
    return new LogsearchKRBAuthenticationFilter(requestMatcher());
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
    LogsearchAuthenticationEntryPoint entryPoint = new LogsearchAuthenticationEntryPoint("/login", authPropsConfig);
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

  private LogsearchFilter logsearchServiceLogFilter() {
    return new LogsearchFilter(serviceLogsRequestMatcher(), new GlobalStateProvider(solrServiceLogsState, solrServiceLogPropsConfig));
  }

  private LogsearchFilter logsearchAuditLogFilter() {
    return new LogsearchFilter(auditLogsRequestMatcher(), new GlobalStateProvider(solrAuditLogsState, solrAuditLogPropsConfig));
  }

  private LogsearchFilter logsearchEventHistoryFilter() {
    return new LogsearchFilter(eventHistoryRequestMatcher(), new GlobalStateProvider(solrEventHistoryState, solrEventHistoryPropsConfig));
  }

  private LogsearchFilter logSearchConfigStateFilter() {
    RequestMatcher requestMatcher;
    if (logSearchConfigApiConfig.isSolrFilterStorage() || logSearchConfigApiConfig.isZkFilterStorage()) {
      requestMatcher = shipperConfigInputRequestMatcher();
    } else {
      requestMatcher = logsearchConfigRequestMatcher();
    }

    return new LogsearchFilter(requestMatcher, new ConfigStateProvider(logSearchConfigState, logSearchConfigApiConfig.isConfigApiEnabled()));
  }

  private LogsearchFilter logSearchLogLevelFilterManagerFilter() {
    return new LogsearchFilter(logLevelFilterRequestMatcher(), requestUri ->
            logLevelFilterManagerState.isLogLevelFilterManagerIsReady() ? null : StatusMessage.with(SERVICE_UNAVAILABLE, "Solr log level filter manager is not available"));
  }

  @Bean
  public RequestMatcher requestMatcher() {
    List<RequestMatcher> matchers = Lists.newArrayList();
    matchers.add(new AntPathRequestMatcher("/docs/**"));
    matchers.add(new AntPathRequestMatcher("/swagger-ui/**"));
    matchers.add(new AntPathRequestMatcher("/swagger.html"));
    if (!authPropsConfig.isAuthJwtEnabled()) {
      matchers.add(new AntPathRequestMatcher("/"));
    }
    matchers.add(new AntPathRequestMatcher("/login"));
    matchers.add(new AntPathRequestMatcher("/logout"));
    matchers.add(new AntPathRequestMatcher("/resources/**"));
    matchers.add(new AntPathRequestMatcher("/index.html"));
    matchers.add(new AntPathRequestMatcher("/favicon.ico"));
    matchers.add(new AntPathRequestMatcher("/assets/**"));
    matchers.add(new AntPathRequestMatcher("/templates/**"));
    matchers.add(new AntPathRequestMatcher("/api/v1/info/**"));
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

  public RequestMatcher logLevelFilterRequestMatcher() {
    return new AntPathRequestMatcher("/api/v1/shipper/filters/**");
  }

  public RequestMatcher shipperConfigInputRequestMatcher() {
    return new AntPathRequestMatcher("/api/v1/shipper/input/**");
  }

  private String[] getCookies() {
    List<String> cookies = new ArrayList<>();
    cookies.add(LOGSEARCH_SESSION_ID);
    if (authPropsConfig.isAuthJwtEnabled()) {
      cookies.add(authPropsConfig.getCookieName());
    }
    return cookies.toArray(new String[0]);
  }

}
