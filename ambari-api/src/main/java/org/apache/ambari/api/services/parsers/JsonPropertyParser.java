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

package org.apache.ambari.api.services.parsers;

import org.apache.ambari.api.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON parser which parses a JSON string into a map of properties and values.
 */
public class JsonPropertyParser implements RequestBodyParser {
  //todo: change value type to String when it is supported in back end
  private Map<PropertyId, String> m_properties = new HashMap<PropertyId, String>();

  @Override
  public Map<PropertyId, String> parse(String s) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      processNode(mapper.readValue(s, JsonNode.class), "");
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse json: " + e, e);
    }

    return m_properties;
  }

  private void processNode(JsonNode node, String path) {
    Iterator<String> iter = node.getFieldNames();
    String name;
    while (iter.hasNext()) {
      name = iter.next();
      JsonNode child = node.get(name);
      if (child.isContainerNode()) {
        processNode(child, path.isEmpty() ? name : path + '.' + name);
      } else {
        m_properties.put(PropertyHelper.getPropertyId(name, path), child.asText());
      }
    }
  }
}
