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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.server.controller.dependencies.security;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Consumer;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.Provider;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyDescriptor.ZooKeeperEndpoint;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyErrorCode;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyProviderSecurityProof;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySecurityMode;
import org.apache.ambari.server.controller.dependencies.ManagedDependencySecurityValidationContext;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.controller.dependencies.security.ManagedHdfsAuthToLocalVerifier.PolicySource;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Adapts one authoritative managed HBase plan to the transient security contexts
 * consumed by the dependency snapshot validator.
 */
@Singleton
public final class ManagedHBaseSecurityDescriptorAdapter {
  private final KerberosHelper kerberosHelper;
  private final ManagedHdfsAuthToLocalVerifier hdfsVerifier;
  private final ManagedZooKeeperSaslPolicyVerifier zooKeeperVerifier;

  @Inject
  public ManagedHBaseSecurityDescriptorAdapter(KerberosHelper kerberosHelper) {
    this(kerberosHelper, new ManagedHdfsAuthToLocalVerifier(),
        new ManagedZooKeeperSaslPolicyVerifier());
  }

  ManagedHBaseSecurityDescriptorAdapter(KerberosHelper kerberosHelper,
      ManagedHdfsAuthToLocalVerifier hdfsVerifier,
      ManagedZooKeeperSaslPolicyVerifier zooKeeperVerifier) {
    this.kerberosHelper = Objects.requireNonNull(kerberosHelper, "kerberosHelper");
    this.hdfsVerifier = Objects.requireNonNull(hdfsVerifier, "hdfsVerifier");
    this.zooKeeperVerifier = Objects.requireNonNull(zooKeeperVerifier,
        "zooKeeperVerifier");
  }

  public Resolution resolve(
      Cluster consumerCluster,
      Consumer consumer,
      KerberosDescriptor rawEffectiveComposite,
      @Nullable KerberosDescriptor rawUserDescriptor,
      Map<String, Map<String, String>> existingConsumerConfigurations,
      Map<String, Set<String>> authoritativeConsumerServices,
      ManagedHBaseKerberosOverlaySpec overlaySpec,
      SortedMap<ManagedDependencyType, Provider> providers,
      @Nullable PolicySource hdfsPolicySource) throws AmbariException {
    Objects.requireNonNull(consumerCluster, "consumerCluster");
    Objects.requireNonNull(consumer, "consumer");
    Objects.requireNonNull(rawEffectiveComposite, "rawEffectiveComposite");
    Objects.requireNonNull(overlaySpec, "overlaySpec");

    Map<String, Map<String, String>> configurations =
        immutableConfigurations(existingConsumerConfigurations);
    Map<String, Set<String>> services = immutableServices(authoritativeConsumerServices);
    SortedMap<ManagedDependencyType, Provider> selectedProviders =
        immutableProviders(providers);
    validateSelection(consumerCluster, consumer, overlaySpec, selectedProviders,
        hdfsPolicySource);

    ManagedHBaseKerberosCalculation calculation;
    try {
      calculation = Objects.requireNonNull(
          kerberosHelper.calculateManagedHBaseKerberosConfiguration(
              consumerCluster, rawEffectiveComposite, rawUserDescriptor,
              configurations, services, overlaySpec, true),
          "managed HBase Kerberos calculation");
    } catch (KerberosInvalidConfigurationException e) {
      throw new AmbariException("Managed HBase Kerberos configuration is invalid", e);
    }
    validateCalculation(consumer, overlaySpec, calculation);

    SortedMap<ManagedDependencyType, ManagedDependencySecurityValidationContext> contexts =
        new TreeMap<>();
    for (Map.Entry<ManagedDependencyType, Provider> entry : selectedProviders.entrySet()) {
      ManagedDependencyProviderSecurityProof providerPolicy;
      PolicySource source = null;
      if (entry.getKey() == ManagedDependencyType.HDFS) {
        source = Objects.requireNonNull(hdfsPolicySource, "hdfsPolicySource");
        providerPolicy = ManagedDependencyProviderSecurityProof.forHdfs(
            hdfsVerifier.inspectPolicy(source));
      } else {
        ZooKeeperEndpoint endpoint = Objects.requireNonNull(
            entry.getValue().zooKeeperEndpoint(), "zooKeeperEndpoint");
        ManagedZooKeeperSaslPolicyVerifier.PolicySource zooKeeperSource =
            new ManagedZooKeeperSaslPolicyVerifier.PolicySource(
                entry.getValue().realm(), endpoint.saslEnabled(), endpoint.saslServiceName(),
                endpoint.managedParentAclSupported(), endpoint.kerberosRemoveHostFromPrincipal(),
                endpoint.kerberosRemoveRealmFromPrincipal(),
                endpoint.kerberosAuthToLocalRules());
        providerPolicy = ManagedDependencyProviderSecurityProof.forZooKeeper(
            zooKeeperVerifier.inspectPolicy(zooKeeperSource));
      }
      contexts.put(entry.getKey(), new ManagedDependencySecurityValidationContext(
          calculation.consumerLocalMapping(), providerPolicy, source));
    }
    return new Resolution(calculation, contexts);
  }

