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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for connect a remote source periodically to get / set log level filters.
 */
public abstract class LogLevelFilterUpdater extends Thread {

  private static final Logger LOG = LoggerFactory.getLogger(LogLevelFilterUpdater.class);

  private final LogLevelFilterMonitor logLevelFilterMonitor;
  private final int interval;
  private boolean stop = false;

  public LogLevelFilterUpdater(String threadName, LogLevelFilterMonitor logLevelFilterMonitor, Integer interval) {
    this.setName(threadName);
    this.setDaemon(true);
    this.logLevelFilterMonitor = logLevelFilterMonitor;
    this.interval = interval == null ? 30 : interval;
  }

  public LogLevelFilterMonitor getLogLevelFilterMonitor() {
    return logLevelFilterMonitor;
  }

  public void setStop(boolean stop) {
    this.stop = stop;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted() || !stop) {
      try {
        Thread.sleep(1000 * interval);
        checkFilters(logLevelFilterMonitor);
      } catch (Exception e) {
        LOG.error("Exception happened during log level filter check: {}", e);
      }
    }
  }

  /**
   * Periodically check filters from a source (and use log level filter monitor to create/update/delete it)
   */
  protected abstract void checkFilters(final LogLevelFilterMonitor logLevelFilterMonitor);
}
