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
package org.apache.ambari.server.controller.dependencies.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Pure proof of the supported ZooKeeper server SASL authorization-ID policy. */
public final class ManagedZooKeeperSaslPolicyVerifier {
  public static final int SCHEMA_VERSION = 1;
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  private static final Pattern REALM =
      Pattern.compile("[A-Z0-9](?:[A-Z0-9.-]{0,251}[A-Z0-9])?");
  private static final Pattern IDENTITY = Pattern.compile("[A-Za-z_][A-Za-z0-9._-]{0,127}");

  public Policy inspectPolicy(PolicySource source) {
    Objects.requireNonNull(source, "source");
    String realm = requireRealm(source.realm(), "providerRealm");
    String serviceName = requireIdentity(source.saslServiceName(), "saslServiceName");
    if (!source.saslEnabled() || !source.managedParentAclSupported()) {
      throw invalid("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
          "The ZooKeeper provider does not support the managed SASL parent policy.");
    }
    if (!source.removeHostFromPrincipal() || !source.removeRealmFromPrincipal()
        || !"DEFAULT".equals(source.authToLocalRules())) {
      throw invalid("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
          "The ZooKeeper provider SASL authorization-ID mapping is unsupported.");
    }
    String policyFingerprint = hash("zookeeper-sasl-policy",
        Integer.toString(SCHEMA_VERSION), realm, Boolean.toString(source.saslEnabled()),
        serviceName, Boolean.toString(source.managedParentAclSupported()),
        Boolean.toString(source.removeHostFromPrincipal()),
        Boolean.toString(source.removeRealmFromPrincipal()), "DEFAULT");
    return new Policy(SCHEMA_VERSION, realm, serviceName, policyFingerprint);
  }

  public PairProof provePair(PolicySource source, Policy expectedPolicy,
      ConsumerInput consumer) {
    Objects.requireNonNull(expectedPolicy, "expectedPolicy");
    Objects.requireNonNull(consumer, "consumer");
    Policy currentPolicy = inspectPolicy(source);
    if (!currentPolicy.equals(expectedPolicy)) {
      throw invalid("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_STALE",
          "The ZooKeeper provider SASL policy changed after it was inspected.");
    }
    String realm = requireRealm(consumer.realm(), "consumerRealm");
    if (!realm.equals(currentPolicy.realm())) {
      throw invalid("CROSS_REALM_NOT_SUPPORTED",
          "The consumer and ZooKeeper provider must use the same Kerberos realm.");
    }
    String user = requireIdentity(consumer.effectiveShortUser(), "effectiveShortUser");
    String expectedPattern = user + "/_HOST@" + realm;
    if (!expectedPattern.equals(consumer.principalPattern())) {
      throw invalid("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
          "The ZooKeeper consumer principal does not match its approved unique identity.");
    }
    if (!user.equals(consumer.expectedAuthorizationId())) {
      throw invalid("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
          "The ZooKeeper authorization ID does not match the approved unique identity.");
    }
    String principalFingerprint = fingerprintConsumer(consumer);
    String proofFingerprint = hash("zookeeper-sasl-pair-proof",
        Integer.toString(SCHEMA_VERSION), currentPolicy.policyFingerprint(),
        principalFingerprint, user);
    return new PairProof(SCHEMA_VERSION, currentPolicy.policyFingerprint(),
        principalFingerprint, user, proofFingerprint);
  }

  /** Returns the canonical hash for one exact ZooKeeper consumer authorization identity. */
  public String fingerprintConsumer(ConsumerInput consumer) {
    Objects.requireNonNull(consumer, "consumer");
    String realm = requireRealm(consumer.realm(), "consumerRealm");
    String user = requireIdentity(consumer.effectiveShortUser(), "effectiveShortUser");
    String expectedPattern = user + "/_HOST@" + realm;
    if (!expectedPattern.equals(consumer.principalPattern())
        || !user.equals(consumer.expectedAuthorizationId())) {
      throw invalid("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
          "The ZooKeeper consumer identity does not match its approved principal pattern.");
    }
    return hash("zookeeper-consumer-principal", Integer.toString(SCHEMA_VERSION),
        realm, user, expectedPattern);
  }

