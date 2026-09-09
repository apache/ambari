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

import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerPatternProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier;

/** Raw-free proof that one provider policy authorizes one consumer identity. */
public record ManagedDependencyPairSecurityProof(
    int schemaVersion,
    ManagedDependencyType type,
    ManagedDependencySecurityPolicyKind kind,
    String providerPolicyFingerprint,
    String consumerPrincipalFingerprint,
    String expectedAuthorizationId,
    String proofFingerprint) {
  public static final int SCHEMA_VERSION = 1;
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  private static final Pattern IDENTITY = Pattern.compile("[A-Za-z_][A-Za-z0-9._-]{0,127}");

  public ManagedDependencyPairSecurityProof {
    if (schemaVersion != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported pair security proof schema");
    }
    type = Objects.requireNonNull(type, "type");
    kind = Objects.requireNonNull(kind, "kind");
    if (kind.dependencyType() != type) {
      throw new IllegalArgumentException("pair security proof kind does not match dependency type");
    }
    providerPolicyFingerprint = requireHash(providerPolicyFingerprint,
        "providerPolicyFingerprint");
    consumerPrincipalFingerprint = requireHash(consumerPrincipalFingerprint,
        "consumerPrincipalFingerprint");
    if (expectedAuthorizationId == null || !IDENTITY.matcher(expectedAuthorizationId).matches()) {
      throw new IllegalArgumentException("expectedAuthorizationId is invalid");
    }
    proofFingerprint = requireHash(proofFingerprint, "proofFingerprint");
  }

  public static ManagedDependencyPairSecurityProof forHdfs(ConsumerPatternProof proof) {
    Objects.requireNonNull(proof, "proof");
    return new ManagedDependencyPairSecurityProof(SCHEMA_VERSION, ManagedDependencyType.HDFS,
        ManagedDependencySecurityPolicyKind.HDFS_AUTH_TO_LOCAL,
        proof.providerPolicyFingerprint(), proof.principalPatternFingerprint(),
        proof.mappedShortUser(), proof.proofFingerprint());
  }

  public static ManagedDependencyPairSecurityProof forZooKeeper(
      ManagedZooKeeperSaslPolicyVerifier.PairProof proof) {
    Objects.requireNonNull(proof, "proof");
    return new ManagedDependencyPairSecurityProof(SCHEMA_VERSION, ManagedDependencyType.ZOOKEEPER,
        ManagedDependencySecurityPolicyKind.ZOOKEEPER_SASL_ID,
        proof.providerPolicyFingerprint(), proof.principalPatternFingerprint(),
        proof.expectedAuthorizationId(), proof.proofFingerprint());
  }

  private static String requireHash(String value, String field) {
    if (value == null || !HASH.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " must be a SHA-256 fingerprint");
    }
    return value;
  }
}
