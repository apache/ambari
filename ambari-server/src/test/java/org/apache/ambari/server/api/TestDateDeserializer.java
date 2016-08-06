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

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.bind.util.ISO8601Utils;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class TestDateDeserializer {

  Gson gson = new Gson();
  DateJsonDeserializer deserializer = new DateJsonDeserializer();

  @Test
  public void testDeserializeWithEpochDate() {
    String date = "1470301497";
    assertEquals(parseDate(date), new Date(1470301497));
  }

  @Test
  public void testDeserializeWithLocalDate() throws ParseException {
    String date = "12 Aug, 2016 11:02:47 PM";
    assertEquals(parseDate(date), DateFormat.getDateTimeInstance(2, 2).parse(date));
  }

  @Test
  public void testDeserializeWithUSDate() throws ParseException {
    String date = "Aug 12, 2016 11:01:21 PM";
    assertEquals(parseDate(date), DateFormat.getDateTimeInstance(2, 2, Locale.US).parse(date));
  }

  @Test
  public void testDeserializeWithISODate() throws ParseException {
    String dateString = "2016-07-07T15:15:15Z";
    assertEquals(parseDate(dateString), ISO8601Utils.parse(dateString, new ParsePosition(0)));
  }

  @Test(expected = JsonSyntaxException.class)
  public void testDeserializeWithInvalidDate() {
    String date = "nopattern";
    parseDate(date);
  }

  @Test
  public void testDeserializeWithNullDate() {
    String json = "{\"date\" : null}";
    JsonElement jsonDate = new JsonParser().parse(json).getAsJsonObject().get("date");
    Date date = deserializer.deserialize(jsonDate,
      Mockito.mock(Type.class), Mockito.mock(JsonDeserializationContext.class));
    assertEquals(date, null);
  }

  private Date parseDate(String date) {
    return deserializer.deserialize(new JsonPrimitive(date),
      Mockito.mock(Type.class), Mockito.mock(JsonDeserializationContext.class));
  }
}
