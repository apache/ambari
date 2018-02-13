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

package org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

public class InputAdapter implements JsonDeserializer<InputDescriptorImpl> {
  private static JsonArray globalConfigs;
  public static void setGlobalConfigs(JsonArray globalConfigs_) {
    globalConfigs = globalConfigs_;
  }
  
  @Override
  public InputDescriptorImpl deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    String source = null;
    if (json.getAsJsonObject().has("source")) {
      source = json.getAsJsonObject().get("source").getAsString();
    } else {
      for (JsonElement e : globalConfigs) {
        if (e.getAsJsonObject().has("source")) {
          source = e.getAsJsonObject().get("source").getAsString();
          break;
        }
      }
    }
    
    switch (source) {
      case "file":
        return (InputDescriptorImpl)context.deserialize(json, InputFileDescriptorImpl.class);
      case "s3_file":
        return (InputDescriptorImpl)context.deserialize(json, InputS3FileDescriptorImpl.class);
      case "custom":
        return (InputDescriptorImpl)context.deserialize(json, InputCustomDescriptorImpl.class);
        default:
        throw new IllegalArgumentException("Unknown input type: " + json.getAsJsonObject().get("source").getAsString());
    }
  }
}
