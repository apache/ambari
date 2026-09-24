/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.mpack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackResolutionContext;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** A derived catalog of imported services; Java remains the only definition resolver. */
@Singleton
public class MpackServiceCatalog {
  public record Entry(String id, String releaseId, String archiveDigest, String serviceName,
      String displayName, String description, String serviceVersion, String stackName, String stackVersion,
      List<String> requiredServices, List<StackResolutionContext.Binding> bindings, List<String> releaseIds,
      boolean enabled, boolean clientOnly) { }
  public record Destination(long clusterId, String clusterName, String stackName, String stackVersion) { }
  public record Selection(int schemaVersion, List<String> serviceIds, Long clusterId, boolean maintenance) {
    public Selection {
      if (schemaVersion != 1 || serviceIds == null || serviceIds.isEmpty()
          || serviceIds.size() != new HashSet<>(serviceIds).size() || serviceIds.size() > 256) {
        throw MpackJson.invalid("Select an explicit, nonempty set of catalog services");
      }
      serviceIds = List.copyOf(serviceIds);
      serviceIds.forEach(MpackManifest::requireDigest);
      if (clusterId != null && clusterId < 1) throw MpackJson.invalid("Invalid destination cluster identity");
    }
  }
  public record PreparedSelection(MpackLifecycleState.Mutation mutation, MpackLifecycleState.Deployment deployment) { }
  public record View(int schemaVersion, List<Entry> items, List<Map<String, String>> unavailable,
      List<Destination> destinations) { }
  private record Cached(long revision, List<Entry> entries, List<Map<String, String>> unavailable) { }

  @Inject private MpackCatalog catalog;
  @Inject private MpackSnapshots snapshots;
  @Inject private MpackResources resources;
  @Inject private MpackDefinitionLoader loader;
  @Inject private Provider<Clusters> clusters;
  private volatile Cached cached;

  public synchronized View view() {
    MpackCatalog.Versioned<MpackLifecycleState.Control> control = catalog.control();
    if (control == null) return new View(1, List.of(), List.of(), destinations());
    Cached current = cached;
    if (current == null || current.revision() != control.revision()) {
      current = build(control);
      if (catalog.control().revision() != control.revision()) {
        throw new MpackException(MpackException.Code.STALE_PLAN, "The imported catalog changed; refresh the service list");
      }
      cached = current;
    }
    return new View(1, current.entries(), current.unavailable(), destinations());
  }

  public PreparedSelection prepare(Selection selection) {
    Map<String, Entry> available = new TreeMap<>();
    view().items().forEach(entry -> available.put(entry.id(), entry));
    List<Entry> chosen = selection.serviceIds().stream().map(id -> {
      Entry entry = available.get(id);
      if (entry == null) throw new MpackException(MpackException.Code.STALE_PLAN, "A selected service is no longer available");
      return entry;
    }).toList();
    Entry target = chosen.get(0);
    if (chosen.stream().anyMatch(entry -> !entry.stackName().equals(target.stackName())
        || !entry.stackVersion().equals(target.stackVersion()))) {
      throw new MpackException(MpackException.Code.INVALID_TARGET,
          "Select services for one compatible environment per deployment");
    }
    if (selection.clusterId() != null) {
      Destination destination = destinations().stream().filter(value -> value.clusterId() == selection.clusterId())
          .findFirst().orElseThrow(() -> new MpackException(MpackException.Code.INVALID_TARGET, "The destination cluster is unavailable"));
      if (!destination.stackName().equals(target.stackName()) || !destination.stackVersion().equals(target.stackVersion())) {
        throw new MpackException(MpackException.Code.INVALID_TARGET, "The selected services are incompatible with this cluster");
      }
    }
    List<String> names = chosen.stream().map(Entry::serviceName).distinct().sorted().toList();
    if (names.size() != chosen.size()) throw MpackJson.invalid("Choose one provider for each service");
    List<String> releases = chosen.stream().flatMap(entry -> entry.releaseIds().stream()).distinct().sorted().toList();
    List<StackResolutionContext.Binding> bindings = chosen.stream().flatMap(entry -> entry.bindings().stream()).distinct().toList();
    return new PreparedSelection(new MpackLifecycleState.Mutation(1, MpackLifecycleState.Action.ENABLE,
        List.of(), releases, bindings, true, selection.maintenance()),
        new MpackLifecycleState.Deployment(target.stackName(), target.stackVersion(), selection.clusterId(), names,
            selection.serviceIds().stream().sorted().toList()));
  }

