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
import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.service.metrics.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import io.swagger.annotations.ApiOperation;

@StaticallyInject
@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class BoardApiService {
  private static final Logger LOG = LoggerFactory.getLogger(BoardApiService.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject
  private static DashboardService dashboardService;

  @GET
  @Path("/boards")
  @ApiOperation(value = "Lists monitoring dashboards")
  public Response list(@QueryParam("cluster_name") String clusterName, @QueryParam("query") String query) {
    try {
      return ok(dashboardService.list(clusterName, query, false));
    } catch (Exception e) {
      return error(e, "Unable to list dashboards");
    }
  }

  @GET
  @Path("/public-boards")
  @ApiOperation(value = "Lists public monitoring dashboards")
  public Response publicBoards(@QueryParam("cluster_name") String clusterName, @QueryParam("query") String query) {
    try {
      return ok(dashboardService.list(clusterName, query, true));
    } catch (Exception e) {
      return error(e, "Unable to list public dashboards");
    }
  }

  @POST
  @Path("/boards")
  @ApiOperation(value = "Creates a monitoring dashboard")
  public Response create(@QueryParam("cluster_name") String clusterName, String requestBody) {
    try {
      BoardRequest request = read(requestBody, BoardRequest.class);
      return ok(dashboardService.create(clusterName, request));
    } catch (Exception e) {
      return error(e, "Unable to create dashboard");
    }
  }

  @GET
  @Path("/board/{id}")
  @ApiOperation(value = "Returns a monitoring dashboard")
  public Response get(@PathParam("id") String id, @QueryParam("cluster_name") String clusterName) {
    try {
      return ok(dashboardService.get(clusterName, id, true));
    } catch (Exception e) {
      return error(e, "Unable to read dashboard");
    }
  }

  @GET
  @Path("/board/{id}/pure")
  @ApiOperation(value = "Returns monitoring dashboard metadata")
  public Response getPure(@PathParam("id") String id, @QueryParam("cluster_name") String clusterName) {
    try {
      return ok(dashboardService.get(clusterName, id, false));
    } catch (Exception e) {
      return error(e, "Unable to read dashboard metadata");
    }
  }

  @PUT
  @Path("/board/{id}")
  @ApiOperation(value = "Updates monitoring dashboard metadata")
  public Response update(@PathParam("id") long id, @QueryParam("cluster_name") String clusterName,
      String requestBody) {
    try {
      BoardRequest request = read(requestBody, BoardRequest.class);
      return ok(dashboardService.update(clusterName, id, request));
    } catch (Exception e) {
      return error(e, "Unable to update dashboard");
    }
  }

  @PUT
  @Path("/board/{id}/configs")
  @ApiOperation(value = "Updates a monitoring dashboard payload")
  public Response updateConfigs(@PathParam("id") String id, @QueryParam("cluster_name") String clusterName,
      String requestBody) {
    try {
      JsonNode request = readTree(requestBody);
      if (request == null || !request.path("configs").isTextual()) {
        throw new IllegalArgumentException("configs must be a raw JSON string");
      }
      return ok(dashboardService.updateConfigs(clusterName, id, request.path("configs").textValue()));
    } catch (Exception e) {
      return error(e, "Unable to update dashboard payload");
    }
  }

  @POST
  @Path("/board/{id}/clone")
  @ApiOperation(value = "Clones a monitoring dashboard")
  public Response clone(@PathParam("id") long id, @QueryParam("cluster_name") String clusterName) {
    try {
      return ok(dashboardService.clone(clusterName, id));
    } catch (Exception e) {
      return error(e, "Unable to clone dashboard");
    }
  }

  @POST
  @Path("/boards/clone")
  @ApiOperation(value = "Clones multiple monitoring dashboards")
  public Response cloneMany(@QueryParam("cluster_name") String clusterName, String requestBody) {
    try {
      JsonNode request = readTree(requestBody);
      JsonNode idsNode = request == null ? null : request.path("board_ids");
      if (idsNode == null || !idsNode.isArray()) {
        throw new IllegalArgumentException("board_ids must be an array");
      }
      List<Long> ids = new ArrayList<>();
      for (JsonNode id : idsNode) {
        if (!id.canConvertToLong()) {
          throw new IllegalArgumentException("Every board_ids value must be an integer");
        }
        ids.add(id.asLong());
      }
      return ok(dashboardService.cloneMany(clusterName, ids));
    } catch (Exception e) {
      return error(e, "Unable to clone dashboards");
    }
  }

  @DELETE
  @Path("/board/{id}")
  @ApiOperation(value = "Deletes a monitoring dashboard")
  public Response delete(@PathParam("id") long id, @QueryParam("cluster_name") String clusterName) {
    try {
      dashboardService.delete(clusterName, id);
      return ok(OBJECT_MAPPER.createObjectNode());
    } catch (Exception e) {
      return error(e, "Unable to delete dashboard");
    }
  }

  private Response ok(Object value) {
    ObjectNode body = OBJECT_MAPPER.createObjectNode();
    body.set("data", OBJECT_MAPPER.valueToTree(value));
    body.put("error", "");
    return json(Response.ok(), body);
  }

  private <T> T read(String requestBody, Class<T> type) throws Exception {
    if (requestBody == null || requestBody.isBlank()) {
      throw new IllegalArgumentException("Request body is required");
    }
    return OBJECT_MAPPER.readValue(requestBody, type);
  }

  private JsonNode readTree(String requestBody) throws Exception {
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
