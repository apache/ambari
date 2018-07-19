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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest.MpackAdvisorRequestType;
import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackAdvisorCommand;
import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackComponentLayoutRecommendationCommand;
import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackComponentLayoutValidationCommand;
import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackConfigurationValidationCommand;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse;
import org.apache.ambari.server.api.services.mpackadvisor.validations.MpackValidationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.topology.MpackInstance;
import org.apache.ambari.server.topology.ServiceInstance;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;

/**
 * MpackAdvisorHelper unit tests.
 */
public class MpackAdvisorHelperTest {
  Configuration configuration = null;
  MpackAdvisorRunner maRunner = null;
  AmbariMetaInfo metaInfo = null;
  ServiceInfo service = null;
  MpackAdvisorHelper helper = null;
  MpackAdvisorRequestType requestType = MpackAdvisorRequestType.HOST_GROUPS;

  @Before
  public void setup() throws IOException {
    configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);

    maRunner = mock(MpackAdvisorRunner.class);
    metaInfo = mock(AmbariMetaInfo.class);
    service = mock(ServiceInfo.class);
    helper = mpackAdvisorHelperSpy(configuration, maRunner, metaInfo);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testValidate_returnsCommandResult() throws MpackAdvisorException, IOException {
    MpackAdvisorRequest request = MpackAdvisorRequest.MpackAdvisorRequestBuilder.forStack()
        .ofType(requestType).build();
    MpackValidationResponse expected = mock(MpackValidationResponse.class);
    MpackAdvisorCommand<MpackValidationResponse> command = mock(MpackAdvisorCommand.class);
    when(command.invoke(request)).thenReturn(expected);
    doReturn(command).when(helper).createValidationCommand(request);
    MpackValidationResponse response = helper.validate(request);
    assertEquals(expected, response);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRecommend_returnsCommandResult() throws MpackAdvisorException, IOException {
    MpackAdvisorCommand<MpackRecommendationResponse> command = mock(MpackAdvisorCommand.class);
    MpackRecommendationResponse expected = mock(MpackRecommendationResponse.class);

    MpackAdvisorRequest request = MpackAdvisorRequest.MpackAdvisorRequestBuilder
        .forStack()
        .ofType(requestType)
        .forMpackInstances(createOdsMpackInstance())
        .build();

    when(command.invoke(request)).thenReturn(expected);
    doReturn(command).when(helper).createRecommendationCommand(ServiceInfo.ServiceAdvisorType.PYTHON, request);
    MpackRecommendationResponse response = helper.recommend(request);

    assertEquals(expected, response);
  }

  @Test(expected = MpackAdvisorException.class)
  @SuppressWarnings("unchecked")
  public void testRecommend_commandThrowsException_throwsException() throws MpackAdvisorException,
      IOException {

    MpackAdvisorCommand<MpackRecommendationResponse> command = mock(MpackAdvisorCommand.class);
    MpackAdvisorRequest request = MpackAdvisorRequest.MpackAdvisorRequestBuilder.forStack()
        .ofType(requestType).build();

    when(command.invoke(request)).thenThrow(new MpackAdvisorException("message"));
    doReturn(command).when(helper).createRecommendationCommand(ServiceInfo.ServiceAdvisorType.PYTHON, request);
    helper.recommend(request);

    assertTrue(false);
  }

  @Test
  public void testCreateRecommendationCommand_returnsComponentLayoutRecommendationCommand()
      throws IOException, MpackAdvisorException {
    MpackAdvisorRequest request = MpackAdvisorRequest.MpackAdvisorRequestBuilder.forStack()
        .ofType(requestType).build();

    MpackAdvisorCommand<MpackRecommendationResponse> command = null;

    command = helper.createRecommendationCommand(ServiceInfo.ServiceAdvisorType.PYTHON, request);
    assertEquals(MpackComponentLayoutRecommendationCommand.class, command.getClass());
  }

  @Test
  public void testCreateValidationCommand_returnsComponentLayoutValidationCommand()
      throws IOException, MpackAdvisorException {
    MpackAdvisorRequest request = MpackAdvisorRequest.MpackAdvisorRequestBuilder.forStack()
        .ofType(requestType).forMpackInstances(createOdsMpackInstance()).build();

    MpackAdvisorCommand<MpackValidationResponse> command = helper.createValidationCommand(request);

    assertEquals(MpackComponentLayoutValidationCommand.class, command.getClass());
  }


  @Test
  public void testCreateValidationCommand_returnsConfigurationValidationCommand()
      throws IOException, MpackAdvisorException {
    MpackAdvisorRequestType requestType = MpackAdvisorRequestType.CONFIGURATIONS;
    MpackAdvisorRequest request = MpackAdvisorRequest.MpackAdvisorRequestBuilder.forStack()
        .forMpackInstances(createOdsMpackInstance()).ofType(requestType).build();

    MpackAdvisorCommand<MpackValidationResponse> command = helper.createValidationCommand(request);

    assertEquals(MpackConfigurationValidationCommand.class, command.getClass());
  }

  /* Helper function to create ODS Mpack Instance
   */
  public static Collection<MpackInstance> createOdsMpackInstance() {
    ImmutableList<ServiceInstance> list = ImmutableList.of(
        new ServiceInstance("HBASE", "HBASE", null),
        new ServiceInstance("HBASE_CLIENTS", "HBASE_CLIENTS", null),
        new ServiceInstance("HADOOP_CLIENTS", "HADOOP_CLIENTS", null));
    return new ArrayList<MpackInstance>() {{
      add(new MpackInstance("ODS-DEFAULT", "ODS", "1.0.0-b340", list));
    }};
  }
  private static MpackAdvisorHelper mpackAdvisorHelperSpy(Configuration configuration, MpackAdvisorRunner maRunner, AmbariMetaInfo metaInfo) throws IOException {
    return spy(new MpackAdvisorHelper(configuration, maRunner, metaInfo, null));
  }

}
