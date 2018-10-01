/*
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

package org.apache.ambari.server.api.services.mpackadvisor.commands;

import static junit.framework.Assert.assertNotNull;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorException;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorHelperTest;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorResponse;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRunner;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.topology.AmbariContext;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.google.common.io.Resources;

@PrepareForTest({AmbariContext.class, AmbariServer.class, AmbariManagementController.class})
public class MpackAdvisorCommandTest {
  String servicesJSON = null;
  javax.ws.rs.core.Response response = null;

  private TemporaryFolder temp = new TemporaryFolder();
  @Mock
  AmbariServerConfigurationHandler ambariServerConfigurationHandler;

  @Before
  public void setUp() throws IOException {
    temp.create();
  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test
  public void testInvoke_success() throws Exception {
    String expected = "success";
    final String testResourceString = String.format("{\"type\": \"%s\"}", expected);
    final File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    final int requestId = 2;
    MpackAdvisorRunner maRunner = mock(MpackAdvisorRunner.class);
    ObjectMapper mapper = mock(ObjectMapper.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    final MpackAdvisorCommand<MpackAdvisorCommandTest.TestResource> command = spy(new MpackAdvisorCommandTest.TestMpackAdvisorCommand(
        recommendationsDir, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, requestId, maRunner, metaInfo));

    MpackAdvisorRequest request = MpackAdvisorRequest.MpackAdvisorRequestBuilder.forStack().build();

    String hostsFilePath = Resources.getResource("mpack_advisor/hosts_count2.txt").getPath();
    String hostsJSON = new String(Files.readAllBytes(Paths.get(hostsFilePath)));

    String servicesFilePath = Resources.getResource("mpack_advisor/ods_mpack_services.txt").getPath();
    servicesJSON = new String(Files.readAllBytes(Paths.get(servicesFilePath)));

    File file = mock(File.class);
    AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);

    MpackAdvisorCommand<MpackAdvisorCommandTest.TestResource> cmd = new MpackAdvisorCommandTest.TestMpackAdvisorCommand(file, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, 1,
        maRunner, ambariMetaInfo);
    String objectNodeFilePath = Resources.getResource("mpack_advisor/ods_mpack_objectnode.json").getPath();
    String objectNodeStr = new String(Files.readAllBytes(Paths.get(objectNodeFilePath)));
    ObjectNode objectNode = (ObjectNode) cmd.mapper.readTree(objectNodeStr);

    MpackAdvisorCommand.MpackAdvisorData data = new MpackAdvisorCommand.MpackAdvisorData(hostsJSON, servicesJSON);
    doReturn(hostsJSON).when(command).getHostsInformation(request);
    doReturn(data).when(command).getServicesInformation(request, hostsJSON);
    doReturn(objectNode).when(command).adjust(eq(servicesJSON), eq(request), any());

    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String resultFilePath = String.format("%s/%s", requestId, command.getResultFileName());
        File resultFile = new File(recommendationsDir, resultFilePath);
        resultFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(resultFile, testResourceString);
        return null;
      }
    }).when(maRunner).runScript(any(ServiceInfo.ServiceAdvisorType.class), any(MpackAdvisorCommandType.class), any(File.class));

    MpackAdvisorCommandTest.TestResource result = command.invoke(request);

    assertEquals(expected, result.getType());
    assertEquals(requestId, result.getId());
  }

  @Test
  public void testPopulateServiceAdvisors() throws Exception {
    final File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    final int requestId = 2;
    MpackAdvisorRunner maRunner = mock(MpackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = EasyMock.createNiceMock(AmbariMetaInfo.class);
    ServiceInfo serviceInfo = EasyMock.createNiceMock(ServiceInfo.class);

    String advisorPath = "/var/lib/ambari-server/resources/stacks/ODS/1.0.0-b340/services/HBASE/service_advisor.py";
    String advisorName = "HBASEServiceAdvisor";
    expect(serviceInfo.getAdvisorFile()).andReturn(
        new File(advisorPath)).anyTimes();
    expect(serviceInfo.getAdvisorName()).andReturn(advisorName).anyTimes();
    expect(metaInfo.getService("ODS", "1.0.0-b340", "HBASE")).andReturn(serviceInfo).anyTimes();
    expect(metaInfo.getService("ODS", "1.0.0-b340", "HDFS_CLIENTS")).andReturn(serviceInfo).anyTimes();
    expect(metaInfo.getService("ODS", "1.0.0-b340", "HBASE_CLIENTS")).andReturn(serviceInfo).anyTimes();
    replay(metaInfo, serviceInfo);

    final MpackAdvisorCommand<MpackAdvisorCommandTest.TestResource> command = spy(new MpackAdvisorCommandTest.TestMpackAdvisorCommand(
        recommendationsDir, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, requestId, maRunner, metaInfo));

    File file = mock(File.class);
    AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);

    MpackAdvisorCommand<MpackAdvisorCommandTest.TestResource> cmd = new MpackAdvisorCommandTest.TestMpackAdvisorCommand(
        file, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, 1, maRunner, ambariMetaInfo);
    String objectNodeFilePath = Resources.getResource("mpack_advisor/ods_mpack_objectnode1.json").getPath();
    String objectNodeStr = new String(Files.readAllBytes(Paths.get(objectNodeFilePath)));
    ObjectNode objectNode = (ObjectNode) cmd.mapper.readTree(objectNodeStr);

    command.populateServiceAdvisors(objectNode);

    // Verification for updated objectNode.
    ArrayNode services = (ArrayNode) objectNode.get("services");
    Iterator<JsonNode> servicesIter = services.getElements();
    while (servicesIter.hasNext()) {
      JsonNode service = servicesIter.next();
      ObjectNode serviceVersion = (ObjectNode) service.get("StackServices");
      if (serviceVersion.get("service_name").getTextValue().equals("HBASE")) {
        assertEquals(serviceVersion.get("advisor_name").getTextValue(), advisorName);
        assertEquals(serviceVersion.get("advisor_path").getTextValue(), advisorPath);
      }
    }
  }

  @Test
  public void testGetServicesInformation() throws Exception {
    final File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    final int requestId = 2;
    MpackAdvisorRunner maRunner = mock(MpackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    final MpackAdvisorCommand<MpackAdvisorCommandTest.TestResource> command = spy(new MpackAdvisorCommandTest.TestMpackAdvisorCommand(
        recommendationsDir, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, requestId, maRunner, metaInfo));

    MpackAdvisorRequest request = MpackAdvisorRequest.MpackAdvisorRequestBuilder.forStack().build();

    String hostsFilePath = Resources.getResource("mpack_advisor/hosts_count2.txt").getPath();
    String hostsJSON = new String(Files.readAllBytes(Paths.get(hostsFilePath)));

    String servicesFilePath = Resources.getResource("mpack_advisor/ods_mpack_services.txt").getPath();
    servicesJSON = new String(Files.readAllBytes(Paths.get(servicesFilePath)));

    File file = mock(File.class);
    AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);
    MpackAdvisorCommand<MpackAdvisorCommandTest.TestResource> cmd = new MpackAdvisorCommandTest.TestMpackAdvisorCommand(
        file, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, 1, maRunner, ambariMetaInfo);
    String objectNodeFilePath = Resources.getResource("mpack_advisor/ods_mpack_objectnode.json").getPath();
    String objectNodeStr = new String(Files.readAllBytes(Paths.get(objectNodeFilePath)));
    ObjectNode objectNode = (ObjectNode) cmd.mapper.readTree(objectNodeStr);

    doReturn(hostsJSON).when(command).getHostsInformation(request);
    doReturn(objectNode).when(command).adjust(eq(servicesJSON), eq(request), any());
    request.getRecommendation().getBlueprint().setMpackInstances(MpackAdvisorHelperTest.createOdsMpackInstance());

    response = createNiceMock(Response.class);
    expect(response.getEntity()).andReturn(servicesJSON).anyTimes();
    expect(response.getStatus()).andReturn(Response.Status.OK.getStatusCode()).anyTimes();
    replay(response);

    MpackAdvisorCommand.MpackAdvisorData result = command.getServicesInformation(request, hostsJSON);
    assertNotNull(result.servicesJSON);
  }

  class TestMpackAdvisorCommand extends MpackAdvisorCommand<TestResource> {
    public TestMpackAdvisorCommand(File recommendationsDir, String recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType serviceAdvisorType,
                                   int requestId, MpackAdvisorRunner maRunner, AmbariMetaInfo metaInfo) {
      super(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType, requestId, maRunner, metaInfo, ambariServerConfigurationHandler);
    }

    @Override
    protected void validate(MpackAdvisorRequest request) throws MpackAdvisorException {
      // do nothing
    }

    @Override
    protected String getResultFileName() {
      return "result.json";
    }

    @Override
    protected MpackAdvisorCommandType getCommandType() {
      return MpackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    }

    @Override
    protected TestResource updateResponse(MpackAdvisorRequest request, TestResource response) {
      return response;
    }

    @Override
    public javax.ws.rs.core.Response handleRequest(HttpHeaders headers, String body,
                                                   UriInfo uriInfo, Request.Type requestType,
                                                   MediaType mediaType, ResourceInstance resource) {
      return response;
    }
  }

  public static class TestResource extends MpackAdvisorResponse {
    @JsonProperty
    private String type;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }
}
