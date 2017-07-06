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

import java.io.File;
import java.io.IOException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType;
import org.apache.ambari.server.api.services.stackadvisor.commands.ComponentLayoutRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ComponentLayoutValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationDependenciesRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.configuration.Configuration;

import org.apache.ambari.server.state.ServiceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class StackAdvisorHelper {

  protected static Log LOG = LogFactory.getLog(StackAdvisorHelper.class);

  private File recommendationsDir;
  private String recommendationsArtifactsLifetime;
  private int recommendationsArtifactsRolloverMax;
  public static String pythonStackAdvisorScript;
  private final AmbariMetaInfo metaInfo;

  /* Monotonically increasing requestid */
  private int requestId = 0;
  private StackAdvisorRunner saRunner;

  @Inject
  public StackAdvisorHelper(Configuration conf, StackAdvisorRunner saRunner,
                            AmbariMetaInfo metaInfo) throws IOException {
    this.recommendationsDir = conf.getRecommendationsDir();
    this.recommendationsArtifactsLifetime = conf.getRecommendationsArtifactsLifetime();
    this.recommendationsArtifactsRolloverMax = conf.getRecommendationsArtifactsRolloverMax();

    this.pythonStackAdvisorScript = conf.getStackAdvisorScript();
    this.saRunner = saRunner;
    this.metaInfo = metaInfo;
  }

  /**
   * Returns validation (component-layout or configurations) result for the
   * request.
   * 
   * @param request the validation request
   * @return {@link ValidationResponse} instance
   * @throws StackAdvisorException in case of stack advisor script errors
   */
  public synchronized ValidationResponse validate(StackAdvisorRequest request)
      throws StackAdvisorException {
      requestId = generateRequestId();

    // TODO, need frontend to pass the Service Name that was modified.
    // For now, hardcode.
    // Once fixed, change StackAdvisorHelperTest.java to use the actual service name.
    String serviceName = "ZOOKEEPER";
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorType(request.getStackName(), request.getStackVersion(), serviceName);
    StackAdvisorCommand<ValidationResponse> command = createValidationCommand(serviceName, request);

    return command.invoke(request, serviceAdvisorType);
  }

  StackAdvisorCommand<ValidationResponse> createValidationCommand(String serviceName, StackAdvisorRequest request) throws StackAdvisorException {
    StackAdvisorRequestType requestType = request.getRequestType();
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorType(request.getStackName(), request.getStackVersion(), serviceName);

    StackAdvisorCommand<ValidationResponse> command;
    if (requestType == StackAdvisorRequestType.HOST_GROUPS) {
      command = new ComponentLayoutValidationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATIONS) {
      command = new ConfigurationValidationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo);
    } else {
      throw new StackAdvisorRequestException(String.format("Unsupported request type, type=%s",
          requestType));
    }

    return command;
  }

  /**
   * Returns recommendation (component-layout or configurations) based on the
   * request.
   * 
   * @param request the recommendation request
   * @return {@link RecommendationResponse} instance
   * @throws StackAdvisorException in case of stack advisor script errors
   */
  public synchronized RecommendationResponse recommend(StackAdvisorRequest request)
      throws StackAdvisorException {
      requestId = generateRequestId();

    // TODO, need to pass the service Name that was modified.
    // For now, hardcode
    String serviceName = "ZOOKEEPER";

    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorType(request.getStackName(), request.getStackVersion(), serviceName);
    StackAdvisorCommand<RecommendationResponse> command = createRecommendationCommand(serviceName, request);

    return command.invoke(request, serviceAdvisorType);
  }

  StackAdvisorCommand<RecommendationResponse> createRecommendationCommand(String serviceName, StackAdvisorRequest request) throws StackAdvisorException {
    StackAdvisorRequestType requestType = request.getRequestType();
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorType(request.getStackName(), request.getStackVersion(), serviceName);

    StackAdvisorCommand<RecommendationResponse> command;
    if (requestType == StackAdvisorRequestType.HOST_GROUPS) {
      command = new ComponentLayoutRecommendationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATIONS) {
      command = new ConfigurationRecommendationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATION_DEPENDENCIES) {
      command = new ConfigurationDependenciesRecommendationCommand(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, saRunner, metaInfo);
    } else {
      throw new StackAdvisorRequestException(String.format("Unsupported request type, type=%s",
          requestType));
    }

    return command;
  }

  /**
   * Get the Service Advisor type that the service defines for the specified stack and version. If an error, return null.
   * @param stackName Stack Name
   * @param stackVersion Stack Version
   * @param serviceName Service Name
   * @return Service Advisor type for that Stack, Version, and Service
   */
  private ServiceInfo.ServiceAdvisorType getServiceAdvisorType(String stackName, String stackVersion, String serviceName) {
    try {
      ServiceInfo service = metaInfo.getService(stackName, stackVersion, serviceName);
      ServiceInfo.ServiceAdvisorType serviceAdvisorType = service.getServiceAdvisorType();

      return serviceAdvisorType;
    } catch (AmbariException e) {
      ;
    }
    return null;
  }

  /**
   * Returns an incremented requestId. Rollsover back to 0 in case the requestId >= recommendationsArtifactsrollovermax
   * @return {int requestId}
   */
  private int generateRequestId(){
      requestId += 1;
      return requestId % recommendationsArtifactsRolloverMax;

  }

}
