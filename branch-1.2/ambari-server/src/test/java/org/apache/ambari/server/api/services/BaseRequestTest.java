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

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.PredicateCompiler;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Base tests for service requests.
 */
public abstract class BaseRequestTest {

  public void testGetQueryPredicate(String uriString) throws Exception {
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    Predicate p = createMock(Predicate.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));

    expect(uriInfo.getRequestUri()).andReturn(uri);
    expect(compiler.compile(uriString.substring(uriString.indexOf("?") + 1))).andReturn(p);

    replay(uriInfo, compiler, p);

    Request request = getTestRequest(null, null, uriInfo, compiler);

    assertEquals(p, request.getQueryPredicate());

    verify(uriInfo, compiler, p);
  }

  @Test
  public void testGetQueryPredicate_noQueryString() throws Exception {
    String uriString = "http://localhost.com:8080/api/v1/clusters";
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));

    expect(uriInfo.getRequestUri()).andReturn(uri);

    replay(uriInfo, compiler);

    Request request = getTestRequest(null, null, uriInfo, compiler);

    assertEquals(null, request.getQueryPredicate());

    verify(uriInfo, compiler);
  }

  @Test
  public void testGetQueryPredicate_invalidQuery() throws Exception {
    String uriString = "http://localhost.com:8080/api/v1/clusters?&foo|";
    PredicateCompiler compiler = createStrictMock(PredicateCompiler.class);
    UriInfo uriInfo = createMock(UriInfo.class);
    URI uri = new URI(URLEncoder.encode(uriString, "UTF-8"));

    expect(uriInfo.getRequestUri()).andReturn(uri);
    expect(compiler.compile(uriString.substring(uriString.indexOf("?") + 1))).
        andThrow(new InvalidQueryException("test"));
    replay(uriInfo, compiler);

    Request request = getTestRequest(null, null, uriInfo, compiler);

    try {
      request.getQueryPredicate();
      fail("Expected InvalidQueryException due to invalid query");
    } catch (InvalidQueryException e) {
      //expected
    }

    verify(uriInfo, compiler);
  }

  public void testGetFields(String fields) {
    UriInfo uriInfo = createMock(UriInfo.class);
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    Request request = getTestRequest(null, null, uriInfo, null);

    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams);
    expect(mapQueryParams.getFirst("fields")).andReturn(fields);

    replay(uriInfo, mapQueryParams);

    Map<String, TemporalInfo> mapFields = request.getFields();

    assertEquals(7, mapFields.size());

    String prop = "prop";
    assertTrue(mapFields.containsKey(prop));
    assertNull(mapFields.get(prop));

    String prop1 = PropertyHelper.getPropertyId("category", "prop1");
    assertTrue(mapFields.containsKey(prop1));
    assertNull(mapFields.get(prop1));

    String prop2 = PropertyHelper.getPropertyId("category2/category3", "prop2");
    assertTrue(mapFields.containsKey(prop2));
    assertEquals(new TemporalInfoImpl(1, 2, 3), mapFields.get(prop2));

    String prop3 = "prop3";
    assertTrue(mapFields.containsKey(prop3));
    assertEquals(new TemporalInfoImpl(4, 5, 6), mapFields.get(prop3));

    String category4 = "category4";
    assertTrue(mapFields.containsKey(category4));
    assertEquals(new TemporalInfoImpl(7, 8, 9), mapFields.get(category4));

    String subResource = PropertyHelper.getPropertyId("sub-resource", "*");
    assertTrue(mapFields.containsKey(subResource));
    assertEquals(new TemporalInfoImpl(10, 11, 12), mapFields.get(subResource));

    String finalProp = "finalProp";
    assertTrue(mapFields.containsKey(finalProp));
    assertNull(mapFields.get(finalProp));

    verify(uriInfo, mapQueryParams);
  }

   protected abstract Request getTestRequest(HttpHeaders headers, String body,
                                             UriInfo uriInfo, PredicateCompiler compiler);
}
