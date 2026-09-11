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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.orm.entities.ServiceDependencySnapshotEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.jupiter.api.Test;

class ManagedDependencyConfigPolicyTest {
  private static final UUID BINDING_ID =
      UUID.fromString("26588fef-d534-49fa-9e10-59c45cc2e59c");
  private static final String HASH = "sha256:" + "c".repeat(64);

  @Test
  void activeBindingSealsOnlyHbaseOwnedDesiredConfiguration() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ManagedDependencyConfigPolicy policy = new ManagedDependencyConfigPolicy(dao);
    Cluster cluster = mock(Cluster.class);
    DesiredConfig desired = new DesiredConfig();
    desired.setTag("current");
    when(cluster.getClusterId()).thenReturn(11L);
    when(cluster.getDesiredConfigs()).thenReturn(Map.of("hbase-site", desired));
    when(cluster.getConfigGroups()).thenReturn(Map.of());
    when(dao.findByConsumer(11L, "HBASE")).thenReturn(List.of(binding()));
    when(dao.findSnapshot(BINDING_ID.toString(), 1L)).thenReturn(snapshotEntity());

    Config exact = config("hbase-site", "next", Map.of(
        "hbase.rootdir", root(), "hbase.wal.dir", wal(), "unrelated", "tunable"));
    assertDoesNotThrow(() -> policy.validateDesiredConfigs(cluster, List.of(exact)));
    assertDoesNotThrow(() -> policy.validateDesiredConfigs(cluster,
        List.of(config("core-site", "next", Map.of("fs.defaultFS", "hdfs://local")))));

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> policy.validateDesiredConfigs(cluster, List.of(config("hbase-site", "next",
            Map.of("hbase.rootdir", "hdfs://other/root", "hbase.wal.dir", wal())))));
    assertEquals("DEPENDENCY_MANAGED_CONFIG_CONFLICT", error.getCode());
  }

  @Test
  void directDesiredMutationCannotDeleteSealedValue() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    ManagedDependencyConfigPolicy policy = new ManagedDependencyConfigPolicy(dao);
    Cluster cluster = mock(Cluster.class);
    DesiredConfig desired = new DesiredConfig();
    desired.setTag("current");
    when(cluster.getClusterId()).thenReturn(11L);
    when(cluster.getDesiredConfigs()).thenReturn(Map.of("hbase-site", desired));
    when(cluster.getConfigGroups()).thenReturn(Map.of());
    when(dao.findByConsumer(11L, "HBASE")).thenReturn(List.of(binding()));
    when(dao.findSnapshot(BINDING_ID.toString(), 1L)).thenReturn(snapshotEntity());

    assertThrows(ManagedDependencyIntegrationException.class,
        () -> policy.validateConfigMutation(cluster, "hbase-site", "current",
            Map.of("hbase.wal.dir", wal())));
  }

  private Config config(String type, String tag, Map<String, String> properties) {
    Config config = mock(Config.class);
    when(config.getType()).thenReturn(type);
    when(config.getTag()).thenReturn(tag);
    when(config.getProperties()).thenReturn(properties);
    return config;
  }

  private ServiceDependencyBindingEntity binding() {
    ServiceDependencyBindingEntity binding = new ServiceDependencyBindingEntity();
    binding.setBindingId(BINDING_ID.toString());
    binding.setState("READY");
    binding.setDesiredSnapshotVersion(1L);
    return binding;
  }

  private ServiceDependencySnapshotEntity snapshotEntity() {
    ServiceDependencySnapshotEntity entity = new ServiceDependencySnapshotEntity();
    entity.setBindingId(BINDING_ID.toString());
    entity.setSnapshotVersion(1L);
    entity.setSnapshotJson(StageUtils.getGson().toJson(snapshot()));
    return entity;
  }

  private ManagedDependencySnapshot snapshot() {
    ManagedDependencyVersion version = new ManagedDependencyVersion(
        "BIGTOP", "3.3.0", true, "3.3.0", new TreeMap<>(),
        new TreeSet<>(Set.of("STANDARD_RPC_CLIENT")), 31L, List.of());
    ManagedDependencyIdentity identity = new ManagedDependencyIdentity(
        "hbase_mc_cb", new TreeSet<>(), false, "hbase_mc_cb", true, "0700", false);
    return new ManagedDependencySnapshot(ManagedDependencySnapshot.CURRENT_SCHEMA_VERSION,
        BINDING_ID, 1L, ManagedDependencyType.HDFS,
        new ManagedDependencyServiceKey(22L, "HDFS"),
        ManagedDependencyNamespace.hdfs(BINDING_ID, "hdfs://provider"),
        version.compatibility(), ManagedDependencySecurityMode.INSECURE, identity,
        new TreeMap<>(Map.of("fs.defaultFS", "hdfs://provider")), new TreeMap<>(),
        new TreeMap<>(), HASH, HASH, HASH);
  }

  private String root() {
    return "hdfs://provider/apps/ambari-managed/hbase/" + BINDING_ID + "/root";
  }

  private String wal() {
    return "hdfs://provider/apps/ambari-managed/hbase/" + BINDING_ID + "/wal";
  }
}
