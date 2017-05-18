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

/**
 * Monitors log level filter changes.
 */
package org.apache.ambari.logsearch.config.api;

import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;

public interface LogLevelFilterMonitor {

  /**
   * Notification of a new or updated log level filter.
   * 
   * @param logId The log for which the log level filter was created/updated.
   * @param logLevelFilter The log level filter to apply from now on to the log.
   */
  void setLogLevelFilter(String logId, LogLevelFilter logLevelFilter);

  /**
   * Notification of the removal of a log level filter.
   * 
   * @param logId The log of which's log level filter was removed.
   */
  void removeLogLevelFilter(String logId);

}
