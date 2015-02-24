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

package org.apache.ambari.view.hive;

import org.junit.Assert;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.net.URI;

import static org.easymock.EasyMock.*;

public class ServiceTestUtils {
  public static void assertHTTPResponseOK(Response response) {
    Assert.assertEquals(200, response.getStatus());
  }

  public static void assertHTTPResponseCreated(Response response) {
    Assert.assertEquals(201, response.getStatus());
  }

  public static void assertHTTPResponseNoContent(Response response) {
    Assert.assertEquals(204, response.getStatus());
  }

  public static void expectLocationHeaderInResponse(HttpServletResponse resp_obj) {
    resp_obj.setHeader(eq("Location"), anyString());
  }

  public static UriInfo getDefaultUriInfo() {
    UriInfo uriInfo = createNiceMock(UriInfo.class);
    URI uri = UriBuilder.fromUri("http://host/a/b").build();
    expect(uriInfo.getAbsolutePath()).andReturn(uri);
    replay(uriInfo);
    return uriInfo;
  }

  public static HttpServletResponse getResponseWithLocation() {
    HttpServletResponse resp_obj = createNiceMock(HttpServletResponse.class);
    expectLocationHeaderInResponse(resp_obj);
    replay(resp_obj);
    return resp_obj;
  }
}
