/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Configurable {
  void setConfiguration(Configuration configuration);

  @JsonProperty("configurations")
  default void setConfigs(Collection<Map<String, Map<String, Map<String, String>>>> configs) {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, Map<String, String>> attributes = new HashMap<>();
    configs.forEach( item -> {
      String configName = item.keySet().iterator().next();
      Map<String, Map<String, String>> configData = item.get(configName);
      if (configData.containsKey("properties")) {
        properties.put(configName, configData.get("properties"));
      }
//      if (configData.containsKey("attributes")) {
//        attributes.put(configName, configData.get("attributes"));
//      }
    });
    // TODO: handle attributes
    setConfiguration(new Configuration(properties, new HashMap<>()));
  }

}
