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

package org.apache.ambari.view.tez;

import org.apache.ambari.view.URLConnectionProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;

/**
 * Simple servlet for proxying requests with doAs impersonation.
 */
public class ProxyServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(ProxyServlet.class);

  private URLConnectionProvider urlConnectionProvider;
  private final static String nullData = null;
  private final static Map<String, String> emptyHeaders = Collections.EMPTY_MAP;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ServletContext context = config.getServletContext();
    ViewContext viewContext = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);
    this.urlConnectionProvider = viewContext.getURLConnectionProvider();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String urlToRead = URLDecoder.decode(request.getParameter("url"));

    response.setContentType(request.getContentType());

    if (LOG.isDebugEnabled()) {
      LOG.debug("Requesting data from ATS, url=" + urlToRead);
    }

    InputStream resultInputStream;
    String result = "";
    // When in doubt, assume error
    int responseCode = Status.INTERNAL_SERVER_ERROR.getStatusCode();
    HttpURLConnection connection;
    try {
      // Use nullData as null string and null inputstream cannot be disambiguated
      // URL Stream Provider will automatically inject the doAs param with the current
      // user's info
      connection = urlConnectionProvider.getConnectionAsCurrent(urlToRead,
          HttpMethod.GET, nullData, emptyHeaders);
      responseCode = connection.getResponseCode();

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received response from ATS, url=" + urlToRead
            + ", responseCode=" + responseCode);
      }

      if (responseCode >= Response.Status.BAD_REQUEST.getStatusCode()) {
        resultInputStream = connection.getErrorStream();
      } else {
        resultInputStream = connection.getInputStream();
      }

      result = IOUtils.toString(resultInputStream);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received response from ATS, url=" + urlToRead
            + ", responseCode=" + responseCode
            + ", responseBodyLen=" + result.length());
      }

    } catch (IOException e) {
      // We might kill the ambari server by logging this error every time a call fails
      if (LOG.isDebugEnabled()) {
        LOG.warn("Failed to retrieve data from ATS, url=" + urlToRead, e);
      }
      responseCode = Status.INTERNAL_SERVER_ERROR.getStatusCode();
      result = e.toString();
    } catch (Exception e) {
      LOG.warn("Unknown Exception: Failed to retrieve data from ATS, url=" + urlToRead, e);
      responseCode = Status.INTERNAL_SERVER_ERROR.getStatusCode();
      result = e.toString();
    } finally {
      // not disconnecting http conn as it might be cached/re-used internally
      // in the UrlStreamProvider
    }

    response.setStatus(responseCode);
    if (result != null) {
      PrintWriter writer = response.getWriter();
      writer.print(result);
    }
  }

}

