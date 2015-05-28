/**
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

package org.apache.ambari.view.hive.resources.savedQueries;

import org.apache.ambari.view.hive.HDFSTest;
import org.apache.ambari.view.hive.utils.NotFoundFormattedException;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class SavedQueryServiceTest extends HDFSTest {
  //TODO: run without HDFS cluster
  private SavedQueryService savedQueryService;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void startUp() throws Exception {
    HDFSTest.startUp(); // super
  }

  @AfterClass
  public static void shutDown() throws Exception {
    HDFSTest.shutDown(); // super
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    savedQueryService = getService(SavedQueryService.class, handler, context);
    savedQueryService.getSharedObjectsFactory().clear();
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Override
  protected void setupProperties(Map<String, String> properties, File baseDir) throws Exception {
    super.setupProperties(properties, baseDir);
    properties.put("scripts.dir", "/tmp/.hiveQueries");
  }

  private Response doCreateSavedQuery() {
      return doCreateSavedQuery("Luke", "/tmp/luke.hql", savedQueryService);
  }

  public static Response doCreateSavedQuery(String title, String path, SavedQueryService service) {
    SavedQueryService.SavedQueryRequest request = new SavedQueryService.SavedQueryRequest();
    request.savedQuery = new SavedQuery();
    request.savedQuery.setTitle(title);
    request.savedQuery.setQueryFile(path);

    UriInfo uriInfo = createNiceMock(UriInfo.class);
    URI uri = UriBuilder.fromUri("http://host/a/b").build();
    expect(uriInfo.getAbsolutePath()).andReturn(uri);

    HttpServletResponse resp_obj = createNiceMock(HttpServletResponse.class);

    resp_obj.setHeader(eq("Location"), anyString());

    replay(uriInfo, resp_obj);
    return service.create(request, resp_obj, uriInfo);
  }

  private Response doCreateSavedQuery(String title, String path) {
      return doCreateSavedQuery(title, path, savedQueryService);
  }

  @Test
  public void createSavedQuery() {
    Response response = doCreateSavedQuery();
    Assert.assertEquals(201, response.getStatus());

    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("savedQuery"));
    Assert.assertNotNull(((SavedQuery) obj.get("savedQuery")).getId());
    Assert.assertTrue(((SavedQuery) obj.get("savedQuery")).getId() != null);
  }

  @Test
  public void createSavedQueryAutoCreate() {
    Response response = doCreateSavedQuery("Test", null);
    Assert.assertEquals(201, response.getStatus());

    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("savedQuery"));
    Assert.assertNotNull(((SavedQuery) obj.get("savedQuery")).getId());
    Assert.assertFalse(((SavedQuery) obj.get("savedQuery")).getId() == null);
    Assert.assertFalse(((SavedQuery) obj.get("savedQuery")).getQueryFile().isEmpty());
  }

  @Test
  public void notFound() {
    thrown.expect(NotFoundFormattedException.class);
    savedQueryService.getOne("4242");
  }

  @Test
  public void update() {
    Response created = doCreateSavedQuery();
    Object createdId = ((SavedQuery) ((JSONObject) created.getEntity()).get("savedQuery")).getId();

    SavedQueryService.SavedQueryRequest request = new SavedQueryService.SavedQueryRequest();
    request.savedQuery = new SavedQuery();
    request.savedQuery.setTitle("Updated Query");

    Response response = savedQueryService.update(request, String.valueOf(createdId));
    Assert.assertEquals(204, response.getStatus());

    Response response2 = savedQueryService.getOne(String.valueOf(createdId));
    Assert.assertEquals(200, response2.getStatus());

    JSONObject obj = ((JSONObject) response2.getEntity());
    Assert.assertTrue(obj.containsKey("savedQuery"));
    Assert.assertEquals(((SavedQuery) obj.get("savedQuery")).getTitle(), request.savedQuery.getTitle());
  }

  @Test
  public void delete() {
    Response created = doCreateSavedQuery();
    Object createdId = ((SavedQuery) ((JSONObject) created.getEntity()).get("savedQuery")).getId();

    Response response = savedQueryService.delete(String.valueOf(createdId));
    Assert.assertEquals(204, response.getStatus());

    thrown.expect(NotFoundFormattedException.class);
    savedQueryService.getOne(String.valueOf(createdId));
  }

  @Test
  public void list() {
    doCreateSavedQuery("Title 1", "/path/to/file.hql");
    doCreateSavedQuery("Title 2", "/path/to/file.hql");

    Response response = savedQueryService.getList();
    Assert.assertEquals(200, response.getStatus());

    JSONObject obj = (JSONObject) response.getEntity();
    Assert.assertTrue(obj.containsKey("savedQueries"));
    List<SavedQuery> items = (List<SavedQuery>) obj.get("savedQueries");
    boolean containsTitle = false;
    for(SavedQuery item : items)
        containsTitle = containsTitle || item.getTitle().compareTo("Title 1") == 0;
    Assert.assertTrue(containsTitle);

    containsTitle = false;
    for(SavedQuery item : items)
        containsTitle = containsTitle || item.getTitle().compareTo("Title 2") == 0;
    Assert.assertTrue(containsTitle);
  }
}
