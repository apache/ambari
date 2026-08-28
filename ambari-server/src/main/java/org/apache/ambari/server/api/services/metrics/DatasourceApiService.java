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

import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.service.metrics.DatasourceService;
import org.apache.ambari.server.service.metrics.PrometheusQueryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import io.swagger.annotations.ApiOperation;

@StaticallyInject
@Path("/metrics/datasource")
@Produces(MediaType.APPLICATION_JSON)
public class DatasourceApiService {
  private static final Logger LOG = LoggerFactory.getLogger(DatasourceApiService.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject
  private static DatasourceService datasourceService;

  @Inject
  private static PrometheusQueryClient queryClient;

  @GET
  @ApiOperation(value = "Lists monitoring datasources")
  public Response list(@QueryParam("cluster_name") String clusterName) {
    try {
      return ok(datasourceService.list(clusterName));
    } catch (Exception e) {
      return error(e, "Unable to list datasources");
    }
  }

  @POST
  @Path("/list")
  @ApiOperation(value = "Lists monitoring datasources using a request body")
  public Response list(String requestBody, @QueryParam("cluster_name") String queryClusterName) {
    try {
      JsonNode request = readTree(requestBody);
      return ok(datasourceService.list(clusterName(request, queryClusterName)));
    } catch (Exception e) {
      return error(e, "Unable to list datasources");
    }
  }

  @GET
  @Path("/brief")
  @ApiOperation(value = "Lists compact monitoring datasource metadata")
  public Response brief(@QueryParam("cluster_name") String clusterName) {
    try {
      return ok(datasourceService.brief(clusterName));
    } catch (Exception e) {
      return error(e, "Unable to list datasource summaries");
    }
  }

  @POST
  @Path("/plugin/list")
  @ApiOperation(value = "Lists monitoring datasource plugins")
  public Response plugins(String requestBody, @QueryParam("cluster_name") String queryClusterName) {
    try {
      JsonNode request = readTree(requestBody);
      return ok(datasourceService.plugins(clusterName(request, queryClusterName)));
    } catch (Exception e) {
      return error(e, "Unable to list datasource plugins");
    }
  }

  @POST
  @Path("/query")
  @ApiOperation(value = "Queries monitoring datasources")
  public Response query(String requestBody, @QueryParam("cluster_name") String queryClusterName) {
    try {
      JsonNode request = readTree(requestBody);
      return ok(datasourceService.query(
          clusterName(request, queryClusterName),
          text(request, "type", text(request, "plugin_type", null)),
          text(request, "category", null),
          text(request, "name", null)));
    } catch (Exception e) {
      return error(e, "Unable to query datasources");
    }
  }

  @POST
  @Path("/desc")
  @ApiOperation(value = "Returns monitoring datasource plugin metadata")
  public Response description(String requestBody) {
    try {
      JsonNode request = readTree(requestBody);
      long id = requiredId(request);
      return ok(datasourceService.get(id, clusterName(request, null)));
    } catch (Exception e) {
      return error(e, "Unable to read datasource");
    }
  }

  @GET
  @Path("/{id}")
  @ApiOperation(value = "Returns a monitoring datasource")
  public Response get(@PathParam("id") long id, @QueryParam("cluster_name") String clusterName) {
    try {
      return ok(datasourceService.get(id, clusterName));
    } catch (Exception e) {
      return error(e, "Unable to read datasource");
    }
  }

  @GET
  @Path("/{id}/metadata")
  @ApiOperation(value = "Returns monitoring datasource metadata")
  public Response metadata(@PathParam("id") long id, @QueryParam("cluster_name") String clusterName) {
    try {
      DatasourceResponse datasource = datasourceService.get(id, clusterName);
      return ok(Map.of(
          "id", datasource.getId(),
          "name", datasource.getName(),
          "plugin_type", datasource.getPluginType(),
          "status", datasource.getStatus()));
    } catch (Exception e) {
      return error(e, "Unable to read datasource metadata");
    }
  }

  @POST
  @Path("/{id}/test")
  @ApiOperation(value = "Tests a monitoring datasource connection")
  public Response test(@PathParam("id") long id, @QueryParam("cluster_name") String clusterName) {
    try {
      datasourceService.verifyTestAccess(id, clusterName);
      JsonNode upstream = queryClient.testConnection(id);
      return ok(Map.of("status", "success", "upstream", upstream));
    } catch (Exception e) {
      return error(e, "Datasource connection test failed");
    }
  }

  @POST
  @Path("/upsert")
  @ApiOperation(value = "Creates or updates a monitoring datasource")
  public Response upsert(String requestBody) {
    try {
      JsonNode json = readTree(requestBody);
      if (json == null || !json.isObject()) {
        throw new IllegalArgumentException("Datasource request body is required");
      }
      DatasourceRequest request = OBJECT_MAPPER.treeToValue(json, DatasourceRequest.class);
      Long id = request.getId();
      return ok(id == null || id <= 0
          ? datasourceService.create(request)
          : datasourceService.update(id, request));
    } catch (Exception e) {
      return error(e, "Unable to save datasource");
    }
  }

  @POST
  @Path("/status/update")
  @ApiOperation(value = "Updates monitoring datasource status")
  public Response updateStatus(String requestBody) {
    try {
      JsonNode request = readTree(requestBody);
      long id = requiredId(request);
      String status = request.path("status").asText(null);
      return ok(datasourceService.updateStatus(id, clusterName(request, null), status));
    } catch (Exception e) {
      return error(e, "Unable to update datasource status");
    }
  }

  @DELETE
  @Path("/{id}")
  @ApiOperation(value = "Deletes a monitoring datasource")
  public Response delete(@PathParam("id") long id, @QueryParam("cluster_name") String clusterName) {
    try {
      datasourceService.delete(id, clusterName);
      return ok(Boolean.TRUE);
    } catch (Exception e) {
      return error(e, "Unable to delete datasource");
    }
  }

  private long requiredId(JsonNode request) {
    if (request == null || !request.path("id").canConvertToLong()) {
      throw new IllegalArgumentException("Datasource id is required");
    }
    return request.path("id").asLong();
  }

  private String clusterName(JsonNode request, String queryClusterName) {
    if (queryClusterName != null && !queryClusterName.isBlank()) {
      return queryClusterName;
    }
    String value = request == null ? null : request.path("cluster_name").asText(null);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("cluster_name is required");
    }
    return value;
  }

