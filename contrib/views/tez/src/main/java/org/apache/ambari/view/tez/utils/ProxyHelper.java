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

package org.apache.ambari.view.tez.utils;


import com.google.inject.Inject;
import org.apache.ambari.view.URLConnectionProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.tez.exceptions.TezWebAppException;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

public class ProxyHelper {

  private ViewContext viewContext;

  private static final Logger LOG = LoggerFactory.getLogger(ProxyHelper.class);

  @Inject
  public ProxyHelper(ViewContext viewContext) {
    this.viewContext = viewContext;
  }


  public String getResponse(String url, Map<String, String> headers, String authType) {
    LOG.debug("Fetching the result from the URL: {} using proxy", url);
    InputStream inputStream = null;
    try {
      HttpURLConnection connection;
      URLConnectionProvider provider = viewContext.getURLConnectionProvider();
      if(authType == null || authType.equalsIgnoreCase("simple")) {
        connection = provider.getConnection(url, "GET", (String) null, headers);
      } else {
        connection = provider.getConnectionAsCurrent(url, "GET", (String) null, headers);
      }

      if (!(connection.getResponseCode() >= 200 && connection.getResponseCode() < 300)) {
        LOG.error("Failure in fetching results for the URL: {}. Status: {}", url, connection.getResponseCode());
        String trace = "";
        inputStream = connection.getErrorStream();
        if (inputStream != null) {
          trace = IOUtils.toString(inputStream);
        }
        throw new TezWebAppException("Failed to fetch results by the proxy from url: " + url, connection.getResponseCode(), trace);
      }

      inputStream = connection.getInputStream();
      return IOUtils.toString(inputStream);

    } catch (IOException e) {
      LOG.error("Cannot access the url: {}", url, e);
      throw new TezWebAppException("Failed to fetch results by the proxy from url: " + url + ". Internal Error.", e);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) { /* Noting to do */ }
      }
    }
  }

  public String getProxyUrl(String baseUrl, String endPoint, MultivaluedMap<String, String> queryParameters, String authType) {
    Set<String> keySet = queryParameters.keySet();
    URIBuilder builder;
    try {
      builder = new URIBuilder(baseUrl + "/" + endPoint);
      for(String key: keySet) {
        builder.addParameter(key, queryParameters.getFirst(key));
      }
      if(authType == null || authType.equalsIgnoreCase("simple")) {
        builder.addParameter("user.name", viewContext.getUsername());
      }
      return builder.build().toString();
    } catch (URISyntaxException e) {
      LOG.error("Failed to build a URL from the baseUrl: {} and endPoint: {}. Exception: {}", baseUrl, endPoint, e);
      throw new TezWebAppException("Failed to build a URL from the baseUrl:" + baseUrl + "and endPoint: " + endPoint, e);
    }
  }
}
