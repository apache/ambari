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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.ambari.server.controller.AuthToLocalBuilder;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.CommandName;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.Envelope;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.Result;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyCommand.ResultStatus;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Plan;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosDescriptorOverlay.SealedConfigurations;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerLocalMappingProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerPatternInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.ConsumerInput;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class ManagedDependencyCommandTest {
  private static final UUID BINDING_ID = UUID.fromString("59646bd5-39eb-414e-a07e-f5469d369687");
  private static final UUID OPERATION_ID = UUID.fromString("43d3dad3-d0b0-4ba2-a9ae-2ccf7ab758f2");
  private static final long ACTION_HOST_ID = 21L;
  private static final String IDENTITY_HASH =
      "sha256:427bca797fe223f7a612abe0292533df9a97eda718746863a056e186736846bb";
  private static final String PROVIDER_HASH =
      "sha256:540491df8aa26c85b4082bec6030c2ccaba50950c79f5dfc12208e1addb1d155";
  private static final String CONSUMER_HASH =
      "sha256:8b909f58874fb7c54894e73bfcce5be08fae2b75c1660c1c084462c164a12912";
  private static final UUID PREPARATION_OBSERVATION_ID =
      UUID.fromString("4be3c05a-b27c-44c7-a430-a0ff6bfddb28");
  private static final String PREPARATION_OBSERVATION_HASH =
      "sha256:7dc1dd44b1a75f745d6a32ca91f69159096f314ca842d43c6371d27eb467b67e";
  private static final UUID LOW_BIT_BINDING_ID =
      UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final UUID HIGH_BIT_BINDING_ID =
      UUID.fromString("f0000000-0000-4000-8000-000000000002");
  private static final UUID HIGH_BIT_OPERATION_ID =
      UUID.fromString("f0000000-0000-4000-8000-000000000003");

  @Test
  void buildersIncludeCompleteEnvelopeAndReservedParameters() {
    ManagedDependencySnapshot snapshot = snapshot(ManagedDependencyType.HDFS);

    ManagedDependencyCommand provision = ManagedDependencyCommand.provision(
        snapshot, OPERATION_ID, 9, ACTION_HOST_ID);
    ManagedDependencyCommand prepare = ManagedDependencyCommand.prepareConsumer(
        snapshot, OPERATION_ID, 9, 42, "hadoop_3_3_0_0_1-client", "3.3.0", IDENTITY_HASH);
    ManagedDependencyCommand verify = verifyConsumer(
        snapshot, OPERATION_ID, 9, 42, "hadoop_3_3_0_0_1-client", "3.3.0-1", IDENTITY_HASH);
    ManagedDependencyCommand invalidate = ManagedDependencyCommand.invalidate(
        snapshot, OPERATION_ID, 10, ACTION_HOST_ID);

    assertEquals(CommandName.PROVISION_HDFS_NAMESPACE, provision.name());
    assertEquals(CommandName.PREPARE_HDFS_CONSUMER, prepare.name());
    assertEquals(CommandName.VERIFY_HDFS_CONSUMER, verify.name());
    assertEquals(CommandName.INVALIDATE_BINDING_EPOCH, invalidate.name());
    assertTrue(!prepare.parameters().containsKey("client.package.version"));
    assertEquals(prepare.envelope().immutableRequestHash(),
        verify.parameters().get("preparation.request.hash"));
    for (ManagedDependencyCommand command : List.of(provision, prepare, verify, invalidate)) {
      assertEquals(ManagedDependencyCommand.CURRENT_PROTOCOL_VERSION, command.envelope().protocolVersion());
      assertEquals(BINDING_ID, command.envelope().bindingId());
      assertEquals(OPERATION_ID, command.envelope().operationId());
      assertEquals(7, command.envelope().snapshotVersion());
      assertTrue(command.envelope().immutableRequestHash().matches("sha256:[0-9a-f]{64}"));
    }
  }

  @Test
  void immutableRequestHashRejectsAllowedValueTamperingAndUnknownPaths() {
    ManagedDependencyCommand command = verifyConsumer(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 9, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0-1", IDENTITY_HASH);
    TreeMap<String, String> tampered = new TreeMap<>(command.parameters());
    tampered.put("host.id", "43");
    assertThrows(IllegalArgumentException.class,
        () -> new ManagedDependencyCommand(command.name(), command.envelope(), tampered));

    TreeMap<String, String> arbitraryPath = new TreeMap<>(command.parameters());
    arbitraryPath.put("arbitrary.provider.path", "/user/hbase");
    assertThrows(IllegalArgumentException.class,
        () -> new ManagedDependencyCommand(command.name(), command.envelope(), arbitraryPath));

    ManagedDependencyCommand provision = ManagedDependencyCommand.provision(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 9, ACTION_HOST_ID);
    TreeMap<String, String> traversal = new TreeMap<>(provision.parameters());
    traversal.put("namespace.root.uri", "hdfs://nn.example.test:8020/apps/ambari-managed/hbase/"
        + BINDING_ID + "/../../foreign/root");
    assertThrows(IllegalArgumentException.class,
        () -> new ManagedDependencyCommand(provision.name(), provision.envelope(), traversal));
  }

  @Test
  void verifyCommandCarriesOnlyCanonicalAllowlistedSnapshotConfiguration() {
    ManagedDependencyCommand command = verifyConsumer(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 9, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0-1", IDENTITY_HASH);

    assertEquals("hbase_mc_c1", command.parameters().get("consumer.user"));
    assertEquals("{\"coreSite\":{\"fs.defaultFS\":\"hdfs://nn.example.test:8020\"},"
            + "\"hdfsSite\":{},\"zooKeeperClient\":{}}",
        command.parameters().get("client.config.json"));

    TreeMap<String, String> nonCanonical = new TreeMap<>(command.parameters());
    nonCanonical.put("client.config.json",
        "{\"hdfsSite\":{},\"coreSite\":{\"fs.defaultFS\":\"hdfs://nn.example.test:8020\"},"
            + "\"zooKeeperClient\":{}}");
    assertThrows(IllegalArgumentException.class,
        () -> rehash(command, nonCanonical));

    TreeMap<String, String> forbidden = new TreeMap<>(command.parameters());
    forbidden.put("client.config.json",
        "{\"coreSite\":{\"fs.defaultFS\":\"hdfs://nn.example.test:8020\","
            + "\"fs.s3a.secret.key\":\"redacted\"},\"hdfsSite\":{},\"zooKeeperClient\":{}}");
    assertThrows(IllegalArgumentException.class,
        () -> rehash(command, forbidden));
  }

  @Test
  void requestHashUsesJavaUtf16LengthsAndUtf8DigestBytes() {
    ManagedDependencyCommand command = verifyConsumer(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 3, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0-\u7248\u672c-\ud83d\ude00", IDENTITY_HASH);

    assertEquals("sha256:8dceee0e7453229fce1ded1bd641a6c6809995b9c87f648f120632eaafe317ca",
        command.envelope().immutableRequestHash());
  }

  @Test
  void secureTransportHashVectorMatchesThePythonParserContract() {
    ManagedDependencyCommand command = ManagedDependencyCommand.prepareConsumer(
        secureHdfsSnapshot(), OPERATION_ID, 9, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0", IDENTITY_HASH);

    assertEquals("sha256:630da583e3050d9c20820af4a6c3b7c22a4227f0574152d2eebfd2a2ea569344",
        command.parameters().get("consumer.mapping.rules.fingerprint"));
    assertEquals("sha256:aadab72f4f85131ac69d8415ad39bbb59105829d8da73976ec61cfc7623a8b28",
        command.parameters().get("consumer.mapping.proof.fingerprint"));
    assertEquals("sha256:8b8fcaee5c89ab3cd239eb26da714d4b587aef04639b0d96feedceb15b0344ef",
        command.parameters().get("consumer.mapping.profile.fingerprint"));
    assertEquals("sha256:497cf0d53b9fc2e1fd7490ebb9f1dd56cf2d25d45831b088d4faf77cf27a713d",
        command.parameters().get("client.config.fingerprint"));
    assertEquals("sha256:add05f0fbf9ca7951a938b5ed25e064aba9ca485494e3e978b008738dede770d",
        command.envelope().immutableRequestHash());
  }

  @Test
  void commandParametersAreDefensivelyCopied() {
    ManagedDependencyCommand original = ManagedDependencyCommand.provision(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 9, ACTION_HOST_ID);
    TreeMap<String, String> source = new TreeMap<>(original.parameters());
    ManagedDependencyCommand copy = new ManagedDependencyCommand(
        original.name(), original.envelope(), source);

    source.put("owner.user", "mutated");

    assertEquals("hbase_mc_c1", copy.parameters().get("owner.user"));
    assertThrows(UnsupportedOperationException.class,
        () -> copy.parameters().put("owner.user", "mutated"));
  }

  @Test
  void zooKeeperProvisionUsesReservedParentAndExplicitAclPolicies() {
    ManagedDependencyCommand command = ManagedDependencyCommand.provision(
        snapshot(ManagedDependencyType.ZOOKEEPER), OPERATION_ID, 9, ACTION_HOST_ID);

    assertEquals(CommandName.PROVISION_ZOOKEEPER_NAMESPACE, command.name());
    assertEquals("/ambari-managed-hbase", command.parameters().get("namespace.parent.znode"));
    assertEquals("/ambari-managed-hbase/" + BINDING_ID,
        command.parameters().get("namespace.container.znode"));
    assertEquals("/ambari-managed-hbase/" + BINDING_ID + "/hbase",
        command.parameters().get("namespace.znode"));
    assertEquals("INSECURE_PROVIDER_PREPARED", command.parameters().get("parent.acl.policy"));
    assertEquals("INSECURE_BINDING_SCOPED", command.parameters().get("subtree.acl.policy"));
  }

  @Test
  void secureZooKeeperHandoffCarriesExactLedgerIdentityAndReconciliationEvidence() {
    ManagedDependencyCommand command = ManagedDependencyCommand.provision(
        secureZooKeeperSnapshot(), OPERATION_ID, 9, ACTION_HOST_ID);

    assertEquals("/ambari-managed-hbase/.bindings/" + BINDING_ID,
        command.parameters().get("namespace.ledger.znode"));
    assertEquals("hbase_mc_c1", command.parameters().get("consumer.zk.sasl.id"));
    TreeMap<String, String> facts = new TreeMap<>(Map.of(
        "applied.snapshot.fingerprint", command.parameters().get("snapshot.fingerprint"),
        "consumer.zk.sasl.id", "hbase_mc_c1",
        "namespace.container.znode", "/ambari-managed-hbase/" + BINDING_ID,
        "namespace.ledger.znode", "/ambari-managed-hbase/.bindings/" + BINDING_ID,
        "namespace.znode", "/ambari-managed-hbase/" + BINDING_ID + "/hbase",
        "provider.action.host.id", Long.toString(ACTION_HOST_ID),
        "provider.handoff.reconciliation.required", "true"));
    Result reconciliation = new Result(command.name(), command.envelope(),
        ResultStatus.RECONCILIATION_REQUIRED, facts,
        ManagedDependencyErrorCode.DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED,
        "The consumer must reconcile the exact ZooKeeper handoff");

    command.validateResult(reconciliation);
    facts.put("provider.action.host.id", "22");
    Result wrongHost = new Result(command.name(), command.envelope(),
        ResultStatus.RECONCILIATION_REQUIRED, facts,
        ManagedDependencyErrorCode.DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED,
        "The consumer must reconcile the exact ZooKeeper handoff");
    assertThrows(IllegalArgumentException.class, () -> command.validateResult(wrongHost));
  }

  @Test
  void secureConsumerCommandsAndResultsBindRolePrincipalAndZooKeeperAclProof()
      throws Exception {
    ManagedDependencyCommand hdfs = verifyConsumer(
        secureHdfsSnapshot(), OPERATION_ID, 9, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0-1", IDENTITY_HASH);
    assertEquals("hbase_mc_c1/_HOST@EXAMPLE.COM",
        hdfs.parameters().get("consumer.kerberos.principal.pattern"));
    ManagedHBaseConsumerLocalMapping mapping = secureHdfsSnapshot().consumerLocalMapping();
    assertEquals(mapping.profileFingerprint(),
        hdfs.parameters().get("consumer.mapping.profile.fingerprint"));
    assertEquals(mapping.proof().proofFingerprint(),
        hdfs.parameters().get("consumer.mapping.proof.fingerprint"));
    assertEquals(mapping.proof().rulesFingerprint(),
        hdfs.parameters().get("consumer.mapping.rules.fingerprint"));
    JsonNode hdfsClient = new ObjectMapper().readTree(
        hdfs.parameters().get("client.config.json"));
    assertEquals(mapping.canonicalRules(),
        hdfsClient.get("coreSite").get("hadoop.security.auth_to_local").asText());
    TreeMap<String, String> missingProof = new TreeMap<>(hdfs.parameters());
    missingProof.remove("provider.security.policy.fingerprint");
    assertThrows(IllegalArgumentException.class,
        () -> new ManagedDependencyCommand(hdfs.name(), hdfs.envelope(), missingProof));
    TreeMap<String, String> wrongRules = new TreeMap<>(hdfs.parameters());
    wrongRules.put("consumer.mapping.rules.fingerprint", "sha256:" + "0".repeat(64));
    assertThrows(IllegalArgumentException.class, () -> rehash(hdfs, wrongRules));
    Result hdfsResult = new Result(hdfs.name(), hdfs.envelope(), ResultStatus.SUCCEEDED,
        new TreeMap<>(Map.ofEntries(
            Map.entry("applied.snapshot.fingerprint",
                hdfs.parameters().get("snapshot.fingerprint")),
            Map.entry("client.config.fingerprint",
                hdfs.parameters().get("client.config.fingerprint")),
            Map.entry("client.package.name", "hadoop_3_3_0_0_1-client"),
            Map.entry("client.package.version", "3.3.0-1"),
            Map.entry("client.software.kind", "HADOOP_CLIENT"),
            Map.entry("client.software.semantic.version", "3.3.0"),
            Map.entry("consumer.kerberos.principal",
                "hbase_mc_c1/host.example.test@EXAMPLE.COM"),
            Map.entry("datanode.read.write.verified", "true"),
            Map.entry("host.id", "42"),
            Map.entry("identity.fingerprint", IDENTITY_HASH),
            Map.entry("namenode.rpc.connected", "true"))), null, null);
    hdfs.validateResult(hdfsResult);

    ManagedDependencyCommand zooKeeper = verifyConsumer(
        secureZooKeeperSnapshot(), OPERATION_ID, 9, 42,
        "hbase_3_3_0_0_1", "2.4.13-1", IDENTITY_HASH);
    JsonNode zooKeeperClient = new ObjectMapper().readTree(
        zooKeeper.parameters().get("client.config.json"));
    assertEquals(0, zooKeeperClient.get("coreSite").size());
    assertEquals(mapping.profileFingerprint(),
        zooKeeper.parameters().get("consumer.mapping.profile.fingerprint"));
    Result zooKeeperResult = new Result(zooKeeper.name(), zooKeeper.envelope(),
        ResultStatus.SUCCEEDED, new TreeMap<>(Map.ofEntries(
            Map.entry("applied.snapshot.fingerprint",
                zooKeeper.parameters().get("snapshot.fingerprint")),
            Map.entry("client.config.fingerprint",
                zooKeeper.parameters().get("client.config.fingerprint")),
            Map.entry("client.package.name", "hbase_3_3_0_0_1"),
            Map.entry("client.package.version", "2.4.13-1"),
            Map.entry("client.software.kind", "HBASE_CLIENT"),
            Map.entry("client.software.semantic.version", "2.4.13"),
            Map.entry("consumer.kerberos.principal",
                "hbase_mc_c1/host.example.test@EXAMPLE.COM"),
            Map.entry("consumer.zk.sasl.id", "hbase_mc_c1"),
            Map.entry("container.acl.verified", "true"),
            Map.entry("hbase.znode.verified", "true"),
            Map.entry("host.id", "42"),
            Map.entry("identity.fingerprint", IDENTITY_HASH),
            Map.entry("private.znode.verified", "true"),
            Map.entry("sibling.authority.denied", "true"),
            Map.entry("zookeeper.quorum.connected", "true"))), null, null);
    zooKeeper.validateResult(zooKeeperResult);

    TreeMap<String, String> broadened = new TreeMap<>(zooKeeperResult.facts());
    broadened.put("consumer.zk.sasl.id", "hbase");
    Result wrongIdentity = new Result(zooKeeper.name(), zooKeeper.envelope(),
        ResultStatus.SUCCEEDED, broadened, null, null);
    assertThrows(IllegalArgumentException.class, () -> zooKeeper.validateResult(wrongIdentity));
  }

  @Test
  void resultUsesRequestEnvelopeAndRejectsUnknownFacts() {
    ManagedDependencyCommand command = ManagedDependencyCommand.provision(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 9, ACTION_HOST_ID);
    Result success = new Result(command.name(), command.envelope(), ResultStatus.SUCCEEDED,
        new TreeMap<>(Map.of(
            "applied.snapshot.fingerprint", command.parameters().get("snapshot.fingerprint"),
            "directory.mode", "0700",
            "namespace.root.exists", "true",
            "namespace.wal.exists", "true",
            "owner.group", "hbase_mc_c1",
            "owner.user", "hbase_mc_c1",
            "provider.action.host.id", Long.toString(ACTION_HOST_ID))), null, null);

    assertEquals(command.envelope(), success.envelope());
    command.validateResult(success);
    TreeMap<String, String> wrongOwner = new TreeMap<>(success.facts());
    wrongOwner.put("owner.user", "another-user");
    Result mismatched = new Result(command.name(), command.envelope(), ResultStatus.SUCCEEDED,
        wrongOwner, null, null);
    assertThrows(IllegalArgumentException.class, () -> command.validateResult(mismatched));
    assertThrows(IllegalArgumentException.class, () -> new Result(
        command.name(), command.envelope(), ResultStatus.SUCCEEDED,
        new TreeMap<>(Map.of("untrusted.output", "value")), null, null));
    assertThrows(IllegalArgumentException.class, () -> new Result(
        command.name(), command.envelope(), ResultStatus.FAILED, new TreeMap<>(), null, null));
  }

  @Test
  void consumerResultMustMatchPersistedHostPackageIdentityAndSnapshot() {
    ManagedDependencyCommand command = verifyConsumer(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 9, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0-1", IDENTITY_HASH);
    TreeMap<String, String> facts = new TreeMap<>(Map.of(
        "applied.snapshot.fingerprint", command.parameters().get("snapshot.fingerprint"),
        "client.config.fingerprint", command.parameters().get("client.config.fingerprint"),
        "client.package.name", "hadoop_3_3_0_0_1-client",
        "client.package.version", "3.3.0-1",
        "client.software.kind", "HADOOP_CLIENT",
        "client.software.semantic.version", "3.3.0",
        "datanode.read.write.verified", "true",
        "host.id", "42",
        "identity.fingerprint", IDENTITY_HASH,
        "namenode.rpc.connected", "true"));
    Result result = new Result(command.name(), command.envelope(), ResultStatus.SUCCEEDED,
        facts, null, null);

    command.validateResult(result);

    facts.put("host.id", "43");
    Result wrongHost = new Result(command.name(), command.envelope(), ResultStatus.SUCCEEDED,
        facts, null, null);
    assertThrows(IllegalArgumentException.class, () -> command.validateResult(wrongHost));
  }

  @Test
  void envelopeRejectsUnknownProtocolAndMalformedHash() {
    assertThrows(IllegalArgumentException.class,
        () -> new Envelope(2, BINDING_ID, OPERATION_ID, 1, 1, IDENTITY_HASH));
    assertThrows(IllegalArgumentException.class,
        () -> new Envelope(1, BINDING_ID, OPERATION_ID, 1, 1, "not-a-hash"));
  }

  @Test
  void invalidationRejectsAnUnreservedProviderService() {
    ManagedDependencyCommand command = ManagedDependencyCommand.invalidate(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 10, ACTION_HOST_ID);
    TreeMap<String, String> parameters = new TreeMap<>(command.parameters());
    parameters.put("provider.service", "HBASE");

    assertThrows(IllegalArgumentException.class,
        () -> new ManagedDependencyCommand(command.name(), command.envelope(), parameters));
  }

  @Test
  void journalHandshakeBindsChallengeAuthorizationAndPinnedHost() {
    ManagedDependencySnapshot snapshot = snapshot(ManagedDependencyType.HDFS);
    ManagedDependencyCommand prepare = ManagedDependencyCommand.prepareJournal(
        snapshot, OPERATION_ID, 9, ACTION_HOST_ID);
    UUID challenge = UUID.fromString("5ab78d49-b473-4d81-b5ae-f8910a555d63");
    UUID authorization = UUID.fromString("3db076d6-ff66-4460-90b5-5b7d2f2b81af");
    UUID initializeOperation = UUID.fromString("cb5b2b96-ed50-47fb-aad0-628835ad72b5");
    ManagedDependencyCommand initialize = ManagedDependencyCommand.initializeJournal(
        snapshot, initializeOperation, 9, ACTION_HOST_ID, challenge, authorization,
        prepare.envelope().immutableRequestHash());

    assertEquals(CommandName.PREPARE_BINDING_JOURNAL, prepare.name());
    assertEquals(CommandName.INITIALIZE_BINDING_JOURNAL, initialize.name());
    assertEquals(Long.toString(ACTION_HOST_ID),
        initialize.parameters().get("provider.action.host.id"));
    assertEquals(prepare.envelope().immutableRequestHash(),
        initialize.parameters().get("initialization.prepare.request.hash"));

    Result prepareResult = new Result(prepare.name(), prepare.envelope(), ResultStatus.SUCCEEDED,
        new TreeMap<>(Map.of("initialization.challenge", challenge.toString())), null, null);
    prepare.validateResult(prepareResult);
    Result initializeResult = new Result(
        initialize.name(), initialize.envelope(), ResultStatus.SUCCEEDED,
        new TreeMap<>(Map.of(
            "initialization.authorization.id", authorization.toString(),
            "journal.initialized", "true",
            "provider.action.host.id", Long.toString(ACTION_HOST_ID))), null, null);
    initialize.validateResult(initializeResult);
  }

  @Test
  void consumerBundleAllowsOneCommandPerTypeAndBindsHostAndIdentity() {
    ManagedDependencyCommand hdfs = ManagedDependencyCommand.prepareConsumer(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 9, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0", IDENTITY_HASH);
    UUID zooKeeperBinding = UUID.fromString("997f43e1-9af8-4c74-97fb-1bb4d7a7f839");
    ManagedDependencyCommand zooKeeper = ManagedDependencyCommand.prepareConsumer(
        snapshot(ManagedDependencyType.ZOOKEEPER, zooKeeperBinding),
        UUID.fromString("2d84a326-ceb9-420a-aa53-0a963ac63db0"), 4, 42,
        "hbase_3_3_0_0_1", "2.4.13", IDENTITY_HASH);

    ManagedDependencyCommandBundle bundle = ManagedDependencyCommandBundle.of(
        42, "hbase_mc_c1", IDENTITY_HASH, List.of(zooKeeper, hdfs));

    assertEquals(List.of(hdfs, zooKeeper), bundle.commands());
    assertEquals(List.of(BINDING_ID, zooKeeperBinding), bundle.preparationBindingIds());
    assertEquals(bundle.commands(), bundle.preparationCommands());
    assertTrue(bundle.immutableBundleHash().matches("sha256:[0-9a-f]{64}"));
    ManagedDependencyCommandBundle hdfsOnly = ManagedDependencyCommandBundle.of(
        42, "hbase_mc_c1", IDENTITY_HASH, List.of(zooKeeper, hdfs), Set.of(BINDING_ID));
    assertEquals(List.of(BINDING_ID), hdfsOnly.preparationBindingIds());
    assertEquals(List.of(hdfs), hdfsOnly.preparationCommands());
    assertThrows(IllegalArgumentException.class, () -> ManagedDependencyCommandBundle.of(
        42, "hbase_mc_c1", IDENTITY_HASH, List.of(hdfs, hdfs)));
    assertThrows(IllegalArgumentException.class, () -> ManagedDependencyCommandBundle.of(
        43, "hbase_mc_c1", IDENTITY_HASH, List.of(hdfs)));
  }

  @Test
  void schemaOneGsonPayloadRetainsOldHashAndTreatsEveryCommandAsActive() throws Exception {
    ManagedDependencyCommand hdfs = ManagedDependencyCommand.prepareConsumer(
        snapshot(ManagedDependencyType.HDFS), OPERATION_ID, 9, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0", IDENTITY_HASH);
    ManagedDependencyCommandBundle current = ManagedDependencyCommandBundle.of(
        42, "hbase_mc_c1", IDENTITY_HASH, List.of(hdfs));
    ObjectNode legacy = (ObjectNode) new ObjectMapper().readTree(
        StageUtils.getGson().toJson(current));
    legacy.put("schemaVersion", 1);
    legacy.remove("preparationBindingIds");
    legacy.put("immutableBundleHash", ManagedDependencyCommandBundle.bundleHash(
        1, 42, "hbase_mc_c1", IDENTITY_HASH, current.commands()));

    ManagedDependencyCommandBundle parsed = StageUtils.getGson().fromJson(
        legacy.toString(), ManagedDependencyCommandBundle.class);

    assertEquals(1, parsed.schemaVersion());
    assertEquals(List.of(hdfs.envelope().bindingId()), parsed.preparationBindingIds());
    assertEquals(parsed.commands(), parsed.preparationCommands());
    assertEquals(legacy.get("immutableBundleHash").asText(), parsed.immutableBundleHash());
  }

  @Test
  void schemaTwoHashUsesCanonicalStringUuidOrderingAcrossSignedBoundary() {
    ManagedDependencyCommand lowBit = ManagedDependencyCommand.prepareConsumer(
        snapshot(ManagedDependencyType.HDFS, LOW_BIT_BINDING_ID), OPERATION_ID, 3, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0", IDENTITY_HASH);
    ManagedDependencyCommand highBit = ManagedDependencyCommand.prepareConsumer(
        snapshot(ManagedDependencyType.ZOOKEEPER, HIGH_BIT_BINDING_ID), HIGH_BIT_OPERATION_ID,
        4, 42, "hbase_3_3_0_0_1", "2.4.13", IDENTITY_HASH);

    ManagedDependencyCommandBundle bundle = ManagedDependencyCommandBundle.of(
        42, "hbase_mc_c1", IDENTITY_HASH, List.of(highBit, lowBit),
        Set.of(HIGH_BIT_BINDING_ID, LOW_BIT_BINDING_ID));

    assertEquals(List.of(LOW_BIT_BINDING_ID, HIGH_BIT_BINDING_ID),
        bundle.preparationBindingIds());
    assertEquals("sha256:f3ee1267cd79aa9f80faeac9efe4d7bc36bfa2fbf07b19a0ddda605ee9e9f719",
        bundle.immutableBundleHash());
  }

  @Test
  void secureBundleRequiresOneExactConsumerMappingProfile() {
    ManagedDependencyCommand hdfs = ManagedDependencyCommand.prepareConsumer(
        secureSnapshot(ManagedDependencyType.HDFS, BINDING_ID, false),
        OPERATION_ID, 9, 42, "hadoop_3_3_0_0_1-client", "3.3.0", IDENTITY_HASH);
    UUID zooKeeperBinding = UUID.fromString("997f43e1-9af8-4c74-97fb-1bb4d7a7f839");
    ManagedDependencyCommand zooKeeper = ManagedDependencyCommand.prepareConsumer(
        secureSnapshot(ManagedDependencyType.ZOOKEEPER, zooKeeperBinding, false),
        UUID.fromString("2d84a326-ceb9-420a-aa53-0a963ac63db0"), 9, 42,
        "hbase_3_3_0_0_1", "2.4.13", IDENTITY_HASH);

    ManagedDependencyCommandBundle.of(
        42, "hbase_mc_c1", IDENTITY_HASH, List.of(hdfs, zooKeeper));

    ManagedDependencyCommand changedZooKeeper = ManagedDependencyCommand.prepareConsumer(
        secureSnapshot(ManagedDependencyType.ZOOKEEPER, zooKeeperBinding, true),
        UUID.fromString("5e4ee70c-8781-43bb-90c5-0c873dbe1d7c"), 10, 42,
        "hbase_3_3_0_0_1", "2.4.13", IDENTITY_HASH);
    assertThrows(IllegalArgumentException.class, () -> ManagedDependencyCommandBundle.of(
        42, "hbase_mc_c1", IDENTITY_HASH, List.of(hdfs, changedZooKeeper)));
  }

  @Test
  void preparationObservationContainsMeasuredFactsAndVerifyBindsItsLineage() {
    ManagedDependencySnapshot snapshot = snapshot(ManagedDependencyType.HDFS);
    ManagedDependencyCommand prepare = ManagedDependencyCommand.prepareConsumer(
        snapshot, OPERATION_ID, 9, 42, "hadoop_3_3_0_0_1-client", "3.3.0",
        IDENTITY_HASH);
    Result observation = new Result(prepare.name(), prepare.envelope(), ResultStatus.SUCCEEDED,
        new TreeMap<>(Map.of(
            "applied.snapshot.fingerprint", prepare.parameters().get("snapshot.fingerprint"),
            "client.config.fingerprint", prepare.parameters().get("client.config.fingerprint"),
            "client.package.name", "hadoop_3_3_0_0_1-client",
            "client.package.version", "3.3.0-1.el8",
            "client.software.kind", "HADOOP_CLIENT",
            "client.software.semantic.version", "3.3.0",
            "consumer.user", "hbase_mc_c1",
            "host.id", "42",
            "identity.fingerprint", IDENTITY_HASH)), null, null);

    prepare.validateResult(observation);
    ManagedDependencyCommand verify = ManagedDependencyCommand.verifyConsumer(
        snapshot, UUID.fromString("8f4b458d-291a-4d92-9e0c-d160d815a843"), 9, 42,
        "hadoop_3_3_0_0_1-client", "3.3.0-1.el8", "3.3.0", IDENTITY_HASH,
        PREPARATION_OBSERVATION_ID, prepare.envelope().immutableRequestHash(),
        PREPARATION_OBSERVATION_HASH);
    assertEquals(prepare.envelope().immutableRequestHash(),
        verify.parameters().get("preparation.request.hash"));
    assertEquals(PREPARATION_OBSERVATION_ID.toString(),
        verify.parameters().get("preparation.observation.id"));
    assertThrows(IllegalArgumentException.class, () -> {
      TreeMap<String, String> changed = new TreeMap<>(prepare.parameters());
      changed.put("client.software.semantic.version", "3.3.1");
      ManagedDependencyCommand changedPreparation = rehash(prepare, changed);
      Result mismatchedMeasurement = new Result(changedPreparation.name(),
          changedPreparation.envelope(), ResultStatus.SUCCEEDED,
          new TreeMap<>(observation.facts()), null, null);
      changedPreparation.validateResult(mismatchedMeasurement);
    });
  }

  private ManagedDependencyCommand verifyConsumer(ManagedDependencySnapshot snapshot,
      UUID operationId, long epoch, long hostId, String packageName, String packageVersion,
      String identityFingerprint) {
    String softwareVersion = snapshot.type() == ManagedDependencyType.HDFS ? "3.3.0" : "2.4.13";
    ManagedDependencyCommand preparation = ManagedDependencyCommand.prepareConsumer(
        snapshot, operationId, epoch, hostId, packageName, softwareVersion,
        identityFingerprint);
    return ManagedDependencyCommand.verifyConsumer(snapshot, operationId, epoch, hostId,
        packageName, packageVersion, softwareVersion, identityFingerprint,
        PREPARATION_OBSERVATION_ID, preparation.envelope().immutableRequestHash(),
        PREPARATION_OBSERVATION_HASH);
  }

  private ManagedDependencyCommand rehash(ManagedDependencyCommand command,
      TreeMap<String, String> parameters) {
    Envelope unhashed = new Envelope(command.envelope().protocolVersion(),
        command.envelope().bindingId(), command.envelope().operationId(),
        command.envelope().epoch(), command.envelope().snapshotVersion(),
        "sha256:" + "0".repeat(64));
    String hash = ManagedDependencyCommand.requestHash(command.name(), unhashed, parameters);
    return new ManagedDependencyCommand(command.name(), new Envelope(
        unhashed.protocolVersion(), unhashed.bindingId(), unhashed.operationId(),
        unhashed.epoch(), unhashed.snapshotVersion(), hash), parameters);
  }

  private ManagedDependencySnapshot snapshot(ManagedDependencyType type) {
    return snapshot(type, BINDING_ID);
  }

  private ManagedDependencySnapshot snapshot(ManagedDependencyType type, UUID bindingId) {
    String snapshotHash = "sha256:2bff3497f87dd9a5f2cf9049c3dba3f4caa589e49ecfead1fff799790853fc15";
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, type.getProviderServiceName() + "-3.3.0",
        new TreeMap<>(Map.of("repository.version", "3.3.0-1")),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 18L, List.of("https://mirror.example/repo"));
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_c1", new TreeSet<>(), false, "hbase_mc_c1", true, "0700", false);
    ManagedDependencyNamespace namespace = type == ManagedDependencyType.HDFS
        ? ManagedDependencyNamespace.hdfs(bindingId, "hdfs://nn.example.test:8020")
        : ManagedDependencyNamespace.zooKeeper(bindingId);
    return new ManagedDependencySnapshot(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        bindingId, 7, type, new ManagedDependencyServiceKey(2L, type.getProviderServiceName()),
        namespace, version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        type == ManagedDependencyType.HDFS
            ? new TreeMap<>(Map.of("fs.defaultFS", "hdfs://nn.example.test:8020"))
            : new TreeMap<>(),
        new TreeMap<>(), type == ManagedDependencyType.ZOOKEEPER
            ? new TreeMap<>(Map.of("hbase.zookeeper.property.clientPort", "2181",
                "hbase.zookeeper.quorum", "zk1.example.test"))
            : new TreeMap<>(),
        CONSUMER_HASH, PROVIDER_HASH, snapshotHash);
  }

  private ManagedDependencySnapshot secureZooKeeperSnapshot() {
    return secureSnapshot(ManagedDependencyType.ZOOKEEPER, BINDING_ID, false);
  }

  private ManagedDependencySnapshot secureHdfsSnapshot() {
    return secureSnapshot(ManagedDependencyType.HDFS, BINDING_ID, false);
  }

  ManagedDependencySnapshot secureSnapshot(ManagedDependencyType type,
      UUID bindingId, boolean includeUnrelatedRule) {
    ManagedDependencySnapshot insecure = snapshot(type, bindingId);
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_c1", new TreeSet<>(Set.of("hbase_mc_c1/_HOST@EXAMPLE.COM")),
        false, "hbase_mc_c1", true, "0700", type == ManagedDependencyType.ZOOKEEPER);
    TreeMap<String, String> coreSite = new TreeMap<>(insecure.coreSite());
    TreeMap<String, String> hdfsSite = new TreeMap<>();
    TreeMap<String, String> zooKeeperClient = new TreeMap<>(insecure.zooKeeperClient());
    if (type == ManagedDependencyType.HDFS) {
      coreSite.put("hadoop.security.authentication", "kerberos");
      coreSite.put("ipc.client.fallback-to-simple-auth-allowed", "false");
      hdfsSite.put("dfs.client.socket-timeout", "60000");
      hdfsSite.put("dfs.datanode.kerberos.principal", "dn/_HOST@EXAMPLE.COM");
      hdfsSite.put("dfs.namenode.kerberos.principal", "nn/_HOST@EXAMPLE.COM");
    } else {
      zooKeeperClient.put("zookeeper.sasl.client", "true");
      zooKeeperClient.put("zookeeper.sasl.client.username", "zookeeper");
      zooKeeperClient.put("zookeeper.sasl.clientconfig", "Client");
      zooKeeperClient.put("hbase.zookeeper.property.clientPort", "2181");
      zooKeeperClient.put("zookeeper.znode.parent",
          ManagedDependencyNamespace.zooKeeper(bindingId).znode());
    }
    ManagedHBaseConsumerLocalMapping mapping = consumerMapping(
        type, bindingId, includeUnrelatedRule);
    String policyFingerprint = "sha256:"
        + (type == ManagedDependencyType.HDFS ? "6" : "7").repeat(64);
    ManagedDependencyProviderSecurityProof providerProof =
        new ManagedDependencyProviderSecurityProof(
            ManagedDependencyProviderSecurityProof.SCHEMA_VERSION, type,
            type == ManagedDependencyType.HDFS
                ? ManagedDependencySecurityPolicyKind.HDFS_AUTH_TO_LOCAL
                : ManagedDependencySecurityPolicyKind.ZOOKEEPER_SASL_ID,
            "EXAMPLE.COM", policyFingerprint);
    String principalFingerprint = type == ManagedDependencyType.HDFS
        ? new ManagedHdfsAuthToLocalVerifier().fingerprintConsumerPattern(
            new ConsumerPatternInput("EXAMPLE.COM", "hbase_mc_c1",
                "hbase_mc_c1/_HOST@EXAMPLE.COM"))
        : new ManagedZooKeeperSaslPolicyVerifier().fingerprintConsumer(
            new ConsumerInput("EXAMPLE.COM", "hbase_mc_c1",
                "hbase_mc_c1/_HOST@EXAMPLE.COM", "hbase_mc_c1"));
    ManagedDependencyPairSecurityProof pairProof = new ManagedDependencyPairSecurityProof(
        ManagedDependencyPairSecurityProof.SCHEMA_VERSION, type, providerProof.kind(),
        policyFingerprint, principalFingerprint, "hbase_mc_c1", "sha256:" + "8".repeat(64));
    return new ManagedDependencySnapshot(insecure.schemaVersion(), insecure.bindingId(),
        insecure.snapshotVersion(), insecure.type(), insecure.providerService(), insecure.namespace(),
        insecure.providerVersion(), ManagedDependencySecurityMode.KERBEROS, identity,
        coreSite, hdfsSite, zooKeeperClient, insecure.consumerFingerprint(),
        insecure.providerFingerprint(), insecure.snapshotFingerprint(), mapping,
        providerProof, pairProof);
  }

  private ManagedHBaseConsumerLocalMapping consumerMapping(ManagedDependencyType type,
      UUID bindingId, boolean includeUnrelatedRule) {
    String user = "hbase_mc_c1";
    String smokePrincipal = "ambari-qa-plan@EXAMPLE.COM";
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", "", false);
    builder.addRule(user + "/_HOST@EXAMPLE.COM", user);
    builder.addRule(user + "@EXAMPLE.COM", user);
    builder.addRule(smokePrincipal, "ambari-qa");
    if (includeUnrelatedRule) {
      builder.addRule("auxiliary@EXAMPLE.COM", "auxiliary");
    }
    String rules = builder.generate();
    ManagedHdfsAuthToLocalVerifier verifier = new ManagedHdfsAuthToLocalVerifier();
    ConsumerLocalMappingProof proof = verifier.proveConsumerLocalMappings(rules,
        new ConsumerLocalMappingInput("EXAMPLE.COM", user, user + "/_HOST@EXAMPLE.COM",
            user + "@EXAMPLE.COM", smokePrincipal, "ambari-qa"));
    TreeMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings = new TreeMap<>();
    bindings.put(type, new ManagedBindingSnapshotRef(type, bindingId));
    ManagedHBaseKerberosOverlaySpec spec = ManagedHBaseKerberosOverlaySpec.create(
        "EXAMPLE.COM", user, smokePrincipal, "ambari-qa",
        Plan.forExistingCluster(1L).planFingerprint(), bindings);
    return ManagedHBaseConsumerLocalMapping.create(spec,
        new SealedConfigurations(Map.of(
            "core-site", Map.of("hadoop.security.auth_to_local", rules)), proof));
  }
}
