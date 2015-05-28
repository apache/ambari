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

package org.apache.ambari.view.utils.ambari;

import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.ambari.view.URLStreamProvider;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for URLStreamProvider that adds authentication header.
 * Also supports AmbariStreamProvider. readAs or readAsCurrent should not be used
 * with AmbariStreamProvider.
 */
public class URLStreamProviderBasicAuth implements URLStreamProvider {
  private URLStreamProvider urlStreamProvider;
  private AmbariStreamProvider ambariStreamProvider;
  private String username;
  private String password;
  private String requestedBy = "views";

  public URLStreamProviderBasicAuth(URLStreamProvider urlStreamProvider, String username, String password) {
    this.urlStreamProvider = urlStreamProvider;
    this.username = username;
    this.password = password;
  }

  public URLStreamProviderBasicAuth(AmbariStreamProvider urlStreamProvider) {
    this.ambariStreamProvider = urlStreamProvider;
  }

  /**
   * X-Requested-By header value
   * @param requestedBy value of X-Requested-By header
   */
  public void setRequestedBy(String requestedBy) {
    this.requestedBy = requestedBy;
  }

  @Override
  public InputStream readFrom(String url, String method, String data, Map<String, String> headers) throws IOException {
    if (urlStreamProvider != null) {
      return urlStreamProvider.readFrom(url, method, data, addHeaders(headers));
    } else {
      return ambariStreamProvider.readFrom(url, method, data, addHeaders(headers), true);
    }
  }

  @Override
  public InputStream readFrom(String url, String method, InputStream data, Map<String, String> headers) throws IOException {
    if (urlStreamProvider != null) {
      return urlStreamProvider.readFrom(url, method, data, addHeaders(headers));
    } else {
      return ambariStreamProvider.readFrom(url, method, data, addHeaders(headers), true);
    }
  }

  @Override
  public InputStream readAs(String url, String method, String data, Map<String, String> headers, String doAs) throws IOException {
    return urlStreamProvider.readAs(url, method, data, addHeaders(headers), doAs);
  }

  @Override
  public InputStream readAs(String url, String method, InputStream data, Map<String, String> headers, String doAs) throws IOException {
    return urlStreamProvider.readAs(url, method, data, addHeaders(headers), doAs);
  }

  @Override
  public InputStream readAsCurrent(String url, String method, String data, Map<String, String> headers) throws IOException {
    return urlStreamProvider.readAsCurrent(url, method, data, addHeaders(headers));
  }

  @Override
  public InputStream readAsCurrent(String url, String method, InputStream data, Map<String, String> headers) throws IOException {
    return urlStreamProvider.readAsCurrent(url, method, data, addHeaders(headers));
  }

  private HashMap<String, String> addHeaders(Map<String, String> customHeaders) {
    HashMap<String, String> newHeaders = new HashMap<String, String>();
    if (customHeaders != null)
      newHeaders.putAll(customHeaders);

    if (urlStreamProvider != null) {
      // basic auth is not needed for AmbariStreamProvider
      addBasicAuthHeaders(newHeaders);
    }
    addRequestedByHeaders(newHeaders);
    return newHeaders;
  }

  private void addRequestedByHeaders(HashMap<String, String> newHeaders) {
    newHeaders.put("X-Requested-By", requestedBy);
  }

  private void addBasicAuthHeaders(HashMap<String, String> headers) {
    String authString = username + ":" + password;
    byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
    String authStringEnc = new String(authEncBytes);

    headers.put("Authorization", "Basic " + authStringEnc);
  }
}
