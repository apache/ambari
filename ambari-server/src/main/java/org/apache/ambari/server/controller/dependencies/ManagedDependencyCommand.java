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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Strict protocol DTO for server-reserved managed-dependency custom actions. */
public record ManagedDependencyCommand(
    CommandName name,
    Envelope envelope,
    SortedMap<String, String> parameters) {
  public static final int CURRENT_PROTOCOL_VERSION = 1;
  private static final int MAX_CLIENT_CONFIG_BYTES = 32 * 1024;
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  private static final Pattern POSITIVE_INTEGER = Pattern.compile("[1-9][0-9]*");
  private static final Pattern PACKAGE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9+._~-]{0,127}");
  private static final Pattern SOFTWARE_VERSION = Pattern.compile(
      "[0-9]+(?:\\.[0-9]+){1,3}(?:[-+][A-Za-z0-9][A-Za-z0-9._-]*)?");
  private static final Pattern IDENTITY = Pattern.compile("[A-Za-z_][A-Za-z0-9._-]{0,127}");
  private static final Pattern KERBEROS_REALM = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,254}");
  private static final Pattern HA_TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9.-]*");
  private static final String STANDARD_HA_PROVIDER =
      "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider";
  private static final String AUTH_TO_LOCAL = "hadoop.security.auth_to_local";
  private static final Set<String> COMMON = Set.of(
      "provider.cluster.id", "provider.fingerprint", "provider.service",
      "security.mode", "snapshot.fingerprint");
  private static final Set<String> ACTION_HOST = Set.of("provider.action.host.id");
  private static final Set<String> CONSUMER_SECURITY_PROOFS = Set.of(
      "consumer.mapping.profile.fingerprint", "consumer.mapping.proof.fingerprint",
      "consumer.mapping.rules.fingerprint", "provider.security.pair.proof.fingerprint",
      "provider.security.policy.fingerprint");
  private static final Set<String> ZOOKEEPER_INSECURE_SUCCESS_FACTS = Set.of(
      "applied.snapshot.fingerprint", "namespace.znode.exists", "owner.user",
      "parent.acl.policy", "provider.action.host.id", "subtree.acl.policy");
  private static final Set<String> ZOOKEEPER_SECURE_SUCCESS_FACTS = Set.of(
      "applied.snapshot.fingerprint", "consumer.zk.sasl.id", "namespace.container.znode",
      "namespace.ledger.znode", "namespace.znode", "provider.action.host.id",
      "provider.handoff.acknowledged");
  private static final Set<String> ZOOKEEPER_RECONCILIATION_FACTS = Set.of(
      "applied.snapshot.fingerprint", "consumer.zk.sasl.id", "namespace.container.znode",
      "namespace.ledger.znode", "namespace.znode", "provider.action.host.id",
      "provider.handoff.reconciliation.required");
  private static final Set<String> HDFS_CONSUMER_SUCCESS_FACTS = Set.of(
      "applied.snapshot.fingerprint", "client.config.fingerprint", "client.package.name",
      "client.package.version", "client.software.kind", "client.software.semantic.version",
      "datanode.read.write.verified", "host.id", "identity.fingerprint",
      "namenode.rpc.connected");
  private static final Set<String> ZOOKEEPER_CONSUMER_SUCCESS_FACTS = Set.of(
      "applied.snapshot.fingerprint", "client.config.fingerprint", "client.package.name",
      "client.package.version", "client.software.kind", "client.software.semantic.version",
      "host.id", "identity.fingerprint", "private.znode.verified", "zookeeper.quorum.connected");
  private static final Set<String> ZOOKEEPER_SECURE_CONSUMER_SUCCESS_FACTS = Set.of(
      "applied.snapshot.fingerprint", "client.config.fingerprint", "client.package.name",
      "client.package.version", "client.software.kind", "client.software.semantic.version",
      "consumer.kerberos.principal", "consumer.zk.sasl.id", "container.acl.verified",
      "hbase.znode.verified", "host.id", "identity.fingerprint", "private.znode.verified",
      "sibling.authority.denied", "zookeeper.quorum.connected");
  private static final Set<String> CONSUMER_PREPARATION_SUCCESS_FACTS = Set.of(
      "applied.snapshot.fingerprint", "client.config.fingerprint", "client.package.name",
      "client.package.version", "client.software.kind", "client.software.semantic.version",
      "consumer.user", "host.id", "identity.fingerprint");
  private static final Map<CommandName, Set<String>> REQUIRED_PARAMETERS;
  private static final Map<CommandName, Set<String>> OPTIONAL_PARAMETERS;
  private static final Map<CommandName, Set<String>> ALLOWED_RESULT_FACTS;
  private static final Map<CommandName, Set<String>> REQUIRED_SUCCESS_FACTS;

  static {
    EnumMap<CommandName, Set<String>> required = new EnumMap<>(CommandName.class);
    required.put(CommandName.PREPARE_BINDING_JOURNAL, union(COMMON, ACTION_HOST));
    required.put(CommandName.INITIALIZE_BINDING_JOURNAL, union(COMMON, ACTION_HOST, Set.of(
        "initialization.authorization.id", "initialization.challenge",
        "initialization.prepare.request.hash")));
    required.put(CommandName.PROVISION_HDFS_NAMESPACE, union(COMMON, ACTION_HOST, Set.of(
        "namespace.root.uri", "namespace.wal.uri", "owner.user", "owner.group", "directory.mode")));
    required.put(CommandName.PROVISION_ZOOKEEPER_NAMESPACE, union(COMMON, ACTION_HOST, Set.of(
        "expected.client.port", "expected.quorum", "namespace.container.znode",
        "namespace.parent.znode", "namespace.znode",
        "owner.user", "parent.acl.policy", "subtree.acl.policy")));
    required.put(CommandName.INVALIDATE_BINDING_EPOCH, union(COMMON, ACTION_HOST));
    Set<String> consumerPreparation = Set.of(
        "host.id", "client.package.name", "client.software.kind",
        "client.software.semantic.version", "client.config.json", "client.config.fingerprint",
        "consumer.user", "identity.fingerprint");
    Set<String> strictVerification = Set.of(
        "client.package.version", "preparation.observation.fingerprint",
        "preparation.observation.id", "preparation.request.hash");
    Set<String> hdfsConsumer = Set.of(
        "expected.directory.mode", "expected.owner.group",
        "expected.default.fs", "expected.namespace.root.uri", "expected.namespace.wal.uri");
    Set<String> zooKeeperConsumer = Set.of(
        "expected.namespace.container.znode", "expected.namespace.parent.znode",
        "expected.namespace.znode");
    required.put(CommandName.PREPARE_HDFS_CONSUMER,
        union(COMMON, consumerPreparation, hdfsConsumer));
    required.put(CommandName.PREPARE_ZOOKEEPER_CONSUMER,
        union(COMMON, consumerPreparation, zooKeeperConsumer));
    required.put(CommandName.VERIFY_HDFS_CONSUMER,
        union(COMMON, consumerPreparation, strictVerification, hdfsConsumer));
    required.put(CommandName.VERIFY_ZOOKEEPER_CONSUMER,
        union(COMMON, consumerPreparation, strictVerification, zooKeeperConsumer));
    REQUIRED_PARAMETERS = Collections.unmodifiableMap(required);

    EnumMap<CommandName, Set<String>> optional = new EnumMap<>(CommandName.class);
    optional.put(CommandName.PREPARE_BINDING_JOURNAL, Set.of());
    optional.put(CommandName.INITIALIZE_BINDING_JOURNAL, Set.of());
    optional.put(CommandName.PROVISION_HDFS_NAMESPACE, Set.of());
    optional.put(CommandName.PROVISION_ZOOKEEPER_NAMESPACE, Set.of(
        "consumer.zk.sasl.id", "namespace.ledger.znode"));
    optional.put(CommandName.INVALIDATE_BINDING_EPOCH, Set.of());
    optional.put(CommandName.PREPARE_HDFS_CONSUMER, union(
        CONSUMER_SECURITY_PROOFS, Set.of("consumer.kerberos.principal.pattern")));
    optional.put(CommandName.PREPARE_ZOOKEEPER_CONSUMER, union(
        CONSUMER_SECURITY_PROOFS,
        Set.of("consumer.kerberos.principal.pattern", "expected.consumer.zk.sasl.id")));
    optional.put(CommandName.VERIFY_HDFS_CONSUMER, union(
        CONSUMER_SECURITY_PROOFS, Set.of("consumer.kerberos.principal.pattern")));
    optional.put(CommandName.VERIFY_ZOOKEEPER_CONSUMER, union(
        CONSUMER_SECURITY_PROOFS,
        Set.of("consumer.kerberos.principal.pattern", "expected.consumer.zk.sasl.id")));
    OPTIONAL_PARAMETERS = Collections.unmodifiableMap(optional);

    EnumMap<CommandName, Set<String>> facts = new EnumMap<>(CommandName.class);
    facts.put(CommandName.PREPARE_BINDING_JOURNAL, Set.of("initialization.challenge"));
    facts.put(CommandName.INITIALIZE_BINDING_JOURNAL, Set.of(
        "initialization.authorization.id", "journal.initialized", "provider.action.host.id"));
    facts.put(CommandName.PROVISION_HDFS_NAMESPACE, Set.of(
        "applied.snapshot.fingerprint", "directory.mode", "namespace.root.exists",
        "namespace.wal.exists", "owner.group", "owner.user", "provider.action.host.id"));
    facts.put(CommandName.PROVISION_ZOOKEEPER_NAMESPACE, union(
        ZOOKEEPER_INSECURE_SUCCESS_FACTS, ZOOKEEPER_SECURE_SUCCESS_FACTS,
        ZOOKEEPER_RECONCILIATION_FACTS));
    facts.put(CommandName.INVALIDATE_BINDING_EPOCH, Set.of(
        "highest.accepted.epoch", "provider.action.host.id"));
    facts.put(CommandName.PREPARE_HDFS_CONSUMER, CONSUMER_PREPARATION_SUCCESS_FACTS);
    facts.put(CommandName.PREPARE_ZOOKEEPER_CONSUMER, CONSUMER_PREPARATION_SUCCESS_FACTS);
    facts.put(CommandName.VERIFY_HDFS_CONSUMER, union(
        HDFS_CONSUMER_SUCCESS_FACTS, Set.of("consumer.kerberos.principal")));
    facts.put(CommandName.VERIFY_ZOOKEEPER_CONSUMER, union(
        ZOOKEEPER_CONSUMER_SUCCESS_FACTS, ZOOKEEPER_SECURE_CONSUMER_SUCCESS_FACTS));
    ALLOWED_RESULT_FACTS = Collections.unmodifiableMap(facts);
    EnumMap<CommandName, Set<String>> requiredFacts = new EnumMap<>(facts);
    requiredFacts.put(CommandName.PROVISION_ZOOKEEPER_NAMESPACE, Set.of(
        "applied.snapshot.fingerprint", "provider.action.host.id"));
    requiredFacts.put(CommandName.PREPARE_HDFS_CONSUMER, CONSUMER_PREPARATION_SUCCESS_FACTS);
    requiredFacts.put(CommandName.PREPARE_ZOOKEEPER_CONSUMER, CONSUMER_PREPARATION_SUCCESS_FACTS);
    requiredFacts.put(CommandName.VERIFY_HDFS_CONSUMER, HDFS_CONSUMER_SUCCESS_FACTS);
    requiredFacts.put(CommandName.VERIFY_ZOOKEEPER_CONSUMER, ZOOKEEPER_CONSUMER_SUCCESS_FACTS);
    REQUIRED_SUCCESS_FACTS = Collections.unmodifiableMap(requiredFacts);
  }

  public ManagedDependencyCommand {
    name = Objects.requireNonNull(name, "name");
    envelope = Objects.requireNonNull(envelope, "envelope");
    parameters = immutable(parameters);
    Set<String> required = REQUIRED_PARAMETERS.get(name);
    Set<String> allowed = union(required, OPTIONAL_PARAMETERS.get(name));
    if (!parameters.keySet().containsAll(required) || !allowed.containsAll(parameters.keySet())) {
      throw invalid("Command parameters contain missing or unknown fields for " + name);
    }
    validateParameters(name, envelope, parameters);
    if (!envelope.immutableRequestHash().equals(requestHash(name, envelope, parameters))) {
      throw invalid("immutableRequestHash does not match the command envelope and parameters");
    }
  }

  public static ManagedDependencyCommand prepareJournal(ManagedDependencySnapshot snapshot,
      UUID operationId, long epoch, long actionHostId) {
    return create(CommandName.PREPARE_BINDING_JOURNAL, snapshot, operationId, epoch,
        providerParameters(snapshot, actionHostId));
  }

  public static ManagedDependencyCommand initializeJournal(ManagedDependencySnapshot snapshot,
      UUID operationId, long epoch, long actionHostId, UUID challenge,
      UUID initializationAuthorizationId, String prepareRequestHash) {
    TreeMap<String, String> parameters = providerParameters(snapshot, actionHostId);
    parameters.put("initialization.authorization.id",
        Objects.requireNonNull(initializationAuthorizationId, "initializationAuthorizationId").toString());
    parameters.put("initialization.challenge", Objects.requireNonNull(challenge, "challenge").toString());
    parameters.put("initialization.prepare.request.hash",
        requireHash(prepareRequestHash, "prepareRequestHash"));
    return create(CommandName.INITIALIZE_BINDING_JOURNAL, snapshot, operationId, epoch, parameters);
  }

  public static ManagedDependencyCommand provision(ManagedDependencySnapshot snapshot,
      UUID operationId, long epoch, long actionHostId) {
    Objects.requireNonNull(snapshot, "snapshot");
    TreeMap<String, String> parameters = providerParameters(snapshot, actionHostId);
    CommandName name;
    if (snapshot.type() == ManagedDependencyType.HDFS) {
      name = CommandName.PROVISION_HDFS_NAMESPACE;
      ManagedDependencyIdentity identity = Objects.requireNonNull(snapshot.consumerIdentity(), "consumerIdentity");
      parameters.put("namespace.root.uri", snapshot.namespace().rootUri());
      parameters.put("namespace.wal.uri", snapshot.namespace().walUri());
      parameters.put("owner.user", identity.effectiveShortUser());
      parameters.put("owner.group", identity.filesystemGroup());
      parameters.put("directory.mode", identity.directoryMode());
    } else {
      name = CommandName.PROVISION_ZOOKEEPER_NAMESPACE;
      ManagedDependencyIdentity identity = Objects.requireNonNull(snapshot.consumerIdentity(), "consumerIdentity");
      parameters.put("namespace.znode", snapshot.namespace().znode());
      parameters.put("namespace.container.znode",
          ManagedDependencyNamespace.zooKeeperContainer(snapshot.bindingId()));
      parameters.put("namespace.parent.znode", ManagedDependencyNamespace.ZOOKEEPER_PARENT);
      parameters.put("expected.client.port",
          snapshot.zooKeeperClient().get("hbase.zookeeper.property.clientPort"));
      parameters.put("expected.quorum", snapshot.zooKeeperClient().get("hbase.zookeeper.quorum"));
      parameters.put("owner.user", identity.effectiveShortUser());
      parameters.put("parent.acl.policy", snapshot.securityMode() == ManagedDependencySecurityMode.KERBEROS
          ? "PROVIDER_ADMIN_CREATE_DELETE" : "INSECURE_PROVIDER_PREPARED");
      parameters.put("subtree.acl.policy", snapshot.securityMode() == ManagedDependencySecurityMode.KERBEROS
          ? "SASL_CONSUMER_PRIVATE" : "INSECURE_BINDING_SCOPED");
      if (snapshot.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
        parameters.put("consumer.zk.sasl.id", identity.effectiveShortUser());
        parameters.put("namespace.ledger.znode",
            ManagedDependencyNamespace.zooKeeperLedger(snapshot.bindingId()));
      }
    }
    return create(name, snapshot, operationId, epoch, parameters);
  }

  public static ManagedDependencyCommand prepareConsumer(ManagedDependencySnapshot snapshot,
      UUID operationId, long epoch, long hostId, String packageName,
      String clientSoftwareVersion, String identityFingerprint) {
    TreeMap<String, String> parameters = consumerParameters(snapshot, hostId, packageName,
        clientSoftwareVersion, identityFingerprint);
    CommandName name = snapshot.type() == ManagedDependencyType.HDFS
        ? CommandName.PREPARE_HDFS_CONSUMER : CommandName.PREPARE_ZOOKEEPER_CONSUMER;
    return create(name, snapshot, operationId, epoch, parameters);
  }

  public static ManagedDependencyCommand verifyConsumer(ManagedDependencySnapshot snapshot,
      UUID operationId, long epoch, long hostId, String packageName, String packageVersion,
      String clientSoftwareVersion, String identityFingerprint, UUID preparationObservationId,
      String preparationRequestHash, String preparationObservationFingerprint) {
    TreeMap<String, String> parameters = consumerParameters(snapshot, hostId, packageName,
        clientSoftwareVersion, identityFingerprint);
    parameters.put("client.package.version",
        requirePackageVersion(packageVersion, "client.package.version"));
    parameters.put("preparation.observation.id",
        Objects.requireNonNull(preparationObservationId, "preparationObservationId").toString());
    parameters.put("preparation.request.hash",
        requireHash(preparationRequestHash, "preparationRequestHash"));
    parameters.put("preparation.observation.fingerprint",
        requireHash(preparationObservationFingerprint, "preparationObservationFingerprint"));
    CommandName name = snapshot.type() == ManagedDependencyType.HDFS
        ? CommandName.VERIFY_HDFS_CONSUMER : CommandName.VERIFY_ZOOKEEPER_CONSUMER;
    return create(name, snapshot, operationId, epoch, parameters);
  }

  private static TreeMap<String, String> consumerParameters(ManagedDependencySnapshot snapshot,
      long hostId, String packageName, String clientSoftwareVersion,
      String identityFingerprint) {
    if (hostId <= 0) {
      throw invalid("hostId must be positive");
    }
    TreeMap<String, String> parameters = commonParameters(snapshot);
    parameters.put("host.id", Long.toString(hostId));
    parameters.put("client.package.name", requirePackageName(packageName));
    parameters.put("client.software.kind", snapshot.type() == ManagedDependencyType.HDFS
        ? "HADOOP_CLIENT" : "HBASE_CLIENT");
    parameters.put("client.software.semantic.version",
        requireSoftwareVersion(clientSoftwareVersion));
    parameters.put("consumer.user", snapshot.consumerIdentity().effectiveShortUser());
    parameters.put("identity.fingerprint", requireHash(identityFingerprint, "identityFingerprint"));
    if (snapshot.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
      parameters.put("consumer.kerberos.principal.pattern", securePrincipalPattern(snapshot));
      ManagedHBaseConsumerLocalMapping mapping = Objects.requireNonNull(
          snapshot.consumerLocalMapping(), "consumerLocalMapping");
      ManagedDependencyProviderSecurityProof providerSecurity = Objects.requireNonNull(
          snapshot.providerSecurity(), "providerSecurity");
      ManagedDependencyPairSecurityProof pairSecurity = Objects.requireNonNull(
          snapshot.pairSecurity(), "pairSecurity");
      parameters.put("consumer.mapping.profile.fingerprint", mapping.profileFingerprint());
      parameters.put("consumer.mapping.proof.fingerprint", mapping.proof().proofFingerprint());
      parameters.put("consumer.mapping.rules.fingerprint", mapping.proof().rulesFingerprint());
      parameters.put("provider.security.policy.fingerprint",
          providerSecurity.policyFingerprint());
      parameters.put("provider.security.pair.proof.fingerprint",
          pairSecurity.proofFingerprint());
    }
    if (snapshot.type() == ManagedDependencyType.HDFS) {
      parameters.put("expected.default.fs", snapshot.coreSite().get("fs.defaultFS"));
      parameters.put("expected.directory.mode", snapshot.consumerIdentity().directoryMode());
      parameters.put("expected.namespace.root.uri", snapshot.namespace().rootUri());
      parameters.put("expected.namespace.wal.uri", snapshot.namespace().walUri());
      parameters.put("expected.owner.group", snapshot.consumerIdentity().filesystemGroup());
      TreeMap<String, String> coreSite = new TreeMap<>(snapshot.coreSite());
      if (snapshot.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
        coreSite.put(AUTH_TO_LOCAL, snapshot.consumerLocalMapping().canonicalRules());
      }
      String configJson = clientConfigJson(
          coreSite, snapshot.hdfsSite(), Collections.emptySortedMap());
      parameters.put("client.config.json", configJson);
      parameters.put("client.config.fingerprint", sha256(configJson));
    } else {
      parameters.put("expected.namespace.znode", snapshot.namespace().znode());
      parameters.put("expected.namespace.container.znode",
          ManagedDependencyNamespace.zooKeeperContainer(snapshot.bindingId()));
      parameters.put("expected.namespace.parent.znode", ManagedDependencyNamespace.ZOOKEEPER_PARENT);
      if (snapshot.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
        parameters.put("expected.consumer.zk.sasl.id",
            snapshot.consumerIdentity().effectiveShortUser());
      }
      TreeMap<String, String> zooKeeperClient = new TreeMap<>(snapshot.zooKeeperClient());
      zooKeeperClient.put("zookeeper.znode.parent", snapshot.namespace().znode());
      String configJson = clientConfigJson(
          Collections.emptySortedMap(), Collections.emptySortedMap(), zooKeeperClient);
      parameters.put("client.config.json", configJson);
      parameters.put("client.config.fingerprint", sha256(configJson));
    }
    return parameters;
  }

  public static ManagedDependencyCommand invalidate(ManagedDependencySnapshot snapshot,
      UUID operationId, long epoch, long actionHostId) {
    return create(CommandName.INVALIDATE_BINDING_EPOCH, snapshot, operationId, epoch,
        providerParameters(snapshot, actionHostId));
  }

  private static ManagedDependencyCommand create(CommandName name, ManagedDependencySnapshot snapshot,
      UUID operationId, long epoch, SortedMap<String, String> parameters) {
    Objects.requireNonNull(snapshot, "snapshot");
    Envelope unhashed = new Envelope(CURRENT_PROTOCOL_VERSION, snapshot.bindingId(), operationId,
        epoch, snapshot.snapshotVersion(), "sha256:" + "0".repeat(64));
    Envelope envelope = new Envelope(unhashed.protocolVersion(), unhashed.bindingId(),
        unhashed.operationId(), unhashed.epoch(), unhashed.snapshotVersion(),
        requestHash(name, unhashed, parameters));
    return new ManagedDependencyCommand(name, envelope, parameters);
  }

  private static TreeMap<String, String> commonParameters(ManagedDependencySnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    TreeMap<String, String> parameters = new TreeMap<>();
    parameters.put("provider.cluster.id", Long.toString(snapshot.providerService().clusterId()));
    parameters.put("provider.fingerprint", snapshot.providerFingerprint());
    parameters.put("provider.service", snapshot.providerService().serviceName());
    parameters.put("security.mode", snapshot.securityMode().name());
    parameters.put("snapshot.fingerprint", snapshot.snapshotFingerprint());
    return parameters;
  }

  private static TreeMap<String, String> providerParameters(ManagedDependencySnapshot snapshot,
      long actionHostId) {
    if (actionHostId <= 0) {
      throw invalid("actionHostId must be positive");
    }
    TreeMap<String, String> parameters = commonParameters(snapshot);
    parameters.put("provider.action.host.id", Long.toString(actionHostId));
    return parameters;
  }

  static String requestHash(CommandName name, Envelope envelope, Map<String, String> parameters) {
    StringBuilder canonical = new StringBuilder();
    append(canonical, name.name(), Integer.toString(envelope.protocolVersion()),
        envelope.bindingId().toString(), envelope.operationId().toString(),
        Long.toString(envelope.epoch()), Long.toString(envelope.snapshotVersion()));
    parameters.forEach((key, value) -> append(canonical, key, value));
    return sha256(canonical.toString());
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

  private static void validateParameters(CommandName name, Envelope envelope,
      Map<String, String> parameters) {
    parsePositiveLong(parameters.get("provider.cluster.id"), "provider.cluster.id");
    requireHash(parameters.get("provider.fingerprint"), "provider.fingerprint");
    requireHash(parameters.get("snapshot.fingerprint"), "snapshot.fingerprint");
    try {
      ManagedDependencySecurityMode.valueOf(parameters.get("security.mode"));
    } catch (RuntimeException e) {
      throw invalid("security.mode is unknown");
    }
    if (isProviderCommand(name)) {
      parsePositiveLong(parameters.get("provider.action.host.id"), "provider.action.host.id");
    }
    if (name == CommandName.PREPARE_BINDING_JOURNAL) {
      return;
    }
    if (name == CommandName.INITIALIZE_BINDING_JOURNAL) {
      requireCanonicalUuid(parameters.get("initialization.authorization.id"),
          "initialization.authorization.id");
      requireCanonicalUuid(parameters.get("initialization.challenge"), "initialization.challenge");
      requireHash(parameters.get("initialization.prepare.request.hash"),
          "initialization.prepare.request.hash");
      return;
    }
    if (name == CommandName.INVALIDATE_BINDING_EPOCH) {
      if (!Set.of("HDFS", "ZOOKEEPER").contains(parameters.get("provider.service"))) {
        throw invalid("provider.service must identify a reserved managed dependency service");
      }
    } else {
      String expectedService = name == CommandName.PROVISION_HDFS_NAMESPACE
          || name == CommandName.PREPARE_HDFS_CONSUMER
          || name == CommandName.VERIFY_HDFS_CONSUMER ? "HDFS" : "ZOOKEEPER";
      if (!expectedService.equals(parameters.get("provider.service"))) {
        throw invalid("provider.service does not match the reserved command");
      }
    }
    if (name == CommandName.PROVISION_HDFS_NAMESPACE) {
      validateHdfsProvision(envelope, parameters);
    } else if (name == CommandName.PROVISION_ZOOKEEPER_NAMESPACE) {
      validateZooKeeperProvision(envelope, parameters);
    } else if (name == CommandName.PREPARE_HDFS_CONSUMER
        || name == CommandName.PREPARE_ZOOKEEPER_CONSUMER
        || name == CommandName.VERIFY_HDFS_CONSUMER
        || name == CommandName.VERIFY_ZOOKEEPER_CONSUMER) {
      boolean hdfs = name == CommandName.PREPARE_HDFS_CONSUMER
          || name == CommandName.VERIFY_HDFS_CONSUMER;
      boolean verification = name == CommandName.VERIFY_HDFS_CONSUMER
          || name == CommandName.VERIFY_ZOOKEEPER_CONSUMER;
      parsePositiveLong(parameters.get("host.id"), "host.id");
      requirePackageName(parameters.get("client.package.name"));
      String expectedSoftwareKind = hdfs ? "HADOOP_CLIENT" : "HBASE_CLIENT";
      if (!expectedSoftwareKind.equals(parameters.get("client.software.kind"))) {
        throw invalid("client.software.kind does not match the managed dependency type");
      }
      requireSoftwareVersion(parameters.get("client.software.semantic.version"));
      requireHash(parameters.get("client.config.fingerprint"), "client.config.fingerprint");
      requireNonBlank(parameters.get("consumer.user"), "consumer.user");
      requireHash(parameters.get("identity.fingerprint"), "identity.fingerprint");
      if (verification) {
        requirePackageVersion(parameters.get("client.package.version"), "client.package.version");
        requireCanonicalUuid(parameters.get("preparation.observation.id"),
            "preparation.observation.id");
        requireHash(parameters.get("preparation.request.hash"), "preparation.request.hash");
        requireHash(parameters.get("preparation.observation.fingerprint"),
            "preparation.observation.fingerprint");
      }
      ManagedDependencySecurityMode securityMode =
          ManagedDependencySecurityMode.valueOf(parameters.get("security.mode"));
      if (securityMode == ManagedDependencySecurityMode.KERBEROS) {
        requirePrincipalPattern(parameters.get("consumer.kerberos.principal.pattern"),
            parameters.get("consumer.user"));
        if (!parameters.keySet().containsAll(CONSUMER_SECURITY_PROOFS)) {
          throw invalid("Kerberos consumer command is missing typed security proof fingerprints");
        }
        CONSUMER_SECURITY_PROOFS.forEach(
            key -> requireHash(parameters.get(key), key));
      } else if (parameters.containsKey("consumer.kerberos.principal.pattern")
          || !Collections.disjoint(parameters.keySet(), CONSUMER_SECURITY_PROOFS)) {
        throw invalid("Insecure consumer command must not carry Kerberos security proofs");
      }
      ClientConfig clientConfig = parseClientConfig(parameters.get("client.config.json"));
      if (!parameters.get("client.config.fingerprint")
          .equals(sha256(parameters.get("client.config.json")))) {
        throw invalid("client.config.fingerprint does not match client.config.json");
      }
      if (hdfs) {
        HdfsNamespace namespace = validateHdfsNamespace(envelope,
            parameters.get("expected.namespace.root.uri"),
            parameters.get("expected.namespace.wal.uri"));
        URI defaultFs = parseHdfsUri(parameters.get("expected.default.fs"), "expected.default.fs");
        if (defaultFs.getPath() != null && !defaultFs.getPath().isEmpty()
            || !defaultFs.getAuthority().equals(namespace.authority())) {
          throw invalid("expected.default.fs must exactly match the managed namespace authority");
        }
        String directoryMode = parameters.get("expected.directory.mode");
        String ownerGroup = requireNonBlank(parameters.get("expected.owner.group"), "expected.owner.group");
        if (!"0700".equals(directoryMode)
            && !("0750".equals(directoryMode) && !"hadoop".equals(ownerGroup))) {
          throw invalid("expected HDFS ownership must enforce private per-consumer authority");
        }
        Set<String> coreSiteAllowlist = securityMode == ManagedDependencySecurityMode.KERBEROS
            ? union(ManagedDependencySnapshotValidator.CORE_SITE_ALLOWLIST, Set.of(AUTH_TO_LOCAL))
            : ManagedDependencySnapshotValidator.CORE_SITE_ALLOWLIST;
        requireAllowedClientConfig(clientConfig.coreSite(), coreSiteAllowlist, "coreSite");
        requireAllowedClientConfig(clientConfig.hdfsSite(),
            allowedHdfsClientKeys(clientConfig.hdfsSite()), "hdfsSite");
        validateHdfsClientValues(securityMode, clientConfig.coreSite(), clientConfig.hdfsSite());
        String consumerRules = clientConfig.coreSite().get(AUTH_TO_LOCAL);
        if (securityMode == ManagedDependencySecurityMode.KERBEROS) {
          if (consumerRules == null
              || !parameters.get("consumer.mapping.rules.fingerprint")
              .equals(ManagedHBaseConsumerLocalMapping.fingerprintRules(consumerRules))) {
            throw invalid("Consumer auth-to-local rules do not match their approved fingerprint");
          }
        } else if (consumerRules != null) {
          throw invalid("Insecure HDFS client config must not carry consumer auth-to-local rules");
        }
        if (!clientConfig.zooKeeperClient().isEmpty()
            || !parameters.get("expected.default.fs").equals(clientConfig.coreSite().get("fs.defaultFS"))) {
          throw invalid("HDFS client config must contain only the matching core-site and hdfs-site snapshot");
        }
      } else if (!ManagedDependencyNamespace.zooKeeperContainer(envelope.bindingId())
          .equals(parameters.get("expected.namespace.container.znode"))
          || !ManagedDependencyNamespace.ZOOKEEPER_PARENT
              .equals(parameters.get("expected.namespace.parent.znode"))
          || !ManagedDependencyNamespace.zooKeeper(envelope.bindingId()).znode()
              .equals(parameters.get("expected.namespace.znode"))) {
        throw invalid("ZooKeeper container and HBase root must be derived from the binding UUID");
      } else {
        if (securityMode == ManagedDependencySecurityMode.KERBEROS) {
          requireIdentity(parameters.get("expected.consumer.zk.sasl.id"),
              "expected.consumer.zk.sasl.id");
          if (!parameters.get("consumer.user")
              .equals(parameters.get("expected.consumer.zk.sasl.id"))) {
            throw invalid("Expected ZooKeeper SASL identity must match the consumer identity");
          }
        } else if (parameters.containsKey("expected.consumer.zk.sasl.id")) {
          throw invalid("Insecure ZooKeeper consumer command must not carry a SASL identity");
        }
        requireAllowedClientConfig(clientConfig.zooKeeperClient(),
            union(ManagedDependencySnapshotValidator.ZOOKEEPER_ALLOWLIST,
                Set.of("zookeeper.znode.parent")), "zooKeeperClient");
        validateZooKeeperClientValues(securityMode, clientConfig.zooKeeperClient());
        if (!clientConfig.coreSite().isEmpty() || !clientConfig.hdfsSite().isEmpty()
            || !parameters.get("expected.namespace.znode")
                .equals(clientConfig.zooKeeperClient().get("zookeeper.znode.parent"))) {
          throw invalid("ZooKeeper client config must contain only the matching private znode snapshot");
        }
      }
    }
  }

  private static void validateHdfsProvision(Envelope envelope, Map<String, String> parameters) {
    validateHdfsNamespace(envelope,
        parameters.get("namespace.root.uri"), parameters.get("namespace.wal.uri"));
    requireNonBlank(parameters.get("owner.user"), "owner.user");
    String mode = parameters.get("directory.mode");
    String group = parameters.get("owner.group");
    if (!"0700".equals(mode)
        && !("0750".equals(mode) && group != null && !group.isBlank() && !"hadoop".equals(group))) {
      throw invalid("directory.mode must enforce private per-consumer authority");
    }
  }

  private static HdfsNamespace validateHdfsNamespace(Envelope envelope, String rootValue,
      String walValue) {
    URI root = parseHdfsUri(rootValue, "namespace.root.uri");
    URI wal = parseHdfsUri(walValue, "namespace.wal.uri");
    String base = "/apps/ambari-managed/hbase/" + envelope.bindingId();
    if (!base.concat("/root").equals(root.getRawPath())
        || !base.concat("/wal").equals(wal.getRawPath())
        || !root.getAuthority().equals(wal.getAuthority())) {
      throw invalid("HDFS namespace paths must exactly match the binding UUID and authority");
    }
    return new HdfsNamespace(root.getAuthority());
  }

  private static URI parseHdfsUri(String value, String field) {
    try {
      URI uri = URI.create(requireNonBlank(value, field));
      if (!"hdfs".equals(uri.getScheme()) || uri.getAuthority() == null || uri.getAuthority().isBlank()
          || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
          || uri.getRawPath() == null || uri.getRawPath().contains("%")
          || !uri.normalize().equals(uri)) {
        throw invalid(field + " must be a normalized credential-free HDFS URI");
      }
      return uri;
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null
          && e.getMessage().startsWith(ManagedDependencyErrorCode.DEPENDENCY_COMMAND_INVALID.name())) {
        throw e;
      }
      throw invalid(field + " must be a normalized credential-free HDFS URI");
    }
  }

  private static void validateZooKeeperProvision(Envelope envelope, Map<String, String> parameters) {
    if (!ManagedDependencyNamespace.ZOOKEEPER_PARENT.equals(parameters.get("namespace.parent.znode"))
        || !ManagedDependencyNamespace.zooKeeperContainer(envelope.bindingId())
            .equals(parameters.get("namespace.container.znode"))
        || !ManagedDependencyNamespace.zooKeeper(envelope.bindingId()).znode()
            .equals(parameters.get("namespace.znode"))) {
      throw invalid("ZooKeeper namespace must use the reserved parent, binding container, and HBase root");
    }
    requireNonBlank(parameters.get("owner.user"), "owner.user");
    parseClientPort(parameters.get("expected.client.port"));
    validateQuorum(parameters.get("expected.quorum"));
    ManagedDependencySecurityMode mode = ManagedDependencySecurityMode.valueOf(parameters.get("security.mode"));
    String expectedParentPolicy = mode == ManagedDependencySecurityMode.KERBEROS
        ? "PROVIDER_ADMIN_CREATE_DELETE" : "INSECURE_PROVIDER_PREPARED";
    String expectedSubtreePolicy = mode == ManagedDependencySecurityMode.KERBEROS
        ? "SASL_CONSUMER_PRIVATE" : "INSECURE_BINDING_SCOPED";
    if (!expectedParentPolicy.equals(parameters.get("parent.acl.policy"))
        || !expectedSubtreePolicy.equals(parameters.get("subtree.acl.policy"))) {
      throw invalid("ZooKeeper ACL policies do not match the snapshot security mode");
    }
    if (mode == ManagedDependencySecurityMode.KERBEROS) {
      requireIdentity(parameters.get("consumer.zk.sasl.id"), "consumer.zk.sasl.id");
      if (!parameters.get("owner.user").equals(parameters.get("consumer.zk.sasl.id"))
          || !ManagedDependencyNamespace.zooKeeperLedger(envelope.bindingId())
              .equals(parameters.get("namespace.ledger.znode"))) {
        throw invalid("Secure ZooKeeper handoff identity or ledger does not match the binding");
      }
    } else if (parameters.containsKey("consumer.zk.sasl.id")
        || parameters.containsKey("namespace.ledger.znode")) {
      throw invalid("Insecure ZooKeeper commands must not carry secure handoff fields");
    }
  }

  private static void append(StringBuilder canonical, String... values) {
    for (String value : values) {
      String actual = value == null ? "" : value;
      canonical.append(actual.length()).append(':').append(actual);
    }
  }

  private static SortedMap<String, String> immutable(Map<String, String> values) {
    return ManagedDependencyVersion.immutableSortedMap(values);
  }

  @SafeVarargs
  private static Set<String> union(Set<String>... sets) {
    java.util.HashSet<String> result = new java.util.HashSet<>();
    for (Set<String> set : sets) {
      result.addAll(set);
    }
    return Set.copyOf(result);
  }

  private static IllegalArgumentException invalid(String message) {
    return new IllegalArgumentException(ManagedDependencyErrorCode.DEPENDENCY_COMMAND_INVALID
        + ": " + message);
  }

  private static String requireNonBlank(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw invalid(field + " must not be blank");
    }
    return value;
  }

  private static String requireHash(String value, String field) {
    requireNonBlank(value, field);
    if (!HASH.matcher(value).matches()) {
      throw invalid(field + " must use sha256:<64 lowercase hex> format");
    }
    return value;
  }

  private static String requirePackageName(String value) {
    requireNonBlank(value, "client.package.name");
    if (!PACKAGE_NAME.matcher(value).matches()) {
      throw invalid("client.package.name must be a bounded operating-system package name");
    }
    return value;
  }

  private static String requirePackageVersion(String value, String field) {
    requireNonBlank(value, field);
    if (value.length() > 512 || value.chars().anyMatch(Character::isISOControl)) {
      throw invalid(field + " must be a bounded single-line package-manager version");
    }
    return value;
  }

  private static String requireSoftwareVersion(String value) {
    requireNonBlank(value, "client.software.semantic.version");
    if (value.length() > 128 || !SOFTWARE_VERSION.matcher(value).matches()) {
      throw invalid("client.software.semantic.version is not a supported semantic version");
    }
    return value;
  }

  private static String requireIdentity(String value, String field) {
    requireNonBlank(value, field);
    if (!IDENTITY.matcher(value).matches()) {
      throw invalid(field + " must be a bounded simple authorization identity");
    }
    return value;
  }

  private static String securePrincipalPattern(ManagedDependencySnapshot snapshot) {
    if (snapshot.consumerIdentity().principalPatterns().size() != 1) {
      throw invalid("Secure consumer verification requires exactly one role principal pattern");
    }
    return requirePrincipalPattern(snapshot.consumerIdentity().principalPatterns().first(),
        snapshot.consumerIdentity().effectiveShortUser());
  }

  private static String requirePrincipalPattern(String value, String consumerUser) {
    requireNonBlank(value, "consumer.kerberos.principal.pattern");
    String prefix = requireIdentity(consumerUser, "consumer.user") + "/_HOST@";
    String realm = value.startsWith(prefix) ? value.substring(prefix.length()) : "";
    if (value.length() > 512 || !KERBEROS_REALM.matcher(realm).matches()) {
      throw invalid("consumer.kerberos.principal.pattern must match consumer.user/_HOST@REALM");
    }
    return value;
  }

  private static String clientConfigJson(Map<String, String> coreSite,
      Map<String, String> hdfsSite, Map<String, String> zooKeeperClient) {
    TreeMap<String, Object> value = new TreeMap<>();
    value.put("coreSite", new TreeMap<>(coreSite));
    value.put("hdfsSite", new TreeMap<>(hdfsSite));
    value.put("zooKeeperClient", new TreeMap<>(zooKeeperClient));
    try {
      return JSON.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Approved client configuration must be JSON serializable", e);
    }
  }

  private static ClientConfig parseClientConfig(String value) {
    requireNonBlank(value, "client.config.json");
    if (value.getBytes(StandardCharsets.UTF_8).length > MAX_CLIENT_CONFIG_BYTES) {
      throw invalid("client.config.json exceeds the managed dependency size limit");
    }
    requireJsonNesting(value, 2);
    try {
      JsonNode root = JSON.readTree(value);
      if (root == null || !root.isObject() || root.size() != 3
          || !root.has("coreSite") || !root.has("hdfsSite") || !root.has("zooKeeperClient")) {
        throw invalid("client.config.json must contain exactly the three approved configuration maps");
      }
      ClientConfig config = new ClientConfig(
          stringMap(root.get("coreSite"), "coreSite"),
          stringMap(root.get("hdfsSite"), "hdfsSite"),
          stringMap(root.get("zooKeeperClient"), "zooKeeperClient"));
      if (!value.equals(clientConfigJson(config.coreSite(), config.hdfsSite(), config.zooKeeperClient()))) {
        throw invalid("client.config.json must use canonical sorted compact JSON");
      }
      return config;
    } catch (JsonProcessingException e) {
      throw invalid("client.config.json must be valid JSON");
    }
  }

  private static SortedMap<String, String> stringMap(JsonNode node, String field) {
    if (node == null || !node.isObject() || node.size() > 64) {
      throw invalid(field + " must be a bounded object of string properties");
    }
    TreeMap<String, String> result = new TreeMap<>();
    node.fields().forEachRemaining(entry -> {
      String text = entry.getValue().textValue();
      int maximumBytes = "coreSite".equals(field) && AUTH_TO_LOCAL.equals(entry.getKey())
          ? ManagedHBaseConsumerLocalMapping.MAX_RULE_BYTES : 4096;
      if (!entry.getValue().isTextual() || entry.getKey().length() > 256
          || text.getBytes(StandardCharsets.UTF_8).length > maximumBytes) {
        throw invalid(field + " must contain only bounded string properties");
      }
      result.put(entry.getKey(), text);
    });
    return Collections.unmodifiableSortedMap(result);
  }

  private static void requireJsonNesting(String value, int maximumDepth) {
    int depth = 0;
    boolean quoted = false;
    boolean escaped = false;
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (quoted) {
        if (escaped) {
          escaped = false;
        } else if (current == '\\') {
          escaped = true;
        } else if (current == '"') {
          quoted = false;
        }
      } else if (current == '"') {
        quoted = true;
      } else if (current == '{' || current == '[') {
        if (++depth > maximumDepth) {
          throw invalid("client.config.json exceeds the supported nesting depth");
        }
      } else if (current == '}' || current == ']') {
        depth--;
      }
    }
  }

  private static void requireAllowedClientConfig(Map<String, String> config,
      Set<String> allowedKeys, String field) {
    if (!allowedKeys.containsAll(config.keySet())) {
      throw invalid(field + " contains a property outside the managed dependency allowlist");
    }
  }

  private static Set<String> allowedHdfsClientKeys(Map<String, String> config) {
    String nameService = config.get("dfs.nameservices");
    if (nameService == null) {
      return ManagedDependencySnapshotValidator.HDFS_SITE_ALLOWLIST;
    }
    if (!HA_TOKEN.matcher(nameService).matches()) {
      throw invalid("dfs.nameservices must identify one approved HA nameservice");
    }
    String idsKey = "dfs.ha.namenodes." + nameService;
    String idsValue = config.get(idsKey);
    if (idsValue == null) {
      throw invalid("HA client config is missing the approved NameNode IDs");
    }
    java.util.HashSet<String> dynamic = new java.util.HashSet<>();
    dynamic.add(idsKey);
    String[] ids = idsValue.split(",", -1);
    if (ids.length < 2) {
      throw invalid("HA client config requires at least two NameNode IDs");
    }
    for (String id : ids) {
      if (!HA_TOKEN.matcher(id).matches()) {
        throw invalid("HA client config contains an invalid NameNode ID");
      }
      dynamic.add("dfs.namenode.rpc-address." + nameService + "." + id);
    }
    String providerKey = "dfs.client.failover.proxy.provider." + nameService;
    dynamic.add(providerKey);
    if (!config.keySet().containsAll(dynamic)
        || !STANDARD_HA_PROVIDER.equals(config.get(providerKey))) {
      throw invalid("HA client config is incomplete or uses an unsupported failover provider");
    }
    return union(ManagedDependencySnapshotValidator.HDFS_SITE_ALLOWLIST, dynamic);
  }

  private static void validateHdfsClientValues(ManagedDependencySecurityMode securityMode,
      Map<String, String> coreSite, Map<String, String> hdfsSite) {
    requireEnum(coreSite, "hadoop.security.authentication", Set.of("simple", "kerberos"));
    requireEnum(coreSite, "hadoop.rpc.protection", Set.of("authentication", "integrity", "privacy"));
    requireBoolean(coreSite, "hadoop.security.authorization");
    requireBoolean(coreSite, "ipc.client.fallback-to-simple-auth-allowed");
    requireInteger(coreSite, "ipc.client.connect.max.retries", 0, 50);
    requireInteger(coreSite, "ipc.client.connect.retry.interval", 100, 60_000);
    requireBoolean(hdfsSite, "dfs.block.access.token.enable");
    requireInteger(hdfsSite, "dfs.client.failover.connection.retries", 0, 100);
    requireInteger(hdfsSite, "dfs.client.failover.connection.retries.on.timeouts", 0, 100);
    requireInteger(hdfsSite, "dfs.client.socket-timeout", 1_000, 600_000);
    requireBoolean(hdfsSite, "dfs.client.use.datanode.hostname");
    requireBoolean(hdfsSite, "dfs.encrypt.data.transfer");
    String protection = hdfsSite.get("dfs.data.transfer.protection");
    if (protection != null && java.util.Arrays.stream(protection.split(",", -1))
        .map(String::trim)
        .anyMatch(value -> !Set.of("authentication", "integrity", "privacy").contains(value))) {
      throw invalid("dfs.data.transfer.protection contains an unsupported value");
    }
    if (securityMode == ManagedDependencySecurityMode.KERBEROS) {
      if (!"kerberos".equals(coreSite.get("hadoop.security.authentication"))
          || !"false".equals(coreSite.get("ipc.client.fallback-to-simple-auth-allowed"))
          || isBlank(hdfsSite.get("dfs.namenode.kerberos.principal"))
          || isBlank(hdfsSite.get("dfs.datanode.kerberos.principal"))) {
        throw invalid("Secure HDFS client config requires Kerberos principals and no simple fallback");
      }
    } else if ("kerberos".equals(coreSite.get("hadoop.security.authentication"))) {
      throw invalid("Insecure HDFS client config cannot enable Kerberos authentication");
    }
  }

  private static void validateZooKeeperClientValues(ManagedDependencySecurityMode securityMode,
      Map<String, String> config) {
    requireBoolean(config, "zookeeper.sasl.client");
    if (config.containsKey("zookeeper.sasl.clientconfig")
        && !"Client".equals(config.get("zookeeper.sasl.clientconfig"))) {
      throw invalid("zookeeper.sasl.clientconfig must use the Client login context");
    }
    String service = config.get("zookeeper.sasl.client.username");
    if (service != null && !IDENTITY.matcher(service).matches()) {
      throw invalid("zookeeper.sasl.client.username must be a bounded service primary");
    }
    if (securityMode == ManagedDependencySecurityMode.KERBEROS
        && (!"true".equals(config.get("zookeeper.sasl.client"))
            || service == null || !"Client".equals(config.get("zookeeper.sasl.clientconfig")))) {
      throw invalid("Secure ZooKeeper client config requires the exact SASL client settings");
    }
    if (securityMode == ManagedDependencySecurityMode.INSECURE
        && "true".equals(config.get("zookeeper.sasl.client"))) {
      throw invalid("Insecure ZooKeeper client config cannot enable SASL");
    }
  }

  private static void requireEnum(Map<String, String> values, String key, Set<String> allowed) {
    String value = values.get(key);
    if (value != null && !allowed.contains(value)) {
      throw invalid(key + " has an unsupported value");
    }
  }

  private static void requireBoolean(Map<String, String> values, String key) {
    requireEnum(values, key, Set.of("true", "false"));
  }

  private static void requireInteger(Map<String, String> values, String key, int minimum, int maximum) {
    String value = values.get(key);
    if (value == null) {
      return;
    }
    try {
      if (!value.equals(Integer.toString(Integer.parseInt(value)))
          || Integer.parseInt(value) < minimum || Integer.parseInt(value) > maximum) {
        throw invalid(key + " is outside the supported range");
      }
    } catch (NumberFormatException e) {
      throw invalid(key + " must be a canonical bounded integer");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static long parsePositiveLong(String value, String field) {
    if (value == null || !POSITIVE_INTEGER.matcher(value).matches()) {
      throw invalid(field + " must be a canonical positive integer");
    }
    try {
      long parsed = Long.parseLong(value);
      return parsed;
    } catch (NumberFormatException e) {
      throw invalid(field + " must be a canonical positive integer");
    }
  }

  private static int parseClientPort(String value) {
    long port = parsePositiveLong(value, "expected.client.port");
    if (port > 65535) {
      throw invalid("expected.client.port must be between 1 and 65535");
    }
    return (int) port;
  }

  private static void validateQuorum(String value) {
    requireNonBlank(value, "expected.quorum");
    java.util.TreeSet<String> hosts = new java.util.TreeSet<>();
    for (String host : value.split(",", -1)) {
      if (!HA_TOKEN.matcher(host).matches() || !hosts.add(host)) {
        throw invalid("expected.quorum must contain unique approved host names");
      }
    }
    if (!String.join(",", hosts).equals(value)) {
      throw invalid("expected.quorum must use canonical sorted host order");
    }
  }

  public enum CommandName {
    PREPARE_BINDING_JOURNAL,
    INITIALIZE_BINDING_JOURNAL,
    PROVISION_HDFS_NAMESPACE,
    PROVISION_ZOOKEEPER_NAMESPACE,
    INVALIDATE_BINDING_EPOCH,
    PREPARE_HDFS_CONSUMER,
    PREPARE_ZOOKEEPER_CONSUMER,
    VERIFY_HDFS_CONSUMER,
    VERIFY_ZOOKEEPER_CONSUMER
  }

  public enum ResultStatus {
    SUCCEEDED,
    FAILED,
    RECONCILIATION_REQUIRED,
    STALE_REJECTED
  }

  public void validateResult(Result result) {
    Objects.requireNonNull(result, "result");
    if (result.commandName() != name || !result.envelope().equals(envelope)) {
      throw invalid("Result command and operation envelope must match the persisted command");
    }
    if (result.status() == ResultStatus.RECONCILIATION_REQUIRED) {
      validateZooKeeperReconciliation(result);
      return;
    }
    if (result.status() != ResultStatus.SUCCEEDED) {
      return;
    }
    if (!Set.of(CommandName.PREPARE_BINDING_JOURNAL,
        CommandName.INITIALIZE_BINDING_JOURNAL,
        CommandName.INVALIDATE_BINDING_EPOCH).contains(name)) {
      requireEqual(parameters.get("snapshot.fingerprint"),
          result.facts().get("applied.snapshot.fingerprint"), "applied.snapshot.fingerprint");
    }
    if (isProviderCommand(name) && name != CommandName.PREPARE_BINDING_JOURNAL) {
      requireEqual(parameters.get("provider.action.host.id"),
          result.facts().get("provider.action.host.id"), "provider.action.host.id");
    }
    switch (name) {
      case PREPARE_BINDING_JOURNAL:
        requireCanonicalUuid(result.facts().get("initialization.challenge"),
            "initialization.challenge");
        break;
      case INITIALIZE_BINDING_JOURNAL:
        requireEqual(parameters.get("initialization.authorization.id"),
            result.facts().get("initialization.authorization.id"),
            "initialization.authorization.id");
        requireEqual("true", result.facts().get("journal.initialized"), "journal.initialized");
        break;
      case PROVISION_HDFS_NAMESPACE:
        requireEqual(parameters.get("owner.user"), result.facts().get("owner.user"), "owner.user");
        requireEqual(parameters.get("owner.group"), result.facts().get("owner.group"), "owner.group");
        requireEqual(parameters.get("directory.mode"), result.facts().get("directory.mode"), "directory.mode");
        break;
      case PROVISION_ZOOKEEPER_NAMESPACE:
        validateZooKeeperProvisionSuccess(result);
        break;
      case INVALIDATE_BINDING_EPOCH:
        if (parsePositiveLong(result.facts().get("highest.accepted.epoch"), "highest.accepted.epoch")
            != envelope.epoch()) {
          throw invalid("Invalidation result epoch must match the persisted command epoch");
        }
        break;
      case PREPARE_HDFS_CONSUMER:
      case PREPARE_ZOOKEEPER_CONSUMER:
        validateConsumerPreparationFacts(result);
        break;
      case VERIFY_HDFS_CONSUMER:
      case VERIFY_ZOOKEEPER_CONSUMER:
        requireEqual(parameters.get("host.id"), result.facts().get("host.id"), "host.id");
        requireEqual(parameters.get("client.package.name"),
            result.facts().get("client.package.name"), "client.package.name");
        requireEqual(parameters.get("client.package.version"),
            result.facts().get("client.package.version"), "client.package.version");
        requireEqual(parameters.get("identity.fingerprint"),
            result.facts().get("identity.fingerprint"), "identity.fingerprint");
        requireEqual(parameters.get("client.config.fingerprint"),
            result.facts().get("client.config.fingerprint"), "client.config.fingerprint");
        requireEqual(parameters.get("client.software.kind"),
            result.facts().get("client.software.kind"), "client.software.kind");
        requireEqual(parameters.get("client.software.semantic.version"),
            result.facts().get("client.software.semantic.version"),
            "client.software.semantic.version");
        validateConsumerVerificationFacts(result);
        break;
      default:
        throw invalid("Unsupported reserved command " + name);
    }
  }

  private static void requireEqual(String expected, String actual, String field) {
    if (!Objects.equals(expected, actual)) {
      throw invalid(field + " does not match the persisted command");
    }
  }

  private void validateConsumerPreparationFacts(Result result) {
    requireExactFacts(result, CONSUMER_PREPARATION_SUCCESS_FACTS);
    requireEqual(parameters.get("host.id"), result.facts().get("host.id"), "host.id");
    requireEqual(parameters.get("client.package.name"),
        result.facts().get("client.package.name"), "client.package.name");
    requirePackageVersion(result.facts().get("client.package.version"),
        "client.package.version");
    requireEqual(parameters.get("client.software.kind"),
        result.facts().get("client.software.kind"), "client.software.kind");
    requireEqual(parameters.get("client.software.semantic.version"),
        result.facts().get("client.software.semantic.version"),
        "client.software.semantic.version");
    requireEqual(parameters.get("client.config.fingerprint"),
        result.facts().get("client.config.fingerprint"), "client.config.fingerprint");
    requireEqual(parameters.get("consumer.user"),
        result.facts().get("consumer.user"), "consumer.user");
    requireEqual(parameters.get("identity.fingerprint"),
        result.facts().get("identity.fingerprint"), "identity.fingerprint");
  }

  private void validateConsumerVerificationFacts(Result result) {
    ManagedDependencySecurityMode mode =
        ManagedDependencySecurityMode.valueOf(parameters.get("security.mode"));
    if (name == CommandName.VERIFY_HDFS_CONSUMER) {
      requireExactFacts(result, mode == ManagedDependencySecurityMode.KERBEROS
          ? union(HDFS_CONSUMER_SUCCESS_FACTS, Set.of("consumer.kerberos.principal"))
          : HDFS_CONSUMER_SUCCESS_FACTS);
    } else if (mode == ManagedDependencySecurityMode.KERBEROS) {
      requireExactFacts(result, ZOOKEEPER_SECURE_CONSUMER_SUCCESS_FACTS);
      requireEqual(parameters.get("expected.consumer.zk.sasl.id"),
          result.facts().get("consumer.zk.sasl.id"), "consumer.zk.sasl.id");
      requireEqual("true", result.facts().get("container.acl.verified"),
          "container.acl.verified");
      requireEqual("true", result.facts().get("hbase.znode.verified"),
          "hbase.znode.verified");
      requireEqual("true", result.facts().get("sibling.authority.denied"),
          "sibling.authority.denied");
    } else {
      requireExactFacts(result, ZOOKEEPER_CONSUMER_SUCCESS_FACTS);
    }
    if (mode == ManagedDependencySecurityMode.KERBEROS) {
      requirePrincipalMatchesPattern(
          parameters.get("consumer.kerberos.principal.pattern"),
          result.facts().get("consumer.kerberos.principal"));
    }
  }

  private static void requirePrincipalMatchesPattern(String pattern, String actual) {
    String marker = "/_HOST@";
    int split = pattern.indexOf(marker);
    String prefix = pattern.substring(0, split + 1);
    String suffix = pattern.substring(split + marker.length() - 1);
    if (actual == null || !actual.startsWith(prefix) || !actual.endsWith(suffix)) {
      throw invalid("consumer.kerberos.principal does not match the persisted role pattern");
    }
    String host = actual.substring(prefix.length(), actual.length() - suffix.length());
    if (!HA_TOKEN.matcher(host).matches() || !host.equals(host.toLowerCase(java.util.Locale.ROOT))) {
      throw invalid("consumer.kerberos.principal contains an invalid canonical host");
    }
  }

  private void validateZooKeeperProvisionSuccess(Result result) {
    ManagedDependencySecurityMode mode =
        ManagedDependencySecurityMode.valueOf(parameters.get("security.mode"));
    if (mode == ManagedDependencySecurityMode.INSECURE) {
      requireExactFacts(result, ZOOKEEPER_INSECURE_SUCCESS_FACTS);
      requireEqual(parameters.get("owner.user"), result.facts().get("owner.user"), "owner.user");
      requireEqual(parameters.get("parent.acl.policy"),
          result.facts().get("parent.acl.policy"), "parent.acl.policy");
      requireEqual(parameters.get("subtree.acl.policy"),
          result.facts().get("subtree.acl.policy"), "subtree.acl.policy");
      return;
    }
    requireExactFacts(result, ZOOKEEPER_SECURE_SUCCESS_FACTS);
    validateZooKeeperHandoffFacts(result);
    requireEqual("true", result.facts().get("provider.handoff.acknowledged"),
        "provider.handoff.acknowledged");
  }

  private void validateZooKeeperReconciliation(Result result) {
    if (name != CommandName.PROVISION_ZOOKEEPER_NAMESPACE
        || ManagedDependencySecurityMode.valueOf(parameters.get("security.mode"))
            != ManagedDependencySecurityMode.KERBEROS
        || result.errorCode()
            != ManagedDependencyErrorCode.DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED) {
      throw invalid("RECONCILIATION_REQUIRED is reserved for a secure ZooKeeper handoff");
    }
    requireExactFacts(result, ZOOKEEPER_RECONCILIATION_FACTS);
    validateZooKeeperHandoffFacts(result);
    requireEqual("true", result.facts().get("provider.handoff.reconciliation.required"),
        "provider.handoff.reconciliation.required");
  }

  private void validateZooKeeperHandoffFacts(Result result) {
    requireEqual(parameters.get("snapshot.fingerprint"),
        result.facts().get("applied.snapshot.fingerprint"), "applied.snapshot.fingerprint");
    requireEqual(parameters.get("provider.action.host.id"),
        result.facts().get("provider.action.host.id"), "provider.action.host.id");
    requireEqual(parameters.get("namespace.ledger.znode"),
        result.facts().get("namespace.ledger.znode"), "namespace.ledger.znode");
    requireEqual(parameters.get("namespace.container.znode"),
        result.facts().get("namespace.container.znode"), "namespace.container.znode");
    requireEqual(parameters.get("namespace.znode"),
        result.facts().get("namespace.znode"), "namespace.znode");
    requireEqual(parameters.get("consumer.zk.sasl.id"),
        result.facts().get("consumer.zk.sasl.id"), "consumer.zk.sasl.id");
  }

  private static void requireExactFacts(Result result, Set<String> expected) {
    if (!result.facts().keySet().equals(expected)) {
      throw invalid("Result facts do not match the exact outcome contract for "
          + result.commandName());
    }
  }

  private static boolean isProviderCommand(CommandName name) {
    return Set.of(CommandName.PREPARE_BINDING_JOURNAL,
        CommandName.INITIALIZE_BINDING_JOURNAL,
        CommandName.PROVISION_HDFS_NAMESPACE,
        CommandName.PROVISION_ZOOKEEPER_NAMESPACE,
        CommandName.INVALIDATE_BINDING_EPOCH).contains(name);
  }

  private static UUID requireCanonicalUuid(String value, String field) {
    requireNonBlank(value, field);
    try {
      UUID parsed = UUID.fromString(value);
      if (!parsed.toString().equals(value)) {
        throw invalid(field + " must be a canonical lower-case UUID");
      }
      return parsed;
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null
          && e.getMessage().startsWith(ManagedDependencyErrorCode.DEPENDENCY_COMMAND_INVALID.name())) {
        throw e;
      }
      throw invalid(field + " must be a canonical lower-case UUID");
    }
  }

  public record Envelope(
      int protocolVersion,
      UUID bindingId,
      UUID operationId,
      long epoch,
      long snapshotVersion,
      String immutableRequestHash) {
    public Envelope {
      if (protocolVersion != CURRENT_PROTOCOL_VERSION) {
        throw invalid("unsupported protocolVersion " + protocolVersion);
      }
      bindingId = Objects.requireNonNull(bindingId, "bindingId");
      operationId = Objects.requireNonNull(operationId, "operationId");
      if (epoch <= 0 || snapshotVersion <= 0) {
        throw invalid("epoch and snapshotVersion must be positive");
      }
      immutableRequestHash = requireHash(immutableRequestHash, "immutableRequestHash");
    }
  }

  public record Result(
      CommandName commandName,
      Envelope envelope,
      ResultStatus status,
      SortedMap<String, String> facts,
      ManagedDependencyErrorCode errorCode,
      String errorMessage) {
    public Result {
      commandName = Objects.requireNonNull(commandName, "commandName");
      envelope = Objects.requireNonNull(envelope, "envelope");
      status = Objects.requireNonNull(status, "status");
      facts = immutable(facts);
      if (!ALLOWED_RESULT_FACTS.get(commandName).containsAll(facts.keySet())) {
        throw invalid("Result facts contain unknown fields for " + commandName);
      }
      if (status == ResultStatus.SUCCEEDED
          && !facts.keySet().containsAll(REQUIRED_SUCCESS_FACTS.get(commandName))) {
        throw invalid("Successful result is missing required facts for " + commandName);
      }
      for (Map.Entry<String, String> fact : facts.entrySet()) {
        if (fact.getKey().endsWith("fingerprint")) {
          requireHash(fact.getValue(), fact.getKey());
        }
        if (status == ResultStatus.SUCCEEDED
            && (fact.getKey().endsWith(".exists")
                || fact.getKey().endsWith(".connected")
                || fact.getKey().endsWith(".verified"))
            && !"true".equals(fact.getValue())) {
          throw invalid("Successful verification facts must be true: " + fact.getKey());
        }
      }
      if (status == ResultStatus.SUCCEEDED && (errorCode != null || errorMessage != null)
          || status != ResultStatus.SUCCEEDED
              && (errorCode == null || errorMessage == null || errorMessage.isBlank())) {
        throw invalid("successful results have no error; failed/stale results require one");
      }
      if (status == ResultStatus.STALE_REJECTED
          && errorCode != ManagedDependencyErrorCode.DEPENDENCY_OPERATION_STALE) {
        throw invalid("STALE_REJECTED results require DEPENDENCY_OPERATION_STALE");
      }
      if (status == ResultStatus.RECONCILIATION_REQUIRED
          && (commandName != CommandName.PROVISION_ZOOKEEPER_NAMESPACE
              || errorCode
                  != ManagedDependencyErrorCode.DEPENDENCY_ZOOKEEPER_HANDOFF_RECONCILIATION_REQUIRED)) {
        throw invalid("RECONCILIATION_REQUIRED is reserved for ZooKeeper handoff recovery");
      }
      if (errorMessage != null
          && (errorMessage.length() > 512 || errorMessage.indexOf('\n') >= 0 || errorMessage.indexOf('\r') >= 0)) {
        throw invalid("errorMessage must be a bounded single-line sanitized value");
      }
    }
  }

  private record HdfsNamespace(String authority) {
  }

  private record ClientConfig(
      SortedMap<String, String> coreSite,
      SortedMap<String, String> hdfsSite,
      SortedMap<String, String> zooKeeperClient) {
  }
}
