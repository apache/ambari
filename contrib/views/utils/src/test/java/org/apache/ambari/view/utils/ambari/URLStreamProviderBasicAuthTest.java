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

package org.apache.ambari.view.utils.ambari;

import org.apache.ambari.view.URLStreamProvider;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class URLStreamProviderBasicAuthTest {

  @Test
  public void testReadFrom() throws Exception {
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);
    expect(urlStreamProvider.readFrom(anyString(), anyString(), anyString(), HeadersMatcher.mapContainsAuthHeader())).andReturn(null);
    URLStreamProviderBasicAuth urlStreamProviderBasicAuth =
        new URLStreamProviderBasicAuth(urlStreamProvider, "user", "pass");

    replay(urlStreamProvider);

    urlStreamProviderBasicAuth.readFrom("http://example.com", "GET",
        (String) null, null);
    urlStreamProviderBasicAuth.readFrom("http://example.com", "GET",
        (String)null, new HashMap<String, String>());
  }

  @Test
  public void testReadFrom1() throws Exception {
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);
    expect(urlStreamProvider.readFrom(anyString(), anyString(), (InputStream)anyObject(),
        HeadersMatcher.mapContainsAuthHeader())).andReturn(null);
    URLStreamProviderBasicAuth urlStreamProviderBasicAuth =
        new URLStreamProviderBasicAuth(urlStreamProvider, "user", "pass");

    replay(urlStreamProvider);

    urlStreamProviderBasicAuth.readFrom("http://example.com", "GET",
        (InputStream) null, null);
    urlStreamProviderBasicAuth.readFrom("http://example.com", "GET",
        (InputStream)null, new HashMap<String, String>());
  }

  @Test
  public void testReadAs() throws Exception {
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);
    expect(urlStreamProvider.readAs(anyString(), anyString(), anyString(),
        HeadersMatcher.mapContainsAuthHeader(), anyString())).andReturn(null);
    URLStreamProviderBasicAuth urlStreamProviderBasicAuth =
        new URLStreamProviderBasicAuth(urlStreamProvider, "user", "pass");

    replay(urlStreamProvider);

    urlStreamProviderBasicAuth.readAs("http://example.com", "GET",
        (String) null, null, "admin");
    urlStreamProviderBasicAuth.readAs("http://example.com", "GET",
        (String) null, new HashMap<String, String>(), "admin");
  }

  @Test
  public void testReadAs1() throws Exception {
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);
    expect(urlStreamProvider.readAs(anyString(), anyString(), (InputStream) anyObject(),
        HeadersMatcher.mapContainsAuthHeader(), anyString())).andReturn(null);
    URLStreamProviderBasicAuth urlStreamProviderBasicAuth =
        new URLStreamProviderBasicAuth(urlStreamProvider, "user", "pass");

    replay(urlStreamProvider);

    urlStreamProviderBasicAuth.readAs("http://example.com", "GET",
        (InputStream) null, null, "admin");
    urlStreamProviderBasicAuth.readAs("http://example.com", "GET",
        (InputStream) null, new HashMap<String, String>(), "admin");
  }

  @Test
  public void testReadAsCurrent() throws Exception {
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);
    expect(urlStreamProvider.readAsCurrent(anyString(), anyString(), anyString(),
        HeadersMatcher.mapContainsAuthHeader())).andReturn(null);
    URLStreamProviderBasicAuth urlStreamProviderBasicAuth =
        new URLStreamProviderBasicAuth(urlStreamProvider, "user", "pass");

    replay(urlStreamProvider);

    urlStreamProviderBasicAuth.readAsCurrent("http://example.com", "GET",
        (String) null, null);
    urlStreamProviderBasicAuth.readAsCurrent("http://example.com", "GET",
        (String) null, new HashMap<String, String>());
  }

  @Test
  public void testReadAsCurrent1() throws Exception {
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);
    expect(urlStreamProvider.readAsCurrent(anyString(), anyString(), (InputStream) anyObject(),
        HeadersMatcher.mapContainsAuthHeader())).andReturn(null);
    URLStreamProviderBasicAuth urlStreamProviderBasicAuth =
        new URLStreamProviderBasicAuth(urlStreamProvider, "user", "pass");

    replay(urlStreamProvider);

    urlStreamProviderBasicAuth.readAsCurrent("http://example.com", "GET",
        (InputStream) null, null);
    urlStreamProviderBasicAuth.readAsCurrent("http://example.com", "GET",
        (InputStream)null, new HashMap<String, String>());
  }


  public static class HeadersMatcher implements IArgumentMatcher {

    public static Map<String, String> mapContainsAuthHeader() {
      EasyMock.reportMatcher(new HeadersMatcher());
      return null;
    }

    public void appendTo(StringBuffer buffer) {
      buffer.append("Authentication header matcher");
    }

    public boolean matches(Object headers) {
      if (!(headers instanceof Map)) {
        return false;
      }

      Map<String, String> headersMap = (Map<String, String>) headers;

      if (!headersMap.containsKey("Authorization"))
        return false;
      String authHeader = headersMap.get("Authorization");

      if (!authHeader.startsWith("Basic "))
        return false;

      return true;
    }
  }
}