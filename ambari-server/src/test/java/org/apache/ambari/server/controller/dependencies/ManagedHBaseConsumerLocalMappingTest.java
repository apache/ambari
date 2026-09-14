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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.ambari.server.controller.AuthToLocalBuilder;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Plan;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosDescriptorOverlay.SealedConfigurations;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingProof;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ManagedHBaseConsumerLocalMappingTest {
  private static final String REALM = "EXAMPLE.COM";
  private static final String SMOKE_PRINCIPAL = "ambari-qa-plan@EXAMPLE.COM";
  private static final String SMOKE_USER = "ambari-qa";
  private static final UUID BINDING_ID =
      UUID.fromString("cd6de25d-a556-41f3-90d8-bcec840c27f3");

  @Test
  void createsAnImmutableConsumerOwnedProfileFromSealedConfiguration() throws Exception {
    ManagedHBaseKerberosOverlaySpec spec = spec(1L);
    String rules = rules(spec.effectiveShortUser());
    ManagedHBaseConsumerLocalMapping mapping = create(spec, rules);
    String json = new ObjectMapper().writeValueAsString(mapping);

    assertEquals(rules, mapping.canonicalRules());
    assertEquals(spec.identityPlanFingerprint(), mapping.identityPlanFingerprint());
    assertEquals(spec.effectiveShortUser(), mapping.proof().effectiveShortUser());
    assertFalse(mapping.toString().contains("RULE:["));
    assertFalse(json.contains("provider"));
  }

  @Test
  void equivalentPlanAndLiveFactsProduceTheSameProfile() {
    ManagedHBaseKerberosOverlaySpec plan = spec(1L);
    ManagedHBaseKerberosOverlaySpec live = spec(1L);

    assertEquals(create(plan, rules(plan.effectiveShortUser())),
        create(live, rules(live.effectiveShortUser())));
  }

  @Test
  void changedConsumerRulesOrIdentityPlanProduceDifferentProfiles() {
    ManagedHBaseKerberosOverlaySpec first = spec(1L);
    ManagedHBaseKerberosOverlaySpec second = spec(2L);
    ManagedHBaseConsumerLocalMapping firstMapping =
        create(first, rules(first.effectiveShortUser()));
    ManagedHBaseConsumerLocalMapping secondMapping =
        create(second, rules(second.effectiveShortUser()));

    assertNotEquals(firstMapping.profileFingerprint(), secondMapping.profileFingerprint());
    assertNotEquals(firstMapping.proof().rulesFingerprint(),
        secondMapping.proof().rulesFingerprint());
  }

  @Test
  void rejectsNoncanonicalRulesAndForgedProof() {
    ManagedHBaseKerberosOverlaySpec spec = spec(1L);
    ManagedHBaseConsumerLocalMapping mapping = create(spec, rules(spec.effectiveShortUser()));
    assertThrows(IllegalArgumentException.class, () -> new ManagedHBaseConsumerLocalMapping(
        mapping.schemaVersion(), mapping.realm(), mapping.rolePrincipalPattern(),
        mapping.headlessPrincipal(), mapping.smokePrincipal(), mapping.smokeShortUser(),
        mapping.identityPlanFingerprint(), mapping.canonicalRules().replace("\n", "\r\n"),
        mapping.proof(), mapping.profileFingerprint()));

    ManagedHBaseKerberosOverlaySpec other = spec(2L);
    ConsumerLocalMappingProof otherProof = proof(other, rules(other.effectiveShortUser()));
    assertThrows(IllegalArgumentException.class, () -> new ManagedHBaseConsumerLocalMapping(
        mapping.schemaVersion(), mapping.realm(), mapping.rolePrincipalPattern(),
        mapping.headlessPrincipal(), mapping.smokePrincipal(), mapping.smokeShortUser(),
        mapping.identityPlanFingerprint(), mapping.canonicalRules(), otherProof,
        mapping.profileFingerprint()));

    assertThrows(IllegalArgumentException.class,
        () -> ManagedHBaseConsumerLocalMapping.fingerprintRules("RULE:x\n\nDEFAULT"));
    assertThrows(IllegalArgumentException.class,
        () -> ManagedHBaseConsumerLocalMapping.fingerprintRules(
            "x".repeat(ManagedHBaseConsumerLocalMapping.MAX_RULE_BYTES + 1)));
  }

  private ManagedHBaseConsumerLocalMapping create(ManagedHBaseKerberosOverlaySpec spec,
      String rules) {
    ConsumerLocalMappingProof proof = proof(spec, rules);
    Map<String, Map<String, String>> configs = Map.of(
        "core-site", Map.of("hadoop.security.auth_to_local", rules));
    return ManagedHBaseConsumerLocalMapping.create(spec,
        new SealedConfigurations(configs, proof));
  }

  private ConsumerLocalMappingProof proof(ManagedHBaseKerberosOverlaySpec spec, String rules) {
    return new ManagedHdfsAuthToLocalVerifier().proveConsumerLocalMappings(rules,
        new ConsumerLocalMappingInput(spec.realm(), spec.effectiveShortUser(),
            spec.rolePrincipalPattern(), spec.headlessPrincipal(), spec.smokePrincipal(),
            spec.smokeShortUser()));
  }

  private ManagedHBaseKerberosOverlaySpec spec(long clusterId) {
    Plan plan = Plan.forExistingCluster(clusterId);
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings = new TreeMap<>();
    bindings.put(ManagedDependencyType.HDFS,
        new ManagedBindingSnapshotRef(ManagedDependencyType.HDFS, BINDING_ID));
    return ManagedHBaseKerberosOverlaySpec.create(REALM, plan.plannedShortUser(),
        SMOKE_PRINCIPAL, SMOKE_USER, plan.planFingerprint(), bindings);
  }

  private String rules(String user) {
    AuthToLocalBuilder builder = new AuthToLocalBuilder(REALM, "", false);
    builder.addRule(user + "/_HOST@" + REALM, user);
    builder.addRule(user + "@" + REALM, user);
    builder.addRule(SMOKE_PRINCIPAL, SMOKE_USER);
    return builder.generate();
  }
}
