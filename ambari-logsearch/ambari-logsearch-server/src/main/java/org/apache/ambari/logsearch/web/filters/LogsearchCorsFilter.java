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

import org.apache.ambari.logsearch.conf.LogSearchHttpHeaderConfig;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LogsearchCorsFilter implements Filter {

  private LogSearchHttpHeaderConfig logSearchHttpHeaderConfig;

  public LogsearchCorsFilter(LogSearchHttpHeaderConfig logSearchHttpHeaderConfig) {
    this.logSearchHttpHeaderConfig = logSearchHttpHeaderConfig;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
    throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.setHeader("Access-Control-Allow-Origin", logSearchHttpHeaderConfig.getAccessControlAllowOrigin());
    response.setHeader("Access-Control-Allow-Headers", logSearchHttpHeaderConfig.getAccessControlAllowHeaders());
    response.setHeader("Access-Control-Allow-Credentials", logSearchHttpHeaderConfig.getAccessControlAllowCredentials());
    response.setHeader("Access-Control-Allow-Methods", logSearchHttpHeaderConfig.getAccessControlAllowMethods());
    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {

  }
}
