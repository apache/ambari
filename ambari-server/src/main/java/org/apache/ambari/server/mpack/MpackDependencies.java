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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Resolves an explicit provider set without catalog searches or newest-version selection. */
public final class MpackDependencies {
  public record Provider(MpackManifest manifest, String digest) {
    public Provider {
      MpackManifest.requireDigest(digest);
    }

    public String identity() {
      return manifest.identity();
    }
  }

  public record Reference(String release, String digest) {
  }

  public record Resolution(Reference provider, Map<String, Reference> dependencies) {
    public Resolution {
      dependencies = Map.copyOf(dependencies);
    }
  }

  private MpackDependencies() {
  }

  public static List<Resolution> resolve(Collection<Provider> requested, Collection<Provider> available) {
    Map<String, Provider> catalog = new TreeMap<>();
    for (Provider provider : available) {
      add(catalog, provider);
    }
    for (Provider provider : requested) {
      add(catalog, provider);
    }
    Map<String, Resolution> resolved = new LinkedHashMap<>();
    Set<String> visiting = new HashSet<>();
    requested.stream().sorted(java.util.Comparator.comparing(Provider::identity))
        .forEach(provider -> visit(provider, catalog.values(), visiting, resolved));
    return List.copyOf(resolved.values());
  }

  public static void verifyRetainedReferences(Collection<Resolution> existing,
      Collection<Provider> retained, Set<String> removedConsumers) {
    Map<String, Provider> catalog = new TreeMap<>();
    retained.forEach(provider -> add(catalog, provider));
    List<Map<String, String>> blockers = new ArrayList<>();
    for (Resolution resolution : existing) {
      if (removedConsumers.contains(resolution.provider().release())) {
        continue;
      }
      for (Reference dependency : resolution.dependencies().values()) {
        Provider provider = catalog.get(dependency.release());
        if (provider == null || !provider.digest().equals(dependency.digest())) {
          blockers.add(Map.of("consumer", resolution.provider().release(),
              "provider", dependency.release(), "digest", dependency.digest()));
        }
      }
    }
    if (!blockers.isEmpty()) {
      throw new MpackException(MpackException.Code.RESOURCE_IN_USE,
          "The change would remove providers required by retained consumers",
          Map.of("blockers", blockers));
    }
  }

  private static void add(Map<String, Provider> catalog, Provider provider) {
    Provider previous = catalog.putIfAbsent(provider.identity(), provider);
    if (previous != null && (!previous.digest().equals(provider.digest())
        || !previous.manifest().canonicalJson().equals(provider.manifest().canonicalJson()))) {
      throw new MpackException(MpackException.Code.RELEASE_CONFLICT,
          "The same package release identifies different content",
          Map.of("release", provider.identity()));
    }
  }

  private static void visit(Provider provider, Collection<Provider> catalog, Set<String> visiting,
      Map<String, Resolution> resolved) {
    if (resolved.containsKey(provider.identity())) {
      return;
    }
    if (!visiting.add(provider.identity())) {
      throw new MpackException(MpackException.Code.DEPENDENCY_CYCLE,
          "Management pack dependency cycle", Map.of("release", provider.identity()));
    }
    Map<String, Reference> references = new TreeMap<>();
    for (MpackManifest.Dependency dependency : provider.manifest().dependencies()) {
      List<Provider> candidates = catalog.stream()
          .filter(candidate -> candidate.manifest().name().equals(dependency.name()))
          .filter(candidate -> dependency.accepts(candidate.manifest().version(), candidate.digest()))
          .toList();
      if (candidates.isEmpty()) {
        throw new MpackException(MpackException.Code.DEPENDENCY_MISSING,
            "A required management pack provider is unavailable",
            Map.of("consumer", provider.identity(), "dependency", dependency.name()));
      }
      if (candidates.size() != 1) {
        throw new MpackException(MpackException.Code.DEPENDENCY_AMBIGUOUS,
            "Select an exact provider for the dependency",
            Map.of("consumer", provider.identity(), "dependency", dependency.name(),
                "providers", candidates.stream().map(Provider::identity).sorted().toList()));
      }
      Provider selected = candidates.get(0);
      visit(selected, catalog, visiting, resolved);
      references.put(dependency.name(), new Reference(selected.identity(), selected.digest()));
    }
    visiting.remove(provider.identity());
    resolved.put(provider.identity(), new Resolution(new Reference(provider.identity(), provider.digest()), references));
  }
}
