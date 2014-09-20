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

package org.apache.ambari.server.controller.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

/**
 * URL based implementation of a stream provider.
 */
public class URLStreamProvider implements StreamProvider {

  private static final String COOKIE = "Cookie";
  private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
  private static final String NEGOTIATE = "Negotiate";
  private static Log LOG = LogFactory.getLog(URLStreamProvider.class);

  private final int connTimeout;
  private final int readTimeout;
  private final String path;
  private final String password;
  private final String type;
  private volatile SSLSocketFactory sslSocketFactory = null;
  private AppCookieManager appCookieManager;

  /**
   * Provide the connection timeout for the underlying connection.
   * 
   * @param connectionTimeout
   *          time, in milliseconds, to attempt a connection
   * @param readTimeout
   *          the read timeout in milliseconds
   * @param configuration configuration holding TrustStore information
   */
  public URLStreamProvider(int connectionTimeout, int readTimeout, 
      ComponentSSLConfiguration configuration) {  
    this(connectionTimeout, readTimeout,
        configuration.getTruststorePath(),
        configuration.getTruststorePassword(),
        configuration.getTruststoreType());
  }
  /**
   * Provide the connection timeout for the underlying connection.
   * 
   * @param connectionTimeout
   *          time, in milliseconds, to attempt a connection
   * @param readTimeout
   *          the read timeout in milliseconds
   */
  public URLStreamProvider(int connectionTimeout, int readTimeout, String path,
      String password, String type) {

    this.connTimeout = connectionTimeout;
    this.readTimeout = readTimeout;
    this.path = path;          // truststroe path
    this.password = password;  // truststore password
    this.type = type;          // truststroe type
    appCookieManager = new AppCookieManager();
  }

  
  @Override
  public InputStream readFrom(String spec, String requestMethod, String params) throws IOException {
    return processURL(spec, requestMethod, params, null).getInputStream();
  }

  public HttpURLConnection processURL(String spec, String requestMethod, Object params, Map<String, List<String>> headers)
          throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("readFrom spec:" + spec);
    }

    HttpURLConnection connection = spec.startsWith("https") ?
            (HttpURLConnection)getSSLConnection(spec)
            : (HttpURLConnection)getConnection(spec);

    String appCookie = appCookieManager.getCachedAppCookie(spec);
    if (appCookie != null) {
      LOG.debug("Using cached app cookie for URL:" + spec);
      connection.setRequestProperty(COOKIE, appCookie);
    }
    connection.setConnectTimeout(connTimeout);
    connection.setReadTimeout(readTimeout);
    connection.setDoOutput(true);
    connection.setRequestMethod(requestMethod);

    if (headers != null) {
      for (Map.Entry<String, List<String>> entry: headers.entrySet()) {
        String paramValue = entry.getValue().toString();
        connection.setRequestProperty(entry.getKey(), paramValue.substring(1, paramValue.length() - 1));
      }
    }

    if (params != null) {
      byte[] info;
      if (params instanceof InputStream) {
        info = IOUtils.toByteArray((InputStream)params);
      } else {
        info = ((String)params).getBytes();
      }
      connection.getOutputStream().write(info);
    }

    int statusCode = connection.getResponseCode();
    if (statusCode == HttpStatus.SC_UNAUTHORIZED ) {
      String wwwAuthHeader = connection.getHeaderField(WWW_AUTHENTICATE);
      if (LOG.isInfoEnabled()) {
        LOG.info("Received WWW-Authentication header:" + wwwAuthHeader + ", for URL:" + spec);
      }
      if (wwwAuthHeader != null &&
        wwwAuthHeader.trim().startsWith(NEGOTIATE)) {
        //connection.getInputStream().close();
        connection = spec.startsWith("https") ?
           (HttpURLConnection)getSSLConnection(spec)
           : (HttpURLConnection)getConnection(spec);
        appCookie = appCookieManager.getAppCookie(spec, true);
        connection.setRequestProperty(COOKIE, appCookie);
        connection.setConnectTimeout(connTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setDoOutput(true);

        return connection;
      } else {
        // no supported authentication type found
        // we would let the original response propogate
        LOG.error("Unsupported WWW-Authentication header:" + wwwAuthHeader+ ", for URL:" + spec);
        return connection;
      }
    } else {
        // not a 401 Unauthorized status code
        // we would let the original response propogate
        return connection;
    }
  }
  
  @Override
  public InputStream readFrom(String spec) throws IOException {
    
    return readFrom(spec, "GET", null);
    
  }
  
  // ----- helper methods ----------------------------------------------------

  // Get a connection
  private URLConnection getConnection(String spec) throws IOException {
    return new URL(spec).openConnection();
  }

  // Get an ssl connection
  private HttpsURLConnection getSSLConnection(String spec) throws IOException {

    if (sslSocketFactory == null) {
      synchronized (this) {
        if (sslSocketFactory == null) {
          try {
            FileInputStream in = new FileInputStream(new File(path));
            KeyStore store = KeyStore.getInstance(type == null ? KeyStore
                .getDefaultType() : type);

            store.load(in, password.toCharArray());
            in.close();

            TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());

            tmf.init(store);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            sslSocketFactory = context.getSocketFactory();
          } catch (Exception e) {
            throw new IOException("Can't get connection.", e);
          }
        }
      }
    }
    HttpsURLConnection connection = (HttpsURLConnection) (new URL(spec)
        .openConnection());

    connection.setSSLSocketFactory(sslSocketFactory);
 
    return connection;
  }

  public AppCookieManager getAppCookieManager() {
    return appCookieManager;
  }
}
