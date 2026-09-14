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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public record ManagedDependencyVersion(
    String stackName,
    String stackVersion,
    boolean active,
    String serviceVersion,
    SortedMap<String, String> resolvedVersions,
    SortedSet<String> clientFeatures,
    Long repositoryRowId,
    List<String> mirrorUrls) {

  /**
   * resolvedVersions is canonical shared distribution/client metadata. Service-specific
   * HBASE or HDFS labels belong in serviceVersion and do not make compatible repositories unequal.
   */

  public ManagedDependencyVersion {
    stackName = Objects.requireNonNull(stackName, "stackName");
    stackVersion = Objects.requireNonNull(stackVersion, "stackVersion");
    serviceVersion = Objects.requireNonNull(serviceVersion, "serviceVersion");
    resolvedVersions = immutableSortedMap(resolvedVersions);
    clientFeatures = clientFeatures == null
        ? java.util.Collections.emptySortedSet()
        : java.util.Collections.unmodifiableSortedSet(new TreeSet<>(clientFeatures));
    mirrorUrls = mirrorUrls == null ? List.of() : List.copyOf(mirrorUrls);
  }

  public boolean resolvedEquivalent(ManagedDependencyVersion other) {
    return other != null
        && stackName.equals(other.stackName)
        && stackVersion.equals(other.stackVersion)
        && resolvedVersions.equals(other.resolvedVersions);
  }

  /** BIGTOP service metadata appends the package release to the upstream version. */
  public static String clientSoftwareVersion(String stackName, String serviceVersion) {
    Objects.requireNonNull(serviceVersion, "serviceVersion");
    if ("BIGTOP".equals(stackName) && serviceVersion.matches("[0-9]+\\.[0-9]+\\.[0-9]+-[0-9]+")) {
      return serviceVersion.substring(0, serviceVersion.lastIndexOf('-'));
    }
    return serviceVersion;
  }

  public Compatibility compatibility() {
    return new Compatibility(stackName, stackVersion, active, serviceVersion,
        resolvedVersions, clientFeatures);
  }

  public record Compatibility(
      String stackName,
      String stackVersion,
      boolean active,
      String serviceVersion,
      SortedMap<String, String> resolvedVersions,
      SortedSet<String> clientFeatures) {
    public Compatibility {
      stackName = Objects.requireNonNull(stackName, "stackName");
      stackVersion = Objects.requireNonNull(stackVersion, "stackVersion");
      serviceVersion = Objects.requireNonNull(serviceVersion, "serviceVersion");
      resolvedVersions = immutableSortedMap(resolvedVersions);
      clientFeatures = clientFeatures == null
          ? java.util.Collections.emptySortedSet()
          : java.util.Collections.unmodifiableSortedSet(new TreeSet<>(clientFeatures));
    }
  }

  static SortedMap<String, String> immutableSortedMap(Map<String, String> values) {
    if (values == null || values.isEmpty()) {
      return java.util.Collections.emptySortedMap();
    }
    TreeMap<String, String> copy = new TreeMap<>();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      copy.put(Objects.requireNonNull(entry.getKey(), "map key"),
          Objects.requireNonNull(entry.getValue(), "map value"));
    }
    return java.util.Collections.unmodifiableSortedMap(copy);
  }
}
