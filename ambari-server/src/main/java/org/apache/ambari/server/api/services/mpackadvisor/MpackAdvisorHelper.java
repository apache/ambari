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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackAdvisorCommand;
import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackComponentLayoutRecommendationCommand;
import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackComponentLayoutValidationCommand;
import org.apache.ambari.server.api.services.mpackadvisor.commands.MpackConfigurationRecommendationCommand;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse;
import org.apache.ambari.server.api.services.mpackadvisor.validations.MpackValidationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.topology.MpackInstance;
import org.apache.ambari.server.topology.ServiceInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MpackAdvisorHelper {

  protected static Log LOG = LogFactory.getLog(org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorHelper.class);

  private File mpackRecommendationsDir;
  private String recommendationsArtifactsLifetime;
  private int recommendationsArtifactsRolloverMax;
  public static String pythonMpackAdvisorScript;
  private final AmbariMetaInfo metaInfo;
  private final AmbariServerConfigurationHandler ambariServerConfigurationHandler;

  /* Monotonically increasing requestid */
  private int requestId = 0;
  private MpackAdvisorRunner maRunner;

  @Inject
  public MpackAdvisorHelper(Configuration conf, MpackAdvisorRunner maRunner,
                            AmbariMetaInfo metaInfo, AmbariServerConfigurationHandler ambariServerConfigurationHandler) throws IOException {
    this.mpackRecommendationsDir = conf.getMpackRecommendationsDir();
    this.recommendationsArtifactsLifetime = conf.getRecommendationsArtifactsLifetime();
    this.recommendationsArtifactsRolloverMax = conf.getRecommendationsArtifactsRolloverMax();
    this.pythonMpackAdvisorScript = conf.getMpackAdvisorScript();
    this.maRunner = maRunner;
    this.metaInfo = metaInfo;
    this.ambariServerConfigurationHandler = ambariServerConfigurationHandler;
  }

  /**
   * Returns validation (component-layout or configurations) result for the
   * request.
   *
   * @param request the validation request
   * @return {@link MpackValidationResponse} instance
   * @throws MpackAdvisorException in case of mpack advisor script errors
   */
  public synchronized MpackValidationResponse validate(MpackAdvisorRequest request)
      throws MpackAdvisorException {
    requestId = generateRequestId();

    MpackAdvisorCommand<MpackValidationResponse> command = createValidationCommand(request);

    return command.invoke(request);
  }

  MpackAdvisorCommand<MpackValidationResponse> createValidationCommand(MpackAdvisorRequest request) throws MpackAdvisorException {
    MpackAdvisorCommand<MpackValidationResponse> command = null;

    MpackAdvisorRequest.MpackAdvisorRequestType requestType = request.getRequestType();
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorTypeFromRequest(request);

    if (requestType == MpackAdvisorRequest.MpackAdvisorRequestType.HOST_GROUPS) {
          command = new MpackComponentLayoutValidationCommand(mpackRecommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
              requestId, maRunner, metaInfo, ambariServerConfigurationHandler);
    } else {
      // TODO : for other MpackAdvisorRequestType's.
      throw new MpackAdvisorRequestException(String.format("Unsupported request type, type=%s",
          requestType));
    }

    return command;
  }

  public synchronized MpackRecommendationResponse recommend(MpackAdvisorRequest request)
      throws MpackAdvisorException {
    requestId = generateRequestId();

    MpackRecommendationResponse response = null;
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = getServiceAdvisorTypeFromRequest(request);
    MpackAdvisorCommand<MpackRecommendationResponse> command = createRecommendationCommand(serviceAdvisorType, request);
    response = command.invoke(request);

    return response;
  }


  MpackAdvisorCommand<MpackRecommendationResponse> createRecommendationCommand(ServiceInfo.ServiceAdvisorType serviceAdvisorType, MpackAdvisorRequest request) throws MpackAdvisorException {
      MpackAdvisorRequest.MpackAdvisorRequestType requestType = request.getRequestType();

    MpackAdvisorCommand<MpackRecommendationResponse> command;
    if (requestType == MpackAdvisorRequest.MpackAdvisorRequestType.HOST_GROUPS) {
      command = new MpackComponentLayoutRecommendationCommand(mpackRecommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, maRunner, metaInfo, ambariServerConfigurationHandler);
    }  else if (requestType == MpackAdvisorRequest.MpackAdvisorRequestType.CONFIGURATIONS) {
      command = new MpackConfigurationRecommendationCommand(mpackRecommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType,
          requestId, maRunner, metaInfo, ambariServerConfigurationHandler);
    } else {
      // TODO : for other MpackAdvisorRequestType's.
      throw new MpackAdvisorRequestException(String.format("Unsupported request type, type=%s",
          requestType));
    }

    return command;
  }

  /**
   * Iterates over the request->mpackInstances and its serviceInstances to determine the ServiceAdvisorType.
   * Queries for the Service Advisor Type as soon as it gets a serviceInstance in MpackInstance which is of
   * Server Type. ("*_CLIENTS" will not have a Service Advisor, hence can't be queried for).
   *
   * Assumes that for a given request that all the ServiceInstances (across all Mpack Instances) will be
   * of same Advisor Type (ServiceInfo.ServiceAdvisorType).
   *
   * @param  request Passed-in request.
   * @return Service Advisor type for that Stack, Version, and Service
   */
  private ServiceInfo.ServiceAdvisorType getServiceAdvisorTypeFromRequest(MpackAdvisorRequest request)
      throws MpackAdvisorRequestException {
    Collection<MpackInstance> mpackInstances = request.getMpackInstances();
    ServiceInfo.ServiceAdvisorType serviceAdvisorType = null;
    if (mpackInstances != null && !mpackInstances.isEmpty()) {
      Iterator<MpackInstance> mpackInstancesItr = mpackInstances.iterator();
      while (mpackInstancesItr.hasNext()) {
        MpackInstance mpackInstance = mpackInstancesItr.next();
        Collection<ServiceInstance> serviceInstances = mpackInstance.getServiceInstances();
        if (serviceInstances != null && !serviceInstances.isEmpty()) {
          Iterator<ServiceInstance> svcInstancesItr = serviceInstances.iterator();
          while (svcInstancesItr.hasNext()) {
            ServiceInstance serviceInstance = svcInstancesItr.next();
            String serviceType = serviceInstance.getType();
            if (serviceType.endsWith("_CLIENTS")) {
              continue; // Ignore if a client is fetched.
            } else {
              return getServiceAdvisorType(mpackInstance.getMpackType(), mpackInstance.getMpackVersion(), serviceType);
            }
          }
        }
      }
    } else {
      throw new MpackAdvisorRequestException(String.format("Read MpackInstances is null or empty. Request type =%s",
          request.getRequestType()));
    }
    return serviceAdvisorType;
  }

  /**
   * Get the Service Advisor type that the service defines for the specified mpack's stack and version.
   * If an error, return null.
   *
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
