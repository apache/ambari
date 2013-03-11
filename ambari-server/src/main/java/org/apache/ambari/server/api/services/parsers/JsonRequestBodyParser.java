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

import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * JSON parser which parses a JSON string into a map of properties and values.
 */
public class JsonRequestBodyParser implements RequestBodyParser {
  /**
   *  Logger instance.
   */
  private final static Logger LOG = LoggerFactory.getLogger(JsonRequestBodyParser.class);

  private String m_body;

  @Override
  public RequestBody parse(String s) throws BodyParseException {
    m_body = s;
    RequestBody body = new RequestBody();

    if (s != null && s.length() != 0) {
      s = ensureArrayFormat(s);
      ObjectMapper mapper = new ObjectMapper();
      try {
        JsonNode root = mapper.readTree(s);

        Iterator<JsonNode> iter = root.getElements();
        while (iter.hasNext()) {
          Map<String, Object> mapProperties = new HashMap<String, Object>();
          NamedPropertySet propertySet = new NamedPropertySet("", mapProperties);
          JsonNode node = iter.next();
          processNode(node, "", propertySet, body);

          String query = (String) mapProperties.remove(QUERY_FIELD_PATH);
          if (query != null) {
            body.setQueryString(query);
          }
          if (propertySet.getProperties().size() != 0) {
            body.addPropertySet(propertySet);
          }
        }

        if (body.getPropertySets().size() != 0) {
          body.setBody(m_body);
        }
      } catch (IOException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Caught exception parsing msg body.");
          LOG.debug("Message Body: " + s, e);
        }
        throw new BodyParseException(e);
      }
    }
    return body;
  }

  private void processNode(JsonNode node, String path, NamedPropertySet propertySet, RequestBody body) {
    Iterator<String> iter = node.getFieldNames();
    String name;
    while (iter.hasNext()) {
      name = iter.next();
      JsonNode child = node.get(name);
      if (child.isArray()) {
        //array
        Iterator<JsonNode> arrayIter = child.getElements();
        while (arrayIter.hasNext()) {
          NamedPropertySet arrayPropertySet = new NamedPropertySet(name, new HashMap<String, Object>());
          processNode(arrayIter.next(), "", arrayPropertySet, body);
          body.addPropertySet(arrayPropertySet);
        }
      } else if (child.isContainerNode()) {
        // object
        if (name.equals(BODY_TITLE)) {
          name = "";
          m_body = child.toString();
        }
        processNode(child, path.isEmpty() ? name : path + '/' + name, propertySet, body);
      } else {
        // field
       propertySet.getProperties().put(PropertyHelper.getPropertyId(
           path.equals(BODY_TITLE) ? "" : path, name), child.asText());
      }
    }
  }

  private String ensureArrayFormat(String s) {
    return s.startsWith("[") ? s : '[' + s + ']';
  }
}
