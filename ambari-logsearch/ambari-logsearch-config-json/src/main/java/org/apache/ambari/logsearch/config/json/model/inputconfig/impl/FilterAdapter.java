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

package org.apache.ambari.logsearch.config.json.model.inputconfig.impl;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

public class FilterAdapter implements JsonDeserializer<FilterDescriptorImpl> {
  @Override
  public FilterDescriptorImpl deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    switch (json.getAsJsonObject().get("filter").getAsString()) {
      case "grok":
        return (FilterDescriptorImpl)context.deserialize(json, FilterGrokDescriptorImpl.class);
      case "keyvalue":
        return (FilterDescriptorImpl)context.deserialize(json, FilterKeyValueDescriptorImpl.class);
      case "json":
        return (FilterDescriptorImpl)context.deserialize(json, FilterJsonDescriptorImpl.class);
      default:
        throw new IllegalArgumentException("Unknown filter type: " + json.getAsJsonObject().get("filter").getAsString());
    }
  }
}
