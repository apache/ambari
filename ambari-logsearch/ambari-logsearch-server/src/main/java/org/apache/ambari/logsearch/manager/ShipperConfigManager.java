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

import org.apache.ambari.logsearch.configurer.LogSearchConfigConfigurer;
import org.apache.ambari.logsearch.model.common.LSServerLogLevelFilterMap;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Named
public class ShipperConfigManager extends JsonManagerBase {

  private static final Logger logger = Logger.getLogger(ShipperConfigManager.class);

  @Inject
  private LogSearchConfigConfigurer logSearchConfigConfigurer;

  @PostConstruct
  private void postConstructor() {
    logSearchConfigConfigurer.start();
  }
  
  public List<String> getServices(String clusterName) {
    return LogSearchConfigConfigurer.getConfig().getServices(clusterName);
  }

  public String getInputConfig(String clusterName, String serviceName) {
    return LogSearchConfigConfigurer.getConfig().getInputConfig(clusterName, serviceName);
  }

  public Response createInputConfig(String clusterName, String serviceName, String inputConfig) {
    
    try {
      if (LogSearchConfigConfigurer.getConfig().inputConfigExists(clusterName, serviceName)) {
        return Response.serverError()
            .type(MediaType.APPLICATION_JSON)
            .entity(ImmutableMap.of("errorMessage", "Input config already exists for service " + serviceName))
            .build();
      }
      
      LogSearchConfigConfigurer.getConfig().createInputConfig(clusterName, serviceName, inputConfig);
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn("Could not create input config", e);
      return Response.serverError().build();
    }
  }

  public Response setInputConfig(String clusterName, String serviceName, String inputConfig) {
    try {
      if (!LogSearchConfigConfigurer.getConfig().inputConfigExists(clusterName, serviceName)) {
        return Response.serverError()
            .type(MediaType.APPLICATION_JSON)
            .entity(ImmutableMap.of("errorMessage", "Input config doesn't exist for service " + serviceName))
            .build();
      }
      
      LogSearchConfigConfigurer.getConfig().setInputConfig(clusterName, serviceName, inputConfig);
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn("Could not update input config", e);
      return Response.serverError().build();
    }
  }

  public LSServerLogLevelFilterMap getLogLevelFilters(String clusterName) {
    return new LSServerLogLevelFilterMap(LogSearchConfigConfigurer.getConfig().getLogLevelFilters(clusterName));
  }

  public Response setLogLevelFilters(String clusterName, LSServerLogLevelFilterMap request) {
    try {
      LogSearchConfigConfigurer.getConfig().setLogLevelFilters(clusterName, request.convertToApi());
      return Response.ok().build();
    } catch (Exception e) {
      logger.warn("Could not update log level filters", e);
      return Response.serverError().build();
    }
  }
}
