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
package org.apache.ambari.server.mpack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.MpackRecordDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;
import org.apache.ambari.server.orm.entities.MpackRecordEntity.Kind;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackResolutionContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/** Explicit database and runtime publication boundaries; neither implies the other. */
@Singleton
public class MpackActivation {
  @Inject private MpackCatalog catalog;
  @Inject private MpackSnapshots snapshots;
  @Inject private MpackRuntime runtime;
  @Inject private StackDAO stackDAO;
  @Inject private ExtensionDAO extensionDAO;
  @Inject private ExtensionLinkDAO linkDAO;
  @Inject private MpackDefinitionLoader loader;
  @Inject private Provider<AmbariMetaInfo> metadata;
  @Inject private Provider<Clusters> clusters;
  @Inject private Provider<StackAdvisorHelper> advisors;
  @Inject private Provider<MetadataHolder> agentMetadata;

  public void activate(MpackLifecycleState.Plan plan,
      MpackCatalog.Versioned<MpackLifecycleState.Operation> operation, StackManager candidate) throws AmbariException {
    runtime.requirePublicationLock();
    if (runtime.snapshot() != null && plan.candidateSnapshot().equals(runtime.snapshot().id())
        && plan.candidateSnapshot().equals(catalog.control().value().effectiveSnapshot())) {
      commitEffective(plan, operation);
      return;
    }
    AmbariMetaInfo.DefinitionView previous = metadata.get().captureDefinitionView();
    MpackSnapshots.Snapshot previousSnapshot = runtime.snapshot();
    try {
      registerCandidate(candidate, operation);
      publishRuntime(operation.value().id(), snapshots.load(plan.candidateSnapshot()), candidate);
      commitEffective(plan, catalog.operation(operation.value().id()));
    } catch (AmbariException | RuntimeException failure) {
      runtime.markUnreadable();
      MpackCatalog.Versioned<MpackLifecycleState.Control> observed = catalog.control();
      if (observed == null || !plan.candidateSnapshot().equals(observed.value().effectiveSnapshot())) {
        try {
          metadata.get().restoreDefinitionView(previous);
          refreshConsumers();
          advisors.get().clearDefinitionCaches();
          runtime.restore(previousSnapshot, operation.value().id());
        } catch (AmbariException | RuntimeException rollback) {
          failure.addSuppressed(rollback);
        }
      } else {
        runtime.publish(operation.value().id(), snapshots.load(plan.candidateSnapshot()));
      }
      throw failure;
    }
  }

  @Transactional(rollbackOn = {RuntimeException.class, AmbariException.class})
  public void registerCandidate(StackManager candidate,
      MpackCatalog.Versioned<MpackLifecycleState.Operation> operation) throws AmbariException {
    runtime.requirePublicationLock();
    MpackCatalog.Versioned<MpackLifecycleState.Control> current = catalog.control();
    requireOwner(current.value(), operation.value());
    candidate.registerCandidateDefinitions(stackDAO, extensionDAO);
    catalog.apply(List.of(MpackCatalog.change(operation.key(), Kind.OPERATION, operation.revision(),
        operation.value().transition(MpackLifecycleState.Phase.PUBLISHING,
            operation.value().effectiveSnapshot(), null, Map.of()))));
  }

