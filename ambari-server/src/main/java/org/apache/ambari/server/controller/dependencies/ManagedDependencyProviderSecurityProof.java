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

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.Policy;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier;

/** Raw-free identity of one provider's effective authorization policy. */
public record ManagedDependencyProviderSecurityProof(
    int schemaVersion,
    ManagedDependencyType type,
    ManagedDependencySecurityPolicyKind kind,
    String realm,
    String policyFingerprint) {
  public static final int SCHEMA_VERSION = 1;
  private static final Pattern REALM =
      Pattern.compile("[A-Z0-9](?:[A-Z0-9.-]{0,251}[A-Z0-9])?");
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public ManagedDependencyProviderSecurityProof {
    if (schemaVersion != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported provider security proof schema");
    }
    type = Objects.requireNonNull(type, "type");
    kind = Objects.requireNonNull(kind, "kind");
    if (kind.dependencyType() != type) {
      throw new IllegalArgumentException("provider security proof kind does not match dependency type");
    }
    realm = requireRealm(realm);
    policyFingerprint = requireHash(policyFingerprint, "policyFingerprint");
  }

  public static ManagedDependencyProviderSecurityProof forHdfs(Policy policy) {
    Objects.requireNonNull(policy, "policy");
    return new ManagedDependencyProviderSecurityProof(SCHEMA_VERSION,
        ManagedDependencyType.HDFS, ManagedDependencySecurityPolicyKind.HDFS_AUTH_TO_LOCAL,
        policy.effectiveDefaultRealm(), policy.policyFingerprint());
  }

  public static ManagedDependencyProviderSecurityProof forZooKeeper(
      ManagedZooKeeperSaslPolicyVerifier.Policy policy) {
    Objects.requireNonNull(policy, "policy");
    return new ManagedDependencyProviderSecurityProof(SCHEMA_VERSION,
        ManagedDependencyType.ZOOKEEPER, ManagedDependencySecurityPolicyKind.ZOOKEEPER_SASL_ID,
        policy.realm(), policy.policyFingerprint());
  }

  private static String requireRealm(String value) {
    Objects.requireNonNull(value, "realm");
    if (!value.equals(value.trim()) || !value.equals(value.toUpperCase(Locale.ENGLISH))
        || !REALM.matcher(value).matches()) {
      throw new IllegalArgumentException("realm must be a canonical Kerberos realm");
    }
    return value;
  }

  private static String requireHash(String value, String field) {
    if (value == null || !HASH.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " must be a SHA-256 fingerprint");
    }
    return value;
  }
}
