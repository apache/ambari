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

import junit.framework.TestCase;
import org.apache.ambari.view.ImpersonatorSetting;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.server.controller.internal.AppCookieManager;
import org.junit.Assert;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;


public class HttpImpersonatorImplTest extends TestCase {

  String cookie;
  String username;
  ViewContext viewContext;
  HttpImpersonatorImpl impersonator;
  String expectedResult;

  public void setUp() throws Exception {
    String uuid = UUID.randomUUID().toString().replace("-", "");
    this.cookie = uuid;
    this.username = "admin" + uuid;

    AppCookieManager mockAppCookieManager = Mockito.mock(AppCookieManager.class);
    when(mockAppCookieManager.getAppCookie(anyString(), anyBoolean())).thenReturn(cookie);

    this.expectedResult = "Dummy text from HTTP response";
    BufferedReader mockBufferedReader = Mockito.mock(BufferedReader.class);
    when(mockBufferedReader.readLine()).thenReturn(expectedResult).thenReturn(null);

    HttpImpersonatorImpl.FactoryHelper mockFactory = Mockito.mock(HttpImpersonatorImpl.FactoryHelper.class);
    when(mockFactory.makeBR(any(InputStreamReader.class))).thenReturn(mockBufferedReader);

    this.viewContext = Mockito.mock(ViewContext.class);
    when(this.viewContext.getUsername()).thenReturn(username);

    this.impersonator = new HttpImpersonatorImpl(this.viewContext, mockAppCookieManager, mockFactory);
    when(this.viewContext.getHttpImpersonator()).thenReturn(this.impersonator);
  }

  @org.junit.Test
  public void testBasic() throws Exception {
    String urlToRead = "http://foo.com";
    String requestMethod = "GET";
    URL url = new URL(urlToRead);

    // Test default params
    HttpURLConnection conn1 = (HttpURLConnection) url.openConnection();

    conn1 = this.viewContext.getHttpImpersonator().doAs(conn1, requestMethod);
    Assert.assertEquals(requestMethod, conn1.getRequestMethod());
    Assert.assertEquals(username, conn1.getRequestProperty("doAs"));

    // Test specific params
    HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
    conn2 = this.viewContext.getHttpImpersonator().doAs(conn1, requestMethod, "admin", "username");
    Assert.assertEquals(requestMethod, conn1.getRequestMethod());
    Assert.assertEquals("admin", conn1.getRequestProperty("username"));
  }

  @org.junit.Test
  public void testRequestURL() throws Exception {
    String urlToRead = "http://foo.com";
    String requestMethod = "GET";

    // Test default params
    ImpersonatorSetting impersonatorSetting = new ImpersonatorSettingImpl(this.viewContext);
    when(this.viewContext.getImpersonatorSetting()).thenReturn(impersonatorSetting);
    String actualResult = this.viewContext.getHttpImpersonator().requestURL(urlToRead, requestMethod, impersonatorSetting);
    Assert.assertEquals(this.expectedResult, actualResult);
  }

  @org.junit.Test
  public void testRequestURLWithCustom() throws Exception {
    String urlToRead = "http://foo.com";
    String requestMethod = "GET";

    // Test custom params
    ImpersonatorSetting impersonatorSetting = new ImpersonatorSettingImpl("hive", "impersonate");
    when(this.viewContext.getImpersonatorSetting()).thenReturn(impersonatorSetting);
    String actualResult = this.viewContext.getHttpImpersonator().requestURL(urlToRead, requestMethod, impersonatorSetting);
    Assert.assertEquals(this.expectedResult, actualResult);
  }

  @org.junit.Test
  public void testInvalidURL() throws Exception {
    String urlToRead = "http://foo.com?" + "doAs" + "=hive";
    String requestMethod = "GET";
    URL url = new URL(urlToRead);

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    try {
      conn = this.viewContext.getHttpImpersonator().doAs(conn, requestMethod);
      fail("Expected an exception to be thrown." );
    } catch(Exception e) {
      ;
    }
  }
}