  public void publishRuntime(String operationId, MpackSnapshots.Snapshot snapshot, StackManager candidate)
      throws AmbariException {
    runtime.requirePublicationLock();
    if (!snapshot.id().equals(candidate.getDefinitionSnapshotId())) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Candidate identity does not match publication");
    }
    AmbariMetaInfo.DefinitionView previous = metadata.get().captureDefinitionView();
    MpackSnapshots.Snapshot previousSnapshot = runtime.snapshot();
    runtime.markUnreadable();
    try {
      metadata.get().publishMpackCandidate(candidate, snapshots.resourceRoot(snapshot.id()));
      refreshConsumers();
      advisors.get().clearDefinitionCaches();
      runtime.publish(operationId, snapshot);
    } catch (AmbariException | RuntimeException failure) {
      try {
        metadata.get().restoreDefinitionView(previous);
        refreshConsumers();
        advisors.get().clearDefinitionCaches();
        runtime.restore(previousSnapshot, operationId);
      } catch (AmbariException | RuntimeException rollback) {
        failure.addSuppressed(rollback);
      }
      throw failure;
    }
  }

  private void refreshConsumers() throws AmbariException {
    for (Cluster cluster : clusters.get().getClusters().values()) {
      for (Service service : cluster.getServices().values()) {
        if (!runtime.conflicts(service.getDesiredStackId(), service.getName())) continue;
        service.updateServiceInfo();
        for (ServiceComponent component : service.getServiceComponents().values()) {
          component.updateComponentInfo();
        }
      }
    }
  }

  @Transactional(rollbackOn = {RuntimeException.class, AmbariException.class})
  public void commitCatalogOnly(MpackLifecycleState.Plan plan,
      MpackCatalog.Versioned<MpackLifecycleState.Operation> operation) {
    runtime.requirePublicationLock();
    MpackCatalog.Versioned<MpackLifecycleState.Control> current = catalog.control();
    requireOwner(current.value(), operation.value());
    if (!plan.previousSnapshot().equals(plan.candidateSnapshot())
        || !plan.candidateSnapshot().equals(current.value().effectiveSnapshot())
        || !new HashSet<>(plan.activeReleases()).equals(new HashSet<>(current.value().activeReleases()))) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Catalog-only operation changed active definitions");
    }
    persistInventory(plan, operation, current, current.value().effectiveSnapshot());
  }

  @Transactional(rollbackOn = {RuntimeException.class, AmbariException.class})
  public void commitEffective(MpackLifecycleState.Plan plan,
      MpackCatalog.Versioned<MpackLifecycleState.Operation> operation) throws AmbariException {
    runtime.requirePublicationLock();
    MpackCatalog.Versioned<MpackLifecycleState.Control> current = catalog.control();
    requireOwner(current.value(), operation.value());
    MpackSnapshots.Snapshot candidate = snapshots.load(plan.candidateSnapshot());
    if (runtime.snapshot() == null || !candidate.id().equals(runtime.snapshot().id())
        || !candidate.id().equals(metadata.get().getStackManager().getDefinitionSnapshotId())) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Runtime publication has not been verified");
    }
    Set<StackResolutionContext.Binding> expected = new HashSet<>(snapshots.load(current.value().effectiveSnapshot()).bindings());
    if (!expected.equals(new HashSet<>(loader.existingBindings()))) {
      throw new MpackException(MpackException.Code.STALE_PLAN, "Effective extension bindings changed outside this operation");
    }
    Set<StackResolutionContext.Binding> desired = new HashSet<>(candidate.bindings());
    for (ExtensionLinkEntity link : linkDAO.findAll()) {
      StackResolutionContext.Binding binding = new StackResolutionContext.Binding(
          link.getStack().getStackName(), link.getStack().getStackVersion(),
          link.getExtension().getExtensionName(), link.getExtension().getExtensionVersion());
      if (!desired.contains(binding)) {
        linkDAO.remove(link);
      }
    }
    for (StackResolutionContext.Binding binding : desired) {
      if (!expected.contains(binding)) {
        ExtensionLinkEntity entity = new ExtensionLinkEntity();
        entity.setStack(stackDAO.find(binding.stackName(), binding.stackVersion()));
        entity.setExtension(extensionDAO.find(binding.extensionName(), binding.extensionVersion()));
        linkDAO.create(entity);
      }
    }
    persistInventory(plan, operation, current, candidate.id());
  }

  private void persistInventory(MpackLifecycleState.Plan plan,
      MpackCatalog.Versioned<MpackLifecycleState.Operation> operation,
      MpackCatalog.Versioned<MpackLifecycleState.Control> current, String effectiveSnapshot) {
    List<MpackRecordDAO.Change> changes = new ArrayList<>();
    Map<String, MpackCatalog.Versioned<MpackLifecycleState.Release>> existing = new java.util.TreeMap<>();
    catalog.releases().forEach(value -> existing.put(value.value().id(), value));
    Set<String> retained = new HashSet<>();
    for (MpackLifecycleState.Release release : plan.releases()) {
      retained.add(release.id());
      MpackCatalog.Versioned<MpackLifecycleState.Release> old = existing.get(release.id());
      if (old != null && !old.value().archiveDigest().equals(release.archiveDigest())) {
        throw new MpackException(MpackException.Code.RELEASE_CONFLICT, "Immutable release content conflicts with inventory");
      }
      if (old == null || !old.value().equals(release)) {
        changes.add(MpackCatalog.change(MpackCatalog.releaseKey(release.id()), Kind.RELEASE,
            old == null ? MpackRecordDAO.ABSENT : old.revision(), release));
      }
    }
    for (MpackCatalog.Versioned<MpackLifecycleState.Release> old : existing.values()) {
      MpackLifecycleState.Release release = old.value();
      if (release.installed() && !retained.contains(release.id())) {
        MpackLifecycleState.Release retired = new MpackLifecycleState.Release(1, release.id(),
            release.archiveDigest(), release.manifestJson(), release.contributions(), release.dependencies(), false);
        changes.add(MpackCatalog.change(old.key(), Kind.RELEASE, old.revision(), retired));
      }
    }
    MpackLifecycleState.Control effective = new MpackLifecycleState.Control(1,
        current.value().builtinSnapshot(), effectiveSnapshot, operation.value().id(), current.value().generation(),
        plan.activeReleases());
    changes.add(MpackCatalog.change(MpackCatalog.CONTROL, Kind.CONTROL, current.revision(), effective));
    changes.add(MpackCatalog.change(operation.key(), Kind.OPERATION, operation.revision(),
        operation.value().transition(MpackLifecycleState.Phase.PUBLISHING, effectiveSnapshot, null, Map.of())));
    catalog.apply(changes);
  }

  public void announce() throws AmbariException {
    runtime.requirePublicationLock();
    agentMetadata.get().refreshDefinitionSnapshot();
  }

  private void requireOwner(MpackLifecycleState.Control control, MpackLifecycleState.Operation operation) {
    if (!operation.id().equals(control.pendingOperation()) || operation.generation() != control.generation()) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Publication belongs to a different operation or generation");
    }
  }
}
