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

import org.apache.ambari.logfeeder.filter.Filter;
import org.apache.ambari.logfeeder.filter.FilterJSON;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.util.Base64;

public class InputSimulate extends Input {

  private static final String LOG_MESSAGE_PREFIX = "Simulated log message for testing, line";
  
  private static final String LOG_TEXT_PATTERN =
      "{ logtime=\"%d\", level=\"%s\", log_message=\"<LOG_MESSAGE_PATTERN>\"}";
  
  private static final Map<String, String> typeToFilePath = new HashMap<>();
  public static void loadTypeToFilePath(List<Map<String, Object>> inputList) {
    for (Map<String, Object> input : inputList) {
      if (input.containsKey("type") && input.containsKey("path")) {
        typeToFilePath.put((String)input.get("type"), (String)input.get("path"));
      }
    }
  }
  
  private static final Map<String, Integer> typeToLineNumber = new HashMap<>();
  
  private final Random random = new Random(System.currentTimeMillis());
  
  private final List<String> types;
  private final String level;
  private final String logText;
  private final long sleepMillis;
  
  public InputSimulate() throws Exception {
    this.types = getSimulatedLogTypes();
    this.level = LogFeederUtil.getStringProperty("logfeeder.simulate.log_level", "WARN");
    this.logText = getLogText();
    this.sleepMillis = LogFeederUtil.getIntProperty("logfeeder.simulate.sleep_milliseconds", 10000);
    
    Filter filter = new FilterJSON();
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

  private String getLogText() {
    int logTextSize = LogFeederUtil.getIntProperty("logfeeder.simulate.log_message_size", 100);
    int fillerSize = Math.max(logTextSize - LOG_MESSAGE_PREFIX.length() - 10, 0);
    String filler = StringUtils.repeat("X", fillerSize);
    String logMessagePattern = LOG_MESSAGE_PREFIX + " %08d " + filler;
    
    return LOG_TEXT_PATTERN.replaceAll("<LOG_MESSAGE_PATTERN>", logMessagePattern);
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
      
      InputMarker marker = getInputMarker(type);
      String line = getLine(marker);
      
      outputLine(line, marker);
      
      try { Thread.sleep(sleepMillis); } catch(Exception e) {}
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

  private String getLine(InputMarker marker) {
    Date d = new Date();
    return String.format(logText, d.getTime(), level, marker.lineNumber);
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
