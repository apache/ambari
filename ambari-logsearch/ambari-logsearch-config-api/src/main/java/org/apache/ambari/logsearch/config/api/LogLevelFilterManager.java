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

import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilterMap;

public interface LogLevelFilterManager {

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
}
