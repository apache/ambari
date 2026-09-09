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
package org.apache.ambari.server.controller.dependencies;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosLivePlan;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosLivePlanProvider;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Reads one coherent approved managed HBase security plan from persistence. */
@Singleton
public final class PersistedManagedHBaseKerberosLivePlanProvider
    implements ManagedHBaseKerberosLivePlanProvider {
  private static final String KERBEROS_ENV = "kerberos-env";
  private static final String REALM = "realm";

  private final ServiceDependencyDAO dependencyDAO;

  @Inject
  public PersistedManagedHBaseKerberosLivePlanProvider(ServiceDependencyDAO dependencyDAO) {
    this.dependencyDAO = Objects.requireNonNull(dependencyDAO, "dependencyDAO");
  }

  @Override
  public Optional<ManagedHBaseKerberosLivePlan> findApprovedLivePlan(Cluster cluster) {
    Objects.requireNonNull(cluster, "cluster");
    return cluster.executeUnderReadLock(() -> findApprovedLivePlanLocked(cluster));
  }

  private Optional<ManagedHBaseKerberosLivePlan> findApprovedLivePlanLocked(Cluster cluster) {
    Optional<List<ManagedDependencySnapshot>> persisted = Objects.requireNonNull(
        dependencyDAO.findApprovedLivePlanSnapshots(cluster.getClusterId()),
        "findApprovedLivePlanSnapshots decision");
    if (persisted.isEmpty()) {
      return Optional.empty();
    }

    List<ManagedDependencySnapshot> snapshots = persisted.orElseThrow();
    if (snapshots.isEmpty() || snapshots.size() > ManagedDependencyType.values().length) {
      throw invalidPlan("The approved managed HBase binding set must contain one or two snapshots.");
    }

    SortedMap<ManagedDependencyType, ManagedDependencySnapshot> byType = new TreeMap<>();
    Set<UUID> bindingIds = new HashSet<>();
    Set<ManagedDependencySecurityMode> securityModes = new HashSet<>();
    for (ManagedDependencySnapshot snapshot : snapshots) {
      if (snapshot == null
          || !hasSupportedSchema(snapshot)
          || byType.putIfAbsent(snapshot.type(), snapshot) != null
          || !bindingIds.add(snapshot.bindingId())) {
        throw invalidPlan("The approved managed HBase binding identities are inconsistent.");
      }
      requireCanonicalNamespace(snapshot);
      securityModes.add(snapshot.securityMode());
    }

    if (securityModes.size() != 1) {
      throw securityMismatch(
          "Secure and insecure managed HBase dependencies cannot be active together.");
    }
    ManagedDependencySecurityMode mode = securityModes.iterator().next();
    SecurityType clusterSecurity = cluster.getSecurityType();
    if (mode == ManagedDependencySecurityMode.INSECURE) {
      if (clusterSecurity != SecurityType.NONE) {
        throw securityMismatch(
            "The approved managed HBase dependencies are insecure but the consumer is Kerberized.");
      }
      return Optional.empty();
    }
    if (clusterSecurity != SecurityType.KERBEROS) {
      throw securityMismatch(
          "The approved managed HBase dependencies require a Kerberized consumer cluster.");
    }
    if (snapshots.stream().anyMatch(snapshot ->
        snapshot.schemaVersion() != ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION)) {
      throw invalidPlan(
          "Secure managed HBase dependencies require current typed security proofs.");
    }

    ManagedDependencySnapshot first = snapshots.get(0);
    ManagedHBaseConsumerLocalMapping mapping = Objects.requireNonNull(
        first.consumerLocalMapping(), "consumerLocalMapping");
    String consumerFingerprint = first.consumerFingerprint();
    for (ManagedDependencySnapshot snapshot : snapshots) {
      if (!first.consumerIdentity().equals(snapshot.consumerIdentity())
          || !consumerFingerprint.equals(snapshot.consumerFingerprint())
          || !mapping.equals(snapshot.consumerLocalMapping())) {
        throw invalidPlan(
            "The approved managed HBase dependencies do not share one consumer identity and mapping profile.");
      }
    }

    String actualRealm = consumerRealm(cluster);
    if (!mapping.realm().equals(actualRealm)) {
      throw securityMismatch(
          "The approved managed HBase identity realm differs from the consumer cluster realm.");
    }

    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings = new TreeMap<>();
    byType.forEach((type, snapshot) -> bindings.put(type,
        new ManagedBindingSnapshotRef(type, snapshot.bindingId())));
    ManagedHBaseKerberosOverlaySpec overlaySpec = ManagedHBaseKerberosOverlaySpec.create(
        mapping.realm(), first.consumerIdentity().effectiveShortUser(), mapping.smokePrincipal(),
        mapping.smokeShortUser(), mapping.identityPlanFingerprint(), bindings);
    return Optional.of(new ManagedHBaseKerberosLivePlan(
        overlaySpec, mapping.profileFingerprint()));
  }

  private boolean hasSupportedSchema(ManagedDependencySnapshot snapshot) {
    return snapshot.schemaVersion() == ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION
        || (snapshot.schemaVersion() == ManagedDependencySnapshot.LEGACY_INSECURE_SCHEMA_VERSION
            && snapshot.securityMode() == ManagedDependencySecurityMode.INSECURE);
  }

  private void requireCanonicalNamespace(ManagedDependencySnapshot snapshot) {
    ManagedDependencyNamespace expected;
    try {
      if (snapshot.type() == ManagedDependencyType.HDFS) {
        expected = ManagedDependencyNamespace.hdfs(snapshot.bindingId(),
            snapshot.coreSite().get("fs.defaultFS"));
      } else {
        expected = ManagedDependencyNamespace.zooKeeper(snapshot.bindingId());
      }
    } catch (RuntimeException error) {
      throw new ManagedDependencyIntegrationException(409,
          ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED.name(),
          "The approved managed HBase namespace cannot be reconstructed.", error);
    }
    if (!expected.equals(snapshot.namespace())) {
      throw invalidPlan(
          "The approved managed HBase namespace does not match its immutable binding UUID.");
    }
  }

  private String consumerRealm(Cluster cluster) {
    Config kerberosEnv = cluster.getDesiredConfigByType(KERBEROS_ENV);
    String realm = kerberosEnv == null || kerberosEnv.getProperties() == null
        ? null : kerberosEnv.getProperties().get(REALM);
    if (realm == null || realm.isBlank()) {
      throw securityMismatch(
          "The Kerberized consumer cluster has no current kerberos-env realm.");
    }
    return realm;
  }

  private ManagedDependencyIntegrationException invalidPlan(String message) {
    return new ManagedDependencyIntegrationException(409,
        ManagedDependencyErrorCode.DEPENDENCY_SECURITY_PROOF_UPDATE_REQUIRED.name(), message);
  }

  private ManagedDependencyIntegrationException securityMismatch(String message) {
    return new ManagedDependencyIntegrationException(409,
        ManagedDependencyErrorCode.DEPENDENCY_SECURITY_MISMATCH.name(), message);
  }
}
