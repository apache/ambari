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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public final class ManagedDependencyDescriptor {
  private ManagedDependencyDescriptor() {
  }

  public record Consumer(
      String sourceScope,
      UUID draftId,
      Long clusterId,
      String clusterName,
      String serviceName,
      ConsumerLifecycle lifecycle,
      ManagedDependencyVersion version,
      ManagedDependencySecurityMode securityMode,
      String realm,
      ManagedDependencyIdentity identity,
      ManagedDependencyIdentity.Plan identityPlan) {

    public Consumer {
      sourceScope = requireNonBlank(sourceScope, "sourceScope");
      serviceName = requireNonBlank(serviceName, "serviceName");
      lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
      version = Objects.requireNonNull(version, "version");
      securityMode = Objects.requireNonNull(securityMode, "securityMode");
      realm = normalize(realm);
      identity = Objects.requireNonNull(identity, "identity");
      identityPlan = Objects.requireNonNull(identityPlan, "identityPlan");
    }

    @Override
    public boolean equals(Object value) {
      if (this == value) {
        return true;
      }
      if (!(value instanceof Consumer other)) {
        return false;
      }
      return serviceName.equals(other.serviceName)
          && version.active() == other.version.active()
          && version.stackName().equals(other.version.stackName())
          && version.stackVersion().equals(other.version.stackVersion())
          && version.serviceVersion().equals(other.version.serviceVersion())
          && version.resolvedVersions().equals(other.version.resolvedVersions())
          && version.clientFeatures().equals(other.version.clientFeatures())
          && securityMode == other.securityMode
          && realm.equals(other.realm)
          && identity.equals(other.identity);
    }

    @Override
    public int hashCode() {
      return Objects.hash(serviceName, version.active(), version.stackName(), version.stackVersion(),
          version.serviceVersion(), version.resolvedVersions(), version.clientFeatures(),
          securityMode, realm, identity);
    }
  }

  public enum ConsumerLifecycle {
    DRAFT,
    INIT_UNINSTALLED,
    MANAGED_UPDATE,
    INSTALLED_LOCAL
  }

  public record Provider(
      ManagedDependencyServiceKey serviceKey,
      ManagedDependencyType type,
      ManagedDependencyVersion version,
      boolean installed,
      boolean healthy,
      ManagedDependencySecurityMode securityMode,
      String realm,
      ManagedDependencyIdentity identity,
      HdfsEndpoint hdfsEndpoint,
      ZooKeeperEndpoint zooKeeperEndpoint,
      SortedMap<String, String> coreSite,
      SortedMap<String, String> hdfsSite,
      SortedMap<String, String> zooKeeperClient,
      SortedSet<String> requiredClientProperties,
      SortedSet<String> unsupportedFeatures) {

    public Provider {
      serviceKey = Objects.requireNonNull(serviceKey, "serviceKey");
      type = Objects.requireNonNull(type, "type");
      version = Objects.requireNonNull(version, "version");
      securityMode = Objects.requireNonNull(securityMode, "securityMode");
      realm = normalize(realm);
      if (securityMode == ManagedDependencySecurityMode.KERBEROS) {
        identity = Objects.requireNonNull(identity, "identity");
      }
      coreSite = ManagedDependencyVersion.immutableSortedMap(coreSite);
      hdfsSite = ManagedDependencyVersion.immutableSortedMap(hdfsSite);
      zooKeeperClient = ManagedDependencyVersion.immutableSortedMap(zooKeeperClient);
      requiredClientProperties = immutableSortedSet(requiredClientProperties);
      unsupportedFeatures = immutableSortedSet(unsupportedFeatures);
    }

    public Provider(ManagedDependencyServiceKey serviceKey, ManagedDependencyType type,
        ManagedDependencyVersion version, boolean installed, boolean healthy,
        ManagedDependencySecurityMode securityMode, String realm, ManagedDependencyIdentity identity,
        HdfsEndpoint hdfsEndpoint, ZooKeeperEndpoint zooKeeperEndpoint,
        Map<String, String> coreSite, Map<String, String> hdfsSite,
        Map<String, String> zooKeeperClient, Set<String> requiredClientProperties,
        Set<String> unsupportedFeatures) {
      this(serviceKey, type, version, installed, healthy, securityMode, realm, identity,
          hdfsEndpoint, zooKeeperEndpoint, mutableMap(coreSite), mutableMap(hdfsSite),
          mutableMap(zooKeeperClient), mutableSet(requiredClientProperties),
          mutableSet(unsupportedFeatures));
    }
  }

  public record HdfsEndpoint(
      String defaultFs,
      boolean highAvailability,
      String nameService,
      SortedMap<String, String> nameNodeRpcAddresses,
      String failoverProxyProvider,
      boolean federationEnabled,
      boolean observerNameNodeEnabled) {

    public HdfsEndpoint {
      defaultFs = requireNonBlank(defaultFs, "defaultFs");
      nameService = normalize(nameService);
      nameNodeRpcAddresses = ManagedDependencyVersion.immutableSortedMap(nameNodeRpcAddresses);
      failoverProxyProvider = normalize(failoverProxyProvider);
    }

    public HdfsEndpoint(String defaultFs, boolean highAvailability, String nameService,
        Map<String, String> nameNodeRpcAddresses, String failoverProxyProvider,
        boolean federationEnabled, boolean observerNameNodeEnabled) {
      this(defaultFs, highAvailability, nameService, mutableMap(nameNodeRpcAddresses),
          failoverProxyProvider, federationEnabled, observerNameNodeEnabled);
    }
  }

  public record ZooKeeperEndpoint(
      List<String> quorumHosts,
      int clientPort,
      boolean saslEnabled,
      String saslServiceName,
      boolean managedParentAclSupported,
      List<String> configuredApplicationParents,
      boolean kerberosRemoveHostFromPrincipal,
      boolean kerberosRemoveRealmFromPrincipal,
      String kerberosAuthToLocalRules) {
    public ZooKeeperEndpoint {
      quorumHosts = sortedList(quorumHosts);
      saslServiceName = normalize(saslServiceName);
      configuredApplicationParents = sortedList(configuredApplicationParents);
      kerberosAuthToLocalRules = normalize(kerberosAuthToLocalRules);
    }

    public ZooKeeperEndpoint(List<String> quorumHosts, int clientPort, boolean saslEnabled,
        String saslServiceName, boolean managedParentAclSupported,
        List<String> configuredApplicationParents) {
      this(quorumHosts, clientPort, saslEnabled, saslServiceName,
          managedParentAclSupported, configuredApplicationParents, false, false, "");
    }
  }

  /** Implemented by the integration layer; both paths must return the same descriptor shape. */
  public interface Resolver {
    Consumer resolveDraft(UUID draftId, long expectedRevision);

    Consumer resolveService(ManagedDependencyServiceKey serviceKey);
  }

  private static SortedSet<String> immutableSortedSet(Set<String> values) {
    return values == null || values.isEmpty()
        ? Collections.emptySortedSet()
        : Collections.unmodifiableSortedSet(new TreeSet<>(values));
  }

  private static TreeMap<String, String> mutableMap(Map<String, String> values) {
    return values == null ? new TreeMap<>() : new TreeMap<>(values);
  }

  private static TreeSet<String> mutableSet(Set<String> values) {
    return values == null ? new TreeSet<>() : new TreeSet<>(values);
  }

  private static List<String> sortedList(List<String> values) {
    return values == null || values.isEmpty() ? List.of() : List.copyOf(new TreeSet<>(values));
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private static String requireNonBlank(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
