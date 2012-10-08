package org.apache.ambari.api.handlers;

import org.apache.ambari.api.query.Query;
import org.apache.ambari.api.resource.ResourceDefinition;
import org.apache.ambari.api.services.Request;
import org.apache.ambari.api.services.Result;
import org.junit.Test;


import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertSame;

/**
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 9/12/12
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
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
