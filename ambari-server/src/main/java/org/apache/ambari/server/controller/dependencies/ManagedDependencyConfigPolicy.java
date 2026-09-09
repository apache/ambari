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

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.utils.StageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Protects the HBase-owned values sealed by an active managed dependency. */
@Singleton
public class ManagedDependencyConfigPolicy {
  private final ServiceDependencyDAO dependencyDAO;

  @Inject
  public ManagedDependencyConfigPolicy(ServiceDependencyDAO dependencyDAO) {
    this.dependencyDAO = dependencyDAO;
  }

  public void validateDesiredConfigs(Cluster cluster, Iterable<Config> configs) {
    Map<String, Map<String, String>> controlled = controlledProperties(cluster);
    for (Config config : configs) {
      if (config != null && controlled.containsKey(config.getType())) {
        requireExact(config.getType(), config.getProperties(), controlled.get(config.getType()));
      }
    }
  }

  public void validateConfigMutation(Cluster cluster, String type, String tag,
      Map<String, String> proposed) {
    Map<String, String> controlled = controlledProperties(cluster).get(type);
    if (controlled == null) {
      return;
    }
    DesiredConfig desired = cluster.getDesiredConfigs().get(type);
    if (desired != null && Objects.equals(desired.getTag(), tag)) {
      requireExact(type, proposed, controlled);
    }
    for (ConfigGroup group : cluster.getConfigGroups().values()) {
      Config config = group.getConfigurations().get(type);
      if ("HBASE".equals(group.getServiceName()) && config != null
          && Objects.equals(config.getTag(), tag)) {
        requireCompatibleOverride(type, proposed, controlled);
      }
    }
  }

  public void validateConfigGroup(Cluster cluster, String serviceName,
      Map<String, Config> configurations) {
    if (!"HBASE".equals(serviceName) || configurations == null) {
      return;
    }
    Map<String, Map<String, String>> controlled = controlledProperties(cluster);
    for (Map.Entry<String, Config> entry : configurations.entrySet()) {
      Map<String, String> expected = controlled.get(entry.getKey());
      if (expected != null && entry.getValue() != null) {
        requireCompatibleOverride(entry.getKey(), entry.getValue().getProperties(), expected);
      }
    }
  }

  private Map<String, Map<String, String>> controlledProperties(Cluster cluster) {
    Map<String, Map<String, String>> result = new TreeMap<>();
    for (ServiceDependencyBindingEntity binding
        : dependencyDAO.findByConsumer(cluster.getClusterId(), "HBASE")) {
      if ("RETIRED".equals(binding.getState())) {
        continue;
      }
      ServiceDependencySnapshotEntity entity = dependencyDAO.findSnapshot(
          binding.getBindingId(), binding.getDesiredSnapshotVersion());
      if (entity == null) {
        throw conflict("An active managed dependency snapshot is missing; configuration is blocked.");
      }
      ManagedDependencySnapshot snapshot;
      try {
        snapshot = StageUtils.getGson().fromJson(
            entity.getSnapshotJson(), ManagedDependencySnapshot.class);
      } catch (RuntimeException e) {
        throw new ManagedDependencyIntegrationException(409,
            "DEPENDENCY_MANAGED_CONFIG_CONFLICT",
            "An active managed dependency snapshot is invalid; configuration is blocked.", e);
      }
      Map<String, String> hbaseSite = result.computeIfAbsent("hbase-site",
          ignored -> new TreeMap<>());
      if (snapshot.type() == ManagedDependencyType.HDFS) {
        putConsistent(hbaseSite, "hbase.rootdir", snapshot.namespace().rootUri());
        putConsistent(hbaseSite, "hbase.wal.dir", snapshot.namespace().walUri());
      } else {
        snapshot.zooKeeperClient().forEach((key, value) ->
            putConsistent(hbaseSite, key, value));
      }
      putConsistent(result.computeIfAbsent("hbase-env", ignored -> new TreeMap<>()),
          "hbase_user", snapshot.consumerIdentity().effectiveShortUser());
    }
    return result;
  }

  private void putConsistent(Map<String, String> values, String key, String value) {
    String existing = values.putIfAbsent(key, value);
    if (existing != null && !existing.equals(value)) {
      throw conflict("Active managed dependency snapshots have conflicting HBase settings.");
    }
  }

  private void requireExact(String type, Map<String, String> actual,
      Map<String, String> expected) {
    for (Map.Entry<String, String> property : expected.entrySet()) {
      if (!Objects.equals(property.getValue(), actual.get(property.getKey()))) {
        throw conflict("Configuration " + type
            + " conflicts with an active managed dependency; update the binding first.");
      }
    }
  }

  private void requireCompatibleOverride(String type, Map<String, String> actual,
      Map<String, String> expected) {
    for (Map.Entry<String, String> property : expected.entrySet()) {
      if (actual.containsKey("DELETED_" + property.getKey())
          || actual.containsKey(property.getKey())
              && !Objects.equals(property.getValue(), actual.get(property.getKey()))) {
        throw conflict("HBase config group " + type
            + " cannot override an active managed dependency setting.");
      }
    }
  }

  private ManagedDependencyIntegrationException conflict(String message) {
    return new ManagedDependencyIntegrationException(409,
        "DEPENDENCY_MANAGED_CONFIG_CONFLICT", message);
  }
}
