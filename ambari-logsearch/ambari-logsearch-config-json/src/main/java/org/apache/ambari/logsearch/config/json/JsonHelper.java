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
package org.apache.ambari.logsearch.config.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

/**
 * Utility class to help JSON operations.
 */
public class JsonHelper {

  private JsonHelper() {
  }

  public static JsonElement mergeGlobalConfigWithInputConfig(JsonParser parser, String inputConfig, JsonArray globalConfigNode) {
    JsonElement inputConfigJson = parser.parse(inputConfig);
    for (Map.Entry<String, JsonElement> typeEntry : inputConfigJson.getAsJsonObject().entrySet()) {
      for (JsonElement e : typeEntry.getValue().getAsJsonArray()) {
        for (JsonElement globalConfig : globalConfigNode) {
          merge(globalConfig.getAsJsonObject(), e.getAsJsonObject());
        }
      }
    }
    return inputConfigJson;
  }

  public static void merge(JsonObject source, JsonObject target) {
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
