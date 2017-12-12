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
package org.apache.ambari.logfeeder.metrics;

import org.apache.ambari.logfeeder.common.ConfigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class StatsLogger extends Thread {

  private static final Logger LOG = LoggerFactory.getLogger(StatsLogger.class);

  private static final int CHECKPOINT_CLEAN_INTERVAL_MS = 24 * 60 * 60 * 60 * 1000; // 24 hours

  private long lastCheckPointCleanedMS = 0;

  @Inject
  private ConfigHandler configHandler;

  @Inject
  private MetricsManager metricsManager;

  public StatsLogger() {
    super("statLogger");
    setDaemon(true);
  }

  @PostConstruct
  public void init() {
    this.start();
  }

  @Override
  public void run() {
    while (true) {
      try {
        Thread.sleep(30 * 1000);
      } catch (Throwable t) {
        // Ignore
      }
      try {
        logStats();
      } catch (Throwable t) {
        LOG.error("LogStats: Caught exception while logging stats.", t);
      }

      if (System.currentTimeMillis() > (lastCheckPointCleanedMS + CHECKPOINT_CLEAN_INTERVAL_MS)) {
        lastCheckPointCleanedMS = System.currentTimeMillis();
        configHandler.cleanCheckPointFiles();
      }
    }
  }

  private void logStats() {
    configHandler.logStats();
    if (metricsManager.isMetricsEnabled()) {
      List<MetricData> metricsList = new ArrayList<MetricData>();
      configHandler.addMetrics(metricsList);
      metricsManager.useMetrics(metricsList);
    }
  }
}
