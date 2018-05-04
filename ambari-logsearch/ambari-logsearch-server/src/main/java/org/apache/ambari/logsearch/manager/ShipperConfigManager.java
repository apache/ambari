/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.manager;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.logfeeder.common.LogEntryParseTester;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;
import org.apache.ambari.logsearch.configurer.LogSearchConfigConfigurer;
import org.apache.ambari.logsearch.model.common.LSServerInputConfig;
import org.apache.ambari.logsearch.model.common.LSServerLogLevelFilterMap;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Named
public class ShipperConfigManager extends JsonManagerBase {

  private static final Logger logger = Logger.getLogger(ShipperConfigManager.class);

  @Inject
  private LogSearchConfigConfigurer logSearchConfigConfigurer;
  
  public List<String> getServices(String clusterName) {
    return logSearchConfigConfigurer.getConfig().getServices(clusterName);
  }

  public LSServerInputConfig getInputConfig(String clusterName, String serviceName) {
    InputConfig inputConfig = logSearchConfigConfigurer.getConfig().getInputConfig(clusterName, serviceName);
    return new LSServerInputConfig(inputConfig);
  }

  public Response createInputConfig(String clusterName, String serviceName, LSServerInputConfig inputConfig) {
    try {
      if (logSearchConfigConfigurer.getConfig().inputConfigExists(clusterName, serviceName)) {
        return Response.serverError()
            .type(MediaType.APPLICATION_JSON)
            .entity(ImmutableMap.of("errorMessage", "Input config already exists for service " + serviceName))
            .build();
      }
      
      logSearchConfigConfigurer.getConfig().createInputConfig(clusterName, serviceName, new ObjectMapper().writeValueAsString(inputConfig));
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn("Could not create input config", e);
      return Response.serverError().build();
    }
  }

  public Response setInputConfig(String clusterName, String serviceName, LSServerInputConfig inputConfig) {
    try {
      if (!logSearchConfigConfigurer.getConfig().inputConfigExists(clusterName, serviceName)) {
        return Response.serverError()
            .type(MediaType.APPLICATION_JSON)
            .entity(ImmutableMap.of("errorMessage", "Input config doesn't exist for service " + serviceName))
            .build();
      }
      
      logSearchConfigConfigurer.getConfig().setInputConfig(clusterName, serviceName, new ObjectMapper().writeValueAsString(inputConfig));
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn("Could not update input config", e);
      return Response.serverError().build();
    }
  }

  public Response testShipperConfig(String shipperConfig, String logId, String testEntry, String clusterName) {
    try {
      LSServerInputConfig inputConfigValidate = new ObjectMapper().readValue(shipperConfig, LSServerInputConfig.class);
      Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
      Set<ConstraintViolation<LSServerInputConfig>> violations = validator.validate(inputConfigValidate);
      if (!violations.isEmpty()) {
        throw new IllegalArgumentException("Error validating shipper config:\n" + violations);
      }
      String globalConfigs = logSearchConfigConfigurer.getConfig().getGlobalConfigs(clusterName);
      LogEntryParseTester tester = new LogEntryParseTester(testEntry, shipperConfig, globalConfigs, logId);
      Map<String, Object> resultEntrty = tester.parse();
      return Response.ok().entity(resultEntrty).build();
    } catch (Exception e) {
      Map<String, Object> errorResponse = ImmutableMap.of("errorMessage", (Object)e.toString());
      return Response.serverError().entity(errorResponse).build();
    }
  }

  public LSServerLogLevelFilterMap getLogLevelFilters(String clusterName) {
    return new LSServerLogLevelFilterMap(logSearchConfigConfigurer.getConfig().getLogLevelFilters(clusterName));
  }

  public Response setLogLevelFilters(String clusterName, LSServerLogLevelFilterMap request) {
    try {
      logSearchConfigConfigurer.getConfig().setLogLevelFilters(clusterName, request.convertToApi());
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn("Could not update log level filters", e);
      return Response.serverError().build();
    }
  }
}
