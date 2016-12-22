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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;


import org.apache.ambari.server.controller.AmbariManagementController;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.common.cache.Cache;
import com.google.inject.Injector;

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

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    LoggingRequestHelper helperMock =
      mockSupport.createMock(LoggingRequestHelper.class);

    expect(helperFactoryMock.getHelper(null, expectedClusterName)).andReturn(helperMock);
    expect(helperMock.createLogFileTailURI("http://localhost", expectedComponentName, expectedHostName)).andReturn(expectedResultURI);

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();

    String resultTailFileURI =
      retrievalService.getLogFileTailURI("http://localhost", expectedComponentName, expectedHostName, expectedClusterName);

    assertEquals("TailFileURI was not returned as expected", expectedResultURI, resultTailFileURI);

    mockSupport.verifyAll();
  }

  @Test
  public void testGetTailFileWhenRequestHelperIsNull() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    // return null, to simulate the case where LogSearch Server is
    // not available for some reason
    expect(helperFactoryMock.getHelper(null, expectedClusterName)).andReturn(null);

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();

    String resultTailFileURI =
      retrievalService.getLogFileTailURI("http://localhost", expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("TailFileURI should be null in this case", resultTailFileURI);

    mockSupport.verifyAll();
  }

  @Test
  public void testGetLogFileNamesDefault() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    Executor executorMock = mockSupport.createMock(Executor.class);

    Injector injectorMock =
      mockSupport.createMock(Injector.class);

    // expect the executor to be called to execute the LogSearch request
    executorMock.execute(isA(LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable.class));
    // executor should only be called once
    expectLastCall().once();

    expect(injectorMock.getInstance(LoggingRequestHelperFactory.class)).andReturn(helperFactoryMock);

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    retrievalService.setInjector(injectorMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();
    retrievalService.setExecutor(executorMock);


    assertEquals("Default request set should be empty", 0, retrievalService.getCurrentRequests().size());

    Set<String> resultSet = retrievalService.getLogFileNames(expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("Inital query on the retrieval service should be null, since cache is empty by default", resultSet);
    assertEquals("Incorrect number of entries in the current request set", 1, retrievalService.getCurrentRequests().size());

    assertTrue("Incorrect HostComponent set on request set",
                retrievalService.getCurrentRequests().contains(expectedComponentName + "+" + expectedHostName));

    mockSupport.verifyAll();
  }

  @Test
  public void testGetLogFileNamesIgnoreMultipleRequestsForSameHostComponent() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);

    Executor executorMock = mockSupport.createMock(Executor.class);

    mockSupport.replayAll();

    LogSearchDataRetrievalService retrievalService = new LogSearchDataRetrievalService();
    retrievalService.setLoggingRequestHelperFactory(helperFactoryMock);
    // call the initialization routine called by the Google framework
    retrievalService.doStart();
    // there should be no expectations set on this mock
    retrievalService.setExecutor(executorMock);

    // set the current requests to include this expected HostComponent
    // this simulates the case where a request is ongoing for this HostComponent,
    // but is not yet completed.
    retrievalService.getCurrentRequests().add(expectedComponentName + "+" + expectedHostName);

    Set<String> resultSet = retrievalService.getLogFileNames(expectedComponentName, expectedHostName, expectedClusterName);

    assertNull("Inital query on the retrieval service should be null, since cache is empty by default", resultSet);

    mockSupport.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithSuccessfulCall() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);
    LoggingRequestHelper helperMock = mockSupport.createMock(LoggingRequestHelper.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);

    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(helperMock);
    expect(helperMock.sendGetLogFileNamesRequest(expectedComponentName, expectedHostName)).andReturn(Collections.singleton("/this/is/just/a/test/directory"));
    // expect that the results will be placed in the cache
    cacheMock.put(expectedComponentAndHostName, Collections.singleton("/this/is/just/a/test/directory"));
    // expect that the completed request is removed from the current request set
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();

    mockSupport.replayAll();

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
          cacheMock, currentRequestsMock, helperFactoryMock, controllerMock);
    loggingRunnable.run();

    mockSupport.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithFailedCallNullHelper() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);

    // return null to simulate an error during helper instance creation
    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(null);
    // expect that the completed request is removed from the current request set,
    // even in the event of a failure to obtain the LogSearch data
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();

    mockSupport.replayAll();

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
          cacheMock, currentRequestsMock, helperFactoryMock, controllerMock);
    loggingRunnable.run();

    mockSupport.verifyAll();

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithFailedCallNullResult() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);
    LoggingRequestHelper helperMock = mockSupport.createMock(LoggingRequestHelper.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);

    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(helperMock);
    // return null to simulate an error occurring during the LogSearch data request
    expect(helperMock.sendGetLogFileNamesRequest(expectedComponentName, expectedHostName)).andReturn(null);
    // expect that the completed request is removed from the current request set,
    // even in the event of a failure to obtain the LogSearch data
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();

    mockSupport.replayAll();

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
          cacheMock, currentRequestsMock, helperFactoryMock, controllerMock);
    loggingRunnable.run();

    mockSupport.verifyAll();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRunnableWithFailedCallEmptyResult() throws Exception {
    final String expectedHostName = "c6401.ambari.apache.org";
    final String expectedComponentName = "DATANODE";
    final String expectedClusterName = "clusterone";
    final String expectedComponentAndHostName = expectedComponentName + "+" + expectedHostName;

    EasyMockSupport mockSupport = new EasyMockSupport();

    LoggingRequestHelperFactory helperFactoryMock = mockSupport.createMock(LoggingRequestHelperFactory.class);
    AmbariManagementController controllerMock = mockSupport.createMock(AmbariManagementController.class);
    LoggingRequestHelper helperMock = mockSupport.createMock(LoggingRequestHelper.class);

    Cache<String, Set<String>> cacheMock = mockSupport.createMock(Cache.class);
    Set<String> currentRequestsMock = mockSupport.createMock(Set.class);

    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(helperMock);
    // return null to simulate an error occurring during the LogSearch data request
    expect(helperMock.sendGetLogFileNamesRequest(expectedComponentName, expectedHostName)).andReturn(Collections.<String>emptySet());
    // expect that the completed request is removed from the current request set,
    // even in the event of a failure to obtain the LogSearch data
    expect(currentRequestsMock.remove(expectedComponentAndHostName)).andReturn(true).once();

    mockSupport.replayAll();

    LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable loggingRunnable =
      new LogSearchDataRetrievalService.LogSearchFileNameRequestRunnable(expectedHostName, expectedComponentName, expectedClusterName,
          cacheMock, currentRequestsMock, helperFactoryMock, controllerMock);
    loggingRunnable.run();

    mockSupport.verifyAll();
  }
}
