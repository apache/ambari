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

package org.apache.ambari.api.handlers;

import org.apache.ambari.api.query.Query;
import org.apache.ambari.api.resources.ResourceDefinition;
import org.apache.ambari.api.services.Request;
import org.apache.ambari.api.services.Result;
import org.junit.Test;


import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for ReadHandler.
 */
public class ReadHandlerTest {

  @Test
  public void testHandlerRequest() throws Exception {
    Request request = createStrictMock(Request.class);
    ResourceDefinition resourceDefinition = createStrictMock(ResourceDefinition.class);
    Query query = createMock(Query.class);
    Result result = createStrictMock(Result.class);

    Set<String> setPartialResponseFields = new HashSet<String>();
    setPartialResponseFields.add("foo");
    setPartialResponseFields.add("bar/c");
    setPartialResponseFields.add("bar/d/e");

    //expectations
    expect(request.getResourceDefinition()).andReturn(resourceDefinition);
    expect(resourceDefinition.getQuery()).andReturn(query);

    expect(request.getPartialResponseFields()).andReturn(setPartialResponseFields);
    query.addProperty(null, "foo");
    query.addProperty("bar", "c");
    query.addProperty("bar/d", "e");
    expect(query.execute()).andReturn(result);

    replay(request, resourceDefinition, query, result);

    //test
    ReadHandler handler = new ReadHandler();
    assertSame(result, handler.handleRequest(request));

    verify(request, resourceDefinition, query, result);

  }
}
