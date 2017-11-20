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

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Configurable {
  void setConfiguration(Configuration configuration);
  Configuration getConfiguration();

  @JsonProperty("configurations")
  default void setConfigs(Collection<Map<String, Map<String, Map<String, String>>>> configs) {
    if (null != configs) {
      Map<String, Map<String, String>> allProps = configs.stream().
        filter(map -> map != null && !map.isEmpty() && map.values().iterator().next().get(Configuration.PROPERTIES_KEY) != null).
        collect(toMap(
          config -> config.keySet().iterator().next(),
          config -> config.values().iterator().next().get(Configuration.PROPERTIES_KEY)
        ));
      setConfiguration(new Configuration(allProps, new HashMap<>()));
    }
  }

  @JsonProperty("configurations")
  default Collection<Map<String, Map<String, Map<String, String>>>> getConfigs() {
    Configuration config = getConfiguration();
    return config != null
      ? config.getProperties().entrySet().stream()
        .map(e -> singletonMap(e.getKey(), singletonMap(Configuration.PROPERTIES_KEY, e.getValue())))
        .collect(toList())
      : Collections.emptyList();
  }

}
