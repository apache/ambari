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
package org.apache.ambari.server.mpack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Strict JSON and deterministic object ordering shared by lifecycle contracts. */
public final class MpackJson {
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
      .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
      .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
      .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
      .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

  private MpackJson() {
  }

  public static JsonNode read(byte[] bytes) {
    try (JsonParser parser = MAPPER.getFactory().createParser(bytes)) {
      JsonNode node = MAPPER.readTree(parser);
      if (node == null || parser.nextToken() != null) {
        throw invalid("A single JSON value is required");
      }
      return node;
    } catch (IOException e) {
      throw invalid("Invalid JSON document");
    }
  }

  public static JsonNode read(String text) {
    if (text == null) {
      throw invalid("JSON document is required");
    }
    return read(text.getBytes(StandardCharsets.UTF_8));
  }

  public static ObjectNode object() {
    return MAPPER.createObjectNode();
  }

  public static JsonNode tree(Object value) {
    return MAPPER.valueToTree(value);
  }

  public static <T> T decode(String json, Class<T> type) {
    try {
      return MAPPER.treeToValue(read(json), type);
    } catch (IOException | IllegalArgumentException e) {
      throw new MpackException(MpackException.Code.INVALID_RECEIPT,
          "Stored management pack contract is missing required fields or has invalid types");
    }
  }

  public static String canonical(JsonNode node) {
    return sorted(node).toString();
  }

  public static String digest(JsonNode node) {
    return DigestUtils.sha256Hex(canonical(node).getBytes(StandardCharsets.UTF_8));
  }

  public static void fields(JsonNode node, Set<String> allowed) {
    if (node == null || !node.isObject()) {
      throw invalid("Expected a JSON object");
    }
    node.fieldNames().forEachRemaining(name -> {
      if (!allowed.contains(name)) {
        throw invalid("Unknown contract field: " + name);
      }
    });
  }

  public static String string(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isTextual() || value.textValue().isEmpty()) {
      throw invalid("A nonempty string is required: " + field);
    }
    return value.textValue();
  }

  public static String optionalString(JsonNode node, String field) {
    return node.has(field) ? string(node, field) : null;
  }

  public static ArrayNode array(JsonNode node, String field, boolean required) {
    JsonNode value = node.get(field);
    if (value == null && !required) {
      return MAPPER.createArrayNode();
    }
    if (value == null || !value.isArray() || (required && value.isEmpty())) {
      throw invalid("An array is required: " + field);
    }
    return (ArrayNode) value;
  }

  public static MpackException invalid(String message) {
    return new MpackException(MpackException.Code.INVALID_MANIFEST, message);
  }

  private static JsonNode sorted(JsonNode node) {
    if (node.isObject()) {
      ObjectNode result = object();
      ArrayList<String> names = new ArrayList<>();
      node.fieldNames().forEachRemaining(names::add);
      names.sort(String::compareTo);
      names.forEach(name -> result.set(name, sorted(node.get(name))));
      return result;
    }
    if (node.isArray()) {
      ArrayNode result = MAPPER.createArrayNode();
      node.forEach(value -> result.add(sorted(value)));
      return result;
    }
    return node;
  }
}
