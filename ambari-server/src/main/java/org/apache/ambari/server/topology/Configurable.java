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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public interface Configurable {
  void setConfiguration(Configuration configuration);
  Configuration getConfiguration();

  @JsonProperty("configurations")
  default void setConfigs(Collection<Map<String, Map<String, Map<String, ?>>>> configs) {
    if (null != configs) {
      Map<String, Map<String, String>> allProps = configs.stream().
        filter(map -> map != null && !map.isEmpty() && map.values().iterator().next().get(Configuration.PROPERTIES_KEY) != null).
        collect(toMap(
          config -> config.keySet().iterator().next(),
          config -> (Map<String, String>)config.values().iterator().next().get(Configuration.PROPERTIES_KEY)
        ));
      Map<String, Map<String, Map<String, String>>> allAttributes = configs.stream().
        filter(map -> map != null && !map.isEmpty() && map.values().iterator().next().get(Configuration.ATTRIBUTES_KEY) != null).
        collect(toMap(
          config -> config.keySet().iterator().next(),
          config -> (Map<String, Map<String, String>>)
            config.values().iterator().next().get(Configuration.ATTRIBUTES_KEY)
        ));
      setConfiguration(new Configuration(allProps, allAttributes));
    }
  }

  @JsonProperty("configurations")
  default Collection<Map<String, Map<String, Map<String, ?>>>> getConfigs() {
    Configuration config = getConfiguration();
    if (config != null) {
      Set<String> keys = Sets.union(config.getProperties().keySet(), config.getAttributes().keySet());
      return keys.stream().map(key -> {
        Map<String, Map<String, ?>> map = new HashMap<>(2);
        if (config.getProperties().containsKey(key)) {
          map.put(Configuration.PROPERTIES_KEY, config.getProperties().get(key));
        }
        if (config.getAttributes().containsKey(key)) {
          map.put(Configuration.ATTRIBUTES_KEY, config.getAttributes().get(key));
        }
        return ImmutableMap.of(key, map);
      }).collect(toList());
    }
    else {
      return Collections.emptyList();
    }
  }

}