  private void validateSelection(Cluster cluster, Consumer consumer,
      ManagedHBaseKerberosOverlaySpec overlaySpec,
      SortedMap<ManagedDependencyType, Provider> providers,
      @Nullable PolicySource hdfsPolicySource) {
    if (!"HBASE".equals(consumer.serviceName())) {
      throw invalid(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
          "The managed security consumer must be HBASE.");
    }
    if (consumer.clusterId() == null || consumer.clusterId() != cluster.getClusterId()) {
      throw invalid(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
          "The managed security consumer does not belong to the target cluster.");
    }
    if (consumer.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
      throw invalid(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH,
          "The managed security adapter requires a Kerberized consumer.");
    }
    if (!overlaySpec.bindings().keySet().equals(providers.keySet())) {
      throw invalid(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "The provider set must match the complete managed binding selection.");
    }
    if (providers.containsKey(ManagedDependencyType.HDFS) != (hdfsPolicySource != null)) {
      throw invalid(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_MISSING,
          "Exactly one current HDFS policy source is required for a selected HDFS provider.");
    }

    String expectedPrincipal = consumer.identity().effectiveShortUser()
        + "/_HOST@" + consumer.realm();
    if (!consumer.realm().equals(overlaySpec.realm())
        || !consumer.identity().effectiveShortUser().equals(overlaySpec.effectiveShortUser())
        || !Set.of(expectedPrincipal).equals(consumer.identity().principalPatterns())
        || !expectedPrincipal.equals(overlaySpec.rolePrincipalPattern())
        || consumer.identity().authToLocalVerified()
        || !consumer.identityPlan().plannedShortUser().equals(overlaySpec.effectiveShortUser())
        || !consumer.identityPlan().planFingerprint().equals(
            overlaySpec.identityPlanFingerprint())) {
      throw invalid(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED,
          "The managed Kerberos overlay does not match the consumer identity plan.");
    }

    for (Map.Entry<ManagedDependencyType, Provider> entry : providers.entrySet()) {
      ManagedDependencyType type = entry.getKey();
      Provider provider = entry.getValue();
      ManagedBindingSnapshotRef binding = overlaySpec.bindings().get(type);
      if (provider.type() != type || binding == null || binding.type() != type
          || !type.getProviderServiceName().equals(provider.serviceKey().serviceName())) {
        throw invalid(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
            "A selected provider does not match its dependency type.");
      }
      if (provider.securityMode() != ManagedDependencySecurityMode.KERBEROS) {
        throw invalid(ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH,
            "Secure and insecure managed dependencies cannot be combined.");
      }
      if (!consumer.realm().equals(provider.realm())) {
        throw invalid(ManagedDependencyErrorCode.CROSS_REALM_NOT_SUPPORTED,
            "The consumer and every managed provider must use the same Kerberos realm.");
      }
    }
  }

  private void validateCalculation(Consumer consumer,
      ManagedHBaseKerberosOverlaySpec overlaySpec,
      ManagedHBaseKerberosCalculation calculation) {
    if (!consumer.realm().equals(calculation.realm())
        || !consumer.identity().effectiveShortUser().equals(
            calculation.effectiveShortUser())
        || !overlaySpec.rolePrincipalPattern().equals(
            calculation.rolePrincipalPattern())
        || !overlaySpec.headlessPrincipal().equals(calculation.headlessPrincipal())
        || !overlaySpec.smokePrincipal().equals(calculation.smokePrincipal())
        || !overlaySpec.smokeShortUser().equals(
            calculation.consumerLocalMapping().smokeShortUser())
        || !consumer.identityPlan().planFingerprint().equals(
            calculation.consumerLocalMapping().identityPlanFingerprint())) {
      throw invalid(ManagedDependencyErrorCode.DEPENDENCY_AUTHORIZATION_FAILED,
          "The calculated consumer mapping does not match the managed identity plan.");
    }
  }

