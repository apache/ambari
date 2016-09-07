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

import java.io.BufferedInputStream;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputManager;
import org.apache.ambari.logfeeder.input.InputSimulate;
import org.apache.ambari.logfeeder.logconfig.LogConfigHandler;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.metrics.MetricsManager;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.ambari.logfeeder.output.OutputManager;
import org.apache.ambari.logfeeder.util.AliasUtil;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ambari.logfeeder.util.AliasUtil.AliasType;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

public class LogFeeder {
  private static final Logger LOG = Logger.getLogger(LogFeeder.class);

  private static final int LOGFEEDER_SHUTDOWN_HOOK_PRIORITY = 30;
  private static final int CHECKPOINT_CLEAN_INTERVAL_MS = 24 * 60 * 60 * 60 * 1000; // 24 hours

  private OutputManager outputManager = new OutputManager();
  private InputManager inputManager = new InputManager();
  private MetricsManager metricsManager = new MetricsManager();

  public static Map<String, Object> globalConfigs = new HashMap<>();

  private List<Map<String, Object>> inputConfigList = new ArrayList<>();
  private List<Map<String, Object>> filterConfigList = new ArrayList<>();
  private List<Map<String, Object>> outputConfigList = new ArrayList<>();
  
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
    Date startTime = new Date();

    loadConfigFiles();
    addSimulatedInputs();
    mergeAllConfigs();
    
    LogConfigHandler.handleConfig();
    
    outputManager.init();
    inputManager.init();
    metricsManager.init();
    
    LOG.debug("==============");
    
