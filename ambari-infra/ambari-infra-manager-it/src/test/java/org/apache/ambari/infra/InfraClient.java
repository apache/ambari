/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import static org.apache.commons.lang.StringUtils.isBlank;

// TODO: use swagger
public class InfraClient implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(InfraClient.class);

  private final CloseableHttpClient httpClient;
  private final URI baseUrl;

  public InfraClient(String baseUrl) {
    try {
      this.baseUrl = new URI(baseUrl);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    httpClient = HttpClientBuilder.create().setRetryHandler(new DefaultHttpRequestRetryHandler(0, false)).build();
  }

  @Override
  public void close() throws Exception {
    httpClient.close();
  }

  // TODO: return job data
  public void getJobs() {
    execute(new HttpGet(baseUrl));
  }

  private String execute(HttpRequestBase post) {
    try (CloseableHttpResponse response = httpClient.execute(post)) {
      String responseBodyText = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
      LOG.info("Response code {} body {} ", response.getStatusLine().getStatusCode(), responseBodyText);
      return responseBodyText;
    } catch (ClientProtocolException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  // TODO: return job data
  public void startJob(String jobName, String parameters) {
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setScheme("http");
    uriBuilder.setPath(uriBuilder.getPath() + "/" + jobName);
    if (!isBlank(parameters))
      uriBuilder.addParameter("params", parameters);
    try {
      execute(new HttpPost(uriBuilder.build()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
