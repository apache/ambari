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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorException;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorHelperTest;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRunner;
import org.apache.ambari.server.state.ServiceInfo;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MpackConfigurationRecommendationCommandTest {
  private TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    temp.create();
  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  public void testValidate_success() throws Exception {
    MpackAdvisorRequest request = EasyMock.createNiceMock(MpackAdvisorRequest.class);
    List<String> hosts = new ArrayList<String>() {{
      add("host1.example.com");
      add("host2.example.com");
    }};
    expect(request.getHosts()).andReturn(hosts).anyTimes();
    expect(request.getMpackInstances()).andReturn(MpackAdvisorHelperTest.createOdsMpackInstance()).anyTimes();
    replay(request);
    MpackAdvisorRunner maRunner = mock(MpackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    File file = mock(File.class);
    MpackConfigurationRecommendationCommand command = new MpackConfigurationRecommendationCommand(file, "1w", ServiceInfo.ServiceAdvisorType.PYTHON, 1, maRunner, metaInfo, null);

    command.validate(request);
  }

  @Test(expected = MpackAdvisorException.class)
  public void testValidate_failureEmptyHosts() throws Exception {
    MpackAdvisorRequest request = EasyMock.createNiceMock(MpackAdvisorRequest.class);
    expect(request.getHosts()).andReturn(null).anyTimes();
    MpackAdvisorRunner maRunner = mock(MpackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    File file = mock(File.class);
    MpackConfigurationRecommendationCommand command = new MpackConfigurationRecommendationCommand(file, "1w", ServiceInfo.ServiceAdvisorType.PYTHON, 1, maRunner, metaInfo, null);

    command.validate(request);
  }

  @Test(expected = MpackAdvisorException.class)
  public void testValidate_failureEmptyMpackInstances() throws Exception {
    MpackAdvisorRequest request = EasyMock.createNiceMock(MpackAdvisorRequest.class);
    List<String> hosts = new ArrayList<String>() {{
      add("host1.example.com");
      add("host2.example.com");
    }};
    expect(request.getHosts()).andReturn(hosts).anyTimes();
    expect(request.getMpackInstances()).andReturn(null).anyTimes();
    replay(request);
    MpackAdvisorRunner maRunner = mock(MpackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    File file = mock(File.class);
    MpackConfigurationRecommendationCommand command = new MpackConfigurationRecommendationCommand(file, "1w", ServiceInfo.ServiceAdvisorType.PYTHON, 1, maRunner, metaInfo, null);

    command.validate(request);
  }
}
