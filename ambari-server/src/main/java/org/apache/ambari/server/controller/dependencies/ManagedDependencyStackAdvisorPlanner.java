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
package org.apache.ambari.server.controller.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.AdvisorSelection;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ConsumerReference;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ProviderReference;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.AdvisorSelectionRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Resolves caller plans into request-local, authorized Stack Advisor facts. */
@Singleton
public class ManagedDependencyStackAdvisorPlanner {
  public static final String PROPERTY_PREFIX = "managed_dependency_plan/";
  private static final int REQUIRED_PREVIEW_SCHEMA_VERSION = 2;
  private static final int MAX_SELECTIONS = 2;
  private static final Set<String> CONSUMER_FIELDS = Set.of(
      "scope", "draft_id", "cluster_id", "expected_revision");
  private static final Set<String> SELECTION_FIELDS = Set.of(
      "dependency_type", "binding_id", "provider/cluster_id", "provider/service_name",
      "preview_schema_version", "expected_provider_fingerprint",
      "expected_consumer_descriptor_fingerprint", "expected_snapshot_fingerprint");
  private static final Object TRUSTED_PERMIT = new Object();

  private final ManagedServiceDependencyCoordinator coordinator;

  @Inject
  public ManagedDependencyStackAdvisorPlanner(ManagedServiceDependencyCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  public static PlanRequest parse(Map<String, Object> properties) {
    boolean present = properties.keySet().stream()
        .anyMatch(property -> property.equals("managed_dependency_plan")
            || property.startsWith(PROPERTY_PREFIX));
    if (!present) {
      return null;
    }

    Set<String> allowedTopLevel = new HashSet<>();
    CONSUMER_FIELDS.forEach(field -> allowedTopLevel.add(PROPERTY_PREFIX + "consumer/" + field));
    allowedTopLevel.add(PROPERTY_PREFIX + "selections");
    for (String property : properties.keySet()) {
      if ((property.equals("managed_dependency_plan") || property.startsWith(PROPERTY_PREFIX))
          && !allowedTopLevel.contains(property)) {
        throw invalid("Unknown managed dependency plan field");
      }
    }

    ConsumerPlan consumer = parseConsumer(properties);
    Object rawSelections = properties.get(PROPERTY_PREFIX + "selections");
    if (!(rawSelections instanceof Collection<?> selections)
        || selections.isEmpty() || selections.size() > MAX_SELECTIONS) {
      throw invalid("The managed dependency plan requires one or two selections");
    }

    List<SelectionPlan> parsedSelections = new ArrayList<>();
    Set<ManagedDependencyType> types = EnumSet.noneOf(ManagedDependencyType.class);
    Set<UUID> bindingIds = new HashSet<>();
    for (Object rawSelection : selections) {
      if (!(rawSelection instanceof Map<?, ?> selection)) {
        throw invalid("Each managed dependency selection must be an object");
      }
      for (Object key : selection.keySet()) {
        if (!(key instanceof String) || !SELECTION_FIELDS.contains(key)) {
          throw invalid("Unknown managed dependency selection field");
        }
      }
      SelectionPlan parsed = parseSelection(selection);
      if (!types.add(parsed.type())) {
        throw invalid("Managed dependency types must be unique");
      }
      if (!bindingIds.add(parsed.bindingId())) {
        throw invalid("Managed dependency binding IDs must be unique");
      }
      parsedSelections.add(parsed);
    }
    return new PlanRequest(consumer, parsedSelections);
  }

  public TrustedPlan authorize(PlanRequest request, Long requestClusterId,
      String stackName, String stackVersion, Collection<String> services) {
    Objects.requireNonNull(request, "request");
    if (!services.contains("HBASE")) {
      throw invalid("A managed dependency plan requires the HBASE service");
    }
    if (request.consumer().scope() == ConsumerScope.DRAFT) {
      if (requestClusterId != null) {
        throw conflict("DEPENDENCY_CONSUMER_MISMATCH",
            "The advisor target does not match the managed dependency draft");
      }
    } else if (!Objects.equals(requestClusterId, request.consumer().clusterId())) {
      throw conflict("DEPENDENCY_CONSUMER_MISMATCH",
          "The advisor target does not match the managed dependency consumer");
    }

    List<AdvisorSelectionRequest> selections = request.selections().stream()
        .map(selection -> new AdvisorSelectionRequest(selection.type(), selection.provider(),
            selection.bindingId())).toList();
    List<AdvisorSelection> authorizedSelections = coordinator.authorizeAdvisorSelections(
        request.consumer().reference(), selections);
    if (authorizedSelections.size() != request.selections().size()) {
      throw conflict("DEPENDENCY_PREVIEW_STALE",
          "The managed dependency selection count changed during authorization");
    }
    Long authoritativeClusterId = null;
    String authoritativeStackName = null;
    String authoritativeStackVersion = null;
    Set<ManagedDependencyType> satisfied = EnumSet.noneOf(ManagedDependencyType.class);
    for (int index = 0; index < request.selections().size(); index++) {
      SelectionPlan selection = request.selections().get(index);
      AdvisorSelection authorized = authorizedSelections.get(index);
      verifySelection(selection, authorized);
      if (authoritativeStackName == null) {
        authoritativeClusterId = authorized.consumerClusterId();
        authoritativeStackName = authorized.consumerStackName();
        authoritativeStackVersion = authorized.consumerStackVersion();
      } else if (!Objects.equals(authoritativeClusterId, authorized.consumerClusterId())
          || !authoritativeStackName.equals(authorized.consumerStackName())
          || !authoritativeStackVersion.equals(authorized.consumerStackVersion())) {
        throw conflict("DEPENDENCY_CONSUMER_MISMATCH",
            "Managed dependency selections do not share one consumer");
      }
      satisfied.add(selection.type());
    }

    if (!Objects.equals(requestClusterId, authoritativeClusterId)
        || !Objects.equals(stackName, authoritativeStackName)
        || !Objects.equals(stackVersion, authoritativeStackVersion)) {
      throw conflict("DEPENDENCY_CONSUMER_MISMATCH",
          "The advisor stack or target does not match the managed dependency consumer");
    }
    return new TrustedPlan(TRUSTED_PERMIT, authoritativeClusterId, stackName, stackVersion,
        satisfied);
  }

  private static ConsumerPlan parseConsumer(Map<String, Object> properties) {
    String scopeValue = required(properties, PROPERTY_PREFIX + "consumer/scope");
    ConsumerScope scope;
    try {
      scope = ConsumerScope.valueOf(scopeValue);
    } catch (RuntimeException e) {
      throw invalid("Managed dependency consumer scope is invalid");
    }

    Set<String> fields = new HashSet<>();
    for (String field : CONSUMER_FIELDS) {
      if (properties.containsKey(PROPERTY_PREFIX + "consumer/" + field)) {
        fields.add(field);
      }
    }
    return switch (scope) {
      case DRAFT -> {
        requireFields(fields, Set.of("scope", "draft_id", "expected_revision"));
        yield new ConsumerPlan(scope,
            canonicalUuid(required(properties, PROPERTY_PREFIX + "consumer/draft_id")), null,
            positiveLong(required(properties, PROPERTY_PREFIX + "consumer/expected_revision"),
                "expected_revision"));
      }
      case SERVICE_PLAN -> {
        requireFields(fields, Set.of("scope", "cluster_id", "expected_revision"));
        yield new ConsumerPlan(scope, null,
            positiveLong(required(properties, PROPERTY_PREFIX + "consumer/cluster_id"), "cluster_id"),
            positiveLong(required(properties, PROPERTY_PREFIX + "consumer/expected_revision"),
                "expected_revision"));
      }
      case SERVICE -> {
        requireFields(fields, Set.of("scope", "cluster_id"));
        yield new ConsumerPlan(scope, null,
            positiveLong(required(properties, PROPERTY_PREFIX + "consumer/cluster_id"), "cluster_id"), 0);
      }
    };
  }

  private static SelectionPlan parseSelection(Map<?, ?> selection) {
    if (!selection.keySet().equals(SELECTION_FIELDS)) {
      throw invalid("Managed dependency selection fields are incomplete");
    }
    ManagedDependencyType type;
    try {
      type = ManagedDependencyType.valueOf(required(selection, "dependency_type"));
    } catch (RuntimeException e) {
      throw invalid("Dependency type must be HDFS or ZOOKEEPER");
    }
    UUID bindingId = canonicalUuid(required(selection, "binding_id"));
    ProviderReference provider = new ProviderReference(
        positiveLong(required(selection, "provider/cluster_id"), "provider cluster_id"),
        required(selection, "provider/service_name"));
    int schemaVersion = positiveInt(required(selection, "preview_schema_version"),
        "preview_schema_version");
    return new SelectionPlan(type, bindingId, provider, schemaVersion,
        fingerprint(selection, "expected_provider_fingerprint"),
        fingerprint(selection, "expected_consumer_descriptor_fingerprint"),
        fingerprint(selection, "expected_snapshot_fingerprint"));
  }

  private static void verifySelection(SelectionPlan expected, AdvisorSelection actual) {
    if (!expected.bindingId().equals(actual.bindingId())
        || expected.type() != actual.type()
        || expected.provider().clusterId() != actual.providerClusterId()
        || !expected.provider().serviceName().equals(actual.providerServiceName())
        || expected.previewSchemaVersion() != REQUIRED_PREVIEW_SCHEMA_VERSION
        || expected.previewSchemaVersion() != actual.previewSchemaVersion()
        || !expected.providerFingerprint().equals(actual.providerFingerprint())
        || !expected.consumerDescriptorFingerprint().equals(actual.consumerDescriptorFingerprint())
        || !expected.snapshotFingerprint().equals(actual.snapshotFingerprint())
        || !"HBASE".equals(actual.consumerServiceName())) {
      throw conflict("DEPENDENCY_PREVIEW_STALE",
          "Managed dependency facts changed; review the dependency plan again");
    }
  }

  private static void requireFields(Set<String> actual, Set<String> expected) {
    if (!actual.equals(expected)) {
      throw invalid("Managed dependency consumer fields do not match its scope");
    }
  }

  private static String fingerprint(Map<?, ?> values, String field) {
    String value = required(values, field);
    if (!value.matches("sha256:[0-9a-f]{64}")) {
      throw invalid(field + " must be a SHA-256 fingerprint");
    }
    return value;
  }

  private static String required(Map<?, ?> values, String field) {
    Object raw = values.get(field);
    if (!(raw instanceof String value) || value.isBlank() || !value.equals(value.trim())) {
      throw invalid(field + " must be a non-empty string");
    }
    return value;
  }

  private static long positiveLong(String value, String field) {
    try {
      long parsed = Long.parseLong(value);
      if (parsed <= 0 || !Long.toString(parsed).equals(value)) {
        throw new NumberFormatException();
      }
      return parsed;
    } catch (RuntimeException e) {
      throw invalid(field + " must be a positive integer");
    }
  }

  private static int positiveInt(String value, String field) {
    long parsed = positiveLong(value, field);
    if (parsed > Integer.MAX_VALUE) {
      throw invalid(field + " must be a positive integer");
    }
    return (int) parsed;
  }

  private static UUID canonicalUuid(String value) {
    try {
      UUID uuid = UUID.fromString(value);
      if (!uuid.toString().equals(value)) {
        throw new IllegalArgumentException();
      }
      return uuid;
    } catch (RuntimeException e) {
      throw invalid("Binding and draft IDs must be canonical lower-case UUIDs");
    }
  }

  private static ManagedDependencyIntegrationException invalid(String message) {
    return new ManagedDependencyIntegrationException(400, "INVALID_MANAGED_DEPENDENCY_PLAN", message);
  }

  private static ManagedDependencyIntegrationException conflict(String code, String message) {
    return new ManagedDependencyIntegrationException(409, code, message);
  }

  public enum ConsumerScope {
    DRAFT,
    SERVICE_PLAN,
    SERVICE
  }

  public record ConsumerPlan(ConsumerScope scope, UUID draftId, Long clusterId,
      long expectedRevision) {
    ConsumerReference reference() {
      return switch (scope) {
        case DRAFT -> ConsumerReference.draft(draftId, expectedRevision);
        case SERVICE_PLAN -> ConsumerReference.servicePlan(clusterId, expectedRevision);
        case SERVICE -> ConsumerReference.service(clusterId);
      };
    }
  }

  public record SelectionPlan(ManagedDependencyType type, UUID bindingId,
      ProviderReference provider, int previewSchemaVersion, String providerFingerprint,
      String consumerDescriptorFingerprint, String snapshotFingerprint) {
  }

  public record PlanRequest(ConsumerPlan consumer, List<SelectionPlan> selections) {
    public PlanRequest {
      selections = List.copyOf(selections);
    }
  }

  /** A capability object that can only be created after coordinator authorization. */
  public static final class TrustedPlan {
    private final Object permit;
    private final Long consumerClusterId;
    private final String stackName;
    private final String stackVersion;
    private final Set<ManagedDependencyType> satisfiedTypes;

    private TrustedPlan(Object permit, Long consumerClusterId, String stackName,
        String stackVersion, Set<ManagedDependencyType> satisfiedTypes) {
      this.permit = permit;
      this.consumerClusterId = consumerClusterId;
      this.stackName = stackName;
      this.stackVersion = stackVersion;
      this.satisfiedTypes = Collections.unmodifiableSet(EnumSet.copyOf(satisfiedTypes));
    }

    public boolean appliesTo(Long clusterId, String requestStackName,
        String requestStackVersion, Collection<String> services) {
      return permit == TRUSTED_PERMIT
          && Objects.equals(consumerClusterId, clusterId)
          && stackName.equals(requestStackName)
          && stackVersion.equals(requestStackVersion)
          && services.contains("HBASE");
    }

    public Set<String> satisfiedComponents() {
      Set<String> components = new LinkedHashSet<>();
      if (satisfiedTypes.contains(ManagedDependencyType.HDFS)) {
        components.add("HDFS_CLIENT");
      }
      if (satisfiedTypes.contains(ManagedDependencyType.ZOOKEEPER)) {
        components.add("ZOOKEEPER_SERVER");
      }
      return Collections.unmodifiableSet(components);
    }
  }
}
