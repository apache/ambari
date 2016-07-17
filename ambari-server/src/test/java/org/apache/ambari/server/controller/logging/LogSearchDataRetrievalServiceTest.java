/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.logging;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * This test verifies the basic behavior of the
 *   LogSearchDataRetrievalServiceTest, and should
 *   verify the interaction with its dependencies, as
 *   well as the interaction with the LogSearch
 *   server.
 *
 */
public class LogSearchDataRetrievalServiceTest {

  @Test
  public void testGetTailFileWhenHelperIsAvailable() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedResultURI = "http://localhost/test/result";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock =
      mockSupport.createMock(LoggingRequestHelperFactory.class);

    LoggingRequestHelper helperMock =
      mockSupport.createMock(LoggingRequestHelper.class);

    expect(helperFactoryMock.getHelper(null, expectedClusterName)).andReturn(helperMock);
    expect(helperMock.createLogFileTailURI("http://localhost", expectedComponentName, expectedHostName)).andReturn(expectedResultURI);

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService =
      new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();

    String resultTailFileURI =
      retrievalService.getLogFileTailURI("http://localhost", expectedComponentName, expectedHostName, expectedClusterName);

    assertEquals("TailFileURI was not returned as expected",
                 expectedResultURI, resultTailFileURI);

    mockSupport.verifyAll();
  }

  @Test
  public void testGetTailFileWhenRequestHelperIsNull() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock =
      mockSupport.createMock(LoggingRequestHelperFactory.class);

    // return null, to simulate the case where LogSearch Server is
    // not available for some reason
    expect(helperFactoryMock.getHelper(null, expectedClusterName)).andReturn(null);

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService =
      new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();

    String resultTailFileURI =
      retrievalService.getLogFileTailURI("http://localhost", expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("TailFileURI should be null in this case",
               resultTailFileURI);

    mockSupport.verifyAll();

  }

}
