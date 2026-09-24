/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Usage is a derived observation, never a separately writable eligibility flag. */
@Singleton
public class MpackUsage {
  @Inject private MpackCatalog catalog;
  @Inject private MpackSnapshots snapshots;
  @Inject private Provider<Clusters> clusters;
  @Inject private MpackRuntime runtime;
  @Inject private MpackDefinitionLoader loader;

  public Map<String, Object> release(String id) {
    java.util.concurrent.locks.Lock read = runtime.readLock();
    read.lock();
    try {
      MpackLifecycleState.Release release = catalog.release(id).value();
      MpackCatalog.Versioned<MpackLifecycleState.Control> control = catalog.control();
      List<Map<String, Object>> blockers = new ArrayList<>();
      Set<String> scopes = release.contributions().stream().map(MpackResources.Contribution::scope)
          .collect(Collectors.toSet());
      for (MpackCatalog.Versioned<MpackLifecycleState.Release> consumer : catalog.releases()) {
        if (consumer.value().installed() && consumer.value().dependencies().values().stream()
            .anyMatch(reference -> reference.release().equals(id))) {
          blockers.add(Map.of("type", "PACKAGE_DEPENDENCY", "release_id", consumer.value().id()));
        }
      }
      if (control != null) {
        MpackSnapshots.Snapshot published = snapshots.load(control.value().effectiveSnapshot());
        for (org.apache.ambari.server.stack.StackResolutionContext.Binding binding :
            published.bindings()) {
          MpackSnapshots.Resource owner = published.resources().get(
              "extensions/" + binding.extensionName() + "/" + binding.extensionVersion() + "/metainfo.xml");
          if (owner != null && owner.provider().equals(id)) {
            blockers.add(Map.of("type", "BINDING", "binding", binding));
          }
        }
        if (control.value().pendingOperation() != null) {
          blockers.add(Map.of("type", "LIFECYCLE_OPERATION", "operation_id", control.value().pendingOperation()));
        }
      }
      MpackSnapshots.Snapshot effective = control == null ? null : snapshots.load(control.value().effectiveSnapshot());
      org.apache.ambari.server.stack.StackManager definitions = effective == null ? null : loader.resolve(effective);
      clusters.get().getClusters().values().forEach(cluster -> {
        StackId stack = cluster.getDesiredStackVersion();
        MpackSnapshots.Resource stackOwner = effective == null ? null : effective.resources().get(
            "stacks/" + stack.getStackName() + "/" + stack.getStackVersion() + "/metainfo.xml");
        if (stackOwner != null && stackOwner.provider().equals(id)) {
          blockers.add(Map.of("type", "CLUSTER_STACK", "cluster_id", cluster.getClusterId(),
              "cluster_name", cluster.getClusterName(), "stack", stack.getStackId()));
        }
        if (definitions != null) {
          for (org.apache.ambari.server.state.Service service : cluster.getServices().values()) {
            StackId context = service.getDesiredStackId();
            org.apache.ambari.server.state.ServiceInfo definition = definitions.getStack(
                context.getStackName(), context.getStackVersion()).getService(service.getName());
            if (definition == null) {
              throw new MpackException(MpackException.Code.INVALID_RECEIPT, "A deployed service has no effective definition");
            }
            for (String directory : definition.getDefinitionResourceRoots()) {
              java.nio.file.Path source = java.nio.file.Path.of(directory).resolve("metainfo.xml");
              String path = snapshots.resourceRoot(effective.id()).relativize(source).toString()
                  .replace(source.getFileSystem().getSeparator(), "/");
              MpackSnapshots.Resource resource = effective.resources().get(path);
              if (resource != null && resource.provider().equals(id)) {
                blockers.add(Map.of("type", "DEPLOYED_SERVICE", "cluster_id", cluster.getClusterId(),
                    "service_name", service.getName()));
                break;
              }
            }
          }
        }
      });
      return Map.of("schema_version", 1, "release_id", id, "blockers", blockers,
          "can_uninstall", release.installed() && blockers.isEmpty(),
          "archive_retention", "REFERENCES_AND_RECOVERY_POLICY",
          "catalog_revision", control == null ? -1 : control.revision());
    } finally {
      read.unlock();
    }
  }
}
