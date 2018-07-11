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
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Helper class to convert between json string and InputConfig class.
 */
public class InputConfigGson {
  public static Gson gson;
  static {
    Type inputType = new TypeToken<InputDescriptorImpl>() {}.getType();
    Type filterType = new TypeToken<FilterDescriptorImpl>() {}.getType();
    Type postMapValuesType = new TypeToken<List<PostMapValuesImpl>>() {}.getType();
    gson = new GsonBuilder()
        .registerTypeAdapter(inputType, new InputAdapter())
        .registerTypeAdapter(filterType, new FilterAdapter())
        .registerTypeAdapter(postMapValuesType, new PostMapValuesAdapter())
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .create();
  }
}