  private Cached build(MpackCatalog.Versioned<MpackLifecycleState.Control> control) {
    Map<String, MpackLifecycleState.Release> installed = new TreeMap<>();
    catalog.releases().forEach(value -> { if (value.value().installed()) installed.put(value.value().id(), value.value()); });
    MpackSnapshots.Snapshot builtin = snapshots.load(control.value().builtinSnapshot());
    MpackSnapshots.Snapshot effective = snapshots.load(control.value().effectiveSnapshot());
    List<Entry> entries = new ArrayList<>();
    List<Map<String, String>> unavailable = new ArrayList<>();
    for (MpackLifecycleState.Release release : installed.values()) {
      try {
        Map<String, MpackLifecycleState.Release> closure = new LinkedHashMap<>();
        collect(release.id(), installed, closure, new HashSet<>());
        List<MpackResources.PreparedPack> packs = closure.values().stream().map(MpackLifecycleState.Release::prepared).toList();
        MpackSnapshots.Snapshot unbound = snapshots.compose(builtin, packs, builtin.bindings());
        StackManager definitions = loader.resolve(unbound);
        List<Map<String, Object>> extensions = packs.stream().flatMap(pack -> resources.extensions(pack).stream()).toList();
        for (StackInfo stack : definitions.getStacks()) {
          if (!stack.isActive()) continue;
          List<StackResolutionContext.Binding> bindings = new ArrayList<>(builtin.bindings());
          for (Map<String, Object> extension : extensions) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> minimum = (List<Map<String, String>>) extension.get("minimum_stacks");
            if (minimum.stream().anyMatch(value -> stack.getName().equals(value.get("stack_name"))
                && MpackManifest.compareVersions(stack.getVersion(), value.get("stack_version")) >= 0)) {
              bindings.add(new StackResolutionContext.Binding(stack.getName(), stack.getVersion(),
                  (String) extension.get("name"), (String) extension.get("version")));
            }
          }
          MpackSnapshots.Snapshot candidate = bindings.equals(builtin.bindings()) ? unbound : snapshots.compose(builtin, packs, bindings);
          StackManager resolved = candidate == unbound ? definitions : loader.resolve(candidate);
          for (ServiceInfo service : resolved.getStack(stack.getName(), stack.getVersion()).getServices()) {
            if (!ownsService(candidate, release.id(), service)) continue;
            List<StackResolutionContext.Binding> selectedBindings = bindings.stream().filter(binding ->
                !builtin.bindings().contains(binding)).toList();
            String id = MpackJson.digest(MpackJson.tree(Map.of("release", release.id(), "archive", release.archiveDigest(),
                "service", service.getName(), "stack", stack.getName(), "version", stack.getVersion())));
            boolean enabled = control.value().activeReleases().containsAll(closure.keySet())
                && effective.bindings().containsAll(selectedBindings);
            entries.add(new Entry(id, release.id(), release.archiveDigest(), service.getName(),
                service.getDisplayName(), service.getComment(), service.getVersion(), stack.getName(), stack.getVersion(),
                List.copyOf(service.getRequiredServices()), selectedBindings, List.copyOf(closure.keySet()), enabled,
                !service.getComponents().isEmpty() && service.getComponents().stream().allMatch(component -> component.isClient())));
          }
        }
      } catch (MpackException e) {
        unavailable.add(Map.of("release_id", release.id(), "code", e.getCode().name(), "message", e.getMessage()));
      }
    }
    return new Cached(control.revision(), List.copyOf(entries), List.copyOf(unavailable));
  }

  private boolean ownsService(MpackSnapshots.Snapshot snapshot, String release, ServiceInfo service) {
    Path root = snapshots.resourceRoot(snapshot.id());
    return service.getDefinitionResourceRoots().stream().anyMatch(directory -> {
      Path source = Path.of(directory).resolve("metainfo.xml").toAbsolutePath().normalize();
      if (!source.startsWith(root)) return false;
      String path = root.relativize(source).toString().replace(source.getFileSystem().getSeparator(), "/");
      MpackSnapshots.Resource resource = snapshot.resources().get(path);
      return resource != null && resource.provider().equals(release);
    });
  }

  private static void collect(String id, Map<String, MpackLifecycleState.Release> installed,
      Map<String, MpackLifecycleState.Release> selected, Set<String> visiting) {
    if (selected.containsKey(id)) return;
    if (!visiting.add(id)) throw new MpackException(MpackException.Code.DEPENDENCY_CYCLE, "Imported dependency cycle");
    MpackLifecycleState.Release release = installed.get(id);
    if (release == null) throw new MpackException(MpackException.Code.DEPENDENCY_MISSING, "An imported provider is unavailable");
    release.dependencies().values().forEach(provider -> {
      if (!installed.containsKey(provider.release()) || !installed.get(provider.release()).archiveDigest().equals(provider.digest())) {
        throw new MpackException(MpackException.Code.DEPENDENCY_MISSING, "An exact dependency provider is unavailable");
      }
      collect(provider.release(), installed, selected, visiting);
    });
    visiting.remove(id);
    selected.put(id, release);
  }

  private List<Destination> destinations() {
    return clusters.get().getClusters().values().stream().map(cluster -> new Destination(cluster.getClusterId(),
        cluster.getClusterName(), cluster.getDesiredStackVersion().getStackName(),
        cluster.getDesiredStackVersion().getStackVersion())).sorted(java.util.Comparator.comparingLong(Destination::clusterId)).toList();
  }
}
