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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackResolutionContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Builds reviewable plans from exact catalog revisions and authoritative service usage. */
@Singleton
public class MpackPlanner {
  private final MpackCatalog catalog;
  private final MpackResources resources;
  private final MpackSnapshots snapshots;
  private final MpackDefinitionLoader loader;
  private final Provider<AmbariMetaInfo> metadata;
  private final Provider<Clusters> clusters;
  @Inject private org.apache.ambari.server.configuration.Configuration configuration;

  @Inject
  public MpackPlanner(MpackCatalog catalog, MpackResources resources, MpackSnapshots snapshots,
      MpackDefinitionLoader loader, Provider<AmbariMetaInfo> metadata, Provider<Clusters> clusters) {
    this.catalog = catalog;
    this.resources = resources;
    this.snapshots = snapshots;
    this.loader = loader;
    this.metadata = metadata;
    this.clusters = clusters;
  }

  public MpackLifecycleState.Plan plan(MpackLifecycleState.Mutation mutation) {
    return plan(mutation, null);
  }

  public MpackLifecycleState.Plan plan(MpackLifecycleState.Mutation mutation, MpackLifecycleState.Deployment deployment) {
    MpackCatalog.Versioned<MpackLifecycleState.Control> versioned = catalog.control();
    if (versioned == null) {
      MpackSnapshots.Snapshot builtin = snapshots.captureBuiltins(loader.existingBindings());
      loader.resolve(builtin);
      versioned = new MpackCatalog.Versioned<>(MpackCatalog.CONTROL,
          org.apache.ambari.server.orm.dao.MpackRecordDAO.ABSENT,
          new MpackLifecycleState.Control(1, builtin.id(), builtin.id(), null, 0, List.of()));
    }
    if (versioned.value().pendingOperation() != null) {
      throw new MpackException(MpackException.Code.OPERATION_CONFLICT,
          "The catalog must be initialized and free of unfinished definition mutations");
    }
    MpackLifecycleState.Control control = versioned.value();
    Map<String, MpackLifecycleState.Release> installed = new TreeMap<>();
    catalog.releases().stream().map(MpackCatalog.Versioned::value).filter(MpackLifecycleState.Release::installed)
        .forEach(release -> installed.put(release.id(), release));
    for (String id : mutation.releaseIds()) {
      if (!installed.containsKey(id)) {
        throw new MpackException(MpackException.Code.NOT_FOUND, "Selected release is not installed",
            Map.of("release", id));
      }
    }
    List<MpackResources.PreparedPack> imports = mutation.archiveDigests().stream().map(resources::inspect).toList();
    for (MpackCatalog.Versioned<MpackLifecycleState.Release> known : catalog.releases()) {
      for (MpackResources.PreparedPack pack : imports) {
        if (known.value().id().equals(pack.manifest().identity())
            && !known.value().archiveDigest().equals(pack.archiveDigest())) {
          throw new MpackException(MpackException.Code.RELEASE_CONFLICT,
              "A retained release identity cannot be reused for different content");
        }
      }
    }
    List<MpackDependencies.Provider> requested = imports.stream()
        .map(pack -> new MpackDependencies.Provider(pack.manifest(), pack.archiveDigest())).toList();
    List<MpackDependencies.Provider> available = installed.values().stream()
        .map(release -> new MpackDependencies.Provider(release.manifest(), release.archiveDigest())).toList();
    Map<String, MpackDependencies.Resolution> resolved = new TreeMap<>();
    MpackDependencies.resolve(requested, available).forEach(value -> resolved.put(value.provider().release(), value));
    Map<String, MpackLifecycleState.Release> proposed = new TreeMap<>(installed);
    for (MpackResources.PreparedPack pack : imports) {
      pack.manifest().verifyAmbariVersion(metadata.get().getServerVersion());
      MpackDependencies.Resolution resolution = resolved.get(pack.manifest().identity());
      proposed.put(pack.manifest().identity(), new MpackLifecycleState.Release(1, pack.manifest().identity(),
          pack.archiveDigest(), pack.manifest().canonicalJson(), pack.contributions(), resolution.dependencies(), true));
    }
    Set<String> active = new TreeSet<>(control.activeReleases());
    switch (mutation.action()) {
      case IMPORT -> { }
      case INSTALL -> {
        if (mutation.activate()) {
          active.addAll(resolved.keySet());
        }
      }
      case UPDATE -> {
        Set<String> oldNames = mutation.releaseIds().stream().map(id -> installed.get(id).manifest().name())
            .collect(Collectors.toSet());
        Set<String> newNames = imports.stream().map(pack -> pack.manifest().name()).collect(Collectors.toSet());
        if (!oldNames.equals(newNames)) {
          throw MpackJson.invalid("Updates must identify the old release of each uploaded package");
        }
        active.removeAll(mutation.releaseIds());
        active.addAll(resolved.keySet());
      }
      case ENABLE, BIND -> {
        for (String id : mutation.releaseIds()) {
          addActiveClosure(id, proposed, active);
        }
      }
      case UNBIND -> {
        // Resource retirement is explicit; removing a link alone retains its definitions.
      }
      case UNINSTALL -> {
        active.removeAll(mutation.releaseIds());
        mutation.releaseIds().forEach(proposed::remove);
      }
      default -> throw MpackJson.invalid("Unsupported mutation action");
    }
    List<MpackDependencies.Resolution> existing = proposed.values().stream().map(release ->
        new MpackDependencies.Resolution(new MpackDependencies.Reference(release.id(), release.archiveDigest()),
            release.dependencies())).toList();
    MpackDependencies.verifyRetainedReferences(existing, proposed.values().stream()
        .map(release -> new MpackDependencies.Provider(release.manifest(), release.archiveDigest())).toList(), Set.of());
    // Retaining bytes is insufficient when an active consumer's provider is absent from the projection.
    MpackDependencies.verifyRetainedReferences(existing.stream()
        .filter(resolution -> active.contains(resolution.provider().release())).toList(),
        active.stream().map(proposed::get).map(release ->
            new MpackDependencies.Provider(release.manifest(), release.archiveDigest())).toList(), Set.of());

    MpackSnapshots.Snapshot previous = snapshots.load(control.effectiveSnapshot());
    if (mutation.action() == MpackLifecycleState.Action.UNINSTALL) {
      Set<String> scopes = mutation.releaseIds().stream().map(installed::get)
          .filter(release -> control.activeReleases().contains(release.id()))
          .flatMap(release -> release.contributions().stream()).map(MpackResources.Contribution::scope)
          .collect(Collectors.toSet());
      List<StackResolutionContext.Binding> blockers = previous.bindings().stream().filter(binding ->
          scopes.contains("extensions/" + binding.extensionName() + "/" + binding.extensionVersion())).toList();
      if (!blockers.isEmpty()) {
        throw new MpackException(MpackException.Code.RESOURCE_IN_USE,
            "Unbind the selected extension definitions before uninstalling their package",
            Map.of("bindings", blockers));
      }
    }
    List<StackResolutionContext.Binding> bindings = bindings(mutation, previous.bindings(), proposed);
    MpackSnapshots.Snapshot candidate = mutation.action() == MpackLifecycleState.Action.IMPORT
        || (mutation.action() == MpackLifecycleState.Action.INSTALL && !mutation.activate()) ? previous
        : snapshots.compose(snapshots.load(control.builtinSnapshot()),
            active.stream().map(proposed::get).map(MpackLifecycleState.Release::prepared).toList(), bindings);
    StackManager candidateMetadata = loader.resolve(candidate);
    for (MpackLifecycleState.Release release : proposed.values()) {
      if (!active.contains(release.id())) continue;
      List<MpackManifest.Target> requirements = release.manifest().minStackVersions();
      if (!requirements.isEmpty() && requirements.stream().noneMatch(required -> candidateMetadata.getStacks().stream()
          .anyMatch(stack -> stack.getName().equals(required.stackName())
              && MpackManifest.compareVersions(stack.getVersion(), required.stackVersion()) >= 0))) {
        throw new MpackException(MpackException.Code.VERSION_INCOMPATIBLE,
            "No stack satisfies the management pack prerequisites", Map.of("release", release.id()));
      }
    }
    Impact impact = impact(previous, candidate, candidateMetadata);
    if (impact.inUseDefinitions() && !mutation.maintenance()) {
      throw new MpackException(MpackException.Code.MAINTENANCE_REQUIRED,
          "Updating in-use definitions requires an explicit maintenance policy",
          Map.of("affected_clusters", impact.clusters()));
    }
    String id = UUID.randomUUID().toString();
    MpackLifecycleState.Plan unsigned = new MpackLifecycleState.Plan(1, id, "0".repeat(64),
        versioned.revision(), System.currentTimeMillis() + 3600000, mutation, previous.id(), candidate.id(),
        List.copyOf(proposed.values()), List.copyOf(active), impact.clusters(), impact.inUseDefinitions(),
        impact.restartRequired(), definitionScopes(previous, candidate, candidateMetadata),
        active.stream().filter(release -> !control.activeReleases().contains(release)).toList(),
        mutation.action() == MpackLifecycleState.Action.UNINSTALL
            ? control.activeReleases().stream().filter(mutation.releaseIds()::contains).toList() : List.of(), deployment);
    ObjectNode body = (ObjectNode) MpackJson.tree(unsigned);
    List<MpackLifecycleState.Release> hookMembers = mutation.action() == MpackLifecycleState.Action.UNINSTALL
        ? mutation.releaseIds().stream().map(installed::get).toList() : unsigned.releases();
    for (MpackLifecycleState.Release release : hookMembers) {
      boolean selected = mutation.archiveDigests().contains(release.archiveDigest()) ||
          (mutation.action() == MpackLifecycleState.Action.ENABLE && unsigned.initializingReleases().contains(release.id())) ||
          mutation.action() == MpackLifecycleState.Action.UNINSTALL;
      if (selected && release.manifest().hooks().stream().anyMatch(hook ->
          MpackLifecycleState.requiredHooks(unsigned, release).contains(MpackHookRunner.key(release, hook))
              && !"DEFINITIONS".equals(hook.scope()))) {
        throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION,
            "Online activation requires hooks restricted to the declared definition resources",
            Map.of("release_id", release.id(), "reason", "UNBOUNDED_HOOK_SCOPE"));
      }
    }
    body.remove("digest");
    return new MpackLifecycleState.Plan(1, id, MpackJson.digest(body), unsigned.catalogRevision(),
        unsigned.expiresAt(), mutation, previous.id(), candidate.id(), unsigned.releases(),
        unsigned.activeReleases(), unsigned.affectedClusters(), unsigned.maintenanceRequired(), unsigned.restartRequired(),
        unsigned.affectedDefinitions(), unsigned.initializingReleases(), unsigned.retiringReleases(), deployment);
  }

  public List<MpackScope> definitionScopes(MpackSnapshots.Snapshot previous,
      MpackSnapshots.Snapshot candidate, StackManager next) {
    if (previous.id().equals(candidate.id())) {
      return List.of();
    }
    StackManager old = loader.resolve(previous);
    Map<String, StackInfo> contexts = new TreeMap<>();
    old.getStacks().forEach(stack -> contexts.put(stack.getName() + "/" + stack.getVersion(), stack));
    next.getStacks().forEach(stack -> contexts.put(stack.getName() + "/" + stack.getVersion(), stack));
    List<MpackScope> result = new ArrayList<>();
    for (StackInfo context : contexts.values()) {
      StackId id = new StackId(context.getName(), context.getVersion());
      StackInfo before = old.getStack(id.getStackName(), id.getStackVersion());
      StackInfo after = next.getStack(id.getStackName(), id.getStackVersion());
      if (before == null || after == null) {
        result.add(new MpackScope(id.getStackName(), id.getStackVersion(), null, Set.of()));
        continue;
      }
      Set<String> services = new TreeSet<>();
      before.getServices().forEach(service -> services.add(service.getName()));
      after.getServices().forEach(service -> services.add(service.getName()));
      if (services.isEmpty() && !sameContent(contextResources(previous, old, id), contextResources(candidate, next, id))) {
        result.add(new MpackScope(id.getStackName(), id.getStackVersion(), null, Set.of()));
      }
      for (String service : services) {
        ServiceInfo first = before.getService(service);
        ServiceInfo second = after.getService(service);
        if (first == null || second == null || !sameContent(serviceResources(previous, old, id, first),
            serviceResources(candidate, next, id, second))) {
          Set<String> types = new TreeSet<>();
          types.add("cluster-env");
          if (first != null) types.addAll(first.getConfigTypeAttributes().keySet());
          if (second != null) types.addAll(second.getConfigTypeAttributes().keySet());
          result.add(new MpackScope(id.getStackName(), id.getStackVersion(), service, types));
        }
      }
    }
    return List.copyOf(result);
  }

  static boolean sameContent(Map<String, MpackSnapshots.Resource> first, Map<String, MpackSnapshots.Resource> second) {
    return first.keySet().equals(second.keySet()) && first.entrySet().stream().allMatch(entry -> {
      MpackSnapshots.Resource other = second.get(entry.getKey());
      return entry.getValue().digest().equals(other.digest()) && entry.getValue().executable() == other.executable()
          && entry.getValue().size() == other.size();
    });
  }

  public String executionIdentity(MpackSnapshots.Snapshot snapshot, StackManager definitions,
      StackId stack, String service) {
    Map<String, MpackSnapshots.Resource> selected = service == null
        ? contextResources(snapshot, definitions, stack)
        : serviceResources(snapshot, definitions, stack,
            definitions.getStack(stack.getStackName(), stack.getStackVersion()).getService(service));
    Map<String, Object> contents = new TreeMap<>();
    selected.forEach((path, value) -> contents.put(path, List.of(value.digest(), value.size(), value.executable())));
    return MpackJson.digest(MpackJson.tree(contents));
  }

  public record Impact(List<String> clusters, boolean inUseDefinitions, boolean restartRequired) {
    public Impact {
      clusters = List.copyOf(clusters);
    }
  }

  public Impact impact(MpackSnapshots.Snapshot previous, MpackSnapshots.Snapshot candidate, StackManager resolved) {
    return impact(previous, candidate, previous.id().equals(candidate.id()) ? resolved : loader.resolve(previous), resolved);
  }

  public Impact impact(MpackSnapshots.Snapshot previous, MpackSnapshots.Snapshot candidate, StackManager old, StackManager resolved) {
    Set<String> affected = new TreeSet<>();
    boolean restart = false;
    boolean inUse = false;
    if (previous.id().equals(candidate.id())) {
      return new Impact(List.of(), false, false);
    }
    for (Cluster cluster : clusters.get().getClusters().values()) {
      StackId clusterStack = cluster.getDesiredStackVersion();
      if (resolved.getStack(clusterStack.getStackName(), clusterStack.getStackVersion()) == null) {
        throw new MpackException(MpackException.Code.RESOURCE_IN_USE,
            "A cluster still references the stack definition being removed",
            Map.of("cluster", cluster.getClusterName(), "stack", clusterStack.getStackId()));
      }
      if (!contextResources(previous, old, clusterStack).equals(contextResources(candidate, resolved, clusterStack))) {
        affected.add(cluster.getClusterName());
      }
      for (Service service : cluster.getServices().values()) {
        StackId stackId = service.getDesiredStackId();
        StackInfo nextStack = resolved.getStack(stackId.getStackName(), stackId.getStackVersion());
        ServiceInfo next = nextStack == null ? null : nextStack.getService(service.getName());
        if (next == null) {
          throw new MpackException(MpackException.Code.RESOURCE_IN_USE,
              "A deployed service still references the definition being removed",
              Map.of("cluster", cluster.getClusterName(), "service", service.getName()));
        }
        Set<String> componentNames = next.getComponents().stream().map(component -> component.getName())
            .collect(Collectors.toSet());
        if (!componentNames.containsAll(service.getServiceComponents().keySet())) {
          throw new MpackException(MpackException.Code.RESOURCE_IN_USE,
              "Remove or migrate deployed components before removing their definitions",
              Map.of("cluster", cluster.getClusterName(), "service", service.getName()));
        }
        ServiceInfo before = old.getStack(stackId.getStackName(), stackId.getStackVersion()).getService(service.getName());
        for (org.apache.ambari.server.state.ComponentInfo component : next.getComponents()) {
          org.apache.ambari.server.state.ComponentInfo previousComponent = before.getComponentByName(component.getName());
          if (service.getServiceComponents().containsKey(component.getName()) && previousComponent != null
              && (!java.util.Objects.equals(previousComponent.getCategory(), component.getCategory())
                  || !java.util.Objects.equals(previousComponent.getCardinality(), component.getCardinality())
                  || previousComponent.isVersionAdvertised() != component.isVersionAdvertised())) {
            throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION,
                "Changing a deployed component model requires an explicit migration",
                Map.of("cluster_id", cluster.getClusterId(), "service", service.getName(),
                    "component", component.getName()));
          }
        }
        if (!sameContent(serviceResources(previous, old, stackId, before),
            serviceResources(candidate, resolved, stackId, next))) {
          affected.add(cluster.getClusterName());
          inUse = true;
          if (!componentNames.equals(before.getComponents().stream().map(component -> component.getName()).collect(Collectors.toSet()))) {
            throw new MpackException(MpackException.Code.UNSUPPORTED_OPERATION,
                "Changing an in-use component set requires an explicit migration",
                Map.of("cluster_id", cluster.getClusterId(), "service", service.getName()));
          }
        }
      }
      if (affected.contains(cluster.getClusterName()) && cluster.getUpgradeInProgress() != null) {
        throw new MpackException(MpackException.Code.OPERATION_CONFLICT,
            "Finish the active stack upgrade before changing its definitions",
            Map.of("cluster", cluster.getClusterName(),
                "upgrade_id", cluster.getUpgradeInProgress().getId()));
      }
    }
    return new Impact(List.copyOf(affected), inUse, restart);
  }

  private List<StackResolutionContext.Binding> bindings(MpackLifecycleState.Mutation mutation,
      List<StackResolutionContext.Binding> previous, Map<String, MpackLifecycleState.Release> proposed) {
    Map<List<String>, StackResolutionContext.Binding> result = new LinkedHashMap<>();
    previous.forEach(binding -> result.put(bindingKey(binding), binding));
    if (mutation.action() == MpackLifecycleState.Action.UNINSTALL) {
      Set<String> retiredExtensions = new HashSet<>();
      for (String id : mutation.releaseIds()) {
        if (!catalog.control().value().activeReleases().contains(id)) continue;
        catalog.release(id).value().contributions().stream()
            .map(MpackResources.Contribution::scope).filter(scope -> scope.startsWith("extensions/"))
            .forEach(retiredExtensions::add);
      }
      result.values().removeIf(binding -> retiredExtensions.contains(
          "extensions/" + binding.extensionName() + "/" + binding.extensionVersion()));
    }
    for (StackResolutionContext.Binding binding : mutation.bindings()) {
      if (mutation.action() == MpackLifecycleState.Action.UNBIND) {
        String scope = "extensions/" + binding.extensionName() + "/" + binding.extensionVersion();
        if (mutation.releaseIds().stream().map(proposed::get)
            .noneMatch(release -> release.contributions().stream().anyMatch(value -> value.scope().equals(scope)))) {
          throw new MpackException(MpackException.Code.INVALID_TARGET,
              "The selected package does not own the binding being removed");
        }
        if (!binding.equals(result.get(bindingKey(binding)))) {
          throw new MpackException(MpackException.Code.STALE_PLAN, "The selected binding is no longer effective");
        }
        result.remove(bindingKey(binding));
      } else {
        String prefix = "extensions/" + binding.extensionName() + "/" + binding.extensionVersion() + "/";
        boolean owned = proposed.values().stream().anyMatch(release ->
            (mutation.releaseIds().contains(release.id()) || mutation.archiveDigests().contains(release.archiveDigest()))
                && release.contributions().stream().anyMatch(value -> value.target().startsWith(prefix)));
        if (!owned) {
          throw new MpackException(MpackException.Code.INVALID_TARGET,
              "The selected package does not provide the requested extension");
        }
        StackResolutionContext.Binding old = result.get(bindingKey(binding));
        if (old != null && !old.equals(binding) && mutation.action() != MpackLifecycleState.Action.UPDATE) {
          throw new MpackException(MpackException.Code.RESOURCE_CONFLICT, "Use update to replace an existing binding");
        }
        result.put(bindingKey(binding), binding);
      }
    }
    return List.copyOf(result.values());
  }

  private static List<String> bindingKey(StackResolutionContext.Binding binding) {
    return List.of(binding.stackName(), binding.stackVersion(), binding.extensionName());
  }

  private static void addActiveClosure(String id, Map<String, MpackLifecycleState.Release> releases, Set<String> active) {
    if (active.add(id)) {
      MpackLifecycleState.Release release = releases.get(id);
      if (release == null) {
        throw new MpackException(MpackException.Code.DEPENDENCY_MISSING, "An installed dependency is missing");
      }
      release.dependencies().values().forEach(reference -> addActiveClosure(reference.release(), releases, active));
    }
  }

  private Map<String, MpackSnapshots.Resource> serviceResources(MpackSnapshots.Snapshot snapshot,
      StackManager definitions, StackId stack, ServiceInfo service) {
    Map<String, MpackSnapshots.Resource> selected = contextResources(snapshot, definitions, stack);
    selected.entrySet().removeIf(entry -> entry.getKey().contains("/services/")
        && !entry.getKey().endsWith("/services/stack_advisor.py"));
    if (service == null) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Deployed service is absent from the effective snapshot");
    }
    java.nio.file.Path root = snapshots.resourceRoot(snapshot.id()).toAbsolutePath().normalize();
    Set<String> extensionRoots = new HashSet<>();
    for (String directory : service.getDefinitionResourceRoots()) {
      java.nio.file.Path source = java.nio.file.Path.of(directory).toAbsolutePath().normalize();
      if (!source.startsWith(root)) {
        throw new MpackException(MpackException.Code.INVALID_RECEIPT, "Service provenance leaves its snapshot");
      }
      String prefix = root.relativize(source).toString().replace(source.getFileSystem().getSeparator(), "/") + "/";
      String[] parts = prefix.split("/");
      if (parts.length >= 3 && parts[0].equals("extensions")) {
        extensionRoots.add(String.join("/", java.util.Arrays.copyOf(parts, 3)) + "/");
      }
      snapshot.resources().forEach((path, value) -> {
        if (path.startsWith(prefix)) {
          selected.put(path, value);
        }
      });
    }
    String hooks = definitions.getStack(stack.getStackName(), stack.getStackVersion()).getHooksFolder();
    if (hooks == null) hooks = configuration.getProperty(org.apache.ambari.server.configuration.Configuration.HOOKS_FOLDER);
    final String sharedHooks = hooks.isEmpty() ? null : hooks + "/";
    selected.keySet().removeIf(path -> path.startsWith("extensions/")
        && extensionRoots.stream().noneMatch(path::startsWith)
        && (sharedHooks == null || !path.startsWith(sharedHooks)));
    return selected;
  }

  private Map<String, MpackSnapshots.Resource> contextResources(MpackSnapshots.Snapshot snapshot,
      StackManager definitions, StackId stack) {
    Set<String> prefixes = new HashSet<>();
    StackInfo current = definitions.getStack(stack.getStackName(), stack.getStackVersion());
    String hooks = current == null ? null : current.getHooksFolder();
    if (hooks == null) {
      hooks = configuration.getProperty(org.apache.ambari.server.configuration.Configuration.HOOKS_FOLDER);
    }
    String hookPrefix = hooks.isEmpty() ? null : hooks + "/";
    while (current != null) {
      prefixes.add("stacks/" + current.getName() + "/" + current.getVersion() + "/");
      current.getExtensions().forEach(extension -> prefixes.add(
          "extensions/" + extension.getName() + "/" + extension.getVersion() + "/"));
      String parent = current.getParentStackVersion();
      current = parent == null || parent.isEmpty() ? null : definitions.getStack(current.getName(), parent);
    }
    Map<String, MpackSnapshots.Resource> result = new TreeMap<>();
    snapshot.resources().forEach((path, value) -> {
      if (prefixes.stream().anyMatch(path::startsWith)
          || path.equals("stacks/stack_advisor.py")
          || path.equals("stacks/service_advisor.py") || path.equals("stacks/ambari_configuration.py")
          || path.equals("stacks/" + stack.getStackName() + "/stack_advisor.py")
          || (hookPrefix != null && path.startsWith(hookPrefix)) || path.startsWith("host_scripts/")) {
        result.put(path, value);
      }
    });
    return result;
  }
}
