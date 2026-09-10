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

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.topology.Blueprint;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Blueprint requirements are declarations; only approved live bindings authorize execution. */
@Singleton
public class ManagedDependencyBlueprintPlan {
  public static final String SETTING = "managed_dependencies";
  private final TopologyRequestDAO topologyRequests;
  private final ServiceDependencyDAO dependencies;

  @Inject
  public ManagedDependencyBlueprintPlan(TopologyRequestDAO topologyRequests, ServiceDependencyDAO dependencies) {
    this.topologyRequests = topologyRequests;
    this.dependencies = dependencies;
  }

  public static Set<ManagedDependencyType> requirements(Blueprint blueprint) {
    Set<ManagedDependencyType> types = EnumSet.noneOf(ManagedDependencyType.class);
    if (blueprint.getSetting() == null) return types;
    for (var declaration : blueprint.getSetting().getSettingValue(SETTING)) {
      if (!declaration.keySet().equals(Set.of("consumer_service", "dependency_type"))
          || !"HBASE".equals(declaration.get("consumer_service"))
          || !blueprint.getServices().contains("HBASE")) {
        throw new IllegalArgumentException("Blueprint managed dependencies require an HBASE consumer and a dependency_type");
      }
      if (!types.add(ManagedDependencyType.valueOf(declaration.get("dependency_type")))) {
        throw new IllegalArgumentException("Blueprint managed dependency types must be unique");
      }
    }
    return Set.copyOf(types);
  }

  /** Explicit local daemons retain normal cardinality; client components are always retained. */
  public static boolean externalOnlyComponent(Blueprint blueprint, String component) {
    var info = blueprint.getStack().getComponentInfo(component);
    if (info == null || info.isClient()) return false;
    for (ManagedDependencyType type : requirements(blueprint)) {
      if (!blueprint.getStack().getComponents(type.name()).contains(component)) continue;
      boolean localDaemon = blueprint.getHostGroups().values().stream()
          .flatMap(group -> group.getComponentNames().stream())
          .anyMatch(name -> blueprint.getStack().getComponents(type.name()).contains(name)
              && !blueprint.getStack().getComponentInfo(name).isClient());
      return !localDaemon;
    }
    return false;
  }

  public static String serialize(Blueprint blueprint) {
    return requirements(blueprint).stream().map(Enum::name).sorted().collect(Collectors.joining(","));
  }

  /** Checks the frozen provisioning intent, so deleting or replacing a template cannot bypass it. */
  public void requireBindings(long clusterId) throws AmbariException {
    var request = topologyRequests.findProvisionByClusterId(clusterId);
    if (request == null || request.getManagedDependencyTypes() == null
        || request.getManagedDependencyTypes().isEmpty()) return;
    Set<String> bound = dependencies.findByConsumer(clusterId, "HBASE").stream()
        .map(binding -> binding.getDependencyType()).collect(Collectors.toSet());
    for (String type : request.getManagedDependencyTypes().split(",")) {
      ManagedDependencyType.valueOf(type);
      if (!bound.contains(type)) {
        throw new AmbariException("Approve the Blueprint's " + type + " binding before installing or starting HBASE");
      }
    }
  }
}
