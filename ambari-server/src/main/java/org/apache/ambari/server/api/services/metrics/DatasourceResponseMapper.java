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
package org.apache.ambari.server.api.services.metrics;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.orm.entities.DatasourceEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.inject.Singleton;

/** Converts datasource entities without exposing stored credentials. */
@Singleton
public class DatasourceResponseMapper {
  private static final String REDACTED = "[redacted]";
  private static final Set<String> SENSITIVE_NAME_FRAGMENTS = Set.of(
      "password", "passwd", "secret", "token", "apikey", "authorization", "cookie",
      "privatekey", "clientkey", "accesskey", "credential");

  private final ObjectMapper objectMapper = new ObjectMapper();

  public DatasourceResponse toResponse(DatasourceEntity entity) {
    return new DatasourceResponse(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.getCategory(),
        entity.getPluginId(),
        entity.getPluginType(),
        entity.getPluginTypeName(),
        entity.getClusterName(),
        parseAndRedact(entity.getSettings()),
        parseAndRedact(entity.getHttp()),
        hasConfiguredAuth(entity.getAuth()),
        entity.getStatus(),
        Boolean.TRUE.equals(entity.getDefaultDatasource()),
        entity.getCreatedAt(),
        entity.getCreatedBy(),
        entity.getUpdatedAt(),
        entity.getUpdatedBy());
  }

  JsonNode parseAndRedact(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return objectMapper.createObjectNode();
    }

    try {
      return redact(objectMapper.readTree(rawJson));
    } catch (JsonProcessingException e) {
      return objectMapper.createObjectNode();
    }
  }

  boolean hasConfiguredAuth(String rawAuth) {
    if (rawAuth == null || rawAuth.isBlank()) {
      return false;
    }

    try {
      JsonNode auth = objectMapper.readTree(rawAuth);
      return auth != null && !auth.isNull() && !(auth.isObject() && auth.isEmpty());
    } catch (JsonProcessingException e) {
      return true;
    }
  }

  private JsonNode redact(JsonNode node) {
    if (node == null || node.isNull()) {
      return node;
    }
    if (node.isArray()) {
      ArrayNode result = objectMapper.createArrayNode();
      for (JsonNode child : node) {
        result.add(redact(child));
      }
      return result;
    }
    if (!node.isObject()) {
      return node.deepCopy();
    }

    ObjectNode result = objectMapper.createObjectNode();
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      if (isSensitiveName(field.getKey())) {
        result.put(field.getKey(), REDACTED);
      } else if ("headers".equalsIgnoreCase(field.getKey()) && field.getValue().isArray()) {
        result.set(field.getKey(), redactHeaders(field.getValue()));
      } else if ("url".equalsIgnoreCase(field.getKey()) && field.getValue().isTextual()) {
        result.set(field.getKey(), TextNode.valueOf(redactUrl(field.getValue().textValue())));
      } else {
        result.set(field.getKey(), redact(field.getValue()));
      }
    }
    return result;
  }

  private JsonNode redactHeaders(JsonNode headers) {
    ArrayNode result = objectMapper.createArrayNode();
    for (JsonNode header : headers) {
      if (!header.isObject()) {
        result.add(redact(header));
        continue;
      }
      ObjectNode copy = (ObjectNode) redact(header);
      String name = header.path("key").asText(header.path("name").asText(""));
      if (isSensitiveName(name)) {
        if (copy.has("value")) {
          copy.put("value", REDACTED);
        }
        if (copy.has("val")) {
          copy.put("val", REDACTED);
        }
      }
      result.add(copy);
    }
    return result;
  }

  private boolean isSensitiveName(String name) {
    String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    return SENSITIVE_NAME_FRAGMENTS.stream().anyMatch(normalized::contains);
  }

  private String redactUrl(String value) {
    try {
      URI uri = new URI(value);
      String authority = uri.getRawAuthority();
      if (authority != null && authority.contains("@")) {
        authority = authority.substring(authority.lastIndexOf('@') + 1);
      }
      String query = redactQuery(uri.getRawQuery());
      return new URI(uri.getScheme(), authority, uri.getRawPath(), query, uri.getRawFragment()).toString();
    } catch (URISyntaxException e) {
      return REDACTED;
    }
  }

  private String redactQuery(String query) {
    if (query == null || query.isEmpty()) {
      return query;
    }

    String[] parameters = query.split("&", -1);
    for (int i = 0; i < parameters.length; i++) {
      int separator = parameters[i].indexOf('=');
      String name = separator < 0 ? parameters[i] : parameters[i].substring(0, separator);
      if (isSensitiveName(name)) {
        parameters[i] = name + "=" + REDACTED;
      }
    }
    return String.join("&", parameters);
  }
}
