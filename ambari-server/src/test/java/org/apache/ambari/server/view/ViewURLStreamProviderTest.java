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

package org.apache.ambari.server.view;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class ViewURLStreamProviderTest {

  @Test
  public void testReadFrom() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadFromNullBody() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq((byte[]) null), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", (String) null, headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadAs() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readAs("spec", "requestMethod", "params", headers, "joe"));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadAsCurrent() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);
    expect(viewContext.getUsername()).andReturn("joe").anyTimes();

    replay(streamProvider, urlConnection, inputStream, viewContext);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readAsCurrent("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection, inputStream, viewContext);
  }

  @Test
  public void testReadFromInputStream() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    InputStream body = new ByteArrayInputStream("params".getBytes());

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", body, headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadFromNullInputStreamBody() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq((byte[]) null), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", (InputStream) null, headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadAsInputStream() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    InputStream body = new ByteArrayInputStream("params".getBytes());

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readAs("spec", "requestMethod", body, headers, "joe"));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testReadAsCurrentInputStream() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    InputStream body = new ByteArrayInputStream("params".getBytes());

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);
    expect(viewContext.getUsername()).andReturn("joe").anyTimes();

    replay(streamProvider, urlConnection, inputStream, viewContext);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readAsCurrent("spec", "requestMethod", body, headers));

    verify(streamProvider, urlConnection, inputStream, viewContext);
  }

  @Test
  public void testGetConnection() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);

    replay(streamProvider, urlConnection);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(urlConnection, viewURLStreamProvider.getConnection("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection);
  }

  @Test
  public void testGetConnectionAs() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);

    replay(streamProvider, urlConnection);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(urlConnection, viewURLStreamProvider.getConnectionAs("spec", "requestMethod", "params", headers, "joe"));

    verify(streamProvider, urlConnection);
  }

  @Test
  public void testGetConnectionCurrent() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    ViewContext viewContext = createNiceMock(ViewContext.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL(eq("spec?doAs=joe"), eq("requestMethod"), aryEq("params".getBytes()), eq(headerMap))).andReturn(urlConnection);
    expect(viewContext.getUsername()).andReturn("joe").anyTimes();

    replay(streamProvider, urlConnection, viewContext);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(viewContext, streamProvider);

    Assert.assertEquals(urlConnection, viewURLStreamProvider.getConnectionAsCurrent("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection, viewContext);
  }
}
