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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.parsers.JsonRequestBodyParser;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyStackAdvisorPlanner.PlanRequest;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyStackAdvisorPlanner.TrustedPlan;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.AdvisorSelection;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ConsumerReference;
import org.junit.Before;
import org.junit.Test;

public class ManagedDependencyStackAdvisorPlannerTest {
  private static final UUID HDFS_BINDING = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
  private static final UUID ZK_BINDING = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
  private static final String PROVIDER_FP = "sha256:" + "1".repeat(64);
  private static final String CONSUMER_FP = "sha256:" + "2".repeat(64);
  private static final String SNAPSHOT_FP = "sha256:" + "3".repeat(64);

  private ManagedServiceDependencyCoordinator coordinator;
  private ManagedDependencyStackAdvisorPlanner planner;

  @Before
  public void setUp() {
    coordinator = mock(ManagedServiceDependencyCoordinator.class);
    planner = new ManagedDependencyStackAdvisorPlanner(coordinator);
  }

  @Test
  public void parsesActualFlattenedJsonShapeAndAuthorizesExactSelections() throws Exception {
    PlanRequest request = ManagedDependencyStackAdvisorPlanner.parse(parsedProperties(
        body("SERVICE_PLAN", "\"cluster_id\":27,\"expected_revision\":9")));
    when(coordinator.authorizeAdvisorSelections(eq(ConsumerReference.servicePlan(27, 9)),
        any())).thenReturn(java.util.List.of(
            selection(HDFS_BINDING, ManagedDependencyType.HDFS, 41, "HDFS"),
            selection(ZK_BINDING, ManagedDependencyType.ZOOKEEPER, 42, "ZOOKEEPER")));

    TrustedPlan trusted = planner.authorize(request, 27L, "BIGTOP", "3.2.0",
        Set.of("HBASE"));

    assertEquals(Set.of("HDFS_CLIENT", "ZOOKEEPER_SERVER"), trusted.satisfiedComponents());
    assertTrue(trusted.appliesTo(27L, "BIGTOP", "3.2.0", Set.of("HBASE")));
    assertFalse(trusted.appliesTo(28L, "BIGTOP", "3.2.0", Set.of("HBASE")));
  }

  @Test
  public void rejectsUnknownFieldsBeforeCoordinatorAccess() throws Exception {
    Map<String, Object> properties = parsedProperties(
        body("SERVICE", "\"cluster_id\":27").replace(
            "\"selections\":", "\"trusted\":true,\"selections\":"));

    assertCode("INVALID_MANAGED_DEPENDENCY_PLAN",
        () -> ManagedDependencyStackAdvisorPlanner.parse(properties));
    verifyNoInteractions(coordinator);
  }

  @Test
  public void rejectsStaleFingerprintAndSchemaWithoutCreatingTrustedFacts() throws Exception {
    PlanRequest request = ManagedDependencyStackAdvisorPlanner.parse(parsedProperties(
        body("SERVICE", "\"cluster_id\":27")));
    AdvisorSelection changed = new AdvisorSelection(HDFS_BINDING, ManagedDependencyType.HDFS,
        27L, "HBASE", 41, "HDFS", 2, "BIGTOP", "3.2.0",
        "sha256:" + "9".repeat(64), CONSUMER_FP, SNAPSHOT_FP);
    when(coordinator.authorizeAdvisorSelections(eq(ConsumerReference.service(27)), any()))
        .thenReturn(java.util.List.of(changed));

    assertCode("DEPENDENCY_PREVIEW_STALE",
        () -> planner.authorize(request, 27L, "BIGTOP", "3.2.0", Set.of("HBASE")));
  }

  @Test
  public void enforcesDraftAndExistingClusterTargetIdentity() throws Exception {
    UUID draftId = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    String draftBody = body("DRAFT",
        "\"draft_id\":\"" + draftId + "\",\"expected_revision\":4");
    PlanRequest draft = ManagedDependencyStackAdvisorPlanner.parse(parsedProperties(draftBody));

    assertCode("DEPENDENCY_CONSUMER_MISMATCH",
        () -> planner.authorize(draft, 27L, "BIGTOP", "3.2.0", Set.of("HBASE")));
    verifyNoInteractions(coordinator);
  }

  private AdvisorSelection selection(UUID bindingId, ManagedDependencyType type,
      long providerClusterId, String providerService) {
    return new AdvisorSelection(bindingId, type, 27L, "HBASE", providerClusterId,
        providerService, 2, "BIGTOP", "3.2.0", PROVIDER_FP, CONSUMER_FP, SNAPSHOT_FP);
  }

  private Map<String, Object> parsedProperties(String json) throws Exception {
    RequestBody body = new JsonRequestBodyParser().parse(json).iterator().next();
    Map<String, Object> result = new HashMap<>();
    for (NamedPropertySet propertySet : body.getNamedPropertySets()) {
      result.putAll(propertySet.getProperties());
    }
    return result;
  }

  private String body(String scope, String consumerFields) {
    return "{" +
        "\"managed_dependency_plan\":{" +
        "\"consumer\":{\"scope\":\"" + scope + "\"," + consumerFields + "}," +
        "\"selections\":[" + selectionJson(HDFS_BINDING, "HDFS", 41, "HDFS") + "," +
        selectionJson(ZK_BINDING, "ZOOKEEPER", 42, "ZOOKEEPER") + "]}}";
  }

  private String selectionJson(UUID bindingId, String type, long clusterId, String service) {
    return "{" +
        "\"dependency_type\":\"" + type + "\"," +
        "\"binding_id\":\"" + bindingId + "\"," +
        "\"provider\":{\"cluster_id\":" + clusterId + ",\"service_name\":\"" + service + "\"}," +
        "\"preview_schema_version\":2," +
        "\"expected_provider_fingerprint\":\"" + PROVIDER_FP + "\"," +
        "\"expected_consumer_descriptor_fingerprint\":\"" + CONSUMER_FP + "\"," +
        "\"expected_snapshot_fingerprint\":\"" + SNAPSHOT_FP + "\"}";
  }

  private void assertCode(String expected, ThrowingRunnable runnable) {
    try {
      runnable.run();
    } catch (ManagedDependencyIntegrationException e) {
      assertEquals(expected, e.getCode());
      return;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    throw new AssertionError("Expected managed dependency error " + expected);
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
