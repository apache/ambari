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

package org.apache.ambari.logsearch.config.api;

import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilterMap;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputSolrProperties;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;

/**
 * Log Search Configuration for Log Search Server.
 */
public interface LogSearchConfigServer extends LogSearchConfig {
  /**
   * Initialization of the configuration.
   * 
   * @param properties The properties of that component.
   * @throws Exception
   */
  void init(Map<String, String> properties) throws Exception;

  /**
   * Returns all the service names with input configurations of a cluster.
   * 
   * @param clusterName The name of the cluster which's services are required.
   * @return List of the service names.
   */
  List<String> getServices(String clusterName);

  /**
   * Checks if input configuration exists.
   * 
   * @param clusterName The name of the cluster where the service is looked for.
   * @param serviceName The name of the service looked for.
   * @return If input configuration exists for the service.
   * @throws Exception
   */
  boolean inputConfigExists(String clusterName, String serviceName) throws Exception;

  /**
   * Returns the global configurations of a cluster.
   * 
   * @param clusterName The name of the cluster where the service is looked for.
   * @return The global configurations of the cluster if it exists, null otherwise.
   */
  String getGlobalConfigs(String clusterName);

  /**
   * Modifies the input configuration for a service in a cluster.
   * 
   * @param clusterName The name of the cluster where the service is.
   * @param serviceName The name of the service of which's input configuration is uploaded.
   * @param inputConfig The input configuration of the service.
   * @throws Exception
   */
  void setInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception;

  /**
   * Returns the input configuration of a service in a cluster.
   * 
   * @param clusterName The name of the cluster where the service is looked for.
   * @param serviceName The name of the service looked for.
   * @return The input configuration for the service if it exists, null otherwise.
   */
  InputConfig getInputConfig(String clusterName, String serviceName);

  /**
   * Modifies the log level filters for all the logs.
   * 
   * @param clusterName The name of the cluster where the logs are.
   * @param filters The log level filters to set.
   * @throws Exception
   */
  void setLogLevelFilters(String clusterName, LogLevelFilterMap filters) throws Exception;

  /**
   * Returns the Log Level Filters of a cluster.
   * 
   * @param clusterName The name of the cluster which's log level filters are required.
   * @return All the log level filters of the cluster.
   */
  LogLevelFilterMap getLogLevelFilters(String clusterName);

  /**
   * Saves the properties of an Output Solr.
   * 
   * @param type The type of the Output Solr.
   * @param outputSolrProperties The properties of the Output Solr.
   * @throws Exception
   */
  void saveOutputSolrProperties(String type, OutputSolrProperties outputSolrProperties) throws Exception;
}
