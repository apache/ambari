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

package org.apache.ambari.server.controller.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.utils.URLCredentialsHider;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URL based implementation of a stream provider.
 */
public class URLStreamProvider implements StreamProvider {

  public static final String COOKIE = "Cookie";
  private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
  private static final String NEGOTIATE = "Negotiate";
  private static final String AUTHORIZATION = "Authorization";
  private static final String BASIC_AUTH = "Basic %s";
  private static final Logger LOG = LoggerFactory.getLogger(URLStreamProvider.class);

  private boolean setupTruststoreForHttps;
  private final int connTimeout;
  private final int readTimeout;
  private final String trustStorePath;
  private final String trustStorePassword;
  private final String trustStoreType;
  private volatile SSLSocketFactory sslSocketFactory = null;
  private AppCookieManager appCookieManager = null;


  // ----- Constructors ------------------------------------------------------

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
   * @param connectionTimeout   time, in milliseconds, to attempt a connection
   * @param readTimeout         the read timeout in milliseconds
   * @param trustStorePath      the path to the truststore required for secure connections
   * @param trustStorePassword  the truststore password
   * @param trustStoreType      the truststore type (e.g. "JKS")
   */
  public URLStreamProvider(int connectionTimeout, int readTimeout, String trustStorePath,
                           String trustStorePassword, String trustStoreType) {

    this.connTimeout        = connectionTimeout;
    this.readTimeout        = readTimeout;
    this.trustStorePath     = trustStorePath;
    this.trustStorePassword = trustStorePassword;
    this.trustStoreType     = trustStoreType;
    this.setupTruststoreForHttps = true;
  }

  public void setSetupTruststoreForHttps(boolean setupTruststoreForHttps) {
    this.setupTruststoreForHttps = setupTruststoreForHttps;
  }
  
  public boolean getSetupTruststoreForHttps() {
    return this.setupTruststoreForHttps;
  }

  // ----- StreamProvider ----------------------------------------------------

  @Override
  public InputStream readFrom(String spec, String requestMethod, String params) throws IOException {
    return processURL(spec, requestMethod, params, null).getInputStream();
  }

  @Override
  public InputStream readFrom(String spec) throws IOException {
    return readFrom(spec, "GET", null);
  }


  // ----- URLStreamProvider -------------------------------------------------

  /**
   * Get a URL connection from the given spec.
   *
   * @param spec           the String to parse as a URL
   * @param requestMethod  the HTTP method (GET,POST,PUT,etc.).
   * @param body           the body of the request; may be null
   * @param headers        the headers of the request; may be null
   *
   * @return a URL connection
   *
   * @throws IOException if the URL connection can not be established
   */
  public HttpURLConnection processURL(String spec, String requestMethod, String body, Map<String, List<String>> headers)
      throws IOException {

    return processURL(spec, requestMethod, body == null ? null : body.getBytes(), headers);
  }

  /**
   * Get a URL connection from the given spec.
   *
   * @param spec           the String to parse as a URL
   * @param requestMethod  the HTTP method (GET,POST,PUT,etc.).
   * @param body           the body of the request; may be null
   * @param headers        the headers of the request; may be null
   *
   * @return a URL connection
   *
   * @throws IOException if the URL connection can not be established
   */
  public HttpURLConnection processURL(String spec, String requestMethod, InputStream body, Map<String, List<String>> headers)
      throws IOException {

    return processURL(spec, requestMethod, body == null ? null : IOUtils.toByteArray(body), headers);
  }

