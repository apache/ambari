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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ConsumerReference;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ProviderReference;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.PreviewSelection;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

@StaticallyInject
@Path("/service-dependencies")
public class ServiceDependenciesApiService {
  @Inject
  private static ManagedServiceDependencyCoordinator coordinator;

  @GET
  @Path("/candidates")
  public Response candidates(@QueryParam("consumer_scope") String consumerScope,
      @QueryParam("draft_id") String draftId,
      @QueryParam("expected_draft_revision") String revision,
      @QueryParam("cluster_id") String clusterId,
      @QueryParam("expected_workflow_revision") String workflowRevision,
      @QueryParam("dependency_type") String dependencyType) {
    return ManagedDependencyApiSupport.invoke(() -> {
      ConsumerReference consumer;
      if ("draft".equalsIgnoreCase(consumerScope)) {
        if (clusterId != null || workflowRevision != null) {
          throw new IllegalArgumentException("Draft and service plan fields cannot be mixed");
        }
        consumer = ConsumerReference.draft(
            ManagedDependencyApiSupport.uuid(draftId),
            ManagedDependencyApiSupport.positiveLong(revision, "expected_draft_revision"));
      } else if ("service_plan".equalsIgnoreCase(consumerScope)) {
        if (draftId != null || revision != null) {
          throw new IllegalArgumentException("Draft and service plan fields cannot be mixed");
        }
        consumer = ConsumerReference.servicePlan(
            ManagedDependencyApiSupport.positiveLong(clusterId, "cluster_id"),
            ManagedDependencyApiSupport.positiveLong(
                workflowRevision, "expected_workflow_revision"));
      } else {
        throw new IllegalArgumentException("consumer_scope must be draft or service_plan");
      }
      return Map.of("items", coordinator.candidates(consumer,
          ManagedDependencyApiSupport.type(dependencyType)));
    });
  }

  @POST
  @Path("/preview")
  public Response preview(String body) {
    return ManagedDependencyApiSupport.invoke(() -> {
      JsonNode root = ManagedDependencyApiSupport.body(body,
          Set.of("binding_id", "consumer", "dependency_type", "provider", "selections"));
      if (root.has("selections")) {
        if (root.size() != 2 || !root.has("consumer")
            || !root.get("selections").isArray()
            || root.get("selections").size() < 1 || root.get("selections").size() > 2) {
          throw new IllegalArgumentException(
              "A complete preview requires consumer and one or two selections");
        }
        JsonNode consumer = ManagedDependencyApiSupport.requiredObject(root, "consumer",
            Set.of("scope", "draft_id", "cluster_id", "expected_revision"));
        List<PreviewSelection> selections = new ArrayList<>();
        for (JsonNode selection : root.get("selections")) {
          selections.add(ManagedDependencyApiSupport.previewSelection(selection));
        }
        return coordinator.preview(consumerReference(consumer), selections);
      }
      JsonNode consumer = ManagedDependencyApiSupport.requiredObject(root, "consumer",
          Set.of("scope", "draft_id", "cluster_id", "expected_revision"));
      ConsumerReference consumerReference = consumerReference(consumer);
      JsonNode provider = ManagedDependencyApiSupport.requiredObject(root, "provider",
          Set.of("cluster_id", "service_name"));
      ManagedDependencyType type = ManagedDependencyApiSupport.type(
          ManagedDependencyApiSupport.text(root, "dependency_type"));
      return coordinator.preview(consumerReference, type,
          ManagedDependencyApiSupport.provider(provider),
          ManagedDependencyApiSupport.optionalUuid(root, "binding_id"));
    });
  }

  private static ConsumerReference consumerReference(JsonNode consumer) {
    String scope = ManagedDependencyApiSupport.text(consumer, "scope");
    if ("DRAFT".equals(scope)) {
      if (consumer.has("cluster_id")) {
        throw new IllegalArgumentException("Draft and service plan fields cannot be mixed");
      }
      return ConsumerReference.draft(
          ManagedDependencyApiSupport.uuid(
              ManagedDependencyApiSupport.text(consumer, "draft_id")),
          ManagedDependencyApiSupport.positiveLong(consumer, "expected_revision"));
    }
    if ("SERVICE_PLAN".equals(scope)) {
      if (consumer.has("draft_id")) {
        throw new IllegalArgumentException("Draft and service plan fields cannot be mixed");
      }
      return ConsumerReference.servicePlan(
          ManagedDependencyApiSupport.positiveLong(consumer, "cluster_id"),
          ManagedDependencyApiSupport.positiveLong(consumer, "expected_revision"));
    }
    if ("SERVICE".equals(scope)) {
      if (consumer.has("draft_id") || consumer.has("expected_revision")) {
        throw new IllegalArgumentException("Live service fields cannot include workflow identity");
      }
      return ConsumerReference.service(
          ManagedDependencyApiSupport.positiveLong(consumer, "cluster_id"));
    }
    throw new IllegalArgumentException("consumer scope must be DRAFT, SERVICE_PLAN, or SERVICE");
  }
}
