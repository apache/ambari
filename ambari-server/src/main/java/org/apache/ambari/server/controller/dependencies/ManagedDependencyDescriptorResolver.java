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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.PersistKeyValueImpl;
import org.apache.ambari.server.api.services.ScopedWorkflowState;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ConsumerLifecycle;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.HdfsEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ZooKeeperEndpoint;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec
    .ManagedBindingSnapshotRef;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseSecurityDescriptorAdapter;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.PolicySource;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Resolves trusted live and draft facts consumed by the pure snapshot validator. */
@Singleton
public class ManagedDependencyDescriptorResolver implements ManagedDependencyDescriptor.Resolver {
  static final String STANDARD_RPC_CLIENT = "STANDARD_RPC_CLIENT";

  /** One server-owned binding selection in a complete security calculation. */
  record BindingSelection(UUID bindingId, ManagedDependencyServiceKey providerServiceKey) {
    BindingSelection {
      bindingId = Objects.requireNonNull(bindingId, "bindingId");
      providerServiceKey = Objects.requireNonNull(providerServiceKey, "providerServiceKey");
    }
  }

  /** Immutable one- or two-provider selection passed to the transient security producer. */
  record CompleteSelection(SortedMap<ManagedDependencyType, BindingSelection> bindings) {
    CompleteSelection {
      Objects.requireNonNull(bindings, "bindings");
      if (bindings.isEmpty() || bindings.size() > ManagedDependencyType.values().length) {
        throw new IllegalArgumentException("one or two managed provider selections are required");
      }
      TreeMap<ManagedDependencyType, BindingSelection> copy = new TreeMap<>();
      Set<UUID> ids = new java.util.HashSet<>();
      for (Map.Entry<ManagedDependencyType, BindingSelection> entry : bindings.entrySet()) {
        ManagedDependencyType type = Objects.requireNonNull(entry.getKey(), "selection type");
        BindingSelection selection = Objects.requireNonNull(entry.getValue(), "selection");
        if (!ids.add(selection.bindingId())
            || !type.getProviderServiceName().equals(selection.providerServiceKey().serviceName())) {
          throw new IllegalArgumentException("provider selections must have unique UUIDs and matching types");
        }
        copy.put(type, selection);
      }
      bindings = Collections.unmodifiableSortedMap(copy);
    }
  }

  private final Clusters clusters;
  private final AmbariMetaInfo metaInfo;
  private final RepositoryVersionDAO repositoryVersionDAO;
  private final ServiceDependencyDAO dependencyDAO;
  private final PersistKeyValueImpl persistKeyValue;
  private final com.google.inject.Provider<AmbariManagementController> managementController;
  private final com.google.inject.Provider<ManagedHBaseSecurityDescriptorAdapter> securityAdapter;

  @Inject
  public ManagedDependencyDescriptorResolver(Clusters clusters, AmbariMetaInfo metaInfo,
      RepositoryVersionDAO repositoryVersionDAO, ServiceDependencyDAO dependencyDAO,
      PersistKeyValueImpl persistKeyValue,
      com.google.inject.Provider<AmbariManagementController> managementController,
      com.google.inject.Provider<ManagedHBaseSecurityDescriptorAdapter> securityAdapter) {
    this.clusters = clusters;
    this.metaInfo = metaInfo;
    this.repositoryVersionDAO = repositoryVersionDAO;
    this.dependencyDAO = dependencyDAO;
    this.persistKeyValue = persistKeyValue;
    this.managementController = managementController;
    this.securityAdapter = securityAdapter;
  }

  public ManagedDependencyDescriptorResolver(Clusters clusters, AmbariMetaInfo metaInfo,
      RepositoryVersionDAO repositoryVersionDAO, ServiceDependencyDAO dependencyDAO,
      PersistKeyValueImpl persistKeyValue) {
    this(clusters, metaInfo, repositoryVersionDAO, dependencyDAO, persistKeyValue, null, null);
  }

