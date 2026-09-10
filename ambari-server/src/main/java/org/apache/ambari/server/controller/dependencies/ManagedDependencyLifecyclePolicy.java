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

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.state.Cluster;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Enforces dependency ownership at the lower service lifecycle boundary. */
@Singleton
public class ManagedDependencyLifecyclePolicy {
  public static final String CONSUMER_SERVICE = "HBASE";
  public static final String IMPACT_ACTION_PARAMETER = "managed_dependency_impact_action";
  public static final String IMPACT_REVISION_PARAMETER = "managed_dependency_impact_revision";
  public static final String IMPACT_CONFIRMATIONS_PARAMETER =
      "managed_dependency_impact_confirmations";
  public static final String IMPACT_CONFIRMATION_MODE_PARAMETER =
      "managed_dependency_impact_confirmation_mode";
  public static final Set<String> IMPACT_PARAMETERS = Set.of(
      IMPACT_ACTION_PARAMETER, IMPACT_REVISION_PARAMETER, IMPACT_CONFIRMATIONS_PARAMETER);
  private static final Set<String> PROVIDER_SERVICES = Set.of("HDFS", "ZOOKEEPER");
  private static final Set<String> IMPACT_ACTIONS = Set.of("STOP", "RESTART");
  private static final Set<String> IMPACT_CONFIRMATION_FIELDS = Set.of(
      "provider_cluster_id", "service_name", "action", "revision");
  private static final int MAX_IMPACT_CONFIRMATION_BYTES = 16 * 1024;
  private static final int MAX_IMPACT_CONFIRMATIONS = 32;

  private final ServiceDependencyDAO dependencyDAO;

  @Inject
  public ManagedDependencyLifecyclePolicy(ServiceDependencyDAO dependencyDAO) {
    this.dependencyDAO = dependencyDAO;
  }

  /**
   * Returns the opaque revision used by the existing provider impact endpoint.
   * Only binding identity, row version, and lifecycle state contribute to the
   * value; dependent names never become part of the confirmation contract.
   */
  public String impactRevision(long providerClusterId, String serviceName) {
    return impactRevision(providerClusterId, serviceName, dependencyDAO.findByProvider(providerClusterId, serviceName));
  }

  public String impactRevision(long providerClusterId, String serviceName,
      List<ServiceDependencyBindingEntity> bindings) {
    String canonical = bindings.stream()
        .map(binding -> binding.getBindingId() + ":" + binding.getRowVersion() + ":"
            + binding.getState())
        .sorted()
        .reduce("", (left, right) -> left + "|" + right);
    return hash(Long.toString(providerClusterId), serviceName, canonical);
  }

