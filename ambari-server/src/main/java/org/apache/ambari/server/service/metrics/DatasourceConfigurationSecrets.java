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
package org.apache.ambari.server.service.metrics;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/** Separates sensitive datasource values from configuration safe to persist in the Ambari database. */
final class DatasourceConfigurationSecrets {
  static final String REDACTED = "[redacted]";

  private static final Set<String> SENSITIVE_NAME_FRAGMENTS = Set.of(
      "password", "passwd", "secret", "token", "apikey", "authorization", "cookie",
      "privatekey", "clientkey", "accesskey", "credential");

  private final ObjectMapper objectMapper;

  DatasourceConfigurationSecrets(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  Split split(JsonNode source) {
    JsonNode value = source == null ? objectMapper.createObjectNode() : source.deepCopy();
    return splitNode(value);
  }

  JsonNode restore(JsonNode sanitized, JsonNode secrets) {
    JsonNode base = sanitized == null ? NullNode.getInstance() : sanitized.deepCopy();
    if (!hasSecrets(secrets)) {
      return base;
    }
    if (secrets.isObject()) {
      ObjectNode restored = base.isObject() ? (ObjectNode) base : objectMapper.createObjectNode();
      secrets.fields().forEachRemaining(field ->
          restored.set(field.getKey(), restore(restored.get(field.getKey()), field.getValue())));
      return restored;
    }
    if (secrets.isArray()) {
      ArrayNode restored = base.isArray() ? (ArrayNode) base : objectMapper.createArrayNode();
      for (int index = 0; index < secrets.size(); index++) {
        JsonNode secret = secrets.get(index);
        if (!hasSecrets(secret)) {
          continue;
        }
        while (restored.size() <= index) {
          restored.addNull();
        }
        restored.set(index, restore(restored.get(index), secret));
      }
      return restored;
    }
    return secrets.deepCopy();
  }

  boolean hasSecrets(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return false;
    }
    if (node.isObject()) {
      return node.fields().hasNext();
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        if (hasSecrets(child)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private Split splitNode(JsonNode source) {
    if (source.isArray()) {
      ArrayNode sanitized = objectMapper.createArrayNode();
      ArrayNode secrets = objectMapper.createArrayNode();
      for (JsonNode child : source) {
        Split split = splitNode(child);
        sanitized.add(split.sanitized());
        secrets.add(hasSecrets(split.secrets()) ? split.secrets() : NullNode.getInstance());
      }
      return new Split(sanitized, secrets);
    }
    if (!source.isObject()) {
      return new Split(source.deepCopy(), NullNode.getInstance());
    }

    ObjectNode sanitized = objectMapper.createObjectNode();
    ObjectNode secrets = objectMapper.createObjectNode();
    Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String name = field.getKey();
      JsonNode value = field.getValue();
      if (isSensitiveName(name)) {
        sanitized.set(name, TextNode.valueOf(REDACTED));
        secrets.set(name, value.deepCopy());
        continue;
      }
      Split split = "headers".equalsIgnoreCase(name) && value.isArray()
          ? splitHeaders((ArrayNode) value)
          : splitNode(value);
      sanitized.set(name, split.sanitized());
      if (hasSecrets(split.secrets())) {
        secrets.set(name, split.secrets());
      }
    }
    return new Split(sanitized, secrets);
  }

  private Split splitHeaders(ArrayNode headers) {
    ArrayNode sanitized = objectMapper.createArrayNode();
    ArrayNode secrets = objectMapper.createArrayNode();
    for (JsonNode header : headers) {
      Split split = splitNode(header);
      JsonNode safeHeader = split.sanitized();
      JsonNode secretHeader = split.secrets();
      if (header.isObject()) {
        String name = header.path("key").asText(header.path("name").asText(""));
        if (isSensitiveName(name)) {
          ObjectNode safeObject = safeHeader.isObject()
              ? (ObjectNode) safeHeader : objectMapper.createObjectNode();
          ObjectNode secretObject = secretHeader.isObject()
              ? (ObjectNode) secretHeader : objectMapper.createObjectNode();
          moveHeaderValue(header, safeObject, secretObject, "value");
          moveHeaderValue(header, safeObject, secretObject, "val");
          safeHeader = safeObject;
          secretHeader = secretObject;
        }
      }
      sanitized.add(safeHeader);
      secrets.add(hasSecrets(secretHeader) ? secretHeader : NullNode.getInstance());
    }
    return new Split(sanitized, secrets);
  }

  private void moveHeaderValue(JsonNode source, ObjectNode sanitized, ObjectNode secrets, String field) {
    if (source.has(field)) {
      sanitized.put(field, REDACTED);
      secrets.set(field, source.get(field).deepCopy());
    }
  }

  private boolean isSensitiveName(String name) {
    String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    return SENSITIVE_NAME_FRAGMENTS.stream().anyMatch(normalized::contains);
  }

  record Split(JsonNode sanitized, JsonNode secrets) {
  }
}
