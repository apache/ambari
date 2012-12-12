package org.apache.ambari.server.api.handlers;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertSame;

/**
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 9/12/12
 * Time: 12:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class DelegatingRequestHandlerTest {

  @Test
  public void testHandleRequest_GET() {
    Request request = createStrictMock(Request.class);
    RequestHandlerFactory factory = createStrictMock(RequestHandlerFactory.class);
    RequestHandler readRequestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    ResultPostProcessor resultProcessor = createStrictMock(ResultPostProcessor.class);

    // expectations
    expect(request.getRequestType()).andReturn(Request.Type.GET);
    expect(factory.getRequestHandler(Request.Type.GET)).andReturn(readRequestHandler);
    expect(readRequestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultPostProcessor()).andReturn(resultProcessor);
    resultProcessor.process(result);

    replay(request, factory, readRequestHandler, result, resultProcessor);

    RequestHandler delegatingRequestHandler = new TestDelegatingRequestHandler(factory);

    assertSame(result, delegatingRequestHandler.handleRequest(request));
    verify(request, factory, readRequestHandler, result, resultProcessor);
  }

  @Test
  public void testHandleRequest_PUT() {
    Request request = createStrictMock(Request.class);
    RequestHandlerFactory factory = createStrictMock(RequestHandlerFactory.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    ResultPostProcessor resultProcessor = createStrictMock(ResultPostProcessor.class);

    // expectations
    expect(request.getRequestType()).andReturn(Request.Type.POST);
    expect(factory.getRequestHandler(Request.Type.POST)).andReturn(requestHandler);
    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultPostProcessor()).andReturn(resultProcessor);
    resultProcessor.process(result);

    replay(request, factory, requestHandler, result, resultProcessor);

    RequestHandler delegatingRequestHandler = new TestDelegatingRequestHandler(factory);

    assertSame(result, delegatingRequestHandler.handleRequest(request));
    verify(request, factory, requestHandler, result, resultProcessor);
  }

  @Test
  public void testHandleRequest_POST() {
    Request request = createStrictMock(Request.class);
    RequestHandlerFactory factory = createStrictMock(RequestHandlerFactory.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    ResultPostProcessor resultProcessor = createStrictMock(ResultPostProcessor.class);

    // expectations
    expect(request.getRequestType()).andReturn(Request.Type.PUT);
    expect(factory.getRequestHandler(Request.Type.PUT)).andReturn(requestHandler);
    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultPostProcessor()).andReturn(resultProcessor);
    resultProcessor.process(result);

    replay(request, factory, requestHandler, result, resultProcessor);

    RequestHandler delegatingRequestHandler = new TestDelegatingRequestHandler(factory);

    assertSame(result, delegatingRequestHandler.handleRequest(request));
    verify(request, factory, requestHandler, result, resultProcessor);
  }

  @Test
  public void testHandleRequest_DELETE() {
    Request request = createStrictMock(Request.class);
    RequestHandlerFactory factory = createStrictMock(RequestHandlerFactory.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    ResultPostProcessor resultProcessor = createStrictMock(ResultPostProcessor.class);

    // expectations
    expect(request.getRequestType()).andReturn(Request.Type.DELETE);
    expect(factory.getRequestHandler(Request.Type.DELETE)).andReturn(requestHandler);
    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultPostProcessor()).andReturn(resultProcessor);
    resultProcessor.process(result);

    replay(request, factory, requestHandler, result, resultProcessor);

    RequestHandler delegatingRequestHandler = new TestDelegatingRequestHandler(factory);

    assertSame(result, delegatingRequestHandler.handleRequest(request));
    verify(request, factory, requestHandler, result, resultProcessor);
  }

  private class TestDelegatingRequestHandler extends DelegatingRequestHandler {
    private RequestHandlerFactory m_factory;

    private TestDelegatingRequestHandler(RequestHandlerFactory factory) {
      m_factory = factory;
    }

    @Override
    public RequestHandlerFactory getRequestHandlerFactory() {
      return m_factory;
    }
  }
}
