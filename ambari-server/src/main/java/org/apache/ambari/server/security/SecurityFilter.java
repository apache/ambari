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

package org.apache.ambari.server.security;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.ambari.server.controller.AmbariServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SecurityFilter implements Filter {
	
  //Allowed pathes for one way auth https
  private static String CA = "/ca";
  private final static Log LOG = LogFactory.getLog(SecurityFilter.class);

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest serReq, ServletResponse serResp,
		FilterChain filtCh) throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) serReq;
    String reqUrl = req.getRequestURL().toString();
	
    if (serReq.getLocalPort() == AmbariServer.AGENT_ONE_WAY_AUTH) {
      if (isRequestAllowed(reqUrl)) {
        filtCh.doFilter(serReq, serResp);
      }
      else {
        LOG.warn("This request is not allowed on this port");
      }

	}
	else
      filtCh.doFilter(serReq, serResp);
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
  }

  private boolean isRequestAllowed(String reqUrl) {
	try {

      boolean isMatch = Pattern.matches("https://[A-z]*:[0-9]*/cert/ca[/]*", reqUrl);
		
      if (isMatch)
    	  return true;
		
		 isMatch = Pattern.matches("https://[A-z]*:[0-9]*/certs/[A-z0-9-.]*", reqUrl);
		
		 if (isMatch)
			 return true;
		
		 isMatch = Pattern.matches("https://[A-z]*:[0-9]*/resources/.*", reqUrl);
		
		 if (isMatch)
			 return true;
		
	} catch (Exception e) {
	}
  LOG.warn("Request " + reqUrl + " doesn't match any pattern.");
	return false;
  }
}
