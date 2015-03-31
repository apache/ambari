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

import java.io.File;
import java.io.IOException;

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

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StackAdvisorHelper {

  private File recommendationsDir;
  private String stackAdvisorScript;
  private final AmbariMetaInfo metaInfo;

  /* Monotonically increasing requestid */
  private int requestId = 0;
  private StackAdvisorRunner saRunner;

  @Inject
  public StackAdvisorHelper(Configuration conf, StackAdvisorRunner saRunner,
                            AmbariMetaInfo metaInfo) throws IOException {
    this.recommendationsDir = conf.getRecommendationsDir();
    this.stackAdvisorScript = conf.getStackAdvisorScript();
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
    requestId += 1;

    StackAdvisorCommand<ValidationResponse> command = createValidationCommand(request
        .getRequestType());

    return command.invoke(request);
  }

  StackAdvisorCommand<ValidationResponse> createValidationCommand(
      StackAdvisorRequestType requestType) throws StackAdvisorException {
    StackAdvisorCommand<ValidationResponse> command;
    if (requestType == StackAdvisorRequestType.HOST_GROUPS) {
      command = new ComponentLayoutValidationCommand(recommendationsDir, stackAdvisorScript,
          requestId, saRunner, metaInfo);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATIONS) {
      command = new ConfigurationValidationCommand(recommendationsDir, stackAdvisorScript,
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
    requestId += 1;

    StackAdvisorCommand<RecommendationResponse> command = createRecommendationCommand(request
        .getRequestType());

    return command.invoke(request);
  }

  StackAdvisorCommand<RecommendationResponse> createRecommendationCommand(
      StackAdvisorRequestType requestType) throws StackAdvisorException {
    StackAdvisorCommand<RecommendationResponse> command;
    if (requestType == StackAdvisorRequestType.HOST_GROUPS) {
      command = new ComponentLayoutRecommendationCommand(recommendationsDir, stackAdvisorScript,
          requestId, saRunner, metaInfo);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATIONS) {
      command = new ConfigurationRecommendationCommand(recommendationsDir, stackAdvisorScript,
          requestId, saRunner, metaInfo);
    } else if (requestType == StackAdvisorRequestType.CONFIGURATION_DEPENDENCIES) {
      command = new ConfigurationDependenciesRecommendationCommand(recommendationsDir, stackAdvisorScript,
          requestId, saRunner, metaInfo);
    } else {
      throw new StackAdvisorRequestException(String.format("Unsupported request type, type=%s",
          requestType));
    }

    return command;
  }

}
