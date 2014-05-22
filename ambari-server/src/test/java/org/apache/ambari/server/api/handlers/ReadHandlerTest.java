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
import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.controller.spi.*;
import org.easymock.Capture;
import org.junit.Test;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for ReadHandler.
 */
public class ReadHandlerTest {

  @Test
  public void testHandleRequest__InvalidField() {
    Request request = createNiceMock(Request.class);
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    Query query = createStrictMock(Query.class);

    Map<String, TemporalInfo> mapPartialResponseFields = new HashMap<String, TemporalInfo>();
    mapPartialResponseFields.put("foo/bar", null);
    Renderer renderer = new DefaultRenderer();

    expect(request.getResource()).andReturn(resource);
    expect(request.getFields()).andReturn(mapPartialResponseFields);
    expect(request.getRenderer()).andReturn(renderer);
    expect(resource.getQuery()).andReturn(query);

    query.setPageRequest(null);
    query.setSortRequest(null);
    query.setRenderer(renderer);
    query.addProperty("foo/bar", null);
    expectLastCall().andThrow(new IllegalArgumentException("testMsg"));

    replay(request, resource, query);

    ReadHandler handler = new ReadHandler();
    Result result = handler.handleRequest(request);

    assertEquals(ResultStatus.STATUS.BAD_REQUEST, result.getStatus().getStatus());
    assertEquals("testMsg", result.getStatus().getMessage());

    verify(request, resource, query);
  }

  @Test
  public void testHandleRequest__OK() throws Exception {
    Request request = createStrictMock(Request.class);
    ResourceInstance resource = createStrictMock(ResourceInstance.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);
    Result result = createStrictMock(Result.class);
    Renderer renderer = new DefaultRenderer();
    Capture<ResultStatus> resultStatusCapture = new Capture<ResultStatus>();

    Map<String, TemporalInfo> mapPartialResponseFields = new HashMap<String, TemporalInfo>();
    mapPartialResponseFields.put("foo", null);
    mapPartialResponseFields.put("bar/c", null);
    mapPartialResponseFields.put("bar/d/e", null);
    mapPartialResponseFields.put("category/", null);
    //expectations
    expect(request.getResource()).andReturn(resource);
    expect(resource.getQuery()).andReturn(query);

    expect(request.getPageRequest()).andReturn(null);
    expect(request.getSortRequest()).andReturn(null);
    expect(request.getRenderer()).andReturn(renderer);
    expect(request.getFields()).andReturn(mapPartialResponseFields);

    query.addProperty("foo", null);
    query.addProperty("bar/c", null);
    query.addProperty("bar/d/e", null);
    query.addProperty("category/", null);

    expect(request.getQueryPredicate()).andReturn(predicate);
    query.setUserPredicate(predicate);
    query.setPageRequest(null);
    query.setSortRequest(null);
    query.setRenderer(renderer);
    expect(query.execute()).andReturn(result);
    result.setResultStatus(capture(resultStatusCapture));

    replay(request, resource, query, predicate, result);

    //test
    ReadHandler handler = new ReadHandler();
    assertSame(result, handler.handleRequest(request));
    assertEquals(ResultStatus.STATUS.OK, resultStatusCapture.getValue().getStatus());
    verify(request, resource, query, predicate, result);
  }

  @Test
  public void testHandleRequest__SystemException() throws Exception {
    Request request = createStrictMock(Request.class);
    ResourceInstance resource = createStrictMock(ResourceInstance.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);
    Renderer renderer = new DefaultRenderer();

    expect(request.getResource()).andReturn(resource);
    expect(resource.getQuery()).andReturn(query);

    expect(request.getPageRequest()).andReturn(null);
    expect(request.getSortRequest()).andReturn(null);
    expect(request.getRenderer()).andReturn(renderer);
    expect(request.getFields()).andReturn(Collections.<String, TemporalInfo>emptyMap());

    expect(request.getQueryPredicate()).andReturn(predicate);
    query.setUserPredicate(predicate);
    query.setPageRequest(null);
    query.setSortRequest(null);
    query.setRenderer(renderer);
    SystemException systemException = new SystemException("testMsg", new RuntimeException());
    expect(query.execute()).andThrow(systemException);

    replay(request, resource, query, predicate);

    //test
    ReadHandler handler = new ReadHandler();
    Result result = handler.handleRequest(request);
    assertEquals(ResultStatus.STATUS.SERVER_ERROR, result.getStatus().getStatus());
    assertEquals(systemException.toString(), result.getStatus().getMessage());
    verify(request, resource, query, predicate);
  }

