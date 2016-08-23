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
package org.apache.ambari.logsearch.util;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.ambari.logsearch.web.security.LogsearchAbstractAuthenticationProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.filter.LoggingFilter;
import org.springframework.stereotype.Component;

/**
 * Layer to send REST request to External server using jersey client
 */
@Component
public class ExternalServerClient {
  private static Logger LOG = Logger.getLogger(ExternalServerClient.class);
  private static final ThreadLocal<JerseyClient> localJerseyClient = new ThreadLocal<JerseyClient>(){
    @Override
    protected JerseyClient initialValue() {
      return JerseyClientBuilder.createClient();
    }
  };
  private String hostURL = "http://host:ip";// default
  private boolean enableLog = false;// default

  @PostConstruct
  public void initialization() {
    hostURL = PropertiesUtil.getProperty(
        LogsearchAbstractAuthenticationProvider.AUTH_METHOD_PROP_START_WITH
            + "external_auth.host_url", hostURL);
  }

  /**
   * Send GET request to an external server
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Object sendGETRequest(String url, Class klass, MultivaluedMap<String, String> queryParam,
                               String username, String password)
      throws Exception {
    url = hostURL + url;
    JerseyClient client = localJerseyClient.get();
    HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder().build();

    client.register(authFeature);
    if (enableLog) {
      client.register(LoggingFilter.class);
    }

    WebTarget target = client.target(url);
    LOG.debug("URL: " + url);
    for (Map.Entry<String, List<String>> entry : queryParam.entrySet()) {
      target = target.queryParam(entry.getKey(), entry.getValue());
      LOG.debug(
        String.format("Query parameter: name - %s  ; value - %s ;" + entry.getKey(), StringUtils.join(entry.getValue(),',')));
    }
    target
      .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, username)
      .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, password);
    Invocation.Builder invocationBuilder =  target.request(MediaType.APPLICATION_JSON_TYPE);
    try {
      return invocationBuilder.get().readEntity(klass);
    } catch (Exception e) {
      throw new Exception(e.getCause());
    } finally {
      localJerseyClient.remove();
    }
  }
}