  /**
   * Get a URL connection from the given spec.
   *
   * @param spec           the String to parse as a URL
   * @param requestMethod  the HTTP method (GET,POST,PUT,etc.).
   * @param body           the body of the request; may be null
   * @param headers        the headers of the request; may be null
   *
   * @return a URL connection
   *
   * @throws IOException if the URL connection can not be established
   */
  public HttpURLConnection processURL(String spec, String requestMethod, byte[] body, Map<String, List<String>> headers)
          throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("readFrom spec:{}", URLCredentialsHider.hideCredentials(spec));
    }

    URL url = new URL(spec);
    HttpURLConnection connection = (spec.startsWith("https") && this.setupTruststoreForHttps) ?
            getSSLConnection(url) : getConnection(url);

    AppCookieManager appCookieManager = getAppCookieManager();

    String appCookie = appCookieManager.getCachedAppCookie(spec);
    if (appCookie != null) {
      LOG.debug("Using cached app cookie for URL:{}", URLCredentialsHider.hideCredentials(spec));

      // allow for additional passed in cookies
      if (headers == null || headers.isEmpty()) {
        headers = Collections.singletonMap(COOKIE, Collections.singletonList(appCookie));
      } else {
        headers = new HashMap<>(headers);

        List<String> cookieList = headers.get(COOKIE);
        String       cookies    = cookieList == null || cookieList.isEmpty() ? null : cookieList.get(0);

        headers.put(COOKIE, Collections.singletonList(appendCookie(cookies, appCookie)));
      }
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

    if (body != null) {
      connection.getOutputStream().write(body);
    }

    if (url.getUserInfo() != null) {
      String basicAuth = String.format(BASIC_AUTH, new String(new Base64().encode(url.getUserInfo().getBytes())));
      connection.setRequestProperty(AUTHORIZATION, basicAuth);
    }

    int statusCode = connection.getResponseCode();
    if (statusCode == HttpStatus.SC_UNAUTHORIZED ) {
      String wwwAuthHeader = connection.getHeaderField(WWW_AUTHENTICATE);
      if (LOG.isInfoEnabled()) {
        LOG.info("Received WWW-Authentication header:" + wwwAuthHeader + ", for URL:" +
                   URLCredentialsHider.hideCredentials(spec));
      }
      if (wwwAuthHeader != null &&
        wwwAuthHeader.trim().startsWith(NEGOTIATE)) {
        connection = spec.startsWith("https") ?
           getSSLConnection(url) : getConnection(url);
        appCookie = appCookieManager.getAppCookie(spec, true);
        connection.setRequestProperty(COOKIE, appCookie);
        connection.setConnectTimeout(connTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setDoOutput(true);

        return connection;
      } else {
        // no supported authentication type found
        // we would let the original response propagate
        LOG.error("Unsupported WWW-Authentication header:" + wwwAuthHeader+ ", for URL:" +
                    URLCredentialsHider.hideCredentials(spec));
        return connection;
      }
    } else {
        // not a 401 Unauthorized status code
        // we would let the original response propagate
        if (statusCode == HttpStatus.SC_NOT_FOUND || statusCode == HttpStatus.SC_FORBIDDEN){
          LOG.error(String.format("Received HTTP %s response from URL: %s", statusCode,
                                  URLCredentialsHider.hideCredentials(spec)));
        }
        return connection;
    }
  }

  /**
   * Get the associated app cookie manager.
   *
   * @return the app cookie manager
   */
  public synchronized AppCookieManager getAppCookieManager() {
    if (appCookieManager == null) {
      appCookieManager = new AppCookieManager();
    }
    return appCookieManager;
  }

  /**
   * Utility method to append a new cookie value to an existing list of cookies.
   *
   * @param cookies    the semicolon separated list of cookie values
   * @param newCookie  a new cookie value to be appended.
   *
   * @return the new list of cookie values
   */
  public static String appendCookie(String cookies, String newCookie) {
    if (cookies == null || cookies.length() == 0) {
      return newCookie;
    }
    return cookies + "; " + newCookie;
  }

  public static class TrustAllHostnameVerifier implements HostnameVerifier
  {
    @Override
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  }

  public static class TrustAllManager implements X509TrustManager
  {
    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
      return new X509Certificate[0];
    }
    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
  }

  // ----- helper methods ----------------------------------------------------

  // Get a connection
  protected HttpURLConnection getConnection(URL url) throws IOException {
    URLConnection connection = url.openConnection();

    if (!setupTruststoreForHttps) {
      HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[] {
          new TrustAllManager()
      };

      // Ignore differences between given hostname and certificate hostname
      HostnameVerifier hostnameVerifier = new TrustAllHostnameVerifier();
      // Install the all-trusting trust manager
      try {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
        httpsConnection.setHostnameVerifier(hostnameVerifier);
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
        throw new IllegalStateException("Cannot create unverified ssl context.", e);
      }
    }

    return (HttpURLConnection) connection;
  }

  // Get an ssl connection
  protected HttpsURLConnection getSSLConnection(URL url) throws IOException, IllegalStateException {

    if (sslSocketFactory == null) {
      synchronized (this) {
        if (sslSocketFactory == null) {

          if (trustStorePath == null || trustStorePassword == null) {
            String msg =
                String.format("Can't get secure connection to %s.  Truststore path or password is not set.",
                              URLCredentialsHider.hideCredentials(url.toString()));

            LOG.error(msg);
            throw new IllegalStateException(msg);
          }
          FileInputStream in = null;
          try {
            in = new FileInputStream(new File(trustStorePath));
            KeyStore store = KeyStore.getInstance(trustStoreType == null ?
                KeyStore.getDefaultType() : trustStoreType);
            store.load(in, trustStorePassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(store);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            sslSocketFactory = context.getSocketFactory();
          } catch (Exception e) {
            throw new IOException("Can't get connection.", e);
          } finally {
            if (in != null) {
              in.close();
            }
          }
        }
      }
    }
    HttpsURLConnection connection = (HttpsURLConnection) (url
        .openConnection());

    connection.setSSLSocketFactory(sslSocketFactory);
 
    return connection;
  }
}
