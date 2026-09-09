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

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.WebApplicationException;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.dao.ScopedWorkflowStateDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.ScopedWorkflowStateEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ClusterCreationContext;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;


public class PersistKeyValueImplTest extends Assert {

  public static final int NUMB_THREADS = 1000;

  private Injector injector;
  private PersistKeyValueImpl impl;
  private OrmTestHelper helper;
  private Clusters clusters;
  private UserDAO userDAO;
  private Users users;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    impl = injector.getInstance(PersistKeyValueImpl.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusters = injector.getInstance(Clusters.class);
    userDAO = injector.getInstance(UserDAO.class);
    users = injector.getInstance(Users.class);
    helper.createTestUsers();
    users.createUser("alice", "alice", "Alice");
    users.createUser("bob", "bob", "Bob");
    users.createUser("alice-admin", "alice-admin", "Alice Admin");
  }

  @After
  public void tearDown() throws Exception {
    SecurityContextHolder.clearContext();
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testStore() throws Exception {
    Map<String, String> map = impl.getAllKeyValues();
    assertEquals(0, map.size());

    impl.put("key1", "value1");
    impl.put("key2", "value2");

    map = impl.getAllKeyValues();
    assertEquals(2, map.size());
    assertEquals("value1", impl.getValue("key1"));
    assertEquals("value2", impl.getValue("key2"));
    assertEquals(map.get("key1"), impl.getValue("key1"));

    impl.put("key1", "value1-2");
    assertEquals("value1-2", impl.getValue("key1"));
    assertEquals(2, map.size());

    StringBuilder largeValueBuilder = new StringBuilder();
    for (int i = 0; i < 320; i++) {
      largeValueBuilder.append("0123456789");
    }
    String largeValue = largeValueBuilder.toString();

    impl.put("key3", largeValue);

    assertEquals(largeValue, impl.getValue("key3"));

  }

  @Test
  public void testMultiThreaded() throws Exception {
    Thread[] threads = new Thread[NUMB_THREADS];

    for ( int i = 0; i < NUMB_THREADS; ++i ) {
      threads[i] = new Thread() {
        @Override
        public void run() {

          for (int i = 0; i < 100; ++i) {
            impl.put("key1", "value1");
            impl.put("key2", "value2");
            impl.put("key3", "value3");
            impl.put("key4", "value4");
          }
        }
      };
    }

    for ( int i = 0; i < NUMB_THREADS; ++i ) {
      threads[i].start();
    }

    for ( int i = 0; i < NUMB_THREADS; ++i ) {
      threads[i].join();
    }
  }

  @Test
  public void testClusterScopedStateAuthorizationAndOwnership() throws Exception {
    Cluster clusterA = createCluster("scope-a");
    Cluster clusterB = createCluster("scope-b");
    authenticateClusterAdministrator("alice", clusterA.getResourceId());

    ScopedWorkflowState empty = impl.getScopedState("clusters", String.valueOf(clusterA.getClusterId()), false);
    assertEquals(0, empty.getRevision());
    try {
      impl.getScopedState("clusters", String.valueOf(clusterB.getClusterId()), true);
      fail("Expected cluster B state to be hidden from an A-only user");
    } catch (AuthorizationException expected) {
      // Expected.
    }

    ScopedWorkflowState claimed = impl.putScopedState("clusters", String.valueOf(clusterA.getClusterId()),
        update(0, "ADD_HOST", "SELECT_HOSTS", Map.of("hosts", 2)));
    assertEquals(1, claimed.getRevision());
    assertEquals("alice", claimed.getOwner());

    authenticateClusterAdministrator("bob", clusterA.getResourceId());
    try {
      impl.putScopedState("clusters", String.valueOf(clusterA.getClusterId()),
          update(1, "ADD_HOST", "INSTALL", Map.of()));
      fail("Expected the active workflow owner to be enforced");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 409, "WORKFLOW_OWNED");
    }
  }

  @Test
  public void testServicePlanRequiresExactOwnedAddServiceCheckpoint() throws Exception {
    Cluster cluster = createCluster("scope-service-plan");
    authenticateClusterAdministrator("alice", cluster.getResourceId());
    String clusterId = String.valueOf(cluster.getClusterId());
    Map<String, Object> services = Map.of("HBASE", Map.of("selected", true));
    impl.putScopedState("clusters", clusterId, update(0, "ADD_SERVICE", "SERVICES",
        Map.of("ADD_SERVICE", Map.of("addServiceSteps", Map.of(
            "SERVICES", Map.of("data", Map.of("services", services)))))));

    ScopedWorkflowState current = impl.getActiveOwnedClusterWorkflowState(
        cluster.getClusterId(), "ADD_SERVICE", 1);
    assertEquals(1, current.getRevision());

    try {
      impl.getActiveOwnedClusterWorkflowState(cluster.getClusterId(), "ADD_SERVICE", 2);
      fail("Expected a stale planning revision to fail");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 409, "WORKFLOW_VERSION_CONFLICT");
    }

