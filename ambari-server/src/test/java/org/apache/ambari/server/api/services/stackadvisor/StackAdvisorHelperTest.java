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

package org.apache.ambari.server.api.services.stackadvisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestBuilder;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType;
import org.apache.ambari.server.api.services.stackadvisor.commands.ComponentLayoutRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ComponentLayoutValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationDependenciesRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommandType;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.ServiceInfo;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * StackAdvisorHelper unit tests.
 */
public class StackAdvisorHelperTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testValidate_returnsCommandResult() throws StackAdvisorException, IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = stackAdvisorHelperSpy(configuration, saRunner, metaInfo);

    StackAdvisorCommand<ValidationResponse> command = mock(StackAdvisorCommand.class);
    ValidationResponse expected = mock(ValidationResponse.class);

    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON)).thenReturn(expected);
    doReturn(command).when(helper).createValidationCommand("ZOOKEEPER", request);
    ValidationResponse response = helper.validate(request);
    assertEquals(expected, response);
  }

  @Test(expected = StackAdvisorException.class)
  @SuppressWarnings("unchecked")
  public void testValidate_commandThrowsException_throwsException() throws StackAdvisorException,
      IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = stackAdvisorHelperSpy(configuration, saRunner, metaInfo);

    StackAdvisorCommand<ValidationResponse> command = mock(StackAdvisorCommand.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON)).thenThrow(new StackAdvisorException("message"));
    doReturn(command).when(helper).createValidationCommand("ZOOKEEPER", request);
    helper.validate(request);

    fail();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRecommend_returnsCommandResult() throws StackAdvisorException, IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = stackAdvisorHelperSpy(configuration, saRunner, metaInfo);

    StackAdvisorCommand<RecommendationResponse> command = mock(StackAdvisorCommand.class);
    RecommendationResponse expected = mock(RecommendationResponse.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON)).thenReturn(expected);
    doReturn(command).when(helper).createRecommendationCommand("ZOOKEEPER", request);
    RecommendationResponse response = helper.recommend(request);

    assertEquals(expected, response);
  }

  @Test(expected = StackAdvisorException.class)
  @SuppressWarnings("unchecked")
  public void testRecommend_commandThrowsException_throwsException() throws StackAdvisorException,
      IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = stackAdvisorHelperSpy(configuration, saRunner, metaInfo);

    StackAdvisorCommand<RecommendationResponse> command = mock(StackAdvisorCommand.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON)).thenThrow(new StackAdvisorException("message"));
    doReturn(command).when(helper).createRecommendationCommand("ZOOKEEPER", request);
    helper.recommend(request);

    fail("Expected StackAdvisorException to be thrown");
  }

  @Test
  public void testCreateRecommendationCommand_returnsComponentLayoutRecommendationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<RecommendationResponse> command = helper
        .createRecommendationCommand("ZOOKEEPER", request);

    assertEquals(ComponentLayoutRecommendationCommand.class, command.getClass());
  }

  @Test
  public void testCreateRecommendationCommand_returnsConfigurationRecommendationCommand() throws IOException, StackAdvisorException {
    testCreateConfigurationRecommendationCommand(StackAdvisorRequestType.CONFIGURATIONS, StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS);
  }

  @Test
  public void testCreateRecommendationCommand_returnsSingleSignOnConfigurationRecommendationCommand() throws IOException, StackAdvisorException {
    testCreateConfigurationRecommendationCommand(StackAdvisorRequestType.SSO_CONFIGURATIONS, StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS_FOR_SSO);
  }

  @Test
  public void testCreateRecommendationCommand_returnsKerberosConfigurationRecommendationCommand() throws IOException, StackAdvisorException {
    testCreateConfigurationRecommendationCommand(StackAdvisorRequestType.KERBEROS_CONFIGURATIONS, StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS_FOR_KERBEROS);
  }

  @Test
  public void testCreateValidationCommand_returnsComponentLayoutValidationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<ValidationResponse> command = helper.createValidationCommand("ZOOKEEPER", request);

    assertEquals(ComponentLayoutValidationCommand.class, command.getClass());
  }

  @Test
  public void testCreateValidationCommand_returnsConfigurationValidationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.CONFIGURATIONS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<ValidationResponse> command = helper.createValidationCommand("ZOOKEEPER", request);

    assertEquals(ConfigurationValidationCommand.class, command.getClass());
  }

  @Test
  public void testCreateRecommendationDependencyCommand_returnsConfigurationDependencyRecommendationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.CONFIGURATION_DEPENDENCIES;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<RecommendationResponse> command = helper.createRecommendationCommand("ZOOKEEPER", request);

    assertEquals(ConfigurationDependenciesRecommendationCommand.class, command.getClass());
  }

  private void testCreateConfigurationRecommendationCommand(StackAdvisorRequestType requestType, StackAdvisorCommandType expectedCommandType)
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null);

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<RecommendationResponse> command = helper.createRecommendationCommand("ZOOKEEPER", request);

    assertTrue(command instanceof ConfigurationRecommendationCommand);
    assertEquals(expectedCommandType, ((ConfigurationRecommendationCommand) command).getCommandType());

  }

  private static StackAdvisorHelper stackAdvisorHelperSpy(Configuration configuration, StackAdvisorRunner saRunner, AmbariMetaInfo metaInfo) throws IOException {
    return spy(new StackAdvisorHelper(configuration, saRunner, metaInfo, null));
  }
}
