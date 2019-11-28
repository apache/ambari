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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.ambari.server.utils.URLCredentialsHider;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that provides support to work with URLs behind redirects.
 */
public class URLRedirectProvider {
  private static final Logger LOG = LoggerFactory.getLogger(URLRedirectProvider.class);

  private final int connTimeout;
  private final int readTimeout;

  public URLRedirectProvider(int connectionTimeout, int readTimeout) {
    this.connTimeout = connectionTimeout;
    this.readTimeout = readTimeout;
  }

  public RequestResult executeGet(String spec) throws IOException {
    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      HttpGet httpGet = new HttpGet(spec);

      RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(connTimeout)
        .setSocketTimeout(readTimeout).build();
      httpGet.setConfig(requestConfig);

      try (CloseableHttpResponse response = httpClient.execute(httpGet);) {
        final HttpEntity entity = response.getEntity();
        final InputStream is = entity.getContent();

        final int statusCode = response.getStatusLine().getStatusCode();
        final RequestResult result = new RequestResult(IOUtils.toString(is, StandardCharsets.UTF_8), statusCode);

        if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_NOT_FOUND
          || statusCode == HttpStatus.SC_FORBIDDEN) {
          LOG.error(String.format("Received HTTP '%s' response from URL: '%s'", statusCode,
                                  URLCredentialsHider.hideCredentials(spec)));
        }
        return result;
      }
    }
  }

  public static class RequestResult {
    private final String content;
    private final int code;

    public RequestResult(String content, int code) {
      this.content = content;
      this.code = code;
    }

    public String getContent() {
      return content;
    }

    public int getCode() {
      return code;
    }
  }
}
