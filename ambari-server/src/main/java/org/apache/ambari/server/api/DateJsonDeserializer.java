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

package org.apache.ambari.server.api;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.bind.util.ISO8601Utils;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

/**
 * Custom deserializer for date conversion.
 * This will support following formats:
 * <ul>
 *   <li> Epoch </li>
 *   <li> Local Date </li>
 *   <li> US Date </li>
 *   <li> ISO8601 Date </li>
 * </ul>
 */
public class DateJsonDeserializer implements JsonDeserializer<Date> {

  /**
   * Date Format for US Date.
   */
  private static final DateFormat EN_US_FORMAT = DateFormat.getDateTimeInstance(2, 2, Locale.US);

  /**
   * Date Format for local Date
   */
  private static final DateFormat LOCAL_FORMAT = DateFormat.getDateTimeInstance(2, 2);

  @Override
  public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    if (json.isJsonNull()) {
      return null;
    }

    return deserializeToDate(json);
  }

  /**
   * Convert {@link JsonElement} to {@link Date} object
   *
   * @param json
   * @return
   * @throws JsonSyntaxException
   */
  private synchronized Date deserializeToDate(JsonElement json) throws JsonSyntaxException {
    try {
      return new Date(json.getAsJsonPrimitive().getAsLong());
    } catch (Exception ex1) {
      try {
        return LOCAL_FORMAT.parse(json.getAsJsonPrimitive().getAsString());
      } catch (ParseException ex2) {
        try {
          return EN_US_FORMAT.parse(json.getAsJsonPrimitive().getAsString());
        } catch (ParseException ex3) {
          try {
            return ISO8601Utils.parse(json.getAsJsonPrimitive().getAsString(), new ParsePosition(0));
          } catch (ParseException ex4) {
            throw new JsonSyntaxException(json.getAsJsonPrimitive().getAsString(), ex4);
          }
        }
      }
    }
  }
}
