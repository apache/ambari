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
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosDescriptorOverlay.SealedConfigurations;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingProof;

/** Immutable consumer-owned auth-to-local rules and their semantic proof. */
public record ManagedHBaseConsumerLocalMapping(
    int schemaVersion,
    String realm,
    String rolePrincipalPattern,
    String headlessPrincipal,
    String smokePrincipal,
    String smokeShortUser,
    String identityPlanFingerprint,
    String canonicalRules,
    ConsumerLocalMappingProof proof,
    String profileFingerprint) {
  public static final int SCHEMA_VERSION = 1;
  public static final int MAX_RULE_BYTES = 24 * 1024;
  public static final int MAX_RULE_COUNT = 256;
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public ManagedHBaseConsumerLocalMapping {
    if (schemaVersion != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported consumer mapping schema");
    }
    Objects.requireNonNull(realm, "realm");
    Objects.requireNonNull(rolePrincipalPattern, "rolePrincipalPattern");
    Objects.requireNonNull(headlessPrincipal, "headlessPrincipal");
    Objects.requireNonNull(smokePrincipal, "smokePrincipal");
    Objects.requireNonNull(smokeShortUser, "smokeShortUser");
    identityPlanFingerprint = requireHash(identityPlanFingerprint, "identityPlanFingerprint");
    canonicalRules = requireCanonicalRules(canonicalRules);
    proof = Objects.requireNonNull(proof, "proof");

    ConsumerLocalMappingProof actualProof = new ManagedHdfsAuthToLocalVerifier()
        .proveConsumerLocalMappings(canonicalRules,
            new ConsumerLocalMappingInput(realm, proof.effectiveShortUser(),
                rolePrincipalPattern, headlessPrincipal, smokePrincipal, smokeShortUser));
    if (!actualProof.equals(proof)) {
      throw new IllegalArgumentException("consumer mapping proof does not match its rules and identities");
    }
    String expectedFingerprint = fingerprint(schemaVersion, realm, rolePrincipalPattern,
        headlessPrincipal, smokePrincipal, smokeShortUser, identityPlanFingerprint, proof);
    profileFingerprint = requireHash(profileFingerprint, "profileFingerprint");
    if (!expectedFingerprint.equals(profileFingerprint)) {
      throw new IllegalArgumentException("profileFingerprint does not match the consumer mapping");
    }
  }

  public static ManagedHBaseConsumerLocalMapping create(
      ManagedHBaseKerberosOverlaySpec spec, SealedConfigurations sealed) {
    Objects.requireNonNull(spec, "spec");
    Objects.requireNonNull(sealed, "sealed");
    Map<String, String> coreSite = sealed.configurations().get("core-site");
    String rules = coreSite == null ? null : coreSite.get("hadoop.security.auth_to_local");
    ConsumerLocalMappingProof proof = sealed.mappingProof();
    String fingerprint = fingerprint(SCHEMA_VERSION, spec.realm(), spec.rolePrincipalPattern(),
        spec.headlessPrincipal(), spec.smokePrincipal(), spec.smokeShortUser(),
        spec.identityPlanFingerprint(), proof);
    return new ManagedHBaseConsumerLocalMapping(SCHEMA_VERSION, spec.realm(),
        spec.rolePrincipalPattern(), spec.headlessPrincipal(), spec.smokePrincipal(),
        spec.smokeShortUser(), spec.identityPlanFingerprint(), rules, proof, fingerprint);
  }

  public static String fingerprintRules(String canonicalRules) {
    return new ManagedHdfsAuthToLocalVerifier()
        .fingerprintConsumerLocalRules(requireCanonicalRules(canonicalRules));
  }

  private static String requireCanonicalRules(String value) {
    Objects.requireNonNull(value, "canonicalRules");
    int bytes = value.getBytes(StandardCharsets.UTF_8).length;
    if (bytes == 0 || bytes > MAX_RULE_BYTES || value.indexOf('\r') >= 0
        || value.startsWith("\n") || value.endsWith("\n")) {
      throw new IllegalArgumentException("consumer rules are empty, noncanonical, or exceed the size limit");
    }
    String[] rules = value.split("\n", -1);
    if (rules.length == 0 || rules.length > MAX_RULE_COUNT) {
      throw new IllegalArgumentException("consumer rules exceed the supported rule count");
    }
    for (String rule : rules) {
      if (rule.isEmpty()) {
        throw new IllegalArgumentException("consumer rules must not contain blank entries");
      }
    }
    return value;
  }

  private static String fingerprint(int schemaVersion, String realm, String rolePrincipalPattern,
      String headlessPrincipal, String smokePrincipal, String smokeShortUser,
      String identityPlanFingerprint, ConsumerLocalMappingProof proof) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      StringBuilder canonical = new StringBuilder();
      append(canonical, "managed-hbase-consumer-local-profile", Integer.toString(schemaVersion),
          realm, rolePrincipalPattern, headlessPrincipal, smokePrincipal, smokeShortUser,
          identityPlanFingerprint, proof.rulesFingerprint(), proof.identityFingerprint(),
          proof.proofFingerprint());
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

  private static String requireHash(String value, String field) {
    if (value == null || !HASH.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " must be a SHA-256 fingerprint");
    }
    return value;
  }

  @Override
  public String toString() {
    return "ManagedHBaseConsumerLocalMapping[schemaVersion=" + schemaVersion
        + ", realm=" + realm + ", rolePrincipalPattern=" + rolePrincipalPattern
        + ", headlessPrincipal=" + headlessPrincipal + ", smokePrincipal=" + smokePrincipal
        + ", smokeShortUser=" + smokeShortUser + ", identityPlanFingerprint="
        + identityPlanFingerprint + ", canonicalRules=redacted, proof=" + proof
        + ", profileFingerprint=" + profileFingerprint + "]";
  }
}
