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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ConsumerLifecycle;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.HdfsEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ZooKeeperEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIdentity.Allocation;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerPatternInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.ConsumerPatternProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.Policy;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.ConsumerInput;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.PairProof;
import org.apache.ambari.server.controller.dependencies.security.ManagedZooKeeperSaslPolicyVerifier.PolicySource;

/** Pure construction and validation of an approved managed-dependency snapshot. */
public final class ManagedDependencySnapshotValidator {
  public static final int FINGERPRINT_SCHEMA_VERSION = 1;
  private static final String SUPPORTED_STACK_NAME = "BIGTOP";
  private static final String SUPPORTED_STACK_VERSION = "3.3.0";
  private static final String STANDARD_HA_FAILOVER_PROVIDER =
      "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider";
  private static final Pattern HOST = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?");
  static final Set<String> CORE_SITE_ALLOWLIST = Set.of(
      "fs.defaultFS",
      "hadoop.rpc.protection",
      "hadoop.security.authentication",
      "hadoop.security.authorization",
      "ipc.client.connect.max.retries",
      "ipc.client.connect.retry.interval",
      "ipc.client.fallback-to-simple-auth-allowed");
  static final Set<String> HDFS_SITE_ALLOWLIST = Set.of(
      "dfs.block.access.token.enable",
      "dfs.client.failover.connection.retries",
      "dfs.client.failover.connection.retries.on.timeouts",
      "dfs.client.socket-timeout",
      "dfs.client.use.datanode.hostname",
      "dfs.data.transfer.protection",
      "dfs.datanode.kerberos.principal",
      "dfs.encrypt.data.transfer",
      "dfs.namenode.kerberos.principal",
      "dfs.nameservices");
  static final Set<String> ZOOKEEPER_ALLOWLIST = Set.of(
      "hbase.zookeeper.property.clientPort",
      "hbase.zookeeper.quorum",
      "zookeeper.sasl.client",
      "zookeeper.sasl.client.username",
      "zookeeper.sasl.clientconfig");
  private static final List<String> SECRET_FRAGMENTS = List.of(
      "credential", "keytab", "password", "secret", "ticket.cache", "token.file");

  private final boolean sameRealmSecureEnabled;
  private final ManagedHdfsAuthToLocalVerifier hdfsSecurityVerifier =
      new ManagedHdfsAuthToLocalVerifier();
  private final ManagedZooKeeperSaslPolicyVerifier zooKeeperSecurityVerifier =
      new ManagedZooKeeperSaslPolicyVerifier();

  public ManagedDependencySnapshotValidator(boolean sameRealmSecureEnabled) {
    this.sameRealmSecureEnabled = sameRealmSecureEnabled;
  }

  public ValidationResult validate(
      UUID bindingId,
      long snapshotVersion,
      ManagedDependencyType type,
      Consumer consumer,
      Provider provider,
      Collection<ManagedDependencyNamespace> existingNamespaces) {
    return validate(bindingId, snapshotVersion, type, consumer, provider,
        existingNamespaces, List.of(), null);
  }

  public ValidationResult validate(
      UUID bindingId,
      long snapshotVersion,
      ManagedDependencyType type,
      Consumer consumer,
      Provider provider,
      Collection<ManagedDependencyNamespace> existingNamespaces,
      Collection<Allocation> existingIdentityAllocations,
      ManagedDependencySnapshot currentManagedSnapshot) {
    return validate(bindingId, snapshotVersion, type, consumer, provider,
        existingNamespaces, existingIdentityAllocations, currentManagedSnapshot,
        ManagedDependencySecurityValidationContext.none());
  }

