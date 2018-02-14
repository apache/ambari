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

import org.apache.ambari.logsearch.config.api.model.outputconfig.OutputSolrProperties;

/**
 * Log Search Configuration for Log Feeder.
 */
public interface LogSearchConfigLogFeeder extends LogSearchConfig {
  /**
   * Initialization of the configuration.
   * 
   * @param properties The properties of that component.
   * @param clusterName The name of the cluster.
   * @throws Exception
   */
  void init(Map<String, String> properties, String clusterName) throws Exception;

  /**
   * Checks if input configuration exists.
   * 
   * @param serviceName The name of the service looked for.
   * @return If input configuration exists for the service.
   * @throws Exception
   */
  boolean inputConfigExists(String serviceName) throws Exception;

  /**
   * Starts the monitoring of the input configurations, asynchronously.
   * 
   * @param inputConfigMonitor The input config monitor to call in case of an input config change.
   * @param logLevelFilterMonitor The log level filter monitor to call in case of a log level filter change.
   * @param clusterName The name of the cluster.
   * @throws Exception
   */
  void monitorInputConfigChanges(InputConfigMonitor inputConfigMonitor, LogLevelFilterMonitor logLevelFilterMonitor,
      String clusterName) throws Exception;

  /**
   * Get the properties of an Output Solr.
   * 
   * @param type The type of the Output Solr.
   * @return The properties of the Output Solr, or null if it doesn't exist.
   * @throws Exception
   */
  OutputSolrProperties getOutputSolrProperties(String type) throws Exception;

  /**
   * Saves the properties of an Output Solr.
   *
   * @param outputConfigMonitors The monitors which want to watch the output config changes.
   * @throws Exception
   */
  void monitorOutputProperties(List<? extends OutputConfigMonitor> outputConfigMonitors) throws Exception;
}
