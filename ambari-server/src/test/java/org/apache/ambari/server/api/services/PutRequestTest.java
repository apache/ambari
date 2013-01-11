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

package org.apache.ambari.server.api.services;

import org.junit.Test;

import static org.junit.Assert.*;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * PutRequest unit tests
 */
public class PutRequestTest extends BaseRequestTest {
  @Test
  public void testRequestType() {
    assertSame(Request.Type.PUT, new PutRequest(null, null, null, null).getRequestType());
  }

  @Test
  public void testGetQueryPredicate() throws Exception {
    String uri = "http://foo.bar.com/api/v1/clusters?foo=bar&orProp1=5|orProp2!=6|orProp3<100&prop!=5&prop2>10&prop3>=20&prop4<500&prop5<=1&fields=field1,category/field2";
    super.testGetQueryPredicate(uri);
  }

  @Test
  public void testGetFields() {
    String fields = "prop,category/prop1,category2/category3/prop2[1,2,3],prop3[4,5,6],category4[7,8,9],sub-resource/*[10,11,12],finalProp";
    super.testGetFields(fields);
  }

  protected Request getTestRequest(HttpHeaders headers, String body, UriInfo uriInfo) {
    return new PutRequest(headers, body, uriInfo, null);
  }
}
