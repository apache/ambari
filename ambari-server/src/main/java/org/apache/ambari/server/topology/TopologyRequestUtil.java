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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

public class TopologyRequestUtil {

  private static final Logger LOG = LoggerFactory.getLogger(TopologyRequestUtil.class);
  public static final String NAME = "name";
  public static final String VERSION = "version";


  /**
   * @param rawRequestJson The topology request in raw JSON format. Null input is handled gracefully.
   * @return a Set of stack id's contained in the request
   */
  public static Set<StackId> getStackIdsFromRequest(String rawRequestJson) {
    return getStackIdsFromRequest(getPropertyMap(rawRequestJson));
  }


  /**
   * @param rawRequestMap The topology request in raw JSON format. Null input is handled gracefully.
   * @return a Set of stack id's contained in the request
   */
  public static Set<StackId> getStackIdsFromRequest(Map<String, Object> rawRequestMap) {
    return rawRequestMap.entrySet().stream().
      filter(e -> "mpack_instances".equals(e.getKey())).
      flatMap(e -> ((List<Map<String, String>>) e.getValue()).stream()).
      map(m -> {
        checkArgument(m.containsKey(NAME), "Missing mpack name");
        checkArgument(m.containsKey(VERSION), "Missing mpack version");
        return new StackId(m.get(NAME), m.get(VERSION));
      }).
      collect(toSet());
  }

  /**
   * @param rawRequestJson The topology request in raw JSON format. Null input is handled gracefully.
   * @return the request body parsed as map (null is parsed as empty map)
   */
  public static Map<String, Object> getPropertyMap(String rawRequestJson) {
    return null == rawRequestJson ?
      emptyMap() :
      JsonUtils.fromJson(rawRequestJson, new TypeReference<Map<String, Object>>() {});
  }
}
