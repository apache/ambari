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
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;

import org.apache.ambari.server.controller.utilities.StreamProvider;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * URL based implementation of a stream provider.
 */
public class URLStreamProvider implements StreamProvider {

  private final int connTimeout;
  private final int readTimeout;
  private final String path;
  private final String password;
  private final String type;
  private volatile SSLSocketFactory sslSocketFactory = null;

  /**
   * Provide the connection timeout for the underlying connection.
   *
   * @param connectionTimeout  time, in milliseconds, to attempt a connection
   * @param readTimeout        the read timeout in milliseconds
   */
  public URLStreamProvider(int connectionTimeout, int readTimeout,
                           String path, String password, String type) {
    this.connTimeout = connectionTimeout;
    this.readTimeout = readTimeout;
    this.path        = path;
    this.password    = password;
    this.type        = type;
  }
  
  @Override
  public InputStream readFrom(String spec) throws IOException {

    URLConnection connection = spec.startsWith("https") ?
        getSSLConnection(spec) : getConnection(spec);

    connection.setConnectTimeout(connTimeout);
    connection.setReadTimeout(readTimeout);
    connection.setDoOutput(true);

    return connection.getInputStream();
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
            FileInputStream in    = new FileInputStream(new File(path));
            KeyStore        store = KeyStore.getInstance(type == null ? KeyStore.getDefaultType() : type);

            store.load(in, password.toCharArray());
            in.close();

            TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

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
    HttpsURLConnection connection = (HttpsURLConnection)(new URL(spec).openConnection());

    connection.setSSLSocketFactory(sslSocketFactory);

    return connection;
  }
}