  /**
   * Validates a provider STOP/RESTART confirmation. Callers must hold the
   * provider's canonical write lock when this method protects an action
   * publication boundary, so a newly-created binding cannot pass after the
   * revision has been checked.
   */
  public boolean validateProviderAction(Cluster provider, String serviceName,
      String action, Map<String, String> commandParameters) {
    String effectiveService = serviceName == null
        ? null : serviceName.trim().toUpperCase(Locale.ROOT);
    String effectiveAction = action == null ? null : action.trim().toUpperCase(Locale.ROOT);
    if (!PROVIDER_SERVICES.contains(effectiveService) || effectiveAction == null
        || !IMPACT_ACTIONS.contains(effectiveAction)) {
      throw lifecycleConflict(400, "INVALID_MANAGED_DEPENDENCY_LIFECYCLE",
          "The managed dependency lifecycle action is not recognized.");
    }

    String rawConfirmations = parameter(commandParameters, IMPACT_CONFIRMATIONS_PARAMETER);
    List<ImpactConfirmation> confirmations = rawConfirmations == null
        ? List.of() : parseConfirmations(rawConfirmations);

    List<ServiceDependencyBindingEntity> bindings = dependencyDAO.findByProvider(
        provider.getClusterId(), effectiveService);
    if (bindings.isEmpty()) {
      return false;
    }

    if (rawConfirmations != null) {
      ImpactConfirmation confirmation = confirmations.stream()
          .filter(candidate -> candidate.providerClusterId() == provider.getClusterId()
              && effectiveService.equals(candidate.serviceName())
              && effectiveAction.equals(candidate.action()))
          .findFirst().orElse(null);
      if (confirmation == null) {
        boolean actionMismatch = confirmations.stream()
                .anyMatch(candidate -> candidate.providerClusterId() == provider.getClusterId()
                && effectiveService.equals(candidate.serviceName()));
        throw lifecycleConflict(409, actionMismatch
                ? "DEPENDENCY_IMPACT_ACTION_MISMATCH"
                : "DEPENDENCY_IMPACT_CONFIRMATION_REQUIRED",
            actionMismatch
                ? "The managed dependency impact confirmation does not match the requested action."
                : "The current managed dependency impact must be confirmed before this action.");
      }
      if (!Objects.equals(impactRevision(provider.getClusterId(), effectiveService),
          confirmation.revision())) {
        throw lifecycleConflict(409, "DEPENDENCY_IMPACT_PREVIEW_STALE",
            "The managed dependency impact changed; review the current impact before retrying.");
      }
      return true;
    }

    String confirmedAction = parameter(commandParameters, IMPACT_ACTION_PARAMETER);
    String confirmedRevision = parameter(commandParameters, IMPACT_REVISION_PARAMETER);
    if (confirmedAction == null || confirmedAction.isBlank()
        || confirmedRevision == null || confirmedRevision.isBlank()) {
      throw lifecycleConflict(409, "DEPENDENCY_IMPACT_CONFIRMATION_REQUIRED",
          "The current managed dependency impact must be confirmed before this action.");
    }
    if (!effectiveAction.equals(confirmedAction.trim().toUpperCase(Locale.ROOT))) {
      throw lifecycleConflict(409, "DEPENDENCY_IMPACT_ACTION_MISMATCH",
          "The managed dependency impact confirmation does not match the requested action.");
    }
    if (!Objects.equals(impactRevision(provider.getClusterId(), effectiveService),
        confirmedRevision.trim())) {
      throw lifecycleConflict(409, "DEPENDENCY_IMPACT_PREVIEW_STALE",
          "The managed dependency impact changed; review the current impact before retrying.");
    }
    return true;
  }

  /** Identifies one provider lifecycle target in a complete request. */
  public record ImpactTarget(long providerClusterId, String serviceName, String action) {
  }

  /**
   * Ensures a multi-provider request used a complete confirmation set. Each
   * generated task carries its selected record as immutable action/revision
   * fields, so this check is performed from task metadata at both boundaries.
   */
  public void validateProviderTargetSet(
      Map<ImpactTarget, Map<String, String>> targetConfirmations) {
    if (targetConfirmations == null || targetConfirmations.size() <= 1) {
      return;
    }
    for (Map.Entry<ImpactTarget, Map<String, String>> entry : targetConfirmations.entrySet()) {
      Map<String, String> parameters = entry.getValue();
      if (!"set".equals(parameter(parameters, IMPACT_CONFIRMATION_MODE_PARAMETER))) {
        throw lifecycleConflict(409, "DEPENDENCY_IMPACT_CONFIRMATION_SET_REQUIRED",
            "Every provider impact must be confirmed together for this request.");
      }
      String confirmedAction = parameter(parameters, IMPACT_ACTION_PARAMETER);
      String confirmedRevision = parameter(parameters, IMPACT_REVISION_PARAMETER);
      if (confirmedAction == null || confirmedAction.isBlank()
          || confirmedRevision == null || confirmedRevision.isBlank()) {
        throw lifecycleConflict(409, "DEPENDENCY_IMPACT_CONFIRMATION_SET_REQUIRED",
            "Every provider impact must be confirmed together for this request.");
      }
      if (!entry.getKey().action().equals(confirmedAction.trim().toUpperCase(Locale.ROOT))) {
        throw lifecycleConflict(409, "DEPENDENCY_IMPACT_ACTION_MISMATCH",
            "The managed dependency impact confirmation does not match the requested action.");
      }
    }
  }

  /** Accepts the wire form used by request-info and the immutable command form. */
  public static String parameter(Map<String, String> parameters, String key) {
    if (parameters == null) {
      return null;
    }
    String value = parameters.get(key);
    if (value != null) {
      return value;
    }
    value = parameters.get("parameters/" + key);
    if (value != null) {
      return value;
    }
    value = parameters.get("inputs/" + key);
    if (value != null) {
      return value;
    }
    return parameters.get("params/" + key);
  }

