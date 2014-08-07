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

package org.apache.ambari.server.api.services.stackadvisor.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestBuilder;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand.StackAdvisorData;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * StackAdvisorCommand unit tests.
 */
public class StackAdvisorCommandTest {
  private TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    temp.create();
  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test(expected = StackAdvisorException.class)
  public void testInvoke_invalidRequest_throwsException() throws StackAdvisorException {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String stackAdvisorScript = "echo";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    StackAdvisorCommand<TestResource> command = spy(new TestStackAdvisorCommand(recommendationsDir,
        stackAdvisorScript, requestId, saRunner));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .build();

    doThrow(new StackAdvisorException("message")).when(command).validate(request);
    command.invoke(request);

    assertTrue(false);
  }

  @Test(expected = StackAdvisorException.class)
  public void testInvoke_saRunnerNotSucceed_throwsException() throws StackAdvisorException {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String stackAdvisorScript = "echo";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    StackAdvisorCommand<TestResource> command = spy(new TestStackAdvisorCommand(recommendationsDir,
        stackAdvisorScript, requestId, saRunner));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .build();

    String hostsJSON = "{\"hosts\" : \"localhost\"";
    String servicesJSON = "{\"services\" : \"HDFS\"";
    StackAdvisorData data = new StackAdvisorData(hostsJSON, servicesJSON);
    doReturn(hostsJSON).when(command).getHostsInformation(request);
    doReturn(servicesJSON).when(command).getServicesInformation(request);
    doReturn(data).when(command)
        .adjust(any(StackAdvisorData.class), any(StackAdvisorRequest.class));
    when(saRunner.runScript(any(String.class), any(StackAdvisorCommandType.class), any(File.class)))
        .thenReturn(false);
    command.invoke(request);

    assertTrue(false);
  }

  @Test(expected = WebApplicationException.class)
  public void testInvoke_adjustThrowsException_throwsException() throws StackAdvisorException {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String stackAdvisorScript = "echo";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    StackAdvisorCommand<TestResource> command = spy(new TestStackAdvisorCommand(recommendationsDir,
        stackAdvisorScript, requestId, saRunner));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .build();

    doReturn("{\"hosts\" : \"localhost\"").when(command).getHostsInformation(request);
    doReturn("{\"services\" : \"HDFS\"").when(command).getServicesInformation(request);
    doThrow(new WebApplicationException()).when(command).adjust(any(StackAdvisorData.class),
        any(StackAdvisorRequest.class));
    when(saRunner.runScript(any(String.class), any(StackAdvisorCommandType.class), any(File.class)))
        .thenReturn(false);
    command.invoke(request);

    assertTrue(false);
  }

  @Test
  public void testInvoke_success() throws StackAdvisorException {
    String expected = "success";
    final String testResourceString = String.format("{\"type\": \"%s\"}", expected);
    final File recommendationsDir = temp.newFolder("recommendationDir");
    String stackAdvisorScript = "echo";
    final int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    final StackAdvisorCommand<TestResource> command = spy(new TestStackAdvisorCommand(
        recommendationsDir, stackAdvisorScript, requestId, saRunner));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .build();

    String hostsJSON = "{\"hosts\" : \"localhost\"";
    String servicesJSON = "{\"services\" : \"HDFS\"";
    StackAdvisorData data = new StackAdvisorData(hostsJSON, servicesJSON);
    doReturn(hostsJSON).when(command).getHostsInformation(request);
    doReturn(servicesJSON).when(command).getServicesInformation(request);
    doReturn(data).when(command)
        .adjust(any(StackAdvisorData.class), any(StackAdvisorRequest.class));
    when(saRunner.runScript(any(String.class), any(StackAdvisorCommandType.class), any(File.class)))
        .thenAnswer(new Answer<Boolean>() {
          public Boolean answer(InvocationOnMock invocation) throws Throwable {
            String resultFilePath = String.format("%s/%s", requestId, command.getResultFileName());
            File resultFile = new File(recommendationsDir, resultFilePath);
            resultFile.getParentFile().mkdirs();
            FileUtils.writeStringToFile(resultFile, testResourceString);
            return true;
          }
        });
    TestResource result = command.invoke(request);

    assertEquals(expected, result.getType());
  }

  class TestStackAdvisorCommand extends StackAdvisorCommand<TestResource> {
    public TestStackAdvisorCommand(File recommendationsDir, String stackAdvisorScript,
        int requestId, StackAdvisorRunner saRunner) {
      super(recommendationsDir, stackAdvisorScript, requestId, saRunner);
    }

    @Override
    protected void validate(StackAdvisorRequest request) throws StackAdvisorException {
      // do nothing
    }

    @Override
    protected String getResultFileName() {
      return "result.json";
    }

    @Override
    protected StackAdvisorCommandType getCommandType() {
      return StackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    }

    @Override
    protected TestResource updateResponse(StackAdvisorRequest request, TestResource response) {
      return response;
    }
  }

  public static class TestResource {
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
