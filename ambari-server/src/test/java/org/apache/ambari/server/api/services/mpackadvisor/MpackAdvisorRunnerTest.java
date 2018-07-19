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

package org.apache.ambari.server.api.services.mpackadvisor;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackAdvisorCommandType;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.ServiceInfo;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * MpackAdvisorRunner unit tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MpackAdvisorRunner.class)
public class MpackAdvisorRunnerTest {

  static TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    temp.create();
  }

  @AfterClass
  public static void tearDown() throws IOException {
    temp.delete();
  }

  @Test(expected = MpackAdvisorException.class)
  public void testRunScript_throwsMpackAdvisorException() throws Exception {
    MpackAdvisorCommandType maCommandType = MpackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    File actionDirectory = temp.newFolder("actionDir");
    ProcessBuilder processBuilder = createNiceMock(ProcessBuilder.class);
    MpackAdvisorRunner maRunner = new MpackAdvisorRunner();
    Configuration configMock = createNiceMock(Configuration.class);
    maRunner.setConfigs(configMock);
    stub(PowerMock.method(MpackAdvisorRunner.class, "prepareShellCommand"))
        .toReturn(processBuilder);
    expect(processBuilder.environment()).andReturn(new HashMap<>()).times(3);
    expect(processBuilder.start()).andThrow(new IOException());
    replay(processBuilder, configMock);
    maRunner.runScript(ServiceInfo.ServiceAdvisorType.PYTHON, maCommandType, actionDirectory);
  }

  @Test(expected = MpackAdvisorRequestException.class)
  public void testRunScript_throwsRequestExceptionWithExitCode1() throws Exception {
    MpackAdvisorRunnerTest.buildMocksForRunScript(1);
  }

  @Test(expected = MpackAdvisorException.class)
  public void testRunScript_throwsRequestExceptionWithExitCode2() throws Exception {
    MpackAdvisorRunnerTest.buildMocksForRunScript(2);
  }

  @Test
  public void testRunScript_successWithExitCodeZero() throws Exception {
    String script = "echo";
    MpackAdvisorCommandType maCommandType = MpackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    File actionDirectory = temp.newFolder("actionDir");
    ProcessBuilder processBuilder = createNiceMock(ProcessBuilder.class);
    Process process = createNiceMock(Process.class);
    MpackAdvisorRunner maRunner = new MpackAdvisorRunner();
    Configuration configMock = createNiceMock(Configuration.class);
    maRunner.setConfigs(configMock);

    stub(PowerMock.method(MpackAdvisorRunner.class, "prepareShellCommand"))
        .toReturn(processBuilder);
    expect(processBuilder.environment()).andReturn(new HashMap<>()).times(3);
    expect(processBuilder.start()).andReturn(process);
    expect(process.waitFor()).andReturn(0);
    replay(processBuilder, process, configMock);
    maRunner.runScript(ServiceInfo.ServiceAdvisorType.PYTHON, maCommandType, actionDirectory);
  }

  // Helper Method(s).
  public static void  buildMocksForRunScript(int exitCode) throws Exception {
    MpackAdvisorCommandType maCommandType = MpackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    File actionDirectory = temp.newFolder("actionDir");
    ProcessBuilder processBuilder = createNiceMock(ProcessBuilder.class);
    Process process = createNiceMock(Process.class);
    MpackAdvisorRunner maRunner = new MpackAdvisorRunner();
    Configuration configMock = createNiceMock(Configuration.class);
    maRunner.setConfigs(configMock);

    stub(PowerMock.method(MpackAdvisorRunner.class, "prepareShellCommand"))
        .toReturn(processBuilder);
    expect(processBuilder.environment()).andReturn(new HashMap<>()).times(3);
    expect(processBuilder.start()).andReturn(process);
    expect(process.waitFor()).andReturn(exitCode);
    replay(processBuilder, process, configMock);
    maRunner.runScript(ServiceInfo.ServiceAdvisorType.PYTHON, maCommandType, actionDirectory);
  }
}
