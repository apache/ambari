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

package org.apache.ambari.server.controller;

import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;

/**
 * Tests the {@link FailsafeServletResponse} class
 */
public class FailsafeServletResponseTest {
  @Test
  public void testRequestFailure() throws Exception {
    final HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    final FailsafeServletResponse responseWrapper = new FailsafeServletResponse(response);
    final IAnswer<Void> answer = new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        Assert.fail("Original response should not commit errors");
        return null;
      }
    };
    response.sendError(0);
    EasyMock.expectLastCall().andAnswer(answer).anyTimes();
    response.sendError(0, "");
    EasyMock.expectLastCall().andAnswer(answer).anyTimes();
    EasyMock.replay(response);
    responseWrapper.sendError(0);
    responseWrapper.sendError(0, "");
    Assert.assertTrue(responseWrapper.isRequestFailed());
    EasyMock.verify(response);
  }
}
