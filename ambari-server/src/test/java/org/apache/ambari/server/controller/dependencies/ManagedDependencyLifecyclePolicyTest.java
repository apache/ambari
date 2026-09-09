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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.orm.dao.ServiceDependencyDAO;
import org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity;
import org.apache.ambari.server.state.Cluster;
import org.junit.jupiter.api.Test;

class ManagedDependencyLifecyclePolicyTest {

  @Test
  void providerDeletionRequiresEveryDependentToDetach() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Cluster cluster = cluster(11L);
    when(dao.findByProvider(11L, "HDFS")).thenReturn(List.of(binding()));

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> new ManagedDependencyLifecyclePolicy(dao)
            .validateServiceDeletion(cluster, Set.of("HDFS")));

    assertEquals(409, error.getStatus());
    assertEquals("DEPENDENCY_PROVIDER_DELETE_BLOCKED", error.getCode());
  }

  @Test
  void consumerDeletionRequiresDetachButHistoricalFenceDoesNotBlock() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Cluster cluster = cluster(11L);
    when(dao.findByProvider(11L, "HBASE")).thenReturn(List.of());
    when(dao.findByConsumer(11L, "HBASE")).thenReturn(List.of(binding()));
    ManagedDependencyLifecyclePolicy policy = new ManagedDependencyLifecyclePolicy(dao);

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> policy.validateServiceDeletion(cluster, Set.of("HBASE")));
    assertEquals("DEPENDENCY_CONSUMER_DELETE_REQUIRES_DETACH", error.getCode());

    when(dao.findByConsumer(11L, "HBASE")).thenReturn(List.of());
    assertDoesNotThrow(() -> policy.validateServiceDeletion(cluster, Set.of("HBASE")));
  }

  @Test
  void bulkDeletionPrevalidatesAllReferences() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Cluster cluster = cluster(11L);
    when(dao.findByProvider(11L, "HBASE")).thenReturn(List.of());
    when(dao.findByProvider(11L, "HDFS")).thenReturn(List.of(binding()));
    when(dao.findByConsumer(11L, "HBASE")).thenReturn(List.of());

    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> new ManagedDependencyLifecyclePolicy(dao)
            .validateServiceDeletion(cluster, Set.of("HBASE", "HDFS")));

    assertEquals("DEPENDENCY_PROVIDER_DELETE_BLOCKED", error.getCode());
  }

  @Test
  void providerImpactAcceptsStrictCompleteConfirmationSetForTwoProviders() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Cluster cluster = cluster(11L);
    when(dao.findByProvider(11L, "HDFS")).thenReturn(List.of(binding("hdfs-binding")));
    when(dao.findByProvider(11L, "ZOOKEEPER")).thenReturn(List.of(binding("zk-binding")));
    ManagedDependencyLifecyclePolicy policy = new ManagedDependencyLifecyclePolicy(dao);
    String hdfsRevision = policy.impactRevision(11L, "HDFS");
    String zkRevision = policy.impactRevision(11L, "ZOOKEEPER");
    String confirmations = confirmations(hdfsRevision, zkRevision);

    assertTrue(policy.validateProviderAction(cluster, "HDFS", "STOP",
        Map.of(ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATIONS_PARAMETER, confirmations)));
    assertTrue(policy.validateProviderAction(cluster, "ZOOKEEPER", "RESTART",
        Map.of(ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATIONS_PARAMETER, confirmations)));

    Map<ManagedDependencyLifecyclePolicy.ImpactTarget, Map<String, String>> targets =
        new LinkedHashMap<>();
    targets.put(new ManagedDependencyLifecyclePolicy.ImpactTarget(11L, "HDFS", "STOP"),
        targetParameters("STOP", hdfsRevision));
    targets.put(new ManagedDependencyLifecyclePolicy.ImpactTarget(11L, "ZOOKEEPER", "RESTART"),
        targetParameters("RESTART", zkRevision));
    assertDoesNotThrow(() -> policy.validateProviderTargetSet(targets));
  }

  @Test
  void unboundProviderImpactDoesNotRequireConfirmation() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Cluster cluster = cluster(11L);
    when(dao.findByProvider(11L, "HDFS")).thenReturn(List.of());
    when(dao.findByProvider(11L, "ZOOKEEPER")).thenReturn(List.of());
    ManagedDependencyLifecyclePolicy policy = new ManagedDependencyLifecyclePolicy(dao);

    assertFalse(policy.validateProviderAction(cluster, "HDFS", "STOP", Map.of()));
    assertFalse(policy.validateProviderAction(cluster, "ZOOKEEPER", "RESTART", Map.of()));
  }

  @Test
  void partialSetAndStaleOrMismatchedConfirmationAreRejected() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Cluster cluster = cluster(11L);
    when(dao.findByProvider(11L, "HDFS")).thenReturn(List.of(binding("hdfs-binding")));
    when(dao.findByProvider(11L, "ZOOKEEPER")).thenReturn(List.of(binding("zk-binding")));
    ManagedDependencyLifecyclePolicy policy = new ManagedDependencyLifecyclePolicy(dao);
    String hdfsRevision = policy.impactRevision(11L, "HDFS");
    String zkRevision = policy.impactRevision(11L, "ZOOKEEPER");

    ManagedDependencyIntegrationException missing = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> policy.validateProviderAction(cluster, "ZOOKEEPER", "RESTART",
            Map.of(ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATIONS_PARAMETER,
                confirmations(hdfsRevision, null))));
    assertEquals("DEPENDENCY_IMPACT_CONFIRMATION_REQUIRED", missing.getCode());

    Map<ManagedDependencyLifecyclePolicy.ImpactTarget, Map<String, String>> partialTargets =
        new LinkedHashMap<>();
    partialTargets.put(new ManagedDependencyLifecyclePolicy.ImpactTarget(11L, "HDFS", "STOP"),
        targetParameters("STOP", hdfsRevision));
    partialTargets.put(new ManagedDependencyLifecyclePolicy.ImpactTarget(11L, "ZOOKEEPER", "RESTART"),
        Map.of(ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATION_MODE_PARAMETER, "set"));
    ManagedDependencyIntegrationException partial = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> policy.validateProviderTargetSet(partialTargets));
    assertEquals("DEPENDENCY_IMPACT_CONFIRMATION_SET_REQUIRED", partial.getCode());

    ManagedDependencyIntegrationException stale = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> policy.validateProviderAction(cluster, "HDFS", "STOP",
            Map.of(ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATIONS_PARAMETER,
                confirmations("sha256:" + "b".repeat(64), zkRevision))));
    assertEquals("DEPENDENCY_IMPACT_PREVIEW_STALE", stale.getCode());

    ManagedDependencyIntegrationException mismatch = assertThrows(
        ManagedDependencyIntegrationException.class,
        () -> policy.validateProviderAction(cluster, "HDFS", "STOP",
            Map.of(ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATIONS_PARAMETER,
                confirmationsForAction("RESTART", hdfsRevision, "RESTART", zkRevision))));
    assertEquals("DEPENDENCY_IMPACT_ACTION_MISMATCH", mismatch.getCode());
  }

  @Test
  void malformedConfirmationRecordsAreStableBadRequests() {
    ServiceDependencyDAO dao = mock(ServiceDependencyDAO.class);
    Cluster cluster = cluster(11L);
    when(dao.findByProvider(11L, "HDFS")).thenReturn(List.of(binding("hdfs-binding")));
    ManagedDependencyLifecyclePolicy policy = new ManagedDependencyLifecyclePolicy(dao);
    List<String> malformed = List.of(
        "[{\"provider_cluster_id\":11,\"service_name\":\"HDFS\",\"action\":\"STOP\","
            + "\"revision\":\"sha256:" + "a".repeat(64) + "\",\"extra\":1}]",
        "[{\"provider_cluster_id\":11,\"provider_cluster_id\":11,"
            + "\"service_name\":\"HDFS\",\"action\":\"STOP\",\"revision\":\"sha256:"
            + "a".repeat(64) + "\"}]",
        "[{\"provider_cluster_id\":11.5,\"service_name\":\"HDFS\",\"action\":\"STOP\","
            + "\"revision\":\"sha256:" + "a".repeat(64) + "\"}]",
        "x".repeat(ManagedDependencyLifecyclePolicyTest.MAX_CONFIRMATION_BYTES + 1));

    for (String raw : malformed) {
      ManagedDependencyIntegrationException error = assertThrows(
          ManagedDependencyIntegrationException.class,
          () -> policy.validateProviderAction(cluster, "HDFS", "STOP",
              Map.of(ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATIONS_PARAMETER, raw)));
      assertEquals(400, error.getStatus());
      assertEquals("INVALID_DEPENDENCY_IMPACT_CONFIRMATIONS", error.getCode());
    }
  }

  private static final int MAX_CONFIRMATION_BYTES = 16 * 1024;

  private Cluster cluster(long id) {
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(id);
    return cluster;
  }

  private ServiceDependencyBindingEntity binding() {
    return new ServiceDependencyBindingEntity();
  }

  private ServiceDependencyBindingEntity binding(String bindingId) {
    ServiceDependencyBindingEntity binding = binding();
    binding.setBindingId(bindingId);
    binding.setRowVersion(1L);
    binding.setState("READY");
    return binding;
  }

  private String confirmations(String hdfsRevision, String zkRevision) {
    return confirmationsForAction("STOP", hdfsRevision, "RESTART", zkRevision);
  }

  private String confirmationsForAction(String hdfsAction, String hdfsRevision,
      String zkAction, String zkRevision) {
    String hdfs = "{\"provider_cluster_id\":11,\"service_name\":\"HDFS\",\"action\":\""
        + hdfsAction + "\",\"revision\":\"" + hdfsRevision + "\"}";
    if (zkRevision == null) {
      return "[" + hdfs + "]";
    }
    return "[" + hdfs + ",{" + "\"provider_cluster_id\":11,\"service_name\":\"ZOOKEEPER\","
        + "\"action\":\"" + zkAction + "\",\"revision\":\"" + zkRevision + "\"}]";
  }

  private Map<String, String> targetParameters(String action, String revision) {
    return Map.of(
        ManagedDependencyLifecyclePolicy.IMPACT_CONFIRMATION_MODE_PARAMETER, "set",
        ManagedDependencyLifecyclePolicy.IMPACT_ACTION_PARAMETER, action,
        ManagedDependencyLifecyclePolicy.IMPACT_REVISION_PARAMETER, revision);
  }
}
