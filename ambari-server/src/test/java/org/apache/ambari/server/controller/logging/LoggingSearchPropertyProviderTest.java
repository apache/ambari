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
package org.apache.ambari.server.controller.logging;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.LogDefinition;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * This test case verifies the basic behavior of the
 * LoggingSearchPropertyProvider.
 *
 * Specifically, it verifies that this PropertyProvider
 * implementation uses the output from LogSearch queries
 * to attach the correct logging-related output to the
 * HostComponent resources in Ambari.
 *
 */
public class LoggingSearchPropertyProviderTest {

  /**
   * Verifies the following:
   *
   *   1. That this PropertyProvider implementation uses
   *      the expected interfaces to make queries to the LogSearch
   *      service.
   *   2. That the PropertyProvider queries the current HostComponent
   *      resource in order to obtain the correct information to send to
   *      LogSearch.
   *   3. That the output of the LogSearch query is properly set on the
   *      HostComponent resource in the expected structure.
   *
   * @throws Exception
   */
  @Test
  public void testBasicCall() throws Exception {
    final String expectedLogFilePath =
      "/var/log/hdfs/hdfs_namenode.log";

    final String expectedSearchEnginePath = "/api/v1/clusters/clusterone/logging/searchEngine";

    final String expectedAmbariURL = "http://c6401.ambari.apache.org:8080";

    final String expectedTailFileQueryString = "?components_name=hdfs_namenode&host=c6401.ambari.apache.org&pageSize=50";

    final String expectedStackName = "HDP";
    final String expectedStackVersion = "2.4";
    final String expectedComponentName = "NAMENODE";
    final String expectedServiceName = "HDFS";
    final String expectedLogSearchComponentName = "hdfs_namenode";

    EasyMockSupport mockSupport = new EasyMockSupport();

    Resource resourceMock =
      mockSupport.createMock(Resource.class);
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "component_name"))).andReturn(expectedComponentName).atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "host_name"))).andReturn("c6401.ambari.apache.org").atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "cluster_name"))).andReturn("clusterone").atLeastOnce();

    Capture<HostComponentLoggingInfo> captureLogInfo = new Capture<HostComponentLoggingInfo>();
    // expect set method to be called
    resourceMock.setProperty(eq("logging"), capture(captureLogInfo));

    LogLevelQueryResponse levelQueryResponse =
      new LogLevelQueryResponse();

    levelQueryResponse.setTotalCount("3");
    // setup test data for log levels
    List<NameValuePair> testListOfLogLevels =
      new LinkedList<NameValuePair>();
    testListOfLogLevels.add(new NameValuePair("ERROR", "150"));
    testListOfLogLevels.add(new NameValuePair("WARN", "500"));
    testListOfLogLevels.add(new NameValuePair("INFO", "2200"));

    levelQueryResponse.setNameValueList(testListOfLogLevels);

    Request requestMock =
      mockSupport.createMock(Request.class);

    Predicate predicateMock =
      mockSupport.createMock(Predicate.class);

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    AmbariMetaInfo metaInfoMock =
      mockSupport.createMock(AmbariMetaInfo.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    StackId stackIdMock =
      mockSupport.createMock(StackId.class);

    ComponentInfo componentInfoMock =
      mockSupport.createMock(ComponentInfo.class);

    LogDefinition logDefinitionMock =
      mockSupport.createMock(LogDefinition.class);

    LogSearchDataRetrievalService dataRetrievalServiceMock =
      mockSupport.createMock(LogSearchDataRetrievalService.class);

    expect(dataRetrievalServiceMock.getLogFileNames(expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn(Collections.singleton(expectedLogFilePath)).atLeastOnce();
    expect(dataRetrievalServiceMock.getLogFileTailURI(expectedAmbariURL + expectedSearchEnginePath, expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn("").atLeastOnce();


    expect(controllerMock.getAmbariServerURI(expectedSearchEnginePath)).
      andReturn(expectedAmbariURL + expectedSearchEnginePath).atLeastOnce();
    expect(controllerMock.getAmbariMetaInfo()).andReturn(metaInfoMock).atLeastOnce();
    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster("clusterone")).andReturn(clusterMock).atLeastOnce();
    expect(stackIdMock.getStackName()).andReturn(expectedStackName).atLeastOnce();
    expect(stackIdMock.getStackVersion()).andReturn(expectedStackVersion).atLeastOnce();
    expect(clusterMock.getCurrentStackVersion()).andReturn(stackIdMock).atLeastOnce();

    expect(metaInfoMock.getComponentToService(expectedStackName, expectedStackVersion, expectedComponentName)).andReturn(expectedServiceName).atLeastOnce();
    expect(metaInfoMock.getComponent(expectedStackName, expectedStackVersion, expectedServiceName, expectedComponentName)).andReturn(componentInfoMock).atLeastOnce();

    expect(componentInfoMock.getLogs()).andReturn(Collections.singletonList(logDefinitionMock)).atLeastOnce();
    expect(logDefinitionMock.getLogId()).andReturn(expectedLogSearchComponentName).atLeastOnce();

    mockSupport.replayAll();

    LoggingSearchPropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider();

    propertyProvider.setAmbariManagementController(controllerMock);
    propertyProvider.setLogSearchDataRetrievalService(dataRetrievalServiceMock);


    Set<Resource> returnedResources =
      propertyProvider.populateResources(Collections.singleton(resourceMock), requestMock, predicateMock);

    // verify that the property provider attached
    // the expected logging structure to the associated resource

    assertEquals("Returned resource set was of an incorrect size",
      1, returnedResources.size());

    HostComponentLoggingInfo returnedLogInfo =
      captureLogInfo.getValue();

    assertNotNull("Returned log info should not be null",
      returnedLogInfo);

    assertEquals("Returned component was not the correct name",
      "hdfs_namenode", returnedLogInfo.getComponentName());

    assertEquals("Returned list of log file names for this component was incorrect",
      1, returnedLogInfo.getListOfLogFileDefinitions().size());

    LogFileDefinitionInfo definitionInfo =
      returnedLogInfo.getListOfLogFileDefinitions().get(0);

    assertEquals("Incorrect log file type was found",
      LogFileType.SERVICE, definitionInfo.getLogFileType());
    assertEquals("Incorrect log file path found",
      expectedLogFilePath, definitionInfo.getLogFileName());
    assertEquals("Incorrect URL path to searchEngine",
      expectedAmbariURL + expectedSearchEnginePath, definitionInfo.getSearchEngineURL());

    // verify that the log level count information
    // was not added to the HostComponent resource
    assertNull(returnedLogInfo.getListOfLogLevels());

    mockSupport.verifyAll();
  }

  /**
   * Verifies that this property provider implementation will
   * properly handle the case of LogSearch not being deployed in
   * the cluster or available.
   *
   * @throws Exception
   */
  @Test
  public void testCheckWhenLogSearchNotAvailable() throws Exception {

    final String expectedStackName = "HDP";
    final String expectedStackVersion = "2.4";
    final String expectedComponentName = "NAMENODE";
    final String expectedServiceName = "HDFS";
    final String expectedLogSearchComponentName = "hdfs_namenode";

    EasyMockSupport mockSupport = new EasyMockSupport();

    Resource resourceMock =
      mockSupport.createMock(Resource.class);
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "component_name"))).andReturn(expectedComponentName).atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "host_name"))).andReturn("c6401.ambari.apache.org").atLeastOnce();
    expect(resourceMock.getPropertyValue(PropertyHelper.getPropertyId("HostRoles", "cluster_name"))).andReturn("clusterone").atLeastOnce();

    LoggingRequestHelperFactory helperFactoryMock =
      mockSupport.createMock(LoggingRequestHelperFactory.class);

    Request requestMock =
      mockSupport.createMock(Request.class);

    Predicate predicateMock =
      mockSupport.createMock(Predicate.class);

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    AmbariMetaInfo metaInfoMock =
      mockSupport.createMock(AmbariMetaInfo.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    StackId stackIdMock =
      mockSupport.createMock(StackId.class);

    ComponentInfo componentInfoMock =
      mockSupport.createMock(ComponentInfo.class);

    LogDefinition logDefinitionMock =
      mockSupport.createMock(LogDefinition.class);

    LogSearchDataRetrievalService dataRetrievalServiceMock =
      mockSupport.createMock(LogSearchDataRetrievalService.class);

    expect(controllerMock.getAmbariMetaInfo()).andReturn(metaInfoMock).atLeastOnce();
    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster("clusterone")).andReturn(clusterMock).atLeastOnce();
    expect(stackIdMock.getStackName()).andReturn(expectedStackName).atLeastOnce();
    expect(stackIdMock.getStackVersion()).andReturn(expectedStackVersion).atLeastOnce();
    expect(clusterMock.getCurrentStackVersion()).andReturn(stackIdMock).atLeastOnce();

    expect(metaInfoMock.getComponentToService(expectedStackName, expectedStackVersion, expectedComponentName)).andReturn(expectedServiceName).atLeastOnce();
    expect(metaInfoMock.getComponent(expectedStackName, expectedStackVersion, expectedServiceName, expectedComponentName)).andReturn(componentInfoMock).atLeastOnce();



    // simulate the case when LogSearch is not deployed, or is not available for some reason
    expect(dataRetrievalServiceMock.getLogFileNames(expectedLogSearchComponentName, "c6401.ambari.apache.org", "clusterone")).andReturn(null).atLeastOnce();

    expect(componentInfoMock.getLogs()).andReturn(Collections.singletonList(logDefinitionMock)).atLeastOnce();
    expect(logDefinitionMock.getLogId()).andReturn(expectedLogSearchComponentName).atLeastOnce();

    mockSupport.replayAll();

    LoggingSearchPropertyProvider propertyProvider =
      new LoggingSearchPropertyProvider();

    propertyProvider.setAmbariManagementController(controllerMock);
    propertyProvider.setLogSearchDataRetrievalService(dataRetrievalServiceMock);


    // execute the populate resources method, verify that no exceptions occur, due to
    // the LogSearch helper not being available
    Set<Resource> returnedResources =
      propertyProvider.populateResources(Collections.singleton(resourceMock), requestMock, predicateMock);

    // verify that the set of resources has not changed in size
    assertEquals("Returned resource set was of an incorrect size",
      1, returnedResources.size());

    // verify that the single resource passed in was returned
    assertSame("Returned resource was not the expected instance.",
      resourceMock, returnedResources.iterator().next());

    mockSupport.verifyAll();
  }

}
