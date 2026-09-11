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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

public record ManagedDependencyIdentity(
    String effectiveShortUser,
    SortedSet<String> principalPatterns,
    boolean authToLocalVerified,
    String filesystemGroup,
    boolean distinctConsumerGroup,
    String directoryMode,
    boolean privateZooKeeperAcl) {

  public ManagedDependencyIdentity {
    effectiveShortUser = requireNonBlank(effectiveShortUser, "effectiveShortUser");
    principalPatterns = principalPatterns == null
        ? Collections.emptySortedSet()
        : Collections.unmodifiableSortedSet(new TreeSet<>(principalPatterns));
    filesystemGroup = filesystemGroup == null ? "" : filesystemGroup.trim();
    directoryMode = requireNonBlank(directoryMode, "directoryMode");
  }

  public record Plan(
      PlanKind kind,
      long ownerUserId,
      UUID creationDraftId,
      Long clusterId,
      String plannedShortUser,
      String planFingerprint) {
    public Plan {
      kind = Objects.requireNonNull(kind, "kind");
      String source;
      if (kind == PlanKind.CREATION_DRAFT) {
        if (ownerUserId <= 0 || creationDraftId == null || clusterId != null) {
          throw new IllegalArgumentException("draft identity plans require owner user ID and draft UUID");
        }
        source = ownerUserId + ":" + creationDraftId;
      } else {
        if (ownerUserId != 0 || creationDraftId != null || clusterId == null || clusterId <= 0) {
          throw new IllegalArgumentException("cluster identity plans require only a positive cluster ID");
        }
        source = Long.toString(clusterId);
      }
      String digest = sha256(kind.name() + ":" + source);
      String expectedUser = kind == PlanKind.CREATION_DRAFT
          ? "hbase_mc_" + digest.substring("sha256:".length(), "sha256:".length() + 20)
          : "hbase_mc_c" + Long.toUnsignedString(clusterId, 36);
      if (!expectedUser.equals(plannedShortUser) || !digest.equals(planFingerprint)) {
        throw new IllegalArgumentException("identity plan does not match its immutable source");
      }
    }

    public static Plan forCreationDraft(long ownerUserId, UUID creationDraftId) {
      String source = ownerUserId + ":" + Objects.requireNonNull(creationDraftId, "creationDraftId");
      String digest = sha256(PlanKind.CREATION_DRAFT.name() + ":" + source);
      String shortUser = "hbase_mc_"
          + digest.substring("sha256:".length(), "sha256:".length() + 20);
      return new Plan(PlanKind.CREATION_DRAFT, ownerUserId, creationDraftId,
          null, shortUser, digest);
    }

    public static Plan forExistingCluster(long clusterId) {
      String digest = sha256(PlanKind.EXISTING_CLUSTER.name() + ":" + clusterId);
      return new Plan(PlanKind.EXISTING_CLUSTER, 0, null, clusterId,
          "hbase_mc_c" + Long.toUnsignedString(clusterId, 36), digest);
    }
  }

  public record Allocation(String effectiveShortUser, String ownerPlanFingerprint) {
    public Allocation {
      effectiveShortUser = requireNonBlank(effectiveShortUser, "effectiveShortUser");
      ownerPlanFingerprint = ownerPlanFingerprint == null ? "" : ownerPlanFingerprint;
    }
  }

  public enum PlanKind {
    CREATION_DRAFT,
    EXISTING_CLUSTER
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return "sha256:" + HexFormat.of().formatHex(
          digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }

  public boolean hasPrivateFilesystemAuthority() {
    return "0700".equals(directoryMode)
        || ("0750".equals(directoryMode)
            && distinctConsumerGroup
            && !filesystemGroup.isBlank()
            && !"hadoop".equals(filesystemGroup));
  }

  private static String requireNonBlank(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