  @Test
  public void testHandleRequest__NoSuchParentResourceException() throws Exception {
    Request request = createStrictMock(Request.class);
    ResourceInstance resource = createStrictMock(ResourceInstance.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);
    NoSuchParentResourceException exception = new NoSuchParentResourceException("exceptionMsg", new RuntimeException());
    Renderer renderer = new DefaultRenderer();

    expect(request.getResource()).andReturn(resource);
    expect(resource.getQuery()).andReturn(query);

    expect(request.getPageRequest()).andReturn(null);
    expect(request.getSortRequest()).andReturn(null);
    expect(request.getRenderer()).andReturn(renderer);
    expect(request.getFields()).andReturn(Collections.<String, TemporalInfo>emptyMap());

    expect(request.getQueryPredicate()).andReturn(predicate);
    query.setUserPredicate(predicate);
    query.setPageRequest(null);
    query.setSortRequest(null);
    query.setRenderer(renderer);

    expect(query.execute()).andThrow(exception);

    replay(request, resource, query, predicate);

    //test
    ReadHandler handler = new ReadHandler();
    Result result = handler.handleRequest(request);
    assertEquals(ResultStatus.STATUS.NOT_FOUND, result.getStatus().getStatus());
    assertEquals("exceptionMsg", result.getStatus().getMessage());
    verify(request, resource, query, predicate);
  }

  @Test
  public void testHandleRequest__UnsupportedPropertyException() throws Exception {
    Request request = createStrictMock(Request.class);
    ResourceInstance resource = createStrictMock(ResourceInstance.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);
    Renderer renderer = new DefaultRenderer();
    UnsupportedPropertyException exception = new UnsupportedPropertyException(
        Resource.Type.Cluster, Collections.singleton("foo"));

    expect(request.getResource()).andReturn(resource);
    expect(resource.getQuery()).andReturn(query);

    expect(request.getPageRequest()).andReturn(null);
    expect(request.getSortRequest()).andReturn(null);
    expect(request.getRenderer()).andReturn(renderer);
    expect(request.getFields()).andReturn(Collections.<String, TemporalInfo>emptyMap());

    expect(request.getQueryPredicate()).andReturn(predicate);
    query.setUserPredicate(predicate);
    query.setPageRequest(null);
    query.setSortRequest(null);
    query.setRenderer(renderer);

    expect(query.execute()).andThrow(exception);

    replay(request, resource, query, predicate);

    //test
    ReadHandler handler = new ReadHandler();
    Result result = handler.handleRequest(request);
    assertEquals(ResultStatus.STATUS.BAD_REQUEST, result.getStatus().getStatus());
    assertEquals(exception.getMessage(), result.getStatus().getMessage());
    verify(request, resource, query, predicate);
  }

  @Test
  public void testHandleRequest__NoSuchResourceException_OK() throws Exception {
    Request request = createStrictMock(Request.class);
    ResourceInstance resource = createStrictMock(ResourceInstance.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);
    NoSuchResourceException exception = new NoSuchResourceException("msg", new RuntimeException());
    Renderer renderer = new DefaultRenderer();

    expect(request.getResource()).andReturn(resource);
    expect(resource.getQuery()).andReturn(query);

    expect(request.getPageRequest()).andReturn(null);
    expect(request.getSortRequest()).andReturn(null);
    expect(request.getRenderer()).andReturn(renderer);
    expect(request.getFields()).andReturn(Collections.<String, TemporalInfo>emptyMap());

    expect(request.getQueryPredicate()).andReturn(predicate).anyTimes();
    query.setUserPredicate(predicate);
    query.setPageRequest(null);
    query.setSortRequest(null);
    query.setRenderer(renderer);

    expect(query.execute()).andThrow(exception);

    replay(request, resource, query, predicate);

    //test
    ReadHandler handler = new ReadHandler();
    Result result = handler.handleRequest(request);
    // ok because this is a query that returned no rows
    assertEquals(ResultStatus.STATUS.OK, result.getStatus().getStatus());
    verify(request, resource, query, predicate);
  }

  @Test
  public void testHandleRequest__NoSuchResourceException_NOT_FOUND() throws Exception {
    Request request = createStrictMock(Request.class);
    ResourceInstance resource = createStrictMock(ResourceInstance.class);
    Query query = createMock(Query.class);
    NoSuchResourceException exception = new NoSuchResourceException("msg", new RuntimeException());
    Renderer renderer = new DefaultRenderer();

    expect(request.getResource()).andReturn(resource);
    expect(resource.getQuery()).andReturn(query);

    expect(request.getPageRequest()).andReturn(null);
    expect(request.getSortRequest()).andReturn(null);
    expect(request.getRenderer()).andReturn(renderer);
    expect(request.getFields()).andReturn(Collections.<String, TemporalInfo>emptyMap());

    expect(request.getQueryPredicate()).andReturn(null).anyTimes();
    query.setUserPredicate(null);
    query.setPageRequest(null);
    query.setSortRequest(null);
    query.setRenderer(renderer);

    expect(query.execute()).andThrow(exception);

    replay(request, resource, query);

    //test
    ReadHandler handler = new ReadHandler();
    Result result = handler.handleRequest(request);
    // not a query, so not found
    assertEquals(ResultStatus.STATUS.NOT_FOUND, result.getStatus().getStatus());
    assertEquals(exception.getMessage(), result.getStatus().getMessage());
    verify(request, resource, query);
  }
}
