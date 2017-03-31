/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.util.Date;

public class JsonManagerBase {

  private JsonSerializer<Date> jsonDateSerialiazer = null;
  private JsonDeserializer<Date> jsonDateDeserialiazer = null;

  public JsonManagerBase() {
    jsonDateSerialiazer = new JsonSerializer<Date>() {

      @Override
      public JsonElement serialize(Date paramT, java.lang.reflect.Type paramType, JsonSerializationContext paramJsonSerializationContext) {
        return paramT == null ? null : new JsonPrimitive(paramT.getTime());
      }
    };

    jsonDateDeserialiazer = new JsonDeserializer<Date>() {

      @Override
      public Date deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        return json == null ? null : new Date(json.getAsLong());
      }

    };
  }

  protected String convertObjToString(Object obj) {
    if (obj == null) {
      return "";
    }

    Gson gson = new GsonBuilder()
      .registerTypeAdapter(Date.class, jsonDateSerialiazer)
      .registerTypeAdapter(Date.class, jsonDateDeserialiazer).create();

    return gson.toJson(obj);
  }
}
