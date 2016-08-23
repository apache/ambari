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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.ambari.logfeeder.input.InputMgr;
import org.apache.ambari.logfeeder.input.InputSimulate;
import org.apache.ambari.logfeeder.logconfig.LogfeederScheduler;
import org.apache.ambari.logfeeder.metrics.MetricCount;
import org.apache.ambari.logfeeder.metrics.MetricsMgr;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.ambari.logfeeder.output.OutputMgr;
import org.apache.ambari.logfeeder.util.AliasUtil;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.AliasUtil.ALIAS_PARAM;
import org.apache.ambari.logfeeder.util.AliasUtil.ALIAS_TYPE;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

public class LogFeeder {
  private static final Logger logger = Logger.getLogger(LogFeeder.class);

  private static final int LOGFEEDER_SHUTDOWN_HOOK_PRIORITY = 30;

  private Collection<Output> outputList = new ArrayList<Output>();

  private OutputMgr outMgr = new OutputMgr();
  private InputMgr inputMgr = new InputMgr();
  private MetricsMgr metricsMgr = new MetricsMgr();

  public static Map<String, Object> globalMap = null;
  private String[] inputParams;

  private List<Map<String, Object>> globalConfigList = new ArrayList<Map<String, Object>>();
  private List<Map<String, Object>> inputConfigList = new ArrayList<Map<String, Object>>();
  private List<Map<String, Object>> filterConfigList = new ArrayList<Map<String, Object>>();
  private List<Map<String, Object>> outputConfigList = new ArrayList<Map<String, Object>>();
  
  private int checkPointCleanIntervalMS = 24 * 60 * 60 * 60 * 1000; // 24 hours
  private long lastCheckPointCleanedMS = 0;
  
  private static boolean isLogfeederCompleted = false;
  
  private Thread statLoggerThread = null;

  private LogFeeder(String[] args) {
    inputParams = args;
  }

  private void init() throws Throwable {

    LogFeederUtil.loadProperties("logfeeder.properties", inputParams);

    String configFiles = LogFeederUtil.getStringProperty("logfeeder.config.files");
    logger.info("logfeeder.config.files=" + configFiles);
    
    String[] configFileList = null;
    if (configFiles != null) {
      configFileList = configFiles.split(",");
    }
    //list of config those are there in cmd line config dir , end with .json
    String[] cmdLineConfigs = getConfigFromCmdLine();
    //merge both config
    String mergedConfigList[] = LogFeederUtil.mergeArray(configFileList,
        cmdLineConfigs);
    //mergedConfigList is null then set default conifg 
    if (mergedConfigList == null || mergedConfigList.length == 0) {
      mergedConfigList = LogFeederUtil.getStringProperty("config.file",
          "config.json").split(",");
    }
    for (String configFileName : mergedConfigList) {
      logger.info("Going to load config file:" + configFileName);
      //escape space from config file path
      configFileName= configFileName.replace("\\ ", "%20");
      File configFile = new File(configFileName);
      if (configFile.exists() && configFile.isFile()) {
        logger.info("Config file exists in path."
          + configFile.getAbsolutePath());
        loadConfigsUsingFile(configFile);
      } else {
        // Let's try to load it from class loader
        logger.info("Trying to load config file from classloader: "
          + configFileName);
        loadConfigsUsingClassLoader(configFileName);
        logger.info("Loaded config file from classloader: "
          + configFileName);
      }
    }
    
    addSimulatedInputs();
    
    mergeAllConfigs();
    
    LogfeederScheduler.INSTANCE.start();
    
    outMgr.setOutputList(outputList);
    for (Output output : outputList) {
      output.init();
    }
    inputMgr.init();
    metricsMgr.init();
    logger.debug("==============");
  }

  private void loadConfigsUsingClassLoader(String configFileName) throws Exception {
    BufferedInputStream fileInputStream = null;
    BufferedReader br = null;
    try {
      fileInputStream = (BufferedInputStream) this
        .getClass().getClassLoader()
        .getResourceAsStream(configFileName);
      if (fileInputStream != null) {
        br = new BufferedReader(new InputStreamReader(
          fileInputStream));
        String configData = readFile(br);
        loadConfigs(configData);
      } else {
        throw new Exception("Can't find configFile=" + configFileName);
      }
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
        }
      }

