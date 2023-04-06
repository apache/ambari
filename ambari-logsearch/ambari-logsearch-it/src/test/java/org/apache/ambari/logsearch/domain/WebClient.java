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
package org.apache.ambari.logsearch.domain;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebClient {
  private static Logger LOG = LoggerFactory.getLogger(WebClient.class);

  private final String host;
  private final int port;

  public WebClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String get(String path) {
    JerseyClient jerseyClient = JerseyClientBuilder.createClient();
    HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder()
            .credentials("admin", "admin")
            .build();
    jerseyClient.register(authFeature);

    String url = String.format("http://%s:%d%s", host, port, path);

    LOG.info("Url: {}", url);

    WebTarget target = jerseyClient.target(url);
    Invocation.Builder invocationBuilder =  target.request(MediaType.APPLICATION_JSON_TYPE);
    return invocationBuilder.get().readEntity(String.class);
  }

  public String put(String path, String requestBody) {
    JerseyClient jerseyClient = JerseyClientBuilder.createClient();
    HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder()
            .credentials("admin", "admin")
            .build();
    jerseyClient.register(authFeature);

    String url = String.format("http://%s:%d%s", host, port, path);

    LOG.info("Url: {}", url);

    WebTarget target = jerseyClient.target(url);
    Invocation.Builder invocationBuilder =  target.request(MediaType.APPLICATION_JSON_TYPE);
    String response = invocationBuilder.put(Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE)).readEntity(String.class);

    LOG.info("Response: {}", response);

    return response;
  }

}
