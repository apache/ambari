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

import org.apache.ambari.server.controller.internal.AppCookieManager;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class ViewURLStreamProviderTest {

  @Test
  public void testReadFrom() throws Exception {

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection urlConnection = createNiceMock(HttpURLConnection.class);
    InputStream inputStream = createNiceMock(InputStream.class);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("header", "headerValue");

    Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    headerMap.put("header", Collections.singletonList("headerValue"));

    expect(streamProvider.processURL("spec", "requestMethod", "params", headerMap)).andReturn(urlConnection);
    expect(urlConnection.getInputStream()).andReturn(inputStream);

    replay(streamProvider, urlConnection, inputStream);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(streamProvider);

    Assert.assertEquals(inputStream, viewURLStreamProvider.readFrom("spec", "requestMethod", "params", headers));

    verify(streamProvider, urlConnection, inputStream);
  }

  @Test
  public void testGetAppCookieManager() throws Exception {
    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    AppCookieManager appCookieManager = createNiceMock(AppCookieManager.class);

    expect(streamProvider.getAppCookieManager()).andReturn(appCookieManager);

    replay(streamProvider, appCookieManager);

    ViewURLStreamProvider viewURLStreamProvider = new ViewURLStreamProvider(streamProvider);

    Assert.assertEquals(appCookieManager, viewURLStreamProvider.getAppCookieManager());

    verify(streamProvider, appCookieManager);
  }
}
