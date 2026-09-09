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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.ambari.server.controller.AuthToLocalBuilder;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyIntegrationException;
import org.apache.ambari.server.controller.dependencies.ManagedDependencyType;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosDescriptorOverlay.SealedConfigurations;
import org.apache.ambari.server.controller.dependencies.security.ManagedHBaseKerberosOverlaySpec.ManagedBindingSnapshotRef;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ManagedHBaseKerberosDescriptorOverlayTest {
  private static final String REALM = "EXAMPLE.COM";
  private static final String USER_A = "hbase_mc_0123456789abcdefabcd";
  private static final String USER_B = "hbase_mc_fedcba9876543210abcd";
  private static final UUID HDFS_ID =
      UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID ZK_ID =
      UUID.fromString("22222222-2222-4222-8222-222222222222");

  private final KerberosDescriptorFactory descriptorFactory = new KerberosDescriptorFactory();
  private final ManagedHBaseKerberosDescriptorOverlay overlay =
      new ManagedHBaseKerberosDescriptorOverlay();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void isolatesConcurrentConsumerOverlaysAndLeavesTheInputDescriptorUnmodified()
      throws Exception {
    KerberosDescriptor base = hbaseDescriptor();
    String before = objectMapper.writeValueAsString(base.toMap());
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      ListOfCalls calls = new ListOfCalls(
          () -> overlay.applyCopy(base, null, spec(USER_A, true, true)),
          () -> overlay.applyCopy(base, null, spec(USER_B, true, true)));
      java.util.List<Future<KerberosDescriptor>> results = executor.invokeAll(calls.values());
      KerberosDescriptor first = results.get(0).get();
      KerberosDescriptor second = results.get(1).get();

      assertEquals(USER_A + "/_HOST@" + REALM,
          daemon(first, "HBASE_MASTER", "hbase_master_hbase")
              .getPrincipalDescriptor().getValue());
      assertEquals(USER_B + "/_HOST@" + REALM,
          daemon(second, "HBASE_MASTER", "hbase_master_hbase")
              .getPrincipalDescriptor().getValue());
      assertNotEquals(first.toMap(), second.toMap());
      assertEquals(before, objectMapper.writeValueAsString(base.toMap()));
      assertEquals("hbase/_HOST@${realm}",
          daemon(base, "HBASE_MASTER", "hbase_master_hbase")
              .getPrincipalDescriptor().getValue());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void removesOnlyTheHdfsAdminReferenceForManagedHdfs() throws Exception {
    KerberosDescriptor base = hbaseDescriptor();

    KerberosDescriptor managedHdfs = overlay.applyCopy(base, null,
        spec(USER_A, true, false));
    KerberosDescriptor managedZooKeeperOnly = overlay.applyCopy(base, null,
        spec(USER_A, false, true));

    assertNull(managedHdfs.getService("HBASE").getComponent("HBASE_MASTER")
        .getIdentity("hbase_hbase_master_hdfs"));
    assertNotNull(managedZooKeeperOnly.getService("HBASE").getComponent("HBASE_MASTER")
        .getIdentity("hbase_hbase_master_hdfs"));
    assertNotNull(base.getService("HBASE").getComponent("HBASE_MASTER")
        .getIdentity("hbase_hbase_master_hdfs"));
  }

  @Test
  void appliesUniqueHeadlessDaemonCredentialAndNamespaceFacts() throws Exception {
    ManagedHBaseKerberosOverlaySpec spec = spec(USER_A, true, true);
    KerberosDescriptor result = overlay.applyCopy(hbaseDescriptor(), null, spec);
    KerberosServiceDescriptor hbase = result.getService("HBASE");

    assertIdentity(hbase.getIdentity("hbase"), USER_A + "@" + REALM, USER_A);
    assertIdentity(daemon(result, "HBASE_MASTER", "hbase_master_hbase"),
        spec.rolePrincipalPattern(), USER_A);
    assertIdentity(daemon(result, "HBASE_REGIONSERVER", "hbase_regionserver_hbase"),
        spec.rolePrincipalPattern(), USER_A);
    assertIdentity(daemon(result, "HBASE_THRIFT", "hbase_thrift_hbase"),
        spec.rolePrincipalPattern(), USER_A);
    assertEquals("/smokeuser", hbase.getIdentity("hbase_smokeuser").getReference());
    assertTrue(hbase.getAuthToLocalProperties().contains(
        ManagedHBaseKerberosOverlaySpec.AUTH_TO_LOCAL_PROPERTY));
    assertEquals(USER_A, hbase.getConfiguration("hbase-site")
        .getProperty("hbase.superuser"));
    assertEquals(spec.znodeParent(), hbase.getConfiguration("hbase-site")
        .getProperty("zookeeper.znode.parent"));
  }

  @Test
  void rejectsUserPrincipalCredentialNamespaceAndAdminReferenceConflicts()
      throws Exception {
    assertCode("DEPENDENCY_KERBEROS_OVERRIDE_CONFLICT",
        () -> overlay.applyCopy(hbaseDescriptor(), userDescriptor("""
            {"services":[{"name":"HBASE","components":[{
              "name":"HBASE_MASTER","identities":[{
                "name":"hbase_master_hbase",
                "principal":{"value":"hbase/_HOST@${realm}"}
              }]}]}]}
            """), spec(USER_A, true, false)));
    assertCode("DEPENDENCY_KERBEROS_OVERRIDE_CONFLICT",
        () -> overlay.applyCopy(hbaseDescriptor(), userDescriptor("""
            {"services":[{"name":"HBASE","identities":[{
              "name":"hbase","keytab":{"file":"/ignored",
                "owner":{"name":"root"}}}]}]}
            """), spec(USER_A, true, false)));
    assertCode("DEPENDENCY_KERBEROS_OVERRIDE_CONFLICT",
        () -> overlay.applyCopy(hbaseDescriptor(), userDescriptor("""
            {"services":[{"name":"HBASE","configurations":[{
              "hbase-site":{"zookeeper.znode.parent":"/hbase-secure"}
            }]}]}
            """), spec(USER_A, false, true)));
    assertCode("DEPENDENCY_KERBEROS_OVERRIDE_CONFLICT",
        () -> overlay.applyCopy(hbaseDescriptor(), userDescriptor("""
            {"services":[{"name":"HBASE","components":[{
              "name":"HBASE_MASTER","identities":[{
                "name":"hbase_hbase_master_hdfs","reference":"/HDFS/NAMENODE/hdfs"
              }]}]}]}
            """), spec(USER_A, true, false)));
  }

  @Test
  void rejectsRootAndComponentConfigurationOverridesAfterCompositeMerge()
      throws Exception {
    KerberosDescriptor rootOverride = userDescriptor("""
        {"configurations":[{
          "hbase-site":{"hbase.superuser":"shared_hbase"}
        }]}
        """);
    KerberosDescriptor componentOverride = userDescriptor("""
        {"services":[{"name":"HBASE","components":[{
          "name":"HBASE_REGIONSERVER","configurations":[{
            "hbase-site":{"hbase.regionserver.kerberos.principal":"shared/_HOST@EXAMPLE.COM"}
          }]
        }]}]}
        """);
    String rootBefore = objectMapper.writeValueAsString(rootOverride.toMap());
    String componentBefore = objectMapper.writeValueAsString(componentOverride.toMap());

    assertCode("DEPENDENCY_KERBEROS_OVERRIDE_CONFLICT",
        () -> overlay.applyCopy(mergedWith(rootOverride), rootOverride,
            spec(USER_A, true, false)));
    assertCode("DEPENDENCY_KERBEROS_OVERRIDE_CONFLICT",
        () -> overlay.applyCopy(mergedWith(componentOverride), componentOverride,
            spec(USER_A, true, false)));
    assertEquals(rootBefore, objectMapper.writeValueAsString(rootOverride.toMap()));
    assertEquals(componentBefore, objectMapper.writeValueAsString(componentOverride.toMap()));
  }

  @Test
  void sealsCalculatedValuesAndProvesActualGeneratedConsumerRulesWithoutMutation()
      throws Exception {
    ManagedHBaseKerberosOverlaySpec spec = spec(USER_A, true, true);
    AuthToLocalBuilder rules = new AuthToLocalBuilder(REALM, java.util.List.of(), false);
    rules.addRule(spec.rolePrincipalPattern(), USER_A);
    rules.addRule(spec.headlessPrincipal(), USER_A);
    rules.addRule(spec.smokePrincipal(), spec.smokeShortUser());
    Map<String, Map<String, String>> input = new TreeMap<>();
    input.put("core-site", new TreeMap<>(Map.of(
        "hadoop.security.auth_to_local", rules.generate(),
        "fs.defaultFS", "hdfs://local")));
    input.put("hbase-site", new TreeMap<>(Map.of(
        "unchanged", "value",
        "hbase.superuser", "hbase")));
    String before = objectMapper.writeValueAsString(input);

    SealedConfigurations sealed = overlay.sealCalculatedConfigurations(input, spec);

    assertEquals(before, objectMapper.writeValueAsString(input));
    assertEquals(USER_A, sealed.configurations().get("hbase-env").get("hbase_user"));
    assertEquals(USER_A + "@" + REALM,
        sealed.configurations().get("hbase-env").get("hbase_principal_name"));
    assertEquals(spec.rolePrincipalPattern(),
        sealed.configurations().get("hbase-site")
            .get("hbase.regionserver.kerberos.principal"));
    assertEquals(spec.znodeParent(), sealed.configurations().get("hbase-site")
        .get("zookeeper.znode.parent"));
    assertEquals("value", sealed.configurations().get("hbase-site").get("unchanged"));
    assertEquals(USER_A, sealed.mappingProof().effectiveShortUser());
    assertEquals("ambari-qa", sealed.mappingProof().smokeShortUser());
    assertFalse(sealed.toString().contains(rules.generate()));
    assertThrows(UnsupportedOperationException.class,
        () -> sealed.configurations().get("hbase-site").put("unexpected", "value"));
  }

  @Test
  void refusesToSealAServiceCheckPolicyWithMismatchedSmokeMapping()
      throws Exception {
    ManagedHBaseKerberosOverlaySpec spec = spec(USER_A, true, false);
    AuthToLocalBuilder rules = new AuthToLocalBuilder(REALM, java.util.List.of(), false);
    rules.addRule(spec.rolePrincipalPattern(), USER_A);
    rules.addRule(spec.headlessPrincipal(), USER_A);
    rules.addRule(spec.smokePrincipal(), "shared_smoke");
    Map<String, Map<String, String>> input = Map.of("core-site", Map.of(
        "hadoop.security.auth_to_local", rules.generate()));

    assertCode("DEPENDENCY_HDFS_AUTH_TO_LOCAL_MISMATCH",
        () -> overlay.sealCalculatedConfigurations(input, spec));
  }

  private KerberosDescriptor hbaseDescriptor() throws IOException {
    String path = "stacks/BIGTOP/3.2.0/services/HBASE/kerberos.json";
    try (InputStream input = Objects.requireNonNull(
        getClass().getClassLoader().getResourceAsStream(path), path)) {
      try {
        return descriptorFactory.createInstance(
            new String(input.readAllBytes(), StandardCharsets.UTF_8));
      } catch (org.apache.ambari.server.AmbariException e) {
        throw new IOException("failed to load HBase Kerberos descriptor", e);
      }
    }
  }

  private KerberosDescriptor userDescriptor(String json) throws Exception {
    return descriptorFactory.createInstance(json);
  }

  private KerberosDescriptor mergedWith(KerberosDescriptor userDescriptor) throws IOException {
    KerberosDescriptor effective = hbaseDescriptor();
    effective.update(userDescriptor);
    return effective;
  }

  private ManagedHBaseKerberosOverlaySpec spec(String user, boolean hdfs,
      boolean zooKeeper) {
    SortedMap<ManagedDependencyType, ManagedBindingSnapshotRef> bindings =
        new TreeMap<>();
    if (hdfs) {
      bindings.put(ManagedDependencyType.HDFS,
          new ManagedBindingSnapshotRef(ManagedDependencyType.HDFS, HDFS_ID));
    }
    if (zooKeeper) {
      bindings.put(ManagedDependencyType.ZOOKEEPER,
          new ManagedBindingSnapshotRef(ManagedDependencyType.ZOOKEEPER, ZK_ID));
    }
    return ManagedHBaseKerberosOverlaySpec.create(REALM, user,
        "ambari-qa-draft@" + REALM, "ambari-qa", hash('e'), bindings);
  }

  private KerberosIdentityDescriptor daemon(KerberosDescriptor descriptor, String component,
      String identity) {
    KerberosComponentDescriptor role = descriptor.getService("HBASE").getComponent(component);
    return role.getIdentity(identity);
  }

  private void assertIdentity(KerberosIdentityDescriptor identity, String principal,
      String owner) {
    assertEquals(principal, identity.getPrincipalDescriptor().getValue());
    assertEquals(owner, identity.getPrincipalDescriptor().getLocalUsername());
    assertEquals(owner, identity.getKeytabDescriptor().getOwnerName());
    assertEquals("r", identity.getKeytabDescriptor().getOwnerAccess());
  }

  private void assertCode(String code, ThrowingRunnable action) {
    ManagedDependencyIntegrationException error = assertThrows(
        ManagedDependencyIntegrationException.class, action::run);
    assertEquals(code, error.getCode());
  }

  private static String hash(char value) {
    return "sha256:" + String.valueOf(value).repeat(64);
  }

  private record ListOfCalls(Callable<KerberosDescriptor> first,
      Callable<KerberosDescriptor> second) {
    private java.util.List<Callable<KerberosDescriptor>> values() {
      return java.util.List.of(first, second);
    }
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