    authenticateClusterAdministrator("bob", cluster.getResourceId());
    try {
      impl.getActiveOwnedClusterWorkflowState(cluster.getClusterId(), "ADD_SERVICE", 1);
      fail("Expected another user's planning checkpoint to remain private");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 409, "WORKFLOW_VERSION_CONFLICT");
    }
  }

  @Test
  public void testScopedStateReleaseRetainsRevisionAndReloads() throws Exception {
    Cluster cluster = createCluster("scope-release");
    authenticateClusterAdministrator("alice", cluster.getResourceId());
    String clusterId = String.valueOf(cluster.getClusterId());

    impl.putScopedState("clusters", clusterId,
        update(0, "ADD_SERVICE", "CONFIGURE", Map.of("service", "HDFS")));
    ScopedWorkflowState released = impl.putScopedState("clusters", clusterId,
        update(1, "IDLE", null, Map.of("ignored", true)));
    assertEquals(2, released.getRevision());
    assertNull(released.getOwner());
    assertEquals("IDLE", released.getWorkflow());
    assertTrue(released.getValues().isEmpty());

    try {
      impl.putScopedState("clusters", clusterId,
          update(1, "ADD_SERVICE", "INSTALL", Map.of()));
      fail("Expected a stale write after release to fail");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 409, "WORKFLOW_VERSION_CONFLICT");
    }

    PersistKeyValueImpl reloaded = new PersistKeyValueImpl();
    reloaded.keyValueDAO = injector.getInstance(KeyValueDAO.class);
    reloaded.scopedWorkflowStateDAO = injector.getInstance(ScopedWorkflowStateDAO.class);
    reloaded.userDAO = userDAO;
    reloaded.clusters = clusters;
    ScopedWorkflowState restored = reloaded.getScopedState("clusters", clusterId, false);
    assertEquals(2, restored.getRevision());
    assertEquals("IDLE", restored.getPhase());
  }

  @Test
  public void testRollbackHighAvailabilityScopedWriteReadAndRelease() throws Exception {
    Cluster cluster = createCluster("scope-rollback-ha");
    authenticateClusterAdministrator("alice", cluster.getResourceId());
    String clusterId = String.valueOf(cluster.getClusterId());

    ScopedWorkflowState stored = impl.putScopedState("clusters", clusterId,
        update(0, "ROLLBACK_HIGH_AVAILABILITY", "CHECKPOINT",
            Map.of("activeNameNode", "nn-a")));
    assertEquals(1, stored.getRevision());
    assertEquals("ROLLBACK_HIGH_AVAILABILITY", stored.getWorkflow());
    assertEquals("nn-a", impl.getScopedState("clusters", clusterId, false)
        .getValues().get("activeNameNode"));

    ScopedWorkflowState released = impl.putScopedState("clusters", clusterId,
        update(1, "IDLE", null, Map.of()));
    assertEquals(2, released.getRevision());
    assertEquals("IDLE", released.getWorkflow());
    assertTrue(released.getValues().isEmpty());
  }

  @Test
  public void testConcurrentInitialScopedWritesHaveOneWinner() throws Exception {
    Cluster cluster = createCluster("scope-race");
    String clusterId = String.valueOf(cluster.getClusterId());
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<String> first = executor.submit(() -> putScopedAfterBarrier(
          clusterId, cluster.getResourceId(), "alice", ready, start));
      Future<String> second = executor.submit(() -> putScopedAfterBarrier(
          clusterId, cluster.getResourceId(), "bob", ready, start));
      assertTrue(ready.await(30, TimeUnit.SECONDS));
      start.countDown();

      String firstResult = first.get(30, TimeUnit.SECONDS);
      String secondResult = second.get(30, TimeUnit.SECONDS);
      assertEquals(1, java.util.stream.Stream.of(firstResult, secondResult)
          .filter(result -> result.startsWith("stored:")).count());
      assertEquals(1, java.util.stream.Stream.of(firstResult, secondResult)
          .filter(result -> result.equals("conflict:WORKFLOW_VERSION_CONFLICT")).count());
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void testDraftScopesAreSeparatedByAuthenticatedOwner() throws Exception {
    String draftId = UUID.randomUUID().toString();
    authenticateAdministrator("alice");
    ScopedWorkflowState alice = impl.putScopedState("drafts", draftId,
        update(0, "CLUSTER_CREATE", "HOSTS", Map.of("clusterName", "alpha")));
    assertEquals("alice", alice.getOwner());

    authenticateAdministrator("bob");
    ScopedWorkflowState bob = impl.getScopedState("drafts", draftId, false);
    assertEquals(0, bob.getRevision());
    assertTrue(bob.getValues().isEmpty());
  }

  @Test
  public void testCreationDraftDirectoryAndClusterReconciliationUseImmutableOwner() throws Exception {
    String createdDraftId = "00000000-0000-0000-0000-000000000001";
    String pendingDraftId = "00000000-0000-0000-0000-000000000002";
    authenticateAdministrator("alice");
    impl.putScopedState("drafts", createdDraftId,
        update(0, "CLUSTER_CREATE", "REVIEW", Map.of("clusterName", "draft-cluster")));
    impl.putScopedState("drafts", pendingDraftId,
        update(0, "CLUSTER_CREATE", "HOSTS", Map.of("clusterName", "pending-cluster")));
    ClusterCreationContext creationContext = impl.validateClusterCreationDraft(createdDraftId);
    StackId stackId = new StackId("HDP-0.1");
    helper.createStack(stackId);
    Cluster cluster = clusters.addCluster("draft-cluster", stackId, null, creationContext);

    ScopedWorkflowState checkpoint = impl.putScopedState("drafts", createdDraftId,
        update(1, "CLUSTER_CREATE", "DEPLOYING", Map.of("requestId", 17)));
    assertEquals(2, checkpoint.getRevision());
    assertEquals("DEPLOYING", checkpoint.getPhase());
    assertNotNull(impl.validateClusterCreationDraft(createdDraftId));

    Map<String, Object> associated = impl.getCreationDraftCluster(createdDraftId);
    assertEquals(cluster.getClusterId(), associated.get("cluster_id"));
    assertEquals("draft-cluster", associated.get("cluster_name"));
    Map<String, Object> directory = impl.getCreationDraftDirectory();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) directory.get("items");
    assertEquals(2, items.size());
    assertEquals(createdDraftId, items.get(0).get("draft_id"));
    assertEquals(cluster.getClusterId(), items.get(0).get("cluster_id"));
    assertEquals(2L, ((Number) items.get(0).get("revision")).longValue());
    assertEquals("DEPLOYING", items.get(0).get("phase"));
    assertEquals(pendingDraftId, items.get(1).get("draft_id"));
    assertFalse(items.get(1).containsKey("cluster_id"));

    impl.putScopedState("drafts", createdDraftId, update(2, "IDLE", null, Map.of()));
    ScopedWorkflowStateEntity released = injector.getInstance(ScopedWorkflowStateDAO.class).findByKey(
        PersistKeyValueImpl.SCOPED_KEY_PREFIX + "drafts:"
            + userDAO.findUserByName("alice").getUserId() + ":" + createdDraftId);
    assertEquals(cluster.getClusterId(), released.getCreatedClusterId().longValue());
    try {
      impl.putScopedState("drafts", createdDraftId,
          update(3, "CLUSTER_CREATE", "RETRY", Map.of()));
      fail("Expected a released draft not to be reusable");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 409, "CREATION_DRAFT_RETIRED");
    }

    clusters.deleteCluster("draft-cluster");
    try {
      impl.getCreationDraftCluster(createdDraftId);
      fail("Expected a deleted cluster association to remain hidden");
    } catch (WebApplicationException e) {
      assertEquals(404, e.getResponse().getStatus());
    }
    try {
      impl.validateClusterCreationDraft(createdDraftId);
      fail("Expected a consumed draft not to create a replacement cluster");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 409, "CREATION_DRAFT_CONSUMED");
    }
  }

  @Test
  public void testScopedAndLegacyPersistenceBoundaries() throws Exception {
    Cluster cluster = createCluster("legacy-scope");
    impl.put("admin-settings-timezone-alice", "UTC");
    impl.put("admin-settings-timezone-bob", "PST");
    impl.keyValueDAO.updateValueWithLock(
        PersistKeyValueImpl.SCOPED_KEY_PREFIX + "clusters:" + cluster.getClusterId(),
        ignored -> "reserved");
    authenticateClusterAdministrator("alice", cluster.getResourceId());

    Map<String, String> visible = impl.getAllLegacyKeyValues();
    assertEquals("UTC", visible.get("admin-settings-timezone-alice"));
    assertFalse(visible.containsKey("admin-settings-timezone-bob"));
    assertEquals(1, visible.size());
    try {
      impl.getLegacyValue(PersistKeyValueImpl.SCOPED_KEY_PREFIX + "clusters:" + cluster.getClusterId());
      fail("Expected reserved scoped keys to be inaccessible through legacy APIs");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 403, "RESERVED_PERSIST_KEY");
    }
    try {
      impl.putLegacyValues(Map.of("USER_REDIRECTION_URL", "/clusters/other"));
      fail("Expected ambiguous user state to require an explicit scope");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 409, "LEGACY_PERSIST_SCOPE_REQUIRED");
    }
  }

  @Test
  public void testScopedStateRedactsCredentialValuesAndRejectsOpaqueCredentialBlobs() throws Exception {
    Cluster cluster = createCluster("scope-sensitive");
    authenticateClusterAdministrator("alice", cluster.getResourceId());

    Map<String, Object> passwordConfig = Map.of(
        "name", "database_password",
        "value", "do-not-store",
        "propertyAttributes", Map.of("type", "password", "required", true));
    ScopedWorkflowState stored = impl.putScopedState("clusters", String.valueOf(cluster.getClusterId()),
        update(0, "ADD_SERVICE", "CONFIGURE", Map.of("configs", java.util.List.of(passwordConfig))));
    @SuppressWarnings("unchecked")
    Map<String, Object> sanitizedConfig = (Map<String, Object>)
        ((java.util.List<?>) stored.getValues().get("configs")).get(0);
    assertEquals("database_password", sanitizedConfig.get("name"));
    assertFalse(sanitizedConfig.containsKey("value"));
    assertEquals("password", ((Map<?, ?>) sanitizedConfig.get("propertyAttributes")).get("type"));
    assertEquals(Boolean.TRUE, sanitizedConfig.get("requires_reentry"));

    ScopedWorkflowState credentialsList = impl.putScopedState("clusters",
        String.valueOf(cluster.getClusterId()), update(1, "ADD_SERVICE", "CONFIGURE",
            Map.of("credentials", java.util.List.of("plain-secret", "second-secret"))));
    assertFalse(credentialsList.getValues().containsKey("credentials"));
    assertEquals(Boolean.TRUE, credentialsList.getValues().get("requires_reentry"));

    Map<String, Object> actualFrontendShapes = Map.of(
        "sshKey", "-----BEGIN PRIVATE KEY-----secret",
        "config", Map.of(
            "propertyName", "database_password",
            "value", "secret",
            "defaultValue", "default-secret",
            "confirmPassword", "secret",
            "propertyAttributes", Map.of("type", "password")),
        "passwordProperty", Map.of(
            "property_name", "database_password",
            "property_value", "secret",
            "property_value_attributes", Map.of("type", "password")),
        "repository", Map.of(
            "baseUrl", "https://repository-user:repository-password@example.invalid/repo",
            "defaultUrl", "https://example.invalid/public"),
        "clusterCreationSteps", Map.of(
            "VERSION", Map.of("data", Map.of(
                "versionDefinitionSource", Map.of(
                    "type", "xml",
                    "payload", "<repository><baseurl>https://user:password@example.invalid/repo</baseurl></repository>",
                    "headers", Map.of("Content-Type", "text/xml")))),
            "VERSION_URL", Map.of("data", Map.of(
                "versionDefinitionSource", Map.of(
                    "type", "url",
                    "payload", Map.of("VersionDefinition", Map.of(
                        "version_url", "https://user:password@example.invalid/version.xml")),
                    "headers", Map.of("Authorization", "do-not-store"))))),
        "keytabPath", Map.of("name", "service_keytab", "value", "/etc/security/keytabs/service.keytab"));
    ScopedWorkflowState frontendShape = impl.putScopedState("clusters",
        String.valueOf(cluster.getClusterId()), update(2, "ADD_SERVICE", "CONFIGURE", actualFrontendShapes));
    assertFalse(frontendShape.getValues().containsKey("sshKey"));
    assertEquals(Boolean.TRUE, frontendShape.getValues().get("requires_reentry"));
    Map<?, ?> config = (Map<?, ?>) frontendShape.getValues().get("config");
    assertFalse(config.containsKey("value"));
    assertFalse(config.containsKey("defaultValue"));
    assertFalse(config.containsKey("confirmPassword"));
    Map<?, ?> nestedPassword = (Map<?, ?>) frontendShape.getValues().get("passwordProperty");
    assertFalse(nestedPassword.containsKey("property_value"));
    Map<?, ?> repository = (Map<?, ?>) frontendShape.getValues().get("repository");
    assertFalse(repository.containsKey("baseUrl"));
    assertEquals("https://example.invalid/public", repository.get("defaultUrl"));
    assertEquals(Boolean.TRUE, repository.get("requires_reentry"));
    Map<?, ?> clusterCreationSteps = (Map<?, ?>) frontendShape.getValues().get("clusterCreationSteps");
    Map<?, ?> version = (Map<?, ?>) clusterCreationSteps.get("VERSION");
    Map<?, ?> versionData = (Map<?, ?>) version.get("data");
    Map<?, ?> versionSource = (Map<?, ?>) versionData.get("versionDefinitionSource");
    assertEquals("xml", versionSource.get("type"));
    assertFalse(versionSource.containsKey("payload"));
    assertFalse(versionSource.containsKey("headers"));
    assertEquals(Boolean.TRUE, versionSource.get("requires_reentry"));
    Map<?, ?> urlVersion = (Map<?, ?>) clusterCreationSteps.get("VERSION_URL");
    Map<?, ?> urlVersionData = (Map<?, ?>) urlVersion.get("data");
    Map<?, ?> urlVersionSource = (Map<?, ?>) urlVersionData.get("versionDefinitionSource");
    assertEquals("url", urlVersionSource.get("type"));
    assertFalse(urlVersionSource.containsKey("headers"));
    assertEquals(Boolean.TRUE, urlVersionSource.get("requires_reentry"));
    Map<?, ?> urlPayload = (Map<?, ?>) urlVersionSource.get("payload");
    Map<?, ?> versionDefinition = (Map<?, ?>) urlPayload.get("VersionDefinition");
    assertFalse(versionDefinition.containsKey("version_url"));
    assertEquals("/etc/security/keytabs/service.keytab",
        ((Map<?, ?>) frontendShape.getValues().get("keytabPath")).get("value"));

    try {
      impl.putScopedState("clusters", String.valueOf(cluster.getClusterId()),
          update(3, "ADD_SERVICE", "CONFIGURE",
              Map.of("opaque", "{\"password\":\"do-not-store\"}")));
      fail("Expected an opaque credential-bearing value to be rejected");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 400, "SENSITIVE_WORKFLOW_STATE");
    }

    try {
      impl.putScopedState("clusters", String.valueOf(cluster.getClusterId()),
          update(3, "ADD_SERVICE", "CONFIGURE",
              Map.of("opaque", "-----BEGIN RSA PRIVATE KEY-----\nprivate\n-----END RSA PRIVATE KEY-----")));
      fail("Expected an opaque PEM private key to be rejected");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 400, "SENSITIVE_WORKFLOW_STATE");
    }
  }

  @Test
  public void testActiveWorkflowMustBeReleasedBeforeChangingType() throws Exception {
    Cluster cluster = createCluster("scope-switch");
    authenticateClusterAdministrator("alice", cluster.getResourceId());
    String clusterId = String.valueOf(cluster.getClusterId());

    impl.putScopedState("clusters", clusterId,
        update(0, "ADD_HOST", "HOSTS", Map.of()));
    try {
      impl.putScopedState("clusters", clusterId,
          update(1, "ADD_SERVICE", "SERVICES", Map.of()));
      fail("Expected an active workflow type switch to require release");
    } catch (WebApplicationException e) {
      assertWorkflowError(e, 409, "WORKFLOW_ACTIVE");
    }

    impl.putScopedState("clusters", clusterId, update(1, "IDLE", null, Map.of()));
    ScopedWorkflowState switched = impl.putScopedState("clusters", clusterId,
        update(2, "ADD_SERVICE", "SERVICES", Map.of()));
    assertEquals("ADD_SERVICE", switched.getWorkflow());
  }

  @Test
  public void testDeletedAndRecreatedUserCannotInheritDraft() throws Exception {
    String draftId = UUID.randomUUID().toString();
    authenticateAdministrator("alice");
    impl.putScopedState("drafts", draftId,
        update(0, "CLUSTER_CREATE", "HOSTS", Map.of("clusterName", "private")));

    Integer oldId = userDAO.findUserByName("alice").getUserId();
    users.removeUser(users.getUser("alice"));
    UserEntity recreated = users.createUser("alice", "alice", "Recreated Alice");
    assertNotEquals(oldId, recreated.getUserId());
    authenticateAdministrator("alice");

    ScopedWorkflowState state = impl.getScopedState("drafts", draftId, false);
    assertEquals(0, state.getRevision());
    assertTrue(state.getValues().isEmpty());
    assertTrue(((List<?>) impl.getCreationDraftDirectory().get("items")).isEmpty());
    try {
      impl.getCreationDraftCluster(draftId);
      fail("Expected a recreated account not to inherit the old draft association");
    } catch (WebApplicationException e) {
      assertEquals(404, e.getResponse().getStatus());
    }
  }

  @Test
  public void testHyphenatedLegacyPreferenceOwnershipUsesKnownSuffixes() throws Exception {
    Cluster cluster = createCluster("legacy-users");
    impl.put("user-pref-alice-supports", "alice");
    impl.put("user-pref-alice-admin-supports", "alice-admin");
    impl.put("user-pref-alice-arbitrary", "unknown");
    authenticateClusterAdministrator("alice", cluster.getResourceId());

    Map<String, String> visible = impl.getAllLegacyKeyValues();
    assertEquals("alice", visible.get("user-pref-alice-supports"));
    assertFalse(visible.containsKey("user-pref-alice-admin-supports"));
    assertFalse(visible.containsKey("user-pref-alice-arbitrary"));
    impl.putLegacyValues(Map.of("user-pref-alice-dashboard", "layout"));
    assertEquals("layout", impl.getLegacyValue("user-pref-alice-dashboard"));
    try {
      impl.putLegacyValues(Map.of("user-pref-alice-admin-supports", "overwrite"));
      fail("Expected another hyphenated user's preference to be rejected");
    } catch (AuthorizationException expected) {
      // Expected.
    }
  }

  @Test
  public void testScopedStateStoresPayloadLargerThanLegacyKeyValueLimit() throws Exception {
    Cluster cluster = createCluster("scope-large");
    authenticateClusterAdministrator("alice", cluster.getResourceId());
    String largeValue = "x".repeat(100_000);

    ScopedWorkflowState state = impl.putScopedState("clusters", String.valueOf(cluster.getClusterId()),
        update(0, "ADD_HOST", "HOSTS", Map.of("assignments", largeValue)));

    assertEquals(largeValue, state.getValues().get("assignments"));
  }

  private Cluster createCluster(String clusterName) throws Exception {
    StackId stackId = new StackId("HDP-0.1");
    helper.createStack(stackId);
    clusters.addCluster(clusterName, stackId);
    return clusters.getCluster(clusterName);
  }

  private ScopedWorkflowUpdate update(long expectedRevision, String workflow, String phase,
      Map<String, Object> values) {
    ScopedWorkflowUpdate update = new ScopedWorkflowUpdate();
    update.setExpectedRevision(expectedRevision);
    update.setWorkflow(workflow);
    update.setPhase(phase);
    update.setValues(values);
    return update;
  }

  private String putScopedAfterBarrier(String clusterId, Long resourceId, String username,
      CountDownLatch ready, CountDownLatch start) throws Exception {
    authenticateClusterAdministrator(username, resourceId);
    ready.countDown();
    assertTrue(start.await(30, TimeUnit.SECONDS));
    try {
      ScopedWorkflowState state = impl.putScopedState("clusters", clusterId,
          update(0, "ADD_HOST", "SELECT_HOSTS", Map.of("user", username)));
      return "stored:" + state.getOwner();
    } catch (WebApplicationException e) {
      @SuppressWarnings("unchecked")
      Map<String, String> error = (Map<String, String>) e.getResponse().getEntity();
      return "conflict:" + error.get("code");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void assertWorkflowError(WebApplicationException exception, int status, String code) {
    assertEquals(status, exception.getResponse().getStatus());
    @SuppressWarnings("unchecked")
    Map<String, String> error = (Map<String, String>) exception.getResponse().getEntity();
    assertEquals(code, error.get("code"));
  }

  private void authenticateAdministrator(String username) {
    UserEntity user = userDAO.findUserByName(username);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(user.getUserId(), username));
  }

  private void authenticateClusterAdministrator(String username, Long resourceId) {
    UserEntity user = userDAO.findUserByName(username);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator(user.getUserId(), username, resourceId));
  }
}
