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
package org.apache.ambari.logsearch.web.filters;

import org.apache.ambari.logsearch.auth.filter.AbstractJWTFilter;
import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Collection;
import java.util.List;

public class LogsearchJWTFilter extends AbstractJWTFilter {

  private AuthPropsConfig authPropsConfig;

  public LogsearchJWTFilter(RequestMatcher requestMatcher, AuthPropsConfig authPropsConfig) {
    super(new NegatedRequestMatcher(requestMatcher));
    this.authPropsConfig = authPropsConfig;
  }

  @Override
  protected String getPublicKey() {
    return authPropsConfig.getPublicKey();
  }

  @Override
  protected String getProvidedUrl() {
    return authPropsConfig.getProvidedUrl();
  }

  @Override
  protected boolean isAuthJwtEnabled() {
    return authPropsConfig.isAuthJwtEnabled();
  }

  @Override
  protected String getCookieName() {
    return authPropsConfig.getCookieName();
  }

  @Override
  protected String getOriginalUrlQueryParam() {
    return authPropsConfig.getOriginalUrlQueryParam();
  }

  @Override
  protected List<String> getAudiences() {
    return authPropsConfig.getAudiences();
  }

  @Override
  protected Collection<? extends GrantedAuthority> getAuthorities() {
    return null; // TODO
  }


}