  private static String requireRealm(String value, String field) {
    if (value == null || !value.equals(value.trim())
        || !value.equals(value.toUpperCase(Locale.ENGLISH)) || !REALM.matcher(value).matches()) {
      throw invalid("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
          field + " is not a supported canonical Kerberos realm.");
    }
    return value;
  }

  private static String requireIdentity(String value, String field) {
    if (value == null || !IDENTITY.matcher(value).matches()) {
      throw invalid("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
          field + " is not a supported identity.");
    }
    return value;
  }

  private static String hash(String... values) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      StringBuilder canonical = new StringBuilder();
      append(canonical, "managed-zookeeper-sasl", Integer.toString(SCHEMA_VERSION));
      append(canonical, values);
      return "sha256:" + HexFormat.of().formatHex(
          digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }

  private static void append(StringBuilder target, String... values) {
    for (String value : values) {
      String actual = value == null ? "" : value;
      target.append(actual.length()).append(':').append(actual);
    }
  }

  private static ManagedDependencyIntegrationException invalid(String code, String message) {
    return new ManagedDependencyIntegrationException(422, code, message);
  }

  public record PolicySource(
      String realm,
      boolean saslEnabled,
      String saslServiceName,
      boolean managedParentAclSupported,
      boolean removeHostFromPrincipal,
      boolean removeRealmFromPrincipal,
      @JsonIgnore String authToLocalRules) {
    @Override
    public String toString() {
      return "PolicySource[authorizationPolicy=redacted, saslEnabled=" + saslEnabled
          + ", managedParentAclSupported=" + managedParentAclSupported + "]";
    }
  }

  public record Policy(
      int schemaVersion,
      String realm,
      String saslServiceName,
      String policyFingerprint) {
    public Policy {
      if (schemaVersion != SCHEMA_VERSION) {
        throw new IllegalArgumentException("unsupported ZooKeeper SASL policy schema");
      }
      realm = requireRecordRealm(realm);
      if (saslServiceName == null || !IDENTITY.matcher(saslServiceName).matches()) {
        throw new IllegalArgumentException("saslServiceName is invalid");
      }
      policyFingerprint = requireRecordHash(policyFingerprint, "policyFingerprint");
    }
  }

  public record ConsumerInput(
      String realm,
      String effectiveShortUser,
      String principalPattern,
      String expectedAuthorizationId) {
  }

  public record PairProof(
      int schemaVersion,
      String providerPolicyFingerprint,
      String principalPatternFingerprint,
      String expectedAuthorizationId,
      String proofFingerprint) {
    public PairProof {
      if (schemaVersion != SCHEMA_VERSION) {
        throw new IllegalArgumentException("unsupported ZooKeeper SASL pair proof schema");
      }
      providerPolicyFingerprint = requireRecordHash(providerPolicyFingerprint,
          "providerPolicyFingerprint");
      principalPatternFingerprint = requireRecordHash(principalPatternFingerprint,
          "principalPatternFingerprint");
      if (expectedAuthorizationId == null || !IDENTITY.matcher(expectedAuthorizationId).matches()) {
        throw new IllegalArgumentException("expectedAuthorizationId is invalid");
      }
      proofFingerprint = requireRecordHash(proofFingerprint, "proofFingerprint");
    }
  }

  private static String requireRecordRealm(String value) {
    if (value == null || !REALM.matcher(value).matches()) {
      throw new IllegalArgumentException("realm is invalid");
    }
    return value;
  }

  private static String requireRecordHash(String value, String field) {
    if (value == null || !HASH.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " must be a SHA-256 fingerprint");
    }
    return value;
  }
}
