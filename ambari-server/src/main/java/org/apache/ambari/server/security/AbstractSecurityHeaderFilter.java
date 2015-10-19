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
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
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
 * AbstractSecurityHeaderFilter is an abstract class used to help add security-related headers to
 * HTTP responses.
 * <p/>
 * This class is to be implemented to set the values for the following headers:
 * <ol>
 * <li>Strict-Transport-Security</li>
 * <li>X-Frame-Options</li>
 * <li>X-XSS-Protection</li>
 * </ol>
 * <p/>
 * If the value for a particular header item is empty (or null) that header will not be added to the
 * set of response headers.
 */
public abstract class AbstractSecurityHeaderFilter implements Filter {
  protected final static String STRICT_TRANSPORT_HEADER = "Strict-Transport-Security";
  protected final static String X_FRAME_OPTIONS_HEADER = "X-Frame-Options";
  protected final static String X_XSS_PROTECTION_HEADER = "X-XSS-Protection";

  /**
   * The logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AbstractSecurityHeaderFilter.class);
  /**
   * The Configuration object used to determine how Ambari is configured
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
      processConfig(configuration);
    }
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

    if (servletResponse instanceof HttpServletResponse) {
      HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
      // Conditionally set the Strict-Transport-Security HTTP response header if SSL is enabled and
      // a value is supplied
      if (sslEnabled && !StringUtils.isEmpty(strictTransportSecurity)) {
        httpServletResponse.setHeader(STRICT_TRANSPORT_HEADER, strictTransportSecurity);
      }

      // Conditionally set the X-Frame-Options HTTP response header if a value is supplied
      if (!StringUtils.isEmpty(xFrameOptionsHeader)) {
        httpServletResponse.setHeader(X_FRAME_OPTIONS_HEADER, xFrameOptionsHeader);
      }

      // Conditionally set the X-XSS-Protection HTTP response header if a value is supplied
      if (!StringUtils.isEmpty(xXSSProtectionHeader)) {
        httpServletResponse.setHeader(X_XSS_PROTECTION_HEADER, xXSSProtectionHeader);
      }
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {
    LOG.debug("Destroying {}", this.getClass().getName());
  }

  protected abstract void processConfig(Configuration configuration);


  protected void setSslEnabled(boolean sslEnabled) {
    this.sslEnabled = sslEnabled;
  }

  protected void setStrictTransportSecurity(String strictTransportSecurity) {
    this.strictTransportSecurity = strictTransportSecurity;
  }

  protected void setxFrameOptionsHeader(String xFrameOptionsHeader) {
    this.xFrameOptionsHeader = xFrameOptionsHeader;
  }

  protected void setxXSSProtectionHeader(String xXSSProtectionHeader) {
    this.xXSSProtectionHeader = xXSSProtectionHeader;
  }
}
