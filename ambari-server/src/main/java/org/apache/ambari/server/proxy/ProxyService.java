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

package org.apache.ambari.server.proxy;

import com.google.gson.Gson;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

@Path("/")
public class ProxyService {

  private static final int REPO_URL_CONNECT_TIMEOUT = 3000;
  private static final int REPO_URL_READ_TIMEOUT = 1000;
  private static final int HTTP_ERROR_RANGE_START = 400;

  private static final String REQUEST_TYPE_GET = "GET";
  private static final String REQUEST_TYPE_POST = "POST";
  private static final String REQUEST_TYPE_PUT = "PUT";
  private static final String REQUEST_TYPE_DELETE = "DELETE";
  private static final String QUERY_PARAMETER_URL = "url";
  private static final String ERROR_PROCESSING_URL = "Error occurred during processing URL ";

  private final static Logger LOG = LoggerFactory.getLogger(ProxyService.class);

  @GET
  public Response processGetRequestForwarding(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(REQUEST_TYPE_GET, ui, null, APPLICATION_FORM_URLENCODED);
  }

  @POST
  public Response processPostRequestForwarding(Object body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(REQUEST_TYPE_POST, ui, body, headers.getMediaType().toString());
  }

  @PUT
  public Response processPutRequestForwarding(Object body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(REQUEST_TYPE_PUT, ui, body, headers.getMediaType().toString());
  }

  @DELETE
  public Response processDeleteRequestForwarding(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(REQUEST_TYPE_DELETE, ui, null, APPLICATION_FORM_URLENCODED);
  }

  private Response handleRequest(String requestType, UriInfo ui, Object body, String mediaType) {
    URLStreamProvider urlStreamProvider = new URLStreamProvider(REPO_URL_CONNECT_TIMEOUT,
                                                REPO_URL_READ_TIMEOUT, null, null, null);
    List<String> urlsToForward = ui.getQueryParameters().get(QUERY_PARAMETER_URL);
    if (!urlsToForward.isEmpty()) {
      String url = urlsToForward.get(0);
      try {
        HttpURLConnection connection = urlStreamProvider.processURL(url, requestType, body, mediaType);
        int responseCode = connection.getResponseCode();
        if (responseCode >= HTTP_ERROR_RANGE_START) {
          throw new WebApplicationException(connection.getResponseCode());
        }
        String contentType = connection.getContentType();
        Response.ResponseBuilder rb = Response.status(responseCode);
        if (contentType.indexOf(APPLICATION_JSON) != -1) {
          rb.entity(new Gson().fromJson(new InputStreamReader(connection.getInputStream()), Map.class));
        } else {
          rb.entity(connection.getInputStream());
        }
        return rb.type(contentType).build();
      } catch (IOException e) {
        LOG.error(ERROR_PROCESSING_URL + url, e);
      }
    }
    return null;
  }

}