  private String text(JsonNode request, String field, String fallback) {
    if (request == null || !request.path(field).isTextual()) {
      return fallback;
    }
    return request.path(field).textValue();
  }

  private Response ok(Object value) {
    ObjectNode body = OBJECT_MAPPER.createObjectNode();
    body.set("data", OBJECT_MAPPER.valueToTree(value));
    body.put("error", "");
    return json(Response.ok(), body);
  }

  private JsonNode readTree(String requestBody) throws JsonProcessingException {
    if (requestBody == null || requestBody.isBlank()) {
      throw new IllegalArgumentException("Request body is required");
    }
    return OBJECT_MAPPER.readTree(requestBody);
  }

  private Response json(Response.ResponseBuilder response, JsonNode body) {
    return response.type(MediaType.APPLICATION_JSON_TYPE).entity(body.toString()).build();
  }

  private Response error(Exception exception, String operation) {
    int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    String message = operation;
    if (exception instanceof AuthorizationException) {
      status = Response.Status.FORBIDDEN.getStatusCode();
      message = exception.getMessage();
    } else if (exception instanceof IllegalArgumentException || exception instanceof JsonProcessingException) {
      status = exception.getMessage() != null && exception.getMessage().toLowerCase().contains("not found")
          ? Response.Status.NOT_FOUND.getStatusCode()
          : Response.Status.BAD_REQUEST.getStatusCode();
      message = exception.getMessage();
    } else if (exception instanceof IllegalStateException) {
      status = Response.Status.CONFLICT.getStatusCode();
      message = exception.getMessage();
    } else if (exception instanceof AmbariException) {
      message = exception.getMessage();
    }
    LOG.warn("{}: {}", operation, exception.getMessage());
    ObjectNode body = OBJECT_MAPPER.createObjectNode();
    body.putNull("data");
    body.put("error", message == null ? operation : message);
    return json(Response.status(status), body);
  }
}