  /** Copies only lifecycle confirmation fields into immutable command parameters. */
  public static void copyParameters(Map<String, String> source,
      Map<String, String> target) {
    if (target == null) {
      return;
    }
    for (String key : IMPACT_PARAMETERS) {
      String value = parameter(source, key);
      if (value != null) {
        target.put(key, value);
      }
    }
  }

  /**
   * Selects exactly one confirmation record for a generated provider task.
   * Invalid input is retained for the later policy parser to report as 400.
   */
  public static void copyParametersForTarget(Map<String, String> source,
      Map<String, String> target, long providerClusterId, String serviceName, String action) {
    if (target == null) {
      return;
    }
    for (String key : IMPACT_PARAMETERS) {
      target.remove(key);
      target.remove("parameters/" + key);
      target.remove("inputs/" + key);
      target.remove("params/" + key);
    }
    target.remove(IMPACT_CONFIRMATION_MODE_PARAMETER);

    String effectiveService = serviceName == null
        ? null : serviceName.trim().toUpperCase(Locale.ROOT);
    String effectiveAction = action == null ? null : action.trim().toUpperCase(Locale.ROOT);
    if (effectiveService == null || effectiveAction == null
        || !PROVIDER_SERVICES.contains(effectiveService)
        || !IMPACT_ACTIONS.contains(effectiveAction)) {
      return;
    }
    String rawConfirmations = parameter(source, IMPACT_CONFIRMATIONS_PARAMETER);
    if (rawConfirmations != null) {
      try {
        ImpactConfirmation selected = parseConfirmations(rawConfirmations).stream()
            .filter(candidate -> candidate.providerClusterId() == providerClusterId
                && effectiveService.equals(candidate.serviceName())
                && effectiveAction.equals(candidate.action()))
            .findFirst().orElse(null);
        target.put(IMPACT_CONFIRMATION_MODE_PARAMETER, "set");
        if (selected != null) {
          target.put(IMPACT_ACTION_PARAMETER, selected.action());
          target.put(IMPACT_REVISION_PARAMETER, selected.revision());
        } else {
          target.put(IMPACT_CONFIRMATIONS_PARAMETER, rawConfirmations);
        }
      } catch (RuntimeException e) {
        target.put(IMPACT_CONFIRMATION_MODE_PARAMETER, "set");
        target.put(IMPACT_CONFIRMATIONS_PARAMETER, rawConfirmations);
      }
      return;
    }

    target.put(IMPACT_CONFIRMATION_MODE_PARAMETER, "single");
    String confirmedAction = parameter(source, IMPACT_ACTION_PARAMETER);
    String confirmedRevision = parameter(source, IMPACT_REVISION_PARAMETER);
    if (confirmedAction != null) {
      target.put(IMPACT_ACTION_PARAMETER, confirmedAction);
    }
    if (confirmedRevision != null) {
      target.put(IMPACT_REVISION_PARAMETER, confirmedRevision);
    }
  }

