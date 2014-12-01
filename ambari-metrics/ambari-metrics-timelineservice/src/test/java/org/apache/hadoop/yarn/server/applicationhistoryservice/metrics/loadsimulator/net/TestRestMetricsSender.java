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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .loadsimulator.net;

import org.junit.Test;

import java.io.IOException;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class TestRestMetricsSender {

  @Test
  public void testPushMetrics() throws Exception {
    final UrlService svcMock = createStrictMock(UrlService.class);
    final String payload = "test";
    final String expectedResponse = "mockResponse";

    expect(svcMock.send(anyString())).andReturn(expectedResponse);
    svcMock.disconnect();
    expectLastCall();

    replay(svcMock);

    RestMetricsSender sender = new RestMetricsSender("expectedHostName") {
      @Override
      protected UrlService getConnectedUrlService() throws IOException {
        return svcMock;
      }
    };
    String response = sender.pushMetrics(payload);

    verify(svcMock);
    assertEquals("", expectedResponse, response);
  }

  @Test
  public void testPushMetricsFailed() throws Exception {
    final UrlService svcMock = createStrictMock(UrlService.class);
    final String payload = "test";
    final String expectedResponse = "mockResponse";
    RestMetricsSender sender = new RestMetricsSender("expectedHostName") {
      @Override
      protected UrlService getConnectedUrlService() throws IOException {
        return svcMock;
      }
    };

    expect(svcMock.send(anyString())).andThrow(new IOException());
    svcMock.disconnect();
    expectLastCall();

    replay(svcMock);

    String response = sender.pushMetrics(payload);

    verify(svcMock);
  }
}