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

import java.io.File;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorException;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRunner;
import org.apache.ambari.server.api.services.mpackadvisor.validations.MpackValidationResponse;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.state.ServiceInfo.ServiceAdvisorType;
import org.apache.ambari.server.topology.MpackInstance;

/**
 * {@link MpackAdvisorCommand} implementation for configuration validation.
 */
public class MpackConfigurationValidationCommand extends MpackAdvisorCommand<MpackValidationResponse> {

  public MpackConfigurationValidationCommand(File recommendationsDir,
                                        String recommendationsArtifactsLifetime,
                                        ServiceAdvisorType serviceAdvisorType,
                                        int requestId,
                                        MpackAdvisorRunner maRunner,
                                        AmbariMetaInfo metaInfo,
                                        AmbariServerConfigurationHandler ambariServerConfigurationHandler) {
    super(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType, requestId, maRunner, metaInfo,
          ambariServerConfigurationHandler);
  }

  @Override
  protected MpackAdvisorCommandType getCommandType() {
    return MpackAdvisorCommandType.VALIDATE_CONFIGURATIONS;
  }

  @Override
  protected void validate(MpackAdvisorRequest request) throws MpackAdvisorException {
    if (request.getHosts() == null || request.getHosts().isEmpty()) {
      throw new MpackAdvisorException("Hosts must not be empty");
    }

    if (request.getMpackInstances() != null || !request.getMpackInstances().isEmpty()) {
      for (MpackInstance mpackInstance : request.getMpackInstances()) {
        if (mpackInstance.getServiceInstances() == null || mpackInstance.getServiceInstances().isEmpty()) {
          throw new MpackAdvisorException("Service instances for Mpack Instance " + mpackInstance.getMpackName()
              + " is empty.");
        }
      }
    } else {
      throw new MpackAdvisorException("Request's Mpack instances is null or empty.");
    }
  }

  @Override
  protected MpackValidationResponse updateResponse(MpackAdvisorRequest request,
                                              MpackValidationResponse response) {
    return response;
  }

  @Override
  protected String getResultFileName() {
    return "configurations-validation.json";
  }
}

