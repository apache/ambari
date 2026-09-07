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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import io.swagger.annotations.ApiOperation;

@StaticallyInject
@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class PrometheusApiService {
  private static final Logger LOG = LoggerFactory.getLogger(PrometheusApiService.class);
  private static final int MAX_BATCH_QUERIES = 64;
  private static final long MAX_POINTS = 11_000;
  private static final int MAX_QUERY_LENGTH = 65_536;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject
  private static PrometheusQueryClient prometheusClient;

  @GET
  @Path("/{datasourceId}/api/v1/query")
  @ApiOperation(value = "Executes a Prometheus instant query")
  public Response query(@PathParam("datasourceId") long datasourceId,
      @QueryParam("query") String query, @QueryParam("time") String time) {
    try {
      validateQuery(query);
      return json(Response.ok(), prometheusClient.get(datasourceId, "api/v1/query",
          PrometheusQueryClient.parameters("query", query, "time", time)));
    } catch (Exception e) {
      return error(e, "Prometheus instant query failed");
    }
  }

  @GET
  @Path("/{datasourceId}/api/v1/query_range")
  @ApiOperation(value = "Executes a Prometheus range query")
  public Response queryRange(@PathParam("datasourceId") long datasourceId,
      @QueryParam("query") String query, @QueryParam("start") String start,
      @QueryParam("end") String end, @QueryParam("step") String step) {
    try {
      validateRange(query, start, end, step);
      return json(Response.ok(), prometheusClient.get(datasourceId, "api/v1/query_range",
          PrometheusQueryClient.parameters("query", query, "start", start, "end", end, "step", step)));
    } catch (Exception e) {
      return error(e, "Prometheus range query failed");
    }
  }

  @POST
  @Path("/query-range-batch")
  @ApiOperation(value = "Executes a batch of Prometheus range queries")
  public Response queryRangeBatch(String requestBody) {
    return batch(requestBody, true);
  }

  @POST
  @Path("/query-instant-batch")
  @ApiOperation(value = "Executes a batch of Prometheus instant queries")
  public Response queryInstantBatch(String requestBody) {
    return batch(requestBody, false);
  }

  @GET
  @Path("/{datasourceId}/api/v1/labels")
  @ApiOperation(value = "Returns Prometheus label names")
  public Response labels(@PathParam("datasourceId") long datasourceId, @Context UriInfo uriInfo) {
    return proxy(datasourceId, "api/v1/labels", queryParameters(uriInfo));
  }

  @GET
  @Path("/{datasourceId}/api/v1/label/{name}/values")
  @ApiOperation(value = "Returns values for a Prometheus label")
  public Response labelValues(@PathParam("datasourceId") long datasourceId,
      @PathParam("name") String name, @Context UriInfo uriInfo) {
    if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
      return error(new IllegalArgumentException("Invalid Prometheus label name"), "Label query failed");
    }
    return proxy(datasourceId, "api/v1/label/" + name + "/values", queryParameters(uriInfo));
  }

  @GET
  @Path("/{datasourceId}/api/v1/series")
  @ApiOperation(value = "Returns Prometheus time series")
  public Response series(@PathParam("datasourceId") long datasourceId, @Context UriInfo uriInfo) {
    Map<String, List<String>> parameters = queryParameters(uriInfo);
    if (!parameters.containsKey("match[]") && !parameters.containsKey("match")) {
      return error(new IllegalArgumentException("match[] parameter is required"), "Series query failed");
    }
    return proxy(datasourceId, "api/v1/series", parameters);
  }

  @GET
  @Path("/{datasourceId}/api/v1/metadata")
  @ApiOperation(value = "Returns Prometheus metric metadata")
  public Response metadata(@PathParam("datasourceId") long datasourceId, @Context UriInfo uriInfo) {
    return proxy(datasourceId, "api/v1/metadata", queryParameters(uriInfo));
  }

  @GET
  @Path("/{datasourceId}/api/v1/targets")
  @ApiOperation(value = "Returns Prometheus scrape targets")
  public Response targets(@PathParam("datasourceId") long datasourceId, @Context UriInfo uriInfo) {
    return proxy(datasourceId, "api/v1/targets", queryParameters(uriInfo));
  }

  private Response batch(String requestBody, boolean range) {
    try {
      if (requestBody == null || requestBody.isBlank()) {
        throw new IllegalArgumentException("Request body is required");
      }
      JsonNode request = OBJECT_MAPPER.readTree(requestBody);
      if (request == null || !request.isObject() || !request.path("datasource_id").canConvertToLong()) {
        throw new IllegalArgumentException("datasource_id is required");
      }
      JsonNode queries = request.path("queries");
      if (!queries.isArray() || queries.isEmpty()) {
        throw new IllegalArgumentException("queries must be a non-empty array");
      }
      if (queries.size() > MAX_BATCH_QUERIES) {
        throw new IllegalArgumentException("A batch can contain at most " + MAX_BATCH_QUERIES + " queries");
      }

      long datasourceId = request.path("datasource_id").asLong();
      ArrayNode results = OBJECT_MAPPER.createArrayNode();
      for (JsonNode item : queries) {
        ObjectNode itemResult = OBJECT_MAPPER.createObjectNode();
        if (item != null && item.hasNonNull("refId")) {
          itemResult.put("refId", item.path("refId").asText());
        }
        try {
          String query = requiredText(item, "query");
          JsonNode response;
          if (range) {
            String start = requiredScalar(item, "start");
            String end = requiredScalar(item, "end");
            String step = requiredScalar(item, "step");
            validateRange(query, start, end, step);
            response = prometheusClient.get(datasourceId, "api/v1/query_range",
                PrometheusQueryClient.parameters("query", query, "start", start, "end", end, "step", step));
          } else {
            String time = item.hasNonNull("time") ? item.get("time").asText() : null;
            validateQuery(query);
            response = prometheusClient.get(datasourceId, "api/v1/query",
                PrometheusQueryClient.parameters("query", query, "time", time));
          }
          itemResult.put("status", "success");
          itemResult.set("result", prometheusResult(response));
        } catch (Exception itemException) {
          itemResult.put("status", "error");
          itemResult.put("errorType", itemException.getClass().getSimpleName());
          itemResult.put("error", itemException.getMessage() == null
              ? "Prometheus query failed" : itemException.getMessage());
        }
        results.add(itemResult);
      }
      ObjectNode body = OBJECT_MAPPER.createObjectNode();
      body.put("status", "success");
      body.set("data", results);
      body.put("error", "");
      return json(Response.ok(), body);
    } catch (Exception e) {
      return error(e, range ? "Prometheus range batch failed" : "Prometheus instant batch failed");
    }
  }

  private Response proxy(long datasourceId, String path, Map<String, List<String>> parameters) {
    try {
      return json(Response.ok(), prometheusClient.get(datasourceId, path, parameters));
    } catch (Exception e) {
      return error(e, "Prometheus request failed");
    }
  }

  private JsonNode prometheusResult(JsonNode response) throws PrometheusClientException {
    if (!"success".equals(response.path("status").asText()) || !response.path("data").path("result").isArray()) {
      throw new PrometheusClientException("Prometheus returned an unsuccessful query response");
    }
    return response.path("data").path("result");
  }

  private Map<String, List<String>> queryParameters(UriInfo uriInfo) {
    Map<String, List<String>> parameters = new LinkedHashMap<>();
    uriInfo.getQueryParameters().forEach((name, values) -> parameters.put(name, new ArrayList<>(values)));
    return parameters;
  }

  private void validateQuery(String query) {
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("query is required");
    }
    if (query.length() > MAX_QUERY_LENGTH) {
      throw new IllegalArgumentException("query is too long");
    }
  }

  private void validateRange(String query, String start, String end, String step) {
    validateQuery(query);
    if (start == null || end == null || step == null) {
      throw new IllegalArgumentException("start, end, and step are required");
    }
    double startValue;
    double endValue;
    double stepValue;
    try {
      startValue = Double.parseDouble(start);
      endValue = Double.parseDouble(end);
      stepValue = Double.parseDouble(step);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("start, end, and step must be numeric", e);
    }
    if (!Double.isFinite(startValue) || !Double.isFinite(endValue) || !Double.isFinite(stepValue)
        || stepValue <= 0 || startValue >= endValue) {
      throw new IllegalArgumentException("Invalid range query time values");
    }
    if ((endValue - startValue) / stepValue > MAX_POINTS) {
      throw new IllegalArgumentException("Range query exceeds the " + MAX_POINTS + " point limit");
    }
  }

  private String requiredText(JsonNode item, String name) {
    if (item == null || !item.isObject() || !item.path(name).isTextual() || item.path(name).asText().isBlank()) {
      throw new IllegalArgumentException(name + " is required for every query");
    }
    return item.path(name).asText();
  }

  private String requiredScalar(JsonNode item, String name) {
    JsonNode value = item.path(name);
    if (!value.isNumber() && !value.isTextual()) {
      throw new IllegalArgumentException(name + " is required for every query");
    }
    return value.asText();
  }

  private Response error(Exception exception, String operation) {
    int status = Response.Status.BAD_GATEWAY.getStatusCode();
    String message = operation;
    if (exception instanceof AuthorizationException) {
      status = Response.Status.FORBIDDEN.getStatusCode();
      message = exception.getMessage();
    } else if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException
        || exception instanceof JsonProcessingException) {
      status = Response.Status.BAD_REQUEST.getStatusCode();
      message = exception.getMessage();
    } else if (exception instanceof AmbariException) {
      status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    } else if (exception instanceof PrometheusClientException) {
      message = exception.getMessage();
    }
    LOG.warn("{}: {}", operation, exception.getMessage());
    ObjectNode body = OBJECT_MAPPER.createObjectNode();
    body.put("status", "error");
    body.put("errorType", status == 403 ? "forbidden" : "upstream");
    body.put("error", message == null ? operation : message);
    return json(Response.status(status), body);
  }

  private Response json(Response.ResponseBuilder response, JsonNode body) {
    return response.type(MediaType.APPLICATION_JSON_TYPE).entity(body.toString()).build();
  }
}
