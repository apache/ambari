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

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerPatternInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.ConsumerInput;

public record ManagedDependencySnapshot(
    int schemaVersion,
    UUID bindingId,
    long snapshotVersion,
    ManagedDependencyType type,
    ManagedDependencyServiceKey providerService,
    ManagedDependencyNamespace namespace,
    ManagedDependencyVersion.Compatibility providerVersion,
    ManagedDependencySecurityMode securityMode,
    ManagedDependencyIdentity consumerIdentity,
    SortedMap<String, String> coreSite,
    SortedMap<String, String> hdfsSite,
    SortedMap<String, String> zooKeeperClient,
    String consumerFingerprint,
    String providerFingerprint,
    String snapshotFingerprint,
    ManagedHBaseConsumerLocalMapping consumerLocalMapping,
    ManagedDependencyProviderSecurityProof providerSecurity,
    ManagedDependencyPairSecurityProof pairSecurity) {

  public static final int LEGACY_INSECURE_SCHEMA_VERSION = 1;
  public static final int CURRENT_SCHEMA_VERSION = 2;
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public ManagedDependencySnapshot {
    if (schemaVersion != LEGACY_INSECURE_SCHEMA_VERSION
        && schemaVersion != CURRENT_SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported snapshot schema version");
    }
    bindingId = Objects.requireNonNull(bindingId, "bindingId");
    if (snapshotVersion <= 0) {
      throw new IllegalArgumentException("snapshotVersion must be positive");
    }
    type = Objects.requireNonNull(type, "type");
    providerService = Objects.requireNonNull(providerService, "providerService");
    if (!type.getProviderServiceName().equals(providerService.serviceName())) {
      throw new IllegalArgumentException("providerService does not match dependency type");
    }
    namespace = Objects.requireNonNull(namespace, "namespace");
    if (type == ManagedDependencyType.HDFS && namespace.rootUri().isEmpty()
        || type == ManagedDependencyType.ZOOKEEPER && namespace.znode().isEmpty()) {
      throw new IllegalArgumentException("namespace does not match dependency type");
    }
    providerVersion = Objects.requireNonNull(providerVersion, "providerVersion");
    securityMode = Objects.requireNonNull(securityMode, "securityMode");
    consumerIdentity = Objects.requireNonNull(consumerIdentity, "consumerIdentity");
    coreSite = immutable(coreSite);
    hdfsSite = immutable(hdfsSite);
    zooKeeperClient = immutable(zooKeeperClient);
    consumerFingerprint = requireHash(consumerFingerprint, "consumerFingerprint");
    providerFingerprint = requireHash(providerFingerprint, "providerFingerprint");
    snapshotFingerprint = requireHash(snapshotFingerprint, "snapshotFingerprint");
    requireSupportedSecurityEvidence(schemaVersion, securityMode);

    if (schemaVersion == LEGACY_INSECURE_SCHEMA_VERSION) {
      if (consumerLocalMapping != null || providerSecurity != null || pairSecurity != null) {
        throw new IllegalArgumentException(
            "schema 1 is accepted only for insecure snapshots without security proofs");
      }
    } else if (securityMode == ManagedDependencySecurityMode.INSECURE) {
      if (consumerLocalMapping != null || providerSecurity != null || pairSecurity != null) {
        throw new IllegalArgumentException("insecure snapshots must not contain Kerberos proofs");
      }
    } else {
      validateKerberosProofs(type, consumerIdentity, consumerLocalMapping,
          providerSecurity, pairSecurity);
    }
  }

  /** Source-compatible constructor for new insecure snapshots. */
  public ManagedDependencySnapshot(int schemaVersion, UUID bindingId, long snapshotVersion,
      ManagedDependencyType type, ManagedDependencyServiceKey providerService,
      ManagedDependencyNamespace namespace, ManagedDependencyVersion.Compatibility providerVersion,
      ManagedDependencySecurityMode securityMode, ManagedDependencyIdentity consumerIdentity,
      SortedMap<String, String> coreSite, SortedMap<String, String> hdfsSite,
      SortedMap<String, String> zooKeeperClient, String consumerFingerprint,
      String providerFingerprint, String snapshotFingerprint) {
    this(schemaVersion, bindingId, snapshotVersion, type, providerService, namespace,
        providerVersion, securityMode, consumerIdentity, coreSite, hdfsSite, zooKeeperClient,
        consumerFingerprint, providerFingerprint, snapshotFingerprint, null, null, null);
  }

  public boolean isLegacyInsecure() {
    return schemaVersion == LEGACY_INSECURE_SCHEMA_VERSION;
  }

  /** Stable guard for persistence adapters before binding legacy snapshot JSON. */
  public static void requireSupportedSecurityEvidence(int schemaVersion,
      ManagedDependencySecurityMode securityMode) {
    if (schemaVersion == LEGACY_INSECURE_SCHEMA_VERSION
        && securityMode == ManagedDependencySecurityMode.KERBEROS) {
      throw new ManagedDependencyIntegrationException(409,
          ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED.name(),
          "A legacy secure snapshot has no typed authorization proof; review and approve an update.");
    }
  }

  private static void validateKerberosProofs(ManagedDependencyType type,
      ManagedDependencyIdentity identity, ManagedHBaseConsumerLocalMapping mapping,
      ManagedDependencyProviderSecurityProof provider,
      ManagedDependencyPairSecurityProof pair) {
    Objects.requireNonNull(mapping, "consumerLocalMapping");
    Objects.requireNonNull(provider, "providerSecurity");
    Objects.requireNonNull(pair, "pairSecurity");
    if (provider.type() != type || pair.type() != type || provider.kind() != pair.kind()
        || provider.kind().dependencyType() != type) {
      throw new IllegalArgumentException("security proof type does not match the dependency");
    }
    if (!mapping.realm().equals(provider.realm())
        || !provider.policyFingerprint().equals(pair.providerPolicyFingerprint())) {
      throw new IllegalArgumentException("security proof realm or provider policy does not match");
    }
    String user = identity.effectiveShortUser();
    String principalPattern = mapping.rolePrincipalPattern();
    String expectedPrincipalFingerprint = type == ManagedDependencyType.HDFS
        ? new ManagedHdfsAuthToLocalVerifier().fingerprintConsumerPattern(
            new ConsumerPatternInput(mapping.realm(), user, principalPattern))
        : new ManagedZooKeeperSaslPolicyVerifier().fingerprintConsumer(
            new ConsumerInput(mapping.realm(), user, principalPattern, user));
    if (!user.equals(mapping.proof().effectiveShortUser())
        || !user.equals(pair.expectedAuthorizationId())
        || !Set.of(principalPattern).equals(identity.principalPatterns())
        || !expectedPrincipalFingerprint.equals(pair.consumerPrincipalFingerprint())) {
      throw new IllegalArgumentException("security proof does not match the consumer identity");
    }
  }

  private static SortedMap<String, String> immutable(Map<String, String> values) {
    return ManagedDependencyVersion.immutableSortedMap(values);
  }

  private static String requireHash(String value, String field) {
    Objects.requireNonNull(value, field);
    if (!HASH.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " must use sha256:<64 lowercase hex> format");
    }
    return value;
  }
}
