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
package org.apache.ambari.server.controller.dependencies.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.junit.jupiter.api.Test;

class ManagedHBaseKerberosOverlaySpecTest {
  private static final String REALM = "EXAMPLE.COM";
  private static final String USER = "hbase_mc_0123456789abcdefabcd";
  private static final String PLAN_HASH = hash('a');
  private static final UUID HDFS_ID =
      UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID ZK_ID =
      UUID.fromString("22222222-2222-4222-8222-222222222222");

  @Test
  void createsPlanOverlayWithoutClusterBindingOrApprovalState() {
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> selections =
        new TreeMap<>();
    selections.put(ManagedDependencyType.HDFS,
        reference(ManagedDependencyType.HDFS, HDFS_ID));
    selections.put(ManagedDependencyType.ZOOKEEPER,
        reference(ManagedDependencyType.ZOOKEEPER, ZK_ID));

    ManagedHBaseKerberosOverlaySpec spec = ManagedHBaseKerberosOverlaySpec.create(
        REALM, USER, "ambari-qa-draft@" + REALM, "ambari-qa", PLAN_HASH,
        selections);
    selections.clear();

    assertTrue(spec.hasManagedHdfs());
    assertTrue(spec.hasManagedZooKeeper());
    assertEquals(2, spec.bindings().size());
    assertEquals(USER + "/_HOST@" + REALM, spec.rolePrincipalPattern());
    assertEquals(USER + "@" + REALM, spec.headlessPrincipal());
    assertEquals("/ambari-managed-hbase/" + ZK_ID + "/hbase", spec.znodeParent());
    assertFalse(spec.toString().contains("clusterId"));
    assertFalse(spec.toString().contains("approval"));
  }

  @Test
  void rejectsSharedOrUnapprovedIdentityShapes() {
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> selections =
        new TreeMap<>();
    selections.put(ManagedDependencyType.HDFS,
        reference(ManagedDependencyType.HDFS, HDFS_ID));

    assertThrows(IllegalArgumentException.class,
        () -> ManagedHBaseKerberosOverlaySpec.create(REALM, "hbase",
            "ambari-qa-draft@" + REALM, "ambari-qa", PLAN_HASH, selections));
    assertThrows(IllegalArgumentException.class,
        () -> ManagedHBaseKerberosOverlaySpec.create(REALM, USER,
            USER + "@" + REALM, USER, PLAN_HASH, selections));
    assertThrows(IllegalArgumentException.class,
        () -> new ManagedHBaseKerberosOverlaySpec(
            ManagedHBaseKerberosOverlaySpec.SCHEMA_VERSION, REALM, USER,
            USER + "/_HOST@" + REALM, USER + "@" + REALM,
            "ambari-qa-draft@" + REALM, "ambari-qa", PLAN_HASH, selections,
            "/hbase-secure", USER));
  }

  @Test
  void rejectsBindingMapKeyMismatchAndNonCanonicalRealm() {
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> mismatch =
        new TreeMap<>();
    mismatch.put(ManagedDependencyType.HDFS,
        reference(ManagedDependencyType.ZOOKEEPER, ZK_ID));

    assertThrows(IllegalArgumentException.class,
        () -> ManagedHBaseKerberosOverlaySpec.create(REALM, USER,
            "ambari-qa-draft@" + REALM, "ambari-qa", PLAN_HASH, mismatch));
    assertThrows(IllegalArgumentException.class,
        () -> ManagedHBaseKerberosOverlaySpec.create("example.com", USER,
            "ambari-qa-draft@example.com", "ambari-qa", PLAN_HASH,
            oneHdfsSelection()));
  }

  private SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> oneHdfsSelection() {
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> selections =
        new TreeMap<>();
    selections.put(ManagedDependencyType.HDFS,
        reference(ManagedDependencyType.HDFS, HDFS_ID));
    return selections;
  }

  private ManagedBindingSnapshotRef reference(ManagedDependencyType type, UUID bindingId) {
    return new ManagedBindingSnapshotRef(type, bindingId);
  }

  private static String hash(char value) {
    return "sha256:" + String.valueOf(value).repeat(64);
  }
}
