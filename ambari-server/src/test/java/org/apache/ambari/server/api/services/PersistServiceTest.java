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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.RandomPortJerseyTest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ClusterCreationContext;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.StageUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class PersistServiceTest extends RandomPortJerseyTest {
  static String PACKAGE_NAME = "org.apache.ambari.server.api.services";
  Injector injector;
  protected Client client;

  public PersistServiceTest() {
    super();
  }

  public class MockModule extends AbstractModule {

    @Override
    protected void configure() {
      requestStaticInjection(PersistKeyValueService.class);
    }
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new InMemoryDefaultTestModule(), new MockModule());
    injector.getInstance(GuiceJpaInitializer.class);
    OrmTestHelper helper = injector.getInstance(OrmTestHelper.class);
    helper.createTestUsers();
    UserEntity administrator = injector.getInstance(UserDAO.class).findUserByName("administrator");
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(administrator.getUserId(), "administrator"));
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    SecurityContextHolder.clearContext();
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Override
  protected ResourceConfig configure() {
    return new ResourceConfig().packages(PACKAGE_NAME).register(JacksonFeature.class);
  }

  @Test
  public void testPersistAPIs() throws IOException {
    String input = "{\"key1\" : \"value1\",\"key2\" : \"value2\"}";
    String response = target("persist").request().post(Entity.text(input), String.class);
    assertEquals("", response);
    // END GENAI@CHATGPT4


    String result = target("persist/key1").request().get(String.class);
    assertEquals("value1", result);
    result = target("persist/key2").request().get(String.class);
    assertEquals("value2", result);

    String values = "[\"value3\", \"value4\"]";
    String putResponse = target("persist").request().put(Entity.text(values), String.class);
    Collection<String> keys = StageUtils.fromJson(putResponse, Collection.class);
    assertEquals(2, keys.size());

    String getAllResponse = target("persist").request().get(String.class);
    Map<String, String> allKeys = StageUtils.fromJson(getAllResponse, Map.class);
    assertEquals(4, allKeys.size());
    assertEquals("value1", allKeys.get("key1"));
    assertEquals("value2", allKeys.get("key2"));
  }

  @Test
  public void testScopedWorkflowStateApiAndSummary() throws Exception {
    OrmTestHelper helper = injector.getInstance(OrmTestHelper.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    StackId stackId = new StackId("HDP-0.1");
    helper.createStack(stackId);
    clusters.addCluster("api-scope", stackId);
    Cluster cluster = clusters.getCluster("api-scope");
    UserEntity alice = injector.getInstance(Users.class).createUser("alice", "alice", "Alice");
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator(
            alice.getUserId(), "alice", cluster.getResourceId()));

    String path = "persist/scopes/clusters/" + cluster.getClusterId();
    String request = "{\"expected_revision\":0,\"workflow\":\"ADD_HOST\","
        + "\"phase\":\"SELECT_HOSTS\",\"values\":{\"count\":2}}";
    Response putResponse = target(path).request().put(Entity.json(request));
    assertEquals(200, putResponse.getStatus());
    Map<String, Object> stored = StageUtils.fromJson(putResponse.readEntity(String.class), Map.class);
    assertEquals(1, ((Number) stored.get("revision")).intValue());
    assertEquals("alice", stored.get("owner"));
    assertEquals("ADD_HOST", stored.get("workflow"));
    assertTrue(stored.containsKey("values"));

    Response summaryResponse = target(path).queryParam("summary", true).request().get();
    assertEquals(200, summaryResponse.getStatus());
    Map<String, Object> summary = StageUtils.fromJson(summaryResponse.readEntity(String.class), Map.class);
    assertEquals(1, ((Number) summary.get("revision")).intValue());
    assertFalse(summary.containsKey("values"));
  }

  @Test
  public void testScopedWorkflowRequestBodyLimitIsCheckedBeforeJsonParsing() {
    String oversized = "x".repeat(PersistKeyValueImpl.MAX_SCOPED_REQUEST_BYTES + 1);

    Response response = target("persist/scopes/drafts/00000000-0000-0000-0000-000000000001")
        .request().put(Entity.json(oversized));

    assertEquals(413, response.getStatus());
  }

  @Test
  public void testOwnedCreationDraftDirectoryAndReconciliationEndpoints() throws Exception {
    String draftId = "00000000-0000-0000-0000-000000000001";
    UserEntity alice = injector.getInstance(Users.class).createUser("alice", "alice", "Alice");
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator(alice.getUserId(), "alice"));
    String draftPath = "persist/scopes/drafts/" + draftId;
    Response put = target(draftPath).request().put(Entity.json(
        "{\"expected_revision\":0,\"workflow\":\"CLUSTER_CREATE\","
            + "\"phase\":\"REVIEW\",\"values\":{\"clusterName\":\"api-draft\"}}"));
    assertEquals(200, put.getStatus());

    PersistKeyValueImpl persistence = injector.getInstance(PersistKeyValueImpl.class);
    ClusterCreationContext context = persistence.validateClusterCreationDraft(draftId);
    OrmTestHelper helper = injector.getInstance(OrmTestHelper.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    StackId stackId = new StackId("HDP-0.1");
    helper.createStack(stackId);
    Cluster cluster = clusters.addCluster("api-draft", stackId, null, context);

    Response directoryResponse = target("persist/scopes/drafts").request().get();
    assertEquals(200, directoryResponse.getStatus());
    Map<String, Object> directory = StageUtils.fromJson(
        directoryResponse.readEntity(String.class), Map.class);
    Collection<?> items = (Collection<?>) directory.get("items");
    assertEquals(1, items.size());

    Response clusterResponse = target(draftPath + "/cluster").request().get();
    assertEquals(200, clusterResponse.getStatus());
    Map<String, Object> association = StageUtils.fromJson(
        clusterResponse.readEntity(String.class), Map.class);
    assertEquals(cluster.getClusterId(),
        ((Number) association.get("cluster_id")).longValue());
    assertEquals("api-draft", association.get("cluster_name"));
  }
}
