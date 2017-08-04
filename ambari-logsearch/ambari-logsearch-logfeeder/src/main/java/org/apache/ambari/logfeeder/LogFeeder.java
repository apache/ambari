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

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.common.ConfigHandler;
import org.apache.ambari.logfeeder.common.LogEntryParseTester;
import org.apache.ambari.logsearch.config.api.LogSearchConfig;
import org.apache.ambari.logsearch.config.api.LogSearchConfigFactory;
import org.apache.ambari.logsearch.config.api.LogSearchConfig.Component;
import org.apache.ambari.logsearch.config.zookeeper.LogSearchConfigZK;
import org.apache.commons.io.FileUtils;
import org.apache.ambari.logfeeder.input.InputConfigUploader;
import org.apache.ambari.logfeeder.loglevelfilter.LogLevelFilterHandler;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.metrics.MetricsManager;
import org.apache.ambari.logfeeder.util.LogFeederPropertiesUtil;
import org.apache.ambari.logfeeder.util.SSLUtil;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;

import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class LogFeeder {
  private static final Logger LOG = Logger.getLogger(LogFeeder.class);

  private static final int LOGFEEDER_SHUTDOWN_HOOK_PRIORITY = 30;
  private static final int CHECKPOINT_CLEAN_INTERVAL_MS = 24 * 60 * 60 * 60 * 1000; // 24 hours

  private final LogFeederCommandLine cli;
  
  private ConfigHandler configHandler;
  private LogSearchConfig config;
  
  private MetricsManager metricsManager = new MetricsManager();

  private long lastCheckPointCleanedMS = 0;
  private Thread statLoggerThread = null;

  private LogFeeder(LogFeederCommandLine cli) {
    this.cli = cli;
  }

  public void run() {
    try {
      init();
      monitor();
    } catch (Throwable t) {
      LOG.fatal("Caught exception in main.", t);
      System.exit(1);
    }
  }

  private void init() throws Throwable {
    long startTime = System.currentTimeMillis();

    SSLUtil.ensureStorePasswords();
    
    config = LogSearchConfigFactory.createLogSearchConfig(Component.LOGFEEDER,Maps.fromProperties(LogFeederPropertiesUtil.getProperties()),
        LogFeederPropertiesUtil.getClusterName(), LogSearchConfigZK.class);
    configHandler = new ConfigHandler(config);
    configHandler.init();
    LogLevelFilterHandler.init(config);
    InputConfigUploader.load(config);
    config.monitorInputConfigChanges(configHandler, new LogLevelFilterHandler(), LogFeederPropertiesUtil.getClusterName());
    
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
    JVMShutdownHook logFeederJVMHook = new JVMShutdownHook();
    ShutdownHookManager.get().addShutdownHook(logFeederJVMHook, LOGFEEDER_SHUTDOWN_HOOK_PRIORITY);
    
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

  public void test() {
    try {
      LogManager.shutdown();
      String testLogEntry = cli.getTestLogEntry();
      String testShipperConfig = FileUtils.readFileToString(new File(cli.getTestShipperConfig()), Charset.defaultCharset());
      List<String> testGlobalConfigs = new ArrayList<>();
      for (String testGlobalConfigFile : cli.getTestGlobalConfigs().split(",")) {
        testGlobalConfigs.add(FileUtils.readFileToString(new File(testGlobalConfigFile), Charset.defaultCharset()));
      }
      String testLogId = cli.getTestLogId();
      Map<String, Object> result = new LogEntryParseTester(testLogEntry, testShipperConfig, testGlobalConfigs, testLogId).parse();
      String parsedLogEntry = new GsonBuilder().setPrettyPrinting().create().toJson(result);
      System.out.println("The result of the parsing is:\n" + parsedLogEntry);
    } catch (Exception e) {
      System.out.println("Exception occurred, could not test if log entry is parseable");
      e.printStackTrace(System.out);
    }
  }
  
  public static void main(String[] args) {
    LogFeederCommandLine cli = new LogFeederCommandLine(args);
    
    LogFeeder logFeeder = new LogFeeder(cli);
    
    if (cli.isMonitor()) {
      try {
        LogFeederPropertiesUtil.loadProperties();
      } catch (Throwable t) {
        LOG.warn("Could not load logfeeder properites");
        System.exit(1);
      }
      logFeeder.run();
    } else if (cli.isTest()) {
      logFeeder.test();
    }
  }
}
