/*
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

package org.apache.ambari.server.view;

import com.google.common.base.Strings;
import com.google.inject.Singleton;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Singleton
public class AmbariViewsMDCLoggingFilter implements Filter {


  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
      //do nothing
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

    buildMDC(servletRequest);

    try {
      filterChain.doFilter(servletRequest,servletResponse);
    } finally {
      clear();
    }


  }

  private void buildMDC(ServletRequest request) {

    if ((request instanceof HttpServletRequest) && MDC.getMDCAdapter() != null) {
      HttpServletRequest httpServletRequest = (HttpServletRequest) request;
      MDC.put("remote-host", httpServletRequest
              .getRemoteHost());

      String flowId = httpServletRequest
              .getHeader("request-flow-id");

      if(!Strings.isNullOrEmpty(flowId)) {
        MDC.put("flow-id", flowId);
      }
    }
  }

  private void clear() {
    MDC.clear();
  }

  @Override
  public void destroy() {
    //do nothing
  }
}
