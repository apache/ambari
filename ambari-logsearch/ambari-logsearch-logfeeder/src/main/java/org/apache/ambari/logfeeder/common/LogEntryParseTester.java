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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.output.Output;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputConfigGson;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputConfigImpl;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LogEntryParseTester {

  private final String logEntry;
  private final String shipperConfig;
  private final List<JsonObject> globalConfigs;
  private final String logId;
  
  public LogEntryParseTester(String logEntry, String shipperConfig, String globalConfigsJson, String logId) {
    this.logEntry = logEntry;
    this.shipperConfig = shipperConfig;
    this.globalConfigs = new ArrayList<>();
    this.logId = logId;
    
    JsonParser jsonParser = new JsonParser();
    JsonArray globalConfigArray = jsonParser.parse(globalConfigsJson).getAsJsonArray();
    for (JsonElement e : globalConfigArray) {
      globalConfigs.add(e.getAsJsonObject());
    }
  }
  
  public LogEntryParseTester(String logEntry, String shipperConfig, List<String> globalConfigJsons, String logId) {
    this.logEntry = logEntry;
    this.shipperConfig = shipperConfig;
    this.globalConfigs = new ArrayList<>();
    this.logId = logId;
    
    JsonParser jsonParser = new JsonParser();
    for (String globalConfig : globalConfigJsons) {
      JsonObject globalConfigObject = jsonParser.parse(globalConfig).getAsJsonObject();
      globalConfigs.add(globalConfigObject.get("global").getAsJsonObject());
    }
  }

  public Map<String, Object> parse() throws Exception {
    InputConfig inputConfig = getInputConfig();
    ConfigHandler configHandler = new ConfigHandler(null);
    Input input = configHandler.getTestInput(inputConfig, logId);
    final Map<String, Object> result = new HashMap<>();
    input.getFirstFilter().init(new LogFeederProps());
    input.addOutput(new Output() {
      @Override
      public void write(String block, InputMarker inputMarker) throws Exception {
      }
      
      @Override
      public void copyFile(File inputFile, InputMarker inputMarker) throws UnsupportedOperationException {
      }
      
      @Override
      public void write(Map<String, Object> jsonObj, InputMarker inputMarker) {
        result.putAll(jsonObj);
      }
    });
    input.outputLine(logEntry, new InputMarker(input, null, 0));
    
    return result.isEmpty() ?
        ImmutableMap.of("errorMessage", (Object)"Could not parse test log entry") :
        result;
  }

  private InputConfig getInputConfig() {
    JsonParser jsonParser = new JsonParser();
    JsonElement shipperConfigJson = jsonParser.parse(shipperConfig);
    for (JsonObject globalConfig : globalConfigs) {
      for (Map.Entry<String, JsonElement> typeEntry : shipperConfigJson.getAsJsonObject().entrySet()) {
        for (JsonElement e : typeEntry.getValue().getAsJsonArray()) {
          merge(globalConfig, e.getAsJsonObject());
        }
      }
    }
    return InputConfigGson.gson.fromJson(shipperConfigJson, InputConfigImpl.class);
  }
  
  private void merge(JsonObject source, JsonObject target) {
    for (Map.Entry<String, JsonElement> e : source.entrySet()) {
      if (!target.has(e.getKey())) {
        target.add(e.getKey(), e.getValue());
      } else {
        if (e.getValue().isJsonObject()) {
          JsonObject valueJson = (JsonObject)e.getValue();
          merge(valueJson, target.get(e.getKey()).getAsJsonObject());
        }
      }
    }
  }

}
