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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.ambari.server.controller.AuthToLocalBuilder;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerPatternInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerPatternProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.Policy;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.PolicySource;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ManagedHdfsAuthToLocalVerifierTest {
  private static final String REALM = "EXAMPLE.COM";
  private static final String USER_A = "hbase_mc_0123456789abcdefabcd";
  private static final String USER_B = "hbase_mc_fedcba9876543210abcd";
  private static final String STOCK_TEMPLATE = """
      [libdefaults]
        forwardable = true
        default_realm = {{realm}}
      """;
  private static final String ORDINARY_RULES = String.join("\n",
      "RULE:[1:$1@$0](smoke-a@EXAMPLE.COM)s/.*/ambari-qa/",
      "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*///L",
      "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/",
      "RULE:[2:$1@$0](hbase@EXAMPLE.COM)s/.*/hbase/",
      "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/",
      "DEFAULT");

  private final ManagedHdfsAuthToLocalVerifier verifier =
      new ManagedHdfsAuthToLocalVerifier();

  @Test
  void provesHostIndependentUniquePatternsWithoutRolePlacements() {
    PolicySource source = source(ORDINARY_RULES, REALM);
    Policy policy = verifier.inspectPolicy(source);

    ConsumerPatternProof proofA = verifier.provePattern(source, policy,
        pattern(USER_A, REALM));
    ConsumerPatternProof repeatedPlanOrLiveProof = verifier.provePattern(source, policy,
        pattern(USER_A, REALM));
    ConsumerPatternProof proofB = verifier.provePattern(source, policy,
        pattern(USER_B, REALM));

    assertEquals(USER_A, proofA.mappedShortUser());
    assertEquals(repeatedPlanOrLiveProof, proofA);
    assertEquals(policy.policyFingerprint(), proofA.providerPolicyFingerprint());
    assertNotEquals(proofA.principalPatternFingerprint(), proofB.principalPatternFingerprint());
    assertNotEquals(proofA.proofFingerprint(), proofB.proofFingerprint());
  }

  @Test
  void acceptsGeneratedOneAndTwoComponentRulesAndLowercaseModifier() {
    Policy policy = verifier.inspectPolicy(source(ORDINARY_RULES, REALM));

    assertEquals("hadoop", policy.mechanism());
    assertEquals(REALM, policy.effectiveDefaultRealm());
    assertTrue(policy.ambariManagesRules());
    assertTrue(policy.ambariManagesKrb5Conf());
  }

  @Test
  void acceptsRulesGeneratedByAuthToLocalBuilderAndTheActiveStockTemplate()
      throws IOException {
    AuthToLocalBuilder builder = new AuthToLocalBuilder(REALM, List.of(), false);
    builder.addRule("ambari-qa-draft@" + REALM, "ambari-qa");
    builder.addRule("dn/_HOST@" + REALM, "hdfs");
    builder.addRule("hbase/_HOST@" + REALM, "hbase");
    builder.addRule("nn/_HOST@" + REALM, "hdfs");
    String activeStockTemplate = activeStockTemplate();
    PolicySource source = source(builder.generate(), REALM, activeStockTemplate);

    Policy policy = verifier.inspectPolicy(source);

    assertEquals(REALM, policy.effectiveDefaultRealm());
    assertEquals(verifier.fingerprintTemplate(activeStockTemplate),
        source.expectedStockKrb5ConfTemplateFingerprint());
  }

  @Test
  void provesGeneratedConsumerLocalDaemonHeadlessAndSmokeMappings() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder(REALM, List.of(), false);
    builder.addRule(USER_A + "/_HOST@" + REALM, USER_A);
    builder.addRule(USER_A + "@" + REALM, USER_A);
    builder.addRule("ambari-qa-draft@" + REALM, "ambari-qa");

    ConsumerLocalMappingProof proof = verifier.proveConsumerLocalMappings(
        builder.generate(), new ConsumerLocalMappingInput(REALM, USER_A,
            USER_A + "/_HOST@" + REALM, USER_A + "@" + REALM,
            "ambari-qa-draft@" + REALM, "ambari-qa"));

    assertEquals(USER_A, proof.effectiveShortUser());
    assertEquals("ambari-qa", proof.smokeShortUser());
  }

  @Test
  void rejectsConsumerLocalHeadlessOrSmokeMappingToTheWrongUser() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder(REALM, List.of(), false);
    builder.addRule(USER_A + "/_HOST@" + REALM, USER_A);
    builder.addRule(USER_A + "@" + REALM, "hbase");
    builder.addRule("ambari-qa-draft@" + REALM, "shared_smoke");

    assertCode("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
        () -> verifier.proveConsumerLocalMappings(builder.generate(),
            new ConsumerLocalMappingInput(REALM, USER_A,
                USER_A + "/_HOST@" + REALM, USER_A + "@" + REALM,
                "ambari-qa-draft@" + REALM, "ambari-qa")));
  }

  @Test
  void rejectsConsumerLocalProofThatReliesOnTerminalDefault() {
    AuthToLocalBuilder missingDaemonAndHeadless =
        new AuthToLocalBuilder(REALM, List.of(), false);
    missingDaemonAndHeadless.addRule("ambari-qa-draft@" + REALM, "ambari-qa");

    assertCode("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
        () -> verifier.proveConsumerLocalMappings(missingDaemonAndHeadless.generate(),
            new ConsumerLocalMappingInput(REALM, USER_A,
                USER_A + "/_HOST@" + REALM, USER_A + "@" + REALM,
                "ambari-qa-draft@" + REALM, "ambari-qa")));
  }

  @Test
  void preservesGeneratedUnescapedDotRegexSemantics() {
    String wildcardDotRule = String.join("\n",
        "RULE:[2:$1@$0](" + USER_A + "@EXAMPLE.COM)s/.*/shared_hbase/",
        "RULE:[1:$1@$0](.*@EXAMPLExCOM)s/@.*//",
        "DEFAULT");
    PolicySource source = source(wildcardDotRule, "EXAMPLExCOM");
    Policy policy = verifier.inspectPolicy(source);

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> verifier.provePattern(source, policy, pattern(USER_A, "EXAMPLExCOM")));

    assertEquals("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH", error.getCode());
  }

  @Test
  void rejectsAnEarlierGeneratedRuleThatMapsToSharedAuthority() {
    String rules = String.join("\n",
        "RULE:[2:$1@$0](" + USER_A + "@EXAMPLE.COM)s/.*/hbase/",
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//",
        "DEFAULT");
    PolicySource source = source(rules, REALM);

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> verifier.provePattern(source, verifier.inspectPolicy(source),
            pattern(USER_A, REALM)));

    assertEquals("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH", error.getCode());
    assertFalse(error.getMessage().contains(rules));
  }

  @Test
  void rejectsHostDependentAndUnknownRuleGrammar() {
    String hostRule = "RULE:[2:$1/$2@$0](" + USER_A
        + "/.*@EXAMPLE.COM)s/.*/" + USER_A + "/\nDEFAULT";
    assertCode("DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNSUPPORTED",
        () -> verifier.inspectPolicy(source(hostRule, REALM)));

    String customSubstitution = "RULE:[2:$1@$0](" + USER_A
        + "@EXAMPLE.COM)s/hbase/custom/\nDEFAULT";
    assertCode("DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNSUPPORTED",
        () -> verifier.inspectPolicy(source(customSubstitution, REALM)));
  }

  @Test
  void rejectsUnmanagedMitAndAmbiguousDefaultRealmInputs() {
    PolicySource ordinary = source(ORDINARY_RULES, REALM);
    assertCode("DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNMANAGED",
        () -> verifier.inspectPolicy(copy(ordinary, "", false, true, REALM, "/etc",
            STOCK_TEMPLATE, verifier.fingerprintTemplate(STOCK_TEMPLATE))));
    assertCode("DEPENDENCY_HDFS_AUTH_TO_LOCAL_UNSUPPORTED",
        () -> verifier.inspectPolicy(copy(ordinary, "mit", true, true, REALM, "/etc",
            STOCK_TEMPLATE, verifier.fingerprintTemplate(STOCK_TEMPLATE))));
    assertCode("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
        () -> verifier.inspectPolicy(copy(ordinary, "", true, false, REALM, "/etc",
            STOCK_TEMPLATE, verifier.fingerprintTemplate(STOCK_TEMPLATE))));
    assertCode("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
        () -> verifier.inspectPolicy(copy(ordinary, "", true, true, "OTHER.COM", "/etc",
            STOCK_TEMPLATE, verifier.fingerprintTemplate(STOCK_TEMPLATE))));
    assertCode("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
        () -> verifier.inspectPolicy(copy(ordinary, "", true, true, REALM, "/custom",
            STOCK_TEMPLATE, verifier.fingerprintTemplate(STOCK_TEMPLATE))));
  }

  @Test
  void fallsBackOnlyWhenKrb5ConfRealmKeyIsAbsent() {
    PolicySource absentRealm = source(ORDINARY_RULES, REALM);
    assertEquals(REALM, verifier.inspectPolicy(absentRealm).effectiveDefaultRealm());

    assertCode("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
        () -> verifier.inspectPolicy(copy(absentRealm, "", true, true, "", "/etc",
            STOCK_TEMPLATE, verifier.fingerprintTemplate(STOCK_TEMPLATE))));
    assertCode("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
        () -> verifier.inspectPolicy(copy(absentRealm, "", true, true, "   ", "/etc",
            STOCK_TEMPLATE, verifier.fingerprintTemplate(STOCK_TEMPLATE))));
  }

  @Test
  void rejectsTemplateAndPolicyDrift() {
    PolicySource source = source(ORDINARY_RULES, REALM);
    String changedTemplate = STOCK_TEMPLATE + "  dns_lookup_kdc = false\n";
    assertCode("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
        () -> verifier.inspectPolicy(copy(source, "", true, true, REALM, "/etc",
            changedTemplate, verifier.fingerprintTemplate(STOCK_TEMPLATE))));

    Policy inspected = verifier.inspectPolicy(source);
    PolicySource changedRules = source(ORDINARY_RULES.replace("nn@", "jn@"), REALM);
    assertCode("DEPENDENCY_HDFS_AUTH_TO_LOCAL_STALE",
        () -> verifier.provePattern(changedRules, inspected, pattern(USER_A, REALM)));
  }

  @Test
  void fingerprintsDoNotExportRawRulesOrTemplate() throws Exception {
    PolicySource source = source(ORDINARY_RULES, REALM);
    Policy policy = verifier.inspectPolicy(source);
    ConsumerPatternProof proof = verifier.provePattern(source, policy, pattern(USER_A, REALM));
    String output = new ObjectMapper().writeValueAsString(List.of(source, policy, proof));

    assertFalse(output.contains("effectiveRules"));
    assertFalse(output.contains("krb5ConfTemplate"));
    assertFalse(output.contains("RULE:"));
    assertFalse(source.toString().contains("RULE:"));
    assertFalse(source.toString().contains("default_realm"));
    assertTrue(output.contains("policyFingerprint"));
    assertTrue(output.contains("proofFingerprint"));
  }

  @Test
  void evaluatesIndependentPoliciesConcurrently() throws Exception {
    PolicySource sourceA = source(ORDINARY_RULES, REALM);
    String otherRules = ORDINARY_RULES.replace("EXAMPLE.COM", "OTHER.COM");
    PolicySource sourceB = source(otherRules, "OTHER.COM");
    Policy policyA = verifier.inspectPolicy(sourceA);
    Policy policyB = verifier.inspectPolicy(sourceB);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Callable<ConsumerPatternProof>> calls = List.of(
          () -> verifier.provePattern(sourceA, policyA, pattern(USER_A, REALM)),
          () -> verifier.provePattern(sourceB, policyB, pattern(USER_B, "OTHER.COM")));
      List<Future<ConsumerPatternProof>> results = executor.invokeAll(calls);

      assertEquals(USER_A, results.get(0).get().mappedShortUser());
      assertEquals(USER_B, results.get(1).get().mappedShortUser());
      assertNotEquals(results.get(0).get().providerPolicyFingerprint(),
          results.get(1).get().providerPolicyFingerprint());
    } finally {
      executor.shutdownNow();
    }
  }

  private PolicySource source(String rules, String realm) {
    return source(rules, realm, STOCK_TEMPLATE);
  }

  private PolicySource source(String rules, String realm, String template) {
    return new PolicySource(rules, "", true, realm, true, null, "/etc",
        template, verifier.fingerprintTemplate(template));
  }

  private PolicySource copy(PolicySource source, String mechanism, boolean managesRules,
      boolean managesKrb5Conf, String krb5Realm, String directory, String template,
      String expectedTemplateFingerprint) {
    return new PolicySource(source.effectiveRules(), mechanism, managesRules,
        source.kerberosEnvRealm(), managesKrb5Conf, krb5Realm, directory, template,
        expectedTemplateFingerprint);
  }

  private ConsumerPatternInput pattern(String user, String realm) {
    return new ConsumerPatternInput(realm, user, user + "/_HOST@" + realm);
  }

  private String activeStockTemplate() throws IOException {
    String path = "stacks/BIGTOP/3.2.0/services/KERBEROS/properties/krb5_conf.j2";
    try (InputStream input = Objects.requireNonNull(
        getClass().getClassLoader().getResourceAsStream(path), path)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private void assertCode(String code, Runnable action) {
    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class, action::run);
    assertEquals(code, error.getCode());
  }
}
