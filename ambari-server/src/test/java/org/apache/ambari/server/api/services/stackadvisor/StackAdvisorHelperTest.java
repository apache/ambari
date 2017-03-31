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

package org.apache.ambari.server.api.services.stackadvisor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.junit.Test;

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
    StackAdvisorHelper helper = spy(new StackAdvisorHelper(configuration, saRunner, metaInfo));

    StackAdvisorCommand<ValidationResponse> command = mock(StackAdvisorCommand.class);
    ValidationResponse expected = mock(ValidationResponse.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request)).thenReturn(expected);
    doReturn(command).when(helper).createValidationCommand(requestType);

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
    StackAdvisorHelper helper = spy(new StackAdvisorHelper(configuration, saRunner, metaInfo));

    StackAdvisorCommand<ValidationResponse> command = mock(StackAdvisorCommand.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request)).thenThrow(new StackAdvisorException("message"));
    doReturn(command).when(helper).createValidationCommand(requestType);
    helper.validate(request);

    assertTrue(false);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRecommend_returnsCommandResult() throws StackAdvisorException, IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    StackAdvisorHelper helper = spy(new StackAdvisorHelper(configuration, saRunner, metaInfo));

    StackAdvisorCommand<RecommendationResponse> command = mock(StackAdvisorCommand.class);
    RecommendationResponse expected = mock(RecommendationResponse.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request)).thenReturn(expected);
    doReturn(command).when(helper).createRecommendationCommand(requestType);
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
    StackAdvisorHelper helper = spy(new StackAdvisorHelper(configuration, saRunner, metaInfo));

    StackAdvisorCommand<RecommendationResponse> command = mock(StackAdvisorCommand.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request)).thenThrow(new StackAdvisorException("message"));
    doReturn(command).when(helper).createRecommendationCommand(requestType);
    helper.recommend(request);

    assertTrue(false);
  }

  @Test
  public void testCreateRecommendationCommand_returnsComponentLayoutRecommendationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;

    StackAdvisorCommand<RecommendationResponse> command = helper
        .createRecommendationCommand(requestType);

    assertEquals(ComponentLayoutRecommendationCommand.class, command.getClass());
  }

  @Test
  public void testCreateValidationCommand_returnsComponentLayoutValidationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;

    StackAdvisorCommand<ValidationResponse> command = helper.createValidationCommand(requestType);

    assertEquals(ComponentLayoutValidationCommand.class, command.getClass());
  }

  @Test
  public void testCreateValidationCommand_returnsConfigurationValidationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.CONFIGURATIONS;

    StackAdvisorCommand<ValidationResponse> command = helper.createValidationCommand(requestType);

    assertEquals(ConfigurationValidationCommand.class, command.getClass());
  }

  @Test
  public void testCreateRecommendationDependencyCommand_returnsConfigurationDependencyRecommendationCommand()
    throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.CONFIGURATION_DEPENDENCIES;

    StackAdvisorCommand<RecommendationResponse> command = helper.createRecommendationCommand(requestType);

    assertEquals(ConfigurationDependenciesRecommendationCommand.class, command.getClass());
  }

}