  public ValidationResult validate(
      UUID bindingId,
      long snapshotVersion,
      ManagedDependencyType type,
      Consumer consumer,
      Provider provider,
      Collection<ManagedDependencyNamespace> existingNamespaces,
      Collection<Allocation> existingIdentityAllocations,
      ManagedDependencySnapshot currentManagedSnapshot,
      ManagedDependencySecurityValidationContext securityContext) {
    List<Issue> issues = new ArrayList<>();
    if (bindingId == null || snapshotVersion <= 0) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_BINDING_ID,
          "A binding UUID and positive snapshot version are required."));
    }
    if (consumer == null || !"HBASE".equals(consumer.serviceName())) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
          "The consumer descriptor must resolve the HBASE service."));
    }
    if (provider == null || type == null || provider != null && (provider.type() != type
        || !provider.type().getProviderServiceName().equals(provider.serviceKey().serviceName()))) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "The provider descriptor must match the requested dependency type."));
    }
    if (!issues.isEmpty()) {
      return ValidationResult.failure(issues);
    }

    validateConsumerLifecycle(bindingId, type, consumer, provider, currentManagedSnapshot, issues);
    validateIdentityPlan(consumer, existingIdentityAllocations, issues);
    validateVersion(consumer, provider, issues);
    validateProviderReadiness(provider, issues);
    detectUnsupportedProviderFeatures(provider, issues);
    SecurityResolution security = validateSecurity(type, consumer, provider,
        securityContext, issues);

    ManagedDependencyNamespace namespace = null;
    SortedMap<String, String> coreSite = Collections.emptySortedMap();
    SortedMap<String, String> hdfsSite = Collections.emptySortedMap();
    SortedMap<String, String> zooKeeperClient = Collections.emptySortedMap();
    if (type == ManagedDependencyType.HDFS) {
      HdfsResolution resolution = validateHdfs(provider, bindingId, issues);
      if (resolution != null) {
        namespace = resolution.namespace();
        coreSite = resolution.coreSite();
        hdfsSite = resolution.hdfsSite();
      }
    } else {
      ZooKeeperResolution resolution = validateZooKeeper(provider, bindingId, issues);
      if (resolution != null) {
        namespace = resolution.namespace();
        zooKeeperClient = resolution.clientConfig();
      }
    }

    if (namespace != null && existingNamespaces != null) {
      for (ManagedDependencyNamespace existing : existingNamespaces) {
        if (existing != null && !namespace.equals(existing) && namespace.overlaps(existing)) {
          issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_NAMESPACE_CONFLICT,
              "The derived private namespace overlaps an existing binding namespace."));
          break;
        }
      }
    }
    if (namespace != null && currentManagedSnapshot != null
        && !namespace.equals(currentManagedSnapshot.namespace())) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
          "A managed snapshot update must preserve the exact root, WAL, or znode namespace."));
    }
    if (!issues.isEmpty()) {
      return ValidationResult.failure(issues);
    }

    String consumerFingerprint = consumerFingerprint(consumer,
        security == null ? null : security.consumerMapping());
    String providerFingerprint = providerFingerprint(bindingId, provider, namespace,
        coreSite, hdfsSite, zooKeeperClient,
        security == null ? null : security.providerSecurity());
    String snapshotFingerprint = hash(
        "snapshot", Integer.toString(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION),
        bindingId.toString(), Long.toString(snapshotVersion), type.name(),
        consumerFingerprint, providerFingerprint,
        namespace.rootUri(), namespace.walUri(), namespace.znode(),
        canonicalMap(coreSite), canonicalMap(hdfsSite), canonicalMap(zooKeeperClient),
        security == null ? "" : security.consumerMapping().profileFingerprint(),
        security == null ? "" : security.providerSecurity().policyFingerprint(),
        security == null ? "" : security.pairSecurity().proofFingerprint());
    return ValidationResult.success(new ManagedDependencySnapshot(
        ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        bindingId,
        snapshotVersion,
        type,
        provider.serviceKey(),
        namespace,
        provider.version().compatibility(),
        provider.securityMode(),
        consumer.identity(),
        coreSite,
        hdfsSite,
        zooKeeperClient,
        consumerFingerprint,
        providerFingerprint,
        snapshotFingerprint,
        security == null ? null : security.consumerMapping(),
        security == null ? null : security.providerSecurity(),
        security == null ? null : security.pairSecurity()));
  }

  /** Excludes sourceScope, draftId, clusterId and clusterName by design. */
  public String consumerFingerprint(Consumer consumer) {
    return consumerFingerprint(consumer, null);
  }

  /** Excludes sourceScope, draftId, clusterId and clusterName by design. */
  public String consumerFingerprint(Consumer consumer,
      ManagedHBaseConsumerLocalMapping consumerMapping) {
    Objects.requireNonNull(consumer, "consumer");
    if (consumer.securityMode() == ManagedDependencySecurityMode.KERBEROS
        && consumerMapping == null) {
      return hash("consumer-plan", consumer.serviceName(), canonicalVersion(consumer.version()),
          consumer.securityMode().name(), consumer.realm(), canonicalIdentity(consumer.identity()),
          consumer.identityPlan().planFingerprint());
    }
    return hash(
        "consumer", consumer.serviceName(), canonicalVersion(consumer.version()),
        consumer.securityMode().name(), consumer.realm(), canonicalIdentity(consumer.identity()),
        consumer.identityPlan().planFingerprint(),
        consumerMapping == null ? "" : consumerMapping.profileFingerprint());
  }

  private void validateConsumerLifecycle(UUID bindingId, ManagedDependencyType type,
      Consumer consumer, Provider provider, ManagedDependencySnapshot currentSnapshot,
      List<Issue> issues) {
    if (consumer.lifecycle() == ConsumerLifecycle.INSTALLED_LOCAL) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_LOCAL_DATA_MIGRATION_UNSUPPORTED,
          "A new managed binding requires a draft or fresh INIT HBASE service; local data migration is unsupported."));
      return;
    }
    if (consumer.lifecycle() == ConsumerLifecycle.MANAGED_UPDATE) {
      if (currentSnapshot == null
          || !currentSnapshot.bindingId().equals(bindingId)
          || currentSnapshot.type() != type
          || !currentSnapshot.providerService().equals(provider.serviceKey())
          || !currentSnapshot.consumerIdentity().equals(consumer.identity())) {
        issues.add(issue(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
            "A managed update must preserve binding, provider, namespace, and consumer identity."));
      }
    } else if (currentSnapshot != null) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
          "Only an existing managed binding may supply a current snapshot."));
    }
  }

  private void validateIdentityPlan(Consumer consumer, Collection<Allocation> allocations,
      List<Issue> issues) {
    ManagedDependencyIdentity.Plan plan = consumer.identityPlan();
    String effectiveUser = consumer.identity().effectiveShortUser();
    if (!plan.plannedShortUser().equals(effectiveUser)
        || "hbase".equals(effectiveUser)
        || !Pattern.matches("[a-z_][a-z0-9_-]{0,31}", effectiveUser)) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
          "The HBase effective user must match the server-derived stable identity plan."));
      return;
    }
    if (allocations == null) {
      return;
    }
    for (Allocation allocation : allocations) {
      if (allocation != null && effectiveUser.equals(allocation.effectiveShortUser())
          && !plan.planFingerprint().equals(allocation.ownerPlanFingerprint())) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED,
            "The planned HBase effective user is already allocated to another managed or provider-local service."));
        return;
      }
    }
  }

  private void validateVersion(Consumer consumer, Provider provider, List<Issue> issues) {
    ManagedDependencyVersion consumerVersion = consumer.version();
    ManagedDependencyVersion providerVersion = provider.version();
    if (!isSupportedActiveVersion(consumerVersion)
        || !isSupportedActiveVersion(providerVersion)
        || consumerVersion.resolvedVersions().isEmpty()
        || providerVersion.resolvedVersions().isEmpty()
        || !consumerVersion.resolvedEquivalent(providerVersion)) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_VERSION_UNSUPPORTED,
          "Consumer and provider must use active BIGTOP-3.3.0 repositories with equal resolved version metadata."));
    }
    if (!providerVersion.clientFeatures().containsAll(consumerVersion.clientFeatures())
        || !provider.unsupportedFeatures().isEmpty()) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED,
          "The provider does not support every required HBase client feature: "
              + provider.unsupportedFeatures()));
    }
  }

  private void validateProviderReadiness(Provider provider, List<Issue> issues) {
    if (!provider.installed() || !provider.healthy()) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_PROVIDER_NOT_READY,
          "The provider service must be installed and healthy before snapshot approval."));
    }
  }

  private SecurityResolution validateSecurity(ManagedDependencyType type, Consumer consumer,
      Provider provider, ManagedDependencySecurityValidationContext securityContext,
      List<Issue> issues) {
    if (consumer.securityMode() != provider.securityMode()) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH,
          "Mixed secure and insecure consumer/provider bindings are not supported."));
      return null;
    }
    if (type == ManagedDependencyType.HDFS && !consumer.identity().hasPrivateFilesystemAuthority()) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED,
          "Managed HDFS namespaces require mode 0700 or a distinct per-consumer group with mode 0750."));
    }
    if (consumer.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
      if (securityContext != null && !securityContext.isEmpty()) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH,
            "Insecure bindings must not carry Kerberos policy or mapping proofs."));
      }
      return null;
    }
    if (consumer.realm().isBlank() || provider.realm().isBlank()
        || !consumer.realm().equals(provider.realm())) {
      issues.add(issue(ManagedDependencyErrorCode.CROSS_REALM_NOT_SUPPORTED,
          "Consumer and provider must use the same Kerberos realm; cross-realm bindings are unsupported."));
      return null;
    }
    if (!sameRealmSecureEnabled) {
      issues.add(issue(ManagedDependencyErrorCode.SAME_REALM_BINDING_NOT_AVAILABLE,
          "Same-realm managed bindings require the dedicated identity and ACL integration."));
      return null;
    }

    ManagedDependencyIdentity identity = consumer.identity();
    String expectedPrincipal = identity == null ? ""
        : identity.effectiveShortUser() + "/_HOST@" + consumer.realm();
    if (identity == null
        || "hbase".equals(identity.effectiveShortUser())
        || identity.authToLocalVerified()
        || identity.principalPatterns().isEmpty()
        || !Set.of(expectedPrincipal).equals(identity.principalPatterns())
        || type == ManagedDependencyType.HDFS && !identity.hasPrivateFilesystemAuthority()
        || type == ManagedDependencyType.ZOOKEEPER
            && (provider.zooKeeperEndpoint() == null
                || !provider.zooKeeperEndpoint().managedParentAclSupported()
                || !identity.privateZooKeeperAcl())) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED,
          "Secure bindings require a unique mapped HBase identity and private HDFS/ZooKeeper authority."));
      return null;
    }
    if (type == ManagedDependencyType.ZOOKEEPER
        && (provider.zooKeeperEndpoint() == null
            || !provider.zooKeeperEndpoint().kerberosRemoveHostFromPrincipal()
            || !provider.zooKeeperEndpoint().kerberosRemoveRealmFromPrincipal()
            || !"DEFAULT".equals(provider.zooKeeperEndpoint().kerberosAuthToLocalRules())
            || !Set.of(expectedPrincipal).equals(identity.principalPatterns()))) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED,
          "Secure ZooKeeper bindings require an exact unique consumer principal and supported server SASL mapping."));
      return null;
    }
    if (securityContext == null || securityContext.isEmpty()
        || securityContext.consumerMapping() == null
        || securityContext.providerPolicy() == null) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_MISSING,
          "Secure managed dependencies require current consumer mapping and provider policy proofs."));
      return null;
    }

    ManagedHBaseConsumerLocalMapping mapping = securityContext.consumerMapping();
    ManagedDependencyProviderSecurityProof providerSecurity = securityContext.providerPolicy();
    if (!consumer.realm().equals(mapping.realm())
        || !consumer.realm().equals(providerSecurity.realm())
        || !identity.effectiveShortUser().equals(mapping.proof().effectiveShortUser())
        || !expectedPrincipal.equals(mapping.rolePrincipalPattern())
        || !consumer.identityPlan().planFingerprint().equals(mapping.identityPlanFingerprint())
        || providerSecurity.type() != type) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED,
          "The security proofs do not match the approved consumer/provider identity plan."));
      return null;
    }

    try {
      ManagedDependencyPairSecurityProof pairSecurity;
      if (type == ManagedDependencyType.HDFS) {
        if (securityContext.hdfsPolicySource() == null) {
          issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_MISSING,
              "Secure HDFS validation requires the current provider auth-to-local policy source."));
          return null;
        }
        Policy policy = hdfsSecurityVerifier.inspectPolicy(securityContext.hdfsPolicySource());
        if (!ManagedDependencyProviderSecurityProof.forHdfs(policy).equals(providerSecurity)) {
          issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_HDFS_AUTH_TO_LOCAL_STALE,
              "The HDFS provider policy changed after preview; review the dependency again."));
          return null;
        }
        ConsumerPatternProof proof = hdfsSecurityVerifier.provePattern(
            securityContext.hdfsPolicySource(), policy,
            new ConsumerPatternInput(consumer.realm(), identity.effectiveShortUser(),
                expectedPrincipal));
        pairSecurity = ManagedDependencyPairSecurityProof.forHdfs(proof);
      } else {
        ZooKeeperEndpoint endpoint = provider.zooKeeperEndpoint();
        PolicySource source = new PolicySource(provider.realm(), endpoint.saslEnabled(),
            endpoint.saslServiceName(), endpoint.managedParentAclSupported(),
            endpoint.kerberosRemoveHostFromPrincipal(),
            endpoint.kerberosRemoveRealmFromPrincipal(), endpoint.kerberosAuthToLocalRules());
        ManagedZooKeeperSaslPolicyVerifier.Policy policy =
            zooKeeperSecurityVerifier.inspectPolicy(source);
        if (!ManagedDependencyProviderSecurityProof.forZooKeeper(policy).equals(providerSecurity)) {
          issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_ZOOKEEPER_AUTHORIZATION_STALE,
              "The ZooKeeper provider policy changed after preview; review the dependency again."));
          return null;
        }
        PairProof proof = zooKeeperSecurityVerifier.provePair(source, policy,
            new ConsumerInput(consumer.realm(), identity.effectiveShortUser(),
                expectedPrincipal, identity.effectiveShortUser()));
        pairSecurity = ManagedDependencyPairSecurityProof.forZooKeeper(proof);
      }
      return new SecurityResolution(mapping, providerSecurity, pairSecurity);
    } catch (ManagedDependencyIntegrationException error) {
      issues.add(issue(errorCode(error.getCode()), error.getMessage()));
      return null;
    }
  }

  private ManagedDependencyErrorCode errorCode(String value) {
    try {
      return ManagedDependencyErrorCode.valueOf(value);
    } catch (IllegalArgumentException error) {
      return ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED;
    }
  }

  private HdfsResolution validateHdfs(Provider provider, UUID bindingId, List<Issue> issues) {
    HdfsEndpoint endpoint = provider.hdfsEndpoint();
    if (endpoint == null || provider.zooKeeperEndpoint() != null) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_NAMENODE_INVALID,
          "An HDFS provider requires exactly one HDFS endpoint descriptor."));
      return null;
    }
    if (endpoint.federationEnabled() || endpoint.observerNameNodeEnabled()) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED,
          "Federation and Observer NameNode clients are not supported by managed bindings."));
      return null;
    }

    URI defaultFs;
    try {
      defaultFs = URI.create(endpoint.defaultFs());
    } catch (IllegalArgumentException e) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_NAMENODE_INVALID,
          "fs.defaultFS must be a valid HDFS URI."));
      return null;
    }
    if (!"hdfs".equals(defaultFs.getScheme())
        || defaultFs.getRawUserInfo() != null
        || defaultFs.getRawQuery() != null
        || defaultFs.getRawFragment() != null
        || defaultFs.getPath() != null && !defaultFs.getPath().isEmpty()) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_NAMENODE_INVALID,
          "fs.defaultFS must contain only an hdfs scheme and approved authority."));
      return null;
    }

    if (endpoint.highAvailability()) {
      validateHighAvailabilityEndpoint(endpoint, defaultFs, issues);
    } else {
      validateOrdinaryEndpoint(endpoint, defaultFs, issues);
    }
    validateHdfsConfigurationConsistency(provider, endpoint, issues);
    if (!issues.isEmpty()) {
      return null;
    }

    SortedMap<String, String> coreSite = filter(provider.coreSite(), CORE_SITE_ALLOWLIST, null);
    coreSite = mutableCopy(coreSite);
    coreSite.put("fs.defaultFS", endpoint.defaultFs());
    if (provider.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
      coreSite.put("ipc.client.fallback-to-simple-auth-allowed", "false");
    }
    coreSite = ManagedDependencyVersion.immutableSortedMap(coreSite);

    Set<String> dynamicHdfsKeys = endpoint.highAvailability()
        ? haKeys(endpoint.nameService(), endpoint.nameNodeRpcAddresses().keySet())
        : Set.of();
    TreeMap<String, String> hdfsSiteBuilder = mutableCopy(
        filter(provider.hdfsSite(), HDFS_SITE_ALLOWLIST, dynamicHdfsKeys));
    if (endpoint.highAvailability()) {
      hdfsSiteBuilder.put("dfs.nameservices", endpoint.nameService());
      hdfsSiteBuilder.put("dfs.ha.namenodes." + endpoint.nameService(),
          String.join(",", endpoint.nameNodeRpcAddresses().keySet()));
      endpoint.nameNodeRpcAddresses().forEach((nameNodeId, address) -> hdfsSiteBuilder.put(
          "dfs.namenode.rpc-address." + endpoint.nameService() + "." + nameNodeId, address));
      hdfsSiteBuilder.put("dfs.client.failover.proxy.provider." + endpoint.nameService(),
          endpoint.failoverProxyProvider());
    }
    SortedMap<String, String> hdfsSite = ManagedDependencyVersion.immutableSortedMap(hdfsSiteBuilder);
    validateHdfsClientValues(provider.securityMode(), coreSite, hdfsSite, issues);
    validateRequiredProperties(provider, union(CORE_SITE_ALLOWLIST, HDFS_SITE_ALLOWLIST, dynamicHdfsKeys), issues);
    if (!issues.isEmpty()) {
      return null;
    }
    return new HdfsResolution(ManagedDependencyNamespace.hdfs(bindingId, endpoint.defaultFs()),
        coreSite, hdfsSite);
  }

  private void validateOrdinaryEndpoint(HdfsEndpoint endpoint, URI defaultFs, List<Issue> issues) {
    if (defaultFs.getHost() == null || !HOST.matcher(defaultFs.getHost()).matches()
        || defaultFs.getPort() <= 0 || defaultFs.getPort() > 65535
        || !endpoint.nameService().isEmpty()
        || !endpoint.nameNodeRpcAddresses().isEmpty()
        || !endpoint.failoverProxyProvider().isEmpty()) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_NAMENODE_INVALID,
          "A non-HA provider requires hdfs://host:port and no HA metadata."));
    }
  }

  private void validateHighAvailabilityEndpoint(HdfsEndpoint endpoint, URI defaultFs,
      List<Issue> issues) {
    if (defaultFs.getAuthority() == null
        || defaultFs.getPort() != -1
        || endpoint.nameService().isBlank()
        || !HOST.matcher(endpoint.nameService()).matches()
        || !endpoint.nameService().equals(defaultFs.getAuthority())
        || endpoint.nameNodeRpcAddresses().size() < 2
        || !STANDARD_HA_FAILOVER_PROVIDER.equals(endpoint.failoverProxyProvider())) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_NAMENODE_INVALID,
          "HA requires one logical nameservice, at least two RPC addresses, and the standard failover provider."));
      return;
    }
    for (Map.Entry<String, String> entry : endpoint.nameNodeRpcAddresses().entrySet()) {
      if (entry.getKey().isBlank() || !validHostPort(entry.getValue())) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_NAMENODE_INVALID,
            "Every HA NameNode ID must resolve to a host:port RPC address."));
        return;
      }
    }
  }

  private void validateHdfsConfigurationConsistency(Provider provider, HdfsEndpoint endpoint,
      List<Issue> issues) {
    String configuredDefaultFs = provider.coreSite().get("fs.defaultFS");
    if (configuredDefaultFs != null && !configuredDefaultFs.equals(endpoint.defaultFs())) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "Resolved fs.defaultFS disagrees with the provider endpoint."));
    }
    String authentication = provider.coreSite().getOrDefault(
        "hadoop.security.authentication", "simple");
    String fallbackToSimple = provider.coreSite().get("ipc.client.fallback-to-simple-auth-allowed");
    if (provider.securityMode() == ManagedDependencySecurityMode.KERBEROS) {
      if (!"kerberos".equals(authentication)
          || isBlank(provider.hdfsSite().get("dfs.namenode.kerberos.principal"))
          || isBlank(provider.hdfsSite().get("dfs.datanode.kerberos.principal"))) {
        issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
            "A secure HDFS descriptor requires Kerberos authentication and NameNode/DataNode principals."));
      }
      if (fallbackToSimple != null && !"false".equals(fallbackToSimple)) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH,
            "Secure HDFS providers must disable fallback to simple authentication."));
      }
    } else if ("kerberos".equals(authentication)) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH,
          "An insecure HDFS descriptor cannot export Kerberos client authentication."));
    }
    String configuredNameServices = provider.hdfsSite().get("dfs.nameservices");
    if (!endpoint.highAvailability()) {
      if (configuredNameServices != null && !configuredNameServices.isBlank()) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED,
            "A non-HA snapshot cannot export nameservice or federation metadata."));
      }
      return;
    }
    if (configuredNameServices != null && !configuredNameServices.equals(endpoint.nameService())) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED,
          "The resolved nameservice list does not describe exactly one approved HA nameservice."));
    }
    String nameNodeIdsKey = "dfs.ha.namenodes." + endpoint.nameService();
    String configuredNameNodeIds = provider.hdfsSite().get(nameNodeIdsKey);
    if (configuredNameNodeIds != null
        && !commaSeparatedSet(configuredNameNodeIds).equals(endpoint.nameNodeRpcAddresses().keySet())) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "The resolved HA NameNode IDs disagree with the provider endpoint."));
    }
    for (Map.Entry<String, String> entry : endpoint.nameNodeRpcAddresses().entrySet()) {
      String key = "dfs.namenode.rpc-address." + endpoint.nameService() + "." + entry.getKey();
      if (provider.hdfsSite().containsKey(key) && !entry.getValue().equals(provider.hdfsSite().get(key))) {
        issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
            "The resolved HA RPC address disagrees with the provider endpoint: " + key));
      }
    }
    String failoverKey = "dfs.client.failover.proxy.provider." + endpoint.nameService();
    if (provider.hdfsSite().containsKey(failoverKey)
        && !endpoint.failoverProxyProvider().equals(provider.hdfsSite().get(failoverKey))) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "The resolved HA failover provider disagrees with the provider endpoint."));
    }
  }

  private ZooKeeperResolution validateZooKeeper(Provider provider, UUID bindingId,
      List<Issue> issues) {
    ZooKeeperEndpoint endpoint = provider.zooKeeperEndpoint();
    if (endpoint == null || provider.hdfsEndpoint() != null
        || endpoint.quorumHosts().isEmpty()
        || endpoint.clientPort() <= 0 || endpoint.clientPort() > 65535) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_ZOOKEEPER_INVALID,
          "A ZooKeeper provider requires a non-empty quorum and valid client port."));
      return null;
    }
    for (String host : endpoint.quorumHosts()) {
      if (host == null || !HOST.matcher(host).matches()) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_ZOOKEEPER_INVALID,
            "Every ZooKeeper quorum member must be a host name without embedded credentials or ports."));
        return null;
      }
    }
    for (String applicationParent : endpoint.configuredApplicationParents()) {
      if (ManagedDependencyNamespace.zooKeeperParentConflicts(applicationParent)) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_NAMESPACE_CONFLICT,
            "The reserved /ambari-managed-hbase root overlaps an existing application znode parent."));
        return null;
      }
    }
    if (provider.securityMode() == ManagedDependencySecurityMode.KERBEROS
        && (!endpoint.saslEnabled() || endpoint.saslServiceName().isBlank()
            || !endpoint.managedParentAclSupported())) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "A secure ZooKeeper descriptor requires SASL identity and managed-parent ACL support."));
    } else if (provider.securityMode() == ManagedDependencySecurityMode.INSECURE
        && endpoint.saslEnabled()) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH,
          "An insecure consumer cannot bind to a ZooKeeper endpoint that requires SASL."));
    }
    String configuredQuorum = provider.zooKeeperClient().get("hbase.zookeeper.quorum");
    if (configuredQuorum != null
        && !commaSeparatedSet(configuredQuorum).equals(Set.copyOf(endpoint.quorumHosts()))) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "The resolved ZooKeeper quorum disagrees with the provider endpoint."));
    }
    String configuredPort = provider.zooKeeperClient().get("hbase.zookeeper.property.clientPort");
    if (configuredPort != null && !configuredPort.equals(Integer.toString(endpoint.clientPort()))) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "The resolved ZooKeeper client port disagrees with the provider endpoint."));
    }
    String configuredSasl = provider.zooKeeperClient().get("zookeeper.sasl.client");
    String configuredSaslUser = provider.zooKeeperClient().get("zookeeper.sasl.client.username");
    String configuredSaslContext = provider.zooKeeperClient().get("zookeeper.sasl.clientconfig");
    if (configuredSasl != null && !configuredSasl.equals(Boolean.toString(endpoint.saslEnabled()))
        || configuredSaslUser != null && !configuredSaslUser.equals(endpoint.saslServiceName())
        || configuredSaslContext != null && !"Client".equals(configuredSaslContext)) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "The resolved ZooKeeper SASL settings disagree with the provider endpoint."));
    }
    validateRequiredProperties(provider, ZOOKEEPER_ALLOWLIST, issues);
    if (!issues.isEmpty()) {
      return null;
    }
    SortedMap<String, String> config = mutableCopy(filter(
        provider.zooKeeperClient(), ZOOKEEPER_ALLOWLIST, null));
    config.put("hbase.zookeeper.quorum", String.join(",", endpoint.quorumHosts()));
    config.put("hbase.zookeeper.property.clientPort", Integer.toString(endpoint.clientPort()));
    config.put("zookeeper.znode.parent", ManagedDependencyNamespace.zooKeeper(bindingId).znode());
    if (endpoint.saslEnabled()) {
      config.put("zookeeper.sasl.client", "true");
      config.put("zookeeper.sasl.client.username", endpoint.saslServiceName());
      config.put("zookeeper.sasl.clientconfig", "Client");
    }
    validateZooKeeperClientValues(config, issues);
    return new ZooKeeperResolution(ManagedDependencyNamespace.zooKeeper(bindingId),
        ManagedDependencyVersion.immutableSortedMap(config));
  }

  private void validateRequiredProperties(Provider provider, Set<String> allowed,
      List<Issue> issues) {
    Set<String> allowedWithZnode = union(allowed, Set.of("zookeeper.znode.parent"));
    for (String property : provider.requiredClientProperties()) {
      if (containsSecretFragment(property) || !allowedWithZnode.contains(property)) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED,
            "Required client property is not exportable: " + property));
      } else if (!isAvailableClientProperty(provider, property)) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED,
            "Required client property is missing from the resolved provider descriptor: " + property));
      }
    }
  }

  private void detectUnsupportedProviderFeatures(Provider provider, List<Issue> issues) {
    if (provider.type() == ManagedDependencyType.HDFS
        && "false".equalsIgnoreCase(provider.hdfsSite()
            .getOrDefault("dfs.permissions.enabled", "true").trim())) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED,
          "Managed HDFS namespaces require dfs.permissions.enabled=true"));
    }
    TreeMap<String, String> resolved = new TreeMap<>();
    resolved.putAll(provider.coreSite());
    resolved.putAll(provider.hdfsSite());
    resolved.putAll(provider.zooKeeperClient());
    for (Map.Entry<String, String> entry : resolved.entrySet()) {
      String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
      String value = entry.getValue().trim();
      boolean kms = (key.contains("key.provider") || key.contains("kms")) && !value.isEmpty();
      boolean secureZooKeeperTransport = Set.of(
          "hbase.zookeeper.property.client.secure", "zookeeper.client.secure").contains(key)
          && "true".equalsIgnoreCase(value);
      boolean customAuthentication = key.contains("authentication")
          && (key.equals("hadoop.security.authentication")
              ? !Set.of("simple", "kerberos").contains(value.toLowerCase(java.util.Locale.ROOT))
              : key.contains("class") || key.contains("provider"));
      if (kms || secureZooKeeperTransport || customAuthentication) {
        issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED,
            "Resolved provider client configuration requires an unsupported feature: " + entry.getKey()));
      }
    }
  }

  private void validateHdfsClientValues(ManagedDependencySecurityMode securityMode,
      Map<String, String> coreSite, Map<String, String> hdfsSite, List<Issue> issues) {
    validateEnum(coreSite, "hadoop.security.authentication", Set.of("simple", "kerberos"), issues);
    validateEnum(coreSite, "hadoop.rpc.protection",
        Set.of("authentication", "integrity", "privacy"), issues);
    validateBoolean(coreSite, "hadoop.security.authorization", issues);
    validateBoolean(coreSite, "ipc.client.fallback-to-simple-auth-allowed", issues);
    validateInteger(coreSite, "ipc.client.connect.max.retries", 0, 50, issues);
    validateInteger(coreSite, "ipc.client.connect.retry.interval", 100, 60_000, issues);
    validateBoolean(hdfsSite, "dfs.permissions.enabled", issues);
    validateBoolean(hdfsSite, "dfs.block.access.token.enable", issues);
    validateInteger(hdfsSite, "dfs.client.failover.connection.retries", 0, 100, issues);
    validateInteger(hdfsSite, "dfs.client.failover.connection.retries.on.timeouts", 0, 100, issues);
    validateInteger(hdfsSite, "dfs.client.socket-timeout", 1_000, 600_000, issues);
    validateBoolean(hdfsSite, "dfs.client.use.datanode.hostname", issues);
    validateProtectionList(hdfsSite, "dfs.data.transfer.protection", issues);
    validateBoolean(hdfsSite, "dfs.encrypt.data.transfer", issues);
    if (securityMode == ManagedDependencySecurityMode.KERBEROS
        && !"false".equals(coreSite.get("ipc.client.fallback-to-simple-auth-allowed"))) {
      issues.add(issue(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH,
          "Secure HDFS snapshots require ipc.client.fallback-to-simple-auth-allowed=false."));
    }
  }

  private void validateZooKeeperClientValues(Map<String, String> config, List<Issue> issues) {
    validateBoolean(config, "zookeeper.sasl.client", issues);
    String context = config.get("zookeeper.sasl.clientconfig");
    if (context != null && !"Client".equals(context)) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "zookeeper.sasl.clientconfig must use the supported Client login context."));
    }
    String service = config.get("zookeeper.sasl.client.username");
    if (service != null && !Pattern.matches("[A-Za-z0-9._-]+", service)) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "zookeeper.sasl.client.username must be a valid service primary."));
    }
  }

  private void validateBoolean(Map<String, String> values, String key, List<Issue> issues) {
    String value = values.get(key);
    if (value != null && !Set.of("true", "false").contains(value)) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          key + " must be an explicit lowercase boolean."));
    }
  }

  private void validateEnum(Map<String, String> values, String key, Set<String> allowed,
      List<Issue> issues) {
    String value = values.get(key);
    if (value != null && !allowed.contains(value)) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          key + " has an unsupported value."));
    }
  }

  private void validateProtectionList(Map<String, String> values, String key, List<Issue> issues) {
    String value = values.get(key);
    if (value == null) {
      return;
    }
    Set<String> allowed = Set.of("authentication", "integrity", "privacy");
    for (String protection : value.split(",")) {
      if (!allowed.contains(protection.trim())) {
        issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
            key + " contains an unsupported protection level."));
        return;
      }
    }
  }

  private void validateInteger(Map<String, String> values, String key, int minimum,
      int maximum, List<Issue> issues) {
    String value = values.get(key);
    if (value == null) {
      return;
    }
    try {
      int parsed = Integer.parseInt(value);
      if (parsed < minimum || parsed > maximum) {
        issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
            key + " must be between " + minimum + " and " + maximum + "."));
      }
    } catch (NumberFormatException e) {
      issues.add(issue(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          key + " must be a bounded integer."));
    }
  }

  private boolean isAvailableClientProperty(Provider provider, String property) {
    if (provider.coreSite().containsKey(property)
        || provider.hdfsSite().containsKey(property)
        || provider.zooKeeperClient().containsKey(property)) {
      return true;
    }
    if ("fs.defaultFS".equals(property)) {
      return provider.hdfsEndpoint() != null;
    }
    if (Set.of("hbase.zookeeper.quorum", "hbase.zookeeper.property.clientPort",
        "zookeeper.znode.parent").contains(property)) {
      return provider.zooKeeperEndpoint() != null;
    }
    HdfsEndpoint endpoint = provider.hdfsEndpoint();
    return endpoint != null && endpoint.highAvailability()
        && haKeys(endpoint.nameService(), endpoint.nameNodeRpcAddresses().keySet()).contains(property);
  }

  private String providerFingerprint(UUID bindingId, Provider provider,
      ManagedDependencyNamespace namespace, Map<String, String> coreSite,
      Map<String, String> hdfsSite, Map<String, String> zooKeeperClient,
      ManagedDependencyProviderSecurityProof providerSecurity) {
    return hash("provider", bindingId.toString(), provider.type().name(),
        Long.toString(provider.serviceKey().clusterId()), provider.serviceKey().serviceName(),
        canonicalVersion(provider.version()), provider.securityMode().name(), provider.realm(),
        canonicalIdentity(provider.identity()), namespace.rootUri(), namespace.walUri(), namespace.znode(),
        Boolean.toString(provider.zooKeeperEndpoint() != null
            && provider.zooKeeperEndpoint().managedParentAclSupported()),
        Boolean.toString(provider.zooKeeperEndpoint() != null
            && provider.zooKeeperEndpoint().kerberosRemoveHostFromPrincipal()),
        Boolean.toString(provider.zooKeeperEndpoint() != null
            && provider.zooKeeperEndpoint().kerberosRemoveRealmFromPrincipal()),
        canonicalMap(coreSite), canonicalMap(hdfsSite), canonicalMap(zooKeeperClient),
        providerSecurity == null ? "" : providerSecurity.policyFingerprint());
  }

  private static String canonicalVersion(ManagedDependencyVersion version) {
    return canonicalValues(version.stackName(), version.stackVersion(), version.serviceVersion(),
        Boolean.toString(version.active()), canonicalMap(version.resolvedVersions()),
        canonicalValues(version.clientFeatures().toArray(String[]::new)));
  }

  private static String canonicalIdentity(ManagedDependencyIdentity identity) {
    if (identity == null) {
      return "";
    }
    return canonicalValues(identity.effectiveShortUser(),
        canonicalValues(identity.principalPatterns().toArray(String[]::new)),
        identity.filesystemGroup(),
        Boolean.toString(identity.distinctConsumerGroup()), identity.directoryMode(),
        Boolean.toString(identity.privateZooKeeperAcl()));
  }

  private static String canonicalMap(Map<String, String> values) {
    StringBuilder canonical = new StringBuilder();
    values.forEach((key, value) -> append(canonical, key, value));
    return canonical.toString();
  }

  private static String canonicalValues(String... values) {
    StringBuilder canonical = new StringBuilder();
    for (String value : values) {
      append(canonical, value);
    }
    return canonical.toString();
  }

  private static void append(StringBuilder target, String... values) {
    for (String value : values) {
      String actual = value == null ? "" : value;
      target.append(actual.length()).append(':').append(actual);
    }
  }

  private static String hash(String... values) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(canonicalValues("managed-dependency-fingerprint",
          Integer.toString(FINGERPRINT_SCHEMA_VERSION), canonicalValues(values))
          .getBytes(StandardCharsets.UTF_8));
      return "sha256:" + HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
    }
  }

  private static boolean isSupportedActiveVersion(ManagedDependencyVersion version) {
    return version.active()
        && SUPPORTED_STACK_NAME.equals(version.stackName())
        && SUPPORTED_STACK_VERSION.equals(version.stackVersion());
  }

  private static SortedMap<String, String> filter(Map<String, String> source,
      Set<String> fixedKeys, Set<String> dynamicKeys) {
    TreeMap<String, String> result = new TreeMap<>();
    source.forEach((key, value) -> {
      if (!containsSecretFragment(key)
          && (fixedKeys.contains(key) || dynamicKeys != null && dynamicKeys.contains(key))) {
        result.put(key, value);
      }
    });
    return Collections.unmodifiableSortedMap(result);
  }

  private static boolean containsSecretFragment(String property) {
    String lower = property.toLowerCase(java.util.Locale.ROOT);
    return SECRET_FRAGMENTS.stream().anyMatch(lower::contains);
  }

  private static Set<String> haKeys(String nameService, Collection<String> nameNodeIds) {
    java.util.HashSet<String> keys = new java.util.HashSet<>();
    keys.add("dfs.ha.namenodes." + nameService);
    keys.add("dfs.client.failover.proxy.provider." + nameService);
    for (String nameNodeId : nameNodeIds) {
      keys.add("dfs.namenode.rpc-address." + nameService + "." + nameNodeId);
    }
    return Set.copyOf(keys);
  }

  @SafeVarargs
  private static Set<String> union(Set<String>... sets) {
    java.util.HashSet<String> result = new java.util.HashSet<>();
    for (Set<String> set : sets) {
      result.addAll(set);
    }
    return Set.copyOf(result);
  }

  private static boolean validHostPort(String value) {
    try {
      URI uri = URI.create("hdfs://" + value);
      return uri.getHost() != null && HOST.matcher(uri.getHost()).matches()
          && uri.getPort() > 0 && uri.getPort() <= 65535
          && uri.getRawUserInfo() == null && (uri.getPath() == null || uri.getPath().isEmpty());
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static Set<String> commaSeparatedSet(String value) {
    java.util.HashSet<String> values = new java.util.HashSet<>();
    for (String entry : value.split(",")) {
      values.add(entry.trim());
    }
    return Set.copyOf(values);
  }

  private static TreeMap<String, String> mutableCopy(Map<String, String> values) {
    return new TreeMap<>(values);
  }

  private static Issue issue(ManagedDependencyErrorCode code, String message) {
    return new Issue(code, message);
  }

  public record Issue(ManagedDependencyErrorCode code, String message) {
    public Issue {
      code = Objects.requireNonNull(code, "code");
      message = Objects.requireNonNull(message, "message");
    }
  }

  public record ValidationResult(Optional<ManagedDependencySnapshot> snapshot, List<Issue> issues) {
    public ValidationResult {
      snapshot = Objects.requireNonNull(snapshot, "snapshot");
      issues = List.copyOf(issues);
      if (snapshot.isPresent() == !issues.isEmpty()) {
        throw new IllegalArgumentException("a validation result must contain either a snapshot or issues");
      }
    }

    public static ValidationResult success(ManagedDependencySnapshot snapshot) {
      return new ValidationResult(Optional.of(snapshot), List.of());
    }

    public static ValidationResult failure(Collection<Issue> issues) {
      return new ValidationResult(Optional.empty(), List.copyOf(issues));
    }

    public boolean isValid() {
      return snapshot.isPresent();
    }

    public boolean hasError(ManagedDependencyErrorCode errorCode) {
      return issues.stream().anyMatch(issue -> issue.code() == errorCode);
    }
  }

  private record HdfsResolution(
      ManagedDependencyNamespace namespace,
      SortedMap<String, String> coreSite,
      SortedMap<String, String> hdfsSite) {
  }

  private record ZooKeeperResolution(
      ManagedDependencyNamespace namespace,
      SortedMap<String, String> clientConfig) {
  }

  private record SecurityResolution(
      ManagedHBaseConsumerLocalMapping consumerMapping,
      ManagedDependencyProviderSecurityProof providerSecurity,
      ManagedDependencyPairSecurityProof pairSecurity) {
  }
}
