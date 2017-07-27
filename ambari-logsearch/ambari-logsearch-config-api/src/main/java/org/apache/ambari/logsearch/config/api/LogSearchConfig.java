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

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilterMap;
import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputSolrProperties;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;

/**
 * Log Search Configuration, which uploads, retrieves configurations, and monitors it's changes.
 */
public interface LogSearchConfig extends Closeable {
  /**
   * Enumeration of the components of the Log Search service.
   */
  public enum Component {
    SERVER, LOGFEEDER;
  }

  /**
   * Initialization of the configuration.
   * 
   * @param component The component which will use the configuration.
   * @param properties The properties of that component.
   * @param clusterName The name of the cluster, only need to be specified in LOGFEEDER mode (null for SERVER mode).
   * @throws Exception
   */
  void init(Component component, Map<String, String> properties, String clusterName) throws Exception;

  /**
   * Returns all the service names with input configurations of a cluster. Will be used only in SERVER mode.
   * 
   * @param clusterName The name of the cluster which's services are required.
   * @return List of the service names.
   */
  List<String> getServices(String clusterName);

  /**
   * Checks if input configuration exists. Will be used only in LOGFEEDER mode.
   * 
   * @param serviceName The name of the service looked for.
   * @return If input configuration exists for the service.
   * @throws Exception
   */
  boolean inputConfigExistsLogFeeder(String serviceName) throws Exception;

  /**
   * Checks if input configuration exists. Will be used only in SERVER mode.
   * 
   * @param clusterName The name of the cluster where the service is looked for.
   * @param serviceName The name of the service looked for.
   * @return If input configuration exists for the service.
   * @throws Exception
   */
  boolean inputConfigExistsServer(String clusterName, String serviceName) throws Exception;

  /**
   * Returns the global configurations of a cluster. Will be used only in SERVER mode.
   * 
   * @param clusterName The name of the cluster where the service is looked for.
   * @return The global configurations of the cluster if it exists, null otherwise.
   */
  String getGlobalConfigs(String clusterName);

  /**
   * Returns the input configuration of a service in a cluster. Will be used only in SERVER mode.
   * 
   * @param clusterName The name of the cluster where the service is looked for.
   * @param serviceName The name of the service looked for.
   * @return The input configuration for the service if it exists, null otherwise.
   */
  InputConfig getInputConfig(String clusterName, String serviceName);

  /**
   * Uploads the input configuration for a service in a cluster.
   * 
   * @param clusterName The name of the cluster where the service is.
   * @param serviceName The name of the service of which's input configuration is uploaded.
   * @param inputConfig The input configuration of the service.
   * @throws Exception
   */
  void createInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception;

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
   * Uploads the log level filter of a log.
   * 
   * @param clusterName The name of the cluster where the log is.
   * @param logId The id of the log.
   * @param filter The log level filter for the log.
   * @throws Exception 
   */
  void createLogLevelFilter(String clusterName, String logId, LogLevelFilter filter) throws Exception;

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
   * Starts the monitoring of the input configurations, asynchronously. Will be used only in LOGFEEDER mode.
   * 
   * @param inputConfigMonitor The input config monitor to call in case of an input config change.
   * @param logLevelFilterMonitor The log level filter monitor to call in case of a log level filter change.
   * @param clusterName The name of the cluster, only need to be specified in LOGFEEDER mode (null for SERVER mode).
   * @throws Exception
   */
  void monitorInputConfigChanges(InputConfigMonitor inputConfigMonitor, LogLevelFilterMonitor logLevelFilterMonitor,
      String clusterName) throws Exception;

  /**
   * Saves the properties of an Output Solr. Will be used only in SERVER mode.
   * 
   * @param type The type of the Output Solr.
   * @param outputSolrProperties The properties of the Output Solr.
   * @throws Exception
   */
  void saveOutputSolrProperties(String type, OutputSolrProperties outputSolrProperties) throws Exception;

  /**
   * Get the properties of an Output Solr. Will be used only in LOGFEEDER mode.
   * 
   * @param type The type of the Output Solr.
   * @return The properties of the Output Solr, or null if it doesn't exist.
   * @throws Exception
   */
  OutputSolrProperties getOutputSolrProperties(String type) throws Exception;

  /**
   * Saves the properties of an Output Solr. Will be used only in LOGFEEDER mode.
   * 
   * @param type The type of the Output Solr.
   * @param outputConfigMonitors The monitors which want to watch the output config changes.
   * @throws Exception
   */
  void monitorOutputProperties(List<? extends OutputConfigMonitor> outputConfigMonitors) throws Exception;
}