    Date endTime = new Date();
    LOG.info("Took " + (endTime.getTime() - startTime.getTime()) + " ms to initialize");
  }

  private void loadConfigFiles() throws Exception {
    List<String> configFiles = getConfigFiles();
    for (String configFileName : configFiles) {
      LOG.info("Going to load config file:" + configFileName);
      configFileName = configFileName.replace("\\ ", "%20");
      File configFile = new File(configFileName);
      if (configFile.exists() && configFile.isFile()) {
        LOG.info("Config file exists in path." + configFile.getAbsolutePath());
        loadConfigsUsingFile(configFile);
      } else {
        LOG.info("Trying to load config file from classloader: " + configFileName);
        loadConfigsUsingClassLoader(configFileName);
        LOG.info("Loaded config file from classloader: " + configFileName);
      }
    }
  }

  private List<String> getConfigFiles() {
    List<String> configFiles = new ArrayList<>();
    
    String logfeederConfigFilesProperty = LogFeederUtil.getStringProperty("logfeeder.config.files");
    LOG.info("logfeeder.config.files=" + logfeederConfigFilesProperty);
    if (logfeederConfigFilesProperty != null) {
      configFiles.addAll(Arrays.asList(logfeederConfigFilesProperty.split(",")));
    }

    String inputConfigDir = LogFeederUtil.getStringProperty("input_config_dir");
    if (StringUtils.isNotEmpty(inputConfigDir)) {
      File configDirFile = new File(inputConfigDir);
      List<File> inputConfigFiles = FileUtil.getAllFileFromDir(configDirFile, "json", false);
      for (File inputConfigFile : inputConfigFiles) {
        configFiles.add(inputConfigFile.getAbsolutePath());
      }
    }
    
    if (CollectionUtils.isEmpty(configFiles)) {
      String configFileProperty = LogFeederUtil.getStringProperty("config.file", "config.json");
      configFiles.addAll(Arrays.asList(configFileProperty.split(",")));
    }
    
    return configFiles;
  }

  private void loadConfigsUsingFile(File configFile) throws Exception {
    try {
      String configData = FileUtils.readFileToString(configFile);
      loadConfigs(configData);
    } catch (Exception t) {
      LOG.error("Error opening config file. configFilePath=" + configFile.getAbsolutePath());
      throw t;
    }
  }

  private void loadConfigsUsingClassLoader(String configFileName) throws Exception {
    try (BufferedInputStream fis = (BufferedInputStream) this.getClass().getClassLoader().getResourceAsStream(configFileName)) {
      String configData = IOUtils.toString(fis);
      loadConfigs(configData);
    }
  }

  @SuppressWarnings("unchecked")
  private void loadConfigs(String configData) throws Exception {
    Type type = new TypeToken<Map<String, Object>>() {}.getType();
    Map<String, Object> configMap = LogFeederUtil.getGson().fromJson(configData, type);

    // Get the globals
    for (String key : configMap.keySet()) {
      switch (key) {
        case "global" :
          globalConfigs.putAll((Map<String, Object>) configMap.get(key));
          break;
        case "input" :
          List<Map<String, Object>> inputConfig = (List<Map<String, Object>>) configMap.get(key);
          inputConfigList.addAll(inputConfig);
          break;
        case "filter" :
          List<Map<String, Object>> filterConfig = (List<Map<String, Object>>) configMap.get(key);
          filterConfigList.addAll(filterConfig);
          break;
        case "output" :
          List<Map<String, Object>> outputConfig = (List<Map<String, Object>>) configMap.get(key);
          outputConfigList.addAll(outputConfig);
          break;
        default :
          LOG.warn("Unknown config key: " + key);
      }
    }
  }
  
  private void addSimulatedInputs() {
    int simulatedInputNumber = LogFeederUtil.getIntProperty("logfeeder.simulate.input_number", 0);
    if (simulatedInputNumber == 0)
      return;
    
    InputSimulate.loadTypeToFilePath(inputConfigList);
    inputConfigList.clear();
    
    for (int i = 0; i < simulatedInputNumber; i++) {
      HashMap<String, Object> mapList = new HashMap<String, Object>();
      mapList.put("source", "simulate");
      mapList.put("rowtype", "service");
      inputConfigList.add(mapList);
    }
  }

  private void mergeAllConfigs() {
    loadOutputs();
    loadInputs();
    loadFilters();
    
    assignOutputsToInputs();
  }

  private void loadOutputs() {
    for (Map<String, Object> map : outputConfigList) {
      if (map == null) {
        continue;
      }
      mergeBlocks(globalConfigs, map);

      String value = (String) map.get("destination");
      if (StringUtils.isEmpty(value)) {
        LOG.error("Output block doesn't have destination element");
        continue;
      }
      Output output = (Output) AliasUtil.getClassInstance(value, AliasType.OUTPUT);
      if (output == null) {
        LOG.error("Output object could not be found");
        continue;
      }
      output.setDestination(value);
      output.loadConfig(map);

      // We will only check for is_enabled out here. Down below we will check whether this output is enabled for the input
      if (output.getBooleanValue("is_enabled", true)) {
        output.logConfgs(Level.INFO);
        outputManager.add(output);
      } else {
        LOG.info("Output is disabled. So ignoring it. " + output.getShortDescription());
      }
    }
  }

  private void loadInputs() {
    for (Map<String, Object> map : inputConfigList) {
      if (map == null) {
        continue;
      }
      mergeBlocks(globalConfigs, map);

      String value = (String) map.get("source");
      if (StringUtils.isEmpty(value)) {
        LOG.error("Input block doesn't have source element");
        continue;
      }
      Input input = (Input) AliasUtil.getClassInstance(value, AliasType.INPUT);
      if (input == null) {
        LOG.error("Input object could not be found");
        continue;
      }
      input.setType(value);
      input.loadConfig(map);

      if (input.isEnabled()) {
        input.setOutputManager(outputManager);
        input.setInputManager(inputManager);
        inputManager.add(input);
        input.logConfgs(Level.INFO);
      } else {
        LOG.info("Input is disabled. So ignoring it. " + input.getShortDescription());
      }
    }
  }

  private void loadFilters() {
    sortFilters();

    List<Input> toRemoveInputList = new ArrayList<Input>();
    for (Input input : inputManager.getInputList()) {
      for (Map<String, Object> map : filterConfigList) {
        if (map == null) {
          continue;
        }
        mergeBlocks(globalConfigs, map);

        String value = (String) map.get("filter");
        if (StringUtils.isEmpty(value)) {
          LOG.error("Filter block doesn't have filter element");
          continue;
        }
        Filter filter = (Filter) AliasUtil.getClassInstance(value, AliasType.FILTER);
        if (filter == null) {
          LOG.error("Filter object could not be found");
          continue;
        }
        filter.loadConfig(map);
        filter.setInput(input);

        if (filter.isEnabled()) {
          filter.setOutputManager(outputManager);
          input.addFilter(filter);
          filter.logConfgs(Level.INFO);
        } else {
          LOG.debug("Ignoring filter " + filter.getShortDescription() + " for input " + input.getShortDescription());
        }
      }
      
      if (input.getFirstFilter() == null) {
        toRemoveInputList.add(input);
      }
    }

    for (Input toRemoveInput : toRemoveInputList) {
      LOG.warn("There are no filters, we will ignore this input. " + toRemoveInput.getShortDescription());
      inputManager.removeInput(toRemoveInput);
    }
  }

  private void sortFilters() {
    Collections.sort(filterConfigList, new Comparator<Map<String, Object>>() {

      @Override
      public int compare(Map<String, Object> o1, Map<String, Object> o2) {
        Object o1Sort = o1.get("sort_order");
        Object o2Sort = o2.get("sort_order");
        if (o1Sort == null || o2Sort == null) {
          return 0;
        }
        
        int o1Value = parseSort(o1, o1Sort);
        int o2Value = parseSort(o2, o2Sort);
        
        return o1Value - o2Value;
      }

      private int parseSort(Map<String, Object> map, Object o) {
        if (!(o instanceof Number)) {
          try {
            return (new Double(Double.parseDouble(o.toString()))).intValue();
          } catch (Throwable t) {
            LOG.error("Value is not of type Number. class=" + o.getClass().getName() + ", value=" + o.toString()
              + ", map=" + map.toString());
            return 0;
          }
        } else {
          return ((Number) o).intValue();
        }
      }
    });
  }

  private void assignOutputsToInputs() {
    Set<Output> usedOutputSet = new HashSet<Output>();
    for (Input input : inputManager.getInputList()) {
      for (Output output : outputManager.getOutputs()) {
        if (LogFeederUtil.isEnabled(output.getConfigs(), input.getConfigs())) {
          usedOutputSet.add(output);
          input.addOutput(output);
        }
      }
    }
    outputManager.retainUsedOutputs(usedOutputSet);
  }

  @SuppressWarnings("unchecked")
  private void mergeBlocks(Map<String, Object> fromMap, Map<String, Object> toMap) {
    for (String key : fromMap.keySet()) {
      Object objValue = fromMap.get(key);
      if (objValue == null) {
        continue;
      }
      if (objValue instanceof Map) {
        Map<String, Object> globalFields = LogFeederUtil.cloneObject((Map<String, Object>) objValue);

        Map<String, Object> localFields = (Map<String, Object>) toMap.get(key);
        if (localFields == null) {
          localFields = new HashMap<String, Object>();
          toMap.put(key, localFields);
        }

        if (globalFields != null) {
          for (String fieldKey : globalFields.keySet()) {
            if (!localFields.containsKey(fieldKey)) {
              localFields.put(fieldKey, globalFields.get(fieldKey));
            }
          }
        }
      }
    }

    // Let's add the rest of the top level fields if missing
    for (String key : fromMap.keySet()) {
      if (!toMap.containsKey(key)) {
        toMap.put(key, fromMap.get(key));
      }
    }
  }

  private class JVMShutdownHook extends Thread {

    public void run() {
      try {
        LOG.info("Processing is shutting down.");

        inputManager.close();
        outputManager.close();
        inputManager.checkInAll();

        logStats();

        LOG.info("LogSearch is exiting.");
      } catch (Throwable t) {
        // Ignore
      }
    }
  }

  private void monitor() throws Exception {
    inputManager.monitor();
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
            inputManager.cleanCheckPointFiles();
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
    inputManager.logStats();
    outputManager.logStats();

    if (metricsManager.isMetricsEnabled()) {
      List<MetricData> metricsList = new ArrayList<MetricData>();
      inputManager.addMetricsContainers(metricsList);
      outputManager.addMetricsContainers(metricsList);
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
