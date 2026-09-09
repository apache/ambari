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

import java.util.List;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.ConsumerInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.PairProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.Policy;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.PolicySource;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ManagedZooKeeperSaslPolicyVerifierTest {
  private static final String REALM = "EXAMPLE.COM";
  private static final String USER_A = "hbase_mc_0123456789abcdefabcd";
  private static final String USER_B = "hbase_mc_fedcba9876543210abcd";

  private final ManagedZooKeeperSaslPolicyVerifier verifier =
      new ManagedZooKeeperSaslPolicyVerifier();

  @Test
  void providerPolicyIsIndependentAndPairProofIsConsumerSpecific() {
    PolicySource source = source("DEFAULT");
    Policy policy = verifier.inspectPolicy(source);
    PairProof first = verifier.provePair(source, policy, consumer(USER_A, REALM));
    PairProof second = verifier.provePair(source, policy, consumer(USER_B, REALM));

    assertEquals(first.providerPolicyFingerprint(), second.providerPolicyFingerprint());
    assertNotEquals(first.principalPatternFingerprint(), second.principalPatternFingerprint());
    assertNotEquals(first.proofFingerprint(), second.proofFingerprint());
    assertEquals(USER_A, first.expectedAuthorizationId());
  }

  @Test
  void rejectsCustomMappingMissingRemovalAndCrossRealm() {
    assertCode("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
        () -> verifier.inspectPolicy(source("RULE:[1:$1@$0](.*)s/.*/shared/")));
    PolicySource missingRemoval = new PolicySource(REALM, true, "zookeeper", true,
        true, false, "DEFAULT");
    assertCode("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
        () -> verifier.inspectPolicy(missingRemoval));

    PolicySource source = source("DEFAULT");
    Policy policy = verifier.inspectPolicy(source);
    assertCode("CROSS_REALM_NOT_SUPPORTED",
        () -> verifier.provePair(source, policy, consumer(USER_A, "OTHER.COM")));
  }

  @Test
  void rejectsPolicyDriftAndMismatchedAuthorizationId() {
    PolicySource source = source("DEFAULT");
    Policy policy = verifier.inspectPolicy(source);
    PolicySource changed = new PolicySource(REALM, true, "zk", true,
        true, true, "DEFAULT");
    assertCode("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_STALE",
        () -> verifier.provePair(changed, policy, consumer(USER_A, REALM)));
    assertCode("DEPENDENCY_ZOOKEEPER_AUTHORIZATION_FAILED",
        () -> verifier.provePair(source, policy, new ConsumerInput(REALM, USER_A,
            USER_A + "/_HOST@" + REALM, USER_B)));
  }

  @Test
  void rawMappingSourceIsNotSerializedOrLogged() throws Exception {
    PolicySource source = source("DEFAULT");
    Policy policy = verifier.inspectPolicy(source);
    PairProof proof = verifier.provePair(source, policy, consumer(USER_A, REALM));
    String json = new ObjectMapper().writeValueAsString(List.of(source, policy, proof));

    assertFalse(json.contains("authToLocalRules"));
    assertFalse(source.toString().contains("DEFAULT"));
  }

  private PolicySource source(String rules) {
    return new PolicySource(REALM, true, "zookeeper", true, true, true, rules);
  }

  private ConsumerInput consumer(String user, String realm) {
    return new ConsumerInput(realm, user, user + "/_HOST@" + realm, user);
  }

  private void assertCode(String code, Runnable action) {
    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class, action::run);
    assertEquals(code, error.getCode());
  }
}
