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

import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;

/**
 * Log Search Configuration, which uploads, retrieves configurations, and monitors it's changes.
 */
public interface LogSearchConfig extends Closeable {
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
   * Uploads the log level filter of a log.
   * 
   * @param clusterName The name of the cluster where the log is.
   * @param logId The id of the log.
   * @param filter The log level filter for the log.
   * @throws Exception 
   */
  void createLogLevelFilter(String clusterName, String logId, LogLevelFilter filter) throws Exception;
}
