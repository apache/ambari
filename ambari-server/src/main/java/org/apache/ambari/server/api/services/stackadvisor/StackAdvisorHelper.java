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

import org.apache.ambari.server.api.services.stackadvisor.commands.GetComponentLayoutRecommnedationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.GetComponentLayoutValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.configuration.Configuration;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StackAdvisorHelper {

  private File recommendationsDir;
  private String stackAdvisorScript;

  /* Monotonically increasing requestid */
  private int requestId = 0;
  private StackAdvisorRunner saRunner;

  @Inject
  public StackAdvisorHelper(Configuration conf, StackAdvisorRunner saRunner) throws IOException {
    this.recommendationsDir = conf.getRecommendationsDir();
    this.stackAdvisorScript = conf.getStackAdvisorScript();
    this.saRunner = saRunner;
  }

  /**
   * Return component-layout validation result.
   * 
   * @param validationRequest the validation request
   * @return {@link ValidationResponse} instance
   * @throws StackAdvisorException in case of stack advisor script errors
   */
  public synchronized ValidationResponse getComponentLayoutValidation(StackAdvisorRequest request)
      throws StackAdvisorException {
    requestId += 1;

    GetComponentLayoutValidationCommand command = new GetComponentLayoutValidationCommand(
        recommendationsDir, stackAdvisorScript, requestId, saRunner);
    return command.invoke(request);
  }

  /**
   * Return component-layout recommendation based on hosts and services
   * information.
   * 
   * @param request the recommendation request
   * @return {@link RecommendationResponse} instance
   * @throws StackAdvisorException in case of stack advisor script errors
   */
  public synchronized RecommendationResponse getComponentLayoutRecommnedation(
      StackAdvisorRequest request) throws StackAdvisorException {
    requestId += 1;

    GetComponentLayoutRecommnedationCommand command = new GetComponentLayoutRecommnedationCommand(
        recommendationsDir, stackAdvisorScript, requestId, saRunner);
    return command.invoke(request);
  }

}
