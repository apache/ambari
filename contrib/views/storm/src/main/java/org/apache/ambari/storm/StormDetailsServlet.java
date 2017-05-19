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

package org.apache.ambari.view.storm;

import org.apache.ambari.view.ViewContext;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

/**
 * Simple servlet for proxying requests with doAs impersonation.
 */
public class StormDetailsServlet extends HttpServlet {

  private ViewContext viewContext;
  private static final String STORM_HOST = "storm.host";
  private static final String STORM_PORT = "storm.port";
  private static final String STORM_SSL_ENABLED = "storm.sslEnabled";
  private String stormURL;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ServletContext context = config.getServletContext();
    viewContext = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);
    String sslEnabled = viewContext.getProperties().get(STORM_SSL_ENABLED);
    String hostname = viewContext.getProperties().get(STORM_HOST);
    String port = viewContext.getProperties().get(STORM_PORT);
    stormURL = (sslEnabled.equals("true") ? "https" : "http") + "://" + hostname + ":" + port;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String hostDetails = "{\"hostdata\":\""+stormURL+"\"}";
    InputStream resultStream = new ByteArrayInputStream(hostDetails.getBytes(StandardCharsets.UTF_8));
    this.setResponse(request, response, resultStream);
  }

  /**
   * Set response to the get/post request
   * @param request      HttpServletRequest
   * @param response     HttpServletResponse
   * @param resultStream InputStream
   */
  public void setResponse(HttpServletRequest request, HttpServletResponse response, InputStream resultStream) throws IOException{
    Scanner scanner = new Scanner(resultStream).useDelimiter("\\A");
    String result = scanner.hasNext() ? scanner.next() : "";
    Boolean notFound = result == "" || result.indexOf("\"exception\":\"NotFoundException\"") != -1;
    response.setContentType(request.getContentType());
    response.setStatus(notFound ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_OK);
    PrintWriter writer = response.getWriter();
    writer.print(result);
  }
}
