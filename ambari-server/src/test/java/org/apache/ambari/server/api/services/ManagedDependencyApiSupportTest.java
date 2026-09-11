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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator;
import org.apache.ambari.server.controller.dependencies.ManagedServiceDependencyCoordinator.PreviewSelection;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ManagedDependencyApiSupportTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void numericJsonFieldsRequireIntegralPositiveValues() throws Exception {
    assertEquals(17L, ManagedDependencyApiSupport.positiveLong(
        MAPPER.readTree("{\"cluster_id\":17}"), "cluster_id"));
    assertThrows(IllegalArgumentException.class, () -> ManagedDependencyApiSupport.positiveLong(
        MAPPER.readTree("{\"cluster_id\":17.25}"), "cluster_id"));
    assertThrows(IllegalArgumentException.class, () -> ManagedDependencyApiSupport.positiveInt(
        MAPPER.readTree("{\"preview_schema_version\":1.5}"), "preview_schema_version"));
  }

  @Test
  void lifecycleRequestAcceptsInitialRowVersionAndRejectsUnknownMutationFields() {
    ManagedServiceDependencyCoordinator.LifecycleRequest request =
        ManagedDependencyApiSupport.lifecycleRequest("""
            {"operation_id":"00000000-0000-4000-8000-000000000001",
             "expected_row_version":0}
            """);

    assertEquals(0L, request.expectedRowVersion());
    assertThrows(IllegalArgumentException.class,
        () -> ManagedDependencyApiSupport.lifecycleRequest("""
            {"operation_id":"00000000-0000-4000-8000-000000000001",
             "expected_row_version":0,"force":true}
            """));
  }

  @Test
  void createRequestRequiresTheFullSchemaTwoSnapshotIdentity() {
    String full = """
        {"binding_id":"00000000-0000-4000-8000-000000000011",
         "dependency_type":"HDFS",
         "provider":{"cluster_id":22,"service_name":"HDFS"},
         "expected_provider_fingerprint":"sha256:%s",
         "expected_consumer_descriptor_fingerprint":"sha256:%s",
         "expected_snapshot_fingerprint":"sha256:%s",
         "preview_schema_version":2,
         "operation_id":"00000000-0000-4000-8000-000000000012"}
        """.formatted("a".repeat(64), "b".repeat(64), "c".repeat(64));

    assertEquals("sha256:" + "c".repeat(64),
        ManagedDependencyApiSupport.createRequest(full).expectedSnapshotFingerprint());
    assertThrows(IllegalArgumentException.class, () -> ManagedDependencyApiSupport.createRequest(
        full.replaceFirst(
            "\\s*\\\"expected_snapshot_fingerprint\\\":\\\"sha256:[c]{64}\\\",", "")));
  }

  @Test
  void collectionCreateParsesTwoImmutableItemsAndRejectsMixedEnvelope() {
    String item = """
        {"binding_id":"00000000-0000-4000-8000-000000000011",
         "dependency_type":"HDFS",
         "provider":{"cluster_id":22,"service_name":"HDFS"},
         "expected_provider_fingerprint":"sha256:%s",
         "expected_consumer_descriptor_fingerprint":"sha256:%s",
         "expected_snapshot_fingerprint":"sha256:%s",
         "preview_schema_version":2,
         "operation_id":"00000000-0000-4000-8000-000000000012"}
        """.formatted("a".repeat(64), "b".repeat(64), "c".repeat(64));
    String peer = item.replace("000000000011", "000000000021")
        .replace("\"HDFS\"", "\"ZOOKEEPER\"")
        .replace("000000000012", "000000000022")
        .replace("\"service_name\":\"HDFS\"", "\"service_name\":\"ZOOKEEPER\"");

    List<ManagedServiceDependencyCoordinator.CreateRequest> requests =
        ManagedDependencyApiSupport.createRequests("{\"items\":[" + item + "," + peer + "]}");
    assertEquals(List.of("HDFS", "ZOOKEEPER"),
        requests.stream().map(request -> request.type().name()).toList());
    assertThrows(IllegalArgumentException.class, () ->
        ManagedDependencyApiSupport.createRequests(
            "{\"items\":[" + item + "],\"dependency_type\":\"HDFS\"}"));
  }

  @Test
  void completePreviewSelectionRetainsOptionalCanonicalBindingIdentity() throws Exception {
    PreviewSelection selection = ManagedDependencyApiSupport.previewSelection(
        MAPPER.readTree("""
            {"binding_id":"00000000-0000-4000-8000-000000000031",
             "dependency_type":"ZOOKEEPER",
             "provider":{"cluster_id":22,"service_name":"ZOOKEEPER"}}
            """));
    assertEquals("ZOOKEEPER", selection.type().name());
    assertEquals("00000000-0000-4000-8000-000000000031",
        selection.bindingId().toString());
  }
}
