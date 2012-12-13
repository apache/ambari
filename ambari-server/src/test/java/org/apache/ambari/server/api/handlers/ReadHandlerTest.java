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

package org.apache.ambari.server.api.handlers;

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.junit.Test;


import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for ReadHandler.
 */
public class ReadHandlerTest {

  @Test
  public void testHandlerRequest() throws Exception {
    Request request = createStrictMock(Request.class);
    ResourceInstance resource = createStrictMock(ResourceInstance.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);
    Result result = createStrictMock(Result.class);

    Map<String, TemporalInfo> mapPartialResponseFields = new HashMap<String, TemporalInfo>();
    mapPartialResponseFields.put("foo", null);
    mapPartialResponseFields.put("bar/c", null);
    mapPartialResponseFields.put("bar/d/e", null);
    //expectations
    expect(request.getResource()).andReturn(resource);
    expect(resource.getQuery()).andReturn(query);

    expect(request.getFields()).andReturn(mapPartialResponseFields);
    query.addProperty(null, "foo", null);
    query.addProperty("bar", "c", null);
    query.addProperty("bar/d", "e", null);

    expect(request.getQueryPredicate()).andReturn(predicate);
    query.setUserPredicate(predicate);
    expect(query.execute()).andReturn(result);

    replay(request, resource, query, predicate, result);

    //test
    ReadHandler handler = new ReadHandler();
    assertSame(result, handler.handleRequest(request));

    verify(request, resource, query, predicate, result);

  }
}