  private static List<ImpactConfirmation> parseConfirmations(String raw) {
    try {
      if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length
          > MAX_IMPACT_CONFIRMATION_BYTES) {
        throw new IllegalArgumentException("confirmation set exceeds its size limit");
      }
      JsonReader reader = new JsonReader(new StringReader(raw));
      reader.setLenient(false);
      List<ImpactConfirmation> result = new ArrayList<>();
      Set<String> keys = new HashSet<>();
      reader.beginArray();
      while (reader.hasNext()) {
        if (result.size() >= MAX_IMPACT_CONFIRMATIONS) {
          throw new IllegalArgumentException("confirmation set contains too many records");
        }
        reader.beginObject();
        Set<String> fields = new HashSet<>();
        Long providerClusterId = null;
        String serviceName = null;
        String action = null;
        String revision = null;
        while (reader.hasNext()) {
          String field = reader.nextName();
          if (!IMPACT_CONFIRMATION_FIELDS.contains(field) || !fields.add(field)) {
            throw new IllegalArgumentException("confirmation record has unknown or duplicate fields");
          }
          switch (field) {
            case "provider_cluster_id" -> {
              if (reader.peek() != JsonToken.NUMBER) {
                throw new IllegalArgumentException("provider cluster id must be a number");
              }
              providerClusterId = new BigDecimal(reader.nextString()).longValueExact();
            }
            case "service_name" -> {
              if (reader.peek() != JsonToken.STRING) {
                throw new IllegalArgumentException("service name must be a string");
              }
              serviceName = reader.nextString().trim().toUpperCase(Locale.ROOT);
            }
            case "action" -> {
              if (reader.peek() != JsonToken.STRING) {
                throw new IllegalArgumentException("action must be a string");
              }
              action = reader.nextString().trim().toUpperCase(Locale.ROOT);
            }
            case "revision" -> {
              if (reader.peek() != JsonToken.STRING) {
                throw new IllegalArgumentException("revision must be a string");
              }
              revision = reader.nextString().trim();
            }
            default -> throw new IllegalArgumentException("confirmation record is invalid");
          }
        }
        reader.endObject();
        if (!fields.equals(IMPACT_CONFIRMATION_FIELDS) || providerClusterId == null
            || providerClusterId <= 0 || serviceName == null
            || !PROVIDER_SERVICES.contains(serviceName) || action == null
            || !IMPACT_ACTIONS.contains(action) || !isCanonicalRevision(revision)) {
          throw new IllegalArgumentException("confirmation record is invalid");
        }
        ImpactConfirmation confirmation = new ImpactConfirmation(providerClusterId, serviceName,
            action, revision);
        if (!keys.add(confirmation.key())) {
          throw new IllegalArgumentException("confirmation set contains duplicate records");
        }
        result.add(confirmation);
      }
      reader.endArray();
      if (result.isEmpty() || reader.peek() != JsonToken.END_DOCUMENT) {
        throw new IllegalArgumentException("confirmation set must be a single nonempty array");
      }
      return result;
    } catch (IOException | RuntimeException e) {
      throw new ManagedDependencyIntegrationException(400,
          "INVALID_DEPENDENCY_IMPACT_CONFIRMATIONS",
          "The managed dependency impact confirmation set is invalid.");
    }
  }

  private static boolean isCanonicalRevision(String revision) {
    if (revision == null || revision.length() != "sha256:".length() + 64
        || !revision.startsWith("sha256:")) {
      return false;
    }
    for (int index = "sha256:".length(); index < revision.length(); index++) {
      char value = revision.charAt(index);
      if ((value < '0' || value > '9') && (value < 'a' || value > 'f')) {
        return false;
      }
    }
    return true;
  }

  private record ImpactConfirmation(long providerClusterId, String serviceName, String action,
      String revision) {
    private String key() {
      return providerClusterId + "|" + serviceName + "|" + action;
    }
  }

  /**
   * Must run while the cluster write lock is held. Binding publication holds
   * both parent cluster read locks through commit, making this check and the
   * subsequent service removal one lifecycle boundary.
   */
  public void validateServiceDeletion(Cluster cluster, Collection<String> serviceNames) {
    long clusterId = cluster.getClusterId();
    for (String serviceName : serviceNames) {
      if (!dependencyDAO.findByProvider(clusterId, serviceName).isEmpty()) {
        throw conflict("DEPENDENCY_PROVIDER_DELETE_BLOCKED",
            "The service provides an active managed dependency; detach every dependent first.");
      }
      if (CONSUMER_SERVICE.equals(serviceName)
          && !dependencyDAO.findByConsumer(clusterId, serviceName).isEmpty()) {
        throw conflict("DEPENDENCY_CONSUMER_DELETE_REQUIRES_DETACH",
            "The HBASE service has an active managed dependency; detach it before deletion.");
      }
    }
  }

  private ManagedDependencyIntegrationException conflict(String code, String message) {
    return new ManagedDependencyIntegrationException(409, code, message);
  }

  private ManagedDependencyIntegrationException lifecycleConflict(int status, String code,
      String message) {
    return new ManagedDependencyIntegrationException(status, code, message);
  }

  private static String hash(String... values) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (String value : values) {
        byte[] bytes = Objects.requireNonNullElse(value, "")
            .getBytes(StandardCharsets.UTF_8);
        digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) ':');
        digest.update(bytes);
      }
      return "sha256:" + HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }
}
