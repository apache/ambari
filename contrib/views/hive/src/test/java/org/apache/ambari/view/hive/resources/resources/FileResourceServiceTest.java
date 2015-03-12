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

package org.apache.ambari.view.hive.resources.resources;

import org.apache.ambari.view.hive.BaseHiveTest;
import org.apache.ambari.view.hive.resources.resources.FileResourceItem;
import org.apache.ambari.view.hive.resources.resources.FileResourceService;
import org.apache.ambari.view.hive.utils.NotFoundFormattedException;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.easymock.EasyMock.*;

public class FileResourceServiceTest extends BaseHiveTest {
  @Rule public ExpectedException thrown = ExpectedException.none();
  private FileResourceService resourceService;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    resourceService = getService(FileResourceService.class, handler, context);
  }

  private Response doCreateFileResourceItem() {
    FileResourceService.ResourceRequest request = new FileResourceService.ResourceRequest();
    request.fileResource = new FileResourceItem();
    request.fileResource.setPath("/tmp/file.jar");
    request.fileResource.setName("TestFileResourceItem");

    UriInfo uriInfo = createNiceMock(UriInfo.class);
    URI uri = UriBuilder.fromUri("http://host/a/b").build();
    expect(uriInfo.getAbsolutePath()).andReturn(uri);

    HttpServletResponse resp_obj = createNiceMock(HttpServletResponse.class);

    resp_obj.setHeader(eq("Location"), anyString());

    replay(uriInfo, resp_obj);
    return resourceService.create(request, resp_obj, uriInfo);
  }

  @Test
  public void createFileResourceItem() {
    Response response = doCreateFileResourceItem();
    Assert.assertEquals(201, response.getStatus());

    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("fileResource"));
    Assert.assertNotNull(((FileResourceItem) obj.get("fileResource")).getId());
    Assert.assertFalse(((FileResourceItem) obj.get("fileResource")).getId() == null);
  }

  @Test
  public void resourceNotFound() {
    thrown.expect(NotFoundFormattedException.class);
    resourceService.getOne("4242");
  }

  @Test
  public void updateFileResourceItem() {
    Response createdFileResourceItem = doCreateFileResourceItem();
    Object createdUdfId = ((FileResourceItem) ((JSONObject) createdFileResourceItem.getEntity()).get("fileResource")).getId();

    FileResourceService.ResourceRequest request = new FileResourceService.ResourceRequest();
    request.fileResource = new FileResourceItem();
    request.fileResource.setPath("/tmp/updatedFileResourceItem.jar");
    request.fileResource.setName("TestFileResourceItem2");

    Response response = resourceService.update(request, String.valueOf(createdUdfId));
    Assert.assertEquals(204, response.getStatus());

    Response response2 = resourceService.getOne(String.valueOf(createdUdfId));
    Assert.assertEquals(200, response2.getStatus());

    JSONObject obj = ((JSONObject) response2.getEntity());
    Assert.assertTrue(obj.containsKey("fileResource"));
    Assert.assertEquals(((FileResourceItem) obj.get("fileResource")).getName(), request.fileResource.getName());
    Assert.assertEquals(((FileResourceItem) obj.get("fileResource")).getPath(), request.fileResource.getPath());
  }

  @Test
  public void deleteFileResourceItem() {
    Response createdFileResourceItem = doCreateFileResourceItem();
    Object createdUdfId = ((FileResourceItem) ((JSONObject) createdFileResourceItem.getEntity()).get("fileResource")).getId();

    Response response = resourceService.delete(String.valueOf(createdUdfId));
    Assert.assertEquals(204, response.getStatus());

    thrown.expect(NotFoundFormattedException.class);
    resourceService.getOne(String.valueOf(createdUdfId));
  }
}
