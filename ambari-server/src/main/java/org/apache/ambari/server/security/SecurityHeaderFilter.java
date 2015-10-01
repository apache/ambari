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

package org.apache.ambari.server.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SecurityHeaderFilter adds security-related headers to HTTP response messages
 */
@Singleton
public class SecurityHeaderFilter implements Filter {
  /**
   * The logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(SecurityHeaderFilter.class);

  protected final static String STRICT_TRANSPORT_HEADER = "Strict-Transport-Security";
  protected final static String X_FRAME_OPTIONS_HEADER = "X-Frame-Options";
  protected final static String X_XSS_PROTECTION_HEADER = "X-XSS-Protection";

  /**
   * The Configuration object used to determing how Ambari is configured
   */
  @Inject
  private Configuration configuration;

  /**
   * Indicates whether Ambari is configured for SSL (true) or not (false).  By default true is assumed
   * since preparing for more security will not hurt and is better than not assuming SSL is enabled
   * when it is.
   */
  private boolean sslEnabled = true;

  /**
   * The value for the Strict-Transport-Security HTTP response header.
   */
  private String strictTransportSecurity = Configuration.HTTP_STRICT_TRANSPORT_HEADER_VALUE_DEFAULT;

  /**
   * The value for the X-Frame-Options HTTP response header.
   */
  private String xFrameOptionsHeader = Configuration.HTTP_X_FRAME_OPTIONS_HEADER_VALUE_DEFAULT;

  /**
   * The value for the X-XSS-Protection HTTP response header.
   */
  private String xXSSProtectionHeader = Configuration.HTTP_X_XSS_PROTECTION_HEADER_VALUE_DEFAULT;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    LOG.debug("Initializing {}", this.getClass().getName());

    if (configuration == null) {
      LOG.warn("The Ambari configuration object is not available, all default options will be assumed.");
    } else {
      sslEnabled = configuration.getApiSSLAuthentication();
      strictTransportSecurity = configuration.getStrictTransportSecurityHTTPResponseHeader();
      xFrameOptionsHeader = configuration.getXFrameOptionsHTTPResponseHeader();
      xXSSProtectionHeader = configuration.getXXSSProtectionHTTPResponseHeader();
    }
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

    if (servletResponse instanceof HttpServletResponse) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
      // Conditionally set the Strict-Transport-Security HTTP response header if SSL is enabled and
      // a value is supplied
      if (sslEnabled && (strictTransportSecurity != null) && !strictTransportSecurity.isEmpty()) {
        httpServletResponse.setHeader(STRICT_TRANSPORT_HEADER, strictTransportSecurity);
      }

      // Conditionally set the X-Frame-Options HTTP response header if a value is supplied
      if ((xFrameOptionsHeader != null) && !xFrameOptionsHeader.isEmpty()) {
        httpServletResponse.setHeader(X_FRAME_OPTIONS_HEADER, xFrameOptionsHeader);
      }

      // Conditionally set the X-XSS-Protection HTTP response header if a value is supplied
      if ((xXSSProtectionHeader != null) && !xXSSProtectionHeader.isEmpty()) {
        httpServletResponse.setHeader(X_XSS_PROTECTION_HEADER, xXSSProtectionHeader);
      }
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {
    LOG.debug("Destroying {}", this.getClass().getName());
  }
}
