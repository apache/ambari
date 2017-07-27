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
package org.apache.ambari.logfeeder.input;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.filter.FilterJSON;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.FilterJsonDescriptorImpl;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputDescriptorImpl;
import org.apache.commons.collections.MapUtils;
import org.apache.solr.common.util.Base64;

import com.google.common.base.Joiner;

import static org.apache.ambari.logfeeder.util.LogFeederUtil.LOGFEEDER_PROPERTIES_FILE;

public class InputSimulate extends Input {
  private static final String LOG_TEXT_PATTERN = "{ logtime=\"%d\", level=\"%s\", log_message=\"%s\", host=\"%s\"}";
  
  private static final String DEFAULT_LOG_LEVEL = "WARN";
  @LogSearchPropertyDescription(
    name = "logfeeder.simulate.log_level",
    description = "The log level to create the simulated log entries with.",
    examples = {"INFO"},
    defaultValue = DEFAULT_LOG_LEVEL,
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  private static final String LOG_LEVEL_PROPERTY = "logfeeder.simulate.log_level";
  
  private static final int DEFAULT_NUMBER_OF_WORDS = 1000;
  @LogSearchPropertyDescription(
    name = "logfeeder.simulate.number_of_words",
    description = "The size of the set of words that may be used to create the simulated log entries with.",
    examples = {"100"},
    defaultValue = DEFAULT_NUMBER_OF_WORDS + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  private static final String NUMBER_OF_WORDS_PROPERTY = "logfeeder.simulate.number_of_words";
  
  private static final int DEFAULT_MIN_LOG_WORDS = 5;
  @LogSearchPropertyDescription(
    name = "logfeeder.simulate.min_log_words",
    description = "The minimum number of words in a simulated log entry.",
    examples = {"3"},
    defaultValue = DEFAULT_MIN_LOG_WORDS + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  private static final String MIN_LOG_WORDS_PROPERTY = "logfeeder.simulate.min_log_words";
  
  private static final int DEFAULT_MAX_LOG_WORDS = 5;
  @LogSearchPropertyDescription(
    name = "logfeeder.simulate.max_log_words",
    description = "The maximum number of words in a simulated log entry.",
    examples = {"8"},
    defaultValue = DEFAULT_MAX_LOG_WORDS + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  private static final String MAX_LOG_WORDS_PROPERTY = "logfeeder.simulate.max_log_words";
  
  private static final int DEFAULT_SLEEP_MILLISECONDS = 10000;
  @LogSearchPropertyDescription(
    name = "logfeeder.simulate.sleep_milliseconds",
    description = "The milliseconds to sleep between creating two simulated log entries.",
    examples = {"5000"},
    defaultValue = DEFAULT_SLEEP_MILLISECONDS + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  private static final String SLEEP_MILLISECONDS_PROPERTY = "logfeeder.simulate.sleep_milliseconds";
  
  @LogSearchPropertyDescription(
    name = "logfeeder.simulate.log_ids",
    description = "The comma separated list of log ids for which to create the simulated log entries.",
    examples = {"ambari_server,zookeeper,infra_solr,logsearch_app"},
    defaultValue = "The log ids of the installed services in the cluster",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  private static final String LOG_IDS_PROPERTY = "logfeeder.simulate.log_ids";
  
  private static final Map<String, String> typeToFilePath = new HashMap<>();
  private static final List<String> inputTypes = new ArrayList<>();
  public static void loadTypeToFilePath(List<InputDescriptor> inputList) {
    for (InputDescriptor input : inputList) {
      typeToFilePath.put(input.getType(), input.getPath());
      inputTypes.add(input.getType());
    }
  }
  
  private static final Map<String, Integer> typeToLineNumber = new HashMap<>();
  
  private static final AtomicInteger hostNumber = new AtomicInteger(0);
  
  private static final List<Output> simulateOutputs = new ArrayList<>();
  public static List<Output> getSimulateOutputs() {
    return simulateOutputs;
  }
  
  private final Random random = new Random(System.currentTimeMillis());
  
  private final List<String> types;
  private final String level;
  private final int numberOfWords;
  private final int minLogWords;
  private final int maxLogWords;
  private final long sleepMillis;
  private final String host;
  
  public InputSimulate() throws Exception {
    this.types = getSimulatedLogTypes();
    this.level = LogFeederUtil.getStringProperty(LOG_LEVEL_PROPERTY, DEFAULT_LOG_LEVEL);
    this.numberOfWords = LogFeederUtil.getIntProperty(NUMBER_OF_WORDS_PROPERTY, DEFAULT_NUMBER_OF_WORDS, 50, 1000000);
    this.minLogWords = LogFeederUtil.getIntProperty(MIN_LOG_WORDS_PROPERTY, DEFAULT_MIN_LOG_WORDS, 1, 10);
    this.maxLogWords = LogFeederUtil.getIntProperty(MAX_LOG_WORDS_PROPERTY, DEFAULT_MAX_LOG_WORDS, 10, 20);
    this.sleepMillis = LogFeederUtil.getIntProperty(SLEEP_MILLISECONDS_PROPERTY, DEFAULT_SLEEP_MILLISECONDS);
    this.host = "#" + hostNumber.incrementAndGet() + "-" + LogFeederUtil.hostName;
    
    Filter filter = new FilterJSON();
    filter.loadConfig(new FilterJsonDescriptorImpl());
    filter.setInput(this);
    addFilter(filter);
  }
  
  private List<String> getSimulatedLogTypes() {
    String logsToSimulate = LogFeederUtil.getStringProperty(LOG_IDS_PROPERTY);
    return (logsToSimulate == null) ?
      inputTypes :
      Arrays.asList(logsToSimulate.split(","));
  }

  @Override
  public void addOutput(Output output) {
    try {
      Class<? extends Output> clazz = output.getClass();
      Output outputCopy = clazz.newInstance();
      outputCopy.loadConfig(output.getConfigs());
      outputCopy.setDestination(output.getDestination());
      simulateOutputs.add(outputCopy);
      super.addOutput(outputCopy);
    } catch (Exception e) {
      LOG.warn("Could not copy Output class " + output.getClass() + ", using original output");
      super.addOutput(output);
    }
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  void start() throws Exception {
    getFirstFilter().setOutputManager(outputManager);
    while (true) {
      if (types.isEmpty()) {
        try { Thread.sleep(sleepMillis); } catch(Exception e) { /* Ignore */ }
        continue;
      }
      String type = imitateRandomLogFile();
      
      String line = getLine();
      InputMarker marker = getInputMarker(type);
      
      outputLine(line, marker);
      
      try { Thread.sleep(sleepMillis); } catch(Exception e) { /* Ignore */ }
    }
  }

  private String imitateRandomLogFile() {
    int typePos = random.nextInt(types.size());
    String type = types.get(typePos);
    String filePath = MapUtils.getString(typeToFilePath, type, "path of " + type);
    
    ((InputDescriptorImpl)inputDescriptor).setType(type);
    setFilePath(filePath);
    
    return type;
  }

  private InputMarker getInputMarker(String type) throws Exception {
    InputMarker marker = new InputMarker(this, getBase64FileKey(), getLineNumber(type));
    return marker;
  }

  private static synchronized int getLineNumber(String type) {
    if (!typeToLineNumber.containsKey(type)) {
      typeToLineNumber.put(type, 0);
    }
    Integer lineNumber = typeToLineNumber.get(type) + 1;
    
    typeToLineNumber.put(type, lineNumber);
    return lineNumber;
  }

  private String getBase64FileKey() throws Exception {
    String fileKey = InetAddress.getLocalHost().getHostAddress() + "|" + filePath;
    return Base64.byteArrayToBase64(fileKey.getBytes());
  }

  private String getLine() {
    Date d = new Date();
    String logMessage = createLogMessage();
    return String.format(LOG_TEXT_PATTERN, d.getTime(), level, logMessage, host);
  }
  
  private String createLogMessage() {
    int logMessageLength = minLogWords + random.nextInt(maxLogWords - minLogWords + 1);
    Set<Integer> words = new TreeSet<>();
    List<String> logMessage = new ArrayList<>();
    while (words.size() < logMessageLength) {
      int word = random.nextInt(numberOfWords);
      if (words.add(word)) {
        logMessage.add(String.format("Word%06d", word));
      }
    }
    
    return Joiner.on(' ').join(logMessage);
  }

  @Override
  public void checkIn(InputMarker inputMarker) {}

  @Override
  public void lastCheckIn() {}
  
  @Override
  public String getNameForThread() {
    return "Simulated input";
  }

  @Override
  public String getShortDescription() {
    return "Simulated input";
  }
}
