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
package org.apache.ambari.server.api.services.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.service.metrics.PrometheusClientException;
import org.apache.ambari.server.service.metrics.PrometheusQueryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import io.swagger.annotations.ApiOperation;

@StaticallyInject
@Path("/metrics/proxy/{datasourceId}/{proxyPath:.+}")
@Produces(MediaType.APPLICATION_JSON)
public class DatasourceProxyApiService {
  private static final Logger LOG = LoggerFactory.getLogger(DatasourceProxyApiService.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject
  private static PrometheusQueryClient queryClient;

  @GET
  @ApiOperation(value = "Proxies a GET request to a monitoring datasource")
  public Response get(@PathParam("datasourceId") long datasourceId,
      @PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo) {
    return proxy(datasourceId, proxyPath, uriInfo, "GET", null, null);
  }

  @POST
  @ApiOperation(value = "Proxies a POST request to a monitoring datasource")
  public Response post(@PathParam("datasourceId") long datasourceId,
      @PathParam("proxyPath") String proxyPath, @Context UriInfo uriInfo,
      @HeaderParam("Content-Type") String contentType, String body) {
    return proxy(datasourceId, proxyPath, uriInfo, "POST", body, contentType);
  }

  private Response proxy(long datasourceId, String proxyPath, UriInfo uriInfo,
      String method, String body, String contentType) {
    try {
      Map<String, List<String>> parameters = new LinkedHashMap<>();
      uriInfo.getQueryParameters().forEach((name, values) -> parameters.put(name, new ArrayList<>(values)));
      return json(Response.ok(), queryClient.proxy(
          datasourceId, proxyPath, parameters, method, body, contentType));
    } catch (Exception e) {
      int status = Response.Status.BAD_GATEWAY.getStatusCode();
      String message = "Datasource request failed";
      if (e instanceof AuthorizationException) {
        status = Response.Status.FORBIDDEN.getStatusCode();
        message = e.getMessage();
      } else if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
        status = Response.Status.BAD_REQUEST.getStatusCode();
        message = e.getMessage();
      } else if (e instanceof AmbariException) {
        status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
      } else if (e instanceof PrometheusClientException) {
        message = e.getMessage();
      }
      LOG.warn("Datasource proxy request failed: {}", e.getMessage());
      ObjectNode response = OBJECT_MAPPER.createObjectNode();
      response.put("status", "error");
      response.put("error", message == null ? "Datasource request failed" : message);
      return json(Response.status(status), response);
    }
  }

  private Response json(Response.ResponseBuilder response, JsonNode body) {
    return response.type(MediaType.APPLICATION_JSON_TYPE).entity(body.toString()).build();
  }
}
