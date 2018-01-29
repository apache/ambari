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
package org.apache.ambari.server.utils;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Static Helper methods for Json processing.
 */
public class JsonUtils {

  /**
   * Used to serialize to/from json.
   */
  public static JsonParser jsonParser = new JsonParser();

  private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

  /**
   * Checks if an input string is in valid JSON format
   * @param jsonString input json string to validate
   * @return <tt>true</tt> if the input string is in valid JSON format
   */
  public static boolean isValidJson(String jsonString) {

    if(StringUtils.isBlank(jsonString)) {
      return false;
    }
    try {
      jsonParser.parse(jsonString);
      return true;
    } catch (JsonSyntaxException jse) {
      return false;
    }
  }

  public static <T> T fromJson(String json, TypeReference<? extends T> valueType) {
    if (null == json) {
      return  null;
    }
    try {
      return JSON_SERIALIZER.readValue(json, valueType);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static String toJson(Object object) {
    try {
      return JSON_SERIALIZER.writeValueAsString(object);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
