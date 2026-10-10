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
package org.apache.ambari.server.api.services;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.mpack.MpackCatalog;
import org.apache.ambari.server.mpack.MpackException;
import org.apache.ambari.server.mpack.MpackJson;
import org.apache.ambari.server.mpack.MpackLifecycleService;
import org.apache.ambari.server.mpack.MpackLifecycleState;
import org.apache.ambari.server.mpack.MpackManifest;
import org.apache.ambari.server.security.authorization.AuthorizationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

/** Global pack management is available before any cluster exists. */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@StaticallyInject
public class MpackLifecycleApiService {
  private static com.google.inject.Provider<MpackLifecycleService> lifecycle;
  private static com.google.inject.Provider<MpackCatalog> catalog;
  @Inject private static com.google.inject.Provider<org.apache.ambari.server.mpack.MpackUsage> usage;
  @Inject private static com.google.inject.Provider<org.apache.ambari.server.mpack.MpackServiceCatalog> services;

  @GET
  @Path("mpack_services")
  public Response services() throws AuthorizationException {
    MpackLifecycleService.authorize();
    return ok(services.get().view());
  }

  @POST
  @Path("mpack_service_plans")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response servicePlan(String body) throws AuthorizationException {
    MpackLifecycleService.authorize();
    bounded(body);
    return Response.status(Response.Status.CREATED).entity(MpackJson.tree(lifecycle.get().planServices(
        MpackJson.decode(body, org.apache.ambari.server.mpack.MpackServiceCatalog.Selection.class)))).build();
  }

  @Inject
  public static void initialize(com.google.inject.Provider<MpackLifecycleService> service,
      com.google.inject.Provider<MpackCatalog> inventory) {
    lifecycle = service;
    catalog = inventory;
  }

  public static void initialize(MpackLifecycleService service, MpackCatalog inventory) {
    initialize(() -> service, () -> inventory);
  }

  @GET
  @Path("mpack_capabilities")
  public Response capabilities() throws AuthorizationException {
    MpackLifecycleService.authorize();
    return ok(Map.of("schema_version", 1, "manifest_schema_versions", java.util.List.of(1),
        "artifact_types", MpackManifest.ARTIFACT_TYPES.stream().sorted().toList(),
        "binding_scope", "STACK_VERSION", "per_cluster_definition_versions", false,
        "operations", java.util.List.of("IMPORT", "ENABLE", "INSTALL", "UPDATE", "BIND", "UNBIND", "UNINSTALL"),
        "operation_recovery", "RECONCILE_RECEIPTS", "resource_identity", "IMMUTABLE_SNAPSHOT",
        "target_stack_versions", lifecycle.get().targets(), "required_agent_protocol", "MPACK_RESOURCES_V1"));
  }

  @GET
  @Path("mpack_capabilities/manifest_schema")
  public Response manifestSchema() throws AuthorizationException, java.io.IOException {
    MpackLifecycleService.authorize();
    try (InputStream schema = getClass().getClassLoader().getResourceAsStream("mpack/manifest-v1.schema.json")) {
      if (schema == null) {
        throw new MpackException(MpackException.Code.STORAGE_FAILURE, "The published manifest schema is unavailable");
      }
      return ok(Map.of("schema_version", 1, "manifest_schema", MpackJson.read(schema.readAllBytes())));
    }
  }

  @POST
  @Path("mpack_uploads")
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  public Response upload(InputStream input, @HeaderParam("X-Content-SHA256") String digest)
      throws AuthorizationException {
    return Response.status(Response.Status.CREATED).entity(MpackJson.tree(lifecycle.get().upload(input, digest))).build();
  }

  @POST
  @Path("mpack_plans")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response plan(String body) throws AuthorizationException {
    MpackLifecycleService.authorize();
    bounded(body);
    MpackLifecycleState.Mutation request = MpackJson.decode(body, MpackLifecycleState.Mutation.class);
    return Response.status(Response.Status.CREATED).entity(MpackJson.tree(lifecycle.get().plan(request))).build();
  }

  @GET
  @Path("mpack_plans/{id}")
  public Response getPlan(@PathParam("id") String id) throws AuthorizationException {
    MpackLifecycleService.authorize();
    return ok(catalog.get().plan(id).value());
  }