      if (fileInputStream != null) {
        try {
          fileInputStream.close();
        } catch (IOException e) {
        }
      }
    }
  }

  /**
   * This method loads the configurations from the given file.
   */
  private void loadConfigsUsingFile(File configFile) throws Exception {
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(configFile);
      BufferedReader br = new BufferedReader(new InputStreamReader(
        fileInputStream));
      String configData = readFile(br);
      loadConfigs(configData);
    } catch (Exception t) {
      logger.error("Error opening config file. configFilePath="
        + configFile.getAbsolutePath());
      throw t;
    } finally {
      if (fileInputStream != null) {
        try {
          fileInputStream.close();
        } catch (Throwable t) {
          // ignore
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void loadConfigs(String configData) throws Exception {
    Type type = new TypeToken<Map<String, Object>>() {
    }.getType();
    Map<String, Object> configMap = LogFeederUtil.getGson().fromJson(
      configData, type);

    // Get the globals
    for (String key : configMap.keySet()) {
      if (key.equalsIgnoreCase("global")) {
        globalConfigList.add((Map<String, Object>) configMap.get(key));
      } else if (key.equalsIgnoreCase("input")) {
        List<Map<String, Object>> mapList = (List<Map<String, Object>>) configMap
          .get(key);
        inputConfigList.addAll(mapList);
      } else if (key.equalsIgnoreCase("filter")) {
        List<Map<String, Object>> mapList = (List<Map<String, Object>>) configMap
          .get(key);
        filterConfigList.addAll(mapList);
      } else if (key.equalsIgnoreCase("output")) {
        List<Map<String, Object>> mapList = (List<Map<String, Object>>) configMap
          .get(key);
        outputConfigList.addAll(mapList);
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
    globalMap = mergeConfigs(globalConfigList);

    sortBlocks(filterConfigList);
    // First loop for output
    for (Map<String, Object> map : outputConfigList) {
      if (map == null) {
        continue;
      }
      mergeBlocks(globalMap, map);

      String value = (String) map.get("destination");
      Output output;
      if (value == null || value.isEmpty()) {
        logger.error("Output block doesn't have destination element");
        continue;
      }
      String classFullName = AliasUtil.getInstance().readAlias(value, ALIAS_TYPE.OUTPUT, ALIAS_PARAM.KLASS);
      if (classFullName == null || classFullName.isEmpty()) {
        logger.error("Destination block doesn't have output element");
        continue;
      }
      output = (Output) LogFeederUtil.getClassInstance(classFullName, ALIAS_TYPE.OUTPUT);

      if (output == null) {
        logger.error("Destination Object is null");
        continue;
      }

      output.setDestination(value);
      output.loadConfig(map);

      // We will only check for is_enabled out here. Down below we will
      // check whether this output is enabled for the input
      boolean isEnabled = output.getBooleanValue("is_enabled", true);
      if (isEnabled) {
        outputList.add(output);
        output.logConfgs(Level.INFO);
      } else {
        logger.info("Output is disabled. So ignoring it. "
          + output.getShortDescription());
      }
    }

    // Second loop for input
    for (Map<String, Object> map : inputConfigList) {
      if (map == null) {
        continue;
      }
      mergeBlocks(globalMap, map);

      String value = (String) map.get("source");
      Input input;
      if (value == null || value.isEmpty()) {
        logger.error("Input block doesn't have source element");
        continue;
      }
      String classFullName = AliasUtil.getInstance().readAlias(value, ALIAS_TYPE.INPUT, ALIAS_PARAM.KLASS);
      if (classFullName == null || classFullName.isEmpty()) {
        logger.error("Source block doesn't have source element");
        continue;
      }
      input = (Input) LogFeederUtil.getClassInstance(classFullName, ALIAS_TYPE.INPUT);

      if (input == null) {
        logger.error("Source Object is null");
        continue;
      }

      input.setType(value);
      input.loadConfig(map);

      if (input.isEnabled()) {
        input.setOutputMgr(outMgr);
        input.setInputMgr(inputMgr);
        inputMgr.add(input);
        input.logConfgs(Level.INFO);
      } else {
        logger.info("Input is disabled. So ignoring it. "
          + input.getShortDescription());
      }
    }

    // Third loop is for filter, but we will have to create a filter
    // instance for each input, so it can maintain the state per input
    List<Input> toRemoveInputList = new ArrayList<Input>();
    for (Input input : inputMgr.getInputList()) {
      Filter prevFilter = null;
      for (Map<String, Object> map : filterConfigList) {
        if (map == null) {
          continue;
        }
        mergeBlocks(globalMap, map);

        String value = (String) map.get("filter");
        Filter filter;
        if (value == null || value.isEmpty()) {
          logger.error("Filter block doesn't have filter element");
          continue;
        }

        String classFullName = AliasUtil.getInstance().readAlias(value, ALIAS_TYPE.FILTER, ALIAS_PARAM.KLASS);
        if (classFullName == null || classFullName.isEmpty()) {
          logger.error("Filter block doesn't have filter element");
          continue;
        }
        filter = (Filter) LogFeederUtil.getClassInstance(classFullName, ALIAS_TYPE.FILTER);

        if (filter == null) {
          logger.error("Filter Object is null");
          continue;
        }
        filter.loadConfig(map);
        filter.setInput(input);

        if (filter.isEnabled()) {
          filter.setOutputMgr(outMgr);
          if (prevFilter == null) {
            input.setFirstFilter(filter);
          } else {
            prevFilter.setNextFilter(filter);
          }
          prevFilter = filter;
          filter.logConfgs(Level.INFO);
        } else {
          logger.debug("Ignoring filter "
            + filter.getShortDescription() + " for input "
            + input.getShortDescription());
        }
      }
      if (input.getFirstFilter() == null) {
        toRemoveInputList.add(input);
      }
    }

    // Fourth loop is for associating valid outputs to input
    Set<Output> usedOutputSet = new HashSet<Output>();
    for (Input input : inputMgr.getInputList()) {
      for (Output output : outputList) {
        boolean ret = LogFeederUtil.isEnabled(output.getConfigs(),
          input.getConfigs());
        if (ret) {
          usedOutputSet.add(output);
          input.addOutput(output);
        }
      }
    }
    outputList = usedOutputSet;

    for (Input toRemoveInput : toRemoveInputList) {
      logger.warn("There are no filters, we will ignore this input. "
        + toRemoveInput.getShortDescription());
      inputMgr.removeInput(toRemoveInput);
    }
  }

  private void sortBlocks(List<Map<String, Object>> blockList) {

    Collections.sort(blockList, new Comparator<Map<String, Object>>() {

      @Override
      public int compare(Map<String, Object> o1, Map<String, Object> o2) {
        Object o1Sort = o1.get("sort_order");
        Object o2Sort = o2.get("sort_order");
        if (o1Sort == null) {
          return 0;
        }
        if (o2Sort == null) {
          return 0;
        }
        int o1Value = 0;
        if (!(o1Sort instanceof Number)) {
          try {
            o1Value = (new Double(Double.parseDouble(o1Sort
              .toString()))).intValue();
          } catch (Throwable t) {
            logger.error("Value is not of type Number. class="
              + o1Sort.getClass().getName() + ", value="
              + o1Sort.toString() + ", map=" + o1.toString());
          }
        } else {
          o1Value = ((Number) o1Sort).intValue();
        }
        int o2Value = 0;
        if (!(o2Sort instanceof Integer)) {
          try {
            o2Value = (new Double(Double.parseDouble(o2Sort
              .toString()))).intValue();
          } catch (Throwable t) {
            logger.error("Value is not of type Number. class="
              + o2Sort.getClass().getName() + ", value="
              + o2Sort.toString() + ", map=" + o2.toString());
          }
        } else {

        }
        return o1Value - o2Value;
      }
    });
  }

  private Map<String, Object> mergeConfigs(
    List<Map<String, Object>> configList) {
    Map<String, Object> mergedConfig = new HashMap<String, Object>();
    for (Map<String, Object> config : configList) {
      mergeBlocks(config, mergedConfig);
    }
    return mergedConfig;
  }

  private void mergeBlocks(Map<String, Object> fromMap,
                           Map<String, Object> toMap) {
    // Merge the non-string
    for (String key : fromMap.keySet()) {
      Object objValue = fromMap.get(key);
      if (objValue == null) {
        continue;
      }
      if (objValue instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> globalFields = LogFeederUtil
          .cloneObject((Map<String, Object>) fromMap.get(key));

        @SuppressWarnings("unchecked")
        Map<String, Object> localFields = (Map<String, Object>) toMap
          .get(key);
        if (localFields == null) {
          localFields = new HashMap<String, Object>();
          toMap.put(key, localFields);
        }

        if (globalFields != null) {
          for (String fieldKey : globalFields.keySet()) {
            if (!localFields.containsKey(fieldKey)) {
              localFields.put(fieldKey,
                globalFields.get(fieldKey));
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

  private void monitor() throws Exception {
    inputMgr.monitor();
    JVMShutdownHook logfeederJVMHook = new JVMShutdownHook();
    ShutdownHookManager.get().addShutdownHook(logfeederJVMHook,
        LOGFEEDER_SHUTDOWN_HOOK_PRIORITY);
    
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
            logger.error(
              "LogStats: Caught exception while logging stats.",
              t);
          }

          if (System.currentTimeMillis() > (lastCheckPointCleanedMS + checkPointCleanIntervalMS)) {
            lastCheckPointCleanedMS = System.currentTimeMillis();
            inputMgr.cleanCheckPointFiles();
          }

          // logfeeder is stopped then break the loop
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
    inputMgr.logStats();
    outMgr.logStats();

    if (metricsMgr.isMetricsEnabled()) {
      List<MetricCount> metricsList = new ArrayList<MetricCount>();
      inputMgr.addMetricsContainers(metricsList);
      outMgr.addMetricsContainers(metricsList);
      metricsMgr.useMetrics(metricsList);
    }
  }

  private String readFile(BufferedReader br) throws Exception {
    try {
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();
      while (line != null) {
        sb.append(line);
        line = br.readLine();
      }
      return sb.toString();
    } catch (Exception t) {
      logger.error("Error loading properties file.", t);
      throw t;
    }
  }

  public Collection<Output> getOutputList() {
    return outputList;
  }

  public OutputMgr getOutMgr() {
    return outMgr;
  }

  public static void main(String[] args) {
    LogFeeder logFeeder = new LogFeeder(args);
    logFeeder.run();
  }

  public void run() {
    try {
      Date startTime = new Date();
      this.init();
      Date endTime = new Date();
      logger.info("Took " + (endTime.getTime() - startTime.getTime())
        + " ms to initialize");
      this.monitor();
      //wait for all background thread before stop main thread
      this.waitOnAllDaemonThreads();
    } catch (Throwable t) {
      logger.fatal("Caught exception in main.", t);
      System.exit(1);
    }
  }

  private class JVMShutdownHook extends Thread {

    public void run() {
      try {
        logger.info("Processing is shutting down.");

        inputMgr.close();
        outMgr.close();
        inputMgr.checkInAll();

        logStats();

        logger.info("LogSearch is exiting.");
      } catch (Throwable t) {
        // Ignore
      }
    }
  }
  
  private void waitOnAllDaemonThreads() {
    String foreground = LogFeederUtil.getStringProperty("foreground");
    if (foreground != null && foreground.equalsIgnoreCase("true")) {
      inputMgr.waitOnAllInputs();
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
  
  private String[] getConfigFromCmdLine() {
    String inputConfigDir = LogFeederUtil.getStringProperty("input_config_dir");
    if (inputConfigDir != null && !inputConfigDir.isEmpty()) {
      String[] searchFileWithExtensions = new String[] { "json" };
      File configDirFile = new File(inputConfigDir);
      List<File> configFiles = FileUtil.getAllFileFromDir(configDirFile,
          searchFileWithExtensions, false);
      if (configFiles != null && configFiles.size() > 0) {
        String configPaths[] = new String[configFiles.size()];
        for (int index = 0; index < configFiles.size(); index++) {
          File configFile = configFiles.get(index);
          String configFilePath = configFile.getAbsolutePath();
          configPaths[index] = configFilePath;
        }
        return configPaths;
      }
    }
    return new String[0];
  }
}
