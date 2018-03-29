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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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

  private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper().
    setSerializationInclusion(JsonInclude.Include.NON_NULL);
  private static final ObjectWriter JSON_WRITER = JSON_SERIALIZER.writer();

  /**
   * Checks if an input string is in valid JSON format
   * @param jsonString input json string to validate
   * @return <tt>true</tt> if the input string is in valid JSON format
   * @throws UncheckedIOException when conversion error occurs
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

  /**
   * Converts a json String to the object specified by the received type reference
   * @param json the json String
   * @param valueType the type reference capturing the type of the conversion result
   * @param <T> the type of the resulting object
   * @return the object depersisted from json
   * @throws UncheckedIOException when conversion error occurs
   */
  public static <T> T fromJson(String json, TypeReference<? extends T> valueType) throws UncheckedIOException {
    if (null == json) {
      return  null;
    }
    try {
      return JSON_SERIALIZER.reader(valueType).readValue(json);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Converts a json String to the object specified by the received type reference
   * @param json the json String
   * @param valueType the class of the conversion result
   * @param <T> the type of the resulting object
   * @return the object depersisted from json
   * @throws UncheckedIOException when conversion error occurs
   */
  public static <T> T fromJson(String json, Class<T> valueType) throws UncheckedIOException {
    if (null == json) {
      return  null;
    }
    try {
      return JSON_SERIALIZER.reader(valueType).readValue(json);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Converts an object to json string
   * @param object the object to convert
   * @return the resulting json
   * @throws UncheckedIOException when conversion error occurs
   */
  public static String toJson(Object object) throws UncheckedIOException {
    try {
      return JSON_WRITER.writeValueAsString(object);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
