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
package org.apache.ambari.server.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.ConsumerReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServiceDependenciesApiServiceTest {
  private ManagedServiceDependencyCoordinator coordinator;
  private ServiceDependenciesApiService service;

  @BeforeEach
  void setUp() throws Exception {
    coordinator = mock(ManagedServiceDependencyCoordinator.class);
    Field field = ServiceDependenciesApiService.class.getDeclaredField("coordinator");
    field.setAccessible(true);
    field.set(null, coordinator);
    service = new ServiceDependenciesApiService();
  }

  @AfterEach
  void tearDown() throws Exception {
    Field field = ServiceDependenciesApiService.class.getDeclaredField("coordinator");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  void servicePlanCandidatesUseNumericClusterAndExactWorkflowRevision() {
    when(coordinator.candidates(org.mockito.ArgumentMatchers.any(),
        eq(ManagedDependencyType.HDFS))).thenReturn(List.of());

    Response response = service.candidates(
        "service_plan", null, null, "27", "6", "HDFS");

    assertEquals(200, response.getStatus());
    ArgumentCaptor<ConsumerReference> consumer = ArgumentCaptor.forClass(ConsumerReference.class);
    verify(coordinator).candidates(consumer.capture(), eq(ManagedDependencyType.HDFS));
    assertEquals(27L, consumer.getValue().clusterId());
    assertEquals(6L, consumer.getValue().expectedDraftRevision());
    assertTrue(consumer.getValue().servicePlan());
  }

  @Test
  void previewRejectsMixedDraftAndServicePlanIdentity() {
    Response response = service.preview("""
        {"consumer":{"scope":"SERVICE_PLAN","cluster_id":27,
          "draft_id":"00000000-0000-4000-8000-000000000001","expected_revision":6},
         "dependency_type":"HDFS",
         "provider":{"cluster_id":42,"service_name":"HDFS"}}
        """);

    assertEquals(400, response.getStatus());
    verifyNoInteractions(coordinator);
  }

  @Test
  void previewPassesCanonicalBindingIdentityToDraftResolver() {
    UUID bindingId = UUID.fromString("00000000-0000-4000-8000-000000000041");
    when(coordinator.preview(org.mockito.ArgumentMatchers.any(),
        eq(ManagedDependencyType.HDFS), org.mockito.ArgumentMatchers.any(), eq(bindingId)))
        .thenReturn(Map.of("binding_id", bindingId.toString()));

    Response response = service.preview("""
        {"binding_id":"00000000-0000-4000-8000-000000000041",
         "consumer":{"scope":"DRAFT","draft_id":"00000000-0000-4000-8000-000000000042",
          "expected_revision":6},
         "dependency_type":"HDFS",
         "provider":{"cluster_id":42,"service_name":"HDFS"}}
        """);

    assertEquals(200, response.getStatus());
    verify(coordinator).preview(org.mockito.ArgumentMatchers.any(),
        eq(ManagedDependencyType.HDFS), org.mockito.ArgumentMatchers.any(), eq(bindingId));
  }

  @Test
  void previewRejectsMalformedOptionalBindingIdentityBeforeCoordinator() {
    Response response = service.preview("""
        {"binding_id":"not-a-uuid",
         "consumer":{"scope":"DRAFT","draft_id":"00000000-0000-4000-8000-000000000042",
          "expected_revision":6},
         "dependency_type":"HDFS",
         "provider":{"cluster_id":42,"service_name":"HDFS"}}
        """);

    assertEquals(400, response.getStatus());
    verifyNoInteractions(coordinator);
  }

  @Test
  void completePreviewForwardsBothSelectionsInOneCoordinatorCall() {
    when(coordinator.preview(any(ConsumerReference.class), any(List.class)))
        .thenReturn(Map.of("items", List.of()));

    Response response = service.preview("""
        {"consumer":{"scope":"SERVICE_PLAN","cluster_id":27,"expected_revision":6},
         "selections":[
           {"dependency_type":"HDFS",
            "provider":{"cluster_id":41,"service_name":"HDFS"}},
           {"dependency_type":"ZOOKEEPER",
            "provider":{"cluster_id":42,"service_name":"ZOOKEEPER"}}
         ]}
        """);

    assertEquals(200, response.getStatus());
    verify(coordinator).preview(eq(ConsumerReference.servicePlan(27, 6)), any(List.class));
  }
}
