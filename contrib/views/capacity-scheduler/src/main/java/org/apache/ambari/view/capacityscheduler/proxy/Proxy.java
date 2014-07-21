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

package org.apache.ambari.view.capacityscheduler.proxy;

import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.capacityscheduler.utils.ServiceFormattedException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Proxy with ability to make authorized request
 * with simple authorization headers
 */
public class Proxy {
  private static final Logger LOG = LoggerFactory.getLogger(Proxy.class);

  private URLStreamProvider urlStreamProvider;
  private boolean useAuthorization = false;
  private String username;
  private String password;
  private Map<String, String> customHeaders;

  /**
   * Constructor
   * @param urlStreamProvider url stream provider
   */
  public Proxy(URLStreamProvider urlStreamProvider) {
    this.urlStreamProvider = urlStreamProvider;
  }

  /**
   * Create RequestBuilder object with
   * initialized proxy options
   * @param url url
   * @return RequestBuilder instance
   */
  public RequestBuilder request(String url) {
    return new RequestBuilder(urlStreamProvider, url).
                  setHeaders(makeHeaders());
  }

  private HashMap<String, String> makeHeaders() {
    HashMap<String, String> headers = new HashMap<String, String>();
    headers.putAll(customHeaders);

    if (isUseAuthorization()) {
      String authString = username + ":" + password;
      byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
      String authStringEnc = new String(authEncBytes);

      headers.put("Authorization", "Basic " + authStringEnc);
    }
    return headers;
  }

  public boolean isUseAuthorization() {
    return useAuthorization;
  }

  public void setUseAuthorization(boolean useAuthorization) {
    this.useAuthorization = useAuthorization;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public URLStreamProvider getUrlStreamProvider() {
    return urlStreamProvider;
  }

  public void setUrlStreamProvider(URLStreamProvider urlStreamProvider) {
    this.urlStreamProvider = urlStreamProvider;
  }

  public Map<String, String> getCustomHeaders() {
    return customHeaders;
  }

  public void setCustomHeaders(Map<String, String> customHeaders) {
    this.customHeaders = customHeaders;
  }
}
