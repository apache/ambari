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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
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
import java.util.HashMap;
import java.util.Map;

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

  private HttpResponse execute(HttpRequestBase post) {
    try (CloseableHttpResponse response = httpClient.execute(post)) {
      String responseBodyText = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
      int statusCode = response.getStatusLine().getStatusCode();
      LOG.info("Response code {} body {} ", statusCode, responseBodyText);
      if (!(200 <= statusCode && statusCode <= 299))
        throw new RuntimeException("Error while executing http request: " + responseBodyText);
      return new HttpResponse(statusCode, responseBodyText);
    } catch (ClientProtocolException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JobExecutionInfo startJob(String jobName, String parameters) {
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setScheme("http");
    uriBuilder.setPath(uriBuilder.getPath() + "/" + jobName);
    if (!isBlank(parameters))
      uriBuilder.addParameter("params", parameters);
    try {
      String responseText = execute(new HttpPost(uriBuilder.build())).getBody();
      Map<String, Object> responseContent = new ObjectMapper().readValue(responseText, new TypeReference<HashMap<String,Object>>() {});
      return new JobExecutionInfo(responseContent.get("jobId").toString(), ((Map)responseContent.get("jobExecutionData")).get("id").toString());
    } catch (URISyntaxException | JsonParseException | JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void restartJob(String jobName, String jobId) {
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setScheme("http");
    uriBuilder.setPath(String.format("%s/%s/%s/executions", uriBuilder.getPath(), jobName, jobId));
    uriBuilder.addParameter("operation", "RESTART");
    try {
      HttpResponse httpResponse = execute(new HttpPost(uriBuilder.build()));
      if (httpResponse.getCode() != 200)
        throw new RuntimeException(httpResponse.getBody());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopJob(String jobExecutionId) {
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setScheme("http");
    uriBuilder.setPath(String.format("%s/executions/%s", uriBuilder.getPath(), jobExecutionId));
    uriBuilder.addParameter("operation", "STOP");
    try {
      execute(new HttpDelete(uriBuilder.build()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
