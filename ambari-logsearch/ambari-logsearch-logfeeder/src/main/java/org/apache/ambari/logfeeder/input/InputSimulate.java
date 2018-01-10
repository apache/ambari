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

import com.google.common.base.Joiner;
import org.apache.ambari.logfeeder.conf.InputSimulateConfig;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.filter.FilterJSON;
import org.apache.ambari.logfeeder.plugin.filter.Filter;
import org.apache.ambari.logfeeder.plugin.output.Output;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputDescriptor;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.FilterJsonDescriptorImpl;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputDescriptorImpl;
import org.apache.commons.collections.MapUtils;
import org.apache.solr.common.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class InputSimulate extends InputFile {
  private static final Logger LOG = LoggerFactory.getLogger(InputSimulate.class);
  private static final String LOG_TEXT_PATTERN = "{ logtime=\"%d\", level=\"%s\", log_message=\"%s\", host=\"%s\"}";

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

  private InputSimulateConfig conf;
  private List<String> types;
  private String level;
  private int numberOfWords;
  private int minLogWords;
  private int maxLogWords;
  private long sleepMillis;
  private String host;

  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    super.init(logFeederProps);
    conf = logFeederProps.getInputSimulateConfig();
    this.types = getSimulatedLogTypes();
    this.level = conf.getSimulateLogLevel();
    this.numberOfWords = conf.getSimulateNumberOfWords();
    this.minLogWords = conf.getSimulateMinLogWords();
    this.maxLogWords = conf.getSimulateMaxLogWords();
    this.sleepMillis = conf.getSimulateSleepMilliseconds();
    this.host = "#" + hostNumber.incrementAndGet() + "-" + LogFeederUtil.hostName;

    Filter filter = new FilterJSON();
    filter.loadConfig(new FilterJsonDescriptorImpl());
    filter.setInput(this);
    addFilter(filter);

  }

  private List<String> getSimulatedLogTypes() {
    String logsToSimulate = conf.getSimulateLogIds();
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
  public void start() throws Exception {
    getFirstFilter().setOutputManager(getOutputManager());
    while (true) {
      if (types.isEmpty()) {
        try { Thread.sleep(sleepMillis); } catch(Exception e) { /* Ignore */ }
        continue;
      }
      String type = imitateRandomLogFile();

      String line = getLine();
      InputFileMarker marker = getInputMarker(type);

      outputLine(line, marker);

      try { Thread.sleep(sleepMillis); } catch(Exception e) { /* Ignore */ }
    }
  }

  private String imitateRandomLogFile() {
    int typePos = random.nextInt(types.size());
    String type = types.get(typePos);
    String filePath = MapUtils.getString(typeToFilePath, type, "path of " + type);

    ((InputDescriptorImpl)getInputDescriptor()).setType(type);
    setFilePath(filePath);

    return type;
  }

  private InputFileMarker getInputMarker(String type) throws Exception {
    return new InputFileMarker(this, getBase64FileKey(), getLineNumber(type));
  }

  private static synchronized int getLineNumber(String type) {
    if (!typeToLineNumber.containsKey(type)) {
      typeToLineNumber.put(type, 0);
    }
    Integer lineNumber = typeToLineNumber.get(type) + 1;

    typeToLineNumber.put(type, lineNumber);
    return lineNumber;
  }

  public String getBase64FileKey() throws Exception {
    String fileKey = InetAddress.getLocalHost().getHostAddress() + "|" + getFilePath();
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
  public void checkIn(InputFileMarker inputMarker) {}

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
