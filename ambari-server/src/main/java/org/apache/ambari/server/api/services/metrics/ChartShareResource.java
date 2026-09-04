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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.service.metrics.ChartShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.google.inject.Inject;

import io.swagger.annotations.ApiOperation;

@StaticallyInject
@Path("/metrics/share-charts")
@Produces(MediaType.APPLICATION_JSON)
public class ChartShareResource {
  private static final Logger LOG = LoggerFactory.getLogger(ChartShareResource.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject
  private static ChartShareService chartShareService;

  @GET
  @ApiOperation(value = "Returns shared monitoring charts")
  public Response get(@QueryParam("cluster_name") String clusterName, @QueryParam("ids") String rawIds) {
    try {
      if (rawIds == null || rawIds.isBlank()) {
        throw new IllegalArgumentException("ids is required");
      }
      List<Long> ids = new ArrayList<>();
      for (String value : rawIds.split(",")) {
        ids.add(Long.parseLong(value.trim()));
      }
      return ok(chartShareService.get(clusterName, ids));
    } catch (Exception e) {
      return error(e, "Unable to read chart shares");
    }
  }

  @POST
  @ApiOperation(value = "Creates shared monitoring charts")
  public Response create(@QueryParam("cluster_name") String clusterName, String requestBody) {
    try {
      if (requestBody == null || requestBody.isBlank()) {
        throw new IllegalArgumentException("Request body is required");
      }
      CollectionType type = OBJECT_MAPPER.getTypeFactory()
          .constructCollectionType(List.class, ChartShareRequest.class);
      List<ChartShareRequest> requests = OBJECT_MAPPER.readValue(requestBody, type);
      return ok(chartShareService.create(clusterName, requests));
    } catch (Exception e) {
      return error(e, "Unable to create chart shares");
    }
  }

  private Response ok(Object value) {
    ObjectNode body = OBJECT_MAPPER.createObjectNode();
    body.set("data", OBJECT_MAPPER.valueToTree(value));
    body.put("error", "");
    return json(Response.ok(), body);
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
    } else if (exception instanceof IllegalArgumentException || exception instanceof NumberFormatException
        || exception instanceof JsonProcessingException) {
      status = Response.Status.BAD_REQUEST.getStatusCode();
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