  private static Map<String, Map<String, String>> immutableConfigurations(
      Map<String, Map<String, String>> source) {
    Objects.requireNonNull(source, "existingConsumerConfigurations");
    Map<String, Map<String, String>> copy = new TreeMap<>();
    for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
      String type = Objects.requireNonNull(entry.getKey(), "configuration type");
      Map<String, String> properties = Objects.requireNonNull(entry.getValue(),
          "configuration properties");
      if (type.isBlank() && !type.isEmpty()) {
        throw new IllegalArgumentException("configuration types must be canonical strings");
      }
      TreeMap<String, String> propertyCopy = new TreeMap<>();
      for (Map.Entry<String, String> property : properties.entrySet()) {
        propertyCopy.put(Objects.requireNonNull(property.getKey(), "configuration property"),
            Objects.requireNonNull(property.getValue(), "configuration value"));
      }
      copy.put(type, Collections.unmodifiableMap(propertyCopy));
    }
    return Collections.unmodifiableMap(copy);
  }

  private static Map<String, Set<String>> immutableServices(
      Map<String, Set<String>> source) {
    Objects.requireNonNull(source, "authoritativeConsumerServices");
    Map<String, Set<String>> copy = new TreeMap<>();
    for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
      String service = Objects.requireNonNull(entry.getKey(), "service name");
      Set<String> components = Objects.requireNonNull(entry.getValue(),
          "service components");
      if (service.isBlank() || components.isEmpty()
          || components.stream().anyMatch(component -> component == null
              || component.isBlank())) {
        throw new IllegalArgumentException(
            "authoritative services require non-empty canonical names and components");
      }
      copy.put(service, Collections.unmodifiableSet(new TreeSet<>(components)));
    }
    Set<String> hbase = copy.get("HBASE");
    if (hbase == null || hbase.isEmpty()) {
      throw invalid(ManagedDependencyErrorCode.INVALID_CONSUMER_DESCRIPTOR,
          "The authoritative consumer service plan must include HBASE components.");
    }
    return Collections.unmodifiableMap(copy);
  }

  private static SortedMap<ManagedDependencyType, Provider> immutableProviders(
      SortedMap<ManagedDependencyType, Provider> source) {
    Objects.requireNonNull(source, "providers");
    if (source.isEmpty() || source.size() > ManagedDependencyType.values().length) {
      throw invalid(ManagedDependencyErrorCode.INVALID_PROVIDER_DESCRIPTOR,
          "One or two managed providers are required.");
    }
    TreeMap<ManagedDependencyType, Provider> copy = new TreeMap<>();
    for (Map.Entry<ManagedDependencyType, Provider> entry : source.entrySet()) {
      copy.put(Objects.requireNonNull(entry.getKey(), "provider type"),
          Objects.requireNonNull(entry.getValue(), "provider"));
    }
    return Collections.unmodifiableSortedMap(copy);
  }

  private static ManagedDependencyIntegrationException invalid(
      ManagedDependencyErrorCode code, String message) {
    return new ManagedDependencyIntegrationException(422, code.name(), message);
  }

  /** Request-local result; raw policy sources must never become a wire payload. */
  public static final class Resolution {
    private final transient ManagedHBaseKerberosCalculation consumerCalculation;
    private final transient SortedMap<ManagedDependencyType,
        ManagedDependencySecurityValidationContext> contexts;
    private final Set<ManagedDependencyType> types;

    private Resolution(ManagedHBaseKerberosCalculation consumerCalculation,
        SortedMap<ManagedDependencyType, ManagedDependencySecurityValidationContext> contexts) {
      this.consumerCalculation = Objects.requireNonNull(consumerCalculation,
          "consumerCalculation");
      this.contexts = Collections.unmodifiableSortedMap(new TreeMap<>(contexts));
      this.types = Collections.unmodifiableSet(new TreeSet<>(contexts.keySet()));
    }

    @JsonIgnore
    public ManagedHBaseKerberosCalculation consumerCalculation() {
      return consumerCalculation;
    }

    @JsonIgnore
    public ManagedDependencySecurityValidationContext contextFor(
        ManagedDependencyType type) {
      ManagedDependencySecurityValidationContext context = contexts.get(
          Objects.requireNonNull(type, "type"));
      if (context == null) {
        throw new IllegalArgumentException("No security context exists for " + type);
      }
      return context;
    }

    public Set<ManagedDependencyType> types() {
      return types;
    }

    @Override
    public String toString() {
      return "Resolution[consumerProfile="
          + consumerCalculation.mappingProfileFingerprint()
          + ", types=" + types + "]";
    }
  }
}
