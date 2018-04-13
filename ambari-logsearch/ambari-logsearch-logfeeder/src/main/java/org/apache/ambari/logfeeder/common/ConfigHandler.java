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
package org.apache.ambari.logfeeder.common;

import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.InputSimulate;
import org.apache.ambari.logfeeder.plugin.common.AliasUtil;
import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.apache.ambari.logfeeder.plugin.filter.Filter;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.plugin.manager.InputManager;
import org.apache.ambari.logfeeder.plugin.manager.OutputManager;
import org.apache.ambari.logfeeder.plugin.output.Output;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.InputConfigMonitor;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterGrokDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.FilterDescriptorImpl;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputConfigImpl;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputDescriptorImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigHandler implements InputConfigMonitor {
  private static final Logger LOG = Logger.getLogger(org.apache.ambari.logfeeder.common.ConfigHandler.class);

  private final LogSearchConfigLogFeeder logSearchConfig;

  @Inject
  private InputManager inputManager;
  @Inject
  private OutputManager outputManager;
  @Inject
  private LogFeederProps logFeederProps;

  private final Map<String, Object> globalConfigs = new HashMap<>();
  private final List<String> globalConfigJsons = new ArrayList<>();

  private final List<InputDescriptor> inputConfigList = new ArrayList<>();
  private final List<FilterDescriptor> filterConfigList = new ArrayList<>();
  private final List<Map<String, Object>> outputConfigList = new ArrayList<>();

  private boolean simulateMode = false;

  public ConfigHandler(LogSearchConfigLogFeeder logSearchConfig) {
    this.logSearchConfig = logSearchConfig;
  }

  @PostConstruct
  public void init() throws Exception {
    loadConfigFiles();
    logSearchConfig.init(Maps.fromProperties(logFeederProps.getProperties()), logFeederProps.getClusterName());
    loadOutputs();
    simulateIfNeeded();

    inputManager.init();
    outputManager.init();

    logSearchConfig.monitorOutputProperties(outputManager.getOutputsToMonitor());
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

    String logFeederConfigFilesProperty = logFeederProps.getConfigFiles();
    LOG.info("logfeeder.config.files=" + logFeederConfigFilesProperty);
    if (logFeederConfigFilesProperty != null) {
      configFiles.addAll(Arrays.asList(logFeederConfigFilesProperty.split(",")));
    }

    return configFiles;
  }

  private void loadConfigsUsingFile(File configFile) throws Exception {
    try {
      String configData = FileUtils.readFileToString(configFile, Charset.defaultCharset());
      loadConfigs(configData);
    } catch (Exception t) {
      LOG.error("Error opening config file. configFilePath=" + configFile.getAbsolutePath());
      throw t;
    }
  }

  private void loadConfigsUsingClassLoader(String configFileName) throws Exception {
    try (BufferedInputStream fis = (BufferedInputStream) this.getClass().getClassLoader().getResourceAsStream(configFileName)) {
      ClassPathResource configFile = new ClassPathResource(configFileName);
      if (!configFile.exists()) {
        throw new FileNotFoundException(configFileName);
      }
      String configData = IOUtils.toString(fis, Charset.defaultCharset());
      loadConfigs(configData);
    }
  }

  @Override
  public void loadInputConfigs(String serviceName, InputConfig inputConfig) throws Exception {
    inputConfigList.clear();
    filterConfigList.clear();

    inputConfigList.addAll(inputConfig.getInput());
    filterConfigList.addAll(inputConfig.getFilter());

    if (simulateMode) {
      InputSimulate.loadTypeToFilePath(inputConfigList);
    } else {
      loadInputs(serviceName);
      loadFilters(serviceName);
      assignOutputsToInputs(serviceName);

      inputManager.startInputs(serviceName);
    }
  }

  @Override
  public void removeInputs(String serviceName) {
    inputManager.removeInputsForService(serviceName);
  }

  public Input getTestInput(InputConfig inputConfig, String logId) {
    for (InputDescriptor inputDescriptor : inputConfig.getInput()) {
      if (inputDescriptor.getType().equals(logId)) {
        inputConfigList.add(inputDescriptor);
        break;
      }
    }
    if (inputConfigList.isEmpty()) {
      throw new IllegalArgumentException("Log Id " + logId + " was not found in shipper configuriaton");
    }

    for (FilterDescriptor filterDescriptor : inputConfig.getFilter()) {
      if ("grok".equals(filterDescriptor.getFilter())) {
        // Thus ensure that the log entry passed will be parsed immediately
        ((FilterGrokDescriptor)filterDescriptor).setMultilinePattern(null);
      }
      filterConfigList.add(filterDescriptor);
    }
    loadInputs("test");
    loadFilters("test");
    List<Input> inputList = inputManager.getInputList("test");

    return inputList != null && inputList.size() == 1 ? inputList.get(0) : null;
  }

  @SuppressWarnings("unchecked")
  public void loadConfigs(String configData) throws Exception {
    Type type = new TypeToken<Map<String, Object>>() {}.getType();
    Map<String, Object> configMap = LogFeederUtil.getGson().fromJson(configData, type);

    // Get the globals
    for (String key : configMap.keySet()) {
      switch (key) {
        case "global" :
          globalConfigs.putAll((Map<String, Object>) configMap.get(key));
          globalConfigJsons.add(configData);
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

  @Override
  public List<String> getGlobalConfigJsons() {
    return globalConfigJsons;
  }

  private void simulateIfNeeded() throws Exception {
    int simulatedInputNumber = logFeederProps.getInputSimulateConfig().getSimulateInputNumber();
    if (simulatedInputNumber == 0)
      return;

    InputConfigImpl simulateInputConfig = new InputConfigImpl();
    List<InputDescriptorImpl> inputConfigDescriptors = new ArrayList<>();
    simulateInputConfig.setInput(inputConfigDescriptors);
    simulateInputConfig.setFilter(new ArrayList<FilterDescriptorImpl>());
    for (int i = 0; i < simulatedInputNumber; i++) {
      InputDescriptorImpl inputDescriptor = new InputDescriptorImpl() {};
      inputDescriptor.setSource("simulate");
      inputDescriptor.setRowtype("service");
      inputDescriptor.setAddFields(new HashMap<String, String>());
      inputConfigDescriptors.add(inputDescriptor);
    }

    loadInputConfigs("Simulation", simulateInputConfig);

    simulateMode = true;
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
      Output output = (Output) AliasUtil.getClassInstance(value, AliasUtil.AliasType.OUTPUT);
      if (output == null) {
        LOG.error("Output object could not be found");
        continue;
      }
      output.setDestination(value);
      output.loadConfig(map);
      output.setLogSearchConfig(logSearchConfig);

      // We will only check for is_enabled out here. Down below we will check whether this output is enabled for the input
      if (output.isEnabled()) {
        output.logConfigs();
        outputManager.add(output);
      } else {
        LOG.info("Output is disabled. So ignoring it. " + output.getShortDescription());
      }
    }
  }

  private void loadInputs(String serviceName) {
    for (InputDescriptor inputDescriptor : inputConfigList) {
      if (inputDescriptor == null) {
        continue;
      }

      String source = (String) inputDescriptor.getSource();
      if (StringUtils.isEmpty(source)) {
        LOG.error("Input block doesn't have source element");
        continue;
      }
      Input input = (Input) AliasUtil.getClassInstance(source, AliasUtil.AliasType.INPUT);
      if (input == null) {
        LOG.error("Input object could not be found");
        continue;
      }
      input.setType(source);
      input.loadConfig(inputDescriptor);

      if (input.isEnabled()) {
        input.setOutputManager(outputManager);
        input.setInputManager(inputManager);
        inputManager.add(serviceName, input);
        input.logConfigs();
      } else {
        LOG.info("Input is disabled. So ignoring it. " + input.getShortDescription());
      }
    }
  }

  private void loadFilters(String serviceName) {
    sortFilters();

    List<Input> toRemoveInputList = new ArrayList<Input>();
    for (Input input : inputManager.getInputList(serviceName)) {
      for (FilterDescriptor filterDescriptor : filterConfigList) {
        if (filterDescriptor == null) {
          continue;
        }
        if (BooleanUtils.isFalse(filterDescriptor.isEnabled())) {
          LOG.debug("Ignoring filter " + filterDescriptor.getFilter() + " because it is disabled");
          continue;
        }
        if (!input.isFilterRequired(filterDescriptor)) {
          LOG.debug("Ignoring filter " + filterDescriptor.getFilter() + " for input " + input.getShortDescription());
          continue;
        }

        String value = filterDescriptor.getFilter();
        if (StringUtils.isEmpty(value)) {
          LOG.error("Filter block doesn't have filter element");
          continue;
        }
        Filter filter = (Filter) AliasUtil.getClassInstance(value, AliasUtil.AliasType.FILTER);
        if (filter == null) {
          LOG.error("Filter object could not be found");
          continue;
        }
        filter.loadConfig(filterDescriptor);
        filter.setInput(input);

        filter.setOutputManager(outputManager);
        input.addFilter(filter);
        filter.logConfigs();
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
    Collections.sort(filterConfigList, (o1, o2) -> {
      Integer o1Sort = o1.getSortOrder();
      Integer o2Sort = o2.getSortOrder();
      if (o1Sort == null || o2Sort == null) {
        return 0;
      }

      return o1Sort - o2Sort;
    });
  }

  private void assignOutputsToInputs(String serviceName) {
    Set<Output> usedOutputSet = new HashSet<Output>();
    for (Input input : inputManager.getInputList(serviceName)) {
      for (Output output : outputManager.getOutputs()) {
        if (input.isOutputRequired(output)) {
          usedOutputSet.add(output);
          input.addOutput(output);
        }
      }
    }

    // In case of simulation copies of the output are added for each simulation instance, these must be added to the manager
    for (Output output : InputSimulate.getSimulateOutputs()) {
      output.setLogSearchConfig(logSearchConfig);
      outputManager.add(output);
      usedOutputSet.add(output);
    }
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

  public void cleanCheckPointFiles() {
    inputManager.cleanCheckPointFiles();
  }

  public void logStats() {
    inputManager.logStats();
    outputManager.logStats();
  }

  public void addMetrics(List<MetricData> metricsList) {
    inputManager.addMetricsContainers(metricsList);
    outputManager.addMetricsContainers(metricsList);
  }

  @PreDestroy
  public void close() {
    inputManager.close();
    outputManager.close();
    inputManager.checkInAll();
  }

  public void setInputManager(InputManager inputManager) {
    this.inputManager = inputManager;
  }

  public void setOutputManager(OutputManager outputManager) {
    this.outputManager = outputManager;
  }
}
