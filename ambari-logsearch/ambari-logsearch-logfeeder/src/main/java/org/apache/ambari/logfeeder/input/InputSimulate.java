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
import java.util.Collections;
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
import org.apache.log4j.Logger;
import org.apache.solr.common.util.Base64;

import com.google.common.base.Joiner;

public class InputSimulate extends Input {
  private static final Logger LOG = Logger.getLogger(InputSimulate.class);

  private static final String LOG_TEXT_PATTERN = "{ logtime=\"%d\", level=\"%s\", log_message=\"%s\", host=\"%s\"}";
  
  private static final Map<String, String> typeToFilePath = new HashMap<>();
  public static void loadTypeToFilePath(List<Map<String, Object>> inputList) {
    for (Map<String, Object> input : inputList) {
      if (input.containsKey("type") && input.containsKey("path")) {
        typeToFilePath.put((String)input.get("type"), (String)input.get("path"));
      }
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
    this.level = LogFeederUtil.getStringProperty("logfeeder.simulate.log_level", "WARN");
    this.numberOfWords = LogFeederUtil.getIntProperty("logfeeder.simulate.number_of_words", 1000, 50, 1000000);
    this.minLogWords = LogFeederUtil.getIntProperty("logfeeder.simulate.min_log_words", 5, 1, 10);
    this.maxLogWords = LogFeederUtil.getIntProperty("logfeeder.simulate.max_log_words", 10, 10, 20);
    this.sleepMillis = LogFeederUtil.getIntProperty("logfeeder.simulate.sleep_milliseconds", 10000);
    this.host = "#" + hostNumber.incrementAndGet() + "-" + LogFeederUtil.hostName;
    
    Filter filter = new FilterJSON();
    filter.loadConfig(Collections.<String, Object> emptyMap());
    filter.setInput(this);
    addFilter(filter);
  }
  
  private List<String> getSimulatedLogTypes() {
    String logsToSimulate = LogFeederUtil.getStringProperty("logfeeder.simulate.log_ids");
    if (logsToSimulate == null) {
      return new ArrayList<>(typeToFilePath.keySet());
    } else {
      List<String> simulatedLogTypes = Arrays.asList(logsToSimulate.split(","));
      simulatedLogTypes.retainAll(typeToFilePath.keySet());
      return simulatedLogTypes;
    }
  }

  @Override
  public void addOutput(Output output) {
    try {
      Class<? extends Output> clazz = output.getClass();
      Output outputCopy = clazz.newInstance();
      outputCopy.loadConfig(output.getConfigs());
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
    if (types.isEmpty())
      return;
    
    getFirstFilter().setOutputManager(outputManager);
    while (true) {
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
    String filePath = typeToFilePath.get(type);
    
    configs.put("type", type);
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
