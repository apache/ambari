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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

public class BlueprintTestUtil {
  public static final String BLUEPRINT_30_LOCATION = "blueprint3.0/multi-instance-blueprint.json";

  public static Map<String, Object> getMultiInstanceBlueprintAsMap() {
    URL url = Resources.getResource(BLUEPRINT_30_LOCATION);
    try {
      Map<String, Object> blueprintMap = new ObjectMapper().readValue(url, new TypeReference<Map<String, Object>>(){});

      Map<String, Object> blueprintProps = (Map<String, Object>) blueprintMap.remove("Blueprints");
      blueprintProps.forEach( (key, value) -> blueprintMap.put("Blueprints/" + key, value));
      Map<String, Object> settingProps = (Map<String, Object>) blueprintMap.remove("settings");
      settingProps.forEach( (key, value) -> blueprintMap.put("settings/" + key, value));

      return convertListsToSets(blueprintMap);
    }
    catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static Map<String, Object> convertListsToSets(Map<String, Object> input) {
    Map<String, Object> converted = new HashMap<>();
    input.forEach( (k, v) -> {
      if (v instanceof List) {
        List<Object> list = (List<Object>)v;
        Set<Object> set = list.stream().map(e -> {
          if (e instanceof Map) {
            return convertListsToSets((Map<String, Object>)e);
          }
          else {
            return e;
          }
        }).collect(Collectors.toSet());
        converted.put(k, set);
      }
      else if (v instanceof Map){
        converted.put(k, convertListsToSets((Map<String, Object>)v));
      }
      else {
        converted.put(k, v);
      }
    });
    return converted;
  }


}
