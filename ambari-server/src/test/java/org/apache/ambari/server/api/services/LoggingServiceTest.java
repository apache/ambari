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
package org.apache.ambari.server.api.services;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.logging.LoggingRequestHelperFactory;
import java.net.HttpURLConnection;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.*;

public class LoggingServiceTest {

  @Test
  public void testGetSearchEngineWhenLogSearchNotRunning() throws Exception {
    final String expectedClusterName = "clusterone";
    final String expectedErrorMessage =
      "LogSearch is not currently available.  If LogSearch is deployed in this cluster, please verify that the LogSearch services are running.";

    EasyMockSupport mockSupport =
      new EasyMockSupport();

    LoggingService.ControllerFactory controllerFactoryMock =
      mockSupport.createMock(LoggingService.ControllerFactory.class);

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    LoggingRequestHelperFactory helperFactoryMock =
      mockSupport.createMock(LoggingRequestHelperFactory.class);

    UriInfo uriInfoMock =
      mockSupport.createMock(UriInfo.class);

    expect(uriInfoMock.getQueryParameters()).andReturn(new MultivaluedMapImpl()).atLeastOnce();
    expect(controllerFactoryMock.getController()).andReturn(controllerMock).atLeastOnce();

    // return null from this factory, to simulate the case where LogSearch is
    // not running, or is not deployed in the current cluster
    expect(helperFactoryMock.getHelper(controllerMock, expectedClusterName)).andReturn(null).atLeastOnce();

    mockSupport.replayAll();

    LoggingService loggingService =
      new LoggingService(expectedClusterName, controllerFactoryMock, helperFactoryMock);

    Response resource = loggingService.getSearchEngine("", null, uriInfoMock);

    assertNotNull("The response returned by the LoggingService should not have been null",
                  resource);

    assertEquals("An OK status should have been returned",
                 HttpURLConnection.HTTP_NOT_FOUND, resource.getStatus());
    assertNotNull("A non-null Entity should have been returned",
               resource.getEntity());
    assertEquals("Expected error message was not included in the response",
                 expectedErrorMessage, resource.getEntity());

    mockSupport.verifyAll();
  }

}
