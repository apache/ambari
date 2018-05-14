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

package org.apache.ambari.view.tez.utils;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.tez.exceptions.TezWebAppException;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class ProxyHelperTest {
  @Test
  public void shouldBuildURLWithNoQueryParameters() throws Exception {
    ViewContext context = createNiceMock(ViewContext.class);
    ProxyHelper helper = new ProxyHelper(context);
    assertEquals("http://abc.com/", helper.getProxyUrl("http://abc.com", "", new MultivaluedHashMap<String, String>(), "kerberos"));
    assertEquals("http://abc.com/test/abcd", helper.getProxyUrl("http://abc.com", "test/abcd", new MultivaluedHashMap<String, String>(), "kerberos"));
  }

  @Test
  public void shouldBuildURLWithQueryParametersHavingSpecialCharacters() throws Exception {
    ViewContext context = createNiceMock(ViewContext.class);
    ProxyHelper helper = new ProxyHelper(context);
    MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
    map.putSingle("data", "abcd/efgh");
    assertEquals("http://abc.com/test/abcd?data=abcd%2Fefgh", helper.getProxyUrl("http://abc.com", "test/abcd", map, "kerberos"));
    map.putSingle("data", "abcd efgh");
    assertEquals("http://abc.com/test/abcd?data=abcd+efgh", helper.getProxyUrl("http://abc.com", "test/abcd", map, "kerberos"));
  }

  @Test(expected = TezWebAppException.class)
  public void shouldThrowExceptionIfWrongUrl() throws Exception {
    ViewContext context = createNiceMock(ViewContext.class);
    ProxyHelper helper = new ProxyHelper(context);
    helper.getProxyUrl("####", "", new MultivaluedHashMap<String, String>(), "kerberos");
  }

  @Test
  public void shouldAddUserIfAuthTypeIsSimple() throws Exception {
    ViewContext context = createNiceMock(ViewContext.class);
    expect(context.getUsername()).andReturn("admin").anyTimes();
    ProxyHelper helper = new ProxyHelper(context);
    MultivaluedMap<String, String> map = new MultivaluedHashMap<>();

    replay(context);
    assertEquals("http://abc.com/test/abcd?user.name=admin", helper.getProxyUrl("http://abc.com", "test/abcd", map, "simple"));
    assertEquals("http://abc.com/test/abcd?user.name=admin", helper.getProxyUrl("http://abc.com", "test/abcd", map, null));
  }
}