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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.junit.Test;

/**
 * Tests the {@link FailsafeHandlerList} class
 */
public class FailsafeHandlerListTest {
  @Test
  public void testHandleWithFailures() throws Exception {
    final FailsafeHandlerList handlerList = EasyMock
        .createMockBuilder(FailsafeHandlerList.class).withConstructor()
        .addMockedMethod("isStarted").createMock();
    EasyMock.expect(handlerList.isStarted()).andReturn(false).times(3).andReturn(true).anyTimes();
    final Handler normalHandler1 = EasyMock.createNiceMock(Handler.class);
    final Handler normalHandler2 = EasyMock.createNiceMock(Handler.class);
    final Handler failureHandler = EasyMock.createNiceMock(Handler.class);
    final HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    final HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
    normalHandler1.handle(EasyMock.<String> anyObject(),
        EasyMock.<Request> anyObject(),
        EasyMock.<HttpServletRequest> anyObject(),
        EasyMock.<HttpServletResponse> anyObject());
    EasyMock.expectLastCall().once();
    normalHandler2.handle(EasyMock.<String> anyObject(),
        EasyMock.<Request> anyObject(),
        EasyMock.<HttpServletRequest> anyObject(),
        EasyMock.<HttpServletResponse> anyObject());
    EasyMock.expectLastCall().once();
    failureHandler.handle(EasyMock.<String> anyObject(),
        EasyMock.<Request> anyObject(),
        EasyMock.<HttpServletRequest> anyObject(),
        EasyMock.<HttpServletResponse> anyObject());
    EasyMock.expectLastCall().andThrow(new IOException());
    EasyMock.replay(handlerList, normalHandler1, normalHandler2, failureHandler);

    handlerList.addFailsafeHandler(normalHandler1);
    handlerList.addFailsafeHandler(failureHandler);
    handlerList.addFailsafeHandler(normalHandler2);
    handlerList.start();
    handlerList.handle("", new Request(), request, response);

    EasyMock.verify(handlerList, normalHandler1, normalHandler2, failureHandler);
  }
}
