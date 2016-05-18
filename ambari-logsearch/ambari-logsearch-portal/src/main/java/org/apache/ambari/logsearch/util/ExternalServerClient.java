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

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;

import org.apache.ambari.logsearch.web.security.LogsearchAbstractAuthenticationProvider;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Layer to send REST request to External server using jersey client
 */
@Component
public class ExternalServerClient {
  private static Logger LOG = Logger.getLogger(ExternalServerClient.class);
  private static final ThreadLocal<Client> localJerseyClient = new ThreadLocal<Client>();
  private DefaultApacheHttpClientConfig defaultConfig = new DefaultApacheHttpClientConfig();
  private String hostURL = "http://host:ip";// default
  private boolean enableLog = false;// default

  @PostConstruct
  public void initialization() {
    hostURL = PropertiesUtil.getProperty(
        LogsearchAbstractAuthenticationProvider.AUTH_METHOD_PROP_START_WITH
            + "external_auth.host_url", hostURL);
  }

  private Client getJerseyClient() {
    Client jerseyClient = localJerseyClient.get();
    if (jerseyClient == null) {
      jerseyClient = ApacheHttpClient.create(defaultConfig);
      localJerseyClient.set(jerseyClient);
    }
    return jerseyClient;
  }

  /**
   * Send GET Request to  External server
   * @param url
   * @param klass
   * @param queryParam
   * @param username
   * @param password
   * @return Response Object 
   * @throws UnknownHostException
   * @throws Exception
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Object sendGETRequest(String url, Class klass,
      MultivaluedMapImpl queryParam, String username, String password)
      throws UnknownHostException, Exception {
    // add host url
    url = hostURL + url;
    String parameters = getQueryParameter(queryParam);
    LOG.debug("URL: " + url + " query parameters are : " + parameters);
    WebResource.Builder builder = buildWebResourceBuilder(url, queryParam,
        username, password);
    try {
      return builder.get(klass);
    } catch (WebApplicationException webApplicationException) {
      String errMsg = webApplicationExceptionHandler(webApplicationException,
          url);
      throw new Exception(errMsg);
    } catch (UniformInterfaceException uniformInterfaceException) {
      String errMsg = uniformInterfaceExceptionHandler(
          uniformInterfaceException, url);
      throw new Exception(errMsg);
    } catch (ClientHandlerException clientHandlerException) {
      String errMsg = clientHandlerExceptionHandler(clientHandlerException, url);
      throw new Exception(errMsg);
    } catch (Exception e) {
      Object response = builder.get(Object.class);
      String errMsg = "URL: " + url + response.toString();
      LOG.error(errMsg);
      throw new Exception(errMsg);
    } finally {
      cleanup();
    }
  }

  private WebResource.Builder buildWebResourceBuilder(String url,
      MultivaluedMapImpl queryParam, String username, String password) {
    WebResource webResource = getJerseyClient().resource(url);
    // add filter
    if (enableLog) {
      webResource.addFilter(new LoggingFilter());
    }
    getJerseyClient().addFilter(new HTTPBasicAuthFilter(username, password));
    // add query param
    if (queryParam != null) {
      webResource = webResource.queryParams(queryParam);
    }
    WebResource.Builder builder = webResource.getRequestBuilder();
    return builder;
  }

  private String webApplicationExceptionHandler(
      WebApplicationException webApplicationException, String url) {
    Object object = null;
    try {
      object = webApplicationException.getResponse().getEntity();
    } catch (Exception e) {
      LOG.error(e.getLocalizedMessage());
    }
    String errMsg = null;
    if (object != null) {
      errMsg = object.toString();
    } else {
      errMsg = webApplicationException.getMessage();
    }
    errMsg = "URL: " + url + errMsg;
    LOG.error(errMsg);
    return errMsg;
  }

  private String uniformInterfaceExceptionHandler(
      UniformInterfaceException uniformInterfaceException, String url) {
    Object object = null;
    String errMsg = null;
    ClientResponse clientResponse = uniformInterfaceException.getResponse();
    try {
      object = clientResponse.getEntity(Object.class);
      if (object != null) {
        errMsg = object.toString();
      }
    } catch (Exception e) {
      InputStream inputStream = clientResponse.getEntityInputStream();
      try {
        errMsg = IOUtils.toString(inputStream);
      } catch (IOException e1) {
        LOG.error(e.getLocalizedMessage());
      }
    }
    if (errMsg == null) {
      errMsg = uniformInterfaceException.getLocalizedMessage();
    }
    LOG.error("url :" + url + " Response : " + errMsg);
    return errMsg;
  }

  private String clientHandlerExceptionHandler(
      ClientHandlerException clientHandlerException, String url) {
    String errMsg = clientHandlerException.getLocalizedMessage();
    errMsg = "URL: " + url + errMsg;
    LOG.error(errMsg);
    return errMsg;
  }

  private String getQueryParameter(MultivaluedMapImpl queryParam) {
    StringBuilder builder = new StringBuilder();
    if (queryParam != null) {
      builder.append(" Query param :");
      for (Entry<String, List<String>> entry : queryParam.entrySet()) {
        String name = entry.getKey();
        builder.append(" name : " + name + " " + "values : [");
        List<String> valuesList = entry.getValue();
        if (valuesList != null) {
          for (int index = 0; index < valuesList.size(); index++) {
            String value = valuesList.get(index);
            if (index > 0) {
              builder.append(",");
            }
            builder.append(value);
          }
        }
        builder.append("]");
      }
    }
    return builder.toString();
  }

  private void cleanup() {
    localJerseyClient.remove();
  }
}
