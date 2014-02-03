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

import com.google.gson.Gson;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.sun.jersey.core.spi.factory.ResponseImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertSame;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.List;
import java.util.Collections;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ProxyServiceTest.class, ProxyService.class, URLStreamProvider.class, Response.class, ResponseBuilderImpl.class })
class ProxyServiceTest extends BaseServiceTest {

  @Test
  public void testProxyGetRequest() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    queryParams.add("url","testurl");
    InputStream is = new ByteArrayInputStream("test".getBytes());
    PowerMock.mockStatic(Response.class);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(streamProviderMock.processURL("testurl", "GET", null, APPLICATION_FORM_URLENCODED)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 3000, 1000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(is)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock);
    replay(getUriInfo(), urlConnectionMock);
    Response resultForGetRequest = ps.processGetRequestForwarding(getHttpHeaders(),getUriInfo());
    assertSame(resultForGetRequest, responseMock);
  }

  @Test
  public void testProxyPostRequest() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    queryParams.add("url","testurl");
    InputStream is = new ByteArrayInputStream("test".getBytes());
    PowerMock.mockStatic(Response.class);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(getHttpHeaders().getMediaType()).andReturn(APPLICATION_FORM_URLENCODED_TYPE);
    expect(streamProviderMock.processURL("testurl", "POST", "testbody", APPLICATION_FORM_URLENCODED)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 3000, 1000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(is)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    Response resultForPostRequest = ps.processPostRequestForwarding("testbody", getHttpHeaders(), getUriInfo());
    assertSame(resultForPostRequest, responseMock);
  }

  @Test
  public void testProxyPutRequest() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    queryParams.add("url","testurl");
    InputStream is = new ByteArrayInputStream("test".getBytes());
    PowerMock.mockStatic(Response.class);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(getHttpHeaders().getMediaType()).andReturn(APPLICATION_FORM_URLENCODED_TYPE);
    expect(streamProviderMock.processURL("testurl", "PUT", "testbody", APPLICATION_FORM_URLENCODED)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 3000, 1000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(is)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock);
    replay(getUriInfo(), urlConnectionMock, getHttpHeaders());
    Response resultForPutRequest = ps.processPutRequestForwarding("testbody", getHttpHeaders(), getUriInfo());
    assertSame(resultForPutRequest, responseMock);
  }

  @Test
  public void testProxyDeleteRequest() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    queryParams.add("url","testurl");
    InputStream is = new ByteArrayInputStream("test".getBytes());
    PowerMock.mockStatic(Response.class);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(streamProviderMock.processURL("testurl", "DELETE", null, APPLICATION_FORM_URLENCODED)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("text/plain");
    expect(urlConnectionMock.getInputStream()).andReturn(is);
    PowerMock.expectNew(URLStreamProvider.class, 3000, 1000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(is)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("text/plain")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock);
    replay(getUriInfo(), urlConnectionMock);
    Response resultForDeleteRequest = ps.processDeleteRequestForwarding(getHttpHeaders(), getUriInfo());
    assertSame(resultForDeleteRequest, responseMock);
  }

  @Test(expected = WebApplicationException.class)
  public void testResponseWithError() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add("url","testurl");
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(streamProviderMock.processURL("testurl", "GET", null, APPLICATION_FORM_URLENCODED)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(405).times(2);
    PowerMock.expectNew(URLStreamProvider.class, 3000, 1000, null, null, null).andReturn(streamProviderMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class);
    replay(getUriInfo(), urlConnectionMock);
    ps.processGetRequestForwarding(getHttpHeaders(),getUriInfo());
  }

  @Test
  public void testProxyWithJSONResponse() throws Exception {
    ProxyService ps = new ProxyService();
    URLStreamProvider streamProviderMock = PowerMock.createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnectionMock = createMock(HttpURLConnection.class);
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    Response.ResponseBuilder responseBuilderMock = PowerMock.createMock(ResponseBuilderImpl.class);
    Response responseMock = createMock(ResponseImpl.class);
    queryParams.add("url","testurl");
    Map map = new Gson().fromJson(new InputStreamReader(new ByteArrayInputStream("{ \"test\":\"test\" }".getBytes())), Map.class);
    PowerMock.mockStatic(Response.class);
    expect(getUriInfo().getQueryParameters()).andReturn(queryParams);
    expect(streamProviderMock.processURL("testurl", "GET", null, APPLICATION_FORM_URLENCODED)).andReturn(urlConnectionMock);
    expect(urlConnectionMock.getResponseCode()).andReturn(200);
    expect(urlConnectionMock.getContentType()).andReturn("application/json");
    expect(urlConnectionMock.getInputStream()).andReturn(new ByteArrayInputStream("{ \"test\":\"test\" }".getBytes()));
    PowerMock.expectNew(URLStreamProvider.class, 3000, 1000, null, null, null).andReturn(streamProviderMock);
    expect(Response.status(200)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.entity(map)).andReturn(responseBuilderMock);
    expect(responseBuilderMock.type("application/json")).andReturn(responseBuilderMock);
    expect(responseBuilderMock.build()).andReturn(responseMock);
    PowerMock.replay(streamProviderMock, URLStreamProvider.class, Response.class, responseBuilderMock);
    replay(getUriInfo(), urlConnectionMock);
    Response resultForGetRequest = ps.processGetRequestForwarding(getHttpHeaders(),getUriInfo());
    assertSame(resultForGetRequest, responseMock);
  }

  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    return Collections.emptyList();
  }

}
