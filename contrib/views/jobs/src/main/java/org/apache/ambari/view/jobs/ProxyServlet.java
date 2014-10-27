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

package org.apache.ambari.view.jobs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.HttpImpersonator;
import org.apache.ambari.view.ImpersonatorSetting;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Simple servlet for proxying requests with doAs impersonation.
 */
public class ProxyServlet extends HttpServlet {

  private ViewContext viewContext;
  private HttpImpersonator impersonator;
  private ImpersonatorSetting impersonatorSetting;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ServletContext context = config.getServletContext();
    viewContext = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);

    this.impersonator = viewContext.getHttpImpersonator();
    this.impersonatorSetting = viewContext.getImpersonatorSetting();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String urlToRead = request.getParameter("url");
    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);

    // Getting the result is super simple by using the impersonator and the default values in the factory.
    String result = this.impersonator.requestURL(urlToRead, "GET", this.impersonatorSetting);

    PrintWriter writer = response.getWriter();
    writer.print(result);
  }
}

