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

package org.apache.ambari.view.hive.resources.udfs;

import org.apache.ambari.view.hive.BaseHiveTest;
import org.apache.ambari.view.hive.resources.udfs.UDF;
import org.apache.ambari.view.hive.resources.udfs.UDFService;
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

public class UDFServiceTest extends BaseHiveTest {
  @Rule public ExpectedException thrown = ExpectedException.none();
  private UDFService udfService;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    udfService = getService(UDFService.class, handler, context);
  }

  private Response doCreateUDF() {
    UDFService.UDFRequest request = new UDFService.UDFRequest();
    request.udf = new UDF();
    request.udf.setClassname("/tmp/udf.jar");
    request.udf.setName("TestUDF");

    UriInfo uriInfo = createNiceMock(UriInfo.class);
    URI uri = UriBuilder.fromUri("http://host/a/b").build();
    expect(uriInfo.getAbsolutePath()).andReturn(uri);

    HttpServletResponse resp_obj = createNiceMock(HttpServletResponse.class);

    resp_obj.setHeader(eq("Location"), anyString());

    replay(uriInfo, resp_obj);
    return udfService.create(request, resp_obj, uriInfo);
  }

  @Test
  public void createUDF() {
    Response response = doCreateUDF();
    Assert.assertEquals(201, response.getStatus());

    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("udf"));
    Assert.assertNotNull(((UDF) obj.get("udf")).getId());
    Assert.assertFalse(((UDF) obj.get("udf")).getId() == null);
  }

  @Test
  public void udfNotFound() {
    thrown.expect(NotFoundFormattedException.class);
    udfService.getOne("4242");
  }

  @Test
  public void updateUDF() {
    Response createdUDF = doCreateUDF();
    Object createdUdfId = ((UDF) ((JSONObject) createdUDF.getEntity()).get("udf")).getId();

    UDFService.UDFRequest request = new UDFService.UDFRequest();
    request.udf = new UDF();
    request.udf.setClassname("/tmp/updatedUDF.jar");
    request.udf.setName("TestUDF2");

    Response response = udfService.update(request, String.valueOf(createdUdfId));
    Assert.assertEquals(204, response.getStatus());

    Response response2 = udfService.getOne(String.valueOf(createdUdfId));
    Assert.assertEquals(200, response2.getStatus());

    JSONObject obj = ((JSONObject) response2.getEntity());
    Assert.assertTrue(obj.containsKey("udf"));
    Assert.assertEquals(((UDF) obj.get("udf")).getName(), request.udf.getName());
    Assert.assertEquals(((UDF) obj.get("udf")).getClassname(), request.udf.getClassname());
  }

  @Test
  public void deleteUDF() {
    Response createdUDF = doCreateUDF();
    Object createdUdfId = ((UDF) ((JSONObject) createdUDF.getEntity()).get("udf")).getId();

    Response response = udfService.delete(String.valueOf(createdUdfId));
    Assert.assertEquals(204, response.getStatus());

    thrown.expect(NotFoundFormattedException.class);
    udfService.getOne(String.valueOf(createdUdfId));
  }
}
