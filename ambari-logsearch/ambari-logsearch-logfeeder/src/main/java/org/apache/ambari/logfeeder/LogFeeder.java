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

package org.apache.ambari.logfeeder;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.logfeeder.common.ConfigHandler;
import org.apache.ambari.logsearch.config.api.LogSearchConfig;
import org.apache.ambari.logsearch.config.api.LogSearchConfigFactory;
import org.apache.ambari.logsearch.config.api.LogSearchConfig.Component;
import org.apache.ambari.logsearch.config.zookeeper.LogSearchConfigZK;
import org.apache.ambari.logfeeder.input.InputConfigUploader;
import org.apache.ambari.logfeeder.input.InputManager;
import org.apache.ambari.logfeeder.loglevelfilter.LogLevelFilterHandler;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.metrics.MetricsManager;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.SSLUtil;
import org.apache.curator.shaded.com.google.common.collect.Maps;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.log4j.Logger;

public class LogFeeder {
  private static final Logger LOG = Logger.getLogger(LogFeeder.class);

  private static final int LOGFEEDER_SHUTDOWN_HOOK_PRIORITY = 30;
  private static final int CHECKPOINT_CLEAN_INTERVAL_MS = 24 * 60 * 60 * 60 * 1000; // 24 hours

  private ConfigHandler configHandler = new ConfigHandler();
  private LogSearchConfig config;
  
  private InputManager inputManager = new InputManager();
  private MetricsManager metricsManager = new MetricsManager();

  private long lastCheckPointCleanedMS = 0;
  private boolean isLogfeederCompleted = false;
  private Thread statLoggerThread = null;

  private LogFeeder() {}

  public void run() {
    try {
      init();
      monitor();
      waitOnAllDaemonThreads();
    } catch (Throwable t) {
      LOG.fatal("Caught exception in main.", t);
      System.exit(1);
    }
  }

  private void init() throws Throwable {
    long startTime = System.currentTimeMillis();

    configHandler.init();
    SSLUtil.ensureStorePasswords();
    
    config = LogSearchConfigFactory.createLogSearchConfig(Component.LOGFEEDER,
        Maps.fromProperties(LogFeederUtil.getProperties()), LogSearchConfigZK.class);
    LogLevelFilterHandler.init(config);
    InputConfigUploader.load(config);
    config.monitorInputConfigChanges(configHandler, new LogLevelFilterHandler());
    
    metricsManager.init();
    
    LOG.debug("==============");
    
    long endTime = System.currentTimeMillis();
    LOG.info("Took " + (endTime - startTime) + " ms to initialize");
  }

  private class JVMShutdownHook extends Thread {

    public void run() {
      try {
        LOG.info("Processing is shutting down.");

        configHandler.close();
        config.close();
        logStats();

        LOG.info("LogSearch is exiting.");
      } catch (Throwable t) {
        // Ignore
      }
    }
  }

  private void monitor() throws Exception {
    JVMShutdownHook logfeederJVMHook = new JVMShutdownHook();
    ShutdownHookManager.get().addShutdownHook(logfeederJVMHook, LOGFEEDER_SHUTDOWN_HOOK_PRIORITY);
    
    statLoggerThread = new Thread("statLogger") {

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

          if (isLogfeederCompleted) {
            break;
          }
        }
      }

    };
    statLoggerThread.setDaemon(true);
    statLoggerThread.start();

  }

  private void logStats() {
    configHandler.logStats();

    if (metricsManager.isMetricsEnabled()) {
      List<MetricData> metricsList = new ArrayList<MetricData>();
      configHandler.addMetrics(metricsList);
      metricsManager.useMetrics(metricsList);
    }
  }

  private void waitOnAllDaemonThreads() {
    if ("true".equals(LogFeederUtil.getStringProperty("foreground"))) {
      inputManager.waitOnAllInputs();
      isLogfeederCompleted = true;
      if (statLoggerThread != null) {
        try {
          statLoggerThread.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) {
    try {
      LogFeederUtil.loadProperties("logfeeder.properties", args);
    } catch (Throwable t) {
      LOG.warn("Could not load logfeeder properites");
      System.exit(1);
    }

    LogFeeder logFeeder = new LogFeeder();
    logFeeder.run();
  }
}