  @Override
  public Consumer resolveDraft(UUID draftId, long expectedRevision) {
    if (expectedRevision < 1) {
      throw invalid("INVALID_DRAFT_REVISION", "A positive current draft revision is required.");
    }
    final ScopedWorkflowState draft;
    try {
      draft = persistKeyValue.getScopedState("drafts", draftId.toString(), false);
    } catch (AuthorizationException e) {
      throw new ManagedDependencyIntegrationException(403, "DEPENDENCY_AUTHORIZATION_FAILED",
          "The authenticated user cannot read this creation draft.", e);
    }
    if (!"CLUSTER_CREATE".equals(draft.getWorkflow()) || draft.getRevision() != expectedRevision) {
      throw new ManagedDependencyIntegrationException(409, "DRAFT_REVISION_CONFLICT",
          "Reload the active cluster-creation draft before previewing dependencies.");
    }

    Map<String, Object> steps = mapAt(draft.getValues(), "state", "clusterCreationSteps");
    Map<String, Object> versionData = mapAt(steps, "VERSION", "data");
    Map<String, Object> selectedStack = mapAt(versionData, "selectedStack");
    Map<String, Object> selectedVersion = mapAt(versionData, "selectedVersion");
    String stackName = requiredText(selectedStack, "stack_name");
    String stackVersion = firstPresentText(selectedStack, "stack_version",
        selectedVersion, "stack_version");
    String repositoryVersion = firstPresentText(selectedVersion, "repository_version",
        selectedVersion, "id");
    Map<String, Object> selectedServices = mapAt(steps, "SERVICES", "data", "services");
    if (!isSelected(selectedServices.get("HBASE"))) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR",
          "The current creation draft does not select HBASE.");
    }

    StackId stackId = new StackId(stackName, stackVersion);
    RepositoryVersionEntity repository = repositoryVersionDAO.findByStackAndVersion(
        stackId, repositoryVersion);
    int ownerUserId = AuthorizationHelper.getAuthenticatedId();
    if (ownerUserId <= 0) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR",
          "An active authenticated draft owner is required.");
    }
    ManagedDependencyIdentity.Plan plan = ManagedDependencyIdentity.Plan.forCreationDraft(
        ownerUserId, draftId);
    boolean secure = isSelected(selectedServices.get("KERBEROS"));
    if (secure) {
      throw invalid("DEPENDENCY_SECURITY_PLAN_INCOMPLETE",
          "Persist the effective Kerberos realm before approving a secure managed dependency.");
    }
    ManagedDependencyIdentity identity = identity(plan, secure, "");
    return new Consumer("DRAFT", draftId, null, null, "HBASE", ConsumerLifecycle.DRAFT,
        repository == null
            ? versionFromAvailableDefinition(selectedStack, stackId, repositoryVersion, "HBASE")
            : version(repository, stackName, stackVersion, "HBASE"), security(secure), "",
        identity, plan);
  }

  /** Resolves the planned HBASE descriptor before the Add Service workflow creates it. */
  public Consumer resolveServicePlan(long clusterId, long expectedRevision) {
    Cluster cluster = cluster(clusterId);
    if (cluster.getServices().containsKey("HBASE")) {
      throw new ManagedDependencyIntegrationException(409,
          "DEPENDENCY_SERVICE_PLAN_MATERIALIZED",
          "HBASE already exists; repeat preview against the live service.");
    }

    final ScopedWorkflowState workflow;
    try {
      workflow = persistKeyValue.getActiveOwnedClusterWorkflowState(
          clusterId, "ADD_SERVICE", expectedRevision);
    } catch (AuthorizationException e) {
      throw new ManagedDependencyIntegrationException(403, "DEPENDENCY_AUTHORIZATION_FAILED",
          "The authenticated user cannot read this Add Service plan.", e);
    }
    Map<String, Object> services = mapAt(workflow.getValues(),
        "ADD_SERVICE", "addServiceSteps", "SERVICES", "data", "services");
    if (!isSelected(services.get("HBASE"))) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR",
          "The current Add Service checkpoint does not select HBASE.");
    }

    ManagedDependencyIdentity.Plan plan = identityPlan(cluster);
    boolean secure = cluster.getSecurityType() == SecurityType.KERBEROS;
    String realm = property(cluster, "kerberos-env", "realm", "");
    ManagedDependencyIdentity identity = identity(plan, secure, realm);
    return new Consumer("SERVICE_PLAN", null, cluster.getClusterId(), cluster.getClusterName(),
        "HBASE", ConsumerLifecycle.DRAFT, versionForServicePlan(cluster), security(secure),
        realm, identity, plan);
  }

  @Override
  public Consumer resolveService(ManagedDependencyServiceKey serviceKey) {
    return resolveService(serviceKey, null);
  }

  /**
   * Resolves a live HBASE descriptor with lifecycle classification for one selected type.
   * A new type on a fresh INIT service is distinct from an existing managed type.
   */
  public Consumer resolveService(ManagedDependencyServiceKey serviceKey,
      ManagedDependencyType selectedType) {
    if (!"HBASE".equals(serviceKey.serviceName())) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR", "Managed dependency consumers must be HBASE.");
    }
    Cluster cluster = cluster(serviceKey.clusterId());
    Service service = service(cluster, "HBASE");
    List<ServiceDependencyBindingEntity> bindings = dependencyDAO.findByConsumer(
        cluster.getClusterId(), "HBASE");
    boolean managedType = selectedType == null
        ? !bindings.isEmpty()
        : bindings.stream().anyMatch(
            binding -> selectedType.name().equals(binding.getDependencyType()));
    ConsumerLifecycle lifecycle = managedType
        ? ConsumerLifecycle.MANAGED_UPDATE
        : (isFresh(service) ? ConsumerLifecycle.INIT_UNINSTALLED : ConsumerLifecycle.INSTALLED_LOCAL);
    ManagedDependencyIdentity.Plan plan = identityPlan(cluster);
    boolean secure = cluster.getSecurityType() == SecurityType.KERBEROS;
    String realm = property(cluster, "kerberos-env", "realm", "");
    ManagedDependencyIdentity identity = identity(plan, secure, realm);
    return new Consumer("SERVICE", null, cluster.getClusterId(), cluster.getClusterName(), "HBASE",
        lifecycle, version(cluster, service, "HBASE", !isFresh(service)),
        security(secure), realm, identity, plan);
  }

  public Provider resolveProvider(ManagedDependencyServiceKey serviceKey) {
    ManagedDependencyType type;
    try {
      type = ManagedDependencyType.valueOf(serviceKey.serviceName());
    } catch (IllegalArgumentException e) {
      throw invalid("INVALID_PROVIDER_DESCRIPTOR", "Provider service must be HDFS or ZOOKEEPER.");
    }
    Cluster cluster = cluster(serviceKey.clusterId());
    Service service = service(cluster, serviceKey.serviceName());
    validateProviderOverrides(cluster, service, type);
    boolean secure = cluster.getSecurityType() == SecurityType.KERBEROS;
    String realm = property(cluster, "kerberos-env", "realm", "");
    ManagedDependencyIdentity providerIdentity = secure
        ? new ManagedDependencyIdentity(serviceKey.serviceName().toLowerCase(), new TreeSet<>(), false,
            "", false, "0700", false)
        : null;
    if (type == ManagedDependencyType.HDFS) {
      Map<String, String> coreSite = properties(cluster, "core-site");
      Map<String, String> hdfsSite = properties(cluster, "hdfs-site");
      Set<String> required = requiredHdfsClientProperties(coreSite, hdfsSite);
      Set<String> unsupported = unsupportedHdfsFeatures(coreSite, hdfsSite);
      return new Provider(serviceKey, type,
          version(cluster, service, "HDFS", true),
          installed(service), healthy(service), security(secure), realm, providerIdentity,
          hdfsEndpoint(coreSite, hdfsSite), null, coreSite, hdfsSite, Map.of(), required, unsupported);
    }

    Map<String, String> zooCfg = properties(cluster, "zoo.cfg");
    Map<String, String> hbaseSite = properties(cluster, "hbase-site");
    Map<String, String> zooKeeperEnv = properties(cluster, "zookeeper-env");
    List<String> hosts = service.getServiceComponents().values().stream()
        .filter(component -> "ZOOKEEPER_SERVER".equals(component.getName()))
        .flatMap(component -> component.getServiceComponentHosts().keySet().stream())
        .sorted().distinct().toList();
    int port = integer(zooCfg.get("clientPort"));
    boolean sasl = secure;
    String saslName = secure
        ? kerberosServiceName(zooKeeperEnv.get("zookeeper_principal_name")) : "";
    List<String> applicationParents = new ArrayList<>();
    String applicationParent = hbaseSite.get("zookeeper.znode.parent");
    if (applicationParent != null && !applicationParent.isBlank()) {
      applicationParents.add(applicationParent);
    }
    Map<String, String> client = new LinkedHashMap<>();
    client.put("hbase.zookeeper.quorum", String.join(",", hosts));
    client.put("hbase.zookeeper.property.clientPort", Integer.toString(port));
    if (sasl) {
      client.put("zookeeper.sasl.client", "true");
      client.put("zookeeper.sasl.client.username", saslName);
      client.put("zookeeper.sasl.clientconfig", "Client");
    }
    return new Provider(serviceKey, type,
        version(cluster, service, "ZOOKEEPER", true),
        installed(service), healthy(service), security(secure), realm, providerIdentity, null,
        new ZooKeeperEndpoint(hosts, port, sasl, saslName, secure, applicationParents,
            Boolean.parseBoolean(zooCfg.getOrDefault("kerberos.removeHostFromPrincipal", "false")),
            Boolean.parseBoolean(zooCfg.getOrDefault("kerberos.removeRealmFromPrincipal", "false")),
            zooCfg.getOrDefault("security.auth_to_local", "DEFAULT")),
        Map.of(), Map.of(), client, Set.copyOf(client.keySet()),
        unsupportedZooKeeperFeatures(zooCfg, secure));
  }

  /**
   * Produces the transient HDFS security policy consumed by the security adapter.
   * The caller must hold the canonical provider-cluster read lock and must have
   * authorized the consumer and every selected provider before invoking this seam.
   *
   * @param providerCluster the provider cluster whose current desired facts are read
   * @param resolvedProvider the provider descriptor resolved under the same lock
   * @return a task-private policy source; it is never a REST or persistence model
   */
  PolicySource resolveHdfsPolicySource(Cluster providerCluster, Provider resolvedProvider) {
    Objects.requireNonNull(providerCluster, "providerCluster");
    Objects.requireNonNull(resolvedProvider, "resolvedProvider");
    if (resolvedProvider.type() != ManagedDependencyType.HDFS
        || !"HDFS".equals(resolvedProvider.serviceKey().serviceName())
        || resolvedProvider.serviceKey().clusterId() != providerCluster.getClusterId()) {
      throw invalid("INVALID_PROVIDER_DESCRIPTOR",
          "The HDFS policy source must match the resolved HDFS provider cluster.");
    }
    if (providerCluster.getSecurityType() != SecurityType.KERBEROS
        || resolvedProvider.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
      throw invalid("DEPENDENCY_SECURITY_MISMATCH",
          "The HDFS policy source requires a Kerberized provider.");
    }
    StackId desiredStack = providerCluster.getDesiredStackVersion();
    StackId currentStack = providerCluster.getCurrentStackVersion();
    if (desiredStack == null || currentStack == null || !desiredStack.equals(currentStack)
        || providerCluster.getUpgradeInProgress() != null
        || !desiredStack.getStackName().equals(resolvedProvider.version().stackName())
        || !desiredStack.getStackVersion().equals(resolvedProvider.version().stackVersion())) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "The HDFS provider stack must be stable while its policy is resolved.");
    }

    validateProviderOverrides(providerCluster, service(providerCluster, "HDFS"),
        ManagedDependencyType.HDFS);
    Map<String, String> coreSite = properties(providerCluster, "core-site");
    Map<String, String> kerberosEnv = properties(providerCluster, "kerberos-env");
    Map<String, String> krb5Conf = properties(providerCluster, "krb5-conf");
    String kerberosEnvRealm = valueOrEmpty(kerberosEnv, "realm");
    if (!Objects.equals(resolvedProvider.realm(), kerberosEnvRealm)) {
      throw invalid("CROSS_REALM_NOT_SUPPORTED",
          "The resolved HDFS provider realm changed while its policy was being read.");
    }

    boolean managesRules = strictBoolean(kerberosEnv, "manage_auth_to_local",
        "kerberos-env/manage_auth_to_local", true);
    boolean managesKrb5 = strictBoolean(krb5Conf, "manage_krb5_conf",
        "krb5-conf/manage_krb5_conf", true);
    String krb5Realm = explicitValue(krb5Conf, "realm");
    String krb5Directory = krb5Conf.containsKey("conf_dir")
        ? requireValue(krb5Conf, "conf_dir", "krb5-conf/conf_dir") : "/etc";
    String template = explicitValue(krb5Conf, "content");
    String expectedTemplateFingerprint = expectedStockKrb5Template(providerCluster);
    return new PolicySource(coreSite.get("hadoop.security.auth_to_local"),
        coreSite.get("hadoop.security.auth_to_local.mechanism"), managesRules,
        kerberosEnvRealm, managesKrb5, krb5Conf.containsKey("realm") ? krb5Realm : null,
        krb5Directory, template, expectedTemplateFingerprint);
  }

  private String expectedStockKrb5Template(Cluster providerCluster) {
    StackId stackId = providerCluster.getDesiredStackVersion();
    if (stackId == null) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "The provider has no active stack for its stock krb5.conf template.");
    }
    final Set<PropertyInfo> properties;
    try {
      properties = metaInfo.getServiceProperties(stackId.getStackName(), stackId.getStackVersion(),
          "KERBEROS");
    } catch (AmbariException e) {
      throw new ManagedDependencyIntegrationException(422,
          "DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "The active stack does not expose its stock krb5.conf template.", e);
    }
    if (properties == null) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "The active stack does not expose its stock krb5.conf template.");
    }
    List<PropertyInfo> stockTemplates = properties.stream()
        .filter(Objects::nonNull)
        .filter(property -> "krb5-conf.xml".equals(property.getFilename()))
        .filter(property -> "content".equals(property.getName()))
        .filter(property -> property.getPropertyTypes() != null
            && property.getPropertyTypes().contains(
                PropertyInfo.PropertyType.VALUE_FROM_PROPERTY_FILE))
        .toList();
    if (stockTemplates.size() != 1 || stockTemplates.get(0).getValue() == null) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN",
          "The active stack must expose exactly one stock krb5.conf template.");
    }
    return new ManagedHdfsAuthToLocalVerifier().fingerprintTemplate(stockTemplates.get(0).getValue());
  }

  private boolean strictBoolean(Map<String, String> values, String key, String path,
      boolean defaultValue) {
    if (!values.containsKey(key)) {
      return defaultValue;
    }
    String value = values.get(key);
    if (value == null) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN", path + " must be true or false.");
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if ("true".equals(normalized)) {
      return true;
    }
    if ("false".equals(normalized)) {
      return false;
    }
    throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN", path + " must be true or false.");
  }

  private String explicitValue(Map<String, String> values, String key) {
    return values.containsKey(key) ? values.get(key) : "";
  }

  private String requireValue(Map<String, String> values, String key, String path) {
    String value = values.get(key);
    if (value == null) {
      throw invalid("DEPENDENCY_KRB5_DEFAULT_REALM_UNPROVEN", path + " must not be null.");
    }
    return value;
  }

  private String valueOrEmpty(Map<String, String> values, String key) {
    String value = values.get(key);
    return value == null ? "" : value;
  }

  /**
   * Produces one request-local security resolution for the complete provider selection.
   * The caller must authorize the consumer and every provider and hold canonical parent
   * read locks for all participating clusters before invoking this method. A service-plan
   * caller must pass the exact positive revision of its owned ADD_SERVICE checkpoint;
   * live callers must pass {@code null}.
   *
   * @param consumerCluster the consumer cluster whose trusted configurations are read
   * @param consumer the resolver-produced live or service-plan consumer descriptor
   * @param selection the complete immutable one- or two-provider selection
   * @param expectedServicePlanRevision exact owned ADD_SERVICE revision, or {@code null} for live
   * @return the transient adapter result; no raw descriptor or policy is a wire model
   */
  ManagedHBaseSecurityDescriptorAdapter.Resolution resolveSecureSelection(
      Cluster consumerCluster, Consumer consumer, CompleteSelection selection,
      Long expectedServicePlanRevision) {
    Objects.requireNonNull(consumerCluster, "consumerCluster");
    Objects.requireNonNull(consumer, "consumer");
    Objects.requireNonNull(selection, "selection");
    if (securityAdapter == null || managementController == null) {
      throw invalid("DEPENDENCY_SECURITY_PROOF_MISSING",
          "The authoritative managed security producer is not available.");
    }
    if (consumer.clusterId() == null || consumer.clusterId() != consumerCluster.getClusterId()
        || !"HBASE".equals(consumer.serviceName())) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR",
          "The managed security consumer does not belong to the target HBASE cluster.");
    }
    if (consumer.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
      throw invalid("DEPENDENCY_SECURITY_MISMATCH",
          "The complete managed security selection requires a Kerberized consumer.");
    }
    if ("DRAFT".equals(consumer.sourceScope())) {
      throw invalid("DEPENDENCY_SECURITY_PLAN_INCOMPLETE",
          "A secure creation draft has no authoritative cluster realm or service topology.");
    }
    boolean servicePlan = "SERVICE_PLAN".equals(consumer.sourceScope());
    if (servicePlan && (expectedServicePlanRevision == null
        || expectedServicePlanRevision < 1)) {
      throw new ManagedDependencyIntegrationException(409, "WORKFLOW_VERSION_CONFLICT",
          "Reload the active Add Service checkpoint before previewing dependencies.");
    }
    if (!servicePlan && !"SERVICE".equals(consumer.sourceScope())) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR",
          "The complete managed security selection requires a live or service-plan consumer.");
    }
    if (!servicePlan && expectedServicePlanRevision != null) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "A service-plan revision cannot be supplied for a live consumer.");
    }

    Map<String, Object> selectedServices = servicePlan
        ? selectedAddServiceServices(consumerCluster, expectedServicePlanRevision) : null;

    SortedMap<ManagedDependencyType, Provider> providers = new TreeMap<>();
    PolicySource hdfsPolicy = null;
    for (Map.Entry<ManagedDependencyType, BindingSelection> entry : selection.bindings().entrySet()) {
      Provider provider = resolveProvider(entry.getValue().providerServiceKey());
      providers.put(entry.getKey(), provider);
      if (entry.getKey() == ManagedDependencyType.HDFS) {
        hdfsPolicy = resolveHdfsPolicySource(
            cluster(provider.serviceKey().clusterId()), provider);
      }
    }

    StackId stackId = consumerCluster.getDesiredStackVersion();
    if (stackId == null || !stackId.equals(consumerCluster.getCurrentStackVersion())
        || consumerCluster.getUpgradeInProgress() != null
        || !stackId.getStackName().equals(consumer.version().stackName())
        || !stackId.getStackVersion().equals(consumer.version().stackVersion())) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "The consumer descriptor does not match the stable active cluster stack.");
    }
    try {
      // Command helpers depend on this resolver through the runtime planner.
      // Resolve their controller and Kerberos adapter only for a live calculation.
      AmbariManagementController managementController = this.managementController.get();
      ManagedHBaseSecurityDescriptorAdapter securityAdapter = this.securityAdapter.get();
      KerberosHelper kerberosHelper = managementController.getKerberosHelper();
      if (kerberosHelper == null || managementController.getConfigHelper() == null) {
        throw invalid("DEPENDENCY_SECURITY_PROOF_MISSING",
            "The authoritative Kerberos and configuration services are unavailable.");
      }
      KerberosDescriptor userDescriptor = kerberosHelper.getKerberosDescriptorUpdates(consumerCluster);
      KerberosDescriptor rawUserDescriptor = userDescriptor == null ? null
          : new KerberosDescriptorFactory().createInstance(userDescriptor.toMap());
      KerberosDescriptor rawEffectiveComposite = kerberosHelper.getKerberosDescriptor(
          KerberosHelper.KerberosDescriptorType.COMPOSITE, consumerCluster, stackId, false,
          rawUserDescriptor);
      Map<String, Map<String, String>> existingConfigurations =
          managementController.getConfigHelper().calculateExistingConfigurations(
              managementController, consumerCluster, null, null);
      if (existingConfigurations == null) {
        throw invalid("DEPENDENCY_SECURITY_PROOF_MISSING",
            "The authoritative consumer configurations are unavailable.");
      }
      Map<String, Set<String>> authoritativeServices =
          authoritativeConsumerServices(consumerCluster, stackId, selectedServices);
      Map<String, String> clusterEnv = existingConfigurations.get("cluster-env");
      if (clusterEnv == null) {
        clusterEnv = Map.of();
      }
      String smokeUser = requiredConsumerIdentity(clusterEnv, "smokeuser");
      String smokePrincipal = requiredConsumerIdentity(clusterEnv,
          "smokeuser_principal_name");
      SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindingReferences = new TreeMap<>();
      for (Map.Entry<ManagedDependencyType, BindingSelection> entry : selection.bindings().entrySet()) {
        bindingReferences.put(entry.getKey(), new ManagedBindingSnapshotRef(
            entry.getKey(), entry.getValue().bindingId()));
      }
      ManagedHBaseKerberosOverlaySpec overlaySpec = ManagedHBaseKerberosOverlaySpec.create(
          consumer.realm(), consumer.identityPlan().plannedShortUser(), smokePrincipal, smokeUser,
          consumer.identityPlan().planFingerprint(), bindingReferences);
      return securityAdapter.resolve(consumerCluster, consumer, rawEffectiveComposite,
          rawUserDescriptor, existingConfigurations, authoritativeServices, overlaySpec,
          providers, hdfsPolicy);
    } catch (AmbariException e) {
      throw new ManagedDependencyIntegrationException(422,
          "DEPENDENCY_SECURITY_PROOF_MISSING",
          "Authoritative Kerberos descriptor calculation is unavailable.", e);
    }
  }

  private Map<String, Set<String>> authoritativeConsumerServices(Cluster consumerCluster,
      StackId stackId, Map<String, Object> selectedServices) throws AmbariException {
    Map<String, Set<String>> services = new TreeMap<>();
    for (Map.Entry<String, Service> entry : consumerCluster.getServices().entrySet()) {
      Set<String> components = new TreeSet<>(entry.getValue().getServiceComponents().keySet());
      if (components.isEmpty() && selectedServices == null) {
        continue;
      }
      if (!components.isEmpty()) {
        services.put(entry.getKey(), Collections.unmodifiableSet(components));
      }
    }
    if (selectedServices != null) {
      for (Map.Entry<String, Object> entry : selectedServices.entrySet()) {
        if (!isSelected(entry.getValue())) {
          continue;
        }
        String serviceName = entry.getKey();
        if (serviceName == null || serviceName.isBlank()) {
          throw invalid("INVALID_CONSUMER_DESCRIPTOR",
              "The Add Service checkpoint contains an invalid selected service.");
        }
        if (services.containsKey(serviceName)) {
          continue;
        }
        ServiceInfo serviceInfo = metaInfo.getService(stackId.getStackName(),
            stackId.getStackVersion(), serviceName);
        if (serviceInfo == null) {
          throw invalid("INVALID_CONSUMER_DESCRIPTOR",
              "The active stack does not provide a selected service.");
        }
        List<ComponentInfo> metadataComponents = serviceInfo.getComponents();
        if (metadataComponents == null) {
          throw invalid("INVALID_CONSUMER_DESCRIPTOR",
              "The active stack does not provide selected service components.");
        }
        Set<String> components = metadataComponents.stream()
            .map(org.apache.ambari.server.state.ComponentInfo::getName)
            .filter(Objects::nonNull).filter(name -> !name.isBlank()).collect(
                java.util.stream.Collectors.toCollection(TreeSet::new));
        if (components.isEmpty()) {
          throw invalid("INVALID_CONSUMER_DESCRIPTOR",
              "The active stack does not provide components for a selected service.");
        }
        services.put(serviceName, Collections.unmodifiableSet(components));
      }
    }
    if (!services.containsKey("HBASE")) {
      ServiceInfo hbase = metaInfo.getService(stackId.getStackName(), stackId.getStackVersion(),
          "HBASE");
      if (hbase == null) {
        throw invalid("INVALID_CONSUMER_DESCRIPTOR",
            "The active stack does not provide HBASE components.");
      }
      List<ComponentInfo> metadataComponents = hbase.getComponents();
      if (metadataComponents == null) {
        throw invalid("INVALID_CONSUMER_DESCRIPTOR",
            "The active stack does not provide HBASE components.");
      }
      Set<String> components = metadataComponents.stream()
          .map(org.apache.ambari.server.state.ComponentInfo::getName)
          .filter(Objects::nonNull).filter(name -> !name.isBlank()).collect(
              java.util.stream.Collectors.toCollection(TreeSet::new));
      if (components.isEmpty()) {
        throw invalid("INVALID_CONSUMER_DESCRIPTOR",
            "The active stack does not provide HBASE components.");
      }
      services.put("HBASE", Collections.unmodifiableSet(components));
    }
    return Collections.unmodifiableMap(services);
  }

  private Map<String, Object> selectedAddServiceServices(Cluster consumerCluster,
      long expectedRevision) {
    if (persistKeyValue == null) {
      throw invalid("DEPENDENCY_SECURITY_PROOF_MISSING",
          "The owned Add Service checkpoint store is unavailable.");
    }
    final ScopedWorkflowState workflow;
    try {
      workflow = persistKeyValue.getActiveOwnedClusterWorkflowState(
          consumerCluster.getClusterId(), "ADD_SERVICE", expectedRevision);
    } catch (AuthorizationException e) {
      throw new ManagedDependencyIntegrationException(403,
          "DEPENDENCY_AUTHORIZATION_FAILED",
          "The authenticated user cannot read this Add Service checkpoint.", e);
    }
    if (workflow == null || workflow.getRevision() != expectedRevision
        || !"ADD_SERVICE".equals(workflow.getWorkflow())) {
      throw new ManagedDependencyIntegrationException(409,
          "WORKFLOW_VERSION_CONFLICT",
          "The owned Add Service checkpoint is no longer current.");
    }
    Map<String, Object> selectedServices = mapAt(workflow.getValues(),
        "ADD_SERVICE", "addServiceSteps", "SERVICES", "data", "services");
    if (!isSelected(selectedServices.get("HBASE"))) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR",
          "The owned Add Service checkpoint does not select HBASE.");
    }
    return selectedServices;
  }

  private String requiredConsumerIdentity(Map<String, String> values, String key) {
    String value = values.get(key);
    if (value == null || value.isBlank()) {
      throw invalid("DEPENDENCY_AUTHORIZATION_FAILED",
          "The authoritative consumer " + key + " is missing.");
    }
    return value.trim();
  }

  public Map<String, Cluster> allClusters() {
    return clusters.getClusters();
  }

  /**
   * Seals a prospective preview against the effective HBase configuration.
   * Preview itself remains side-effect free; approval and dispatch call this
   * after the reviewed values have been written.
   */
  public void validateEffectiveConsumerConfig(Cluster cluster,
      ManagedDependencySnapshot snapshot) {
    Map<String, Map<String, String>> expected = new TreeMap<>();
    if (snapshot.type() == ManagedDependencyType.HDFS) {
      expected.put("hbase-site", Map.of(
          "hbase.rootdir", snapshot.namespace().rootUri(),
          "hbase.wal.dir", snapshot.namespace().walUri()));
    } else {
      expected.put("hbase-site", snapshot.zooKeeperClient());
    }
    expected.put("hbase-env", Map.of(
        "hbase_user", snapshot.consumerIdentity().effectiveShortUser()));

    Service hbase = service(cluster, "HBASE");
    Set<Long> daemonHosts = hbase.getServiceComponents().values().stream()
        .filter(component -> Set.of(
            "HBASE_MASTER", "HBASE_REGIONSERVER", "HBASE_THRIFT").contains(component.getName()))
        .flatMap(component -> component.getServiceComponentHosts().values().stream())
        .map(componentHost -> componentHost.getHost().getHostId())
        .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    if (daemonHosts.isEmpty()) {
      throw invalid("DEPENDENCY_MANAGED_CONFIG_MISMATCH",
          "Assign HBase daemon hosts before approving a managed dependency.");
    }
    for (Long hostId : daemonHosts) {
      for (Map.Entry<String, Map<String, String>> type : expected.entrySet()) {
        Map<String, String> actual = effectiveProperties(cluster, hostId, type.getKey());
        for (Map.Entry<String, String> property : type.getValue().entrySet()) {
          if (!Objects.equals(property.getValue(), actual.get(property.getKey()))) {
            throw invalid("DEPENDENCY_MANAGED_CONFIG_MISMATCH",
                "Apply the reviewed managed HBase configuration before approval or dispatch.");
          }
        }
      }
    }
  }

  private Map<String, String> effectiveProperties(Cluster cluster, long hostId, String type) {
    TreeMap<String, String> result = new TreeMap<>(properties(cluster, type));
    try {
      cluster.getConfigGroupsByHostId(hostId).values().stream()
          .filter(group -> "HBASE".equals(group.getServiceName()))
          .sorted(java.util.Comparator.comparingLong(ConfigGroup::getId))
          .map(group -> group.getConfigurations().get(type))
          .filter(Objects::nonNull)
          .forEach(config -> result.putAll(config.getProperties()));
      return result;
    } catch (AmbariException e) {
      throw new ManagedDependencyIntegrationException(409,
          "DEPENDENCY_MANAGED_CONFIG_MISMATCH",
          "Effective HBase configuration is unavailable; reload it before retrying.", e);
    }
  }

  /** Classifies existing HBase endpoints without assuming that an absent binding is local. */
  public String unboundOwnership(Cluster cluster, ManagedDependencyType type) {
    Map<String, String> hbaseSite = properties(cluster, "hbase-site");
    if (type == ManagedDependencyType.HDFS) {
      String root = hbaseSite.getOrDefault("hbase.rootdir", "").trim();
      if (root.isEmpty()) {
        return "unknown";
      }
      if (!cluster.getServices().containsKey("HDFS")) {
        return "unmanaged";
      }
      String localDefaultFs = property(cluster, "core-site", "fs.defaultFS", "");
      String rootEndpoint = uriEndpoint(root);
      String localEndpoint = uriEndpoint(localDefaultFs);
      if (rootEndpoint.isEmpty() || localEndpoint.isEmpty()) {
        return "unknown";
      }
      return rootEndpoint.equals(localEndpoint) && ownsHdfsEndpoint(cluster, localDefaultFs)
          ? "local" : "unmanaged";
    }

    String quorum = hbaseSite.getOrDefault("hbase.zookeeper.quorum", "").trim();
    if (quorum.isEmpty()) {
      return "unknown";
    }
    Service localZooKeeper = cluster.getServices().get("ZOOKEEPER");
    if (localZooKeeper == null) {
      return "unmanaged";
    }
    Set<String> configured = normalizedHosts(commaList(quorum));
    Set<String> local = normalizedHosts(localZooKeeper.getServiceComponents().values().stream()
        .filter(component -> "ZOOKEEPER_SERVER".equals(component.getName()))
        .flatMap(component -> component.getServiceComponentHosts().keySet().stream()).toList());
    if (configured.isEmpty() || local.isEmpty()) {
      return "unknown";
    }
    int configuredPort = integer(hbaseSite.get("hbase.zookeeper.property.clientPort"));
    int localPort = integer(property(cluster, "zoo.cfg", "clientPort", ""));
    if (configuredPort <= 0 || localPort <= 0) {
      return "unknown";
    }
    return configured.equals(local) && configuredPort == localPort ? "local" : "unmanaged";
  }

  public Cluster cluster(long clusterId) {
    try {
      return clusters.getClusterById(clusterId);
    } catch (AmbariException e) {
      throw new ManagedDependencyIntegrationException(404, "DEPENDENCY_CLUSTER_NOT_FOUND",
          "The requested cluster does not exist.", e);
    }
  }

  public Cluster cluster(String clusterName) {
    try {
      return clusters.getCluster(clusterName);
    } catch (AmbariException e) {
      throw new ManagedDependencyIntegrationException(404, "DEPENDENCY_CLUSTER_NOT_FOUND",
          "The requested cluster does not exist.", e);
    }
  }

  private Service service(Cluster cluster, String serviceName) {
    try {
      return cluster.getService(serviceName);
    } catch (AmbariException e) {
      throw new ManagedDependencyIntegrationException(404, "DEPENDENCY_SERVICE_NOT_FOUND",
          "The requested service does not exist in the selected cluster.", e);
    }
  }

  private ManagedDependencyVersion version(Cluster cluster, Service service,
      String serviceName, boolean requireObservedVersion) {
    RepositoryVersionEntity repository = service.getDesiredRepositoryVersion();
    if (repository == null) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR", "The service has no desired repository version.");
    }
    StackId desiredStack = cluster.getDesiredStackVersion();
    StackId currentStack = cluster.getCurrentStackVersion();
    if (currentStack == null || !currentStack.equals(desiredStack)
        || cluster.getUpgradeInProgress() != null) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "Managed dependencies cannot be approved while a cluster stack upgrade is active.");
    }
    validateActiveComponentVersions(service, repository, requireObservedVersion);
    return version(repository, desiredStack.getStackName(), desiredStack.getStackVersion(), serviceName);
  }

  private ManagedDependencyVersion versionForServicePlan(Cluster cluster) {
    StackId desiredStack = cluster.getDesiredStackVersion();
    StackId currentStack = cluster.getCurrentStackVersion();
    if (desiredStack == null || currentStack == null || !currentStack.equals(desiredStack)
        || cluster.getUpgradeInProgress() != null) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "Managed dependencies cannot be planned while a cluster stack upgrade is active.");
    }
    Set<Long> repositoryIds = new TreeSet<>();
    for (Service service : cluster.getServices().values()) {
      RepositoryVersionEntity repository = service.getDesiredRepositoryVersion();
      if (repository == null) {
        throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
            "An installed service has no authoritative repository version.");
      }
      repositoryIds.add(repository.getParentId() == null
          ? repository.getId() : repository.getParentId());
    }
    if (repositoryIds.size() != 1) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "Select an explicit active repository before planning the HBASE service.");
    }
    RepositoryVersionEntity repository = repositoryVersionDAO.findByPK(
        repositoryIds.iterator().next());
    if (repository == null) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "The active cluster repository version no longer exists.");
    }
    return version(repository, desiredStack.getStackName(), desiredStack.getStackVersion(), "HBASE");
  }

  private ManagedDependencyVersion version(RepositoryVersionEntity repository,
      String stackName, String stackVersion, String serviceName) {
    if (!repository.isResolved()) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "The selected repository version has not been resolved by installed components.");
    }
    return versionMetadata(stackName, stackVersion, serviceName, repository.getVersion(),
        repository.getId(), true);
  }

  private ManagedDependencyVersion versionFromAvailableDefinition(Map<String, Object> selectedStack,
      StackId expectedStack, String expectedRepositoryVersion, String serviceName) {
    String definitionId = requiredText(selectedStack, "id");
    VersionDefinitionXml definition = metaInfo.getVersionDefinition(definitionId);
    try {
      StackInfo stack = metaInfo.getStack(expectedStack.getStackName(), expectedStack.getStackVersion());
      String fullVersion = definition == null || definition.release == null
          ? "" : definition.release.getFullVersion(stack.getReleaseVersion());
      if (definition == null || definition.release == null
          || !expectedStack.equals(new StackId(definition.release.stackId))
          || !expectedRepositoryVersion.equals(fullVersion)) {
        throw invalid("DEPENDENCY_DRAFT_VERSION_UNMATERIALIZED",
            "Materialize or reselect the version definition before final dependency approval.");
      }
      return versionMetadata(expectedStack.getStackName(), expectedStack.getStackVersion(),
          serviceName, fullVersion, null, true);
    } catch (AmbariException | IllegalArgumentException e) {
      throw new ManagedDependencyIntegrationException(422,
          "DEPENDENCY_DRAFT_VERSION_UNMATERIALIZED",
          "Materialize or reselect the version definition before final dependency approval.", e);
    }
  }

  private ManagedDependencyVersion versionMetadata(String stackName, String stackVersion,
      String serviceName, String repositoryVersion, Long repositoryRowId, boolean active) {
    try {
      ServiceInfo info = metaInfo.getService(stackName, stackVersion, serviceName);
      verifyClientFeature(info, serviceName);
      return new ManagedDependencyVersion(stackName, stackVersion, active,
          info.getVersion(), new TreeMap<>(Map.of("distribution", repositoryVersion)),
          new TreeSet<>(Set.of(STANDARD_RPC_CLIENT)), repositoryRowId, List.of());
    } catch (AmbariException e) {
      throw new ManagedDependencyIntegrationException(422, "DEPENDENCY_VERSION_UNSUPPORTED",
          "The active stack does not provide the required service metadata.", e);
    }
  }

  private void verifyClientFeature(ServiceInfo info, String serviceName) {
    String componentName = switch (serviceName) {
      case "HBASE" -> "HBASE_CLIENT";
      case "HDFS" -> "HDFS_CLIENT";
      case "ZOOKEEPER" -> "ZOOKEEPER_CLIENT";
      default -> "";
    };
    org.apache.ambari.server.state.ComponentInfo client = info.getComponentByName(componentName);
    if (client == null || !client.isClient() || !client.isVersionAdvertised()
        || client.getCommandScript() == null || client.getClientConfigFiles() == null
        || client.getClientConfigFiles().isEmpty()) {
      throw invalid("DEPENDENCY_CLIENT_FEATURE_UNSUPPORTED",
          "The active stack does not provide a complete managed client component for " + serviceName + ".");
    }
  }

  void validateActiveComponentVersions(Service service,
      RepositoryVersionEntity repository, boolean requireObservedVersion) {
    boolean observed = false;
    for (ServiceComponent component : service.getServiceComponents().values()) {
      RepositoryVersionEntity desired = component.getDesiredRepositoryVersion();
      if (desired == null || !Objects.equals(desired.getId(), repository.getId())) {
        throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
            "Service components target mixed repository versions.");
      }
      if (!component.isVersionAdvertised()) {
        continue;
      }
      for (ServiceComponentHost host : component.getServiceComponentHosts().values()) {
        if (host.getUpgradeState() != UpgradeState.NONE) {
          throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
              "A version-advertising component has an active upgrade state.");
        }
        if (!requireObservedVersion
            && (host.getState() != State.INIT || host.getDesiredState() != State.INIT)) {
          throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
              "A consumer without an observed package version must remain in the fresh INIT state.");
        }
        String hostVersion = host.getVersion();
        if (requireObservedVersion && !repository.getVersion().equals(hostVersion)) {
          throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
              "Version-advertising components are not uniformly on the resolved repository version.");
        }
        if (!requireObservedVersion
            && hostVersion != null && !hostVersion.isBlank()
            && !State.UNKNOWN.name().equals(hostVersion)
            && !repository.getVersion().equals(hostVersion)) {
          throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
              "A fresh consumer contains a component from a different repository version.");
        }
        observed |= repository.getVersion().equals(hostVersion);
      }
    }
    if (requireObservedVersion && !observed) {
      throw invalid("DEPENDENCY_VERSION_UNSUPPORTED",
          "No installed component has advertised the resolved repository version.");
    }
  }

  private boolean isFresh(Service service) {
    if (service.getDesiredState() != State.INIT) {
      return false;
    }
    return service.getServiceComponents().values().stream()
        .flatMap(component -> component.getServiceComponentHosts().values().stream())
        .allMatch(host -> host.getState() == State.INIT && host.getDesiredState() == State.INIT);
  }

  void validateProviderOverrides(Cluster cluster, Service service,
      ManagedDependencyType type) {
    Set<String> providerHosts = normalizedHosts(service.getServiceComponents().values().stream()
        .flatMap(component -> component.getServiceComponentHosts().keySet().stream()).toList());
    for (ConfigGroup group : cluster.getConfigGroups().values()) {
      boolean affectsProvider = group.getHosts().values().stream()
          .map(host -> host.getHostName().toLowerCase(Locale.ROOT))
          .anyMatch(providerHosts::contains);
      if (!affectsProvider) {
        continue;
      }
      for (Config config : group.getConfigurations().values()) {
        if (config.getProperties().keySet().stream()
            .anyMatch(key -> isExportedOverride(type, config.getType(), key))) {
          throw invalid("DEPENDENCY_CONFIG_OVERRIDE_UNSUPPORTED",
              "Provider host configuration groups override managed dependency endpoint or security facts.");
        }
      }
    }
  }

  boolean isExportedOverride(ManagedDependencyType type, String configType, String key) {
    if (type == ManagedDependencyType.HDFS) {
      if ("core-site".equals(configType)) {
        return ManagedDependencySnapshotValidator.CORE_SITE_ALLOWLIST.contains(key)
            || key.equals("hadoop.security.auth_to_local")
            || key.equals("hadoop.security.auth_to_local.mechanism")
            || key.equals("ha.zookeeper.quorum")
            || key.toLowerCase(Locale.ROOT).contains("credential")
            || key.toLowerCase(Locale.ROOT).contains("kms");
      }
      if ("hdfs-site".equals(configType)) {
        String lowerKey = key.toLowerCase(Locale.ROOT);
        return ManagedDependencySnapshotValidator.HDFS_SITE_ALLOWLIST.contains(key)
            || key.startsWith("dfs.ha.namenodes.")
            || key.startsWith("dfs.namenode.rpc-address.")
            || key.startsWith("dfs.client.failover.proxy.provider.")
            || key.equals("dfs.permissions.enabled")
            || lowerKey.contains("observer")
            || lowerKey.contains("encrypt")
            || lowerKey.contains("key.provider")
            || lowerKey.contains("kerberos")
            || lowerKey.contains("principal");
      }
      return "hadoop-env".equals(configType)
              && Set.of("hdfs_user", "hdfs_principal_name", "hdfs_user_keytab").contains(key)
          || "cluster-env".equals(configType)
              && Set.of("security_enabled", "user_group").contains(key)
          || "kerberos-env".equals(configType)
              && Set.of("realm", "manage_auth_to_local").contains(key)
          || "krb5-conf".equals(configType)
              && Set.of("manage_krb5_conf", "realm", "conf_dir", "content").contains(key);
    }
    return "zoo.cfg".equals(configType)
            && (Set.of("clientPort", "kerberos.removeHostFromPrincipal",
                "kerberos.removeRealmFromPrincipal", "security.auth_to_local",
                "secureClientPort", "requireClientAuthScheme").contains(key)
                || key.startsWith("authProvider.") || key.startsWith("ssl."))
        || "zookeeper-env".equals(configType)
            && Set.of("zk_user", "zookeeper_principal_name", "zookeeper_keytab_path").contains(key)
        || "cluster-env".equals(configType)
            && Set.of("security_enabled", "user_group").contains(key)
        || "kerberos-env".equals(configType) && "realm".equals(key)
        || "hbase-site".equals(configType)
            && Set.of("zookeeper.znode.parent", "zookeeper.sasl.client",
                "zookeeper.sasl.client.username").contains(key);
  }

  private Set<String> requiredHdfsClientProperties(Map<String, String> coreSite,
      Map<String, String> hdfsSite) {
    TreeSet<String> required = new TreeSet<>();
    required.add("fs.defaultFS");
    coreSite.forEach((key, value) -> {
      if (ManagedDependencySnapshotValidator.CORE_SITE_ALLOWLIST.contains(key)
          && value != null && !value.isBlank()) {
        required.add(key);
      }
    });
    hdfsSite.forEach((key, value) -> {
      if (value != null && !value.isBlank()
          && (ManagedDependencySnapshotValidator.HDFS_SITE_ALLOWLIST.contains(key)
              || key.startsWith("dfs.ha.namenodes.")
              || key.startsWith("dfs.namenode.rpc-address.")
              || key.startsWith("dfs.client.failover.proxy.provider."))) {
        required.add(key);
      }
    });
    return required;
  }

  private Set<String> unsupportedHdfsFeatures(Map<String, String> coreSite,
      Map<String, String> hdfsSite) {
    TreeSet<String> unsupported = new TreeSet<>();
    Map<String, String> effective = new LinkedHashMap<>(coreSite);
    effective.putAll(hdfsSite);
    effective.forEach((key, value) -> {
      String lowerKey = key.toLowerCase(Locale.ROOT);
      String normalized = value == null ? "" : value.trim();
      if (!normalized.isEmpty() && (lowerKey.contains("key.provider") || lowerKey.contains("kms"))) {
        unsupported.add("hdfs-kms:" + key);
      }
      if (lowerKey.contains("authentication") && (lowerKey.contains("class")
          || lowerKey.contains("provider"))) {
        unsupported.add("hdfs-custom-authentication:" + key);
      }
    });
    if ("false".equalsIgnoreCase(hdfsSite.getOrDefault("dfs.permissions.enabled", "true"))) {
      unsupported.add("hdfs-permissions-disabled");
    }
    return unsupported;
  }

  Set<String> unsupportedZooKeeperFeatures(Map<String, String> zooCfg, boolean secure) {
    TreeSet<String> unsupported = new TreeSet<>();
    zooCfg.forEach((key, value) -> {
      String lowerKey = key.toLowerCase(Locale.ROOT);
      String normalized = value == null ? "" : value.trim();
      if ((Set.of("secureclientport", "sslquorum", "portunification",
          "clientportunification").contains(lowerKey) && !normalized.isEmpty()
              && !"false".equalsIgnoreCase(normalized) && !"0".equals(normalized))
          || ((lowerKey.startsWith("ssl.") || "zookeeper.client.secure".equals(lowerKey))
              && !normalized.isEmpty() && !"false".equalsIgnoreCase(normalized))) {
        unsupported.add("zookeeper-tls:" + key);
      }
      if (lowerKey.startsWith("authprovider.")
          && (!secure
              || !"org.apache.zookeeper.server.auth.SASLAuthenticationProvider".equals(normalized))) {
        unsupported.add("zookeeper-custom-auth-provider:" + key);
      }
      if ("requireclientauthscheme".equals(lowerKey)
          && (!secure || !"sasl".equalsIgnoreCase(normalized))) {
        unsupported.add("zookeeper-custom-client-auth:" + normalized);
      }
    });
    return unsupported;
  }

  private String kerberosServiceName(String principal) {
    if (principal == null || principal.isBlank()) {
      return "";
    }
    String primary = principal.trim().split("[/@]", 2)[0];
    return primary.matches("[A-Za-z0-9._-]+") ? primary : "";
  }

  private boolean installed(Service service) {
    return service.getDesiredState() != State.INIT
        && service.getServiceComponents().values().stream()
            .flatMap(component -> component.getServiceComponentHosts().values().stream())
            .allMatch(host -> host.getState() == State.INSTALLED || host.getState() == State.STARTED);
  }

  private boolean healthy(Service service) {
    if (service.getDesiredState() != State.STARTED) {
      return false;
    }
    for (ServiceComponent component : service.getServiceComponents().values()) {
      for (ServiceComponentHost host : component.getServiceComponentHosts().values()) {
        State expected = component.isClientComponent() ? State.INSTALLED : State.STARTED;
        if (host.getState() != expected || host.getDesiredState() != expected) {
          return false;
        }
      }
    }
    return true;
  }

  private HdfsEndpoint hdfsEndpoint(Map<String, String> coreSite, Map<String, String> hdfsSite) {
    String defaultFs = coreSite.getOrDefault("fs.defaultFS", "");
    String nameservices = hdfsSite.getOrDefault("dfs.nameservices", "").trim();
    List<String> nameserviceList = commaList(nameservices);
    boolean highAvailability = nameserviceList.size() == 1
        && !hdfsSite.getOrDefault("dfs.ha.namenodes." + nameserviceList.get(0), "").isBlank();
    String nameservice = nameserviceList.size() == 1 ? nameserviceList.get(0) : "";
    Map<String, String> rpcAddresses = new TreeMap<>();
    if (highAvailability) {
      for (String nameNode : commaList(hdfsSite.get("dfs.ha.namenodes." + nameservice))) {
        String address = hdfsSite.get("dfs.namenode.rpc-address." + nameservice + "." + nameNode);
        if (address != null) {
          rpcAddresses.put(nameNode, address);
        }
      }
    }
    boolean observer = hdfsSite.keySet().stream().anyMatch(key -> key.toLowerCase().contains("observer"));
    return new HdfsEndpoint(defaultFs, highAvailability, nameservice, rpcAddresses,
        hdfsSite.getOrDefault("dfs.client.failover.proxy.provider." + nameservice, ""),
        nameserviceList.size() > 1, observer);
  }

  private ManagedDependencyIdentity identity(ManagedDependencyIdentity.Plan plan,
      boolean secure, String realm) {
    TreeSet<String> principals = new TreeSet<>();
    if (secure && !realm.isBlank()) {
      principals.add(plan.plannedShortUser() + "/_HOST@" + realm);
    }
    return new ManagedDependencyIdentity(plan.plannedShortUser(), principals, false,
        plan.plannedShortUser(), true, "0700", secure);
  }

  private ManagedDependencyIdentity.Plan identityPlan(Cluster cluster) {
    Integer creatorUserId = cluster.getClusterEntity().getCreatorUserId();
    String creationDraftId = cluster.getClusterEntity().getCreationDraftId();
    return creatorUserId != null && creationDraftId != null
        ? ManagedDependencyIdentity.Plan.forCreationDraft(
            creatorUserId, UUID.fromString(creationDraftId))
        : ManagedDependencyIdentity.Plan.forExistingCluster(cluster.getClusterId());
  }

  private ManagedDependencySecurityMode security(boolean secure) {
    return secure ? ManagedDependencySecurityMode.KERBEROS : ManagedDependencySecurityMode.INSECURE;
  }

  private Map<String, String> properties(Cluster cluster, String type) {
    Config config = cluster.getDesiredConfigByType(type);
    return config == null ? Map.of() : new TreeMap<>(config.getProperties());
  }

  private String property(Cluster cluster, String type, String name, String fallback) {
    return properties(cluster, type).getOrDefault(name, fallback);
  }

  private int integer(String value) {
    try {
      return Integer.parseInt(value == null ? "" : value.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private List<String> commaList(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(value.split(","))
        .map(String::trim).filter(item -> !item.isEmpty()).sorted().distinct().toList();
  }

  private String uriEndpoint(String value) {
    try {
      URI uri = URI.create(value);
      if (uri.getScheme() == null || uri.getAuthority() == null) {
        return "";
      }
      return (uri.getScheme() + "://" + uri.getAuthority()).toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException e) {
      return "";
    }
  }

  private boolean ownsHdfsEndpoint(Cluster cluster, String defaultFs) {
    Service hdfs = cluster.getServices().get("HDFS");
    Set<String> nameNodes = normalizedHosts(hdfs.getServiceComponents().values().stream()
        .filter(component -> "NAMENODE".equals(component.getName()))
        .flatMap(component -> component.getServiceComponentHosts().keySet().stream()).toList());
    if (nameNodes.isEmpty()) {
      return false;
    }
    try {
      URI endpoint = URI.create(defaultFs);
      Map<String, String> hdfsSite = properties(cluster, "hdfs-site");
      String nameservice = endpoint.getAuthority();
      List<String> nodeIds = commaList(hdfsSite.get("dfs.ha.namenodes." + nameservice));
      if (!nodeIds.isEmpty()) {
        Set<String> configuredNameNodes = normalizedHosts(nodeIds.stream()
            .map(id -> hdfsSite.get("dfs.namenode.rpc-address." + nameservice + "." + id))
            .filter(java.util.Objects::nonNull).toList());
        return configuredNameNodes.size() == nodeIds.size() && nameNodes.containsAll(configuredNameNodes);
      }
      return endpoint.getHost() != null
          && nameNodes.contains(endpoint.getHost().toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private Set<String> normalizedHosts(Collection<String> hosts) {
    TreeSet<String> normalized = new TreeSet<>();
    for (String value : hosts) {
      String host = value.trim().toLowerCase(Locale.ROOT);
      int port = host.lastIndexOf(':');
      if (port > 0 && host.indexOf(':') == port) {
        host = host.substring(0, port);
      }
      if (host.endsWith(".")) {
        host = host.substring(0, host.length() - 1);
      }
      if (!host.isEmpty()) {
        normalized.add(host);
      }
    }
    return normalized;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mapAt(Map<String, Object> root, String... path) {
    Object current = root;
    for (String part : path) {
      if (!(current instanceof Map<?, ?> map)) {
        throw invalid("INVALID_CONSUMER_DESCRIPTOR",
            "The cluster-creation draft is missing required dependency metadata.");
      }
      current = map.get(part);
    }
    if (!(current instanceof Map<?, ?>)) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR",
          "The cluster-creation draft is missing required dependency metadata.");
    }
    return (Map<String, Object>) current;
  }

  private boolean isSelected(Object value) {
    if (!(value instanceof Map<?, ?> map)) {
      return false;
    }
    return Boolean.TRUE.equals(map.get("selected"));
  }

  private String requiredText(Map<String, Object> values, String key) {
    Object value = values.get(key);
    if (!(value instanceof String text) || text.isBlank()) {
      throw invalid("INVALID_CONSUMER_DESCRIPTOR",
          "The cluster-creation draft is missing required dependency metadata.");
    }
    return text.trim();
  }

  private String firstPresentText(Map<String, Object> primary, String primaryKey,
      Map<String, Object> fallback, String fallbackKey) {
    Object value = primary.get(primaryKey);
    if (value instanceof String text && !text.isBlank()) {
      return text.trim();
    }
    return requiredText(fallback, fallbackKey);
  }

  private ManagedDependencyIntegrationException invalid(String code, String message) {
    return new ManagedDependencyIntegrationException(422, code, message);
  }
}
