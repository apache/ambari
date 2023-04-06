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

package org.apache.hadoop.metrics2.sink.timeline;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Handles SPNego authentication as a client of hadoop service, caches
 * hadoop.auth cookie returned by hadoop service on successful SPNego
 * authentication. Refreshes hadoop.auth cookie on demand if the cookie has
 * expired.
 *
 */
public class AppCookieManager {

  static final String HADOOP_AUTH = "hadoop.auth";
  private static final String HADOOP_AUTH_EQ = "hadoop.auth=";
  private static final String SET_COOKIE = "Set-Cookie";

  private static final EmptyJaasCredentials EMPTY_JAAS_CREDENTIALS = new EmptyJaasCredentials();

  private Map<String, String> endpointCookieMap = new ConcurrentHashMap<String, String>();
  private static Log LOG = LogFactory.getLog(AppCookieManager.class);

  /**
   * Utility method to exercise AppCookieManager directly
   * @param args element 0 of args should be a URL to hadoop service protected by SPengo
   * @throws IOException in case of errors
   */
  public static void main(String[] args) throws IOException {
    new AppCookieManager().getAppCookie(args[0], false);
  }

  public AppCookieManager() {
  }

  /**
   * Returns hadoop.auth cookie, doing needed SPNego authentication
   *
   * @param endpoint
   *          the URL of the Hadoop service
   * @param refresh
   *          flag indicating wehther to refresh the cookie, if
   *          <code>true</code>, we do a new SPNego authentication and refresh
   *          the cookie even if the cookie already exists in local cache
   * @return hadoop.auth cookie value
   * @throws IOException
   *           in case of problem getting hadoop.auth cookie
   */
  public String getAppCookie(String endpoint, boolean refresh)
      throws IOException {

    HttpUriRequest outboundRequest = new HttpGet(endpoint);
    URI uri = outboundRequest.getURI();
    String scheme = uri.getScheme();
    String host = uri.getHost();
    int port = uri.getPort();
    String path = uri.getPath();
    if (!refresh) {
      String appCookie = endpointCookieMap.get(endpoint);
      if (appCookie != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("got cached cookie");
        }
        return appCookie;
      }
    }

    clearAppCookie(endpoint);

    DefaultHttpClient client = new DefaultHttpClient();
    SPNegoSchemeFactory spNegoSF = new SPNegoSchemeFactory(/* stripPort */true);
    client.getAuthSchemes().register(AuthPolicy.SPNEGO, spNegoSF);
    client.getCredentialsProvider().setCredentials(
        new AuthScope(/* host */null, /* port */-1, /* realm */null),
        EMPTY_JAAS_CREDENTIALS);

    String hadoopAuthCookie = null;
    HttpResponse httpResponse = null;
    try {
      HttpHost httpHost = new HttpHost(host, port, scheme);
      HttpRequest httpRequest = new HttpOptions(path);
      httpResponse = client.execute(httpHost, httpRequest);
      Header[] headers = httpResponse.getHeaders(SET_COOKIE);
      if (LOG.isDebugEnabled()) {
        for (Header header : headers) {
          LOG.debug(header.getName() + " : " + header.getValue());
        }
      }
      hadoopAuthCookie = getHadoopAuthCookieValue(headers);
      if (hadoopAuthCookie == null) {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        HttpEntity entity = httpResponse.getEntity();
        String responseBody = entity != null ? EntityUtils.toString(entity) : null;
        LOG.error("SPNego authentication failed with statusCode = " + statusCode + ", responseBody = " + responseBody + ", can not get hadoop.auth cookie for URL: " + endpoint);
        return null;
      }
    } finally {
      if (httpResponse != null) {
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
          entity.getContent().close();
        }
      }

    }

    hadoopAuthCookie = HADOOP_AUTH_EQ + quote(hadoopAuthCookie);
    setAppCookie(endpoint, hadoopAuthCookie);
    if (LOG.isInfoEnabled()) {
      LOG.info("Successful SPNego authentication to URL:" + uri.toString());
    }
    return hadoopAuthCookie;
  }


  /**
   * Returns the cached app cookie
   *  @param endpoint the hadoop end point we authenticate to
   * @return the cached app cookie, can be null
   */
  public String getCachedAppCookie(String endpoint) {
    return endpointCookieMap.get(endpoint);
  }

  /**
   *  Sets the cached app cookie cache
   *  @param endpoint the hadoop end point we authenticate to
   *  @param appCookie the app cookie
   */
  private void setAppCookie(String endpoint, String appCookie) {
    endpointCookieMap.put(endpoint, appCookie);
  }

  /**
   *  Clears the cached app cookie
   *  @param endpoint the hadoop end point we authenticate to
   */
  private void clearAppCookie(String endpoint) {
    endpointCookieMap.remove(endpoint);
  }

  static String quote(String s) {
    return s == null ? s : "\"" + s + "\"";
  }

  static String getHadoopAuthCookieValue(Header[] headers) {
    if (headers == null) {
      return null;
    }
    for (Header header : headers) {
      HeaderElement[] elements = header.getElements();
      for (HeaderElement element : elements) {
        String cookieName = element.getName();
        if (cookieName.equals(HADOOP_AUTH)) {
          if (element.getValue() != null) {
            String trimmedVal = element.getValue().trim();
            if (!trimmedVal.isEmpty()) {
              return trimmedVal;
            }
          }
        }
      }
    }
    return null;
  }


  private static class EmptyJaasCredentials implements Credentials {

    public String getPassword() {
      return null;
    }

    public Principal getUserPrincipal() {
      return null;
    }

  }

}
