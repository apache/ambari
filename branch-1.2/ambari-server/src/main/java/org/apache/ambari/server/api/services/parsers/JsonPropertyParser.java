/**
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

package org.apache.ambari.server.api.services.parsers;

import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * JSON parser which parses a JSON string into a map of properties and values.
 */
public class JsonPropertyParser implements RequestBodyParser {
  private Set<Map<String, Object>> m_setProperties = new HashSet<Map<String, Object>>();


  @Override
  public Set<Map<String, Object>> parse(String s) {

    ObjectMapper mapper = new ObjectMapper();

    if (s != null && ! s.isEmpty()) {
      s = ensureArrayFormat(s);
      try {
        JsonNode[] nodes = mapper.readValue(s, JsonNode[].class);
        for(JsonNode node : nodes) {
          Map<String, Object> mapProperties = new HashMap<String, Object>();
          processNode(node, "", mapProperties);
          m_setProperties.add(mapProperties);
        }
      } catch (IOException e) {
        throw new RuntimeException("Unable to parse json: " + e, e);
      }
    }
    return m_setProperties;
  }

  private void processNode(JsonNode node, String path, Map<String, Object> mapProperties) {
    Iterator<String> iter = node.getFieldNames();
    String name;
    while (iter.hasNext()) {
      name = iter.next();
      JsonNode child = node.get(name);
      if (child.isContainerNode()) {
        processNode(child, path.isEmpty() ? name : path + '.' + name, mapProperties);
      } else {
        mapProperties.put(PropertyHelper.getPropertyId(path, name), child.asText());
      }
    }
  }

  private String ensureArrayFormat(String s) {
    return s.startsWith("[") ? s : '[' + s + ']';
  }
}
