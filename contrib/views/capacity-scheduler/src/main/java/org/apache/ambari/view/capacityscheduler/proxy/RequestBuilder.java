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

import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.capacityscheduler.utils.ServiceFormattedException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Request builder with fluent interface
 */
public class RequestBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(RequestBuilder.class);

  //request options
  private String url = null;
  private String method = null;
  private String data = null;

  private HashMap<String, String> headers = null;

  private URLStreamProvider urlStreamProvider;

  /**
   * Constructor for RequestBuilder
   * @param urlStreamProvider url stream provider
   * @param url url
   */
  public RequestBuilder(URLStreamProvider urlStreamProvider, String url) {
    this.urlStreamProvider = urlStreamProvider;
    this.url = url;
  }

  /**
   * Shortcut for making GET request
   * @return ResponseTranslator object that encapsulates InputStream
   */
  public ResponseTranslator get() {
    setMethod("GET");
    return doRequest();
  }

  /**
   * Shortcut for making PUT request
   * @return ResponseTranslator object that encapsulates InputStream
   */
  public ResponseTranslator put() {
    setMethod("PUT");
    return doRequest();
  }

  /**
   * Shortcut for making POST request
   * @return ResponseTranslator object that encapsulates InputStream
   */
  public ResponseTranslator post() {
    setMethod("POST");
    return doRequest();
  }

  /**
   * Shortcut for making DELETE request
   * @return ResponseTranslator object that encapsulates InputStream
   */
  public ResponseTranslator delete() {
    setMethod("DELETE");
    return doRequest();
  }

  /**
   * Make request
   * @return ResponseTranslator object that encapsulates InputStream
   */
  public ResponseTranslator doRequest() {
    LOG.debug(String.format("%s Request to %s", method, url));
    InputStream inputStream = null;
    try {
      inputStream = urlStreamProvider.readFrom(url, method, data, headers);
    } catch (IOException e) {
      throw new ServiceFormattedException(e.getMessage(), e);
    }
    return new ResponseTranslator(inputStream);
  }

  public String getUrl() {
    return url;
  }

  public RequestBuilder setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getMethod() {
    return method;
  }

  public RequestBuilder setMethod(String method) {
    this.method = method;
    return this;
  }

  public String getData() {
    return data;
  }

  public RequestBuilder setData(String data) {
    this.data = data;
    return this;
  }

  public RequestBuilder setData(JSONObject data) {
    this.data = data.toString();
    return this;
  }

  public HashMap<String, String> getHeaders() {
    return headers;
  }

  public RequestBuilder setHeaders(HashMap<String, String> headers) {
    this.headers = headers;
    return this;
  }

  public URLStreamProvider getUrlStreamProvider() {
    return urlStreamProvider;
  }

  public RequestBuilder setUrlStreamProvider(URLStreamProvider urlStreamProvider) {
    this.urlStreamProvider = urlStreamProvider;
    return this;
  }
}