  @POST
  @Path("mpack_operations")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response accept(String body, @HeaderParam("Idempotency-Key") String idempotency)
      throws AuthorizationException {
    MpackLifecycleService.authorize();
    bounded(body);
    JsonNode request = MpackJson.read(body);
    MpackJson.fields(request, Set.of("schema_version", "plan_id"));
    if (!request.has("schema_version") || !request.get("schema_version").isIntegralNumber()
        || !request.get("schema_version").canConvertToInt()
        || request.get("schema_version").intValue() != 1) {
      throw new MpackException(MpackException.Code.UNSUPPORTED_SCHEMA, "Operation schema_version must be 1");
    }
    MpackLifecycleState.Operation operation = lifecycle.get().accept(MpackJson.string(request, "plan_id"), idempotency);
    return Response.accepted(MpackJson.tree(operation))
        .location(URI.create("/api/v1/mpack_operations/" + operation.id())).build();
  }

  @GET
  @Path("mpack_operations")
  public Response operations() throws AuthorizationException {
    return ok(Map.of("schema_version", 1, "items", lifecycle.get().operations().stream().map(operation -> {
      MpackLifecycleState.Plan plan = catalog.get().plan(operation.planId()).value();
      com.fasterxml.jackson.databind.node.ObjectNode item = (com.fasterxml.jackson.databind.node.ObjectNode) MpackJson.tree(operation);
      item.put("action", plan.mutation().action().name());
      item.set("service_names", MpackJson.tree(plan.deployment() == null ? java.util.List.of() : plan.deployment().serviceNames()));
      item.set("release_ids", MpackJson.tree(plan.mutation().releaseIds().isEmpty()
          ? plan.releases().stream().filter(release -> plan.mutation().archiveDigests().contains(release.archiveDigest()))
              .map(MpackLifecycleState.Release::id).toList() : plan.mutation().releaseIds()));
      return item;
    }).toList()));
  }

  @GET
  @Path("mpack_operations/{id}")
  public Response operation(@PathParam("id") String id) throws AuthorizationException {
    return ok(lifecycle.get().operation(id));
  }

  @POST
  @Path("mpack_operations/{id}/recover")
  public Response recover(@PathParam("id") String id) throws AuthorizationException {
    return Response.accepted(MpackJson.tree(lifecycle.get().recover(id))).build();
  }

  @POST
  @Path("mpack_operations/{id}/retry")
  public Response retry(@PathParam("id") String id) throws AuthorizationException {
    return Response.accepted(MpackJson.tree(lifecycle.get().retryFailedHooks(id))).build();
  }

  @POST
  @Path("mpack_operations/{id}/cancel")
  public Response cancel(@PathParam("id") String id) throws AuthorizationException {
    return ok(lifecycle.get().cancel(id));
  }

  @GET
  @Path("mpack_operations/{id}/deployment")
  public Response deployment(@PathParam("id") String id) throws AuthorizationException {
    return ok(lifecycle.get().deployment(id));
  }

  @GET
  @Path("mpack_operations/{id}/members")
  public Response members(@PathParam("id") String id) throws AuthorizationException {
    return ok(Map.of("schema_version", 1, "items", lifecycle.get().members(id)));
  }

  @GET
  @Path("mpacks")
  public Response releases() throws AuthorizationException {
    return ok(Map.of("schema_version", 1, "items", lifecycle.get().releases()));
  }

  @GET
  @Path("mpacks/{name}/versions/{version}")
  public Response release(@PathParam("name") String name, @PathParam("version") String version)
      throws AuthorizationException {
    MpackLifecycleService.authorize();
    return ok(catalog.get().release(MpackManifest.requireName(name) + "/" + MpackManifest.requireVersion(version)).value());
  }

  @GET
  @Path("mpack_bindings")
  public Response bindings() throws AuthorizationException {
    return ok(lifecycle.get().bindings());
  }

  @GET
  @Path("mpacks/{name}/versions/{version}/usages")
  public Response usages(@PathParam("name") String name, @PathParam("version") String version)
      throws AuthorizationException {
    MpackLifecycleService.authorize();
    return ok(usage.get().release(MpackManifest.requireName(name) + "/" + MpackManifest.requireVersion(version)));
  }

  private static Response ok(Object value) {
    return Response.ok(MpackJson.tree(value)).build();
  }

  private static void bounded(String body) {
    if (body == null || body.length() > 1024 * 1024 || body.getBytes(StandardCharsets.UTF_8).length > 1024 * 1024) {
      throw new MpackException(MpackException.Code.UPLOAD_LIMIT, "Lifecycle request exceeds the size limit");
    }
  }
}
