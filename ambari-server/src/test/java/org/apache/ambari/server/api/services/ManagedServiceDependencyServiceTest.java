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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManagedServiceDependencyServiceTest {
  private static final UUID BINDING_ID = UUID.fromString(
      "00000000-0000-4000-8000-000000000031");
  private static final UUID OPERATION_ID = UUID.fromString(
      "00000000-0000-4000-8000-000000000032");

  private ManagedServiceDependencyCoordinator coordinator;
  private ManagedServiceDependencyService service;

  @BeforeEach
  void setUp() throws Exception {
    coordinator = mock(ManagedServiceDependencyCoordinator.class);
    Field field = ManagedServiceDependencyService.class.getDeclaredField("coordinator");
    field.setAccessible(true);
    field.set(null, coordinator);
    service = new ManagedServiceDependencyService("consumer", "HBASE");
  }

  @AfterEach
  void tearDown() throws Exception {
    Field field = ManagedServiceDependencyService.class.getDeclaredField("coordinator");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  void retryForwardsImmutableOperationAndInitialRowVersion() {
    when(coordinator.retry(eq("consumer"), eq("HBASE"), eq(BINDING_ID),
        argThat(request -> OPERATION_ID.equals(request.operationId())
            && request.expectedRowVersion() == 0L)))
        .thenReturn(Map.of("binding_id", BINDING_ID.toString()));

    Response response = service.retry(BINDING_ID.toString(), body());

    assertEquals(202, response.getStatus());
    verify(coordinator).retry(eq("consumer"), eq("HBASE"), eq(BINDING_ID),
        argThat(request -> OPERATION_ID.equals(request.operationId())
            && request.expectedRowVersion() == 0L));
  }

  @Test
  void detachUsesTheSameStrictLifecycleEnvelope() {
    when(coordinator.detach(eq("consumer"), eq("HBASE"), eq(BINDING_ID),
        argThat(request -> OPERATION_ID.equals(request.operationId())
            && request.expectedRowVersion() == 0L)))
        .thenReturn(Map.of("binding_id", BINDING_ID.toString(), "state", "DETACHING"));

    Response response = service.detach(BINDING_ID.toString(), body());

    assertEquals(202, response.getStatus());
    verify(coordinator).detach(eq("consumer"), eq("HBASE"), eq(BINDING_ID),
        argThat(request -> OPERATION_ID.equals(request.operationId())
            && request.expectedRowVersion() == 0L));
  }

  @Test
  void legacyCreateKeepsBindingResponseWhileCollectionUsesItemsEnvelope() {
    when(coordinator.create(eq("consumer"), eq("HBASE"), anyList()))
        .thenReturn(List.of(Map.of("binding_id", BINDING_ID.toString())));

    Response legacy = service.create(createItem());
    assertEquals(202, legacy.getStatus());
    assertEquals(Map.of("binding_id", BINDING_ID.toString()), legacy.getEntity());

    Response collection = service.create("{\"items\":[" + createItem() + "]}");
    assertEquals(202, collection.getStatus());
    assertEquals(Map.of("items", List.of(Map.of("binding_id", BINDING_ID.toString()))),
        collection.getEntity());
  }

  private String createItem() {
    return """
        {"binding_id":"00000000-0000-4000-8000-000000000031",
         "dependency_type":"HDFS",
         "provider":{"cluster_id":22,"service_name":"HDFS"},
         "expected_provider_fingerprint":"sha256:%s",
         "expected_consumer_descriptor_fingerprint":"sha256:%s",
         "expected_snapshot_fingerprint":"sha256:%s",
         "preview_schema_version":2,
         "operation_id":"00000000-0000-4000-8000-000000000032"}
        """.formatted("a".repeat(64), "b".repeat(64), "c".repeat(64));
  }

  private String body() {
    return """
        {"operation_id":"00000000-0000-4000-8000-000000000032",
         "expected_row_version":0}
        """;
  }
}
