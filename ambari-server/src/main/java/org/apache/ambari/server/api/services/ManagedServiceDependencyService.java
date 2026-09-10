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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyDeploymentCoordinator;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ConsumerReference;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.PreviewSelection;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Provider;

@StaticallyInject
public class ManagedServiceDependencyService {
  @Inject
  private static Provider<ManagedServiceDependencyCoordinator> coordinator;

  @Inject
  private static Provider<ManagedDependencyDeploymentCoordinator> deployments;

  private final String clusterName;
  private final String serviceName;

  public ManagedServiceDependencyService(String clusterName, String serviceName) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
  }

  @GET
  public Response list() {
    return ManagedDependencyApiSupport.invoke(
        () -> Map.of("items", coordinator.get().list(clusterName, serviceName)));
  }

  @GET
  @Path("/candidates")
  public Response candidates(@QueryParam("type") String dependencyType) {
    return ManagedDependencyApiSupport.invoke(() -> {
      long clusterId = coordinator.get().consumerClusterId(clusterName, serviceName);
      return Map.of("items", coordinator.get().candidates(ConsumerReference.service(clusterId),
          ManagedDependencyApiSupport.type(dependencyType)));
    });
  }

  @POST
  @Path("/preview")
  public Response preview(String body) {
    return ManagedDependencyApiSupport.invoke(() -> {
      JsonNode root = ManagedDependencyApiSupport.body(body,
          Set.of("binding_id", "dependency_type", "provider", "selections"));
      if (root.has("selections")) {
        if (root.size() != 1 || !root.get("selections").isArray()
            || root.get("selections").size() < 1 || root.get("selections").size() > 2) {
          throw new IllegalArgumentException(
              "A complete preview requires one or two selections");
        }
        List<PreviewSelection> selections = new ArrayList<>();
        for (JsonNode selection : root.get("selections")) {
          selections.add(ManagedDependencyApiSupport.previewSelection(selection));
        }
        long clusterId = coordinator.get().consumerClusterId(clusterName, serviceName);
        return coordinator.get().preview(ConsumerReference.service(clusterId), selections);
      }
      ManagedDependencyType type = ManagedDependencyApiSupport.type(
          ManagedDependencyApiSupport.text(root, "dependency_type"));
      JsonNode provider = ManagedDependencyApiSupport.requiredObject(root, "provider",
          Set.of("cluster_id", "service_name"));
      long clusterId = coordinator.get().consumerClusterId(clusterName, serviceName);
      return coordinator.get().preview(ConsumerReference.service(clusterId), type,
          ManagedDependencyApiSupport.provider(provider),
          ManagedDependencyApiSupport.optionalUuid(root, "binding_id"));
    });
  }

  @POST
  public Response create(String body) {
    return ManagedDependencyApiSupport.accepted(() -> {
      List<org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.CreateRequest>
          requests = ManagedDependencyApiSupport.createRequests(body);
      List<Map<String, Object>> responses = coordinator.get().create(clusterName, serviceName, requests);
      return ManagedDependencyApiSupport.isCreateCollection(body)
          ? Map.of("items", responses) : responses.get(0);
    });
  }

  @POST
  @Path("/deployments/{deploymentId}")
  public Response launchDeployment(@PathParam("deploymentId") String deploymentId, String body) {
    return ManagedDependencyApiSupport.accepted(() -> {
      coordinator.get().consumerClusterId(clusterName, serviceName);
      JsonNode root = ManagedDependencyApiSupport.body(body, Set.of("targets", "install_only"));
      if (!root.path("targets").isArray() || !root.path("install_only").isBoolean()) {
        throw new IllegalArgumentException("Exact targets and an explicit installation mode are required");
      }
      List<ManagedDependencyDeploymentCoordinator.Target> targets = new ArrayList<>();
      for (JsonNode target : root.get("targets")) {
        if (!target.isObject()) throw new IllegalArgumentException("A deployment target must be an object");
        target.fieldNames().forEachRemaining(field -> {
          if (!Set.of("serviceName", "componentName", "hostName", "hostId").contains(field)) {
            throw new IllegalArgumentException("Unknown deployment target field");
          }
        });
        for (String field : Set.of("serviceName", "componentName", "hostName")) {
          ManagedDependencyApiSupport.text(target, field);
        }
        if (target.has("hostId") && (!target.get("hostId").canConvertToLong()
            || !target.get("hostId").isIntegralNumber() || target.get("hostId").longValue() <= 0)) {
          throw new IllegalArgumentException("A host identity must be a positive integer");
        }
        targets.add(StageUtils.getGson().fromJson(target.toString(), ManagedDependencyDeploymentCoordinator.Target.class));
      }
      return deployments.get().launch(clusterName, ManagedDependencyApiSupport.uuid(deploymentId),
          targets, root.get("install_only").booleanValue());
    });
  }

  @GET
  @Path("/deployments/{deploymentId}")
  public Response getDeployment(@PathParam("deploymentId") String deploymentId) {
    return ManagedDependencyApiSupport.invoke(() -> {
      coordinator.get().consumerClusterId(clusterName, serviceName);
      return deployments.get().get(clusterName, ManagedDependencyApiSupport.uuid(deploymentId));
    });
  }

  @POST
  @Path("/deployments/{deploymentId}/actions/retry")
  public Response retryDeployment(@PathParam("deploymentId") String deploymentId, String body) {
    return ManagedDependencyApiSupport.accepted(() -> {
      coordinator.get().consumerClusterId(clusterName, serviceName);
      JsonNode root = ManagedDependencyApiSupport.body(body, Set.of("operation_id"));
      return deployments.get().retry(clusterName, ManagedDependencyApiSupport.uuid(deploymentId),
          ManagedDependencyApiSupport.uuid(ManagedDependencyApiSupport.text(root, "operation_id")));
    });
  }

  @POST
  @Path("/{bindingId}/actions/verify-credentials")
  public Response verifyCredentials(@PathParam("bindingId") String bindingId, String body) {
    return ManagedDependencyApiSupport.accepted(() -> {
      JsonNode root = ManagedDependencyApiSupport.body(body, Set.of("expected_epoch"));
      if (!root.path("expected_epoch").isIntegralNumber() || root.get("expected_epoch").longValue() <= 0) {
        throw new IllegalArgumentException("An exact operation epoch is required");
      }
      return coordinator.get().verifyManualCredentials(clusterName, serviceName,
          ManagedDependencyApiSupport.uuid(bindingId), root.get("expected_epoch").longValue());
    });
  }

  @GET
  @Path("/{bindingId}")
  public Response get(@PathParam("bindingId") String bindingId) {
    return ManagedDependencyApiSupport.invoke(() -> coordinator.get().get(clusterName, serviceName,
        ManagedDependencyApiSupport.uuid(bindingId)));
  }

  @GET
  @Path("/{bindingId}/preview-update")
  public Response previewUpdate(@PathParam("bindingId") String bindingId) {
    return ManagedDependencyApiSupport.invoke(() -> coordinator.get().previewUpdate(
        clusterName, serviceName, ManagedDependencyApiSupport.uuid(bindingId)));
  }

  @POST
  @Path("/{bindingId}/actions/update")
  public Response update(@PathParam("bindingId") String bindingId, String body) {
    return ManagedDependencyApiSupport.accepted(() -> coordinator.get().update(
        clusterName, serviceName, ManagedDependencyApiSupport.uuid(bindingId),
        ManagedDependencyApiSupport.updateRequest(body)));
  }

  @POST
  @Path("/{bindingId}/actions/retry")
  public Response retry(@PathParam("bindingId") String bindingId, String body) {
    return ManagedDependencyApiSupport.accepted(() -> coordinator.get().retry(
        clusterName, serviceName, ManagedDependencyApiSupport.uuid(bindingId),
        ManagedDependencyApiSupport.lifecycleRequest(body)));
  }

  @DELETE
  @Path("/{bindingId}")
  public Response detach(@PathParam("bindingId") String bindingId, String body) {
    return ManagedDependencyApiSupport.accepted(() -> coordinator.get().detach(
        clusterName, serviceName, ManagedDependencyApiSupport.uuid(bindingId),
        ManagedDependencyApiSupport.lifecycleRequest(body)));
  }
}
